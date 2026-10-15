# RemoteForge: Secure Remote AI Computer Control Platform

> Production-oriented Final-Year Computer Science & Engineering Project.
> Secure remote PC control over NetBird VPN, HTTPS tunnels, and local networks from Web browsers and native Android devices, backed by policy-enforced tool execution, zero persistent file storage, and a strict user-data-only filesystem sandbox.

---

## 1. System Vision & Architecture

RemoteForge decouples the coordinating server from the controlled computer. The AI and remote clients never possess arbitrary OS shell access; all requests are converted into structured tool calls validated by a policy engine and enforced by the PC agent.

```text
                  ANY CLIENT DEVICE
             ┌─────────────────────────┐
             │ Android App (Compose)   │
             │ Web Browser (React/Vite)│
             └────────────┬────────────┘
                          │
                HTTPS / WSS / NetBird VPN
                          │
                          ▼
             ┌─────────────────────────┐
             │   RemoteForge Server    │
             │                         │
             │ FastAPI (0.0.0.0:8090)  │
             │ SQLite / SQLAlchemy ORM │
             │ JWT Authentication      │
             │ Rate-Limited Login Prot │
             │ WebSocket Coordinator   │
             │ Controlled Tool Policy  │
             │ Immutable Audit Logs    │
             │ Bounded Streaming (256K)│
             └────────────┬────────────┘
                          │
                 Outbound Authenticated
                       WebSocket
                          │
                          ▼
             ┌─────────────────────────┐
             │    RemoteForge Agent    │
             │   running on Target PC  │
             │ (MacBook / Acer Aspire 7│
             │                         │
             │ Controlled Tool Dispatch│
             │ User Files ONLY Sandbox │
             │ System Telemetry (HUD)  │
             │ Secure Screenshots     │
             │ Chunked File Transfers  │
             └─────────────────────────┘
```

---

## 2. Server Migration Strategy (MacBook → Home Server)

A core requirement of RemoteForge is **complete server-agnosticism**. The current MacBook development environment is temporary. Eventually, the FastAPI server will migrate to a dedicated home server (Linux/Docker/Proxmox).

### Current Active Ports & Network Setup
- **Port:** `8090` (configured to avoid conflict with existing services)
- **Local Access:** `http://127.0.0.1:8090`
- **NetBird VPN Access:** `http://100.117.133.109:8090`
- **Public HTTPS / WSS Tunnel:** `https://dream-utilize-offline-chen.trycloudflare.com`

### Zero-Friction Migration Checklist
Because RemoteForge **strictly forbids hard-coded IPs, usernames, or paths**:
1. Copy or clone the repository to the new home server.
2. Run `cp .env.example .env` and configure port and secrets (`SERVER_HOST=0.0.0.0`, `SERVER_PORT=8090`).
3. Start the server via Docker Compose (`docker compose up -d`) or Python virtualenv.
4. Join the home server to your NetBird VPN network.
5. In the Web dashboard (or Android settings), update the Server URL to the new NetBird IP or public tunnel.
6. The PC Agent automatically reconnects to the new server URL specified via `--server ws://<HOME_SERVER_IP>:8090`.

---

## 3. Strict Filesystem Security Model

RemoteForge is a **USER-DATA-ONLY** platform. Operating system files and credentials can never be indexed, searched, or transferred.

```text
Target PC Filesystem
│
├── Allowed User-Data Sandbox       ✓ ACCESSIBLE
│   ├── Desktop
│   ├── Documents
│   ├── Downloads
│   ├── Pictures
│   ├── Projects
│   └── Explicitly Added Folders
│
└── Forbidden System & Secret Files ✗ STRICTLY DENIED (HTTP 403)
    ├── macOS: /System, /Library, /private, /usr, /etc, /Volumes
    ├── Windows: C:\Windows, C:\Program Files, System32
    ├── SSH Keys: ~/.ssh/id_rsa, id_ed25519
    ├── Cloud Credentials: ~/.aws, ~/.azure, ~/.gcloud
    ├── Secrets: .env files, keychains, certificates
    └── Symlink escapes & Path Traversal (../..)
```

---

## 4. Native Android Application (Jetpack Compose)

Built with Kotlin and Jetpack Compose featuring an Apple Liquid Glass aesthetic.

- **Target Device:** Samsung Galaxy S21 FE 5G (`SM-G990B2`)
- **Debug APK Location:** `android/app/build/outputs/apk/debug/app-debug.apk` (Size: 16 MB)
- **Functional Screens:**
  1. **Home:** Connection status HUD, selected PC, real-time CPU/RAM/Storage ring gauges, quick actions, recent activity preview.
  2. **Devices:** Registered computers, online/offline status, operating system, agent versions, and active machine selector.
  3. **Files:** Real folder navigation, breadcrumbs, search, sorting (Name/Size), multi-selection, file uploads, and downloads.
  4. **Transfers:** Active transfer progress tracking, speeds, cancel, and completed history.
  5. **Activity:** Live server audit logs and security rejection events.
  6. **Settings:** Configurable server URL (NetBird / Tunnel / Local), connection ping test, JWT authentication, and build specs.

### Building & Installing via ADB:
```bash
cd android
./gradlew :app:assembleDebug
adb -s RZCW90Z590X install -r app/build/outputs/apk/debug/app-debug.apk
adb -s RZCW90Z590X shell am start -n com.remoteforge.app/.ui.MainActivity
```

---

## 5. Remote Access to Acer Aspire 7 (Windows 11 with RTX 3050)

The target Windows PC is the Acer Aspire 7 (A715-76G-548J).
- **Outbound Authenticated Connection:** Acer connects outbound via WebSocket to `wss://<SERVER_URL>`. No inbound ports or router port forwards required on Windows.
- **Setup Script:** `agent/setup_acer_windows.bat` sets up the virtualenv and starts the agent.
- **Wake-on-LAN Findings:** Uses Modern Standby (S0ix). WoL is supported over Ethernet when in standby. RemoteForge guarantees instant availability whenever the laptop is running without exposing unauthenticated wake endpoints.

---

## 6. Running Automated Verification Tests

```bash
PYTHONPATH="backend:." ./venv/bin/pytest -v tests/
```

**Results:** 10/10 Passed (100% test pass rate).
- Agent WebSocket registration & heartbeat
- Unauthorized agent token rejection
- Health endpoint status
- Admin JWT login
- Controlled tool permission registry
- Arbitrary shell injection prevention
- Allowed user file access
- Restricted system directory denial (`/etc`, `/System`)
- Secrets denial (`.ssh`, `.aws`, `.env`)
- Path traversal & symlink escape protection

---

## 7. Starting the Services

### Server:
```bash
PYTHONPATH="backend:." ./venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8090
```

### PC Agent (Mac):
```bash
PYTHONPATH="backend:." ./venv/bin/python3 agent/remote_forge_agent/main.py --server ws://127.0.0.1:8090
```

### Public Secure Tunnel:
```bash
./tools/cloudflared tunnel --url http://127.0.0.1:8090
```
*(Public endpoint: `https://dream-utilize-offline-chen.trycloudflare.com`)*
