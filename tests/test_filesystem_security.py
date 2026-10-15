import os
import shutil
import sys
import pytest
from pathlib import Path
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer, SecurityPolicyException

@pytest.fixture
def test_sandbox():
    sandbox_base = os.path.expanduser("~/.remoteforge_security_test_sandbox")
    if os.path.exists(sandbox_base):
        shutil.rmtree(sandbox_base, ignore_errors=True)
    os.makedirs(sandbox_base, exist_ok=True)

    try:
        # Create simulated user directories
        docs = os.path.join(sandbox_base, "Documents")
        downloads = os.path.join(sandbox_base, "Downloads")
        desktop = os.path.join(sandbox_base, "Desktop")
        os.makedirs(docs, exist_ok=True)
        os.makedirs(downloads, exist_ok=True)
        os.makedirs(desktop, exist_ok=True)

        # Create sample user files
        doc_file = os.path.join(docs, "test.pdf")
        with open(doc_file, "w") as f:
            f.write("sample pdf content")

        dl_file = os.path.join(downloads, "test.zip")
        with open(dl_file, "w") as f:
            f.write("sample zip content")

        desk_file = os.path.join(desktop, "file.txt")
        with open(desk_file, "w") as f:
            f.write("sample txt content")

        # Create sensitive credentials inside and outside
        ssh_dir = os.path.join(docs, ".ssh")
        os.makedirs(ssh_dir, exist_ok=True)
        id_rsa = os.path.join(ssh_dir, "id_rsa")
        with open(id_rsa, "w") as f:
            f.write("PRIVATE KEY")

        aws_creds = os.path.join(docs, ".aws", "credentials")
        os.makedirs(os.path.dirname(aws_creds), exist_ok=True)
        with open(aws_creds, "w") as f:
            f.write("aws secret key")

        # Create an illegal symlink pointing to root or /etc or /System
        symlink_escape = os.path.join(docs, "escape_link")
        target_restricted = "/etc" if sys.platform != "win32" else "C:\\Windows"
        try:
            os.symlink(target_restricted, symlink_escape)
        except (OSError, NotImplementedError):
            pass

        yield {
            "root": sandbox_base,
            "docs": docs,
            "downloads": downloads,
            "desktop": desktop,
            "doc_file": doc_file,
            "dl_file": dl_file,
            "desk_file": desk_file,
            "id_rsa": id_rsa,
            "aws_creds": aws_creds,
            "symlink_escape": symlink_escape if os.path.islink(symlink_escape) else None
        }
    finally:
        if os.path.exists(sandbox_base):
            shutil.rmtree(sandbox_base, ignore_errors=True)

def test_allowed_user_files(test_sandbox):
    enforcer = FilesystemSecurityEnforcer(custom_roots=[
        test_sandbox["docs"],
        test_sandbox["downloads"],
        test_sandbox["desktop"]
    ])

    # 1. ~/Documents/test.pdf -> ALLOW
    allowed, canon, msg = enforcer.validate_file_access(test_sandbox["doc_file"])
    assert allowed is True
    assert canon == os.path.realpath(test_sandbox["doc_file"])

    # 2. ~/Downloads/test.zip -> ALLOW
    allowed, canon, msg = enforcer.validate_file_access(test_sandbox["dl_file"])
    assert allowed is True
    assert canon == os.path.realpath(test_sandbox["dl_file"])

    # 3. ~/Desktop/file.txt -> ALLOW
    allowed, canon, msg = enforcer.validate_file_access(test_sandbox["desk_file"])
    assert allowed is True
    assert canon == os.path.realpath(test_sandbox["desk_file"])

def test_system_directories_denied(test_sandbox):
    enforcer = FilesystemSecurityEnforcer(custom_roots=[test_sandbox["docs"]])

    # 4. /System/... -> DENY
    allowed, _, _ = enforcer.validate_file_access("/System/Library/CoreServices")
    assert allowed is False

    # 5. /Library/... -> DENY
    allowed, _, _ = enforcer.validate_file_access("/Library/Preferences")
    assert allowed is False

    # 6. /etc/... -> DENY
    allowed, _, _ = enforcer.validate_file_access("/etc/passwd")
    assert allowed is False

def test_credentials_and_secrets_denied(test_sandbox):
    enforcer = FilesystemSecurityEnforcer(custom_roots=[test_sandbox["docs"]])

    # 7. ~/.ssh/id_rsa -> DENY (even if inside allowed folder)
    allowed, _, msg = enforcer.validate_file_access(test_sandbox["id_rsa"])
    assert allowed is False
    assert "sensitive credential" in msg.lower() or "denied" in msg.lower()

    # 8. ~/.aws/credentials -> DENY
    allowed, _, msg = enforcer.validate_file_access(test_sandbox["aws_creds"])
    assert allowed is False
    assert "sensitive credential" in msg.lower() or "denied" in msg.lower()

def test_path_traversal_and_symlinks(test_sandbox):
    enforcer = FilesystemSecurityEnforcer(custom_roots=[test_sandbox["docs"]])

    # 9. ../../Library/... -> DENY
    traversal_path = os.path.join(test_sandbox["docs"], "..", "..", "Library", "Preferences")
    allowed, _, _ = enforcer.validate_file_access(traversal_path)
    assert allowed is False

    # 10. symlink escaping to system -> DENY
    if test_sandbox["symlink_escape"]:
        allowed, _, msg = enforcer.validate_file_access(test_sandbox["symlink_escape"])
        assert allowed is False
        assert "outside allowed" in msg.lower() or "restricted" in msg.lower()

    # 11. arbitrary absolute path outside allowed roots -> DENY
    allowed, _, msg = enforcer.validate_file_access("/var/log/system.log")
    assert allowed is False
    assert "denied" in msg.lower()

def test_file_hash_and_zip_directory(test_sandbox):
    import hashlib
    from agent.remote_forge_agent.filesystem.transfer import ChunkedTransferHandler
    enforcer = FilesystemSecurityEnforcer(custom_roots=[
        test_sandbox["docs"],
        test_sandbox["downloads"]
    ])
    handler = ChunkedTransferHandler(enforcer)

    # 1. Hashing allowed file
    hash_res = handler.get_file_hash(test_sandbox["doc_file"])
    assert "sha256" in hash_res
    expected_hash = hashlib.sha256(b"sample pdf content").hexdigest()
    assert hash_res["sha256"] == expected_hash

    # 2. Hashing sensitive file -> Denied
    with pytest.raises(SecurityPolicyException):
        handler.get_file_hash(test_sandbox["id_rsa"])

    # 3. Zip Directory
    zip_res = handler.zip_directory(test_sandbox["docs"])
    assert "zip_path" in zip_res
    assert os.path.exists(zip_res["zip_path"])
    assert zip_res["filename"].endswith(".zip")
    assert zip_res["file_size"] > 0
    # Clean up test zip
    if os.path.exists(zip_res["zip_path"]):
        os.remove(zip_res["zip_path"])

