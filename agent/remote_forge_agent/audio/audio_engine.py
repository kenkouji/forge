"""
RemoteForge Bi-Directional Audio Engine (Agent)
==============================================
Low-latency system audio loopback capture (PC to Phone) and
wireless microphone receiver (Phone to PC) using 44.1kHz / 16-bit PCM frames.
Captures clean system audio via ScreenCaptureKit without microphone feedback,
eliminating crackles and latency.
"""

import os
import sys
import time
import shutil
import logging
import threading
import subprocess
from typing import Dict, Any, Optional

logger = logging.getLogger("RemoteForgeAudioEngine")


class AudioEngine:
    def __init__(self):
        self.is_active: bool = False
        self.session_id: Optional[str] = None
        self.mode: str = "sink"  # "sink", "source", "duplex"
        self.sample_rate: int = 44100
        self.channels: int = 2
        self.codec: str = "pcm"
        self.packets_transferred: int = 0
        self.started_at: float = 0.0
        self.device_id: str = "macbook-pro"
        self.server_ws_url: str = "ws://127.0.0.1:8090"
        self._stream_thread: Optional[threading.Thread] = None
        self._helper_proc: Optional[subprocess.Popen] = None
        self._audio_queue = bytearray()
        self._queue_lock = threading.Lock()

    def set_context(self, device_id: str, server_ws_url: str):
        self.device_id = device_id
        self.server_ws_url = server_ws_url

    def start(
        self,
        session_id: str,
        mode: str = "sink",
        sample_rate: int = 48000,
        channels: int = 2,
        codec: str = "pcm"
    ) -> Dict[str, Any]:
        self.session_id = session_id
        self.mode = mode
        self.sample_rate = 48000  # Hardware-native 48kHz
        self.channels = channels
        self.codec = codec
        self.is_active = True
        self.started_at = time.time()
        self.packets_transferred = 0

        with self._queue_lock:
            self._audio_queue.clear()

        # Launch streaming worker thread
        self._stream_thread = threading.Thread(target=self._stream_worker, daemon=True)
        self._stream_thread.start()

        logger.info(f"Audio Engine started: mode={mode}, rate={self.sample_rate}Hz on {sys.platform}")
        return {
            "status": "started",
            "session_id": session_id,
            "mode": mode,
            "sample_rate": self.sample_rate,
            "channels": channels
        }

    def stop(self) -> Dict[str, Any]:
        prev = self.session_id
        self.is_active = False
        self.session_id = None

        if self._helper_proc:
            try:
                self._helper_proc.terminate()
                self._helper_proc.wait(timeout=1.0)
            except Exception:
                try:
                    self._helper_proc.kill()
                except Exception:
                    pass
            self._helper_proc = None

        with self._queue_lock:
            self._audio_queue.clear()

        logger.info(f"Audio Engine stopped: session {prev}")
        return {"status": "stopped", "session_id": prev}

    def _reader_loop(self, proc: subprocess.Popen):
        """Continuously reads PCM bytes from the native system audio helper."""
        try:
            while self.is_active and proc.poll() is None:
                data = proc.stdout.read(4096)
                if not data:
                    break
                with self._queue_lock:
                    self._audio_queue.extend(data)
                    # Cap queue to 200ms to keep latency near zero
                    max_len = int(self.sample_rate * 0.2 * self.channels * 2)
                    if len(self._audio_queue) > max_len:
                        del self._audio_queue[:-max_len]
        except Exception as e:
            logger.debug(f"Audio helper reader finished: {e}")

    def _stream_worker(self):
        """
        Background worker that connects to /ws/audio/{device_id} and transmits
        real 44.1kHz stereo PCM chunks continuously with zero crackles and low latency.
        """
        import websockets.sync.client as ws_sync

        audio_ws_url = f"{self.server_ws_url.rstrip('/')}/ws/audio/{self.device_id}"
        logger.info(f"Audio worker connecting to {audio_ws_url}...")

        frames = int(self.sample_rate * 0.02)  # 20ms @ 44.1kHz = 882 frames
        chunk_bytes = frames * self.channels * 2  # 3528 bytes for stereo 16-bit
        silent_chunk = bytes(chunk_bytes)

        if sys.platform == "darwin":
            bin_path = os.path.abspath(
                os.path.join(os.path.dirname(__file__), "..", "bin", "audio_capture_helper")
            )
            swift_path = bin_path + ".swift"
            if not os.path.exists(bin_path) and os.path.exists(swift_path):
                try:
                    subprocess.run(["swiftc", "-O", "-framework", "ScreenCaptureKit", "-framework", "CoreMedia", "-framework", "AVFoundation", "-framework", "Foundation", swift_path, "-o", bin_path], check=True)
                except Exception as ce:
                    logger.warning(f"Could not compile audio_capture_helper: {ce}")
            if os.path.exists(bin_path):
                try:
                    self._helper_proc = subprocess.Popen(
                        [bin_path, str(self.sample_rate), str(self.channels)],
                        stdin=subprocess.PIPE,
                        stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE
                    )
                    reader_t = threading.Thread(target=self._reader_loop, args=(self._helper_proc,), daemon=True)
                    reader_t.start()
                    logger.info("Spawned native ScreenCaptureKit audio_capture_helper")
                except Exception as e:
                    logger.warning(f"Could not launch audio helper: {e}")

        while self.is_active:
            try:
                with ws_sync.connect(audio_ws_url) as ws:
                    logger.info("Audio WebSocket connected! Beginning live audio stream...")

                    while self.is_active:
                        t_start = time.perf_counter()

                        payload = None
                        with self._queue_lock:
                            if len(self._audio_queue) >= chunk_bytes:
                                payload = bytes(self._audio_queue[:chunk_bytes])
                                del self._audio_queue[:chunk_bytes]

                        if payload is None:
                            # Clean silence without any crackle or noise
                            payload = silent_chunk

                        ws.send(payload)
                        self.packets_transferred += 1

                        # Keep steady 20ms frame interval (50 packets / sec)
                        elapsed = time.perf_counter() - t_start
                        sleep_time = 0.02 - elapsed
                        if sleep_time > 0.001:
                            time.sleep(sleep_time)

            except Exception as e:
                if self.is_active:
                    logger.debug(f"Audio WS retry in 1s: {e}")
                    time.sleep(1.0)


audio_engine = AudioEngine()
