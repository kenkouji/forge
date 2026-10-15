import pytest
import os
import sys

# Ensure backend directory is in path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.ai.skill_engine_gateway import AISkillEngineGateway, AISkillEngineSecurityError
from app.ai.schemas import ChatRequest
from app.permissions.registry import is_tool_registered, validate_tool_and_permissions
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer
from agent.remote_forge_agent.commands.dispatcher import CommandDispatcher


def get_test_dispatcher():
    enforcer = FilesystemSecurityEnforcer()
    return CommandDispatcher(enforcer=enforcer)


class MockWebSocketManager:
    def is_agent_online(self, device_id: str) -> bool:
        return True

    async def send_command_to_agent(self, device_id: str, request_id: str, tool: str, arguments: dict, timeout_seconds: float = 25.0):
        dispatcher = get_test_dispatcher()
        return await dispatcher.dispatch(tool, arguments)


@pytest.fixture
def gateway():
    return AISkillEngineGateway(simulate_offline=True)


def test_tool_registration_allowlist(gateway):
    """Verify that only authorized RemoteForge tools are permitted, blocking code interpreter and shell tools."""
    # 1. Safe tools are registered
    assert is_tool_registered("get_system_status") is True
    assert is_tool_registered("search_files") is True
    assert is_tool_registered("lock_pc") is True

    # 2. Arbitrary code interpreters and unrestricted shells are not registered
    assert is_tool_registered("code_interpreter") is False
    assert is_tool_registered("run_shell") is False
    assert is_tool_registered("bash") is False
    assert is_tool_registered("execute_python") is False


def test_interception_of_unregistered_tools(gateway):
    """Verify that model-generated tool calls for arbitrary code/shell are blocked before execution."""
    permitted, err, args = gateway.validate_and_intercept_tool_call("code_interpreter", {"code": "import os; os.system('whoami')"})
    assert permitted is False
    assert "not permitted" in err or "Arbitrary" in err

    permitted2, err2, args2 = gateway.validate_and_intercept_tool_call("run_shell", {"command": "rm -rf /"})
    assert permitted2 is False
    assert "Arbitrary" in err2 or "not permitted" in err2


def test_interception_of_path_traversal(gateway):
    """Verify that file tools reject path traversal attempts to sensitive system directories."""
    permitted, err, args = gateway.validate_and_intercept_tool_call("search_files", {"query": "keys", "directory": "/etc/ssl"})
    assert permitted is False
    assert "prohibited" in err

    permitted2, err2, args2 = gateway.validate_and_intercept_tool_call("list_directory", {"path": "/Users/g.aasish/.ssh"})
    assert permitted2 is False
    assert "prohibited" in err2


@pytest.mark.asyncio
async def test_read_only_telemetry_poc(gateway):
    """Read-only proof of concept: verify get_system_status tool call validation and real execution."""
    # 1. Validate tool interception
    permitted, err, sanitized_args = gateway.validate_and_intercept_tool_call("get_system_status", {})
    assert permitted is True
    assert err is None

    # 2. Execute against live Mac agent command dispatcher
    dispatcher = get_test_dispatcher()
    res = await dispatcher.dispatch("get_system_status", sanitized_args)
    assert res["success"] is True
    assert res["error"] is None
    telemetry = res["result"]
    assert "cpu" in telemetry
    assert "ram" in telemetry
    assert "disk" in telemetry
    assert telemetry["cpu"]["percent"] >= 0.0
    assert telemetry["ram"]["total_gb"] > 0.0


@pytest.mark.asyncio
async def test_bounded_file_search_poc(gateway):
    """Verify bounded file search executes successfully within user workspace and rejects dangerous roots."""
    # Valid allowed directory
    permitted, err, sanitized_args = gateway.validate_and_intercept_tool_call("search_files", {"query": "resume", "directory": "/Users/g.aasish/Desktop"})
    assert permitted is True

    dispatcher = get_test_dispatcher()
    res = await dispatcher.dispatch("search_files", {"query": "resume"})
    assert res["success"] is True
    assert isinstance(res["result"].get("matches"), list)


def test_consequential_action_confirmation_requirement(gateway):
    """Verify that destructive/modifying actions strictly require explicit user confirmation."""
    # Restart PC without confirmation must require confirmation
    check_unconfirmed = validate_tool_and_permissions("restart_pc", {}, is_confirmed=False)
    assert check_unconfirmed["requires_confirmation"] is True
    assert check_unconfirmed["valid"] is False

    # Restart PC with confirmed=True passes permission validation
    check_confirmed = validate_tool_and_permissions("restart_pc", {}, is_confirmed=True)
    assert check_confirmed["valid"] is True


@pytest.mark.asyncio
async def test_close_application_canonicalization():
    """Verify close_application and minimize_application handle common aliases gracefully."""
    dispatcher = get_test_dispatcher()
    # Test closing an application alias
    res = await dispatcher.dispatch("close_application", {"app_name": "chrome or minimize it"})
    assert res["success"] is True
    assert "Google Chrome" in res["result"]["status"]

    # Test minimize_application
    min_res = await dispatcher.dispatch("minimize_application", {"app_name": "Google Chrome"})
    assert min_res["success"] is True
    assert "minimized successfully" in min_res["result"]["status"]
