import json
import datetime
import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.database.session import get_db
from app.models.entities import Task, User
from app.schemas.all_schemas import TaskResponse
from app.auth.dependencies import get_current_user
from app.websocket.manager import ws_manager
from app.permissions.registry import validate_tool_and_permissions


router = APIRouter(prefix="/tasks", tags=["Tasks"])

@router.get("", response_model=List[TaskResponse])
def list_tasks(
    device_id: Optional[str] = None,
    status: Optional[str] = None,
    limit: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    query = db.query(Task)
    if device_id:
        query = query.filter(Task.device_id == device_id)
    if status:
        query = query.filter(Task.status == status.upper())
    tasks = query.order_by(Task.created_at.desc()).limit(limit).all()
    return tasks

@router.get("/{task_id}", response_model=TaskResponse)
def get_task(
    task_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    return task

@router.post("/{task_id}/cancel", response_model=TaskResponse)
def cancel_task(
    task_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    if task.status in ["COMPLETED", "FAILED", "CANCELLED", "REJECTED"]:
        raise HTTPException(status_code=400, detail=f"Task is already in terminal state '{task.status}'.")

    task.status = "CANCELLED"
    task.error_message = f"Cancelled by user '{current_user.username}'"
    task.completed_at = datetime.datetime.now(datetime.timezone.utc)
    db.commit()
    db.refresh(task)
    return task

@router.post("/{task_id}/retry", response_model=TaskResponse)
async def retry_task(
    task_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    args = {}
    if task.arguments_json:
        try:
            args = json.loads(task.arguments_json)
        except Exception:
            args = {}

    policy_check = validate_tool_and_permissions(
        tool_name=task.tool,
        arguments=args,
        is_confirmed=task.is_confirmed
    )
    if not policy_check["valid"]:
        raise HTTPException(status_code=400, detail=policy_check["error"])

    new_task_id = str(uuid.uuid4())
    now = datetime.datetime.now(datetime.timezone.utc)
    new_task = Task(
        id=new_task_id,
        device_id=task.device_id,
        tool=task.tool,
        arguments_json=task.arguments_json,
        permission_level=task.permission_level,
        status="RUNNING",
        requires_confirmation=task.requires_confirmation,
        is_confirmed=task.is_confirmed,
        created_at=now
    )
    db.add(new_task)
    db.commit()

    # Dispatch to agent if connected
    try:
        agent_result = await ws_manager.send_command_to_agent(
            device_id=task.device_id,
            request_id=new_task_id,
            tool=task.tool,
            arguments=args,
            timeout_seconds=30.0
        )
        success = agent_result.get("success", False)
        output = agent_result.get("result")
        err = agent_result.get("error")

        new_task.status = "COMPLETED" if success else "FAILED"
        new_task.result_json = json.dumps(output) if output else None
        new_task.error_message = err
        new_task.completed_at = datetime.datetime.now(datetime.timezone.utc)
        db.commit()
    except Exception as e:
        new_task.status = "FAILED"
        new_task.error_message = str(e)
        new_task.completed_at = datetime.datetime.now(datetime.timezone.utc)
        db.commit()

    db.refresh(new_task)
    return new_task

@router.delete("/cleanup")
def cleanup_tasks(
    keep_count: int = Query(50, ge=10, le=500),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Purge older terminal tasks beyond keep_count to bound storage."""
    terminal_tasks = (
        db.query(Task)
        .filter(Task.status.in_(["COMPLETED", "FAILED", "CANCELLED", "INTERRUPTED", "REJECTED"]))
        .order_by(Task.created_at.desc())
        .all()
    )
    if len(terminal_tasks) > keep_count:
        to_delete = terminal_tasks[keep_count:]
        delete_ids = [t.id for t in to_delete]
        deleted_count = db.query(Task).filter(Task.id.in_(delete_ids)).delete(synchronize_session=False)
        db.commit()
        return {"deleted_count": deleted_count, "retained_count": keep_count}
    return {"deleted_count": 0, "retained_count": len(terminal_tasks)}

