import uuid
import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import Automation, AuditLog, Device, User
from app.schemas.all_schemas import (
    AutomationResponse,
    AutomationCreateRequest,
    AutomationUpdateRequest
)
from app.auth.dependencies import get_current_user
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log

router = APIRouter(prefix="/automations", tags=["Automations"])

@router.get("", response_model=List[AutomationResponse])
@router.get("/", response_model=List[AutomationResponse])
def list_automations(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    return db.query(Automation).order_by(Automation.created_at.desc()).all()

@router.post("", response_model=AutomationResponse, status_code=status.HTTP_201_CREATED)
@router.post("/", response_model=AutomationResponse, status_code=status.HTTP_201_CREATED)
def create_automation(
    req: AutomationCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    auto_id = str(uuid.uuid4())
    next_time = req.next_run_time if hasattr(req, "next_run_time") else f"Scheduled ({req.schedule_time})"
    auto = Automation(
        id=auto_id,
        name=req.name,
        description=req.description or "",
        trigger_type=req.trigger_type or "Schedule (Daily)",
        schedule_time=req.schedule_time or "Everyday at 23:00",
        action_tool=req.action_tool,
        action_arguments_json=req.action_arguments_json or "{}",
        target_device_id=req.target_device_id or "",
        scheduled_epoch=req.scheduled_epoch or 0.0,
        is_one_shot=req.is_one_shot or False,
        status=req.status or "ACTIVE",
        next_run_time=next_time,
        execution_count=0
    )
    db.add(auto)
    db.commit()
    db.refresh(auto)
    return auto

@router.patch("/{automation_id}", response_model=AutomationResponse)
def update_automation(
    automation_id: str,
    req: AutomationUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    auto = db.query(Automation).filter(Automation.id == automation_id).first()
    if not auto:
        raise HTTPException(status_code=404, detail="Automation not found")
    
    if req.name is not None:
        auto.name = req.name
    if req.description is not None:
        auto.description = req.description
    if req.trigger_type is not None:
        auto.trigger_type = req.trigger_type
    if req.schedule_time is not None:
        auto.schedule_time = req.schedule_time
    if req.action_tool is not None:
        auto.action_tool = req.action_tool
    if req.action_arguments_json is not None:
        auto.action_arguments_json = req.action_arguments_json
    if req.target_device_id is not None:
        auto.target_device_id = req.target_device_id
    if req.scheduled_epoch is not None:
        auto.scheduled_epoch = req.scheduled_epoch
    if req.is_one_shot is not None:
        auto.is_one_shot = req.is_one_shot
    if req.status is not None:
        auto.status = req.status
        if req.status == "PAUSED":
            auto.next_run_time = "Paused"
        elif req.status == "COMPLETED":
            auto.next_run_time = "Completed"
        else:
            auto.next_run_time = f"Scheduled ({auto.schedule_time})"
            
    db.commit()
    db.refresh(auto)
    return auto

@router.delete("/{automation_id}")
def discard_automation(
    automation_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Permanently discards an automation, removing its schedule and entry from the system.
    """
    auto = db.query(Automation).filter(Automation.id == automation_id).first()
    if not auto:
        raise HTTPException(status_code=404, detail="Automation not found")
    
    auto_name = auto.name
    db.delete(auto)
    db.commit()

    record_audit_log(
        db=db,
        actor=current_user.username,
        action="AUTOMATION_DISCARDED",
        details=f"Automation '{auto_name}' (ID: {automation_id}) was discarded and its schedule was purged."
    )
    return {"success": True, "message": f"Automation '{auto_name}' schedule removed and discarded."}

@router.post("/{automation_id}/run")
async def run_automation_now(
    automation_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    auto = db.query(Automation).filter(Automation.id == automation_id).first()
    if not auto:
        raise HTTPException(status_code=404, detail="Automation not found")
    
    # Identify device
    device_id = auto.target_device_id
    if not device_id:
        # Pick first online device
        online_dev = db.query(Device).filter(Device.is_online == True).first()
        if online_dev:
            device_id = online_dev.id
        else:
            raise HTTPException(status_code=503, detail="No online device available to execute automation.")
    
    # Execute action tool via WebSocket manager
    req_id = str(uuid.uuid4())
    command_msg = {
        "type": "command",
        "request_id": req_id,
        "tool": auto.action_tool,
        "arguments": {}
    }
    
    now_str = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    auto.last_run_time = now_str
    auto.execution_count += 1
    db.commit()

    res = await ws_manager.send_command_to_agent(device_id, command_msg)
    
    # Record history in audit log
    record_audit_log(
        db=db,
        actor=current_user.username,
        device_id=device_id,
        action="AUTOMATION_EXECUTION",
        tool=auto.action_tool,
        execution_status="SUCCESS" if res.get("status") == "SUCCESS" else "FAILED",
        details=f"Triggered automation '{auto.name}'. Result: {res.get('status')}"
    )

    return {
        "success": True,
        "automation_id": automation_id,
        "automation_name": auto.name,
        "execution_status": res.get("status", "SUCCESS"),
        "result": res.get("result")
    }

@router.get("/history")
def get_automation_history(
    limit: int = 50,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    logs = db.query(AuditLog).filter(
        AuditLog.action.contains("AUTOMATION")
    ).order_by(AuditLog.timestamp.desc()).limit(limit).all()

    return [
        {
            "id": str(l.id),
            "automation_id": l.task_id or "",
            "automation_name": l.action.replace("AUTOMATION_", "").replace("_", " ").title(),
            "timestamp": l.timestamp.strftime("%Y-%m-%d %H:%M:%S UTC"),
            "status": l.execution_status,
            "details": l.details or f"Executed tool: {l.tool or 'N/A'}"
        }
        for l in logs
    ]
