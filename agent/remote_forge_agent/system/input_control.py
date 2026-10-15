import sys
import time
import ctypes
import ctypes.util
from typing import Dict, Any, List, Optional

class CGPoint(ctypes.Structure):
    _fields_ = [("x", ctypes.c_double), ("y", ctypes.c_double)]

class InputControlService:
    def __init__(self):
        self.is_macos = sys.platform == "darwin"
        self._app_services = None
        self._last_event_time = 0.0
        if self.is_macos:
            try:
                self._app_services = ctypes.cdll.LoadLibrary(
                    ctypes.util.find_library("ApplicationServices")
                )
                self._init_macos_bindings()
            except Exception as e:
                print(f"[InputControlService] Warning: Could not initialize macOS CoreGraphics: {e}")

    def _init_macos_bindings(self):
        asvc = self._app_services
        if not asvc:
            return

        asvc.AXIsProcessTrusted.restype = ctypes.c_bool
        asvc.CGEventCreate.restype = ctypes.c_void_p
        asvc.CGEventGetLocation.restype = CGPoint
        asvc.CGEventGetLocation.argtypes = [ctypes.c_void_p]

        asvc.CGEventCreateMouseEvent.restype = ctypes.c_void_p
        asvc.CGEventCreateMouseEvent.argtypes = [
            ctypes.c_void_p, ctypes.c_uint32, CGPoint, ctypes.c_uint32
        ]

        asvc.CGEventCreateScrollWheelEvent.restype = ctypes.c_void_p
        asvc.CGEventCreateScrollWheelEvent.argtypes = [
            ctypes.c_void_p, ctypes.c_uint32, ctypes.c_uint32, ctypes.c_int32
        ]

        asvc.CGEventCreateKeyboardEvent.restype = ctypes.c_void_p
        asvc.CGEventCreateKeyboardEvent.argtypes = [
            ctypes.c_void_p, ctypes.c_uint16, ctypes.c_bool
        ]

        asvc.CGEventSetFlags.argtypes = [ctypes.c_void_p, ctypes.c_uint64]
        asvc.CGEventKeyboardSetUnicodeString.argtypes = [
            ctypes.c_void_p, ctypes.c_ulong, ctypes.c_wchar_p
        ]

        asvc.CGEventPost.argtypes = [ctypes.c_uint32, ctypes.c_void_p]
        asvc.CFRelease.argtypes = [ctypes.c_void_p]

    def has_accessibility_permission(self) -> bool:
        if not self.is_macos or not self._app_services:
            return True
        try:
            return bool(self._app_services.AXIsProcessTrusted())
        except Exception:
            return False

    def get_permission_status(self) -> Dict[str, Any]:
        trusted = self.has_accessibility_permission()
        return {
            "platform": sys.platform,
            "has_permission": trusted,
            "instruction": (
                "Granted" if trusted else
                "macOS Accessibility permission is required for remote touchpad and keyboard. "
                "Open System Settings -> Privacy & Security -> Accessibility and enable your Terminal/Python."
            )
        }

    def _get_current_mouse_loc(self) -> CGPoint:
        if not self._app_services:
            return CGPoint(500.0, 500.0)
        e = self._app_services.CGEventCreate(None)
        if not e:
            return CGPoint(500.0, 500.0)
        loc = self._app_services.CGEventGetLocation(e)
        self._app_services.CFRelease(e)
        return loc

    def process_input(self, args: Dict[str, Any]) -> Dict[str, Any]:
        # Drop stale movement frames to prevent backlog
        client_ts = args.get("timestamp")
        if client_ts and (time.time() - client_ts) > 2.0:
            return {"status": "dropped_stale"}

        action = args.get("action")
        if not action:
            raise ValueError("Missing 'action' in input event.")

        if not self.has_accessibility_permission():
            raise PermissionError(
                "macOS Accessibility permission is not granted. "
                "Please enable Accessibility for RemoteForge/Terminal in macOS System Settings -> Privacy & Security -> Accessibility."
            )

        if action == "check_permission":
            return self.get_permission_status()

        if action == "move":
            return self._handle_move(args)
        elif action == "click":
            return self._handle_click(args)
        elif action == "mouse_down":
            return self._handle_mouse_down(args)
        elif action == "mouse_up":
            return self._handle_mouse_up(args)
        elif action == "scroll":
            return self._handle_scroll(args)
        elif action == "key":
            return self._handle_key(args)
        elif action == "shortcut":
            return self._handle_shortcut(args)
        elif action == "type_text":
            return self._handle_type_text(args)
        else:
            raise ValueError(f"Unknown input action: '{action}'")

    def _handle_move(self, args: Dict[str, Any]) -> Dict[str, Any]:
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        dx = float(args.get("dx", 0.0))
        dy = float(args.get("dy", 0.0))
        is_drag = bool(args.get("is_drag", False))

        curr = self._get_current_mouse_loc()
        target_x = max(0.0, curr.x + dx)
        target_y = max(0.0, curr.y + dy)
        new_loc = CGPoint(target_x, target_y)

        # 5: kCGEventMouseMoved, 6: kCGEventLeftMouseDragged
        event_type = 6 if is_drag else 5
        e = asvc.CGEventCreateMouseEvent(None, event_type, new_loc, 0)
        if e:
            asvc.CGEventPost(0, e)  # kCGHIDEventTap = 0
            asvc.CFRelease(e)

        return {"status": "moved", "x": target_x, "y": target_y}

    def _handle_click(self, args: Dict[str, Any]) -> Dict[str, Any]:
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        button = str(args.get("button", "left")).lower()
        click_count = int(args.get("click_count", 1))
        curr = self._get_current_mouse_loc()

        if button == "right":
            down_type, up_type, btn_num = 3, 4, 1
        else:
            down_type, up_type, btn_num = 1, 2, 0

        for _ in range(click_count):
            e_down = asvc.CGEventCreateMouseEvent(None, down_type, curr, btn_num)
            if e_down:
                asvc.CGEventPost(0, e_down)
                asvc.CFRelease(e_down)

            time.sleep(0.015)

            e_up = asvc.CGEventCreateMouseEvent(None, up_type, curr, btn_num)
            if e_up:
                asvc.CGEventPost(0, e_up)
                asvc.CFRelease(e_up)

            if click_count > 1:
                time.sleep(0.05)

        return {"status": "clicked", "button": button, "click_count": click_count}

    def _handle_mouse_down(self, args: Dict[str, Any]) -> Dict[str, Any]:
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        button = str(args.get("button", "left")).lower()
        curr = self._get_current_mouse_loc()
        event_type = 3 if button == "right" else 1
        btn_num = 1 if button == "right" else 0

        e = asvc.CGEventCreateMouseEvent(None, event_type, curr, btn_num)
        if e:
            asvc.CGEventPost(0, e)
            asvc.CFRelease(e)
        return {"status": "mouse_down", "button": button}

    def _handle_mouse_up(self, args: Dict[str, Any]) -> Dict[str, Any]:
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        button = str(args.get("button", "left")).lower()
        curr = self._get_current_mouse_loc()
        event_type = 4 if button == "right" else 2
        btn_num = 1 if button == "right" else 0

        e = asvc.CGEventCreateMouseEvent(None, event_type, curr, btn_num)
        if e:
            asvc.CGEventPost(0, e)
            asvc.CFRelease(e)
        return {"status": "mouse_up", "button": button}

    def _handle_scroll(self, args: Dict[str, Any]) -> Dict[str, Any]:
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        dy = int(args.get("dy", 0))
        # 1: kCGScrollEventUnitLine
        e = asvc.CGEventCreateScrollWheelEvent(None, 1, 1, dy)
        if e:
            asvc.CGEventPost(0, e)
            asvc.CFRelease(e)
        return {"status": "scrolled", "dy": dy}

    def _get_virtual_key_code(self, key_name: str) -> Optional[int]:
        mapping = {
            "return": 0x24, "enter": 0x24,
            "tab": 0x30,
            "space": 0x31,
            "backspace": 0x33, "delete": 0x33,
            "escape": 0x35, "esc": 0x35,
            "command": 0x37, "cmd": 0x37,
            "shift": 0x38,
            "option": 0x3A, "alt": 0x3A,
            "control": 0x3B, "ctrl": 0x3B,
            "left": 0x7B, "arrow_left": 0x7B,
            "right": 0x7C, "arrow_right": 0x7C,
            "down": 0x7D, "arrow_down": 0x7D,
            "up": 0x7E, "arrow_up": 0x7E,
            "f1": 0x7A, "f2": 0x78, "f3": 0x63, "f4": 0x76,
            "f5": 0x60, "f6": 0x61, "f7": 0x62, "f8": 0x64,
            "f9": 0x65, "f10": 0x6D, "f11": 0x67, "f12": 0x6F,
            "c": 0x08, "v": 0x09, "a": 0x00, "x": 0x07, "z": 0x06,
            "w": 0x0D, "q": 0x0C, "s": 0x01, "f": 0x03, "r": 0x0F
        }
        return mapping.get(key_name.lower())

    def _compute_modifier_flags(self, modifiers: List[str]) -> int:
        flags = 0
        for m in modifiers:
            ml = m.lower()
            if ml in ("command", "cmd"):
                flags |= (1 << 20)  # kCGEventFlagMaskCommand
            elif ml == "shift":
                flags |= (1 << 17)  # kCGEventFlagMaskShift
            elif ml in ("option", "alt"):
                flags |= (1 << 19)  # kCGEventFlagMaskAlternate
            elif ml in ("control", "ctrl"):
                flags |= (1 << 18)  # kCGEventFlagMaskControl
        return flags

    def _handle_key(self, args: Dict[str, Any]) -> Dict[str, Any]:
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        key = str(args.get("key", ""))
        modifiers = args.get("modifiers", [])
        vk = self._get_virtual_key_code(key)

        if vk is None:
            # Fall back to Unicode typing if not mapped
            return self._handle_type_text({"text": key})

        flags = self._compute_modifier_flags(modifiers)

        e_down = asvc.CGEventCreateKeyboardEvent(None, vk, True)
        if e_down:
            if flags:
                asvc.CGEventSetFlags(e_down, flags)
            asvc.CGEventPost(0, e_down)
            asvc.CFRelease(e_down)

        time.sleep(0.01)

        e_up = asvc.CGEventCreateKeyboardEvent(None, vk, False)
        if e_up:
            asvc.CGEventPost(0, e_up)
            asvc.CFRelease(e_up)

        return {"status": "key_pressed", "key": key}

    def _handle_shortcut(self, args: Dict[str, Any]) -> Dict[str, Any]:
        name = str(args.get("name", "")).lower()
        if name in ("copy", "cmd+c"):
            return self._handle_key({"key": "c", "modifiers": ["command"]})
        elif name in ("paste", "cmd+v"):
            return self._handle_key({"key": "v", "modifiers": ["command"]})
        elif name in ("select_all", "cmd+a"):
            return self._handle_key({"key": "a", "modifiers": ["command"]})
        elif name in ("cut", "cmd+x"):
            return self._handle_key({"key": "x", "modifiers": ["command"]})
        elif name in ("undo", "cmd+z"):
            return self._handle_key({"key": "z", "modifiers": ["command"]})
        elif name in ("spotlight", "cmd+space"):
            return self._handle_key({"key": "space", "modifiers": ["command"]})
        elif name in ("app_switcher", "cmd+tab"):
            return self._handle_key({"key": "tab", "modifiers": ["command"]})
        elif name in ("escape", "esc"):
            return self._handle_key({"key": "escape"})
        else:
            raise ValueError(f"Unknown shortcut name: '{name}'")

    def _handle_type_text(self, args: Dict[str, Any]) -> Dict[str, Any]:
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        text = str(args.get("text", ""))
        if not text:
            return {"status": "empty_text"}

        for char in text:
            if char == "\n":
                self._handle_key({"key": "return"})
                continue
            elif char == "\t":
                self._handle_key({"key": "tab"})
                continue

            e_down = asvc.CGEventCreateKeyboardEvent(None, 0, True)
            if e_down:
                asvc.CGEventKeyboardSetUnicodeString(e_down, len(char), char)
                asvc.CGEventPost(0, e_down)
                asvc.CFRelease(e_down)

            time.sleep(0.005)

            e_up = asvc.CGEventCreateKeyboardEvent(None, 0, False)
            if e_up:
                asvc.CGEventPost(0, e_up)
                asvc.CFRelease(e_up)

        return {"status": "text_typed", "length": len(text)}

input_service = InputControlService()
