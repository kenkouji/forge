import sys
import time
import ctypes
import ctypes.util
import subprocess
import logging
from typing import Dict, Any, List, Optional, Tuple

logger = logging.getLogger("RemoteForgeInputControl")

class CGPoint(ctypes.Structure):
    _fields_ = [("x", ctypes.c_double), ("y", ctypes.c_double)]

class CGSize(ctypes.Structure):
    _fields_ = [("width", ctypes.c_double), ("height", ctypes.c_double)]

class CGRect(ctypes.Structure):
    _fields_ = [("origin", CGPoint), ("size", CGSize)]


# Win32 SendInput Structures (Guarded for cross-platform imports)
class KEYBDINPUT(ctypes.Structure):
    _fields_ = [
        ("wVk", ctypes.c_ushort),
        ("wScan", ctypes.c_ushort),
        ("dwFlags", ctypes.c_ulong),
        ("time", ctypes.c_ulong),
        ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong))
    ]

class MOUSEINPUT(ctypes.Structure):
    _fields_ = [
        ("dx", ctypes.c_long),
        ("dy", ctypes.c_long),
        ("mouseData", ctypes.c_ulong),
        ("dwFlags", ctypes.c_ulong),
        ("time", ctypes.c_ulong),
        ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong))
    ]

class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [
        ("uMsg", ctypes.c_ulong),
        ("wParamL", ctypes.c_short),
        ("wParamH", ctypes.c_ushort)
    ]

class INPUT_I(ctypes.Union):
    _fields_ = [
        ("ki", KEYBDINPUT),
        ("mi", MOUSEINPUT),
        ("hi", HARDWAREINPUT)
    ]

class INPUT(ctypes.Structure):
    _fields_ = [
        ("type", ctypes.c_ulong),
        ("ii", INPUT_I)
    ]


class InputControlService:
    def __init__(self):
        self.is_macos = sys.platform == "darwin"
        self.is_windows = sys.platform == "win32"
        self._app_services = None
        self._core_foundation = None
        self._user32 = None
        self._last_event_time = 0.0
        self.display_width = 1920.0 if self.is_windows else 1280.0
        self.display_height = 1080.0 if self.is_windows else 800.0

        if self.is_macos:
            try:
                self._app_services = ctypes.cdll.LoadLibrary(
                    ctypes.util.find_library("ApplicationServices")
                )
                self._core_foundation = ctypes.cdll.LoadLibrary(
                    ctypes.util.find_library("CoreFoundation")
                )
                self._init_macos_bindings()
                self._update_display_bounds()
            except Exception as e:
                logger.warning(f"Could not initialize macOS CoreGraphics bindings: {e}")
        elif self.is_windows:
            try:
                if hasattr(ctypes, "windll"):
                    self._user32 = ctypes.windll.user32
                    self._init_windows_bindings()
                    self._update_display_bounds()
            except Exception as e:
                logger.warning(f"Could not initialize Windows user32 bindings: {e}")

    def _init_macos_bindings(self):
        asvc = self._app_services
        if not asvc:
            return

        asvc.AXIsProcessTrusted.restype = ctypes.c_bool
        asvc.CGEventCreate.restype = ctypes.c_void_p
        asvc.CGEventGetLocation.restype = CGPoint
        asvc.CGEventGetLocation.argtypes = [ctypes.c_void_p]

        # Fast Warp (moves cursor unconditionally without accessibility requirements)
        if hasattr(asvc, "CGWarpMouseCursorPosition"):
            asvc.CGWarpMouseCursorPosition.restype = ctypes.c_int
            asvc.CGWarpMouseCursorPosition.argtypes = [CGPoint]

        asvc.CGMainDisplayID.restype = ctypes.c_uint32
        asvc.CGDisplayBounds.restype = CGRect
        asvc.CGDisplayBounds.argtypes = [ctypes.c_uint32]

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
            ctypes.c_void_p, ctypes.c_ulong, ctypes.POINTER(ctypes.c_uint16)
        ]

        asvc.CGEventPost.argtypes = [ctypes.c_uint32, ctypes.c_void_p]
        asvc.CFRelease.argtypes = [ctypes.c_void_p]

    def _init_windows_bindings(self):
        u32 = self._user32
        if not u32:
            return
        try:
            u32.SetCursorPos.argtypes = [ctypes.c_int, ctypes.c_int]
            u32.SetCursorPos.restype = ctypes.c_bool
            u32.GetSystemMetrics.argtypes = [ctypes.c_int]
            u32.GetSystemMetrics.restype = ctypes.c_int
            u32.mouse_event.argtypes = [
                ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong
            ]
            u32.keybd_event.argtypes = [
                ctypes.c_ubyte, ctypes.c_ubyte, ctypes.c_ulong, ctypes.c_ulong
            ]
        except Exception as e:
            logger.warning(f"Error configuring Windows user32 argtypes: {e}")

    def _update_display_bounds(self):
        if self.is_macos and self._app_services:
            try:
                main_id = self._app_services.CGMainDisplayID()
                bounds = self._app_services.CGDisplayBounds(main_id)
                if bounds.size.width > 0 and bounds.size.height > 0:
                    self.display_width = float(bounds.size.width)
                    self.display_height = float(bounds.size.height)
            except Exception:
                pass
        elif self.is_windows and self._user32:
            try:
                w = self._user32.GetSystemMetrics(0)  # SM_CXSCREEN
                h = self._user32.GetSystemMetrics(1)  # SM_CYSCREEN
                if w > 0 and h > 0:
                    self.display_width = float(w)
                    self.display_height = float(h)
            except Exception:
                pass

    def get_screen_dimensions(self) -> Dict[str, float]:
        self._update_display_bounds()
        return {"width": self.display_width, "height": self.display_height}

    def get_mouse_position(self) -> Dict[str, float]:
        if self.is_macos and self._app_services:
            try:
                ev = self._app_services.CGEventCreate(None)
                if ev:
                    pt = self._app_services.CGEventGetLocation(ev)
                    self._app_services.CFRelease(ev)
                    return {"x": float(pt.x), "y": float(pt.y)}
            except Exception:
                pass
        elif self.is_windows and self._user32:
            try:
                class POINT(ctypes.Structure):
                    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]
                pt = POINT()
                if self._user32.GetCursorPos(ctypes.byref(pt)):
                    return {"x": float(pt.x), "y": float(pt.y)}
            except Exception:
                pass
        return {"x": 0.0, "y": 0.0}

    def has_accessibility_permission(self) -> bool:
        if self.is_windows:
            return True
        if not self.is_macos or not self._app_services:
            return True
        try:
            return bool(self._app_services.AXIsProcessTrusted())
        except Exception:
            return False

    def prompt_accessibility_dialog(self):
        """Triggers the macOS system accessibility permission dialog."""
        if self.is_windows:
            return
        asvc = self._app_services
        cf = self._core_foundation
        if not asvc or not cf:
            return
        try:
            kAXTrustedCheckOptionPrompt = ctypes.c_void_p.in_dll(asvc, 'kAXTrustedCheckOptionPrompt')
            kCFBooleanTrue = ctypes.c_void_p.in_dll(cf, 'kCFBooleanTrue')
            cf.CFDictionaryCreate.restype = ctypes.c_void_p
            cf.CFDictionaryCreate.argtypes = [
                ctypes.c_void_p, ctypes.POINTER(ctypes.c_void_p), ctypes.POINTER(ctypes.c_void_p),
                ctypes.c_long, ctypes.c_void_p, ctypes.c_void_p
            ]
            keys = (ctypes.c_void_p * 1)(kAXTrustedCheckOptionPrompt.value)
            values = (ctypes.c_void_p * 1)(kCFBooleanTrue.value)
            d = cf.CFDictionaryCreate(None, keys, values, 1, None, None)
            asvc.AXIsProcessTrustedWithOptions.restype = ctypes.c_bool
            asvc.AXIsProcessTrustedWithOptions.argtypes = [ctypes.c_void_p]
            asvc.AXIsProcessTrustedWithOptions(d)
            cf.CFRelease(d)
        except Exception as e:
            logger.debug(f"Prompt dialog exception: {e}")

    def get_permission_status(self) -> Dict[str, Any]:
        trusted = self.has_accessibility_permission()
        return {
            "platform": sys.platform,
            "has_permission": trusted,
            "display_width": self.display_width,
            "display_height": self.display_height,
            "instruction": (
                "Granted" if trusted else
                "macOS Accessibility permission is required for remote clicking and keystrokes. "
                "Open System Settings -> Privacy & Security -> Accessibility and toggle on Python/Terminal."
            )
        }

    def _get_current_mouse_loc(self) -> CGPoint:
        if self.is_macos and self._app_services:
            e = self._app_services.CGEventCreate(None)
            if not e:
                return CGPoint(500.0, 500.0)
            loc = self._app_services.CGEventGetLocation(e)
            self._app_services.CFRelease(e)
            return loc
        elif self.is_windows and self._user32:
            class POINT(ctypes.Structure):
                _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]
            pt = POINT()
            if self._user32.GetCursorPos(ctypes.byref(pt)):
                return CGPoint(float(pt.x), float(pt.y))
        return CGPoint(500.0, 500.0)

    def process_input(self, args: Dict[str, Any]) -> Dict[str, Any]:
        raw_action = args.get("action")
        if not raw_action:
            raise ValueError("Missing 'action' in input event.")

        # Drop stale movement requests if delayed by network > 180ms to prevent trackpad gliding
        action = str(raw_action).lower().strip()

        # Update display bounds periodically
        self._update_display_bounds()

        # Permissions status and trigger
        if action == "check_permission":
            return self.get_permission_status()
        elif action in ("request_accessibility", "open_accessibility"):
            if self.is_macos:
                subprocess.Popen(["open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility"])
                self.prompt_accessibility_dialog()
            return {"status": "opened_settings", "has_permission": self.has_accessibility_permission()}

        # Reset all keys
        if action == "reset":
            return self._handle_reset(args)

        # Mouse Movement & Drag
        if action in ("move", "mouse_move_relative"):
            return self._handle_move(args)
        elif action in ("mouse_drag", "drag"):
            mod_args = dict(args)
            mod_args["is_drag"] = True
            return self._handle_move(mod_args)

        # Mouse Click & Tap
        elif action in ("click", "mouse_click"):
            return self._handle_click(args)
        elif action in ("mouse_double_click", "double_click"):
            mod_args = dict(args)
            mod_args["click_count"] = 2
            return self._handle_click(mod_args)

        # Mouse Down / Up
        elif action in ("mouse_down", "down"):
            return self._handle_mouse_down(args)
        elif action in ("mouse_up", "up"):
            return self._handle_mouse_up(args)

        # Scroll
        elif action in ("scroll", "mouse_scroll"):
            return self._handle_scroll(args)

        # Keyboard & Shortcuts
        elif action in ("key", "key_tap"):
            return self._handle_key(args)
        elif action == "shortcut":
            return self._handle_shortcut(args)
        elif action == "type_text":
            return self._handle_type_text(args)
        else:
            raise ValueError(f"Unknown input action: '{raw_action}'")

    def _handle_move(self, args: Dict[str, Any]) -> Dict[str, Any]:
        dx = float(args.get("dx", 0.0))
        dy = float(args.get("dy", 0.0))
        is_drag = bool(args.get("is_drag", False))
        rel_x = args.get("rel_x")
        rel_y = args.get("rel_y")
        abs_x = args.get("x")
        abs_y = args.get("y")

        if rel_x is not None and rel_y is not None:
            target_x = max(0.0, min(self.display_width, float(rel_x) * self.display_width))
            target_y = max(0.0, min(self.display_height, float(rel_y) * self.display_height))
        elif abs_x is not None and abs_y is not None and dx == 0.0 and dy == 0.0:
            target_x = max(0.0, min(self.display_width, float(abs_x)))
            target_y = max(0.0, min(self.display_height, float(abs_y)))
        else:
            curr = self._get_current_mouse_loc()
            target_x = max(0.0, min(self.display_width, curr.x + dx))
            target_y = max(0.0, min(self.display_height, curr.y + dy))

        # Windows Execution
        if self.is_windows and self._user32:
            self._user32.SetCursorPos(int(target_x), int(target_y))
            if is_drag:
                # MOUSEEVENTF_MOVE = 0x0001
                self._user32.mouse_event(0x0001, 0, 0, 0, 0)
            return {"status": "moved", "x": target_x, "y": target_y, "is_drag": is_drag}

        # macOS Execution
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        new_loc = CGPoint(target_x, target_y)

        # Warp cursor position: Instantly and unconditionally updates visual pointer
        if hasattr(asvc, "CGWarpMouseCursorPosition"):
            asvc.CGWarpMouseCursorPosition(new_loc)

        # Post mouse moved or dragged event
        event_type = 6 if is_drag else 5  # 5: kCGEventMouseMoved, 6: kCGEventLeftMouseDragged
        e = asvc.CGEventCreateMouseEvent(None, event_type, new_loc, 0)
        if e:
            asvc.CGEventPost(1, e)  # kCGSessionEventTap
            asvc.CGEventPost(0, e)  # kCGHIDEventTap
            asvc.CFRelease(e)

        return {"status": "moved", "x": target_x, "y": target_y, "is_drag": is_drag}

    def _handle_click(self, args: Dict[str, Any]) -> Dict[str, Any]:
        button = str(args.get("button", "left")).lower()
        click_count = int(args.get("click_count", 1))

        # Check for absolute coordinates or relative touch from screen mirror
        rel_x = args.get("rel_x")
        rel_y = args.get("rel_y")
        abs_x = args.get("x")
        abs_y = args.get("y")

        if rel_x is not None and rel_y is not None:
            tx = max(0.0, min(self.display_width, float(rel_x) * self.display_width))
            ty = max(0.0, min(self.display_height, float(rel_y) * self.display_height))
            curr = CGPoint(tx, ty)
        elif abs_x is not None and abs_y is not None:
            tx = max(0.0, min(self.display_width, float(abs_x)))
            ty = max(0.0, min(self.display_height, float(abs_y)))
            curr = CGPoint(tx, ty)
        else:
            curr = self._get_current_mouse_loc()

        # Windows Execution
        if self.is_windows and self._user32:
            self._user32.SetCursorPos(int(curr.x), int(curr.y))
            # MOUSEEVENTF_LEFTDOWN = 0x0002, MOUSEEVENTF_LEFTUP = 0x0004
            # MOUSEEVENTF_RIGHTDOWN = 0x0008, MOUSEEVENTF_RIGHTUP = 0x0010
            down_flag = 0x0008 if button == "right" else 0x0002
            up_flag = 0x0010 if button == "right" else 0x0004

            for _ in range(click_count):
                self._user32.mouse_event(down_flag, 0, 0, 0, 0)
                time.sleep(0.015)
                self._user32.mouse_event(up_flag, 0, 0, 0, 0)
                if click_count > 1:
                    time.sleep(0.04)

            return {
                "status": "clicked",
                "button": button,
                "click_count": click_count,
                "x": curr.x,
                "y": curr.y,
                "has_permission": True
            }

        # macOS Execution
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        if hasattr(asvc, "CGWarpMouseCursorPosition"):
            asvc.CGWarpMouseCursorPosition(curr)

        if button == "right":
            down_type, up_type, btn_num = 3, 4, 1
        else:
            down_type, up_type, btn_num = 1, 2, 0

        trusted = self.has_accessibility_permission()
        if not trusted:
            self.prompt_accessibility_dialog()

        for _ in range(click_count):
            e_down = asvc.CGEventCreateMouseEvent(None, down_type, curr, btn_num)
            if e_down:
                asvc.CGEventPost(1, e_down)  # kCGSessionEventTap
                asvc.CGEventPost(0, e_down)  # kCGHIDEventTap
                asvc.CFRelease(e_down)

            time.sleep(0.015)

            e_up = asvc.CGEventCreateMouseEvent(None, up_type, curr, btn_num)
            if e_up:
                asvc.CGEventPost(1, e_up)  # kCGSessionEventTap
                asvc.CGEventPost(0, e_up)  # kCGHIDEventTap
                asvc.CFRelease(e_up)

            if click_count > 1:
                time.sleep(0.04)

        return {
            "status": "clicked",
            "button": button,
            "click_count": click_count,
            "x": curr.x,
            "y": curr.y,
            "has_permission": trusted
        }

    def _handle_mouse_down(self, args: Dict[str, Any]) -> Dict[str, Any]:
        button = str(args.get("button", "left")).lower()

        if self.is_windows and self._user32:
            flag = 0x0008 if button == "right" else 0x0002
            self._user32.mouse_event(flag, 0, 0, 0, 0)
            return {"status": "mouse_down", "button": button}

        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        curr = self._get_current_mouse_loc()
        event_type = 3 if button == "right" else 1
        btn_num = 1 if button == "right" else 0

        e = asvc.CGEventCreateMouseEvent(None, event_type, curr, btn_num)
        if e:
            asvc.CGEventPost(1, e)
            asvc.CGEventPost(0, e)
            asvc.CFRelease(e)
        return {"status": "mouse_down", "button": button}

    def _handle_mouse_up(self, args: Dict[str, Any]) -> Dict[str, Any]:
        button = str(args.get("button", "left")).lower()

        if self.is_windows and self._user32:
            flag = 0x0010 if button == "right" else 0x0004
            self._user32.mouse_event(flag, 0, 0, 0, 0)
            return {"status": "mouse_up", "button": button}

        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        curr = self._get_current_mouse_loc()
        event_type = 4 if button == "right" else 2
        btn_num = 1 if button == "right" else 0

        e = asvc.CGEventCreateMouseEvent(None, event_type, curr, btn_num)
        if e:
            asvc.CGEventPost(1, e)
            asvc.CGEventPost(0, e)
            asvc.CFRelease(e)
        return {"status": "mouse_up", "button": button}

    def _handle_scroll(self, args: Dict[str, Any]) -> Dict[str, Any]:
        dy = int(round(float(args.get("dy", 0))))
        if dy == 0:
            dy = 1

        if self.is_windows and self._user32:
            # MOUSEEVENTF_WHEEL = 0x0800, standard wheel tick is 120
            self._user32.mouse_event(0x0800, 0, 0, int(dy * 120), 0)
            return {"status": "scrolled", "dy": dy}

        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        # 1: kCGScrollEventUnitLine
        e = asvc.CGEventCreateScrollWheelEvent(None, 1, 1, dy)
        if e:
            asvc.CGEventPost(1, e)
            asvc.CGEventPost(0, e)
            asvc.CFRelease(e)
        return {"status": "scrolled", "dy": dy}

    def _get_macos_virtual_key_code(self, key_name: str) -> Optional[int]:
        mapping = {
            "return": 0x24, "enter": 0x24,
            "tab": 0x30,
            "space": 0x31, " ": 0x31,
            "backspace": 0x33, "delete": 0x33,
            "escape": 0x35, "esc": 0x35,
            "command": 0x37, "cmd": 0x37,
            "shift": 0x38,
            "capslock": 0x39,
            "option": 0x3A, "alt": 0x3A,
            "control": 0x3B, "ctrl": 0x3B,
            "left": 0x7B, "arrowleft": 0x7B, "arrow_left": 0x7B,
            "right": 0x7C, "arrowright": 0x7C, "arrow_right": 0x7C,
            "down": 0x7D, "arrowdown": 0x7D, "arrow_down": 0x7D,
            "up": 0x7E, "arrowup": 0x7E, "arrow_up": 0x7E,
            "f1": 0x7A, "f2": 0x78, "f3": 0x63, "f4": 0x76,
            "f5": 0x60, "f6": 0x61, "f7": 0x62, "f8": 0x64,
            "f9": 0x65, "f10": 0x6D, "f11": 0x67, "f12": 0x6F,
            "a": 0x00, "s": 0x01, "d": 0x02, "f": 0x03, "h": 0x04, "g": 0x05, "z": 0x06, "x": 0x07, "c": 0x08, "v": 0x09,
            "b": 0x0B, "q": 0x0C, "w": 0x0D, "e": 0x0E, "r": 0x0F, "y": 0x10, "t": 0x11, "1": 0x12, "2": 0x13, "3": 0x14,
            "4": 0x15, "6": 0x16, "5": 0x17, "=": 0x18, "9": 0x19, "7": 0x1A, "-": 0x1B, "8": 0x1C, "0": 0x1D, "]": 0x1E,
            "o": 0x1F, "u": 0x20, "[": 0x21, "i": 0x22, "p": 0x23, "l": 0x25, "j": 0x26, "'": 0x27, "k": 0x28, ";": 0x29,
            "\\": 0x2A, ",": 0x2B, "/": 0x2C, "n": 0x2D, "m": 0x2E, ".": 0x2F, "`": 0x32
        }
        return mapping.get(key_name.lower())

    def _get_windows_virtual_key_code(self, key_name: str) -> Optional[int]:
        mapping = {
            "return": 0x0D, "enter": 0x0D,
            "tab": 0x09,
            "space": 0x20, " ": 0x20,
            "backspace": 0x08, "delete": 0x2E,
            "escape": 0x1B, "esc": 0x1B,
            "command": 0x5B, "cmd": 0x5B, "win": 0x5B, "windows": 0x5B,
            "shift": 0x10,
            "capslock": 0x14,
            "option": 0x12, "alt": 0x12,
            "control": 0x11, "ctrl": 0x11,
            "left": 0x25, "arrowleft": 0x25, "arrow_left": 0x25,
            "up": 0x26, "arrowup": 0x26, "arrow_up": 0x26,
            "right": 0x27, "arrowright": 0x27, "arrow_right": 0x27,
            "down": 0x28, "arrowdown": 0x28, "arrow_down": 0x28,
            "home": 0x24, "end": 0x23, "pageup": 0x21, "pagedown": 0x22,
            "f1": 0x70, "f2": 0x71, "f3": 0x72, "f4": 0x73,
            "f5": 0x74, "f6": 0x75, "f7": 0x76, "f8": 0x77,
            "f9": 0x78, "f10": 0x79, "f11": 0x7A, "f12": 0x7B
        }
        k_lower = key_name.lower()
        if k_lower in mapping:
            return mapping[k_lower]
        if len(key_name) == 1:
            ch = key_name.upper()
            if 'A' <= ch <= 'Z':
                return ord(ch)
            if '0' <= ch <= '9':
                return ord(ch)
        return None

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
        key = str(args.get("key", ""))
        modifiers = args.get("modifiers", [])

        # Windows Execution
        if self.is_windows and self._user32:
            vk = self._get_windows_virtual_key_code(key)
            if vk is None:
                return self._handle_type_text({"text": key})

            mod_vks = []
            for m in modifiers:
                ml = m.lower()
                if ml in ("command", "cmd", "control", "ctrl"):
                    mod_vks.append(0x11)  # VK_CONTROL
                elif ml == "shift":
                    mod_vks.append(0x10)  # VK_SHIFT
                elif ml in ("option", "alt"):
                    mod_vks.append(0x12)  # VK_MENU
                elif ml in ("win", "windows"):
                    mod_vks.append(0x5B)  # VK_LWIN

            for mv in mod_vks:
                self._user32.keybd_event(mv, 0, 0, 0)
            self._user32.keybd_event(vk, 0, 0, 0)
            time.sleep(0.015)
            self._user32.keybd_event(vk, 0, 2, 0)  # KEYEVENTF_KEYUP = 0x0002
            for mv in reversed(mod_vks):
                self._user32.keybd_event(mv, 0, 2, 0)

            return {"status": "key_pressed", "key": key, "has_permission": True}

        # macOS Execution
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        vk = self._get_macos_virtual_key_code(key)
        if vk is None:
            # Fall back to Unicode text typing if not a mapped virtual key
            return self._handle_type_text({"text": key})

        flags = self._compute_modifier_flags(modifiers)
        trusted = self.has_accessibility_permission()
        if not trusted:
            self.prompt_accessibility_dialog()

        e_down = asvc.CGEventCreateKeyboardEvent(None, vk, True)
        if e_down:
            if flags:
                asvc.CGEventSetFlags(e_down, flags)
            asvc.CGEventPost(1, e_down)  # kCGSessionEventTap
            asvc.CGEventPost(0, e_down)  # kCGHIDEventTap
            asvc.CFRelease(e_down)

        time.sleep(0.015)

        e_up = asvc.CGEventCreateKeyboardEvent(None, vk, False)
        if e_up:
            asvc.CGEventPost(1, e_up)  # kCGSessionEventTap
            asvc.CGEventPost(0, e_up)  # kCGHIDEventTap
            asvc.CFRelease(e_up)

        return {"status": "key_pressed", "key": key, "has_permission": trusted}

    def _handle_shortcut(self, args: Dict[str, Any]) -> Dict[str, Any]:
        name = str(args.get("name", "")).lower().replace(" ", "")
        mod_key = "control" if self.is_windows else "command"

        if name in ("copy", "cmd+c", "ctrl+c"):
            return self._handle_key({"key": "c", "modifiers": [mod_key]})
        elif name in ("paste", "cmd+v", "ctrl+v"):
            return self._handle_key({"key": "v", "modifiers": [mod_key]})
        elif name in ("select_all", "cmd+a", "ctrl+a"):
            return self._handle_key({"key": "a", "modifiers": [mod_key]})
        elif name in ("cut", "cmd+x", "ctrl+x"):
            return self._handle_key({"key": "x", "modifiers": [mod_key]})
        elif name in ("undo", "cmd+z", "ctrl+z"):
            return self._handle_key({"key": "z", "modifiers": [mod_key]})
        elif name in ("spotlight", "cmd+space", "win+s"):
            if self.is_windows:
                return self._handle_key({"key": "s", "modifiers": ["win"]})
            return self._handle_key({"key": "space", "modifiers": ["command"]})
        elif name in ("app_switcher", "cmd+tab", "alt+tab"):
            if self.is_windows:
                return self._handle_key({"key": "tab", "modifiers": ["alt"]})
            return self._handle_key({"key": "tab", "modifiers": ["command"]})
        elif name in ("escape", "esc"):
            return self._handle_key({"key": "escape"})
        else:
            raise ValueError(f"Unknown shortcut name: '{name}'")

    def _handle_type_text(self, args: Dict[str, Any]) -> Dict[str, Any]:
        text = str(args.get("text", ""))
        if not text:
            return {"status": "empty_text"}

        # Windows Execution via SendInput KEYEVENTF_UNICODE
        if self.is_windows and self._user32 and hasattr(self._user32, "SendInput"):
            for char in text:
                if char == "\n":
                    self._handle_key({"key": "return"})
                    time.sleep(0.02)
                    continue
                elif char == "\t":
                    self._handle_key({"key": "tab"})
                    time.sleep(0.02)
                    continue

                code = ord(char)
                # Send unicode char down & up
                inp_down = INPUT(type=1)  # INPUT_KEYBOARD = 1
                inp_down.ii.ki = KEYBDINPUT(wVk=0, wScan=code, dwFlags=0x0004, time=0, dwExtraInfo=None)  # KEYEVENTF_UNICODE = 0x0004
                inp_up = INPUT(type=1)
                inp_up.ii.ki = KEYBDINPUT(wVk=0, wScan=code, dwFlags=0x0004 | 0x0002, time=0, dwExtraInfo=None)  # KEYEVENTF_KEYUP = 0x0002

                self._user32.SendInput(1, ctypes.byref(inp_down), ctypes.sizeof(INPUT))
                time.sleep(0.01)
                self._user32.SendInput(1, ctypes.byref(inp_up), ctypes.sizeof(INPUT))
                time.sleep(0.01)

            return {"status": "text_typed", "length": len(text), "has_permission": True}

        # macOS Execution
        asvc = self._app_services
        if not asvc:
            return {"status": "unsupported_platform"}

        trusted = self.has_accessibility_permission()
        if not trusted:
            self.prompt_accessibility_dialog()

        for char in text:
            if char == "\n":
                self._handle_key({"key": "return"})
                time.sleep(0.02)
                continue
            elif char == "\t":
                self._handle_key({"key": "tab"})
                time.sleep(0.02)
                continue

            # Convert char to UTF-16 code units (UniChar in Apple CoreGraphics)
            code_units = [ord(c) for c in char]
            arr = (ctypes.c_uint16 * len(code_units))(*code_units)

            e_down = asvc.CGEventCreateKeyboardEvent(None, 0, True)
            if e_down:
                asvc.CGEventKeyboardSetUnicodeString(e_down, len(code_units), arr)
                asvc.CGEventPost(1, e_down)  # kCGSessionEventTap
                asvc.CGEventPost(0, e_down)  # kCGHIDEventTap
                asvc.CFRelease(e_down)

            time.sleep(0.015)

            e_up = asvc.CGEventCreateKeyboardEvent(None, 0, False)
            if e_up:
                asvc.CGEventKeyboardSetUnicodeString(e_up, len(code_units), arr)
                asvc.CGEventPost(1, e_up)  # kCGSessionEventTap
                asvc.CGEventPost(0, e_up)  # kCGHIDEventTap
                asvc.CFRelease(e_up)

            time.sleep(0.015)

        return {"status": "text_typed", "length": len(text), "has_permission": trusted}

    def _handle_reset(self, args: Dict[str, Any]) -> Dict[str, Any]:
        if self.is_windows and self._user32:
            self._user32.mouse_event(0x0004, 0, 0, 0, 0)  # MOUSEEVENTF_LEFTUP
            self._user32.mouse_event(0x0010, 0, 0, 0, 0)  # MOUSEEVENTF_RIGHTUP
            return {"status": "reset"}

        asvc = self._app_services
        if asvc:
            curr = self._get_current_mouse_loc()
            # Clear mouse buttons
            e1 = asvc.CGEventCreateMouseEvent(None, 2, curr, 0)
            if e1:
                asvc.CGEventPost(0, e1)
                asvc.CFRelease(e1)
            e2 = asvc.CGEventCreateMouseEvent(None, 4, curr, 1)
            if e2:
                asvc.CGEventPost(0, e2)
                asvc.CFRelease(e2)
        return {"status": "reset"}


input_service = InputControlService()
