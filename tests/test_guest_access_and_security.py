import pytest
import time
import uuid
import json
import os
import hashlib
import tempfile
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings
from app.database.session import SessionLocal
from app.models.entities import Device, AllowedDirectory, GuestInvitation, GuestSession, StagedFile, PrivacyShieldState
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer
from agent.remote_forge_agent.commands.dispatcher import CommandDispatcher

@pytest.fixture
def client():
    from app.api.routes.guest import REDEMPTION_RATE_LIMIT
    REDEMPTION_RATE_LIMIT.clear()
    with TestClient(app) as c:
        yield c

@pytest.fixture
def db():
    session = SessionLocal()
    yield session
    session.close()

@pytest.fixture
def auth_headers(client):
    res = client.post("/api/auth/login", json={
        "username": settings.INITIAL_ADMIN_USERNAME,
        "password": settings.INITIAL_ADMIN_PASSWORD
    })
    assert res.status_code == 200
    token = res.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

@pytest.fixture
def test_device(db):
    dev = db.query(Device).filter(Device.id == "test-workstation-1").first()
    if not dev:
        dev = Device(
            id="test-workstation-1",
            name="Test Workstation",
            os_name="macOS",
            is_online=True
        )
        db.add(dev)
        db.commit()
        db.refresh(dev)
    return dev

# =========================================================================
# 1. Invitation Lifecycle Tests
# =========================================================================

def test_invitation_creation_and_redemption(client, auth_headers, test_device, db):
    # Owner creates invitation
    res = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view", "input.control"],
        "expiry_seconds": 3600
    })
    assert res.status_code == 200
    data = res.json()
    assert "invite_secret" in data
    assert data["invite_secret"] is not None
    assert "#invitation_id=" in data["qr_payload"]
    inv_id = data["id"]
    secret = data["invite_secret"]

    # Verify secret is NOT stored in plain text in DB (only token_hash is stored)
    inv_db = db.query(GuestInvitation).filter(GuestInvitation.id == inv_id).first()
    assert inv_db is not None
    assert inv_db.token_hash == hashlib.sha256(secret.encode()).hexdigest()
    assert secret not in inv_db.token_hash

    # Guest redeems invitation
    redeem_res = client.post("/api/guest/redeem", json={
        "invitation_id": inv_id,
        "invite_secret": secret,
        "guest_name": "Alice"
    })
    assert redeem_res.status_code == 200
    redeem_data = redeem_res.json()
    assert redeem_data["status"] == "PENDING_APPROVAL"
    assert "guest_token" in redeem_data
    guest_token = redeem_data["guest_token"]

    # Single-use enforcement: replaying redemption MUST fail (HTTP 409 Conflict)
    replay_res = client.post("/api/guest/redeem", json={
        "invitation_id": inv_id,
        "invite_secret": secret,
        "guest_name": "Attacker"
    })
    assert replay_res.status_code == 409

def test_invitation_revocation_and_expiry(client, auth_headers, test_device):
    # Create invitation
    res = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view"],
        "expiry_seconds": 60
    })
    assert res.status_code == 200
    inv = res.json()

    # Owner revokes invitation
    del_res = client.delete(f"/api/guest/invitations/{inv['id']}", headers=auth_headers)
    assert del_res.status_code == 200

    # Redeeming revoked invitation must be rejected (HTTP 410 Gone)
    redeem_res = client.post("/api/guest/redeem", json={
        "invitation_id": inv["id"],
        "invite_secret": inv["invite_secret"],
        "guest_name": "Bob"
    })
    assert redeem_res.status_code == 410

def test_invalid_secret_rejected(client, auth_headers, test_device):
    res = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view"],
        "expiry_seconds": 3600
    })
    inv = res.json()

    redeem_res = client.post("/api/guest/redeem", json={
        "invitation_id": inv["id"],
        "invite_secret": "wrong_secret_token_here",
        "guest_name": "Eve"
    })
    assert redeem_res.status_code == 401

# =========================================================================
# 2. Authorization, Approval & Co-Control Tests
# =========================================================================

def test_guest_approval_and_capability_upgrade(client, auth_headers, test_device):
    # Create and redeem invitation
    inv_res = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view"],
        "expiry_seconds": 3600
    })
    inv = inv_res.json()
    redeem_res = client.post("/api/guest/redeem", json={
        "invitation_id": inv["id"],
        "invite_secret": inv["invite_secret"],
        "guest_name": "Charlie"
    })
    guest_info = redeem_res.json()
    session_id = guest_info["session_id"]
    guest_headers = {"Authorization": f"Bearer {guest_info['guest_token']}"}

    # Before approval, status is PENDING_APPROVAL
    status_res = client.get("/api/guest/session/status", headers=guest_headers)
    assert status_res.status_code == 200
    assert status_res.json()["status"] == "PENDING_APPROVAL"

    # Owner approves with shared-control and upgraded capabilities
    approve_res = client.post(f"/api/guest/sessions/{session_id}/approve", headers=auth_headers, json={
        "approve": True,
        "control_mode": "shared-control",
        "capabilities": ["screen.view", "input.control", "files.upload"]
    })
    assert approve_res.status_code == 200
    approved_data = approve_res.json()
    assert approved_data["status"] == "APPROVED"
    assert approved_data["control_mode"] == "shared-control"
    assert "input.control" in approved_data["approved_capabilities"]

    # Guest status is now APPROVED
    status_res2 = client.get("/api/guest/session/status", headers=guest_headers)
    assert status_res2.status_code == 200
    assert status_res2.json()["status"] == "APPROVED"

def test_guest_approval_with_approved_capabilities_and_defaults(client, auth_headers, test_device):
    # Test approving using the exact payload format sent by Android and Web clients
    inv_res = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view", "input.control"],
        "expiry_seconds": 3600
    })
    inv = inv_res.json()
    redeem_res = client.post("/api/guest/redeem", json={
        "invitation_id": inv["id"],
        "invite_secret": inv["invite_secret"],
        "guest_name": "Dave"
    })
    session_id = redeem_res.json()["session_id"]

    # Approve sending only approved_capabilities without approve field
    res = client.post(f"/api/guest/sessions/{session_id}/approve", headers=auth_headers, json={
        "approved_capabilities": ["screen.view", "input.control", "files.upload", "files.download"],
        "control_mode": "shared-control",
        "duration_minutes": 30
    })
    assert res.status_code == 200
    data = res.json()
    assert data["status"] == "APPROVED"
    assert "screen.view" in data["approved_capabilities"]
    assert "input.control" in data["approved_capabilities"]

def test_guest_session_revocation(client, auth_headers, test_device):
    inv_res = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view"],
        "expiry_seconds": 3600
    })
    inv = inv_res.json()
    guest_info = client.post("/api/guest/redeem", json={
        "invitation_id": inv["id"],
        "invite_secret": inv["invite_secret"],
        "guest_name": "Dave"
    }).json()
    session_id = guest_info["session_id"]
    guest_headers = {"Authorization": f"Bearer {guest_info['guest_token']}"}

    # Owner revokes session
    client.post(f"/api/guest/sessions/{session_id}/revoke", headers=auth_headers)

    # Guest requests now return 403 Forbidden
    status_res = client.get("/api/guest/session/status", headers=guest_headers)
    assert status_res.status_code == 403

# =========================================================================
# 3. Privacy Shield and Emergency Lockdown Tests
# =========================================================================

def test_privacy_shield_emergency_lockdown(client, auth_headers, test_device):
    # Activate Emergency Lockdown
    res = client.post(f"/api/guest/privacy-shield/{test_device.id}", headers=auth_headers, json={
        "action": "emergency_lockdown"
    })
    assert res.status_code == 200
    ps = res.json()
    assert ps["is_lockdown"] is True
    assert ps["is_screen_paused"] is True
    assert ps["is_input_disabled"] is True
    assert ps["is_transfers_stopped"] is True

    # Under lockdown, new guest invitation creation MUST be blocked
    block_inv = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view"],
        "expiry_seconds": 1800
    })
    assert block_inv.status_code == 403

    # Owner resumes access
    resume_res = client.post(f"/api/guest/privacy-shield/{test_device.id}", headers=auth_headers, json={
        "action": "resume_access"
    })
    assert resume_res.status_code == 200
    assert resume_res.json()["is_lockdown"] is False

# =========================================================================
# 4. Host Agent Independent Enforcement Tests
# =========================================================================

def test_host_agent_rejects_guest_execution_and_lockdown():
    import shutil
    safe_root = os.path.expanduser("~/.remoteforge_guest_test_sandbox")
    if os.path.exists(safe_root):
        shutil.rmtree(safe_root, ignore_errors=True)
    os.makedirs(safe_root, exist_ok=True)
    try:
        enforcer = FilesystemSecurityEnforcer([safe_root])
        dispatcher = CommandDispatcher(enforcer)

        # 1. Privileged command attempted with guest actor MUST be rejected by host agent
        guest_args = {"actor": "guest:session-123:Attacker", "command": "whoami"}
        import asyncio
        res = asyncio.run(dispatcher.dispatch("run_command", guest_args))
        assert res["success"] is False
        assert "Guests are forbidden" in res["error"]

        # 2. Host agent with input disabled rejects guest input
        dispatcher.privacy_shield_state["is_input_disabled"] = True
        input_args = {"actor": "guest:session-123:Attacker", "action": "mouse_move", "x": 100, "y": 100}
        res_input = asyncio.run(dispatcher.dispatch("send_input_event", input_args))
        assert res_input["success"] is False
        assert "input disabled" in res_input["error"]

        # 3. Host agent with emergency lockdown rejects all guest actions
        dispatcher.privacy_shield_state["is_lockdown"] = True
        res_lock = asyncio.run(dispatcher.dispatch("send_input_event", input_args))
        assert res_lock["success"] is False
        assert "Emergency Lockdown is active" in res_lock["error"]
    finally:
        shutil.rmtree(safe_root, ignore_errors=True)


# =========================================================================
# 5. Guest Drop Zone & Quarantine Tests
# =========================================================================

def test_guest_staging_upload_and_promotion(client, auth_headers, test_device, tmp_path, db):
    # Setup allowed directory
    allowed_dir = tmp_path / "workstation_allowed"
    allowed_dir.mkdir()
    ad = AllowedDirectory(
        device_id=test_device.id,
        canonical_path=str(allowed_dir),
        display_name="Workstation Allowed",
        is_active=True
    )
    db.add(ad)
    db.commit()

    # Create approved guest with files.upload & files.list
    inv = client.post("/api/guest/invitations", headers=auth_headers, json={
        "device_id": test_device.id,
        "capabilities": ["screen.view", "files.upload", "files.list"],
        "expiry_seconds": 3600
    }).json()
    guest = client.post("/api/guest/redeem", json={
        "invitation_id": inv["id"],
        "invite_secret": inv["invite_secret"],
        "guest_name": "Frank"
    }).json()
    client.post(f"/api/guest/sessions/{guest['session_id']}/approve", headers=auth_headers, json={
        "approve": True,
        "capabilities": ["screen.view", "files.upload", "files.list"]
    })
    guest_headers = {"Authorization": f"Bearer {guest['guest_token']}"}

    # Guest uploads test file into staging
    file_content = b"console.log('Secure Guest Staged Payload');"
    upload_res = client.post(
        "/api/guest/staging/upload",
        headers=guest_headers,
        files={"file": ("test_script.js", file_content, "application/javascript")}
    )
    assert upload_res.status_code == 200
    staged = upload_res.json()
    assert staged["status"] == "QUARANTINED"
    assert staged["filename"] == "test_script.js"
    assert staged["sha256_hash"] == hashlib.sha256(file_content).hexdigest()
    file_id = staged["id"]

    # Owner promotes file into allowed directory
    promote_res = client.post(
        f"/api/guest/staging/promote/{file_id}",
        headers=auth_headers,
        json={"target_directory": str(allowed_dir)}
    )
    assert promote_res.status_code == 200
    promoted_info = promote_res.json()
    assert promoted_info["status"] == "PROMOTED"
    assert os.path.exists(promoted_info["promoted_to_path"])

    # Attempting to promote to non-allowed directory MUST be rejected
    upload_res2 = client.post(
        "/api/guest/staging/upload",
        headers=guest_headers,
        files={"file": ("test2.txt", b"hello", "text/plain")}
    )
    file_id2 = upload_res2.json()["id"]
    unauth_promote = client.post(
        f"/api/guest/staging/promote/{file_id2}",
        headers=auth_headers,
        json={"target_directory": "/etc"}
    )
    assert unauth_promote.status_code == 403
