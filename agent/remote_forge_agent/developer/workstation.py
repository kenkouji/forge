import os
import sys
import subprocess
import time
import collections
from typing import Dict, Any, Optional, List
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer, SecurityPolicyException


class DeveloperWorkstation:
    """
    Developer workstation utilities for RemoteForge 2.0.
    Handles Git status/diff, bounded project builds, and clipboard operations
    under strict filesystem allowlist enforcement.
    """

    def __init__(self, enforcer: FilesystemSecurityEnforcer):
        self.enforcer = enforcer

    def _resolve_project_path(self, path: Optional[str]) -> str:
        if path and path.strip():
            target = os.path.abspath(os.path.expanduser(path.strip()))
        elif self.enforcer.allowed_roots:
            target = sorted(list(self.enforcer.allowed_roots))[0]
        else:
            target = os.getcwd()
        self.enforcer.assert_path_allowed(target)
        if not os.path.exists(target):
            raise FileNotFoundError(f"Project path does not exist: {target}")
        if not os.path.isdir(target):
            raise NotADirectoryError(f"Project path must be a directory: {target}")
        return target

    def git_status(self, path: Optional[str] = None) -> Dict[str, Any]:
        target = self._resolve_project_path(path)
        # Check if inside git work tree
        try:
            check = subprocess.run(
                ["git", "rev-parse", "--is-inside-work-tree"],
                cwd=target,
                capture_output=True,
                text=True,
                timeout=5
            )
            if check.returncode != 0 or check.stdout.strip() != "true":
                return {
                    "is_repo": False,
                    "path": target,
                    "message": "Not a Git repository."
                }
        except Exception as e:
            return {
                "is_repo": False,
                "path": target,
                "message": f"Git detection failed: {e}"
            }

        # Run git status --porcelain=v1 -b
        st = subprocess.run(
            ["git", "status", "--porcelain=v1", "-b"],
            cwd=target,
            capture_output=True,
            text=True,
            timeout=10
        )
        lines = st.stdout.splitlines()
        branch_line = lines[0] if lines else "## unknown"
        status_lines = lines[1:] if len(lines) > 1 else []

        # Parse branch line e.g. ## main...origin/main [ahead 1, behind 2]
        branch_name = "HEAD"
        tracking = None
        ahead = 0
        behind = 0
        if branch_line.startswith("## "):
            branch_info = branch_line[3:].strip()
            if "..." in branch_info:
                parts = branch_info.split("...")
                branch_name = parts[0]
                rest = parts[1]
                if "[" in rest:
                    tracking = rest.split("[")[0].strip()
                    extra = rest[rest.find("[") + 1 : rest.find("]")]
                    for item in extra.split(","):
                        item = item.strip()
                        if "ahead" in item:
                            try:
                                ahead = int(item.replace("ahead", "").strip())
                            except ValueError:
                                pass
                        elif "behind" in item:
                            try:
                                behind = int(item.replace("behind", "").strip())
                            except ValueError:
                                pass
                else:
                    tracking = rest.strip()
            else:
                branch_name = branch_info

        staged: List[str] = []
        unstaged: List[str] = []
        untracked: List[str] = []

        for line in status_lines:
            if len(line) < 3:
                continue
            index_status = line[0]
            work_status = line[1]
            filepath = line[3:].strip()

            if index_status == "?" and work_status == "?":
                untracked.append(filepath)
            else:
                if index_status not in (" ", "?"):
                    staged.append(f"{index_status} {filepath}")
                if work_status not in (" ", "?"):
                    unstaged.append(f"{work_status} {filepath}")

        # Get latest commit
        latest_commit = None
        try:
            log_res = subprocess.run(
                ["git", "log", "-1", "--pretty=format:%h\t%an\t%ar\t%s"],
                cwd=target,
                capture_output=True,
                text=True,
                timeout=5
            )
            if log_res.returncode == 0 and log_res.stdout.strip():
                parts = log_res.stdout.strip().split("\t")
                if len(parts) >= 4:
                    latest_commit = {
                        "hash": parts[0],
                        "author": parts[1],
                        "relative_time": parts[2],
                        "message": parts[3]
                    }
        except Exception:
            pass

        return {
            "is_repo": True,
            "path": target,
            "branch": branch_name,
            "tracking": tracking,
            "ahead": ahead,
            "behind": behind,
            "clean": len(staged) == 0 and len(unstaged) == 0 and len(untracked) == 0,
            "staged_count": len(staged),
            "unstaged_count": len(unstaged),
            "untracked_count": len(untracked),
            "staged": staged[:50],
            "unstaged": unstaged[:50],
            "untracked": untracked[:50],
            "latest_commit": latest_commit
        }

    def git_diff(
        self,
        path: Optional[str] = None,
        file_path: Optional[str] = None,
        staged: bool = False
    ) -> Dict[str, Any]:
        target = self._resolve_project_path(path)

        cmd = ["git", "diff"]
        if staged:
            cmd.append("--cached")
        if file_path:
            full_file = os.path.abspath(os.path.join(target, file_path))
            self.enforcer.assert_path_allowed(full_file)
            cmd.extend(["--", file_path])

        diff_res = subprocess.run(
            cmd,
            cwd=target,
            capture_output=True,
            text=True,
            timeout=10
        )

        full_diff = diff_res.stdout or ""
        lines = full_diff.splitlines()
        max_lines = 160
        max_chars = 4000
        truncated = False

        if len(lines) > max_lines:
            truncated = True
            selected_lines = lines[:max_lines]
            diff_text = "\n".join(selected_lines) + f"\n\n[... Truncated {len(lines) - max_lines} remaining lines ...]"
        elif len(full_diff) > max_chars:
            truncated = True
            diff_text = full_diff[:max_chars] + "\n\n[... Truncated remaining diff characters ...]"
        else:
            diff_text = full_diff

        # Stat command for additions / deletions count
        stat_cmd = ["git", "diff", "--shortstat"]
        if staged:
            stat_cmd.append("--cached")
        if file_path:
            stat_cmd.extend(["--", file_path])

        stat_res = subprocess.run(
            stat_cmd,
            cwd=target,
            capture_output=True,
            text=True,
            timeout=5
        )
        stat_output = stat_res.stdout.strip()
        files_changed = 0
        insertions = 0
        deletions = 0
        if stat_output:
            import re
            m_fc = re.search(r"(\d+)\s+file", stat_output)
            m_ins = re.search(r"(\d+)\s+insertion", stat_output)
            m_del = re.search(r"(\d+)\s+deletion", stat_output)
            if m_fc:
                files_changed = int(m_fc.group(1))
            if m_ins:
                insertions = int(m_ins.group(1))
            if m_del:
                deletions = int(m_del.group(1))

        return {
            "path": target,
            "staged": staged,
            "file_path": file_path,
            "diff": diff_text if diff_text.strip() else "No differences found.",
            "truncated": truncated,
            "files_changed": files_changed,
            "insertions": insertions,
            "deletions": deletions
        }

    def build_project(
        self,
        project_path: Optional[str] = None,
        task_type: str = "build",
        custom_cmd: Optional[str] = None,
        timeout_seconds: int = 60
    ) -> Dict[str, Any]:
        target = self._resolve_project_path(project_path)
        timeout_seconds = max(5, min(180, int(timeout_seconds)))

        # Resolve command based on project files if not explicitly custom
        cmd: Optional[str] = None
        task_lower = task_type.lower().strip()

        if custom_cmd and custom_cmd.strip():
            # Security sanitization: forbid dangerous patterns
            c = custom_cmd.strip()
            dangerous = [";", "&&", "||", "`", "$(", ">", ">>", "rm -rf /", "mkfs", "dd "]
            for pat in dangerous:
                if pat in c:
                    raise SecurityPolicyException(f"Custom build command contains forbidden character or pattern: '{pat}'")
            cmd = c
        else:
            # Auto-detect build tool in target directory
            has_gradlew = os.path.exists(os.path.join(target, "gradlew")) or os.path.exists(os.path.join(target, "gradlew.bat"))
            has_package_json = os.path.exists(os.path.join(target, "package.json"))
            has_cargo = os.path.exists(os.path.join(target, "Cargo.toml"))
            has_pytest = os.path.exists(os.path.join(target, "pytest.ini")) or os.path.exists(os.path.join(target, "setup.py")) or os.path.exists(os.path.join(target, "pyproject.toml"))
            has_makefile = os.path.exists(os.path.join(target, "Makefile"))

            if has_gradlew:
                wrapper = "./gradlew" if sys.platform != "win32" else "gradlew.bat"
                if task_lower == "test":
                    cmd = f"{wrapper} test"
                elif task_lower == "clean":
                    cmd = f"{wrapper} clean"
                else:
                    cmd = f"{wrapper} assembleDebug"
            elif has_package_json:
                if task_lower == "test":
                    cmd = "npm test"
                elif task_lower == "clean":
                    cmd = "npm run clean"
                else:
                    cmd = "npm run build"
            elif has_cargo:
                if task_lower == "test":
                    cmd = "cargo test"
                elif task_lower == "clean":
                    cmd = "cargo clean"
                else:
                    cmd = "cargo build"
            elif has_pytest:
                if task_lower == "test":
                    cmd = "pytest"
                else:
                    cmd = "python -m py_compile"
            elif has_makefile:
                cmd = f"make {task_lower}" if task_lower != "build" else "make"
            else:
                cmd = "echo 'No recognized build manifest (gradlew, package.json, Cargo.toml, Makefile).'"

        # Execute bounded command
        start_t = time.perf_counter()
        try:
            proc = subprocess.Popen(
                cmd,
                cwd=target,
                shell=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                bufsize=1
            )
            stdout_lines = collections.deque(maxlen=200)
            stderr_lines = collections.deque(maxlen=200)

            try:
                stdout_data, stderr_data = proc.communicate(timeout=timeout_seconds)
                for line in stdout_data.splitlines():
                    stdout_lines.append(line)
                for line in stderr_data.splitlines():
                    stderr_lines.append(line)
                exit_code = proc.returncode
            except subprocess.TimeoutExpired:
                proc.kill()
                exit_code = -9
                stderr_lines.append(f"Build task timed out after {timeout_seconds} seconds.")

        except Exception as e:
            return {
                "command": cmd,
                "project_path": target,
                "success": False,
                "exit_code": -1,
                "error": str(e),
                "stdout": "",
                "stderr": str(e),
                "duration_ms": round((time.perf_counter() - start_t) * 1000, 2)
            }

        duration_ms = round((time.perf_counter() - start_t) * 1000, 2)
        return {
            "command": cmd,
            "project_path": target,
            "success": exit_code == 0,
            "exit_code": exit_code,
            "stdout": "\n".join(stdout_lines),
            "stderr": "\n".join(stderr_lines),
            "line_count": len(stdout_lines) + len(stderr_lines),
            "duration_ms": duration_ms
        }

    def get_clipboard(self) -> Dict[str, Any]:
        """Read system clipboard without storing or logging contents."""
        try:
            if sys.platform == "darwin":
                res = subprocess.run(["pbpaste"], capture_output=True, text=True, timeout=3)
                content = res.stdout if res.returncode == 0 else ""
            elif sys.platform == "win32":
                res = subprocess.run(
                    ["powershell", "-command", "Get-Clipboard"],
                    capture_output=True,
                    text=True,
                    timeout=3
                )
                content = res.stdout if res.returncode == 0 else ""
            else:
                res = subprocess.run(["xclip", "-o", "-selection", "clipboard"], capture_output=True, text=True, timeout=3)
                content = res.stdout if res.returncode == 0 else ""

            # Cap content size to 10,000 characters
            bounded = content[:10000]
            return {
                "text": bounded,
                "length": len(content),
                "truncated": len(content) > 10000
            }
        except Exception as e:
            return {
                "text": "",
                "length": 0,
                "error": str(e)
            }

    def set_clipboard(self, text: str) -> Dict[str, Any]:
        """Write text to system clipboard."""
        if text is None:
            text = ""
        # Cap text to 50,000 chars for safety
        clean_text = str(text)[:50000]
        try:
            if sys.platform == "darwin":
                p = subprocess.Popen(["pbcopy"], stdin=subprocess.PIPE)
                p.communicate(input=clean_text.encode("utf-8"), timeout=3)
            elif sys.platform == "win32":
                p = subprocess.Popen(["powershell", "-command", "Set-Clipboard"], stdin=subprocess.PIPE)
                p.communicate(input=clean_text.encode("utf-8"), timeout=3)
            else:
                p = subprocess.Popen(["xclip", "-selection", "clipboard"], stdin=subprocess.PIPE)
                p.communicate(input=clean_text.encode("utf-8"), timeout=3)

            return {
                "status": "Clipboard updated successfully.",
                "characters": len(clean_text)
            }
        except Exception as e:
            return {
                "status": "Failed to update clipboard.",
                "error": str(e)
            }
