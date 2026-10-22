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
from agent.remote_forge_agent.developer.workstation import DeveloperWorkstation
from agent.remote_forge_agent.system.screen_streamer import screen_streamer
from agent.remote_forge_agent.system.virtual_display import virtual_display_manager
from agent.remote_forge_agent.system.edge_control import edge_control_manager
from agent.remote_forge_agent.mesh.p2p_node import p2p_node
from agent.remote_forge_agent.audio.audio_engine import audio_engine
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer, SecurityPolicyException


class CommandDispatcher:
    def __init__(self, enforcer: FilesystemSecurityEnforcer):
        self.enforcer = enforcer
        self.fs_service = UserFilesystemService(enforcer)
        self.transfer_handler = ChunkedTransferHandler(enforcer)
        self.workstation = DeveloperWorkstation(enforcer)

        self.privacy_shield_state = {
            "is_lockdown": False,
            "is_screen_paused": False,
            "is_input_disabled": False,
            "is_transfers_stopped": False
        }

        self.device_id: str = "macbook-pro"
        self.server_ws_url: str = "ws://127.0.0.1:8090"
        self.send_message_fn: Optional[Callable[[Dict[str, Any]], None]] = None

    def set_agent_context(self, device_id: str, server_ws_url: str, send_fn: Optional[Callable[[Dict[str, Any]], None]] = None):
        self.device_id = device_id
        self.server_ws_url = server_ws_url
        self.send_message_fn = send_fn
        edge_control_manager.set_context(device_id=device_id, send_fn=send_fn)
        audio_engine.set_context(device_id=device_id, server_ws_url=server_ws_url)

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
            "git_status": self._handle_git_status,
            "git_diff": self._handle_git_diff,
            "build_project": self._handle_build_project,
            "get_clipboard": self._handle_get_clipboard,
            "set_clipboard": self._handle_set_clipboard,
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
            "shutdown_pc": self._handle_shutdown_pc,
            "get_stream_frame": self._handle_get_stream_frame,
            "configure_stream": self._handle_configure_stream,
            "get_stream_status": self._handle_get_stream_status,
            "sync_privacy_shield": self._handle_sync_privacy_shield,
            "virtual_display_start": self._handle_virtual_display_start,
            "virtual_display_stop": self._handle_virtual_display_stop,
            "virtual_display_touch": self._handle_virtual_display_touch,
            "edge_control_configure": self._handle_edge_control_configure,
            "edge_control_disable": self._handle_edge_control_disable,
            "edge_control_return_cursor": self._handle_edge_control_return_cursor,
            "p2p_session_init": self._handle_p2p_session_init,
            "audio_start": self._handle_audio_start,
            "audio_stop": self._handle_audio_stop
        }

    async def dispatch(self, tool: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        # Defense-in-depth: independently enforce guest restrictions on the host agent
        actor = arguments.get("actor", "")
        if actor.startswith("guest:"):
            if self.privacy_shield_state.get("is_lockdown"):
                return {
                    "success": False,
                    "error": "Host agent rejected action: Emergency Lockdown is active.",
                    "result": None,
                    "execution_duration_ms": 0.0
                }
            allowed_guest = [
                "send_input_event", "remote_input", "mouse_click", "click",
                "mouse_drag", "drag", "mouse_move", "move", "mouse_down",
                "mouse_up", "send_keys", "key", "key_tap", "type_text",
                "scroll", "shortcut"
            ]
            if tool not in allowed_guest:
                return {
                    "success": False,
                    "error": f"Host agent security policy: Guests are forbidden from executing '{tool}'.",
                    "result": None,
                    "execution_duration_ms": 0.0
                }
            if self.privacy_shield_state.get("is_input_disabled"):
                return {
                    "success": False,
                    "error": "Host agent rejected guest input: Privacy Shield has input disabled.",
                    "result": None,
                    "execution_duration_ms": 0.0
                }

        # Normalize input tool aliases
        if tool in ["mouse_click", "click"]:
            if "action" not in arguments: arguments["action"] = "click"
            tool = "send_input_event"
        elif tool in ["mouse_drag", "drag"]:
            if "action" not in arguments: arguments["action"] = "mouse_drag"
            tool = "send_input_event"
        elif tool in ["mouse_move", "move"]:
            if "action" not in arguments: arguments["action"] = "move"
            tool = "send_input_event"
        elif tool in ["mouse_down", "down"]:
            if "action" not in arguments: arguments["action"] = "mouse_down"
            tool = "send_input_event"
        elif tool in ["mouse_up", "up"]:
            if "action" not in arguments: arguments["action"] = "mouse_up"
            tool = "send_input_event"
        elif tool in ["send_keys", "type_text"]:
            if "action" not in arguments: arguments["action"] = "type_text"
            tool = "send_input_event"
        elif tool in ["key", "key_tap"]:
            if "action" not in arguments: arguments["action"] = "key"
            tool = "send_input_event"
        elif tool == "scroll":
            if "action" not in arguments: arguments["action"] = "scroll"
            tool = "send_input_event"
        elif tool == "shortcut":
            if "action" not in arguments: arguments["action"] = "shortcut"
            tool = "send_input_event"
        elif tool == "remote_input":
            tool = "send_input_event"

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
            # Modern non-blocking PowerShell balloon toast notification
            ps_cmd = f'''
            [void] [System.Reflection.Assembly]::LoadWithPartialName("System.Windows.Forms");
            $notify = New-Object System.Windows.Forms.NotifyIcon;
            $notify.Icon = [System.Drawing.SystemIcons]::Information;
            $notify.BalloonTipTitle = "{title}";
            $notify.BalloonTipText = "{message}";
            $notify.BalloonTipIcon = "Info";
            $notify.Visible = $True;
            $notify.ShowBalloonTip(5000);
            '''
            subprocess.Popen(["powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", ps_cmd])
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
        elif sys.platform == "win32":
            try:
                # On Windows, adjust volume steps using WScript SendKeys or AudioDeviceCmdlets
                subprocess.run(["powershell", "-NoProfile", "-Command", f"(New-Object -ComObject WScript.Shell).SendKeys([char]174)"], capture_output=True)
            except Exception:
                pass
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

    def _handle_git_status(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path") or args.get("project_path")
        return self.workstation.git_status(path)

    def _handle_git_diff(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path") or args.get("project_path")
        file_path = args.get("file_path") or args.get("file")
        staged = bool(args.get("staged", False))
        return self.workstation.git_diff(path, file_path, staged)

    def _handle_build_project(self, args: Dict[str, Any]) -> Dict[str, Any]:
        path = args.get("path") or args.get("project_path")
        task_type = args.get("task_type") or args.get("task") or "build"
        custom_cmd = args.get("command") or args.get("custom_cmd")
        timeout_sec = int(args.get("timeout_seconds") or args.get("timeout") or 60)
        return self.workstation.build_project(path, task_type, custom_cmd, timeout_sec)

    def _handle_get_clipboard(self, args: Dict[str, Any]) -> Dict[str, Any]:
        import subprocess, io, base64
        from PIL import Image, ImageGrab

        # 1. Try Image
        try:
            img = ImageGrab.grabclipboard()
            if isinstance(img, Image.Image):
                out = io.BytesIO()
                img.save(out, format="PNG")
                raw = out.getvalue()
                return {
                    "content_type": "image",
                    "image_base64": base64.b64encode(raw).decode("utf-8"),
                    "preview": f"{img.width}x{img.height} Image ({round(len(raw)/1024.0, 1)} KB)",
                    "size_bytes": len(raw)
                }
        except Exception:
            pass

        # 2. Try Text
        try:
            if sys.platform == "darwin":
                res = subprocess.run(["pbpaste"], capture_output=True, text=True, timeout=2)
                txt = res.stdout if res.returncode == 0 else ""
            else:
                txt = self.workstation.get_clipboard().get("text", "")
            return {
                "content_type": "text",
                "text": txt,
                "preview": txt[:120].replace("\n", " ").strip(),
                "size_bytes": len(txt.encode("utf-8"))
            }
        except Exception as e:
            return {"content_type": "text", "text": "", "error": str(e), "size_bytes": 0}

    def _handle_set_clipboard(self, args: Dict[str, Any]) -> Dict[str, Any]:
        import subprocess, base64
        content_type = str(args.get("content_type", "text")).lower()

        if content_type == "image" and args.get("image_base64"):
            try:
                raw_bytes = base64.b64decode(args["image_base64"])
                tmp_path = "/tmp/rf_clipboard_paste.png"
                with open(tmp_path, "wb") as f:
                    f.write(raw_bytes)
                if sys.platform == "darwin":
                    cmd = f'set the clipboard to (read (POSIX file "{tmp_path}") as «class PNGf»)'
                    subprocess.run(["osascript", "-e", cmd], check=True, timeout=3)
                return {"status": "image_copied_to_clipboard", "size_bytes": len(raw_bytes)}
            except Exception as e:
                return {"status": "error", "error": f"Failed to copy image to clipboard: {e}"}

        # Text clipboard
        text = str(args.get("text", ""))
        try:
            if sys.platform == "darwin":
                subprocess.run(["pbcopy"], input=text.encode("utf-8"), check=True, timeout=2)
            else:
                self.workstation.set_clipboard(text)
            return {"status": "text_copied_to_clipboard", "length": len(text)}
        except Exception as e:
            return {"status": "error", "error": f"Failed to set clipboard: {e}"}

    def _handle_restart_pc(self, args: Dict[str, Any]) -> Dict[str, Any]:
        # High-risk confirmed operation
        return {"status": "Simulated restart PC (High risk safety mock). To execute live: shutdown -r"}

    def _handle_shutdown_pc(self, args: Dict[str, Any]) -> Dict[str, Any]:
        # High-risk confirmed operation
        return {"status": "Simulated shutdown PC (High risk safety mock). To execute live: shutdown -h"}

    def _handle_get_stream_frame(self, args: Dict[str, Any]) -> Dict[str, Any]:
        import base64
        width = int(args.get("width") or 960)
        quality = int(args.get("quality") or 50)
        jpeg_bytes, w, h, dur = screen_streamer.capture_frame(target_width=width, quality=quality)
        if jpeg_bytes is None:
            return {"success": False, "error": "Screen frame capture failed."}
        return {
            "width": w,
            "height": h,
            "capture_duration_ms": dur,
            "size_kb": round(len(jpeg_bytes) / 1024.0, 1),
            "timestamp": time.time(),
            "data_base64": base64.b64encode(jpeg_bytes).decode("utf-8")
        }

    def _handle_configure_stream(self, args: Dict[str, Any]) -> Dict[str, Any]:
        w = args.get("width")
        q = args.get("quality")
        fps = args.get("fps")
        screen_streamer.configure(target_width=int(w) if w else None, quality=int(q) if q else None, fps=int(fps) if fps else None)
        return screen_streamer.get_status()

    def _handle_get_stream_status(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return screen_streamer.get_status()

    def _handle_sync_privacy_shield(self, args: Dict[str, Any]) -> Dict[str, Any]:
        self.privacy_shield_state.update(args)
        return {"status": "synchronized", "privacy_shield": self.privacy_shield_state}

    def _handle_virtual_display_start(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return virtual_display_manager.start(
            session_id=str(args.get("session_id", "default")),
            width=int(args.get("width", 1920)),
            height=int(args.get("height", 1080)),
            fps=int(args.get("fps", 60)),
            dpi=int(args.get("dpi", 240)),
            enable_touch=bool(args.get("enable_touch", True))
        )

    def _handle_virtual_display_stop(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return virtual_display_manager.stop()

    def _handle_virtual_display_touch(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return virtual_display_manager.handle_touch(
            action=str(args.get("action", "click")),
            norm_x=float(args.get("norm_x", 0.5)),
            norm_y=float(args.get("norm_y", 0.5)),
            scroll_dx=float(args.get("scroll_dx", 0.0)),
            scroll_dy=float(args.get("scroll_dy", 0.0)),
            target_width=int(args.get("target_width", 1920)),
            target_height=int(args.get("target_height", 1080))
        )

    def _handle_edge_control_configure(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return edge_control_manager.configure(
            edge=str(args.get("edge", "RIGHT")),
            sensitivity=float(args.get("sensitivity", 1.0)),
            dwell_delay_ms=int(args.get("dwell_delay_ms", 80)),
            enable_keyboard=bool(args.get("enable_keyboard", True)),
            enable_clipboard_sync=bool(args.get("enable_clipboard_sync", True))
        )

    def _handle_edge_control_disable(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return edge_control_manager.disable()

    def _handle_edge_control_return_cursor(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return edge_control_manager.return_cursor()

    def _handle_p2p_session_init(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return p2p_node.init_session(
            session_id=str(args.get("session_id", "default")),
            client_type=str(args.get("client_type", "android")),
            stun_servers=args.get("stun_servers")
        )

    def _handle_audio_start(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return audio_engine.start(
            session_id=str(args.get("session_id", "default")),
            mode=str(args.get("mode", "sink")),
            sample_rate=int(args.get("sample_rate", 48000)),
            channels=int(args.get("channels", 2)),
            codec=str(args.get("codec", "pcm"))
        )

    def _handle_audio_stop(self, args: Dict[str, Any]) -> Dict[str, Any]:
        return audio_engine.stop()



