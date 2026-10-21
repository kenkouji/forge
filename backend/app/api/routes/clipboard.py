import uuid
import datetime
from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel

from app.auth.dependencies import get_current_user
from app.models.entities import User
from app.services.clipboard_service import clipboard_service
from app.websocket.manager import ws_manager

router = APIRouter(prefix="/clipboard", tags=["Universal Clipboard"])

class ClipboardSyncRequest(BaseModel):
    content_type: str  # "text" or "image"
    text: Optional[str] = None
    image_base64: Optional[str] = None
    source_device: Optional[str] = "phone"
    preview: Optional[str] = None

class ClipboardCopyRequest(BaseModel):
    device_id: str
    content_type: str  # "text" or "image"
    text: Optional[str] = None
    image_base64: Optional[str] = None


@router.get("/history")
def get_clipboard_history(
    limit: int = Query(50, ge=1, le=100),
    type: Optional[str] = Query(None),
    current_user: User = Depends(get_current_user)
):
    """Returns chronologically ordered clipboard history items (text & images)."""
    return clipboard_service.get_history(limit=limit, content_type=type)


@router.post("/sync")
async def sync_clipboard_item(
    req: ClipboardSyncRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Called when a new item is copied on phone or laptop.
    Saves to clipboard history and broadcasts real-time sync event.
    """
    item = clipboard_service.add_item(
        content_type=req.content_type,
        text=req.text,
        image_base64=req.image_base64,
        source_device=req.source_device or "phone",
        preview=req.preview
    )

    # Broadcast to connected mobile/web clients
    await ws_manager.broadcast_to_clients({
        "type": "clipboard_sync",
        "item": item
    })

    # If copied from phone, auto-push to online Mac agent!
    if req.source_device == "phone":
        for dev_id in ws_manager.agent_connections.keys():
            try:
                await ws_manager.send_command_to_agent(
                    device_id=dev_id,
                    request_id=str(uuid.uuid4()),
                    tool="set_clipboard",
                    arguments={
                        "content_type": req.content_type,
                        "text": req.text,
                        "image_base64": req.image_base64
                    },
                    timeout_seconds=3.0
                )
            except Exception:
                pass

    return {"success": True, "item": item}


@router.post("/copy")
async def copy_to_target_device(
    req: ClipboardCopyRequest,
    current_user: User = Depends(get_current_user)
):
    """Explicitly copies a clipboard item onto the target Mac or phone."""
    if not ws_manager.is_agent_online(req.device_id):
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Target device '{req.device_id}' is offline."
        )

    res = await ws_manager.send_command_to_agent(
        device_id=req.device_id,
        request_id=str(uuid.uuid4()),
        tool="set_clipboard",
        arguments={
            "content_type": req.content_type,
            "text": req.text,
            "image_base64": req.image_base64
        },
        timeout_seconds=4.0
    )

    # Record into history
    saved = clipboard_service.add_item(
        content_type=req.content_type,
        text=req.text,
        image_base64=req.image_base64,
        source_device="phone"
    )

    return {
        "success": True,
        "device_id": req.device_id,
        "agent_response": res.get("result"),
        "item": saved
    }


@router.delete("/clear")
def clear_clipboard_history(current_user: User = Depends(get_current_user)):
    """Clears all stored clipboard history items."""
    count = clipboard_service.clear_history()
    return {"success": True, "cleared_count": count}


@router.delete("/{item_id}")
def delete_clipboard_item(item_id: str, current_user: User = Depends(get_current_user)):
    """Deletes an individual clipboard history item."""
    ok = clipboard_service.delete_item(item_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Clipboard item not found")
    return {"success": True, "deleted_id": item_id}
