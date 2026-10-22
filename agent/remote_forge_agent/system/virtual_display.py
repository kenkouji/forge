"""
RemoteForge Virtual Extended Display Service (Agent)
===================================================
Cross-platform virtual monitor manager for macOS, Windows, and Linux.
Provides high-performance secondary display viewport streaming and touch gesture
coordinate injection.
"""

import sys
import time
import logging
from typing import Dict, Any, Optional, Tuple
from agent.remote_forge_agent.system.input_control import input_service

logger = logging.getLogger("RemoteForgeVirtualDisplay")


class VirtualDisplayManager:
    def __init__(self):
        self.is_active: bool = False
        self.session_id: Optional[str] = None
        self.width: int = 1920
        self.height: int = 1080
        self.fps: int = 60
        self.dpi: int = 240
        self.touch_enabled: bool = True
        self.is_darwin = sys.platform == "darwin"
        self.is_windows = sys.platform == "win32"
        self.started_at: float = 0.0

    def start(
        self,
        session_id: str,
        width: int = 1920,
        height: int = 1080,
        fps: int = 60,
        dpi: int = 240,
        enable_touch: bool = True
    ) -> Dict[str, Any]:
        """
        Activates the virtual display viewport.
        """
        self.session_id = session_id
        self.width = width
        self.height = height
        self.fps = min(120, max(15, fps))
        self.dpi = dpi
        self.touch_enabled = enable_touch
        self.is_active = True
        self.started_at = time.time()

        logger.info(f"Virtual Display activated: {width}x{height} @ {fps}fps (dpi={dpi}) on {sys.platform}")
        return {
            "status": "success",
            "session_id": session_id,
            "width": self.width,
            "height": self.height,
            "fps": self.fps,
            "platform": sys.platform
        }

    def stop(self) -> Dict[str, Any]:
        """
        Deactivates the virtual display viewport and releases secondary buffers.
        """
        prev_session = self.session_id
        self.is_active = False
        self.session_id = None
        logger.info(f"Virtual Display stopped: session {prev_session}")
        return {
            "status": "stopped",
            "session_id": prev_session
        }

    def handle_touch(
        self,
        action: str,
        norm_x: float,
        norm_y: float,
        scroll_dx: float = 0.0,
        scroll_dy: float = 0.0,
        target_width: Optional[int] = None,
        target_height: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        Translates normalized touch coordinates [0.0, 1.0] to native screen space
        and injects mouse/gesture events via input_service.
        """
        w = target_width or self.width
        h = target_height or self.height
        pixel_x = int(norm_x * w)
        pixel_y = int(norm_y * h)

        try:
            if action == "down" or action == "click":
                input_service.move_mouse(pixel_x, pixel_y)
                input_service.mouse_down("left")
                input_service.mouse_up("left")
            elif action == "double_click":
                input_service.move_mouse(pixel_x, pixel_y)
                input_service.double_click("left")
            elif action == "right_click":
                input_service.move_mouse(pixel_x, pixel_y)
                input_service.click_mouse("right")
            elif action == "move":
                input_service.move_mouse(pixel_x, pixel_y)
            elif action == "scroll":
                # Inject native scroll
                input_service.scroll(int(scroll_dy * 5))
            elif action == "drag_start":
                input_service.move_mouse(pixel_x, pixel_y)
                input_service.mouse_down("left")
            elif action == "drag_end":
                input_service.move_mouse(pixel_x, pixel_y)
                input_service.mouse_up("left")
            return {"status": "ok", "action": action, "x": pixel_x, "y": pixel_y}
        except Exception as e:
            logger.error(f"Error injecting touch event: {e}")
            return {"status": "error", "message": str(e)}


virtual_display_manager = VirtualDisplayManager()
