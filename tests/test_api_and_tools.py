import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings
from app.permissions.registry import (
    validate_tool_and_permissions,
    is_tool_registered,
    TOOL_DEFINITIONS
)

@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c

def test_health_endpoint(client):
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["server"] == "RemoteForge"
    assert data["version"] == "0.1.0"

def test_auth_login(client):
    response = client.post("/api/auth/login", json={
        "username": settings.INITIAL_ADMIN_USERNAME,
        "password": settings.INITIAL_ADMIN_PASSWORD
    })
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert data["username"] == settings.INITIAL_ADMIN_USERNAME

def test_tool_registry_and_policy():
    # Low risk allowed tools
    assert is_tool_registered("get_system_status") is True
    assert is_tool_registered("take_screenshot") is True

    # Arbitrary shell commands MUST NOT exist
    assert is_tool_registered("bash") is False
    assert is_tool_registered("rm") is False
    assert is_tool_registered("sudo") is False
    assert is_tool_registered("curl") is False
    assert is_tool_registered("execute_shell") is False

    # Validation: low risk tool without confirmation is valid
    res = validate_tool_and_permissions("get_system_status", {})
    assert res["valid"] is True
    assert res["permission_level"] == "LOW"

    # Validation: high risk tool requires confirmation
    res_high_unconfirmed = validate_tool_and_permissions("restart_pc", {}, is_confirmed=False)
    assert res_high_unconfirmed["valid"] is False
    assert res_high_unconfirmed["requires_confirmation"] is True

    res_high_confirmed = validate_tool_and_permissions("restart_pc", {}, is_confirmed=True)
    assert res_high_confirmed["valid"] is True

def test_arbitrary_shell_injection_rejected():
    res = validate_tool_and_permissions("open_application", {"app_name": "Calculator; rm -rf /"})
    assert res["valid"] is False
    assert "Disallowed shell pattern" in res["error"]
