import asyncio
import datetime
import json
import logging
import time
import uuid

from sqlalchemy.orm import Session
from app.database.session import SessionLocal
from app.models.entities import Automation, Device, AuditLog
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log
from app.services.workflow_engine import run_workflow_execution, ACTIVE_WORKFLOW_EXECUTIONS

logger = logging.getLogger("RemoteForgeScheduler")

async def run_scheduler_worker():
    """
    Background worker that executes scheduled automations, multi-step workflows,
    and one-shot delayed tasks across process lifetimes.
    """
    logger.info("RemoteForge Task & Workflow Scheduler worker started.")
    while True:
        try:
            await asyncio.sleep(4)
            db: Session = SessionLocal()
            try:
                now_epoch = time.time()
                active_automations = db.query(Automation).filter(Automation.status == "ACTIVE").all()

                for auto in active_automations:
                    # Check 1: One-shot or epoch-scheduled task
                    is_ready = False
                    if auto.scheduled_epoch and auto.scheduled_epoch > 0:
                        if now_epoch >= auto.scheduled_epoch:
                            is_ready = True

                    if not is_ready:
                        continue

                    logger.info(f"Triggering scheduled automation '{auto.name}' (tool: {auto.action_tool})")

                    # Resolve target device
                    target_device_id = auto.target_device_id
                    if not target_device_id or not ws_manager.is_agent_online(target_device_id):
                        online_device = db.query(Device).filter(Device.is_online == True).first()
                        if online_device and ws_manager.is_agent_online(online_device.id):
                            target_device_id = online_device.id

                    now_str = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

                    if not target_device_id or not ws_manager.is_agent_online(target_device_id):
                        logger.warning(f"Skipping scheduled automation '{auto.name}': Target device is offline.")
                        # Advance epoch to avoid tight loop while device is offline
                        if auto.is_one_shot:
                            auto.status = "COMPLETED"
                            auto.next_run_time = "Completed (Device Offline)"
                        else:
                            auto.scheduled_epoch += 86400
                            auto.next_run_time = f"Scheduled ({auto.schedule_time}) [Last missed: Device Offline]"
                        auto.last_run_time = f"Missed ({now_str} - Device Offline)"
                        record_audit_log(
                            db=db,
                            actor="system_scheduler",
                            device_id=target_device_id or "unassigned",
                            action="SCHEDULED_TASK_SKIPPED",
                            tool=auto.action_tool,
                            execution_status="DEVICE_OFFLINE",
                            details=f"Automation '{auto.name}' scheduled trigger skipped because Mac workstation was offline."
                        )
                        db.commit()
                        continue

                    # Multi-step Workflow Execution Pipeline
                    if auto.workflow_definition_json:
                        try:
                            wf_data = json.loads(auto.workflow_definition_json)
                        except Exception:
                            wf_data = None

                        if wf_data and wf_data.get("steps"):
                            exec_id = f"wf_sched_{uuid.uuid4().hex[:12]}"
                            ACTIVE_WORKFLOW_EXECUTIONS[exec_id] = {
                                "execution_id": exec_id,
                                "workflow_id": auto.id,
                                "workflow_name": auto.name,
                                "target_device_id": target_device_id,
                                "status": "RUNNING",
                                "started_at": now_str,
                                "step_logs": []
                            }

                            asyncio.create_task(run_workflow_execution(
                                execution_id=exec_id,
                                workflow_id=auto.id,
                                workflow_name=auto.name,
                                workflow_dict=wf_data,
                                target_device_id=target_device_id,
                                actor_name="system_scheduler"
                            ))

                            if auto.is_one_shot:
                                auto.status = "COMPLETED"
                                auto.next_run_time = "Completed"
                            else:
                                auto.scheduled_epoch += 86400
                                auto.next_run_time = f"Scheduled ({auto.schedule_time})"
                            db.commit()
                            continue

                    # Legacy Single-Action Execution
                    arguments = {}
                    if auto.action_arguments_json:
                        try:
                            arguments = json.loads(auto.action_arguments_json)
                        except Exception:
                            arguments = {}

                    req_id = str(uuid.uuid4())
                    try:
                        res = await ws_manager.send_command_to_agent(
                            device_id=target_device_id,
                            request_id=req_id,
                            tool=auto.action_tool,
                            arguments=arguments,
                            timeout_seconds=25.0
                        )
                        exec_status = "SUCCESS" if res.get("success", True) else "FAILED"
                        logger.info(f"Scheduled task '{auto.name}' executed with status: {exec_status}")
                    except Exception as e:
                        exec_status = "FAILED"
                        logger.error(f"Error executing scheduled task '{auto.name}': {e}")

                    auto.last_run_time = now_str
                    auto.execution_count += 1

                    if auto.is_one_shot:
                        auto.status = "COMPLETED"
                        auto.next_run_time = "Completed"
                    else:
                        auto.scheduled_epoch += 86400
                        auto.next_run_time = f"Scheduled ({auto.schedule_time})"

                    record_audit_log(
                        db=db,
                        actor="system_scheduler",
                        device_id=target_device_id,
                        action="SCHEDULED_TASK_EXECUTION",
                        tool=auto.action_tool,
                        arguments=arguments,
                        execution_status=exec_status,
                        details=f"Automated scheduler executed '{auto.name}' on device '{target_device_id}'."
                    )
                    db.commit()
            finally:
                db.close()
        except asyncio.CancelledError:
            break
        except Exception as e:
            logger.error(f"Scheduler loop error: {e}")
