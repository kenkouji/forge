import uuid
import json
import datetime
import time
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database.session import get_db
from app.models.entities import User, Device, Task, AllowedDirectory
from app.schemas.all_schemas import (
    DeviceResponse,
    CommandRequest,
    CommandResponse,
    AllowedDirectoryResponse,
    AddDirectoryRequest
)
from app.auth.dependencies import get_current_user
from app.permissions.registry import validate_tool_and_permissions
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log

router = APIRouter(prefix="/devices", tags=["Devices"])

@router.get("", response_model=List[DeviceResponse])
@router.get("/", response_model=List[DeviceResponse])
def list_devices(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    devices = db.query(Device).all()
    # Synchronize live connection status with memory state
    for d in devices:
        d.is_online = ws_manager.is_agent_online(d.id)
    return devices

@router.get("/{device_id}", response_model=DeviceResponse)
def get_device(device_id: str, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    device = db.query(Device).filter(Device.id == device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail=f"Device '{device_id}' not found.")
    device.is_online = ws_manager.is_agent_online(device.id)
    return device

@router.post("/{device_id}/commands", response_model=CommandResponse)
async def dispatch_device_command(
    device_id: str,
    req: CommandRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    device = db.query(Device).filter(Device.id == device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail=f"Device '{device_id}' not found.")

    if not ws_manager.is_agent_online(device_id):
        # Calculate last seen
        last_seen_str = device.last_seen.strftime("%Y-%m-%d %H:%M:%S UTC") if device.last_seen else "unknown"
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"PC is offline. Last seen {last_seen_str}."
        )

    # 1. Validate tool and permissions
    policy_check = validate_tool_and_permissions(req.tool, req.arguments, req.confirmed)
    if not policy_check["valid"]:
        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=device_id,
            action=f"COMMAND_REJECTED_{req.tool}",
            tool=req.tool,
            arguments=req.arguments,
            permission_level=policy_check["permission_level"],
            confirmation_status="REQUIRED" if policy_check["requires_confirmation"] else "NOT_REQUIRED",
            execution_status="REJECTED",
            details=policy_check["error"]
        )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=policy_check["error"]
        )

    request_id = str(uuid.uuid4())
    now = datetime.datetime.now(datetime.timezone.utc)

    # 2. Record Task in DB
    task = Task(
        id=request_id,
        device_id=device_id,
        tool=req.tool,
        arguments_json=json.dumps(req.arguments),
        permission_level=policy_check["permission_level"],
        status="RUNNING",
        requires_confirmation=policy_check["requires_confirmation"],
        is_confirmed=req.confirmed,
        created_at=now
    )
    db.add(task)
    db.commit()

    # 3. Dispatch to Agent over WebSocket
    t0 = time.perf_counter()
    try:
        agent_result = await ws_manager.send_command_to_agent(
            device_id=device_id,
            request_id=request_id,
            tool=req.tool,
            arguments=req.arguments,
            timeout_seconds=30.0
        )
        backend_dur_ms = round((time.perf_counter() - t0) * 1000, 2)

        success = agent_result.get("success", False)
        output = agent_result.get("result")
        err = agent_result.get("error")
        mac_exec_ms = agent_result.get("execution_duration_ms")

        # Update Task
        task.status = "COMPLETED" if success else "FAILED"
        task.result_json = json.dumps(output) if output else None
        task.error_message = err
        task.completed_at = datetime.datetime.now(datetime.timezone.utc)
        db.commit()

        # Audit Log
        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=device_id,
            task_id=request_id,
            action=f"EXECUTE_{req.tool}",
            tool=req.tool,
            arguments=req.arguments,
            permission_level=policy_check["permission_level"],
            confirmation_status="CONFIRMED" if req.confirmed else "NOT_REQUIRED",
            execution_status="SUCCESS" if success else "FAILED",
            details=err if err else "Execution completed."
        )

        return CommandResponse(
            request_id=request_id,
            device_id=device_id,
            tool=req.tool,
            permission_level=policy_check["permission_level"],
            requires_confirmation=policy_check["requires_confirmation"],
            status="COMPLETED" if success else "FAILED",
            result=output,
            error=err,
            mac_execution_ms=mac_exec_ms,
            backend_duration_ms=backend_dur_ms
        )

    except (ConnectionError, TimeoutError) as e:
        task.status = "FAILED"
        task.error_message = str(e)
        task.completed_at = datetime.datetime.now(datetime.timezone.utc)
        db.commit()

        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=device_id,
            task_id=request_id,
            action=f"EXECUTE_{req.tool}",
            tool=req.tool,
            arguments=req.arguments,
            permission_level=policy_check["permission_level"],
            confirmation_status="CONFIRMED" if req.confirmed else "NOT_REQUIRED",
            execution_status="TIMEOUT_OR_DISCONNECT",
            details=str(e)
        )
        raise HTTPException(status_code=504, detail=str(e))

@router.get("/{device_id}/allowed-directories", response_model=List[AllowedDirectoryResponse])
def get_allowed_directories(
    device_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    dirs = db.query(AllowedDirectory).filter(
        AllowedDirectory.device_id == device_id,
        AllowedDirectory.is_active == True
    ).all()
    return dirs

@router.post("/{device_id}/allowed-directories", response_model=AllowedDirectoryResponse)
def add_allowed_directory(
    device_id: str,
    req: AddDirectoryRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    device = db.query(Device).filter(Device.id == device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    new_dir = AllowedDirectory(
        device_id=device_id,
        canonical_path=req.canonical_path,
        display_name=req.display_name,
        is_active=True
    )
    db.add(new_dir)
    db.commit()
    db.refresh(new_dir)
    return new_dir
