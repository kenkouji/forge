import os
import sys
import socket
import argparse
import asyncio
import logging
import signal
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer
from agent.remote_forge_agent.commands.dispatcher import CommandDispatcher
from agent.remote_forge_agent.connection import AgentConnection

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [Agent]: %(message)s"
)
logger = logging.getLogger("remoteforge.agent")

def get_default_device_id() -> str:
    try:
        host = socket.gethostname().lower().replace(" ", "-").replace(".", "-")
        return f"pc-{host}"
    except Exception:
        return "pc-device-1"

def parse_args():
    parser = argparse.ArgumentParser(description="RemoteForge PC Agent")
    parser.add_argument(
        "--server",
        default=os.getenv("REMOTFORGE_SERVER_URL", "ws://127.0.0.1:8000"),
        help="WebSocket URL of RemoteForge Server (e.g., ws://<NETBIRD_IP>:8000 or ws://127.0.0.1:8000)"
    )
    parser.add_argument(
        "--token",
        default=os.getenv("REMOTFORGE_AGENT_TOKEN", "rf_agent_secret_token_default_replace_me"),
        help="Secret agent authentication token matching the server AGENT_TOKEN"
    )
    parser.add_argument(
        "--device-id",
        default=os.getenv("REMOTFORGE_DEVICE_ID", get_default_device_id()),
        help="Unique identifier for this computer"
    )
    parser.add_argument(
        "--device-name",
        default=os.getenv("REMOTFORGE_DEVICE_NAME", f"Computer ({socket.gethostname()})"),
        help="Human-readable name for this device"
    )
    return parser.parse_args()

def main():
    args = parse_args()

    # Convert http/https server url if provided to ws/wss
    server_ws_url = args.server
    if server_ws_url.startswith("http://"):
        server_ws_url = "ws://" + server_ws_url[7:]
    elif server_ws_url.startswith("https://"):
        server_ws_url = "wss://" + server_ws_url[8:]
    elif not server_ws_url.startswith("ws://") and not server_ws_url.startswith("wss://"):
        server_ws_url = f"ws://{server_ws_url}"

    logger.info("==================================================")
    logger.info("   Starting RemoteForge PC Agent v0.1.0           ")
    logger.info(f"   Device ID   : {args.device_id}")
    logger.info(f"   Device Name : {args.device_name}")
    logger.info(f"   Target Server: {server_ws_url}")
    logger.info("==================================================")

    # Initialize Security Allowlist & Enforcer
    enforcer = FilesystemSecurityEnforcer()
    logger.info(f"Strict User-Data Allowlist initialized with {len(enforcer.get_allowed_roots())} root directories:")
    for r in enforcer.get_allowed_roots():
        logger.info(f"  ✓ {r}")

    # Initialize Command Dispatcher
    dispatcher = CommandDispatcher(enforcer=enforcer)

    # Initialize Connection
    conn = AgentConnection(
        server_ws_url=server_ws_url,
        agent_token=args.token,
        device_id=args.device_id,
        device_name=args.device_name,
        dispatcher=dispatcher
    )

    # Wire context to dispatcher, edge control, and audio engine
    dispatcher.set_agent_context(
        device_id=args.device_id,
        server_ws_url=server_ws_url,
        send_fn=conn.send_message_sync
    )

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    def handle_signal():
        logger.info("Received termination signal. Shutting down agent...")
        conn.stop()

    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, handle_signal)
        except NotImplementedError:
            pass

    try:
        loop.run_until_complete(conn.run())
    except KeyboardInterrupt:
        logger.info("Agent process interrupted by user.")
    finally:
        loop.close()
        logger.info("RemoteForge PC Agent stopped.")

if __name__ == "__main__":
    main()
