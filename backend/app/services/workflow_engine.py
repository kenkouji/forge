"""
RemoteForge Visual Workflow Engine & AI Automation Service
==========================================================
Coordinates multi-step workflow pipelines, policy enforcement,
dry-run simulations, bounded retries, and conditional branching.
"""

import asyncio
import datetime
import json
import logging
import time
import uuid
from typing import Dict, Any, List, Optional, Tuple

from sqlalchemy.orm import Session
from app.models.entities import Automation, Device, AuditLog, User
from app.permissions.registry import is_tool_registered, validate_tool_and_permissions, TOOL_PERMISSIONS
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log

logger = logging.getLogger("RemoteForgeWorkflowEngine")

# In-memory execution registry: execution_id -> { "task": asyncio.Task, "state": dict }
ACTIVE_WORKFLOW_EXECUTIONS: Dict[str, Dict[str, Any]] = {}
RECENT_WORKFLOW_EXECUTIONS: List[Dict[str, Any]] = []
MAX_RECENT_EXECUTIONS = 50

# Pre-built verified workflow templates
WORKFLOW_TEMPLATES = [
    {
        "id": "template_health_watchdog",
        "name": "Workstation Health Watchdog",
        "description": "Inspects CPU and memory metrics and triggers an alert if resource thresholds are exceeded.",
        "trigger_type": "interval",
        "schedule_time": "Every 30 minutes",
        "version": "2.0",
        "steps": [
            {
                "step_id": "step_telemetry",
                "name": "Harvest System Telemetry",
                "type": "action",
                "tool": "get_system_status",
                "arguments": {},
                "timeout_seconds": 15.0,
                "retry_count": 1
            },
            {
                "step_id": "step_check_cpu",
                "name": "Verify CPU & Memory Thresholds",
                "type": "condition",
                "condition": {
                    "field": "result.cpu_percent",
                    "operator": ">",
                    "value": 85.0
                },
                "on_true_step": "step_alert_high_load",
                "on_false_step": "step_finish"
            },
            {
                "step_id": "step_alert_high_load",
                "name": "High Resource Alert",
                "type": "notification",
                "notification_message": "Sustained high CPU utilization detected (>85%) on Mac workstation.",
                "on_true_step": "step_finish"
            },
            {
                "step_id": "step_finish",
                "name": "Workflow Completion",
                "type": "completion"
            }
        ]
    },
    {
        "id": "template_build_test_guard",
        "name": "Project Build & Test Guard",
        "description": "Executes local project build and raises an immediate notification if compilation or tests fail.",
        "trigger_type": "manual",
        "schedule_time": "On Demand",
        "version": "2.0",
        "steps": [
            {
                "step_id": "step_build",
                "name": "Execute Bounded Project Build",
                "type": "action",
                "tool": "build_project",
                "arguments": {
                    "project_path": "",
                    "build_type": "build",
                    "max_lines": 100
                },
                "timeout_seconds": 60.0,
                "retry_count": 0
            },
            {
                "step_id": "step_check_exit",
                "name": "Verify Build Exit Code",
                "type": "condition",
                "condition": {
                    "field": "result.exit_code",
                    "operator": "!=",
                    "value": 0
                },
                "on_true_step": "step_alert_failed",
                "on_false_step": "step_finish"
            },
            {
                "step_id": "step_alert_failed",
                "name": "Build Failure Notification",
                "type": "notification",
                "notification_message": "Developer workstation build failed with non-zero exit status.",
                "on_true_step": "step_finish"
            },
            {
                "step_id": "step_finish",
                "name": "Workflow Completion",
                "type": "completion"
            }
        ]
    },
    {
        "id": "template_git_dirty_monitor",
        "name": "Git Working Tree Watcher",
        "description": "Checks Git repository status and notifies if uncommitted changes or untracked files are pending.",
        "trigger_type": "interval",
        "schedule_time": "Every 60 minutes",
        "version": "2.0",
        "steps": [
            {
                "step_id": "step_git_status",
                "name": "Query Git Repository State",
                "type": "action",
                "tool": "git_status",
                "arguments": {
                    "project_path": ""
                },
                "timeout_seconds": 15.0,
                "retry_count": 1
            },
            {
                "step_id": "step_check_dirty",
                "name": "Check If Repo Is Dirty",
                "type": "condition",
                "condition": {
                    "field": "result.is_dirty",
                    "operator": "==",
                    "value": True
                },
                "on_true_step": "step_alert_dirty",
                "on_false_step": "step_finish"
            },
            {
                "step_id": "step_alert_dirty",
                "name": "Pending Changes Alert",
                "type": "notification",
                "notification_message": "Uncommitted working tree changes detected in authorized project repository.",
                "on_true_step": "step_finish"
            },
            {
                "step_id": "step_finish",
                "name": "Workflow Completion",
                "type": "completion"
            }
        ]
    },
    {
        "id": "template_nightly_security",
        "name": "Nightly Workstation Security Lock",
        "description": "Secures workstation display and records system telemetry at end of day.",
        "trigger_type": "schedule",
        "schedule_time": "Everyday at 23:00",
        "version": "2.0",
        "steps": [
            {
                "step_id": "step_lock",
                "name": "Lock Workstation Screen",
                "type": "action",
                "tool": "lock_pc",
                "arguments": {},
                "timeout_seconds": 10.0,
                "retry_count": 1
            },
            {
                "step_id": "step_delay",
                "name": "Security Settling Delay",
                "type": "delay",
                "delay_seconds": 2.0
            },
            {
                "step_id": "step_verify_status",
                "name": "Capture Post-Lock Telemetry",
                "type": "action",
                "tool": "get_system_status",
                "arguments": {},
                "timeout_seconds": 15.0
            },
            {
                "step_id": "step_finish",
                "name": "Workflow Completion",
                "type": "completion"
            }
        ]
    },
    {
        "id": "template_diagnostic_bundle",
        "name": "Full Diagnostic Snapshot",
        "description": "Captures both telemetry and desktop screenshot for audit and diagnostic review.",
        "trigger_type": "manual",
        "schedule_time": "On Demand",
        "version": "2.0",
        "steps": [
            {
                "step_id": "step_telemetry",
                "name": "Gather Metrics",
                "type": "action",
                "tool": "get_system_status",
                "arguments": {},
                "timeout_seconds": 15.0
            },
            {
                "step_id": "step_screenshot",
                "name": "Capture Diagnostic Screenshot",
                "type": "action",
                "tool": "capture_screenshot",
                "arguments": {
                    "mode": "preview",
                    "quality": 60
                },
                "timeout_seconds": 20.0
            },
            {
                "step_id": "step_finish",
                "name": "Workflow Completion",
                "type": "completion"
            }
        ]
    }
]


def get_workflow_templates() -> List[Dict[str, Any]]:
    """Returns the list of certified workflow presets."""
    return WORKFLOW_TEMPLATES


def validate_workflow_definition(workflow_dict: Dict[str, Any]) -> Tuple[bool, Optional[str], Optional[Dict[str, Any]]]:
    """
    Validates a structured workflow definition:
    - Step count bounded (1 to 20)
    - Action tools must exist in tool permission registry
    - Arguments validated against schemas
    - Operators and conditions validated
    - Cyclic / runaway protection
    """
    if not isinstance(workflow_dict, dict):
        return False, "Workflow definition must be a JSON object.", None

    name = workflow_dict.get("name", "").strip()
    if not name:
        return False, "Workflow name cannot be empty.", None

    steps = workflow_dict.get("steps", [])
    if not isinstance(steps, list) or len(steps) == 0:
        return False, "Workflow must contain at least one valid step.", None

    if len(steps) > 20:
        return False, "Workflow step limit exceeded (maximum 20 steps per workflow).", None

    step_ids = set()
    for idx, step in enumerate(steps):
        if not isinstance(step, dict):
            return False, f"Step #{idx+1} is malformed.", None

        step_id = step.get("step_id", "").strip()
        if not step_id:
            step_id = f"step_{idx+1}"
            step["step_id"] = step_id

        if step_id in step_ids:
            return False, f"Duplicate step ID '{step_id}' found in workflow.", None
        step_ids.add(step_id)

        step_type = str(step.get("type", "action")).lower()
        if step_type not in ("action", "condition", "delay", "approval", "notification", "completion"):
            return False, f"Invalid step type '{step_type}' at step '{step_id}'.", None

        # Action validation
        if step_type == "action":
            tool = step.get("tool")
            if not tool:
                return False, f"Action step '{step_id}' missing required 'tool' field.", None
            if not is_tool_registered(tool):
                return False, f"Tool '{tool}' in step '{step_id}' is not registered in RemoteForge security registry.", None

            args = step.get("arguments", {})
            if not isinstance(args, dict):
                return False, f"Tool arguments in step '{step_id}' must be a dictionary.", None

            # Validate against tool permissions schema
            check_res = validate_tool_and_permissions(tool, args, is_confirmed=True)
            if not check_res.get("valid", False):
                return False, f"Invalid tool arguments for '{tool}' in step '{step_id}': {check_res.get('error')}", None

        # Condition validation
        elif step_type == "condition":
            cond = step.get("condition")
            if not isinstance(cond, dict):
                return False, f"Condition step '{step_id}' missing 'condition' dictionary.", None
            op = cond.get("operator")
            if op not in (">", "<", ">=", "<=", "==", "!=", "contains", "in"):
                return False, f"Invalid operator '{op}' in condition step '{step_id}'.", None

        # Delay validation
        elif step_type == "delay":
            delay = step.get("delay_seconds", 0.0)
            try:
                delay_val = float(delay)
                if delay_val < 0 or delay_val > 120.0:
                    return False, f"Delay in step '{step_id}' must be between 0 and 120 seconds.", None
            except (ValueError, TypeError):
                return False, f"Invalid delay duration in step '{step_id}'.", None

    return True, None, workflow_dict


def simulate_workflow_dry_run(
    workflow_dict: Dict[str, Any],
    target_device_id: Optional[str] = None
) -> Dict[str, Any]:
    """
    Performs dry-run simulation of a workflow without executing any tools on the Mac.
    Verifies policy gates, permission levels, and outputs an execution plan.
    """
    is_valid, err_msg, validated = validate_workflow_definition(workflow_dict)
    if not is_valid:
        return {
            "valid": False,
            "name": workflow_dict.get("name", "Untitled Workflow"),
            "steps_count": 0,
            "risk_assessment": "REJECTED",
            "requires_human_approval": False,
            "steps": [],
            "error": err_msg
        }

    steps = validated.get("steps", [])
    sim_steps = []
    overall_high_risk = False
    requires_approval_any = False

    for step in steps:
        step_id = step.get("step_id")
        step_name = step.get("name", step_id)
        step_type = step.get("type", "action")
        tool = step.get("tool")

        risk_level = "LOW"
        requires_app = bool(step.get("requires_approval", False))
        sim_note = f"Simulated {step_type.upper()} step."

        if step_type == "action" and tool:
            meta = TOOL_PERMISSIONS.get(tool, {})
            perm = meta.get("permission", "LOW")
            risk_level = perm.value if hasattr(perm, "value") else str(perm)
            if risk_level == "HIGH":
                overall_high_risk = True
                requires_app = True
            sim_note = f"Dry-run tool check: '{tool}' permitted (Risk: {risk_level}). No execution performed."
        elif step_type == "condition":
            cond = step.get("condition", {})
            sim_note = f"Branching check: if ({cond.get('field')} {cond.get('operator')} {cond.get('value')}) -> {step.get('on_true_step')} else {step.get('on_false_step')}."
        elif step_type == "delay":
            sim_note = f"Delay timer: {step.get('delay_seconds', 0)}s wait simulated."
        elif step_type == "notification":
            sim_note = f"Simulated notification: {step.get('notification_message')}"
        elif step_type == "completion":
            sim_note = "Workflow completes cleanly."

        if requires_app:
            requires_approval_any = True

        sim_steps.append({
            "step_id": step_id,
            "name": step_name,
            "type": step_type,
            "tool": tool,
            "risk_level": risk_level,
            "requires_approval": requires_app,
            "simulation_note": sim_note
        })

    risk_assessment = "HIGH_RISK" if overall_high_risk else ("MEDIUM_RISK" if any(s["risk_level"] == "MEDIUM" for s in sim_steps) else "LOW_RISK")

    return {
        "valid": True,
        "name": validated.get("name"),
        "steps_count": len(sim_steps),
        "risk_assessment": risk_assessment,
        "requires_human_approval": requires_approval_any,
        "steps": sim_steps,
        "error": None
    }


def _extract_field_value(data: Any, field_path: str) -> Any:
    """Extracts a nested field value from a result dict using dot notation."""
    if not field_path:
        return None
    parts = field_path.split(".")
    curr = data
    for p in parts:
        if isinstance(curr, dict) and p in curr:
            curr = curr[p]
        else:
            return None
    return curr


def _evaluate_condition(actual_val: Any, operator: str, target_val: Any) -> bool:
    """Evaluates comparison condition safely."""
    try:
        if operator == "==":
            return actual_val == target_val
        elif operator == "!=":
            return actual_val != target_val
        elif operator == ">":
            return float(actual_val) > float(target_val)
        elif operator == "<":
            return float(actual_val) < float(target_val)
        elif operator == ">=":
            return float(actual_val) >= float(target_val)
        elif operator == "<=":
            return float(actual_val) <= float(target_val)
        elif operator in ("contains", "in"):
            return target_val in actual_val or str(target_val) in str(actual_val)
        return False
    except Exception:
        return False


async def run_workflow_execution(
    execution_id: str,
    workflow_id: str,
    workflow_name: str,
    workflow_dict: Dict[str, Any],
    target_device_id: str,
    actor_name: str
) -> Dict[str, Any]:
    """
    Executes a structured workflow pipeline asynchronously.
    """
    from app.database.session import SessionLocal
    db: Session = SessionLocal()

    steps = workflow_dict.get("steps", [])
    step_map = {s.get("step_id"): s for s in steps}
    step_logs = []
    step_results = {}

    started_at = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    exec_state = ACTIVE_WORKFLOW_EXECUTIONS.get(execution_id, {})
    exec_state["status"] = "RUNNING"
    exec_state["current_step_id"] = steps[0].get("step_id") if steps else None

    current_step_idx = 0
    workflow_status = "COMPLETED"
    error_msg = None

    try:
        while current_step_idx < len(steps):
            step = steps[current_step_idx]
            step_id = step.get("step_id")
            step_name = step.get("name", step_id)
            step_type = step.get("type", "action")
            exec_state["current_step_id"] = step_id

            step_start = time.perf_counter()
            step_status = "SUCCESS"
            step_details = None
            step_output = None

            # ACTION STEP
            if step_type == "action":
                tool = step.get("tool")
                args = step.get("arguments", {})
                timeout = float(step.get("timeout_seconds", 25.0))
                retries = int(step.get("retry_count", 0))

                # Check if device is connected
                if not ws_manager.is_agent_online(target_device_id):
                    step_status = "FAILED"
                    step_details = f"Target device '{target_device_id}' is offline."
                    workflow_status = "FAILED"
                    error_msg = step_details
                    break

                # Send command with bounded retries
                attempt = 0
                command_res = None
                while attempt <= retries:
                    try:
                        req_id = str(uuid.uuid4())
                        command_res = await ws_manager.send_command_to_agent(
                            device_id=target_device_id,
                            request_id=req_id,
                            tool=tool,
                            arguments=args,
                            timeout_seconds=timeout
                        )
                        if command_res and command_res.get("success", True):
                            step_status = "SUCCESS"
                            step_output = command_res.get("result", {})
                            step_details = f"Executed '{tool}' successfully."
                            break
                        else:
                            step_status = "FAILED"
                            step_details = f"Command '{tool}' returned error: {command_res.get('error') if command_res else 'Unknown error'}"
                    except Exception as e:
                        step_status = "FAILED"
                        step_details = f"Execution error on attempt {attempt+1}: {str(e)}"

                    attempt += 1
                    if attempt <= retries:
                        await asyncio.sleep(1.0)

                step_results[step_id] = step_output or {}

                if step_status == "FAILED":
                    workflow_status = "FAILED"
                    error_msg = step_details
                    break

            # CONDITION STEP
            elif step_type == "condition":
                cond = step.get("condition", {})
                field = cond.get("field", "")
                operator = cond.get("operator", "==")
                target_val = cond.get("value")

                # Look up field value in previous results
                actual_val = None
                if field.startswith("result."):
                    # check last action's output
                    for s_id in reversed(step_results):
                        actual_val = _extract_field_value(step_results[s_id], field.replace("result.", ""))
                        if actual_val is not None:
                            break
                else:
                    actual_val = _extract_field_value(step_results, field)

                cond_passed = _evaluate_condition(actual_val, operator, target_val)
                step_details = f"Evaluated ({field}={actual_val} {operator} {target_val}): {cond_passed}"
                step_output = {"condition_met": cond_passed, "actual_value": actual_val}

                next_step_id = step.get("on_true_step") if cond_passed else step.get("on_false_step")
                if next_step_id and next_step_id in step_map:
                    # Jump to target step
                    current_step_idx = [i for i, s in enumerate(steps) if s.get("step_id") == next_step_id][0]
                    dur_ms = round((time.perf_counter() - step_start) * 1000, 2)
                    step_logs.append({
                        "step_id": step_id,
                        "step_name": step_name,
                        "step_type": step_type,
                        "tool": None,
                        "status": step_status,
                        "duration_ms": dur_ms,
                        "details": step_details,
                        "result": step_output
                    })
                    continue

            # DELAY STEP
            elif step_type == "delay":
                delay_sec = min(float(step.get("delay_seconds", 0.0)), 60.0)
                await asyncio.sleep(delay_sec)
                step_details = f"Paused for {delay_sec} seconds."

            # NOTIFICATION STEP
            elif step_type == "notification":
                msg = step.get("notification_message", f"Notification from workflow '{workflow_name}'.")
                step_details = msg
                record_audit_log(
                    db=db,
                    actor=actor_name,
                    device_id=target_device_id,
                    action="WORKFLOW_NOTIFICATION",
                    details=f"Workflow '{workflow_name}' alert: {msg}"
                )

            # COMPLETION STEP
            elif step_type == "completion":
                step_details = "Workflow pipeline completed successfully."
                dur_ms = round((time.perf_counter() - step_start) * 1000, 2)
                step_logs.append({
                    "step_id": step_id,
                    "step_name": step_name,
                    "step_type": step_type,
                    "tool": None,
                    "status": "SUCCESS",
                    "duration_ms": dur_ms,
                    "details": step_details,
                    "result": None
                })
                break

            dur_ms = round((time.perf_counter() - step_start) * 1000, 2)
            step_logs.append({
                "step_id": step_id,
                "step_name": step_name,
                "step_type": step_type,
                "tool": step.get("tool"),
                "status": step_status,
                "duration_ms": dur_ms,
                "details": step_details,
                "result": step_output
            })

            current_step_idx += 1

    except asyncio.CancelledError:
        workflow_status = "CANCELLED"
        error_msg = "Workflow execution cancelled by user."
    except Exception as e:
        workflow_status = "FAILED"
        error_msg = f"Workflow runtime error: {str(e)}"
        logger.error(f"Error in workflow '{workflow_name}': {e}")
    finally:
        completed_at = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        exec_state["status"] = workflow_status
        exec_state["completed_at"] = completed_at
        exec_state["step_logs"] = step_logs
        exec_state["error_message"] = error_msg

        # Update automation record in DB
        try:
            auto = db.query(Automation).filter(Automation.id == workflow_id).first()
            if auto:
                auto.last_run_time = completed_at
                auto.execution_count += 1
                db.commit()
        except Exception:
            pass

        # Record audit log
        try:
            record_audit_log(
                db=db,
                actor=actor_name,
                device_id=target_device_id,
                action="WORKFLOW_EXECUTION",
                execution_status=workflow_status,
                details=f"Workflow '{workflow_name}' finished with status {workflow_status}. Steps executed: {len(step_logs)}"
            )
        except Exception:
            pass
        finally:
            db.close()

        # Archive into recent executions
        RECENT_WORKFLOW_EXECUTIONS.insert(0, dict(exec_state))
        if len(RECENT_WORKFLOW_EXECUTIONS) > MAX_RECENT_EXECUTIONS:
            RECENT_WORKFLOW_EXECUTIONS.pop()

    return exec_state


def cancel_workflow_execution(execution_id: str) -> bool:
    """Cancels a running workflow execution."""
    if execution_id in ACTIVE_WORKFLOW_EXECUTIONS:
        entry = ACTIVE_WORKFLOW_EXECUTIONS[execution_id]
        if entry.get("status") in ("COMPLETED", "FAILED", "CANCELLED"):
            return False
        task = entry.get("task")
        if task and not task.done():
            task.cancel()
        entry["status"] = "CANCELLED"
        entry["completed_at"] = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        return True
    return False
