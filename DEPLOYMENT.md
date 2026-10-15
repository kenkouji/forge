# RemoteForge Production Deployment & Migration Guide

## 1. Migrating to Dedicated Home Server

RemoteForge is designed to run seamlessly in Docker on any dedicated home server (Ubuntu, Debian, Proxmox, Raspberry Pi 5).

### Step 1: Transfer Repository to Home Server
```bash
git clone <repo-url> /opt/remoteforge
cd /opt/remoteforge
```

### Step 2: Configure Production Environment
```bash
cp .env.example .env
nano .env
```
Ensure you change `JWT_SECRET` and `AGENT_TOKEN` to strong cryptographically random strings.

### Step 3: Launch with Docker Compose
```bash
docker compose up -d --build
```
The server will build the web application, start FastAPI on `0.0.0.0:8000`, and initialize the SQLite database volume.

---

## 2. NetBird VPN Configuration on Dedicated Server

Install NetBird on your home server:
```bash
# Ubuntu / Debian
curl -fsSL https://pkgs.netbird.io/install.sh | sh
netbird up --setup-key <YOUR_NETBIRD_SETUP_KEY>
```
Check assigned NetBird IP:
```bash
netbird status
# Example: 100.117.x.x
```
Your RemoteForge instance is now reachable securely across all your connected devices at `http://100.117.x.x:8000` without opening any router ports!

---

## 3. Running PC Agent as a Systemd Service (Linux/macOS)

To ensure the PC Agent starts automatically whenever the computer boots:

### Systemd Service (`/etc/systemd/system/remoteforge-agent.service`)
```ini
[Unit]
Description=RemoteForge PC Agent Service
After=network-online.target

[Service]
Type=simple
User=YOUR_USERNAME
WorkingDirectory=/path/to/remoteforge
Environment=PYTHONPATH=backend:.
Environment=REMOTFORGE_SERVER_URL=ws://<HOME_SERVER_NETBIRD_IP>:8000
Environment=REMOTFORGE_AGENT_TOKEN=YOUR_AGENT_TOKEN
ExecStart=/path/to/remoteforge/venv/bin/python3 agent/remote_forge_agent/main.py
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```
Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now remoteforge-agent.service
```

### macOS LaunchAgent (`~/Library/LaunchAgents/com.remoteforge.agent.plist`)
Place in `~/Library/LaunchAgents/` and load with:
```bash
launchctl load ~/Library/LaunchAgents/com.remoteforge.agent.plist
```
