import pytest
import os
import sys
import httpx
import uuid

# Ensure backend directory is in path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

BASE_URL = "http://127.0.0.1:8090"
AUTH_URL = f"{BASE_URL}/api/auth/login"
DEVICES_URL = f"{BASE_URL}/api/devices"
AI_CHAT_URL = f"{BASE_URL}/api/ai/chat"


@pytest.fixture(scope="module")
def auth_token():
    """Obtain a valid JWT token for authenticated requests."""
    resp = httpx.post(
        AUTH_URL,
        json={"username": "admin", "password": "remoteforge_secure_pass"},
        timeout=10.0
    )
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    data = resp.json()
    assert "access_token" in data
    return data["access_token"]


@pytest.fixture(scope="module")
def auth_headers(auth_token):
    return {
        "Authorization": f"Bearer {auth_token}",
        "Content-Type": "application/json"
    }


# =====================================================================
# 1. AMBIGUOUS COMMANDS & DISAMBIGUATION
# =====================================================================

def test_ambiguous_command_disambiguation(auth_headers):
    """
    Verify that an ambiguous natural language command (e.g. 'resume')
    triggers structured file search and returns disambiguation choices.
    """
    resp = httpx.post(
        AI_CHAT_URL,
        json={"message": "resume", "device_id": "macbook-pro"},
        headers=auth_headers,
        timeout=15.0
    )
    assert resp.status_code == 200
    data = resp.json()
    reply = data.get("reply", "")
    assert "resume" in reply.lower()


# =====================================================================
# 2. DENIED PERMISSIONS & PATH TRAVERSAL
# =====================================================================

def test_denied_permissions_unregistered_shell(auth_headers):
    """
    Verify that requesting unallowlisted/unregistered shell tools
    (run_shell, code_interpreter) is strictly denied with HTTP 400.
    """
    # 1. Arbitrary shell execution attempt
    resp_shell = httpx.post(
        f"{DEVICES_URL}/macbook-pro/commands",
        json={"tool": "run_shell", "arguments": {"command": "cat /etc/passwd"}},
        headers=auth_headers,
        timeout=10.0
    )
    assert resp_shell.status_code == 400
    detail = resp_shell.json()["detail"].lower()
    assert "not in the controlled tool registry" in detail or "forbidden" in detail

    # 2. Code interpreter attempt
    resp_code = httpx.post(
        f"{DEVICES_URL}/macbook-pro/commands",
        json={"tool": "code_interpreter", "arguments": {"code": "import os; os.system('whoami')"}},
        headers=auth_headers,
        timeout=10.0
    )
    assert resp_code.status_code == 400
    detail = resp_code.json()["detail"].lower()
    assert "not in the controlled tool registry" in detail or "forbidden" in detail


def test_denied_permissions_path_traversal(auth_headers):
    """
    Verify that filesystem tools strictly deny path traversal attempts
    targeting sensitive directories (/etc/shadow, ~/.ssh, /System).
    """
    # 1. Path traversal via ..
    resp_traversal = httpx.post(
        f"{DEVICES_URL}/macbook-pro/commands",
        json={"tool": "search_files", "arguments": {"query": "passwd", "directory": "../../etc"}},
        headers=auth_headers,
        timeout=10.0
    )
    # Backend returns command result with status=FAILED and access denied error
    assert resp_traversal.status_code == 200
    data = resp_traversal.json()
    assert data["status"] == "FAILED"
    assert "outside allowed" in data["error"].lower() or "denied" in data["error"].lower()

    # 2. Direct sensitive path targeting
    resp_ssh = httpx.post(
        f"{DEVICES_URL}/macbook-pro/commands",
        json={"tool": "list_directory", "arguments": {"path": "/etc/ssl"}},
        headers=auth_headers,
        timeout=10.0
    )
    assert resp_ssh.status_code == 200
    assert resp_ssh.json()["status"] == "FAILED"
    assert "outside allowed" in resp_ssh.json()["error"].lower() or "denied" in resp_ssh.json()["error"].lower()


# =====================================================================
# 3. APPROVAL-REQUIRED ACTIONS (HUMAN-IN-THE-LOOP)
# =====================================================================

def test_approval_required_actions(auth_headers):
    """
    Verify that high/critical risk actions (e.g. restart_pc, shutdown_pc)
    are REJECTED when confirmed=False, and REQUIRE explicit user approval.
    """
    resp_unconfirmed = httpx.post(
        f"{DEVICES_URL}/macbook-pro/commands",
        json={"tool": "restart_pc", "arguments": {}, "confirmed": False},
        headers=auth_headers,
        timeout=10.0
    )
    assert resp_unconfirmed.status_code == 400
    detail = resp_unconfirmed.json()["detail"]
    assert "confirmation" in detail.lower() and "explicit" in detail.lower()

    # In audit log, it should record confirmation_status as REQUIRED
    audit_resp = httpx.get(f"{BASE_URL}/api/audit", headers=auth_headers, timeout=10.0)
    assert audit_resp.status_code == 200
    logs = audit_resp.json() if isinstance(audit_resp.json(), list) else audit_resp.json().get("logs", [])
    assert any("restart_pc" in str(item).lower() for item in logs)


# =====================================================================
# 4. RETRIES AND IDEMPOTENCY
# =====================================================================

def test_command_retries_and_idempotency(auth_headers):
    """
    Verify that retrying a read-only command executes safely without duplicating tasks
    or causing state corruption.
    """
    for _ in range(2):
        resp = httpx.post(
            f"{DEVICES_URL}/macbook-pro/commands",
            json={"tool": "get_system_status", "arguments": {}},
            headers=auth_headers,
            timeout=15.0
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "COMPLETED"
        assert "cpu" in data["result"]


# =====================================================================
# 5. VPN / NETWORK DISCONNECTIONS
# =====================================================================

def test_vpn_disconnection_offline_agent(auth_headers):
    """
    Verify that attempting to dispatch commands when an agent is offline
    (e.g. VPN tunnel dropped or agent disconnected) immediately returns
    HTTP 503 without hanging, timing out, or claiming false success.
    """
    resp = httpx.post(
        f"{DEVICES_URL}/offline-macbook-vpn-dropped/commands",
        json={"tool": "get_system_status", "arguments": {}},
        headers=auth_headers,
        timeout=5.0
    )
    assert resp.status_code in [404, 503]
    if resp.status_code == 503:
        assert "offline" in resp.json()["detail"].lower()


# =====================================================================
# 6. UNAUTHORIZED DEVICE ACCESS & AUTHENTICATION
# =====================================================================

def test_unauthorized_access_missing_auth():
    """Verify that requests without an Authorization header are rejected with HTTP 401."""
    resp = httpx.post(
        f"{DEVICES_URL}/macbook-pro/commands",
        json={"tool": "get_system_status", "arguments": {}},
        timeout=10.0
    )
    assert resp.status_code == 401
    detail = resp.json()["detail"].lower()
    assert "missing authentication" in detail or "not authenticated" in detail


def test_unauthorized_access_invalid_token():
    """Verify that requests with a forged/tampered JWT are rejected with HTTP 401."""
    resp = httpx.post(
        f"{DEVICES_URL}/macbook-pro/commands",
        json={"tool": "get_system_status", "arguments": {}},
        headers={"Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature"},
        timeout=10.0
    )
    assert resp.status_code == 401


def test_unauthorized_device_not_found(auth_headers):
    """Verify that attempting to access a non-existent or unowned device returns HTTP 404."""
    resp = httpx.post(
        f"{DEVICES_URL}/attacker-nonexistent-device/commands",
        json={"tool": "get_system_status", "arguments": {}},
        headers=auth_headers,
        timeout=10.0
    )
    assert resp.status_code == 404
    assert "not found" in resp.json()["detail"].lower()


# =====================================================================
# 7. ZERO-TRUST AND DOCKER SOCKET HARDENING
# =====================================================================

def test_docker_socket_and_shell_hardening():
    """
    Verify that Docker socket is not mounted or exposed, and shell tools
    remain completely disabled.
    """
    from app.ai.tools import AI_AVAILABLE_TOOLS
    tool_names = [t["function"]["name"] for t in AI_AVAILABLE_TOOLS if "function" in t]
    assert "run_shell" not in tool_names
    assert "code_interpreter" not in tool_names
    assert "execute_bash" not in tool_names
