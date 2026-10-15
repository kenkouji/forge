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

logger = logging.getLogger("RemoteForgeScheduler")

async def run_scheduler_worker():
    """
    Background worker that executes scheduled automations and one-shot delayed tasks
    (e.g., 'open youtube and search pewdiepie in 20 mins', 'open chrome and gmail in 20 mins').
    """
    logger.info("RemoteForge Task Scheduler worker started.")
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

                    logger.info(f"Triggering scheduled task '{auto.name}' (tool: {auto.action_tool})")
                    
                    # Resolve target device
                    target_device_id = auto.target_device_id
                    if not target_device_id or not ws_manager.is_agent_online(target_device_id):
                        online_device = db.query(Device).filter(Device.is_online == True).first()
                        if online_device:
                            target_device_id = online_device.id

                    if not target_device_id or not ws_manager.is_agent_online(target_device_id):
                        logger.warning(f"Skipping scheduled task '{auto.name}': Target device is offline.")
                        continue

                    # Parse arguments
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

                    now_str = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
                    auto.last_run_time = now_str
                    auto.execution_count += 1

                    if auto.is_one_shot:
                        auto.status = "COMPLETED"
                        auto.next_run_time = "Completed"
                    else:
                        # Periodic (add 24 hours if daily)
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
