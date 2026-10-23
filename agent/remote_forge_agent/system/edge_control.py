"""
RemoteForge Universal Control Edge Mouse Hopping Service (Agent)
================================================================
Monitors display boundaries, detects cursor push-through gestures, locks local
cursor to the edge, and streams real-time normalized cursor coordinates to Android.
Universal Control is active 24/7 by default (Right edge).
"""

import sys
import time
import logging
import threading
from typing import Dict, Any, Optional, Callable
from agent.remote_forge_agent.system.input_control import input_service

try:
    from pynput import mouse, keyboard
except ImportError:
    mouse = None
    keyboard = None

logger = logging.getLogger("RemoteForgeEdgeControl")


class EdgeControlManager:
    def __init__(self):
        # Universal Control is ON 24/7 by default with RIGHT edge
        self.is_enabled: bool = True
        self.configured_edge: str = "RIGHT"  # "LEFT", "RIGHT", "TOP", "BOTTOM"
        self.sensitivity: float = 1.0
        self.dwell_delay_ms: int = 40
        self.enable_keyboard: bool = True
        self.enable_clipboard_sync: bool = True
        self.is_remote_active: bool = False
        self.device_id: str = "macbook-pro"
        self.send_fn: Optional[Callable[[Dict[str, Any]], None]] = None
        self.monitor_thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()
        self._last_x: float = 0.0
        self._last_y: float = 0.0
        self._mouse_listener = None
        self._keyboard_listener = None

        self._start_monitor_thread()
        self._start_input_listeners()

    def set_context(self, device_id: str, send_fn: Optional[Callable[[Dict[str, Any]], None]] = None):
        self.device_id = device_id
        self.send_fn = send_fn
        logger.info(f"EdgeControlManager context set: device_id={device_id}")

    def configure(
        self,
        edge: str = "RIGHT",
        sensitivity: float = 1.0,
        dwell_delay_ms: int = 40,
        enable_keyboard: bool = True,
        enable_clipboard_sync: bool = True
    ) -> Dict[str, Any]:
        self.configured_edge = edge.upper()
        self.sensitivity = max(0.2, min(5.0, sensitivity))
        self.dwell_delay_ms = dwell_delay_ms
        self.enable_keyboard = enable_keyboard
        self.enable_clipboard_sync = enable_clipboard_sync
        self.is_enabled = True
        self.is_remote_active = False

        self._start_monitor_thread()
        logger.info(f"Edge Control configured: edge={self.configured_edge}, sensitivity={self.sensitivity}")
        return {
            "status": "configured",
            "edge": self.configured_edge,
            "is_enabled": True
        }

    def disable(self) -> Dict[str, Any]:
        # User requested 24/7 operation, but can reset state
        self.is_remote_active = False
        logger.info("Edge Control reset")
        return {"status": "reset", "is_enabled": self.is_enabled}

    def return_cursor(self) -> Dict[str, Any]:
        """
        Returns control to local workstation, releasing cursor lock and snapping back onto screen.
        """
        self.is_remote_active = False
        try:
            dim = input_service.get_screen_dimensions()
            scr_w = dim.get("width", 1280.0)
            scr_h = dim.get("height", 800.0)
            if self.configured_edge == "RIGHT":
                input_service.move_mouse(int(scr_w - 120), int(scr_h / 2))
            elif self.configured_edge == "LEFT":
                input_service.move_mouse(120, int(scr_h / 2))
            elif self.configured_edge == "TOP":
                input_service.move_mouse(int(scr_w / 2), 120)
            else:
                input_service.move_mouse(int(scr_w / 2), int(scr_h - 120))
        except Exception as e:
            logger.debug(f"Error repositioning cursor on return: {e}")

        if self.send_fn:
            try:
                self.send_fn({
                    "type": "edge_handoff",
                    "device_id": self.device_id,
                    "event": "EXIT_REMOTE",
                    "edge": self.configured_edge
                })
            except Exception:
                pass

        logger.info("Cursor returned from remote device to local screen")
        return {"status": "returned", "is_remote_active": False}

    def _start_monitor_thread(self):
        if self.monitor_thread and self.monitor_thread.is_alive():
            return
        self._stop_event.clear()
        self.monitor_thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self.monitor_thread.start()

    def _start_input_listeners(self):
        if mouse is None or keyboard is None:
            logger.warning("pynput not available, skipping event listeners")
            return

        def on_click(x, y, button, pressed):
            if not self.is_remote_active or not self.send_fn:
                return
            btn_str = "left"
            if button == mouse.Button.right:
                btn_str = "right"
            elif button == mouse.Button.middle:
                btn_str = "middle"
            try:
                self.send_fn({
                    "type": "mouse_click",
                    "device_id": self.device_id,
                    "button": btn_str,
                    "action": "down" if pressed else "up",
                    "pressed": pressed
                })
            except Exception as e:
                logger.debug(f"Error forwarding click: {e}")

        def on_scroll(x, y, dx, dy):
            if not self.is_remote_active or not self.send_fn:
                return
            try:
                self.send_fn({
                    "type": "mouse_scroll",
                    "device_id": self.device_id,
                    "dx": dx,
                    "dy": dy
                })
            except Exception as e:
                logger.debug(f"Error forwarding scroll: {e}")

        def on_press(key):
            if not self.is_remote_active or not self.enable_keyboard or not self.send_fn:
                return
            try:
                if hasattr(key, "char") and key.char is not None:
                    k_str = key.char
                elif hasattr(key, "name"):
                    k_str = key.name
                else:
                    k_str = str(key)
                self.send_fn({
                    "type": "keyboard_key",
                    "device_id": self.device_id,
                    "key": k_str,
                    "action": "press"
                })
            except Exception as e:
                logger.debug(f"Error forwarding key press: {e}")

        def on_release(key):
            if not self.is_remote_active or not self.enable_keyboard or not self.send_fn:
                return
            try:
                if hasattr(key, "char") and key.char is not None:
                    k_str = key.char
                elif hasattr(key, "name"):
                    k_str = key.name
                else:
                    k_str = str(key)
                self.send_fn({
                    "type": "keyboard_key",
                    "device_id": self.device_id,
                    "key": k_str,
                    "action": "release"
                })
            except Exception as e:
                logger.debug(f"Error forwarding key release: {e}")

        try:
            self._mouse_listener = mouse.Listener(on_click=on_click, on_scroll=on_scroll)
            self._mouse_listener.daemon = True
            self._mouse_listener.start()

            self._keyboard_listener = keyboard.Listener(on_press=on_press, on_release=on_release)
            self._keyboard_listener.daemon = True
            self._keyboard_listener.start()
            logger.info("Universal Control mouse and keyboard listeners active.")
        except Exception as e:
            logger.warning(f"Could not initialize pynput listeners: {e}")

    def _monitor_loop(self):
        """
        Background 24/7 loop checking cursor position against configured edge and streaming coordinates.
        """
        while not self._stop_event.is_set():
            time.sleep(0.025)  # 40Hz poll rate for ultra-responsive low-latency hopping
            if not self.is_enabled:
                continue

            try:
                pos = input_service.get_mouse_position()
                if not pos:
                    continue
                cur_x, cur_y = float(pos.get("x", 0)), float(pos.get("y", 0))

                dim = input_service.get_screen_dimensions()
                scr_w = float(dim.get("width", 1280.0))
                scr_h = float(dim.get("height", 800.0))

                hit_edge = False
                if self.configured_edge == "RIGHT" and cur_x >= scr_w - 4:
                    hit_edge = True
                elif self.configured_edge == "LEFT" and cur_x <= 4:
                    hit_edge = True
                elif self.configured_edge == "BOTTOM" and cur_y >= scr_h - 4:
                    hit_edge = True
                elif self.configured_edge == "TOP" and cur_y <= 4:
                    hit_edge = True

                if hit_edge and not self.is_remote_active:
                    # Hop cursor directly to remote device!
                    self.is_remote_active = True
                    logger.info(f"Edge crossed on {self.configured_edge}! Hopped cursor to remote device.")

                    # Calculate entry point
                    if self.configured_edge == "RIGHT":
                        norm_x = 0.05
                        norm_y = max(0.0, min(1.0, cur_y / scr_h))
                        # Anchor cursor 25px inside right edge
                        anchor_x = int(scr_w - 25)
                        anchor_y = int(cur_y)
                    elif self.configured_edge == "LEFT":
                        norm_x = 0.95
                        norm_y = max(0.0, min(1.0, cur_y / scr_h))
                        anchor_x = 25
                        anchor_y = int(cur_y)
                    elif self.configured_edge == "TOP":
                        norm_x = max(0.0, min(1.0, cur_x / scr_w))
                        norm_y = 0.95
                        anchor_x = int(cur_x)
                        anchor_y = 25
                    else:
                        norm_x = max(0.0, min(1.0, cur_x / scr_w))
                        norm_y = 0.05
                        anchor_x = int(cur_x)
                        anchor_y = int(scr_h - 25)

                    input_service.move_mouse(anchor_x, anchor_y)
                    self._last_x = float(anchor_x)
                    self._last_y = float(anchor_y)

                    if self.send_fn:
                        try:
                            self.send_fn({
                                "type": "edge_handoff",
                                "device_id": self.device_id,
                                "event": "ENTER_REMOTE",
                                "edge": self.configured_edge,
                                "norm_x": norm_x,
                                "norm_y": norm_y
                            })
                        except Exception:
                            pass
                    continue

                if self.is_remote_active:
                    # Check if user intentionally moved inward back into PC screen
                    dx = cur_x - self._last_x
                    dy = cur_y - self._last_y

                    moved_inward = False
                    if self.configured_edge == "RIGHT" and (cur_x < scr_w - 70 or dx < -35):
                        moved_inward = True
                    elif self.configured_edge == "LEFT" and (cur_x > 70 or dx > 35):
                        moved_inward = True
                    elif self.configured_edge == "TOP" and (cur_y > 70 or dy > 35):
                        moved_inward = True
                    elif self.configured_edge == "BOTTOM" and (cur_y < scr_h - 70 or dy < -35):
                        moved_inward = True

                    if moved_inward:
                        self.is_remote_active = False
                        logger.info(f"Cursor returned to PC from {self.configured_edge} edge")
                        if self.send_fn:
                            try:
                                self.send_fn({
                                    "type": "edge_handoff",
                                    "device_id": self.device_id,
                                    "event": "EXIT_REMOTE",
                                    "edge": self.configured_edge
                                })
                            except Exception:
                                pass
                    else:
                        # Stream movement deltas to phone
                        if self.send_fn and (abs(dx) > 0.4 or abs(dy) > 0.4):
                            try:
                                self.send_fn({
                                    "type": "pointer_move",
                                    "device_id": self.device_id,
                                    "edge": self.configured_edge,
                                    "dx": dx * self.sensitivity,
                                    "dy": dy * self.sensitivity
                                })
                            except Exception:
                                pass

                        # Keep cursor anchored within boundary cushion so movement in the edge direction is continuous
                        if self.configured_edge == "RIGHT" and cur_x >= scr_w - 4:
                            anchor_x = int(scr_w - 25)
                            input_service.move_mouse(anchor_x, int(cur_y))
                            self._last_x = float(anchor_x)
                            self._last_y = cur_y
                        elif self.configured_edge == "LEFT" and cur_x <= 4:
                            anchor_x = 25
                            input_service.move_mouse(anchor_x, int(cur_y))
                            self._last_x = float(anchor_x)
                            self._last_y = cur_y
                        elif self.configured_edge == "TOP" and cur_y <= 4:
                            anchor_y = 25
                            input_service.move_mouse(int(cur_x), anchor_y)
                            self._last_x = cur_x
                            self._last_y = float(anchor_y)
                        elif self.configured_edge == "BOTTOM" and cur_y >= scr_h - 4:
                            anchor_y = int(scr_h - 25)
                            input_service.move_mouse(int(cur_x), anchor_y)
                            self._last_x = cur_x
                            self._last_y = float(anchor_y)
                        else:
                            self._last_x = cur_x
                            self._last_y = cur_y

            except Exception as e:
                logger.debug(f"Edge control monitor error: {e}")


edge_control_manager = EdgeControlManager()
