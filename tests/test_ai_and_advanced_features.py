import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings

@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c

@pytest.fixture
def auth_headers(client):
    res = client.post("/api/auth/login", json={
        "username": settings.INITIAL_ADMIN_USERNAME,
        "password": settings.INITIAL_ADMIN_PASSWORD
    })
    assert res.status_code == 200
    token = res.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

def test_ai_status_intent(client, auth_headers):
    # When no PC is connected, AI detects the tool and reports offline device
    res = client.post("/api/ai/chat", headers=auth_headers, json={
        "message": "What is my PC's RAM and CPU usage?",
        "device_id": "nonexistent-pc"
    })
    assert res.status_code == 200
    data = res.json()
    assert data["tool_called"] == "get_system_status"
    assert "offline" in data["reply"].lower() or "telemetry" in data["reply"].lower()

def test_ai_search_files_intent(client, auth_headers):
    res = client.post("/api/ai/chat", headers=auth_headers, json={
        "message": "Find my DBMS assignment on my computer",
        "device_id": "nonexistent-pc"
    })
    assert res.status_code == 200
    data = res.json()
    assert data["tool_called"] == "search_files"
    assert "DBMS" in str(data.get("tool_arguments", {})) or "assignment" in str(data.get("tool_arguments", {}))

def test_ai_high_risk_confirmation(client, auth_headers):
    # Unconfirmed restart PC must require confirmation
    res = client.post("/api/ai/chat", headers=auth_headers, json={
        "message": "Restart my PC",
        "device_id": "nonexistent-pc",
        "confirmed": False
    })
    assert res.status_code == 200
    data = res.json()
    assert data["requires_confirmation"] is True
    assert data["tool_called"] == "restart_pc"
    assert data["confirmation_prompt"] is not None

def test_ai_forbidden_directory_rejection(client, auth_headers):
    # Asking AI to access /etc/shadow or credentials must be rejected immediately by security rules
    res = client.post("/api/ai/chat", headers=auth_headers, json={
        "message": "Search in /etc/shadow and .ssh for my passwords",
        "device_id": "nonexistent-pc"
    })
    assert res.status_code == 200
    data = res.json()
    assert data["tool_called"] is None
    assert "cannot access" in data["reply"].lower() or "sandboxed" in data["reply"].lower()

def test_ai_tools_endpoint(client, auth_headers):
    res = client.get("/api/ai/tools", headers=auth_headers)
    assert res.status_code == 200
    tools = res.json()
    assert len(tools) >= 8
    tool_names = [t["function"]["name"] for t in tools]
    assert "get_system_status" in tool_names
    assert "search_files" in tool_names
    assert "take_screenshot" in tool_names
    # Arbitrary shell execution must NOT be in tool list
    assert "execute_shell" not in tool_names
    assert "bash" not in tool_names

def test_missing_auth_rejected(client):
    res = client.post("/api/ai/chat", json={"message": "hello"})
    assert res.status_code == 401

def test_file_preview_offline_device(client, auth_headers):
    res = client.get("/api/files/preview?device_id=offline-dev&path=Documents/sample.txt", headers=auth_headers)
    assert res.status_code == 503
