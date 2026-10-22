import json
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings

@pytest.fixture
def auth_headers():
    with TestClient(app) as client:
        login_res = client.post("/api/auth/login", json={
            "username": settings.INITIAL_ADMIN_USERNAME,
            "password": settings.INITIAL_ADMIN_PASSWORD
        })
        assert login_res.status_code == 200
        token = login_res.json()["access_token"]
        return {"Authorization": f"Bearer {token}"}

@pytest.fixture
def test_device_id(auth_headers):
    with TestClient(app) as client:
        # Register a test device
        res = client.get("/api/devices", headers=auth_headers)
        assert res.status_code == 200
        devices = res.json()
        if devices:
            return devices[0]["id"]
        return "macbook-pro-test"

# ==========================================
# 1. Virtual Display Tests
# ==========================================
def test_virtual_display_lifecycle(auth_headers, test_device_id):
    with TestClient(app) as client:
        # Start Virtual Display
        start_res = client.post("/api/virtual-display/start", json={
            "device_id": test_device_id,
            "width": 1920,
            "height": 1080,
            "fps": 60,
            "dpi": 240,
            "enable_touch": True
        }, headers=auth_headers)
        assert start_res.status_code == 200
        data = start_res.json()
        assert data["is_active"] is True
        assert data["width"] == 1920
        assert data["height"] == 1080
        assert data["fps"] == 60

        # Status check
        status_res = client.get(f"/api/virtual-display/status/{test_device_id}", headers=auth_headers)
        assert status_res.status_code == 200
        status_data = status_res.json()
        assert status_data["is_active"] is True

        # Touch gesture injection
        touch_res = client.post("/api/virtual-display/touch", json={
            "device_id": test_device_id,
            "action": "down",
            "norm_x": 0.5,
            "norm_y": 0.5
        }, headers=auth_headers)
        assert touch_res.status_code == 200

        # Stop Virtual Display
        stop_res = client.post("/api/virtual-display/stop", json={
            "device_id": test_device_id
        }, headers=auth_headers)
        assert stop_res.status_code == 200
        assert stop_res.json()["is_active"] is False

# ==========================================
# 2. Universal Control Edge Hopping Tests
# ==========================================
def test_edge_control_lifecycle(auth_headers, test_device_id):
    with TestClient(app) as client:
        # Configure Edge
        config_res = client.post("/api/edge-control/configure", json={
            "device_id": test_device_id,
            "edge": "RIGHT",
            "sensitivity": 1.2,
            "dwell_delay_ms": 100,
            "enable_keyboard": True,
            "enable_clipboard_sync": True
        }, headers=auth_headers)
        assert config_res.status_code == 200
        data = config_res.json()
        assert data["is_enabled"] is True
        assert data["configured_edge"] == "RIGHT"

        # Status check
        status_res = client.get(f"/api/edge-control/status/{test_device_id}", headers=auth_headers)
        assert status_res.status_code == 200
        assert status_res.json()["is_enabled"] is True

        # Handoff event (enter remote screen)
        handoff_res = client.post("/api/edge-control/handoff", json={
            "device_id": test_device_id,
            "event_type": "ENTER_REMOTE",
            "norm_x": 0.05,
            "norm_y": 0.5
        }, headers=auth_headers)
        assert handoff_res.status_code == 200
        assert handoff_res.json()["is_remote_active"] is True

        # Disable
        dis_res = client.post(f"/api/edge-control/disable?device_id={test_device_id}", headers=auth_headers)
        assert dis_res.status_code == 200
        assert dis_res.json()["status"] == "disabled"

# ==========================================
# 3. P2P Mesh Tunneling Tests
# ==========================================
def test_p2p_mesh_lifecycle(auth_headers, test_device_id):
    with TestClient(app) as client:
        # Get STUN Config
        config_res = client.get("/api/p2p/config", headers=auth_headers)
        assert config_res.status_code == 200
        config_data = config_res.json()
        assert len(config_data["stun_servers"]) > 0

        # Create P2P Session
        create_res = client.post("/api/p2p/session/create", json={
            "device_id": test_device_id,
            "client_type": "android"
        }, headers=auth_headers)
        assert create_res.status_code == 200
        session_id = create_res.json()["session_id"]
        assert session_id is not None

        # Post Signal (Offer)
        offer_payload = json.dumps({"sdp": "v=0\r\no=- 123 2 IN IP4 127.0.0.1", "type": "offer"})
        sig_res = client.post(f"/api/p2p/signal/{session_id}", json={
            "session_id": session_id,
            "sender_type": "client",
            "signal_type": "offer",
            "payload_json": offer_payload
        }, headers=auth_headers)
        assert sig_res.status_code == 200

        # Retrieve Signal
        get_sig_res = client.get(f"/api/p2p/signals/{session_id}?for_receiver=agent", headers=auth_headers)
        assert get_sig_res.status_code == 200
        signals = get_sig_res.json()["signals"]
        assert len(signals) >= 1
        assert signals[0]["signal_type"] == "offer"

# ==========================================
# 4. Low-Latency Bi-Directional Audio Tests
# ==========================================
def test_audio_session_lifecycle(auth_headers, test_device_id):
    with TestClient(app) as client:
        # Start Audio Session (Sink)
        start_res = client.post("/api/audio/start", json={
            "device_id": test_device_id,
            "mode": "sink",
            "sample_rate": 48000,
            "channels": 2,
            "bit_depth": 16,
            "codec": "pcm"
        }, headers=auth_headers)
        assert start_res.status_code == 200
        data = start_res.json()
        assert data["is_active"] is True
        assert data["mode"] == "sink"
        assert data["sample_rate"] == 48000

        # Status check
        status_res = client.get(f"/api/audio/status/{test_device_id}", headers=auth_headers)
        assert status_res.status_code == 200
        assert status_res.json()["is_active"] is True

        # Stop Audio Session
        stop_res = client.post("/api/audio/stop", json={
            "device_id": test_device_id
        }, headers=auth_headers)
        assert stop_res.status_code == 200
        assert stop_res.json()["is_active"] is False

# ==========================================
# 5. Multimedia WebSocket Verification
# ==========================================
def test_multimedia_websockets(test_device_id):
    with TestClient(app) as client:
        # Audio WebSocket connect & send binary frame
        with client.websocket_connect(f"/ws/audio/{test_device_id}") as ws1:
            ws1.send_bytes(b"\x00\x01\x02\x03" * 16)

        # P2P Signaling WebSocket connect & send signal
        with client.websocket_connect(f"/ws/p2p/signaling/test-session-123") as ws2:
            ws2.send_text(json.dumps({"type": "ping"}))

        # Edge Control WebSocket connect & send pointer packet
        with client.websocket_connect(f"/ws/edge-control/{test_device_id}") as ws3:
            ws3.send_text(json.dumps({"type": "pointer", "x": 0.5, "y": 0.5}))
