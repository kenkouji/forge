import asyncio
import base64
import time
import uuid
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query, Header, status
from fastapi.responses import StreamingResponse, JSONResponse
from sqlalchemy.orm import Session

from app.database.session import get_db, SessionLocal
from app.models.entities import Device, User
from app.auth.security import decode_access_token
from app.websocket.manager import ws_manager
from app.schemas.all_schemas import StreamControlRequest, StreamStatusResponse

router = APIRouter(prefix="/stream", tags=["Screen Stream"])

# In-memory stream tracking: device_id -> { "is_active": bool, "clients": int, "last_frame_time": float, "fps": float, "latency_ms": float }
STREAM_STATES = {}


async def get_stream_user(
    token: Optional[str] = Query(None),
    authorization: Optional[str] = Header(None),
    db: Session = Depends(get_db)
):
    """Validates JWT auth from either query param (for <img>/video streams) or Authorization header. Supports both User and Guest sessions."""
    from app.models.entities import GuestSession, PrivacyShieldState
    import datetime
    import json

    raw_token = token
    if not raw_token and authorization and authorization.startswith("Bearer "):
        raw_token = authorization.split(" ")[1]

    if not raw_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing authentication token")

    payload = decode_access_token(raw_token)
    if not payload:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid or expired authentication token")

    role = payload.get("role")
    if role == "guest":
        session_id = payload.get("session_id")
        session = db.query(GuestSession).filter(GuestSession.id == session_id).first()
        if not session:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Guest session not found")
        now = datetime.datetime.now(datetime.timezone.utc)
        session_expires = session.expires_at
        if session_expires.tzinfo is None:
            session_expires = session_expires.replace(tzinfo=datetime.timezone.utc)
        if session_expires < now or session.status in ["EXPIRED", "REVOKED"]:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Guest session has expired or been revoked")
        caps = json.loads(session.approved_capabilities_json) if session.approved_capabilities_json else []
        if "screen.view" not in caps:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Missing capability: screen.view")
        # Return guest caller descriptor
        class GuestCaller:
            id = session.id
            username = session.guest_name
            is_guest = True
            session_id = session.id
            device_id = session.device_id
        return GuestCaller()
    else:
        username = payload.get("sub")
        user = db.query(User).filter(User.username == username).first()
        if not user:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
        user.is_guest = False
        return user


@router.get("/live/{device_id}")
async def stream_live_screen(
    device_id: str,
    width: int = Query(960, ge=360, le=1920),
    quality: int = Query(50, ge=20, le=90),
    fps: int = Query(10, ge=1, le=25),
    user = Depends(get_stream_user),
    db: Session = Depends(get_db)
):
    """
    Ultra-low-latency multipart MJPEG screen streaming endpoint.
    Enforces Privacy Shield: if screen is paused or lockdown active, guests receive paused frames.
    """
    from app.models.entities import PrivacyShieldState, GuestSession
    import datetime

    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail=f"Target device '{device_id}' is offline.")

    frame_interval = 1.0 / max(1, min(fps, 25))

    # Pre-generate a 1x1 or dark purple paused JPEG frame
    # 64-byte minimal valid JPEG for paused state
    PAUSED_JPEG = (
        b'\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x01\x00H\x00H\x00\x00\xff\xdb\x00C\x00'
        b'\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\t\t\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19'
        b'\x12\x13\x0f\x14\x1d\x1a\x1f\x1e\x1d\x1a\x1c\x1c $.\' ",#\x1c\x1c(7),01444\x1f\'9=82<.342\xff'
        b'\xc0\x00\x0b\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x1f\x00\x00\x01\x05\x01\x01\x01'
        b'\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\xff\xda'
        b'\x00\x08\x01\x01\x00\x00?\x00\xbf\x00\xff\xd9'
    )

    async def frame_generator():
        stream_entry = STREAM_STATES.setdefault(device_id, {
            "is_active": True,
            "clients": 0,
            "last_frame_time": time.time(),
            "fps": fps,
            "latency_ms": 0.0
        })
        stream_entry["clients"] += 1

        try:
            while True:
                t_start = time.perf_counter()
                if not ws_manager.is_agent_online(device_id):
                    break

                # Check privacy shield and guest status
                is_guest = getattr(user, "is_guest", False)
                if is_guest:
                    # Verify session is still valid
                    check_db = SessionLocal()
                    try:
                        g_sess = check_db.query(GuestSession).filter(GuestSession.id == user.session_id).first()
                        now = datetime.datetime.now(datetime.timezone.utc)
                        if not g_sess or g_sess.status != "APPROVED":
                            break
                        exp = g_sess.expires_at
                        if exp.tzinfo is None:
                            exp = exp.replace(tzinfo=datetime.timezone.utc)
                        if exp < now:
                            break

                        ps = check_db.query(PrivacyShieldState).filter(PrivacyShieldState.device_id == device_id).first()
                        if ps and (ps.is_screen_paused or ps.is_lockdown):
                            # Stream paused frame to guest
                            yield (
                                b"--frame\r\n"
                                b"Content-Type: image/jpeg\r\n"
                                b"Content-Length: " + str(len(PAUSED_JPEG)).encode() + b"\r\n\r\n"
                                + PAUSED_JPEG + b"\r\n"
                            )
                            await asyncio.sleep(0.5)
                            continue
                    finally:
                        check_db.close()

                req_id = str(uuid.uuid4())
                try:
                    res = await ws_manager.send_command_to_agent(
                        device_id=device_id,
                        request_id=req_id,
                        tool="get_stream_frame",
                        arguments={"width": width, "quality": quality},
                        timeout_seconds=3.0
                    )
                except Exception:
                    await asyncio.sleep(frame_interval)
                    continue

                if res and res.get("success") and res.get("result"):
                    frame_info = res["result"]
                    b64_data = frame_info.get("data_base64")
                    dur_ms = frame_info.get("capture_duration_ms", 0.0)
                    stream_entry["latency_ms"] = dur_ms

                    if b64_data:
                        raw_bytes = base64.b64decode(b64_data)
                        yield (
                            b"--frame\r\n"
                            b"Content-Type: image/jpeg\r\n"
                            b"Content-Length: " + str(len(raw_bytes)).encode() + b"\r\n\r\n"
                            + raw_bytes + b"\r\n"
                        )

                elapsed = time.perf_counter() - t_start
                sleep_time = max(0.01, frame_interval - elapsed)
                await asyncio.sleep(sleep_time)

        finally:
            stream_entry["clients"] = max(0, stream_entry["clients"] - 1)
            if stream_entry["clients"] == 0:
                stream_entry["is_active"] = False

    return StreamingResponse(
        frame_generator(),
        media_type="multipart/x-mixed-replace; boundary=frame"
    )


@router.post("/control/{device_id}")
async def control_screen_stream(
    device_id: str,
    req: StreamControlRequest,
    user: User = Depends(get_stream_user)
):
    """
    Controls live screen streaming: start, stop, or adapt quality/fps.
    """
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail=f"Device '{device_id}' is offline.")

    req_id = str(uuid.uuid4())
    res = await ws_manager.send_command_to_agent(
        device_id=device_id,
        request_id=req_id,
        tool="configure_stream",
        arguments={
            "width": req.width or 960,
            "quality": req.quality or 50,
            "fps": req.fps or 10
        },
        timeout_seconds=5.0
    )

    state = STREAM_STATES.setdefault(device_id, {
        "is_active": req.action != "stop",
        "clients": 1 if req.action == "start" else 0,
        "last_frame_time": time.time(),
        "fps": req.fps or 10,
        "latency_ms": 0.0
    })

    return {
        "success": True,
        "action": req.action,
        "device_id": device_id,
        "state": state,
        "agent_response": res.get("result")
    }


@router.get("/status/{device_id}", response_model=StreamStatusResponse)
async def get_screen_stream_status(
    device_id: str,
    user: User = Depends(get_stream_user)
):
    """
    Returns live streaming telemetry (active status, FPS, resolution, latency).
    """
    state = STREAM_STATES.get(device_id, {})
    is_active = state.get("is_active", False) and ws_manager.is_agent_online(device_id)

    # Query agent status if online
    if ws_manager.is_agent_online(device_id):
        try:
            req_id = str(uuid.uuid4())
            res = await ws_manager.send_command_to_agent(
                device_id=device_id,
                request_id=req_id,
                tool="get_stream_status",
                arguments={},
                timeout_seconds=2.0
            )
            agent_stat = res.get("result", {})
            return StreamStatusResponse(
                is_active=is_active,
                width=agent_stat.get("target_width", 960),
                height=agent_stat.get("display_height", 600),
                fps=float(agent_stat.get("target_fps", 10)),
                latency_ms=float(agent_stat.get("last_frame_duration_ms", 0.0)),
                kbps=float(agent_stat.get("last_frame_size_kb", 0.0)) * 8.0
            )
        except Exception:
            pass

    return StreamStatusResponse(
        is_active=is_active,
        width=960,
        height=600,
        fps=0.0,
        latency_ms=0.0,
        kbps=0.0
    )
