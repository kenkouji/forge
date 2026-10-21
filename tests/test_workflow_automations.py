import pytest
import json
from fastapi.testclient import TestClient
from app.main import app
from app.database.session import SessionLocal
from app.models.entities import User, Automation, Device
from app.auth.security import get_password_hash, create_access_token
from app.services.workflow_engine import (
    validate_workflow_definition,
    simulate_workflow_dry_run,
    get_workflow_templates,
    ACTIVE_WORKFLOW_EXECUTIONS
)

client = TestClient(app)

@pytest.fixture(scope="module")
def auth_headers():
    db = SessionLocal()
    try:
        user = db.query(User).filter(User.username == "test_wf_admin").first()
        if not user:
            user = User(
                username="test_wf_admin",
                hashed_password=get_password_hash("testpass123"),
                role="admin",
                is_active=True
            )
            db.add(user)
            db.commit()
        token = create_access_token(data={"sub": user.username, "role": user.role})
        return {"Authorization": f"Bearer {token}"}
    finally:
        db.close()

def test_workflow_templates_endpoint(auth_headers):
    """Verifies that pre-built workflow templates are available."""
    res = client.get("/api/automations/templates", headers=auth_headers)
    assert res.status_code == 200
    templates = res.json()
    assert isinstance(templates, list)
    assert len(templates) >= 3
    template_ids = [t["id"] for t in templates]
    assert "template_health_watchdog" in template_ids
    assert "template_build_test_guard" in template_ids
    assert "template_git_dirty_monitor" in template_ids

def test_workflow_validation_unit():
    """Unit tests for workflow structure and bounds enforcement."""
    # 1. Valid workflow
    valid_wf = {
        "name": "Test Health Flow",
        "steps": [
            {
                "step_id": "s1",
                "name": "Telemetry Check",
                "type": "action",
                "tool": "get_system_status",
                "arguments": {}
            },
            {
                "step_id": "s2",
                "name": "Check CPU",
                "type": "condition",
                "condition": {"field": "result.cpu_percent", "operator": ">", "value": 80}
            }
        ]
    }
    is_valid, err, _ = validate_workflow_definition(valid_wf)
    assert is_valid is True
    assert err is None

    # 2. Unregistered tool rejection
    invalid_tool_wf = {
        "name": "Malicious Flow",
        "steps": [
            {
                "step_id": "s1",
                "name": "Run Arbitrary Shell",
                "type": "action",
                "tool": "arbitrary_shell_command",
                "arguments": {"cmd": "rm -rf /"}
            }
        ]
    }
    is_valid, err, _ = validate_workflow_definition(invalid_tool_wf)
    assert is_valid is False
    assert "not registered" in err

    # 3. Empty steps rejection
    empty_wf = {"name": "Empty Flow", "steps": []}
    is_valid, err, _ = validate_workflow_definition(empty_wf)
    assert is_valid is False
    assert "at least one valid step" in err

def test_adhoc_dry_run_simulation(auth_headers):
    """Simulates workflow execution and policy checks without running commands on Mac."""
    wf_payload = {
        "name": "Dry Run Health Test",
        "steps": [
            {
                "step_id": "s1",
                "name": "Status Step",
                "type": "action",
                "tool": "get_system_status",
                "arguments": {}
            },
            {
                "step_id": "s2",
                "name": "Notification Step",
                "type": "notification",
                "notification_message": "Resource check passed."
            }
        ]
    }
    res = client.post("/api/automations/dry-run", json=wf_payload, headers=auth_headers)
    assert res.status_code == 200
    data = res.json()
    assert data["valid"] is True
    assert data["name"] == "Dry Run Health Test"
    assert data["steps_count"] == 2
    assert data["risk_assessment"] in ("LOW_RISK", "MEDIUM_RISK")
    assert len(data["steps"]) == 2

def test_create_and_dry_run_saved_workflow(auth_headers):
    """Creates a multi-step automation workflow and performs dry-run on saved record."""
    create_payload = {
        "name": "Saved Dev Build Guard",
        "description": "Automated build validator",
        "trigger_type": "manual",
        "schedule_time": "Manual Trigger",
        "action_tool": "build_project",
        "workflow_definition": {
            "name": "Saved Dev Build Guard",
            "version": "2.0",
            "steps": [
                {
                    "step_id": "b1",
                    "name": "Project Build",
                    "type": "action",
                    "tool": "build_project",
                    "arguments": {"project_path": "", "build_type": "build", "max_lines": 50}
                },
                {
                    "step_id": "c1",
                    "name": "Check Exit",
                    "type": "condition",
                    "condition": {"field": "result.exit_code", "operator": "!=", "value": 0}
                }
            ]
        }
    }
    res = client.post("/api/automations", json=create_payload, headers=auth_headers)
    assert res.status_code == 201
    auto_data = res.json()
    auto_id = auto_data["id"]
    assert auto_data["workflow_definition_json"] is not None

    # Perform dry run on saved workflow
    dry_res = client.post(f"/api/automations/{auto_id}/dry-run", headers=auth_headers)
    assert dry_res.status_code == 200
    dry_data = dry_res.json()
    assert dry_data["valid"] is True
    assert dry_data["steps_count"] == 2
    assert dry_data["name"] == "Saved Dev Build Guard"

    # Cleanup
    del_res = client.delete(f"/api/automations/{auto_id}", headers=auth_headers)
    assert del_res.status_code == 200

def test_cancel_workflow_execution(auth_headers):
    """Verifies that an active workflow execution can be cancelled via API."""
    test_exec_id = "test_cancel_exec_99"
    ACTIVE_WORKFLOW_EXECUTIONS[test_exec_id] = {
        "execution_id": test_exec_id,
        "workflow_id": "fake_wf",
        "workflow_name": "Cancelling Flow",
        "target_device_id": "fake_dev",
        "status": "RUNNING",
        "task": None
    }
    res = client.post(f"/api/automations/executions/{test_exec_id}/cancel", headers=auth_headers)
    assert res.status_code == 200
    assert res.json()["success"] is True
    assert ACTIVE_WORKFLOW_EXECUTIONS[test_exec_id]["status"] == "CANCELLED"
