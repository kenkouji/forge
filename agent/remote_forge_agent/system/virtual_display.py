"""
RemoteForge Virtual Extended Display Service (Agent)
===================================================
Cross-platform virtual monitor manager for macOS, Windows, and Linux.
Provides high-performance secondary display viewport streaming and touch gesture
coordinate injection.
"""

import os
import sys
import time
import ctypes
import logging
import subprocess
from typing import Dict, Any, Optional, Tuple
from agent.remote_forge_agent.system.input_control import input_service
from agent.remote_forge_agent.system.screen_streamer import screen_streamer

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
        self.virtual_display_id: Optional[int] = None
        self._proc: Optional[subprocess.Popen] = None

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
        Activates real secondary monitor via CGVirtualDisplay on macOS.
        """
        self.session_id = session_id
        self.width = width
        self.height = height
        self.fps = min(120, max(15, fps))
        self.dpi = dpi
        self.touch_enabled = enable_touch
        self.is_active = True
        self.started_at = time.time()

        # On macOS, spawn real hardware-level virtual secondary display
        if self.is_darwin:
            if self._proc:
                try:
                    self._proc.terminate()
                    self._proc.wait(timeout=1.0)
                except Exception:
                    try:
                        self._proc.kill()
                    except Exception:
                        pass
                self._proc = None
            subprocess.run(["pkill", "-f", "virtual_display_helper"], check=False)

            bin_path = os.path.abspath(
                os.path.join(os.path.dirname(__file__), "..", "bin", "virtual_display_helper")
            )
            swift_path = bin_path + ".swift"
            if not os.path.exists(bin_path) and os.path.exists(swift_path):
                try:
                    subprocess.run(["swiftc", "-O", "-framework", "CoreGraphics", "-framework", "AppKit", "-framework", "Foundation", swift_path, "-o", bin_path], check=True)
                except Exception as ce:
                    logger.warning(f"Could not compile virtual_display_helper: {ce}")
            if os.path.exists(bin_path):
                try:
                    self._proc = subprocess.Popen(
                        [bin_path, str(width), str(height), str(self.fps), "RemoteForge Secondary Monitor"],
                        stdin=subprocess.PIPE,
                        stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE,
                        text=True
                    )
                    line = self._proc.stdout.readline()
                    if "DISPLAY_ID=" in line:
                        for token in line.strip().split():
                            if token.startswith("DISPLAY_ID="):
                                self.virtual_display_id = int(token.split("=")[1])
                                screen_streamer.target_display_id = self.virtual_display_id
                                logger.info(f"Attached real macOS secondary display: ID={self.virtual_display_id}")
                except Exception as e:
                    logger.warning(f"Could not launch virtual display helper: {e}")

        logger.info(f"Virtual Display activated: {width}x{height} @ {fps}fps (dpi={dpi}) on {sys.platform}")
        return {
            "status": "success",
            "session_id": session_id,
            "width": self.width,
            "height": self.height,
            "fps": self.fps,
            "display_id": self.virtual_display_id,
            "platform": sys.platform
        }

    def stop(self) -> Dict[str, Any]:
        """
        Deactivates the virtual display and cleanly releases secondary monitor.
        """
        prev_session = self.session_id
        self.is_active = False
        self.session_id = None

        if self._proc:
            try:
                self._proc.terminate()
                self._proc.wait(timeout=2.0)
            except Exception:
                self._proc.kill()
            self._proc = None

        self.virtual_display_id = None
        screen_streamer.target_display_id = None
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
        Translates normalized touch coordinates [0.0, 1.0] to native secondary display
        pixel space and injects mouse/gesture events via input_service.
        """
        target_x = 0
        target_y = 0

        if self.is_darwin and self.virtual_display_id:
            try:
                cg = ctypes.cdll.LoadLibrary('/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics')
                class CGPoint(ctypes.Structure):
                    _fields_ = [('x', ctypes.c_double), ('y', ctypes.c_double)]
                class CGSize(ctypes.Structure):
                    _fields_ = [('width', ctypes.c_double), ('height', ctypes.c_double)]
                class CGRect(ctypes.Structure):
                    _fields_ = [('origin', CGPoint), ('size', CGSize)]
                cg.CGDisplayBounds.restype = CGRect
                cg.CGDisplayBounds.argtypes = [ctypes.c_uint32]
                b = cg.CGDisplayBounds(self.virtual_display_id)
                target_x = int(b.origin.x + (norm_x * b.size.width))
                target_y = int(b.origin.y + (norm_y * b.size.height))
            except Exception:
                dim = input_service.get_screen_dimensions()
                target_x = int(norm_x * dim.get("width", 1920))
                target_y = int(norm_y * dim.get("height", 1080))
        else:
            dim = input_service.get_screen_dimensions()
            target_x = int(norm_x * dim.get("width", 1920))
            target_y = int(norm_y * dim.get("height", 1080))

        try:
            if action == "down" or action == "click":
                input_service.move_mouse(target_x, target_y)
                input_service.mouse_down("left")
                input_service.mouse_up("left")
            elif action == "double_click":
                input_service.move_mouse(target_x, target_y)
                input_service.double_click("left")
            elif action == "right_click":
                input_service.move_mouse(target_x, target_y)
                input_service.click_mouse("right")
            elif action == "move":
                input_service.move_mouse(target_x, target_y)
            elif action == "drag_start":
                input_service.move_mouse(target_x, target_y)
                input_service.mouse_down("left")
            elif action == "drag_end":
                input_service.mouse_up("left")
            return {"status": "ok", "action": action, "x": target_x, "y": target_y}
        except Exception as e:
            logger.error(f"Error injecting touch event: {e}")
            return {"status": "error", "message": str(e)}


virtual_display_manager = VirtualDisplayManager()
