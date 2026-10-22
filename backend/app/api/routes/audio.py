"""
RemoteForge Low-Latency Bi-Directional Audio API Route
======================================================
Manages high-fidelity PC-to-Phone audio streaming (audio sink) and Phone-to-PC
wireless microphone relay using raw PCM / Opus chunks over WebSocket/P2P.
"""

import asyncio
import datetime
import logging
import uuid
from typing import Dict, Any, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, Header, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import Device, User
from app.auth.security import decode_access_token
from app.websocket.manager import ws_manager
from app.schemas.all_schemas import (
    AudioSessionStartRequest,
    AudioSessionStopRequest,
    AudioStatusResponse,
)

logger = logging.getLogger("remoteforge.audio")
router = APIRouter(prefix="/audio", tags=["Bi-Directional Audio"])

# In-memory audio session states: device_id -> dict
AUDIO_SESSIONS: Dict[str, Dict[str, Any]] = {}


def get_current_user(
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
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token")

    username = payload.get("sub")
    user = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user


@router.post("/start", response_model=AudioStatusResponse)
async def start_audio_session(
    req: AudioSessionStartRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Activates PC audio loopback streaming (sink) or wireless microphone relay (source).
    """
    device = db.query(Device).filter(Device.id == req.device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    session_id = str(uuid.uuid4())
    state = {
        "session_id": session_id,
        "device_id": req.device_id,
        "is_active": True,
        "mode": req.mode,
        "sample_rate": req.sample_rate,
        "channels": req.channels,
        "bit_depth": req.bit_depth,
        "codec": req.codec,
        "latency_ms": 24.5,
        "packets_transferred": 0,
        "started_at": datetime.datetime.now(datetime.timezone.utc),
        "user_id": user.id
    }
    AUDIO_SESSIONS[req.device_id] = state

    # Dispatch audio activation to host agent
    if ws_manager.is_agent_connected(req.device_id):
        await ws_manager.send_to_agent(req.device_id, {
            "type": "command",
            "request_id": session_id,
            "tool": "audio_start",
            "arguments": {
                "session_id": session_id,
                "mode": req.mode,
                "sample_rate": req.sample_rate,
                "channels": req.channels,
                "codec": req.codec
            }
        })
        logger.info(f"Dispatched audio_start to agent {req.device_id} (mode={req.mode}, {req.sample_rate}Hz)")

    return AudioStatusResponse(
        device_id=req.device_id,
        is_active=True,
        mode=req.mode,
        sample_rate=req.sample_rate,
        channels=req.channels,
        codec=req.codec,
        latency_ms=24.5,
        packets_transferred=0
    )


@router.post("/stop", response_model=AudioStatusResponse)
async def stop_audio_session(
    req: AudioSessionStopRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Terminates audio streaming and releases audio loopback devices.
    """
    session = AUDIO_SESSIONS.pop(req.device_id, None)

    if ws_manager.is_agent_connected(req.device_id):
        await ws_manager.send_to_agent(req.device_id, {
            "type": "command",
            "request_id": str(uuid.uuid4()),
            "tool": "audio_stop",
            "arguments": {"device_id": req.device_id}
        })

    mode = session.get("mode", "sink") if session else "sink"
    sample_rate = session.get("sample_rate", 48000) if session else 48000
    channels = session.get("channels", 2) if session else 2
    codec = session.get("codec", "pcm") if session else "pcm"

    return AudioStatusResponse(
        device_id=req.device_id,
        is_active=False,
        mode=mode,
        sample_rate=sample_rate,
        channels=channels,
        codec=codec,
        latency_ms=0.0,
        packets_transferred=0
    )


@router.get("/status/{device_id}", response_model=AudioStatusResponse)
async def get_audio_status(
    device_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    session = AUDIO_SESSIONS.get(device_id)
    if not session or not session.get("is_active"):
        return AudioStatusResponse(
            device_id=device_id,
            is_active=False,
            mode="sink",
            sample_rate=48000,
            channels=2,
            codec="pcm",
            latency_ms=0.0,
            packets_transferred=0
        )

    return AudioStatusResponse(
        device_id=device_id,
        is_active=True,
        mode=session["mode"],
        sample_rate=session["sample_rate"],
        channels=session["channels"],
        codec=session["codec"],
        latency_ms=session.get("latency_ms", 24.5),
        packets_transferred=session.get("packets_transferred", 0)
    )
