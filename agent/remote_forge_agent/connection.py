import asyncio
import json
import logging
import platform
import sys
from typing import Dict, Any, Optional
import websockets

from agent.remote_forge_agent.commands.dispatcher import CommandDispatcher

logger = logging.getLogger("remoteforge.agent.connection")

class AgentConnection:
    def __init__(
        self,
        server_ws_url: str,
        agent_token: str,
        device_id: str,
        device_name: str,
        dispatcher: CommandDispatcher,
        heartbeat_interval: float = 10.0
    ):
        self.server_ws_url = server_ws_url.rstrip("/")
        self.agent_token = agent_token
        self.device_id = device_id
        self.device_name = device_name
        self.dispatcher = dispatcher
        self.heartbeat_interval = heartbeat_interval
        self._running = False
        self._ws: Optional[websockets.WebSocketClientProtocol] = None
        self._loop: Optional[asyncio.AbstractEventLoop] = None

    def _get_registration_payload(self) -> Dict[str, Any]:
        os_name = platform.system()
        if os_name == "Darwin":
            os_name = "macOS"
        os_version = platform.mac_ver()[0] if sys.platform == "darwin" else platform.release()

        return {
            "type": "register",
            "device_id": self.device_id,
            "device_name": self.device_name,
            "os_name": os_name,
            "os_version": os_version,
            "agent_version": "0.1.0"
        }

    def _is_ws_open(self) -> bool:
        if not self._ws:
            return False
        if hasattr(self._ws, "state"):
            return getattr(self._ws.state, "name", "") == "OPEN"
        if hasattr(self._ws, "closed"):
            return not self._ws.closed
        return getattr(self._ws, "open", False)

    def send_message_sync(self, payload: Dict[str, Any]):
        if self._ws and self._is_ws_open() and self._loop and self._loop.is_running():
            try:
                asyncio.run_coroutine_threadsafe(self._ws.send(json.dumps(payload)), self._loop)
            except Exception as e:
                logger.debug(f"Error sending message from agent: {e}")

    async def _heartbeat_loop(self):
        while self._running and self._is_ws_open():
            try:
                await asyncio.sleep(self.heartbeat_interval)
                heartbeat_msg = {
                    "type": "heartbeat",
                    "device_id": self.device_id
                }
                await self._ws.send(json.dumps(heartbeat_msg))
                logger.debug(f"Heartbeat sent for device '{self.device_id}'.")
            except Exception as e:
                logger.warning(f"Error sending heartbeat: {e}")
                break

    async def _handle_incoming_command(self, raw_message: str):
        try:
            data = json.loads(raw_message)
        except json.JSONDecodeError:
            logger.error("Failed to parse incoming WebSocket message as JSON.")
            return

        msg_type = data.get("type")
        if msg_type == "registered":
            logger.info(f"Agent successfully registered on server: {data}")
        elif msg_type == "heartbeat_ack":
            logger.debug("Received heartbeat ack from server.")
        elif msg_type == "command":
            request_id = data.get("request_id")
            tool = data.get("tool")
            arguments = data.get("arguments", {})
            logger.info(f"Executing tool '{tool}' for request_id '{request_id}'")

            # Dispatch command execution
            exec_result = await self.dispatcher.dispatch(tool, arguments)

            # Return command_result to server
            response_payload = {
                "type": "command_result",
                "request_id": request_id,
                "device_id": self.device_id,
                "tool": tool,
                "success": exec_result.get("success", False),
                "result": exec_result.get("result"),
                "error": exec_result.get("error"),
                "execution_duration_ms": exec_result.get("execution_duration_ms", 0.0)
            }
            if self._is_ws_open():
                await self._ws.send(json.dumps(response_payload))
                logger.info(f"Dispatched result for request_id '{request_id}'")

    async def _clipboard_monitor_loop(self):
        last_change_count = None
        last_item_hash = None
        import ctypes, ctypes.util, hashlib

        change_count_fn = None
        if sys.platform == "darwin":
            try:
                ctypes.cdll.LoadLibrary('/System/Library/Frameworks/AppKit.framework/AppKit')
                objc = ctypes.cdll.LoadLibrary(ctypes.util.find_library('objc'))
                objc.objc_getClass.restype = ctypes.c_void_p
                objc.objc_getClass.argtypes = [ctypes.c_char_p]
                objc.sel_registerName.restype = ctypes.c_void_p
                objc.sel_registerName.argtypes = [ctypes.c_char_p]
                
                msgSend = objc.objc_msgSend
                msgSend.restype = ctypes.c_void_p
                msgSend.argtypes = [ctypes.c_void_p, ctypes.c_void_p]

                msgSend_long = ctypes.cast(objc.objc_msgSend, ctypes.CFUNCTYPE(ctypes.c_long, ctypes.c_void_p, ctypes.c_void_p))

                NSPasteboard = objc.objc_getClass(b'NSPasteboard')
                generalPasteboard_sel = objc.sel_registerName(b'generalPasteboard')
                pb = msgSend(NSPasteboard, generalPasteboard_sel)
                changeCount_sel = objc.sel_registerName(b'changeCount')

                def get_cc():
                    return msgSend_long(pb, changeCount_sel)

                change_count_fn = get_cc
            except Exception as e:
                logger.warning(f"NSPasteboard hook error: {e}")

        while self._running and self._is_ws_open():
            try:
                await asyncio.sleep(0.8)
                if change_count_fn:
                    cc = change_count_fn()
                    if last_change_count is not None and cc == last_change_count:
                        continue
                    last_change_count = cc

                # Read clipboard content via dispatcher
                clip_data = self.dispatcher._handle_get_clipboard({})
                if not clip_data or not clip_data.get("content_type"):
                    continue

                c_type = clip_data.get("content_type")
                seed = clip_data.get("text", "") if c_type == "text" else (clip_data.get("image_base64", "")[:200])
                if not seed:
                    continue

                curr_hash = hashlib.sha256(seed.encode("utf-8")).hexdigest()
                if curr_hash == last_item_hash:
                    continue
                last_item_hash = curr_hash

                # Push clipboard update to backend
                update_msg = {
                    "type": "clipboard_update",
                    "device_id": self.device_id,
                    "item": clip_data
                }
                if self._is_ws_open():
                    await self._ws.send(json.dumps(update_msg))
                    logger.info(f"Synchronized Mac clipboard to server: {clip_data.get('preview')}")
            except Exception as e:
                logger.debug(f"Clipboard loop error: {e}")

    async def run(self):
        self._running = True
        self._loop = asyncio.get_running_loop()
        backoff = 2.0
        max_backoff = 30.0

        # Construct full ws url with auth token query param
        ws_endpoint = f"{self.server_ws_url}/ws/agent?token={self.agent_token}"

        while self._running:
            try:
                logger.info(f"Connecting to RemoteForge Server at {self.server_ws_url}...")
                async with websockets.connect(ws_endpoint) as ws:
                    self._ws = ws
                    backoff = 2.0
                    logger.info("Connected to RemoteForge server WebSocket. Identifying device...")

                    # Send registration message
                    await ws.send(json.dumps(self._get_registration_payload()))

                    # Start heartbeat and clipboard sync tasks
                    heartbeat_task = asyncio.create_task(self._heartbeat_loop())
                    clipboard_task = asyncio.create_task(self._clipboard_monitor_loop())

                    # Listen for incoming commands
                    try:
                        async for message in ws:
                            await self._handle_incoming_command(message)
                    finally:
                        heartbeat_task.cancel()
                        clipboard_task.cancel()

            except (websockets.ConnectionClosed, ConnectionRefusedError, OSError) as e:
                logger.warning(f"Connection lost or could not connect: {e}")
            except Exception as e:
                logger.error(f"Unexpected connection error: {e}")

            if self._running:
                logger.info(f"Reconnecting in {backoff:.1f} seconds...")
                await asyncio.sleep(backoff)
                backoff = min(backoff * 2, max_backoff)

    def stop(self):
        self._running = False
        if self._ws:
            asyncio.create_task(self._ws.close())
