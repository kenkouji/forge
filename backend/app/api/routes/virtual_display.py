"""
RemoteForge Virtual Extended Display API Route
==============================================
Manages secondary monitor creation, viewport configuration, touch gesture
translation, and low-latency virtual display streaming for Android & tablets.
"""

import asyncio
import datetime
import logging
import uuid
from typing import Dict, Any, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, Header, status
from fastapi.responses import StreamingResponse, JSONResponse
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import Device, User
from app.auth.security import decode_access_token
from app.websocket.manager import ws_manager
from app.schemas.all_schemas import (
    VirtualDisplayStartRequest,
    VirtualDisplayStopRequest,
    VirtualDisplayStatusResponse,
    VirtualDisplayTouchRequest,
)

logger = logging.getLogger("remoteforge.virtual_display")
router = APIRouter(prefix="/virtual-display", tags=["Virtual Extended Display"])

# In-memory virtual display sessions: device_id -> session dict
VIRTUAL_DISPLAY_SESSIONS: Dict[str, Dict[str, Any]] = {}


def get_current_user_or_guest(
    token: Optional[str] = Query(None),
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db)
):
    raw_token = token
    if not raw_token and authorization and authorization.startswith("Bearer "):
        raw_token = authorization.split(" ")[1]

    if not raw_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing authentication token")

    payload = decode_access_token(raw_token)
    if not payload:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired token")

    username = payload.get("sub")
    user = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user


@router.post("/start", response_model=VirtualDisplayStatusResponse)
async def start_virtual_display(
    req: VirtualDisplayStartRequest,
    user: User = Depends(get_current_user_or_guest),
    db: Session = Depends(get_db)
):
    """
    Spawns or configures a secondary virtual monitor on the target host workstation.
    """
    device = db.query(Device).filter(Device.id == req.device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    session_id = str(uuid.uuid4())
    now = datetime.datetime.now(datetime.timezone.utc)

    session_state = {
        "session_id": session_id,
        "device_id": req.device_id,
        "is_active": True,
        "width": req.width,
        "height": req.height,
        "fps": req.fps,
        "current_fps": float(req.fps),
        "latency_ms": 12.5,
        "dpi": req.dpi,
        "touch_enabled": req.enable_touch,
        "started_at": now,
        "user_id": user.id,
        "frame_queue": asyncio.Queue(maxsize=10)
    }
    VIRTUAL_DISPLAY_SESSIONS[req.device_id] = session_state

    # Dispatch command to host agent
    if ws_manager.is_agent_connected(req.device_id):
        await ws_manager.send_to_agent(req.device_id, {
            "type": "command",
            "request_id": session_id,
            "tool": "virtual_display_start",
            "arguments": {
                "session_id": session_id,
                "width": req.width,
                "height": req.height,
                "fps": req.fps,
                "dpi": req.dpi,
                "enable_touch": req.enable_touch
            }
        })
        logger.info(f"Dispatched virtual_display_start to agent {req.device_id} ({req.width}x{req.height}@{req.fps}fps)")
    else:
        logger.warning(f"Agent {req.device_id} offline; session registered in virtual simulation mode.")

    return VirtualDisplayStatusResponse(
        device_id=req.device_id,
        is_active=True,
        width=req.width,
        height=req.height,
        fps=req.fps,
        current_fps=float(req.fps),
        latency_ms=12.5,
        touch_enabled=req.enable_touch,
        session_id=session_id,
        started_at=now
    )


@router.post("/stop", response_model=VirtualDisplayStatusResponse)
async def stop_virtual_display(
    req: VirtualDisplayStopRequest,
    user: User = Depends(get_current_user_or_guest),
    db: Session = Depends(get_db)
):
    """
    Stops the secondary virtual monitor and restores normal desktop geometry.
    """
    session = VIRTUAL_DISPLAY_SESSIONS.pop(req.device_id, None)

    if ws_manager.is_agent_connected(req.device_id):
        await ws_manager.send_to_agent(req.device_id, {
            "type": "command",
            "request_id": str(uuid.uuid4()),
            "tool": "virtual_display_stop",
            "arguments": {
                "device_id": req.device_id
            }
        })

    width = session.get("width", 1920) if session else 1920
    height = session.get("height", 1080) if session else 1080
    fps = session.get("fps", 60) if session else 60

    return VirtualDisplayStatusResponse(
        device_id=req.device_id,
        is_active=False,
        width=width,
        height=height,
        fps=fps,
        current_fps=0.0,
        latency_ms=0.0,
        touch_enabled=False,
        session_id=None,
        started_at=None
    )


@router.get("/status/{device_id}", response_model=VirtualDisplayStatusResponse)
async def get_virtual_display_status(
    device_id: str,
    user: User = Depends(get_current_user_or_guest),
    db: Session = Depends(get_db)
):
    session = VIRTUAL_DISPLAY_SESSIONS.get(device_id)
    if not session or not session.get("is_active"):
        return VirtualDisplayStatusResponse(
            device_id=device_id,
            is_active=False,
            width=1920,
            height=1080,
            fps=60,
            current_fps=0.0,
            latency_ms=0.0,
            touch_enabled=True,
            session_id=None,
            started_at=None
        )

    return VirtualDisplayStatusResponse(
        device_id=device_id,
        is_active=True,
        width=session["width"],
        height=session["height"],
        fps=session["fps"],
        current_fps=session.get("current_fps", float(session["fps"])),
        latency_ms=session.get("latency_ms", 12.0),
        touch_enabled=session.get("touch_enabled", True),
        session_id=session.get("session_id"),
        started_at=session.get("started_at")
    )


@router.post("/touch")
async def inject_touch_gesture(
    req: VirtualDisplayTouchRequest,
    user: User = Depends(get_current_user_or_guest),
    db: Session = Depends(get_db)
):
    """
    Translates mobile touch gestures (tap, double tap, scroll, drag) into
    native OS input actions on the secondary display viewport.
    """
    session = VIRTUAL_DISPLAY_SESSIONS.get(req.device_id)
    if not session or not session.get("is_active"):
        raise HTTPException(status_code=400, detail="Virtual display session is not active")

    # Forward to agent via websocket
    if ws_manager.is_agent_connected(req.device_id):
        await ws_manager.send_to_agent(req.device_id, {
            "type": "command",
            "request_id": str(uuid.uuid4()),
            "tool": "virtual_display_touch",
            "arguments": {
                "action": req.action,
                "norm_x": req.norm_x,
                "norm_y": req.norm_y,
                "scroll_dx": req.scroll_dx,
                "scroll_dy": req.scroll_dy,
                "target_width": session["width"],
                "target_height": session["height"]
            }
        })
        return {"status": "dispatched", "action": req.action}

    return {"status": "simulated", "action": req.action}


@router.get("/feed/{device_id}")
async def virtual_display_stream_feed(
    device_id: str,
    fps: int = Query(60, ge=1, le=120),
    quality: int = Query(75, ge=20, le=100),
    token: Optional[str] = Query(None),
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db)
):
    """
    MJPEG low-latency streaming feed for the virtual secondary display.
    """
    # Authenticate token
    raw_token = token
    if not raw_token and authorization and authorization.startswith("Bearer "):
        raw_token = authorization.split(" ")[1]
    if not raw_token:
        raise HTTPException(status_code=401, detail="Unauthorized")
    payload = decode_access_token(raw_token)
    if not payload:
        raise HTTPException(status_code=401, detail="Invalid token")

    session = VIRTUAL_DISPLAY_SESSIONS.get(device_id)
    width = session["width"] if session else 1920
    height = session["height"] if session else 1080

    async def frame_generator():
        import base64
        import time
        from PIL import Image, ImageDraw
        import io

        frame_interval = 1.0 / max(1, min(fps, 60))
        target_w = min(width, 1440)

        # Pre-generate standby fallback frame if agent is connecting
        standby_bytes = None
        try:
            im = Image.new("RGB", (min(width, 1280), min(height, 720)), (18, 20, 29))
            draw = ImageDraw.Draw(im)
            draw.rectangle([(20, 20), (min(width, 1280) - 20, min(height, 720) - 20)], outline=(40, 48, 70), width=2)
            draw.text((40, 40), f"RemoteForge Virtual Display • Connecting to Host Display...", fill=(160, 180, 220))
            buf = io.BytesIO()
            im.save(buf, format="JPEG", quality=quality)
            standby_bytes = buf.getvalue()
        except Exception:
            pass

        while True:
            t_start = time.perf_counter()
            frame_bytes = None

            if ws_manager.is_agent_online(device_id):
                req_id = str(uuid.uuid4())
                try:
                    res = await ws_manager.send_command_to_agent(
                        device_id=device_id,
                        request_id=req_id,
                        tool="get_stream_frame",
                        arguments={"width": target_w, "quality": quality},
                        timeout_seconds=2.0
                    )
                    if res and res.get("success") and res.get("result"):
                        b64_data = res["result"].get("data_base64")
                        if b64_data:
                            frame_bytes = base64.b64decode(b64_data)
                except Exception as e:
                    logger.debug(f"Virtual display frame capture timeout or error: {e}")

            if not frame_bytes and standby_bytes:
                frame_bytes = standby_bytes

            if frame_bytes:
                try:
                    yield (
                        b"--frame\r\n"
                        b"Content-Type: image/jpeg\r\n"
                        b"Content-Length: " + str(len(frame_bytes)).encode() + b"\r\n\r\n" +
                        frame_bytes + b"\r\n"
                    )
                except Exception:
                    break

            elapsed = time.perf_counter() - t_start
            sleep_duration = max(0.005, frame_interval - elapsed)
            await asyncio.sleep(sleep_duration)

    return StreamingResponse(
        frame_generator(),
        media_type="multipart/x-mixed-replace; boundary=frame"
    )
