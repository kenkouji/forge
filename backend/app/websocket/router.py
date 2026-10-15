import json
import logging
import uuid
import time
import datetime
from typing import Optional
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query, status
from app.config import settings
from app.websocket.manager import ws_manager
from app.auth.security import decode_access_token
from app.permissions.registry import validate_tool_and_permissions
from app.audit.logger import record_audit_log
from app.database.session import SessionLocal
from app.models.entities import Task

logger = logging.getLogger("remoteforge.ws_router")
ws_router = APIRouter(tags=["WebSockets"])

@ws_router.websocket("/ws/agent")
async def agent_websocket_endpoint(
    websocket: WebSocket,
    token: Optional[str] = Query(None)
):
    # Verify agent authentication token
    if not token or token != settings.AGENT_TOKEN:
        logger.warning("Agent connection rejected: invalid authentication token.")
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Unauthorized Agent Token")
        return

    await websocket.accept()

    device_id: Optional[str] = None
    try:
        while True:
            raw_text = await websocket.receive_text()
            try:
                data = json.loads(raw_text)
            except json.JSONDecodeError:
                logger.error("Received non-JSON message from agent.")
                continue

            msg_type = data.get("type")

            if msg_type == "register":
                device_id = data.get("device_id")
                if not device_id:
                    await websocket.close(code=status.WS_1003_UNSUPPORTED_DATA, reason="Missing device_id")
                    break
                await ws_manager.register_agent(device_id, websocket, data)
                await websocket.send_json({
                    "type": "registered",
                    "device_id": device_id,
                    "status": "authorized"
                })

            elif msg_type == "heartbeat":
                dev = data.get("device_id") or device_id
                if dev:
                    await ws_manager.handle_agent_heartbeat(dev)
                    await websocket.send_json({"type": "heartbeat_ack"})

            elif msg_type == "command_result":
                request_id = data.get("request_id")
                if request_id:
                    ws_manager.resolve_command_result(request_id, data)

            else:
                logger.debug(f"Unhandled agent message type: {msg_type}")

    except WebSocketDisconnect:
        logger.info(f"Agent WebSocket disconnected: device_id={device_id}")
    except Exception as e:
        logger.error(f"Error in agent WebSocket loop: {e}")
    finally:
        if device_id:
            await ws_manager.disconnect_agent(device_id)

@ws_router.websocket("/ws/client")
async def client_websocket_endpoint(
    websocket: WebSocket,
    token: Optional[str] = Query(None)
):
    # Authenticate client JWT
    if not token:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Missing token")
        return
    payload = decode_access_token(token)
    if not payload:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Invalid token")
        return

    username = payload.get("sub", "unknown")
    await ws_manager.connect_client(websocket)

    try:
        while True:
            raw_text = await websocket.receive_text()
            try:
                msg = json.loads(raw_text)
            except json.JSONDecodeError:
                await websocket.send_json({"type": "error", "message": "Invalid JSON payload"})
                continue

            msg_type = msg.get("type")
            if msg_type == "ping":
                await websocket.send_json({"type": "pong", "timestamp": time.time()})
                continue

            if msg_type == "command":
                req_id = msg.get("request_id") or str(uuid.uuid4())
                device_id = msg.get("device_id")
                tool = msg.get("tool")
                args = msg.get("arguments", {})
                confirmed = msg.get("confirmed", False)

                if not device_id or not tool:
                    await websocket.send_json({
                        "type": "command_result",
                        "request_id": req_id,
                        "status": "FAILED",
                        "error": "Missing device_id or tool"
                    })
                    continue

                if not ws_manager.is_agent_online(device_id):
                    await websocket.send_json({
                        "type": "command_result",
                        "request_id": req_id,
                        "device_id": device_id,
                        "tool": tool,
                        "status": "FAILED",
                        "error": f"Device '{device_id}' is offline."
                    })
                    continue

                policy_check = validate_tool_and_permissions(tool, args, confirmed)
                if not policy_check["valid"]:
                    db = SessionLocal()
                    try:
                        record_audit_log(
                            db=db,
                            actor=username,
                            device_id=device_id,
                            action=f"COMMAND_REJECTED_{tool}",
                            tool=tool,
                            arguments=args,
                            permission_level=policy_check["permission_level"],
                            confirmation_status="REQUIRED" if policy_check["requires_confirmation"] else "NOT_REQUIRED",
                            execution_status="REJECTED",
                            details=policy_check["error"]
                        )
                    finally:
                        db.close()

                    await websocket.send_json({
                        "type": "command_result",
                        "request_id": req_id,
                        "device_id": device_id,
                        "tool": tool,
                        "status": "FAILED",
                        "error": policy_check["error"]
                    })
                    continue

                # Record task in DB
                db = SessionLocal()
                t0 = time.perf_counter()
                now = datetime.datetime.now(datetime.timezone.utc)
                task = Task(
                    id=req_id,
                    device_id=device_id,
                    tool=tool,
                    arguments_json=json.dumps(args),
                    permission_level=policy_check["permission_level"],
                    status="RUNNING",
                    requires_confirmation=policy_check["requires_confirmation"],
                    is_confirmed=confirmed,
                    created_at=now
                )
                db.add(task)
                db.commit()

                # Dispatch over agent WebSocket
                try:
                    agent_result = await ws_manager.send_command_to_agent(
                        device_id=device_id,
                        request_id=req_id,
                        tool=tool,
                        arguments=args,
                        timeout_seconds=30.0
                    )
                    backend_dur_ms = round((time.perf_counter() - t0) * 1000, 2)
                    success = agent_result.get("success", False)
                    output = agent_result.get("result")
                    err = agent_result.get("error")
                    mac_exec_ms = agent_result.get("execution_duration_ms")

                    task.status = "COMPLETED" if success else "FAILED"
                    task.result_json = json.dumps(output) if output else None
                    task.error_message = err
                    task.completed_at = datetime.datetime.now(datetime.timezone.utc)
                    db.commit()

                    record_audit_log(
                        db=db,
                        actor=username,
                        device_id=device_id,
                        task_id=req_id,
                        action=f"EXECUTE_{tool}",
                        tool=tool,
                        arguments=args,
                        permission_level=policy_check["permission_level"],
                        confirmation_status="CONFIRMED" if confirmed else "NOT_REQUIRED",
                        execution_status="SUCCESS" if success else "FAILED",
                        details=err if err else "Execution completed via persistent WebSocket."
                    )

                    await websocket.send_json({
                        "type": "command_result",
                        "request_id": req_id,
                        "device_id": device_id,
                        "tool": tool,
                        "permission_level": policy_check["permission_level"],
                        "requires_confirmation": policy_check["requires_confirmation"],
                        "status": "COMPLETED" if success else "FAILED",
                        "result": output,
                        "error": err,
                        "mac_execution_ms": mac_exec_ms,
                        "backend_duration_ms": backend_dur_ms
                    })
                except Exception as e:
                    task.status = "FAILED"
                    task.error_message = str(e)
                    task.completed_at = datetime.datetime.now(datetime.timezone.utc)
                    db.commit()

                    await websocket.send_json({
                        "type": "command_result",
                        "request_id": req_id,
                        "device_id": device_id,
                        "tool": tool,
                        "status": "FAILED",
                        "error": str(e)
                    })
                finally:
                    db.close()
    except WebSocketDisconnect:
        ws_manager.disconnect_client(websocket)
    except Exception:
        ws_manager.disconnect_client(websocket)
