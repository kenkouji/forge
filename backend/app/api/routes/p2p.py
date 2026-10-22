"""
RemoteForge Embedded Zero-Config P2P Mesh Tunneling API Route
==============================================================
Provides WebRTC ICE, STUN/TURN signaling broker, and automated NAT traversal
for direct encrypted peer-to-peer tunnels without port forwarding.
"""

import asyncio
import datetime
import logging
import uuid
from typing import Dict, Any, List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, Header, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import Device, User
from app.auth.security import decode_access_token
from app.websocket.manager import ws_manager
from app.schemas.all_schemas import (
    P2pConfigResponse,
    P2pSessionCreateRequest,
    P2pSessionResponse,
    P2pSignalMessage,
)

logger = logging.getLogger("remoteforge.p2p")
router = APIRouter(prefix="/p2p", tags=["Zero-Config P2P Mesh"])

# In-memory P2P sessions: session_id -> session dict
P2P_SESSIONS: Dict[str, Dict[str, Any]] = {}

# Default public STUN servers for NAT puncture
DEFAULT_STUN_SERVERS = [
    "stun:stun.l.google.com:19302",
    "stun:stun1.l.google.com:19302",
    "stun:stun2.l.google.com:19302",
    "stun:stun.cloudflare.com:3478"
]


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


@router.get("/config", response_model=P2pConfigResponse)
async def get_p2p_configuration(user: User = Depends(get_current_user)):
    """
    Returns high-speed public STUN and TURN server endpoints.
    """
    return P2pConfigResponse(
        stun_servers=DEFAULT_STUN_SERVERS,
        turn_servers=[
            {"url": "turn:openrelay.metered.ca:80", "username": "openrelayproject", "credential": "openrelayproject"}
        ],
        relay_fallback_enabled=True
    )


@router.post("/session/create", response_model=P2pSessionResponse)
async def create_p2p_session(
    req: P2pSessionCreateRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Initializes a WebRTC peer connection session between the client and host agent.
    """
    device = db.query(Device).filter(Device.id == req.device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    session_id = str(uuid.uuid4())
    now = datetime.datetime.now(datetime.timezone.utc)

    session_state = {
        "session_id": session_id,
        "device_id": req.device_id,
        "client_type": req.client_type,
        "status": "INITIATING",
        "created_at": now,
        "signals": [],  # List of dicts: { "sender_type": str, "signal_type": str, "payload_json": str, "timestamp": float }
        "client_connected": True,
        "agent_connected": False
    }
    P2P_SESSIONS[session_id] = session_state

    # Alert agent about inbound P2P request
    if ws_manager.is_agent_connected(req.device_id):
        await ws_manager.send_to_agent(req.device_id, {
            "type": "command",
            "request_id": session_id,
            "tool": "p2p_session_init",
            "arguments": {
                "session_id": session_id,
                "client_type": req.client_type,
                "stun_servers": DEFAULT_STUN_SERVERS
            }
        })
        logger.info(f"Dispatched p2p_session_init to agent {req.device_id} (session {session_id})")

    return P2pSessionResponse(
        session_id=session_id,
        device_id=req.device_id,
        status="INITIATING",
        signaling_channel_ws=f"/ws/p2p/signaling/{session_id}",
        created_at=now
    )


@router.post("/signal/{session_id}")
async def post_signal_message(
    session_id: str,
    signal: P2pSignalMessage,
    user: User = Depends(get_current_user)
):
    """
    Exchanges SDP offers, answers, or ICE candidates between peers.
    """
    session = P2P_SESSIONS.get(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="P2P session not found")

    import time
    entry = {
        "sender_type": signal.sender_type,
        "signal_type": signal.signal_type,
        "payload_json": signal.payload_json,
        "timestamp": time.time()
    }
    session["signals"].append(entry)

    if signal.signal_type == "answer":
        session["status"] = "CONNECTED"
    elif signal.signal_type == "bye":
        session["status"] = "DISCONNECTED"

    # Forward immediately to agent if sender is client
    if signal.sender_type == "client" and ws_manager.is_agent_connected(session["device_id"]):
        await ws_manager.send_to_agent(session["device_id"], {
            "type": "p2p_signal",
            "session_id": session_id,
            "signal": entry
        })

    return {"status": "received", "signal_type": signal.signal_type}


@router.get("/signals/{session_id}")
async def get_pending_signals(
    session_id: str,
    for_receiver: str = Query("client"),  # "client" or "agent"
    user: User = Depends(get_current_user)
):
    session = P2P_SESSIONS.get(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="P2P session not found")

    # Filter signals sent by the OTHER peer
    other_type = "agent" if for_receiver == "client" else "client"
    pending = [s for s in session["signals"] if s["sender_type"] == other_type]
    
    return {
        "session_id": session_id,
        "status": session["status"],
        "signals": pending
    }


@router.get("/session/{session_id}", response_model=P2pSessionResponse)
async def get_session_details(session_id: str, user: User = Depends(get_current_user)):
    session = P2P_SESSIONS.get(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="P2P session not found")

    return P2pSessionResponse(
        session_id=session_id,
        device_id=session["device_id"],
        status=session["status"],
        signaling_channel_ws=f"/ws/p2p/signaling/{session_id}",
        created_at=session["created_at"]
    )
