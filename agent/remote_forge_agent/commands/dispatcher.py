import sys
import subprocess
import re
import os
import time
from typing import Dict, Any, Callable
from agent.remote_forge_agent.system.status import get_system_status_info
from agent.remote_forge_agent.system.screenshot import capture_screenshot
from agent.remote_forge_agent.system.input_control import input_service
from agent.remote_forge_agent.filesystem.scanner import UserFilesystemService
from agent.remote_forge_agent.filesystem.transfer import ChunkedTransferHandler
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer, SecurityPolicyException


class CommandDispatcher:
    def __init__(self, enforcer: FilesystemSecurityEnforcer):
        self.enforcer = enforcer
        self.fs_service = UserFilesystemService(enforcer)
        self.transfer_handler = ChunkedTransferHandler(enforcer)
        self.registry: Dict[str, Callable[[Dict[str, Any]], Dict[str, Any]]] = {
            "get_system_status": self._handle_system_status,
            "take_screenshot": self._handle_screenshot,
            "send_input_event": self._handle_input_event,
            "lock_pc": self._handle_lock_pc,
            "get_allowed_directories": self._handle_get_allowed_directories,
            "add_allowed_directory": self._handle_add_allowed_directory,
            "list_directory": self._handle_list_directory,
            "search_files": self._handle_search_files,
            "get_file_metadata": self._handle_get_file_metadata,
            "download_file_chunk": self._handle_download_file_chunk,
            "upload_file_chunk": self._handle_upload_file_chunk,
            "cancel_file_upload": self._handle_cancel_file_upload,
            "get_file_hash": self._handle_get_file_hash,
            "zip_directory": self._handle_zip_directory,
            "open_application": self._handle_open_application,
            "close_application": self._handle_close_application,
            "minimize_application": self._handle_minimize_application,
            "run_command": self._handle_run_command,
            "say_text": self._handle_say_text,
            "play_alert": self._handle_play_alert,
            "show_notification": self._handle_show_notification,
            "set_volume": self._handle_set_volume,
            "browser_search": self._handle_browser_search,
            "open_browser": self._handle_open_browser,
            "restart_pc": self._handle_restart_pc,
            "shutdown_pc": self._handle_shutdown_pc
        }

    async def dispatch(self, tool: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        handler = self.registry.get(tool)
        if not handler:
            return {
                "success": False,
                "error": f"Tool '{tool}' is not supported on this PC agent.",
                "result": None,
                "execution_duration_ms": 0.0
            }

        start_t = time.perf_counter()
        try:
            result = handler(arguments)
            exec_ms = round((time.perf_counter() - start_t) * 1000, 2)
            return {
                "success": True,
                "result": result,
                "error": None,
                "execution_duration_ms": exec_ms
            }
        except SecurityPolicyException as se:
            exec_ms = round((time.perf_counter() - start_t) * 1000, 2)
            return {
                "success": False,
                "error": str(se),
                "result": None,
                "execution_duration_ms": exec_ms
            }
        except Exception as e:
            exec_ms = round((time.perf_counter() - start_t) * 1000, 2)
            return {
                "success": False,
                "error": f"Execution error: {str(e)}",
                "result": None,
                "execution_duration_ms": exec_ms
            }

    def _handle_system_status(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return get_system_status_info()

    def _handle_screenshot(self, args: Dict[str, Any]) -> Dict[str, Any]:
        quality = args.get("quality", 70)
        mode = args.get("mode", "preview")
        max_width = args.get("max_width", 1280)
        return capture_screenshot(quality=quality, mode=mode, max_width=max_width)

    def _handle_input_event(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return input_service.process_input(args)

    def _handle_lock_pc(self, args: Dict[str, Any]) -> Dict[str, Any]:
        if sys.platform == "darwin":
            subprocess.run(["pmset", "displaysleepnow"])
        elif sys.platform == "win32":
            subprocess.run(["rundll32.exe", "user32.dll,LockWorkStation"])
        else:
            subprocess.run(["xdg-screensaver", "lock"])
        return {"status": "locked"}

    def _handle_get_allowed_directories(self, args: Dict[str, Any]) -> Dict[str, Any]:
        roots = self.enforcer.get_allowed_roots()
        return {"allowed_directories": roots}

    def _handle_add_allowed_directory(self, args: Dict[str, Any]) -> Dict[str, Any]:
        target_path = args.get("path")
        if not target_path:
            raise ValueError("Missing 'path' argument.")
        canonical = self.enforcer.add_allowed_root(target_path)
        return {"added_canonical_path": canonical, "all_allowed": self.enforcer.get_allowed_roots()}

    def _handle_list_directory(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path")
        return self.fs_service.list_directory(path)

    def _handle_search_files(self, args: Dict[str, Any]) -> Dict[str, Any]:
        query = args.get("query", "")
        directory = args.get("directory")
        return self.fs_service.search_files(query, start_directory=directory)

    def _handle_get_file_metadata(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path")
        if not path:
            raise ValueError("Missing 'path' argument.")
        return self.fs_service.get_file_metadata(path)

    def _handle_open_application(self, args: Dict[str, Any]) -> Dict[str, Any]:
        raw_name = str(args.get("app_name") or "").strip().strip("'\"")
        if not raw_name:
            raise ValueError("Missing 'app_name' argument.")
        cleaned = re.sub(r"\s+(?:app|application|please|now)$", "", raw_name, flags=re.IGNORECASE).strip()
        aliases = {
            "chrome": "Google Chrome",
            "google chrome": "Google Chrome",
            "googlechrome": "Google Chrome",
            "google-chrome": "Google Chrome",
            "safari": "Safari",
            "firefox": "Firefox",
            "vscode": "Visual Studio Code",
            "vs code": "Visual Studio Code",
            "code": "Visual Studio Code",
            "terminal": "Terminal",
            "iterm": "iTerm2",
            "iterm2": "iTerm2",
            "finder": "Finder",
            "notes": "Notes",
            "music": "Music",
            "spotify": "Spotify",
            "slack": "Slack",
            "discord": "Discord",
            "calculator": "Calculator",
            "preview": "Preview",
            "settings": "System Settings",
            "system settings": "System Settings",
            "gmail": "Google Chrome"
        }
        canonical = aliases.get(cleaned.lower(), cleaned)
        if sys.platform == "darwin":
            subprocess.run(["open", "-a", canonical])
        elif sys.platform == "win32":
            subprocess.run(["start", canonical], shell=True)
        else:
            subprocess.run([canonical])
        return {"status": f"Application '{canonical}' launch requested."}

    def _handle_close_application(self, args: Dict[str, Any]) -> Dict[str, Any]:
        raw_name = str(args.get("app_name") or "").strip().strip("'\"")
        if not raw_name:
            raise ValueError("Missing 'app_name' argument.")

        # Strip trailing words like 'or minimize it', 'app', 'application', 'window', 'please'
        cleaned = re.sub(r"\s+(?:or\s+minimize\s+(?:it|window)|app|application|window|please|now)$", "", raw_name, flags=re.IGNORECASE).strip()
        aliases = {
            "chrome": "Google Chrome",
            "google chrome": "Google Chrome",
            "google-chrome": "Google Chrome",
            "safari": "Safari",
            "firefox": "Firefox",
            "vscode": "Visual Studio Code",
            "vs code": "Visual Studio Code",
            "code": "Visual Studio Code",
            "terminal": "Terminal",
            "iterm": "iTerm2",
            "iterm2": "iTerm2",
            "finder": "Finder",
            "notes": "Notes",
            "music": "Music",
            "spotify": "Spotify",
            "slack": "Slack",
            "discord": "Discord",
            "calculator": "Calculator",
            "preview": "Preview",
            "settings": "System Settings",
            "system settings": "System Settings"
        }
        canonical = aliases.get(cleaned.lower(), cleaned)

        if sys.platform == "darwin":
            try:
                # 1. Graceful AppleScript quit
                res = subprocess.run(["osascript", "-e", f'tell application "{canonical}" to quit'], capture_output=True, text=True, timeout=3)
                # 2. If AppleScript returned error, force kill with pkill
                if res.returncode != 0:
                    subprocess.run(["pkill", "-i", "-f", canonical], capture_output=True, text=True)
                    if cleaned != canonical:
                        subprocess.run(["pkill", "-i", "-f", cleaned], capture_output=True, text=True)
            except Exception:
                subprocess.run(["pkill", "-i", "-f", canonical], capture_output=True, text=True)
        elif sys.platform == "win32":
            subprocess.run(["taskkill", "/IM", f"{canonical}.exe", "/F"], capture_output=True, text=True)
        else:
            subprocess.run(["pkill", "-i", "-f", canonical], capture_output=True, text=True)

        return {"status": f"Application '{canonical}' closed successfully."}

    def _handle_minimize_application(self, args: Dict[str, Any]) -> Dict[str, Any]:
        raw_name = str(args.get("app_name") or "").strip().strip("'\"")
        if not raw_name:
            raise ValueError("Missing 'app_name' argument.")

        cleaned = re.sub(r"\s+(?:or\s+close\s+(?:it|window)|app|application|window|please|now)$", "", raw_name, flags=re.IGNORECASE).strip()
        aliases = {
            "chrome": "Google Chrome",
            "google chrome": "Google Chrome",
            "safari": "Safari",
            "firefox": "Firefox",
            "vscode": "Visual Studio Code",
            "code": "Visual Studio Code",
            "terminal": "Terminal",
            "finder": "Finder"
        }
        canonical = aliases.get(cleaned.lower(), cleaned)

        if sys.platform == "darwin":
            script = f'''
            tell application "System Events"
                if exists (first process whose name is "{canonical}") then
                    try
                        tell application "{canonical}" to set miniaturized of windows to true
                    on error
                        set visible of (first process whose name is "{canonical}") to false
                    end try
                end if
            end tell
            '''
            try:
                subprocess.run(["osascript", "-e", script], capture_output=True, text=True, timeout=3)
            except Exception:
                pass
        return {"status": f"Application '{canonical}' minimized successfully."}

    def _handle_run_command(self, args: Dict[str, Any]) -> Dict[str, Any]:
        cmd = args.get("command") or args.get("cmd") or ""
        if not cmd.strip():
            raise ValueError("Missing 'command' argument.")
        res = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=15)
        output = (res.stdout or "") + (res.stderr or "")
        if not output.strip():
            output = f"Command finished with return code {res.returncode}"
        return {
            "command": cmd,
            "output": output.strip()[:2000],
            "exit_code": res.returncode
        }

    def _handle_say_text(self, args: Dict[str, Any]) -> Dict[str, Any]:
        text = args.get("text") or "Hello from RemoteForge!"
        if sys.platform == "darwin":
            subprocess.run(["say", text])
        return {"status": f"Spoke: '{text}'"}

    def _handle_play_alert(self, args: Dict[str, Any]) -> Dict[str, Any]:
        import os
        sound = args.get("sound") or "Ping"
        if sys.platform == "darwin":
            sound_path = f"/System/Library/Sounds/{sound}.aiff"
            if os.path.exists(sound_path):
                subprocess.run(["afplay", sound_path])
            else:
                subprocess.run(["afplay", "/System/Library/Sounds/Ping.aiff"])
        return {"status": "Played alert chime."}

    def _handle_show_notification(self, args: Dict[str, Any]) -> Dict[str, Any]:
        message = str(args.get("message") or "RemoteForge Notification").replace('"', '\\"')
        title = str(args.get("title") or "RemoteForge").replace('"', '\\"')
        if sys.platform == "darwin":
            # 1. Native banner notification with sound
            script = f'display notification "{message}" with title "{title}" sound name "Ping"'
            subprocess.run(["osascript", "-e", script], capture_output=True, text=True)
            # 2. Prominent non-blocking dialog alert so user sees it on desktop even if notifications are suppressed
            dialog_script = f'display alert "{title}" message "{message}" as informational'
            subprocess.Popen(["osascript", "-e", dialog_script])
        elif sys.platform == "win32":
            subprocess.Popen(["powershell", "-Command", f'[System.Windows.Forms.MessageBox]::Show("{message}", "{title}")'])
        else:
            subprocess.run(["notify-send", title, message])
        return {"status": f"Desktop alert '{title}: {message}' displayed."}

    def _handle_set_volume(self, args: Dict[str, Any]) -> Dict[str, Any]:
        try:
            level = int(args.get("level", 50))
        except (ValueError, TypeError):
            level = 50
        level = max(0, min(100, level))
        if sys.platform == "darwin":
            subprocess.run(["osascript", "-e", f"set volume output volume {level}"])
        return {"status": f"System volume set to {level}%."}

    def _handle_open_browser(self, args: Dict[str, Any]) -> Dict[str, Any]:
        import webbrowser
        url = args.get("url", "https://google.com")
        browser_choice = str(args.get("browser", "")).lower()
        if sys.platform == "darwin" and ("chrome" in browser_choice or "google" in browser_choice):
            subprocess.run(["open", "-a", "Google Chrome", url])
        else:
            webbrowser.open(url)
        return {"status": f"Opened browser at {url}"}

    def _handle_browser_search(self, args: Dict[str, Any]) -> Dict[str, Any]:
        import webbrowser
        import urllib.parse
        query = args.get("query", "")
        site = args.get("site", "")
        if site.lower() == "youtube":
            url = f"https://www.youtube.com/results?search_query={urllib.parse.quote_plus(query)}"
        else:
            url = f"https://www.google.com/search?q={urllib.parse.quote_plus(query)}"
        webbrowser.open(url)
        return {"status": f"Search launched: {url}"}

    def _handle_download_file_chunk(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path")
        if not path:
            raise ValueError("Missing 'path' argument.")
        offset = int(args.get("offset", 0))
        length = int(args.get("length", 256 * 1024))
        return self.transfer_handler.read_chunk(path, offset=offset, length=length)

    def _handle_upload_file_chunk(self, args: Dict[str, Any]) -> Dict[str, Any]:
        target_dir = args.get("target_directory")
        filename = args.get("filename")
        transfer_id = args.get("transfer_id")
        chunk_data = args.get("chunk_data_base64", "")
        offset = int(args.get("offset", 0))
        is_first = bool(args.get("is_first", False))
        is_last = bool(args.get("is_last", False))

        if not target_dir or not filename or not transfer_id:
            raise ValueError("Missing required arguments for upload chunk.")

        return self.transfer_handler.write_chunk(
            target_directory=target_dir,
            filename=filename,
            transfer_id=transfer_id,
            chunk_data_base64=chunk_data,
            offset=offset,
            is_first=is_first,
            is_last=is_last
        )

    def _handle_cancel_file_upload(self, args: Dict[str, Any]) -> Dict[str, Any]:
        transfer_id = args.get("transfer_id")
        if transfer_id:
            self.transfer_handler.cancel_upload(transfer_id)
        return {"status": "Upload cancelled."}

    def _handle_get_file_hash(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path")
        if not path:
            raise ValueError("Missing 'path' argument.")
        return self.transfer_handler.get_file_hash(path)

    def _handle_zip_directory(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path") or args.get("target_directory")
        if not path:
            raise ValueError("Missing 'path' or 'target_directory' argument.")
        return self.transfer_handler.zip_directory(path)

    def _handle_restart_pc(self, args: Dict[str, Any]) -> Dict[str, Any]:
        # High-risk confirmed operation
        return {"status": "Simulated restart PC (High risk safety mock). To execute live: shutdown -r"}

    def _handle_shutdown_pc(self, args: Dict[str, Any]) -> Dict[str, Any]:
        # High-risk confirmed operation
        return {"status": "Simulated shutdown PC (High risk safety mock). To execute live: shutdown -h"}
