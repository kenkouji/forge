# RemoteForge PC Agent Documentation

The RemoteForge Agent (`agent/remote_forge_agent/`) is a lightweight background service running on the target computer (Mac, Windows, or Linux).

## 1. Core Architecture Principles
1. **Outbound-Only Connection**: The agent never opens or listens on any inbound port. It establishes an outbound authenticated WebSocket connection (`/ws/agent?token=<AGENT_TOKEN>`) to the RemoteForge backend.
2. **Auto-Reconnection**: Resilient against network drops, Wi-Fi switching, and server migrations. Reconnects with exponential backoff (2.0s to 30.0s).
3. **Hardware Telemetry**: Collects real CPU, RAM, Disk, and uptime telemetry via `psutil`.
4. **Strict Local Security Layer**: Validates all paths on the agent itself against canonical user directories (`Desktop`, `Documents`, `Downloads`, `Pictures`, `Projects`). Rejects all system directories (`/etc`, `C:\Windows`, `Program Files`, `.ssh`, `.aws`, `.env`, browser profiles).
5. **Bounded File Streaming**: Files are transferred in 256 KB memory chunks. No large buffers or memory exhaustion.

---

## 2. Remote Access to Acer Aspire 7 (Windows 11, RTX 3050)

The target Windows machine is the **Acer Aspire 7 A715-76G-548J** equipped with an Intel Core i5-12450H and an NVIDIA GeForce RTX 3050 Laptop GPU.

### Setup on Acer Aspire 7
1. Ensure Python 3.10+ is installed on the Acer.
2. Clone or copy the RemoteForge project directory to the Acer.
3. Open Command Prompt or PowerShell in the project directory and run:
   ```cmd
   agent\setup_acer_windows.bat
   ```
   Or manually:
   ```cmd
   python -m venv venv
   call venv\Scripts\activate.bat
   pip install websockets psutil requests
   set PYTHONPATH=.;backend
   python agent\remote_forge_agent\main.py --server wss://<YOUR_SERVER_URL> --token <AGENT_SECRET> --device-name "Acer Aspire 7 (RTX 3050)"
   ```
4. Once started, the Acer will establish an outbound WebSocket connection to the server. No port forwarding or firewall exceptions on the Acer are needed!

### Power Management & Wake-on-LAN Technical Findings
- **Modern Standby (S0 Low Power Idle)**: The Acer Aspire 7 A715-76G uses Windows Modern Standby (S0ix) rather than legacy ACPI S3 sleep.
- **Wake over Ethernet**: Wake-on-LAN is supported when connected via the Realtek Gigabit Ethernet port while in sleep state (S0 standby).
- **Full Shutdown (S5) Wake Limitations**: Modern laptop chipsets disconnect PCIe network controllers from power in full shutdown (S5) unless plugged into AC power with "Fast Startup" enabled.
- **RemoteForge Availability Guarantee**: RemoteForge does **not** rely on full-shutdown wake. Whenever the Acer is running or awoken, the agent automatically re-registers outbound within 2 seconds and is instantly available for transfers and controls.
- **Security Constraint**: RemoteForge never exposes an unauthenticated public wake endpoint. WoL magic packets should only be triggered over an authenticated VPN (e.g., NetBird) broadcast.

---

## 3. CLI Arguments & Environment Variables

| Argument | Environment Variable | Default | Description |
|---|---|---|---|
| `--server` | `REMOTFORGE_SERVER_URL` | `ws://127.0.0.1:8090` | Target server WebSocket address (`ws://` or `wss://`) |
| `--token` | `REMOTFORGE_AGENT_TOKEN` | `rf_agent_secret_token_default_replace_me` | Agent secret authentication token |
| `--device-id` | `REMOTFORGE_DEVICE_ID` | `pc-<hostname>` | Unique device identifier |
| `--device-name` | `REMOTFORGE_DEVICE_NAME` | `Computer (<hostname>)` | Human-readable name for dashboard |

---

## 4. Migration to Dedicated Home Server
When migrating RemoteForge from the development Mac to a dedicated home server:
1. Update `SERVER_PUBLIC_URL` or tunnel URL on the server.
2. Run the agent with the new server address:
   ```bash
   python agent/remote_forge_agent/main.py --server ws://<HOME_SERVER_IP>:8090
   ```
3. Update the Android app in the Settings tab by entering the new server address and tapping "Test Connection (Ping)".
