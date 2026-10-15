import json
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings

def test_agent_websocket_registration_and_heartbeat():
    with TestClient(app) as client:
        # 1. Connect agent via WebSocket
        ws_url = f"/ws/agent?token={settings.AGENT_TOKEN}"
        with client.websocket_connect(ws_url) as websocket:
            # 2. Send registration
            reg_payload = {
                "type": "register",
                "device_id": "test-macbook-pro",
                "device_name": "Test MacBook Pro",
                "os_name": "macOS",
                "os_version": "14.4.1",
                "agent_version": "0.1.0"
            }
            websocket.send_text(json.dumps(reg_payload))

            # Expect registered acknowledgment
            ack_raw = websocket.receive_text()
            ack = json.loads(ack_raw)
            assert ack["type"] == "registered"
            assert ack["device_id"] == "test-macbook-pro"
            assert ack["status"] == "authorized"

            # 3. Send heartbeat
            websocket.send_text(json.dumps({
                "type": "heartbeat",
                "device_id": "test-macbook-pro"
            }))
            hb_ack_raw = websocket.receive_text()
            hb_ack = json.loads(hb_ack_raw)
            assert hb_ack["type"] == "heartbeat_ack"

            # 4. Check device endpoint via authenticated HTTP client
            login_res = client.post("/api/auth/login", json={
                "username": settings.INITIAL_ADMIN_USERNAME,
                "password": settings.INITIAL_ADMIN_PASSWORD
            })
            token = login_res.json()["access_token"]
            headers = {"Authorization": f"Bearer {token}"}

            devices_res = client.get("/api/devices", headers=headers)
            assert devices_res.status_code == 200
            devices = devices_res.json()
            test_dev = next((d for d in devices if d["id"] == "test-macbook-pro"), None)
            assert test_dev is not None
            assert test_dev["is_online"] is True
            assert test_dev["name"] == "Test MacBook Pro"

def test_unauthorized_agent_token_rejected():
    with TestClient(app) as client:
        with pytest.raises(Exception):
            with client.websocket_connect("/ws/agent?token=wrong_token") as ws:
                pass
