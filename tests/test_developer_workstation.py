import os
import pytest
from unittest.mock import MagicMock
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer, SecurityPolicyException
from agent.remote_forge_agent.developer.workstation import DeveloperWorkstation
from agent.remote_forge_agent.commands.dispatcher import CommandDispatcher


import shutil

@pytest.fixture
def test_env():
    sandbox_base = os.path.expanduser("~/.remoteforge_dev_test_sandbox")
    if os.path.exists(sandbox_base):
        shutil.rmtree(sandbox_base, ignore_errors=True)
    os.makedirs(sandbox_base, exist_ok=True)

    proj_dir = os.path.join(sandbox_base, "test_repo")
    os.makedirs(proj_dir, exist_ok=True)
    enforcer = FilesystemSecurityEnforcer([proj_dir])
    ws = DeveloperWorkstation(enforcer)
    dispatcher = CommandDispatcher(enforcer)
    try:
        yield {
            "sandbox": sandbox_base,
            "dir": proj_dir,
            "enforcer": enforcer,
            "ws": ws,
            "dispatcher": dispatcher
        }
    finally:
        shutil.rmtree(sandbox_base, ignore_errors=True)


def test_workstation_rejects_unallowed_path(test_env):
    forbidden = os.path.join(test_env["sandbox"], "forbidden")
    os.makedirs(forbidden, exist_ok=True)
    ws = test_env["ws"]
    with pytest.raises(SecurityPolicyException):
        ws.git_status(str(forbidden))



def test_workstation_git_status_non_repo(test_env):
    ws = test_env["ws"]
    proj_dir = test_env["dir"]
    res = ws.git_status(proj_dir)
    assert res["is_repo"] is False
    assert "Not a Git repository" in res["message"]


def test_workstation_git_workflow(test_env):
    proj_dir = test_env["dir"]
    ws = test_env["ws"]

    import subprocess
    # Init git repo
    subprocess.run(["git", "init"], cwd=proj_dir, check=True, capture_output=True)
    subprocess.run(["git", "config", "user.name", "Test User"], cwd=proj_dir, check=True)
    subprocess.run(["git", "config", "user.email", "test@test.local"], cwd=proj_dir, check=True)

    # Initial status
    status = ws.git_status(proj_dir)
    assert status["is_repo"] is True
    assert status["clean"] is True

    # Create file
    test_file = os.path.join(proj_dir, "hello.txt")
    with open(test_file, "w") as f:
        f.write("Line 1\nLine 2\n")

    status = ws.git_status(proj_dir)
    assert status["clean"] is False
    assert len(status["untracked"]) > 0

    # Add and commit
    subprocess.run(["git", "add", "."], cwd=proj_dir, check=True)
    status = ws.git_status(proj_dir)
    assert len(status["staged"]) > 0

    subprocess.run(["git", "commit", "-m", "Initial commit"], cwd=proj_dir, check=True, capture_output=True)
    status = ws.git_status(proj_dir)
    assert status["clean"] is True
    assert status["latest_commit"] is not None
    assert status["latest_commit"]["message"] == "Initial commit"

    # Modify file and check diff
    with open(test_file, "a") as f:
        f.write("Line 3 added\n")

    diff_res = ws.git_diff(proj_dir)
    assert "+Line 3 added" in diff_res["diff"]
    assert diff_res["files_changed"] >= 1
    assert diff_res["insertions"] >= 1


def test_workstation_bounded_build(test_env):
    proj_dir = test_env["dir"]
    ws = test_env["ws"]

    # Safe command
    res = ws.build_project(proj_dir, task_type="custom", custom_cmd="echo 'Building project...'")
    assert res["success"] is True
    assert res["exit_code"] == 0
    assert "Building project..." in res["stdout"]

    # Dangerous command should be rejected
    with pytest.raises(SecurityPolicyException):
        ws.build_project(proj_dir, task_type="custom", custom_cmd="echo 'test' && rm -rf /")


def test_workstation_clipboard(test_env):
    ws = test_env["ws"]
    set_res = ws.set_clipboard("RemoteForge Clipboard Sync Test")
    assert "successfully" in set_res.get("status", "")

    get_res = ws.get_clipboard()
    assert "RemoteForge Clipboard Sync Test" in get_res.get("text", "")


@pytest.mark.asyncio
async def test_dispatcher_workstation_integration(test_env):
    dispatcher = test_env["dispatcher"]
    proj_dir = test_env["dir"]

    # Test git_status dispatch
    res = await dispatcher.dispatch("git_status", {"path": proj_dir})
    assert res["success"] is True
    assert "is_repo" in res["result"]

    # Test clipboard dispatch
    c_res = await dispatcher.dispatch("set_clipboard", {"text": "WS Dispatcher Clipboard"})
    assert c_res["success"] is True

    g_res = await dispatcher.dispatch("get_clipboard", {})
    assert g_res["success"] is True
    assert "WS Dispatcher Clipboard" in g_res["result"]["text"]
