import os
import io
import sys
import time
import base64
import tempfile
import subprocess
from typing import Dict, Any
from PIL import Image

def capture_screenshot(quality: int = 70, mode: str = "full", max_width: int = 1280) -> Dict[str, Any]:
    """
    Captures display screenshot and returns base64 JPEG data.
    Supports mode='preview' (downscaled for high-FPS, low-latency mobile view)
    and mode='full' (full-resolution capture).
    """
    start_t = time.perf_counter()
    temp_path = None
    try:
        img = None
        # Try direct in-memory grab first (fastest, no disk I/O)
        try:
            from PIL import ImageGrab
            img = ImageGrab.grab()
        except Exception:
            pass

        # Fallback to screencapture utility on macOS if ImageGrab failed
        if img is None and sys.platform == "darwin":
            with tempfile.NamedTemporaryFile(suffix=".jpg", delete=False) as tf:
                temp_path = tf.name
            cmd = ["screencapture", "-x", "-t", "jpg", temp_path]
            res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if res.returncode == 0 and os.path.exists(temp_path) and os.path.getsize(temp_path) > 0:
                img = Image.open(temp_path)

        if img is None:
            raise RuntimeError("No available screenshot capture backend succeeded.")

        orig_width, orig_height = img.size

        # Resize for preview mode if needed
        if mode == "preview" and orig_width > max_width:
            ratio = max_width / float(orig_width)
            new_height = int(orig_height * ratio)
            img = img.resize((max_width, new_height), Image.Resampling.BILINEAR)

        # Convert to RGB if needed for JPEG encoding
        if img.mode != "RGB":
            img = img.convert("RGB")

        width, height = img.size
        out_buf = io.BytesIO()
        jpeg_q = max(55, min(quality, 75)) if mode == "preview" else quality
        img.save(out_buf, format="JPEG", quality=jpeg_q, optimize=True)
        final_bytes = out_buf.getvalue()

        b64_data = base64.b64encode(final_bytes).decode("utf-8")
        capture_dur_ms = round((time.perf_counter() - start_t) * 1000, 2)
        payload_kb = round(len(final_bytes) / 1024, 2)

        return {
            "mode": mode,
            "width": width,
            "height": height,
            "format": "jpeg",
            "capture_duration_ms": capture_dur_ms,
            "payload_size_kb": payload_kb,
            "data_base64": b64_data
        }
    except Exception as e:
        raise RuntimeError(f"Screenshot capture failed: {str(e)}")
    finally:
        if temp_path and os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except Exception:
                pass
