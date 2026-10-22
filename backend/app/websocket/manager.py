import asyncio
import datetime
import json
import logging
from typing import Dict, Any, Optional, Set
from fastapi import WebSocket
from sqlalchemy.orm import Session
from app.database.session import SessionLocal
from app.models.entities import Device, AuditLog

from starlette.websockets import WebSocketState

logger = logging.getLogger("remoteforge.websocket")

class ConnectionManager:
    def __init__(self):
        # device_id -> WebSocket connection
        self.agent_connections: Dict[str, WebSocket] = {}
        # device_id -> device info cache
        self.device_info: Dict[str, Dict[str, Any]] = {}
        # Connected browser/mobile clients
        self.client_connections: Set[WebSocket] = set()
        # In-flight command response futures: request_id -> asyncio.Future
        self.pending_commands: Dict[str, asyncio.Future] = {}

    async def connect_client(self, websocket: WebSocket):
        if websocket.client_state != WebSocketState.CONNECTED:
            await websocket.accept()
        self.client_connections.add(websocket)
        logger.info(f"Client connected. Total clients: {len(self.client_connections)}")

    def disconnect_client(self, websocket: WebSocket):
        self.client_connections.discard(websocket)
        logger.info(f"Client disconnected. Remaining clients: {len(self.client_connections)}")

    async def broadcast_to_clients(self, message: Dict[str, Any]):
        disconnected = set()
        for client in self.client_connections:
            try:
                await client.send_json(message)
            except Exception:
                disconnected.add(client)
        for client in disconnected:
            self.client_connections.discard(client)

    async def register_agent(self, device_id: str, websocket: WebSocket, metadata: Dict[str, Any]):
        self.agent_connections[device_id] = websocket
        self.device_info[device_id] = metadata

        # Update Database
        db: Session = SessionLocal()
        try:
            now = datetime.datetime.now(datetime.timezone.utc)
            dev_name = metadata.get("device_name", device_id)
            # Check by ID or matching name
            device = db.query(Device).filter(
                (Device.id == device_id) | (Device.name == dev_name)
            ).first()

            # If only one device exists and matches the same OS, reuse it to avoid duplicate profiles
            if not device:
                existing_all = db.query(Device).all()
                if len(existing_all) == 1 and existing_all[0].os_name == metadata.get("os_name", "unknown"):
                    device = existing_all[0]

            if not device:
                device = Device(
                    id=device_id,
                    name=dev_name,
                    os_name=metadata.get("os_name", "unknown"),
                    os_version=metadata.get("os_version", ""),
                    agent_version=metadata.get("agent_version", "0.1.0"),
                    is_online=True,
                    last_seen=now,
                    created_at=now
                )
                db.add(device)
            else:
                device.id = device_id
                device.name = dev_name
                device.os_name = metadata.get("os_name", device.os_name)
                device.os_version = metadata.get("os_version", device.os_version)
                device.agent_version = metadata.get("agent_version", device.agent_version)
                device.is_online = True
                device.last_seen = now

            db.commit()
            logger.info(f"Device '{device_id}' ({device.name}) synced/reused and marked ONLINE.")
        finally:
            db.close()

        # Notify clients
        await self.broadcast_to_clients({
            "type": "device_status_change",
            "device_id": device_id,
            "is_online": True,
            "metadata": metadata
        })

    async def handle_agent_heartbeat(self, device_id: str):
        db: Session = SessionLocal()
        try:
            device = db.query(Device).filter(Device.id == device_id).first()
            if device:
                device.is_online = True
                device.last_seen = datetime.datetime.now(datetime.timezone.utc)
                db.commit()
        finally:
            db.close()

        await self.broadcast_to_clients({
            "type": "device_heartbeat",
            "device_id": device_id,
            "last_seen": datetime.datetime.now(datetime.timezone.utc).isoformat()
        })

    async def disconnect_agent(self, device_id: str):
        if device_id in self.agent_connections:
            del self.agent_connections[device_id]
        if device_id in self.device_info:
            del self.device_info[device_id]

        db: Session = SessionLocal()
        try:
            device = db.query(Device).filter(Device.id == device_id).first()
            if device:
                device.is_online = False
                device.last_seen = datetime.datetime.now(datetime.timezone.utc)
                db.commit()
            logger.info(f"Device '{device_id}' disconnected and marked OFFLINE.")
        finally:
            db.close()

        await self.broadcast_to_clients({
            "type": "device_status_change",
            "device_id": device_id,
            "is_online": False
        })

    def is_agent_online(self, device_id: str) -> bool:
        return device_id in self.agent_connections

    def is_agent_connected(self, device_id: str) -> bool:
        return self.is_agent_online(device_id)

    async def send_to_agent(self, device_id: str, message: Dict[str, Any]):
        if self.is_agent_online(device_id):
            ws = self.agent_connections[device_id]
            try:
                await ws.send_json(message)
            except Exception as e:
                logger.warning(f"Failed to send message to agent {device_id}: {e}")

    async def send_command_to_agent(
        self, device_id: str, request_id: str, tool: str, arguments: Dict[str, Any], timeout_seconds: float = 30.0
    ) -> Dict[str, Any]:
        if not self.is_agent_online(device_id):
            raise ConnectionError(f"Device '{device_id}' is offline.")

        ws = self.agent_connections[device_id]
        loop = asyncio.get_running_loop()
        future: asyncio.Future = loop.create_future()
        self.pending_commands[request_id] = future

        message = {
            "type": "command",
            "request_id": request_id,
            "tool": tool,
            "arguments": arguments
        }

        try:
            await ws.send_json(message)
            result = await asyncio.wait_for(future, timeout=timeout_seconds)
            return result
        except asyncio.TimeoutError:
            raise TimeoutError(f"Command '{tool}' timed out after {timeout_seconds}s waiting for PC agent response.")
        finally:
            self.pending_commands.pop(request_id, None)

    def resolve_command_result(self, request_id: str, payload: Dict[str, Any]):
        future = self.pending_commands.get(request_id)
        if future and not future.done():
            future.set_result(payload)

ws_manager = ConnectionManager()
