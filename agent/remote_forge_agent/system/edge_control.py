"""
RemoteForge Universal Control Edge Mouse Hopping Service (Agent)
================================================================
Monitors display boundaries, detects cursor push-through gestures, locks local
cursor to the edge, and streams real-time normalized cursor coordinates to Android.
"""

import sys
import time
import logging
import threading
from typing import Dict, Any, Optional, Callable
from agent.remote_forge_agent.system.input_control import input_service

logger = logging.getLogger("RemoteForgeEdgeControl")


class EdgeControlManager:
    def __init__(self):
        self.is_enabled: bool = False
        self.configured_edge: str = "RIGHT"  # "LEFT", "RIGHT", "TOP", "BOTTOM"
        self.sensitivity: float = 1.0
        self.dwell_delay_ms: int = 80
        self.enable_keyboard: bool = True
        self.enable_clipboard_sync: bool = True
        self.is_remote_active: bool = False
        self.device_id: str = "macbook-pro"
        self.send_fn: Optional[Callable[[Dict[str, Any]], None]] = None
        self.monitor_thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()
        self._last_x: float = 0.0
        self._last_y: float = 0.0

    def set_context(self, device_id: str, send_fn: Optional[Callable[[Dict[str, Any]], None]] = None):
        self.device_id = device_id
        self.send_fn = send_fn

    def configure(
        self,
        edge: str = "RIGHT",
        sensitivity: float = 1.0,
        dwell_delay_ms: int = 80,
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
        self.is_enabled = False
        self.is_remote_active = False
        self._stop_event.set()
        logger.info("Edge Control disabled")
        return {"status": "disabled"}

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
                input_service.move_mouse(int(scr_w - 70), int(scr_h / 2))
            elif self.configured_edge == "LEFT":
                input_service.move_mouse(70, int(scr_h / 2))
            elif self.configured_edge == "TOP":
                input_service.move_mouse(int(scr_w / 2), 70)
            else:
                input_service.move_mouse(int(scr_w / 2), int(scr_h - 70))
        except Exception as e:
            logger.debug(f"Error repositioning cursor on return: {e}")

        if self.send_fn:
            self.send_fn({
                "type": "edge_handoff",
                "device_id": self.device_id,
                "event": "EXIT_REMOTE",
                "edge": self.configured_edge
            })

        logger.info("Cursor returned from remote device to local screen")
        return {"status": "returned", "is_remote_active": False}

    def _start_monitor_thread(self):
        if self.monitor_thread and self.monitor_thread.is_alive():
            return
        self._stop_event.clear()
        self.monitor_thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self.monitor_thread.start()

    def _monitor_loop(self):
        """
        Background loop checking cursor position against configured edge and streaming coordinates.
        """
        dwell_start = 0.0
        while not self._stop_event.is_set() and self.is_enabled:
            time.sleep(0.04)  # 25Hz poll rate for responsive low-latency hopping
            try:
                pos = input_service.get_mouse_position()
                if not pos:
                    continue
                cur_x, cur_y = float(pos.get("x", 0)), float(pos.get("y", 0))

                dim = input_service.get_screen_dimensions()
                scr_w = float(dim.get("width", 1280.0))
                scr_h = float(dim.get("height", 800.0))

                hit_edge = False
                if self.configured_edge == "RIGHT" and cur_x >= scr_w - 5:
                    hit_edge = True
                elif self.configured_edge == "LEFT" and cur_x <= 5:
                    hit_edge = True
                elif self.configured_edge == "BOTTOM" and cur_y >= scr_h - 5:
                    hit_edge = True
                elif self.configured_edge == "TOP" and cur_y <= 5:
                    hit_edge = True

                if hit_edge and not self.is_remote_active:
                    if dwell_start == 0.0:
                        dwell_start = time.time()
                    elif (time.time() - dwell_start) * 1000 >= self.dwell_delay_ms:
                        # Hop to remote Android device!
                        self.is_remote_active = True
                        dwell_start = 0.0
                        self._last_x = cur_x
                        self._last_y = cur_y
                        logger.info(f"Edge crossed on {self.configured_edge}! Transferred cursor to remote device.")
                        if self.send_fn:
                            self.send_fn({
                                "type": "edge_handoff",
                                "device_id": self.device_id,
                                "event": "ENTER_REMOTE",
                                "edge": self.configured_edge,
                                "norm_x": max(0.0, min(1.0, cur_x / scr_w)),
                                "norm_y": max(0.0, min(1.0, cur_y / scr_h))
                            })
                elif not hit_edge and not self.is_remote_active:
                    dwell_start = 0.0

                # While remote is active, check if user moves cursor back into PC screen or moves on phone
                if self.is_remote_active:
                    moved_inward = False
                    if self.configured_edge == "RIGHT" and cur_x < scr_w - 30:
                        moved_inward = True
                    elif self.configured_edge == "LEFT" and cur_x > 30:
                        moved_inward = True
                    elif self.configured_edge == "TOP" and cur_y > 30:
                        moved_inward = True
                    elif self.configured_edge == "BOTTOM" and cur_y < scr_h - 30:
                        moved_inward = True

                    if moved_inward:
                        self.is_remote_active = False
                        logger.info(f"Cursor returned to PC from {self.configured_edge} edge")
                        if self.send_fn:
                            self.send_fn({
                                "type": "edge_handoff",
                                "device_id": self.device_id,
                                "event": "EXIT_REMOTE",
                                "edge": self.configured_edge
                            })
                    else:
                        dx = cur_x - self._last_x
                        dy = cur_y - self._last_y
                        self._last_x = cur_x
                        self._last_y = cur_y
                        if self.send_fn and (abs(dx) > 0.5 or abs(dy) > 0.5):
                            self.send_fn({
                                "type": "pointer_move",
                                "device_id": self.device_id,
                                "edge": self.configured_edge,
                                "dx": dx,
                                "dy": dy,
                                "norm_x": max(0.0, min(1.0, cur_x / scr_w)),
                                "norm_y": max(0.0, min(1.0, cur_y / scr_h))
                            })

            except Exception as e:
                pass


edge_control_manager = EdgeControlManager()
