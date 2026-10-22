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
    # Support query param auth and first-message handshake
    if token:
        if token != settings.AGENT_TOKEN:
            logger.warning("Agent connection rejected: invalid authentication token.")
            await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Unauthorized Agent Token")
            return
        await websocket.accept()
    else:
        await websocket.accept()
        import asyncio
        try:
            raw_init = await asyncio.wait_for(websocket.receive_text(), timeout=5.0)
            init_data = json.loads(raw_init)
            if init_data.get("type") == "authenticate":
                token = init_data.get("token")
        except Exception:
            token = None

        if not token or token != settings.AGENT_TOKEN:
            logger.warning("Agent connection rejected: invalid authentication handshake.")
            await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Unauthorized Agent Token")
            return

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

                # If there is pending host sync for Privacy Shield, push it immediately
                from app.models.entities import PrivacyShieldState
                db = SessionLocal()
                try:
                    ps = db.query(PrivacyShieldState).filter(PrivacyShieldState.device_id == device_id).first()
                    if ps:
                        await websocket.send_json({
                            "type": "command",
                            "request_id": str(uuid.uuid4()),
                            "tool": "sync_privacy_shield",
                            "arguments": {
                                "is_lockdown": ps.is_lockdown,
                                "is_screen_paused": ps.is_screen_paused,
                                "is_input_disabled": ps.is_input_disabled,
                                "is_transfers_stopped": ps.is_transfers_stopped
                            }
                        })
                        if ps.pending_host_sync:
                            ps.pending_host_sync = False
                            db.commit()
                finally:
                    db.close()

            elif msg_type == "heartbeat":
                dev = data.get("device_id") or device_id
                if dev:
                    await ws_manager.handle_agent_heartbeat(dev)
                    await websocket.send_json({"type": "heartbeat_ack"})

            elif msg_type == "command_result":
                request_id = data.get("request_id")
                if request_id:
                    ws_manager.resolve_command_result(request_id, data)

            elif msg_type == "clipboard_update":
                from app.services.clipboard_service import clipboard_service
                item_data = data.get("item", {})
                source = data.get("device_id") or "laptop"
                try:
                    saved_item = clipboard_service.add_item(
                        content_type=item_data.get("content_type", "text"),
                        text=item_data.get("text"),
                        image_base64=item_data.get("image_base64"),
                        source_device=source,
                        preview=item_data.get("preview")
                    )
                    await ws_manager.broadcast_to_clients({
                        "type": "clipboard_sync",
                        "item": saved_item
                    })
                except Exception as ce:
                    logger.warning(f"Error processing clipboard_update: {ce}")

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
    # Origin validation for browser client
    origin = websocket.headers.get("origin")
    if origin and settings.CORS_ORIGINS != ["*"] and origin not in settings.CORS_ORIGINS:
        logger.warning(f"Client connection rejected: unauthorized origin {origin}")
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Unauthorized Origin")
        return

    # Support query param auth and first-message handshake
    if token:
        await websocket.accept()
    else:
        await websocket.accept()
        import asyncio
        try:
            raw_init = await asyncio.wait_for(websocket.receive_text(), timeout=5.0)
            init_data = json.loads(raw_init)
            if init_data.get("type") == "authenticate":
                token = init_data.get("token")
        except Exception:
            token = None

    if not token:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Missing token")
        return

    payload = decode_access_token(token)
    if not payload:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION, reason="Invalid token")
        return

    role = payload.get("role", "admin")
    is_guest = (role == "guest")
    guest_session_id = payload.get("session_id") if is_guest else None
    guest_name = payload.get("guest_name", "Guest") if is_guest else None
    username = payload.get("sub", "unknown")
    actor = f"guest:{guest_session_id}:{guest_name}" if is_guest else username

    await ws_manager.connect_client(websocket)

    if is_guest:
        await ws_manager.broadcast_to_clients({
            "type": "guest_connected",
            "session_id": guest_session_id,
            "guest_name": guest_name,
            "timestamp": time.time()
        })

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

                if is_guest:
                    from app.models.entities import GuestSession, PrivacyShieldState
                    check_db = SessionLocal()
                    try:
                        g_sess = check_db.query(GuestSession).filter(GuestSession.id == guest_session_id).first()
                        now_utc = datetime.datetime.now(datetime.timezone.utc)
                        if not g_sess or g_sess.status != "APPROVED":
                            await websocket.send_json({
                                "type": "command_result",
                                "request_id": req_id,
                                "status": "FAILED",
                                "error": "Guest session is not approved or has been revoked."
                            })
                            continue
                        exp = g_sess.expires_at
                        if exp.tzinfo is None:
                            exp = exp.replace(tzinfo=datetime.timezone.utc)
                        if exp < now_utc:
                            await websocket.send_json({
                                "type": "command_result",
                                "request_id": req_id,
                                "status": "FAILED",
                                "error": "Guest session has expired."
                            })
                            continue

                        # Default device_id from session if omitted by guest client
                        if not device_id and g_sess.device_id:
                            device_id = g_sess.device_id

                        # Privacy Shield verification
                        ps = check_db.query(PrivacyShieldState).filter(PrivacyShieldState.device_id == device_id).first()
                        if ps and ps.is_lockdown:
                            await websocket.send_json({
                                "type": "command_result",
                                "request_id": req_id,
                                "status": "FAILED",
                                "error": "Host workstation is currently in Emergency Lockdown."
                            })
                            continue

                        # Restrict guest tools to input control and normalize aliases
                        allowed_guest_tools = [
                            "send_input_event", "remote_input", "mouse_click", "click",
                            "mouse_drag", "drag", "mouse_move", "move", "mouse_down",
                            "mouse_up", "send_keys", "key", "key_tap", "type_text",
                            "scroll", "shortcut", "telestrator_event"
                        ]
                        if tool not in allowed_guest_tools:
                            await websocket.send_json({
                                "type": "command_result",
                                "request_id": req_id,
                                "status": "FAILED",
                                "error": f"Guest permission denied: Guests cannot execute '{tool}'."
                            })
                            continue

                        # Normalize all input aliases to controlled tool "send_input_event"
                        if tool in ["mouse_click", "click"]:
                            if "action" not in args: args["action"] = "click"
                            tool = "send_input_event"
                        elif tool in ["mouse_drag", "drag"]:
                            if "action" not in args: args["action"] = "mouse_drag"
                            tool = "send_input_event"
                        elif tool in ["mouse_move", "move"]:
                            if "action" not in args: args["action"] = "move"
                            tool = "send_input_event"
                        elif tool in ["mouse_down", "down"]:
                            if "action" not in args: args["action"] = "mouse_down"
                            tool = "send_input_event"
                        elif tool in ["mouse_up", "up"]:
                            if "action" not in args: args["action"] = "mouse_up"
                            tool = "send_input_event"
                        elif tool in ["send_keys", "type_text"]:
                            if "action" not in args: args["action"] = "type_text"
                            tool = "send_input_event"
                        elif tool in ["key", "key_tap"]:
                            if "action" not in args: args["action"] = "key"
                            tool = "send_input_event"
                        elif tool == "scroll":
                            if "action" not in args: args["action"] = "scroll"
                            tool = "send_input_event"
                        elif tool == "shortcut":
                            if "action" not in args: args["action"] = "shortcut"
                            tool = "send_input_event"
                        elif tool == "remote_input":
                            tool = "send_input_event"

                        caps = json.loads(g_sess.approved_capabilities_json) if g_sess.approved_capabilities_json else []
                        if "input.control" not in caps:
                            await websocket.send_json({
                                "type": "command_result",
                                "request_id": req_id,
                                "status": "FAILED",
                                "error": "Guest session does not have input.control capability."
                            })
                            continue

                        if ps and ps.is_input_disabled:
                            await websocket.send_json({
                                "type": "command_result",
                                "request_id": req_id,
                                "status": "FAILED",
                                "error": "Guest input is currently disabled by the owner."
                            })
                            continue
                    finally:
                        check_db.close()

                    # Tag actor on arguments for agent and audit
                    args["actor"] = actor

                    # Broadcast co-control activity to owner
                    await ws_manager.broadcast_to_clients({
                        "type": "co_control_activity",
                        "actor": actor,
                        "device_id": device_id,
                        "action": args.get("action", "input"),
                        "timestamp": time.time()
                    })

                if tool == "telestrator_event":
                    await ws_manager.broadcast_to_clients({
                        "type": "telestrator_event",
                        "actor": actor,
                        "device_id": device_id,
                        "action": args.get("action", "laser"),
                        "rel_x": args.get("rel_x"),
                        "rel_y": args.get("rel_y"),
                        "points": args.get("points"),
                        "color": args.get("color", "#BDA0FF"),
                        "timestamp": time.time()
                    })
                    await websocket.send_json({
                        "type": "command_result",
                        "request_id": req_id,
                        "status": "COMPLETED",
                        "result": {"status": "broadcasted"}
                    })
                    continue

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
                        actor=actor,
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
        if is_guest:
            await ws_manager.broadcast_to_clients({
                "type": "guest_disconnected",
                "session_id": guest_session_id,
                "guest_name": guest_name,
                "timestamp": time.time()
            })
    except Exception:
        ws_manager.disconnect_client(websocket)
        if is_guest:
            await ws_manager.broadcast_to_clients({
                "type": "guest_disconnected",
                "session_id": guest_session_id,
                "guest_name": guest_name,
                "timestamp": time.time()
            })


# =====================================================================
# Real-Time Multimedia & Workstation WebSockets
# =====================================================================

AUDIO_CHANNELS: dict = {}

@ws_router.websocket("/ws/audio/{device_id}")
async def audio_websocket_endpoint(websocket: WebSocket, device_id: str):
    """
    High-throughput, ultra-low-latency binary audio channel.
    Relays raw PCM / Opus chunks between host agent and mobile client.
    """
    await websocket.accept()
    if device_id not in AUDIO_CHANNELS:
        AUDIO_CHANNELS[device_id] = set()
    AUDIO_CHANNELS[device_id].add(websocket)
    try:
        while True:
            msg = await websocket.receive()
            if "bytes" in msg and msg["bytes"]:
                raw_bytes = msg["bytes"]
                disconnected = set()
                for peer in list(AUDIO_CHANNELS[device_id]):
                    if peer != websocket:
                        try:
                            await peer.send_bytes(raw_bytes)
                        except Exception:
                            disconnected.add(peer)
                for d in disconnected:
                    AUDIO_CHANNELS[device_id].discard(d)
            elif "text" in msg and msg["text"]:
                raw_text = msg["text"]
                for peer in list(AUDIO_CHANNELS[device_id]):
                    if peer != websocket:
                        try:
                            await peer.send_text(raw_text)
                        except Exception:
                            pass
    except (WebSocketDisconnect, Exception):
        pass
    finally:
        if device_id in AUDIO_CHANNELS:
            AUDIO_CHANNELS[device_id].discard(websocket)


P2P_SIGNAL_ROOMS: dict = {}

@ws_router.websocket("/ws/p2p/signaling/{session_id}")
async def p2p_signaling_endpoint(websocket: WebSocket, session_id: str):
    """
    Full-duplex WebRTC ICE and SDP signaling broker for peer-to-peer NAT traversal.
    """
    await websocket.accept()
    if session_id not in P2P_SIGNAL_ROOMS:
        P2P_SIGNAL_ROOMS[session_id] = set()
    P2P_SIGNAL_ROOMS[session_id].add(websocket)
    try:
        while True:
            raw_text = await websocket.receive_text()
            for peer in list(P2P_SIGNAL_ROOMS[session_id]):
                if peer != websocket:
                    try:
                        await peer.send_text(raw_text)
                    except Exception:
                        pass
    except (WebSocketDisconnect, Exception):
        pass
    finally:
        if session_id in P2P_SIGNAL_ROOMS:
            P2P_SIGNAL_ROOMS[session_id].discard(websocket)


EDGE_CONTROL_ROOMS: dict = {}

@ws_router.websocket("/ws/edge-control/{device_id}")
async def edge_control_endpoint(websocket: WebSocket, device_id: str):
    """
    Ultra-low latency mouse pointer position and keystroke stream for Universal Control.
    """
    await websocket.accept()
    if device_id not in EDGE_CONTROL_ROOMS:
        EDGE_CONTROL_ROOMS[device_id] = set()
    EDGE_CONTROL_ROOMS[device_id].add(websocket)
    try:
        while True:
            raw_text = await websocket.receive_text()
            for peer in list(EDGE_CONTROL_ROOMS[device_id]):
                if peer != websocket:
                    try:
                        await peer.send_text(raw_text)
                    except Exception:
                        pass
    except (WebSocketDisconnect, Exception):
        pass
    finally:
        if device_id in EDGE_CONTROL_ROOMS:
            EDGE_CONTROL_ROOMS[device_id].discard(websocket)


