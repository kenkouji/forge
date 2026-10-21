import uuid
import datetime
import json
from typing import List, Optional, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import Automation, AuditLog, Device, User
from app.schemas.all_schemas import (
    AutomationResponse,
    AutomationCreateRequest,
    AutomationUpdateRequest,
    WorkflowDryRunResponse,
    WorkflowExecutionResponse
)
from app.auth.dependencies import get_current_user
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log
from app.services.workflow_engine import (
    get_workflow_templates,
    validate_workflow_definition,
    simulate_workflow_dry_run,
    run_workflow_execution,
    cancel_workflow_execution,
    ACTIVE_WORKFLOW_EXECUTIONS,
    RECENT_WORKFLOW_EXECUTIONS
)

router = APIRouter(prefix="/automations", tags=["Automations"])


def _hydrate_automation_response(auto: Automation) -> AutomationResponse:
    wf_dict = None
    if auto.workflow_definition_json:
        try:
            wf_dict = json.loads(auto.workflow_definition_json)
        except Exception:
            wf_dict = None

    return AutomationResponse(
        id=auto.id,
        name=auto.name,
        description=auto.description or "",
        trigger_type=auto.trigger_type or "Schedule (Daily)",
        schedule_time=auto.schedule_time or "Everyday at 23:00",
        action_tool=auto.action_tool or "lock_pc",
        action_arguments_json=auto.action_arguments_json or "{}",
        target_device_id=auto.target_device_id or "",
        status=auto.status or "ACTIVE",
        last_run_time=auto.last_run_time,
        next_run_time=auto.next_run_time,
        scheduled_epoch=auto.scheduled_epoch or 0.0,
        is_one_shot=auto.is_one_shot or False,
        execution_count=auto.execution_count or 0,
        workflow_definition_json=auto.workflow_definition_json,
        workflow_definition=wf_dict,
        created_at=auto.created_at
    )


@router.get("/templates")
def list_workflow_templates(current_user: User = Depends(get_current_user)):
    """
    Returns certified, pre-configured workflow templates ready for one-tap deployment.
    """
    return get_workflow_templates()


@router.post("/dry-run", response_model=WorkflowDryRunResponse)
def dry_run_adhoc_workflow(
    workflow_data: Dict[str, Any],
    current_user: User = Depends(get_current_user)
):
    """
    Simulates execution and verifies policy boundaries for an unpersisted workflow definition.
    """
    res = simulate_workflow_dry_run(workflow_data)
    return res


@router.post("/{automation_id}/dry-run", response_model=WorkflowDryRunResponse)
def dry_run_saved_automation(
    automation_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Simulates execution and policy boundaries for a saved automation workflow.
    """
    auto = db.query(Automation).filter(Automation.id == automation_id).first()
    if not auto:
        raise HTTPException(status_code=404, detail="Automation not found")

    if auto.workflow_definition_json:
        try:
            wf_data = json.loads(auto.workflow_definition_json)
            return simulate_workflow_dry_run(wf_data, auto.target_device_id)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Invalid workflow definition: {e}")

    # Fallback simulation for single-action legacy automation
    single_action_wf = {
        "name": auto.name,
        "steps": [
            {
                "step_id": "step_1",
                "name": f"Execute {auto.action_tool}",
                "type": "action",
                "tool": auto.action_tool,
                "arguments": json.loads(auto.action_arguments_json or "{}")
            }
        ]
    }
    return simulate_workflow_dry_run(single_action_wf, auto.target_device_id)


@router.get("", response_model=List[AutomationResponse])
@router.get("/", response_model=List[AutomationResponse])
def list_automations(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    items = db.query(Automation).order_by(Automation.created_at.desc()).all()
    return [_hydrate_automation_response(a) for a in items]


@router.post("", response_model=AutomationResponse, status_code=status.HTTP_201_CREATED)
@router.post("/", response_model=AutomationResponse, status_code=status.HTTP_201_CREATED)
def create_automation(
    req: AutomationCreateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    auto_id = str(uuid.uuid4())
    wf_json = req.workflow_definition_json

    # If workflow_definition provided as object, validate and serialize
    if req.workflow_definition:
        wf_dict = req.workflow_definition.model_dump()
        is_valid, err_msg, validated = validate_workflow_definition(wf_dict)
        if not is_valid:
            raise HTTPException(status_code=400, detail=f"Invalid workflow definition: {err_msg}")
        wf_json = json.dumps(validated)
    elif wf_json:
        try:
            wf_dict = json.loads(wf_json)
            is_valid, err_msg, validated = validate_workflow_definition(wf_dict)
            if not is_valid:
                raise HTTPException(status_code=400, detail=f"Invalid workflow definition: {err_msg}")
            wf_json = json.dumps(validated)
        except json.JSONDecodeError:
            raise HTTPException(status_code=400, detail="workflow_definition_json must be valid JSON.")

    next_time = f"Scheduled ({req.schedule_time})"
    auto = Automation(
        id=auto_id,
        name=req.name,
        description=req.description or "",
        trigger_type=req.trigger_type or "Schedule (Daily)",
        schedule_time=req.schedule_time or "Everyday at 23:00",
        action_tool=req.action_tool or "lock_pc",
        action_arguments_json=req.action_arguments_json or "{}",
        target_device_id=req.target_device_id or "",
        scheduled_epoch=req.scheduled_epoch or 0.0,
        is_one_shot=req.is_one_shot or False,
        status=req.status or "ACTIVE",
        next_run_time=next_time,
        execution_count=0,
        workflow_definition_json=wf_json
    )
    db.add(auto)
    db.commit()
    db.refresh(auto)
    return _hydrate_automation_response(auto)


@router.get("/executions")
def list_executions(current_user: User = Depends(get_current_user)):
    """
    Returns active and recent workflow execution runs with per-step execution logs.
    """
    return RECENT_WORKFLOW_EXECUTIONS


@router.get("/history")
def get_automation_history(
    limit: int = 50,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    logs = db.query(AuditLog).filter(
        AuditLog.action.contains("AUTOMATION") | AuditLog.action.contains("WORKFLOW")
    ).order_by(AuditLog.timestamp.desc()).limit(limit).all()

    return [
        {
            "id": str(l.id),
            "automation_id": l.task_id or "",
            "automation_name": l.action.replace("AUTOMATION_", "").replace("WORKFLOW_", "").replace("_", " ").title(),
            "timestamp": l.timestamp.strftime("%Y-%m-%d %H:%M:%S UTC"),
            "status": l.execution_status,
            "details": l.details or f"Executed tool: {l.tool or 'N/A'}"
        }
        for l in logs
    ]


@router.get("/{automation_id}", response_model=AutomationResponse)
def get_automation(
    automation_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    auto = db.query(Automation).filter(Automation.id == automation_id).first()
    if not auto:
        raise HTTPException(status_code=404, detail="Automation not found")
    return _hydrate_automation_response(auto)


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
    if req.workflow_definition_json is not None:
        try:
            wf_dict = json.loads(req.workflow_definition_json)
            is_valid, err_msg, validated = validate_workflow_definition(wf_dict)
            if not is_valid:
                raise HTTPException(status_code=400, detail=f"Invalid workflow definition: {err_msg}")
            auto.workflow_definition_json = json.dumps(validated)
        except json.JSONDecodeError:
            raise HTTPException(status_code=400, detail="workflow_definition_json must be valid JSON.")
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
    return _hydrate_automation_response(auto)


@router.delete("/{automation_id}")
def discard_automation(
    automation_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
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
    if not device_id or not ws_manager.is_agent_online(device_id):
        online_dev = db.query(Device).filter(Device.is_online == True).first()
        if online_dev and ws_manager.is_agent_online(online_dev.id):
            device_id = online_dev.id
        else:
            raise HTTPException(status_code=503, detail="No online Mac workstation available to execute automation.")

    # Multi-step Workflow Execution Pipeline
    if auto.workflow_definition_json:
        try:
            wf_data = json.loads(auto.workflow_definition_json)
        except Exception:
            wf_data = None

        if wf_data and wf_data.get("steps"):
            exec_id = f"wf_exec_{uuid.uuid4().hex[:12]}"
            ACTIVE_WORKFLOW_EXECUTIONS[exec_id] = {
                "execution_id": exec_id,
                "workflow_id": auto.id,
                "workflow_name": auto.name,
                "target_device_id": device_id,
                "status": "RUNNING",
                "started_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC"),
                "step_logs": []
            }

            res = await run_workflow_execution(
                execution_id=exec_id,
                workflow_id=auto.id,
                workflow_name=auto.name,
                workflow_dict=wf_data,
                target_device_id=device_id,
                actor_name=current_user.username
            )
            return {
                "success": res.get("status") == "COMPLETED",
                "execution_id": exec_id,
                "automation_id": automation_id,
                "automation_name": auto.name,
                "execution_status": res.get("status", "COMPLETED"),
                "step_logs": res.get("step_logs", []),
                "error_message": res.get("error_message")
            }

    # Legacy Single-Action Execution
    req_id = str(uuid.uuid4())
    arguments = {}
    if auto.action_arguments_json:
        try:
            arguments = json.loads(auto.action_arguments_json)
        except Exception:
            arguments = {}

    now_str = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    auto.last_run_time = now_str
    auto.execution_count += 1
    db.commit()

    res = await ws_manager.send_command_to_agent(
        device_id=device_id,
        request_id=req_id,
        tool=auto.action_tool,
        arguments=arguments,
        timeout_seconds=25.0
    )

    exec_status = "SUCCESS" if res.get("success", True) else "FAILED"
    record_audit_log(
        db=db,
        actor=current_user.username,
        device_id=device_id,
        action="AUTOMATION_EXECUTION",
        tool=auto.action_tool,
        execution_status=exec_status,
        details=f"Triggered automation '{auto.name}'. Result: {exec_status}"
    )

    return {
        "success": exec_status == "SUCCESS",
        "automation_id": automation_id,
        "automation_name": auto.name,
        "execution_status": exec_status,
        "result": res.get("result")
    }


@router.post("/executions/{execution_id}/cancel")
def cancel_execution(
    execution_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    Cancels an active workflow execution.
    """
    cancelled = cancel_workflow_execution(execution_id)
    if not cancelled:
        raise HTTPException(status_code=404, detail="Active execution not found or already completed.")
    return {"success": True, "message": f"Execution '{execution_id}' cancelled."}

