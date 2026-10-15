# RemoteForge REST & WebSocket API Reference

Base URL (Local / NetBird): `http://<SERVER_IP>:8000`

Interactive OpenAPI Documentation: `http://<SERVER_IP>:8000/docs`

---

## 1. System Health & Authentication

### `GET /api/health`
Returns server operational status and version.
```json
{
  "status": "ok",
  "server": "RemoteForge",
  "version": "0.1.0"
}
```

### `POST /api/auth/login`
Authenticates a user and returns a JWT access token.
- **Request Body:**
  ```json
  {
    "username": "admin",
    "password": "remoteforge_secure_pass"
  }
  ```
- **Response:**
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "token_type": "bearer",
    "username": "admin",
    "role": "admin"
  }
  ```

---

## 2. Device Management & Commands

### `GET /api/devices`
Returns all registered remote computers and live connection state.
- **Headers:** `Authorization: Bearer <TOKEN>`
- **Response:**
  ```json
  [
    {
      "id": "pc-my-macbook",
      "name": "My PC",
      "os_name": "macOS",
      "os_version": "14.4.1",
      "agent_version": "0.1.0",
      "is_online": true,
      "last_seen": "2026-10-13T11:20:00Z"
    }
  ]
  ```

### `POST /api/devices/{device_id}/commands`
Dispatches a controlled tool execution to the connected PC agent.
- **Headers:** `Authorization: Bearer <TOKEN>`
- **Request Body:**
  ```json
  {
    "tool": "get_system_status",
    "arguments": {},
    "confirmed": false
  }
  ```
- **Response:**
  ```json
  {
    "request_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "device_id": "pc-my-macbook",
    "tool": "get_system_status",
    "permission_level": "LOW",
    "requires_confirmation": false,
    "status": "COMPLETED",
    "result": {
      "os_name": "macOS",
      "cpu": { "percent": 14.2, "logical_cores": 8 },
      "ram": { "percent": 62.1, "used_gb": 9.9 },
      "uptime_formatted": "3h 45m"
    },
    "error": null
  }
  ```

---

## 3. Filesystem Operations (User-Data Only)

### `GET /api/files/list`
Lists items in an authorized user folder.
- **Query Params:** `device_id=<id>&path=<allowed_path>`

### `GET /api/files/search`
Searches strictly within allowed user roots.
- **Query Params:** `device_id=<id>&query=<search_term>`

---

## 4. WebSockets

### `WS /ws/agent?token=<AGENT_TOKEN>`
Used by the PC Agent for outbound bidirectional communication.
- **Message Types Sent by Agent:**
  - `register`: `{ "type": "register", "device_id": "...", "os_name": "..." }`
  - `heartbeat`: `{ "type": "heartbeat", "device_id": "..." }`
  - `command_result`: `{ "type": "command_result", "request_id": "...", "success": true, "result": {...} }`
- **Messages Received by Agent:**
  - `command`: `{ "type": "command", "request_id": "...", "tool": "...", "arguments": {...} }`

### `WS /ws/client?token=<JWT_TOKEN>`
Used by Browser and Android clients to receive live device state changes, heartbeat updates, and command execution events.
