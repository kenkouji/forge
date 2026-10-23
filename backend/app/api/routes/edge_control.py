"""
RemoteForge Universal Control Edge Mouse Hopping API Route
==========================================================
Manages screen boundary detection, cursor lock/handover, and cross-device
spatial KVM navigation between PC and Android/Tablet screens.
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
    EdgeControlConfigRequest,
    EdgeControlStatusResponse,
    EdgeHandoffEvent,
)

logger = logging.getLogger("remoteforge.edge_control")
router = APIRouter(prefix="/edge-control", tags=["Universal Control Edge Hopping"])

# In-memory edge control states: device_id -> config dict
EDGE_CONTROL_STATES: Dict[str, Dict[str, Any]] = {}


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


@router.post("/configure", response_model=EdgeControlStatusResponse)
async def configure_edge_control(
    req: EdgeControlConfigRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Configures border layout (e.g. mobile placed to the RIGHT of primary monitor)
    and activates edge tracking on the host workstation.
    """
    device = db.query(Device).filter(Device.id == req.device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    state = {
        "device_id": req.device_id,
        "is_enabled": True,
        "configured_edge": req.edge.upper(),
        "sensitivity": req.sensitivity,
        "dwell_delay_ms": req.dwell_delay_ms,
        "enable_keyboard": req.enable_keyboard,
        "enable_clipboard_sync": req.enable_clipboard_sync,
        "is_remote_active": False,
        "cursor_x": 0.5,
        "cursor_y": 0.5,
        "last_handoff_at": None,
        "user_id": user.id
    }
    EDGE_CONTROL_STATES[req.device_id] = state

    # Dispatch configuration to host agent
    if ws_manager.is_agent_connected(req.device_id):
        await ws_manager.send_to_agent(req.device_id, {
            "type": "command",
            "request_id": str(uuid.uuid4()),
            "tool": "edge_control_configure",
            "arguments": {
                "edge": req.edge.upper(),
                "sensitivity": req.sensitivity,
                "dwell_delay_ms": req.dwell_delay_ms,
                "enable_keyboard": req.enable_keyboard,
                "enable_clipboard_sync": req.enable_clipboard_sync
            }
        })
        logger.info(f"Dispatched edge_control_configure to agent {req.device_id} (edge={req.edge})")

    return EdgeControlStatusResponse(
        device_id=req.device_id,
        is_enabled=True,
        configured_edge=req.edge.upper(),
        is_remote_active=False,
        cursor_x=0.5,
        cursor_y=0.5,
        last_handoff_at=None
    )


@router.post("/disable")
async def disable_edge_control(
    device_id: str = Query(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    state = EDGE_CONTROL_STATES.get(device_id)
    if state:
        state["is_enabled"] = False
        state["is_remote_active"] = False

    if ws_manager.is_agent_connected(device_id):
        await ws_manager.send_to_agent(device_id, {
            "type": "command",
            "request_id": str(uuid.uuid4()),
            "tool": "edge_control_disable",
            "arguments": {"device_id": device_id}
        })

    return {"status": "disabled", "device_id": device_id}


@router.get("/status/{device_id}", response_model=EdgeControlStatusResponse)
async def get_edge_control_status(
    device_id: str,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    state = EDGE_CONTROL_STATES.get(device_id)
    if not state:
        return EdgeControlStatusResponse(
            device_id=device_id,
            is_enabled=True,
            configured_edge="RIGHT",
            is_remote_active=False,
            cursor_x=0.5,
            cursor_y=0.5,
            last_handoff_at=None
        )

    return EdgeControlStatusResponse(
        device_id=device_id,
        is_enabled=state["is_enabled"],
        configured_edge=state["configured_edge"],
        is_remote_active=state["is_remote_active"],
        cursor_x=state.get("cursor_x", 0.5),
        cursor_y=state.get("cursor_y", 0.5),
        last_handoff_at=state.get("last_handoff_at")
    )


@router.post("/handoff")
async def process_edge_handoff_event(
    event: EdgeHandoffEvent,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Receives cursor transitions, movement updates, or return handoff triggers.
    """
    state = EDGE_CONTROL_STATES.get(event.device_id)
    if not state:
        state = {
            "device_id": event.device_id,
            "is_enabled": True,
            "configured_edge": "RIGHT",
            "is_remote_active": False,
            "cursor_x": 0.5,
            "cursor_y": 0.5,
            "last_handoff_at": None
        }
        EDGE_CONTROL_STATES[event.device_id] = state

    now = datetime.datetime.now(datetime.timezone.utc)
    if event.event_type == "ENTER_REMOTE":
        state["is_remote_active"] = True
        state["last_handoff_at"] = now
        state["cursor_x"] = event.norm_x
        state["cursor_y"] = event.norm_y
    elif event.event_type == "EXIT_REMOTE":
        state["is_remote_active"] = False
        state["last_handoff_at"] = now
        # Inform agent to unlock cursor
        if ws_manager.is_agent_connected(event.device_id):
            await ws_manager.send_to_agent(event.device_id, {
                "type": "command",
                "request_id": str(uuid.uuid4()),
                "tool": "edge_control_return_cursor",
                "arguments": {"device_id": event.device_id}
            })
    elif event.event_type == "CURSOR_MOVE":
        state["cursor_x"] = event.norm_x
        state["cursor_y"] = event.norm_y

    return {"status": "ok", "event_type": event.event_type, "is_remote_active": state["is_remote_active"]}
