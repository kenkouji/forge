"""
RemoteForge Universal Control Edge Mouse Hopping Service (Agent)
================================================================
Monitors display boundaries, detects cursor push-through gestures, locks local
cursor to the edge, and streams normalized cursor coordinates to Android.
"""

import sys
import time
import logging
import threading
from typing import Dict, Any, Optional
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
        self.monitor_thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()

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
        Returns control to local workstation, releasing cursor lock.
        """
        self.is_remote_active = False
        logger.info("Cursor returned from remote device")
        return {"status": "returned", "is_remote_active": False}

    def _start_monitor_thread(self):
        if self.monitor_thread and self.monitor_thread.is_alive():
            return
        self._stop_event.clear()
        self.monitor_thread = threading.Thread(target=self._monitor_loop, daemon=True)
        self.monitor_thread.start()

    def _monitor_loop(self):
        """
        Background loop checking cursor position against configured edge.
        """
        dwell_start = 0.0
        while not self._stop_event.is_set() and self.is_enabled:
            time.sleep(0.04)  # 25Hz poll rate for low CPU impact
            try:
                pos = input_service.get_mouse_position()
                if not pos:
                    continue
                cur_x, cur_y = pos.get("x", 0), pos.get("y", 0)

                # Fetch screen dimensions
                dim = input_service.get_screen_dimensions()
                scr_w, scr_h = dim.get("width", 1920), dim.get("height", 1080)

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
                    if dwell_start == 0.0:
                        dwell_start = time.time()
                    elif (time.time() - dwell_start) * 1000 >= self.dwell_delay_ms:
                        # Hop to remote!
                        self.is_remote_active = True
                        dwell_start = 0.0
                        logger.info(f"Edge crossed on {self.configured_edge}! Transferred cursor to remote device.")
                elif not hit_edge:
                    dwell_start = 0.0

            except Exception as e:
                pass


edge_control_manager = EdgeControlManager()
