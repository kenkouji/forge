# RemoteForge Multi-Layer Security Architecture

RemoteForge implements an 8-layer defense-in-depth model. No single layer is solely responsible for security.

```text
Layer 1: NetBird Overlay VPN (Network Isolation)
    ↓
Layer 2: Application Authentication (JWT Bearer Tokens)
    ↓
Layer 3: PC Agent Authentication (Secret Agent Tokens)
    ↓
Layer 4: Controlled Tool Registry (No Arbitrary Shell)
    ↓
Layer 5: Permission Tiers (LOW, MEDIUM, HIGH)
    ↓
Layer 6: Explicit User Confirmation Gates (Restart / Shutdown)
    ↓
Layer 7: PC Agent Canonicalization & Filesystem Allowlist
    ↓
Layer 8: Immutable Audit Logging (Sensitive Data Redacted)
```

---

## 1. Authentication & Session Management
- **User Tokens**: Signed with HMAC-SHA256 (HS256) with configurable expiration (default 24 hours).
- **Passwords**: Hashed with BCrypt with unique cryptographic salts.
- **Agent Tokens**: Checked on WebSocket upgrade handshake (`/ws/agent?token=...`).

---

## 2. Controlled Tool Permission Tiers

| Tool | Permission Tier | Confirmation Required | Description |
|---|---|---|---|
| `get_system_status` | **LOW** | No | Hardware metrics (CPU, RAM, Disk, Uptime) |
| `take_screenshot` | **LOW** | No | Ephemeral screen capture |
| `open_browser` | **LOW** | No | Launch browser to URL |
| `browser_search` | **LOW** | No | Search query in browser |
| `search_files` | **LOW** | No | Search within allowed user folders |
| `list_directory` | **LOW** | No | List files in allowed folders |
| `lock_pc` | **LOW** | No | Lock workstation session |
| `close_application` | **MEDIUM** | No | Gracefully close application |
| `download_file` | **MEDIUM** | No | Stream user-data file |
| `restart_pc` | **HIGH** | **YES** | System reboot (requires confirmation modal) |
| `shutdown_pc` | **HIGH** | **YES** | System shutdown (requires confirmation modal) |

---

## 3. No Arbitrary Shell Execution
- RemoteForge explicitly prohibits endpoints like `POST /shell` or functions like `execute_shell("anything")`.
- Disallowed commands (`sh`, `bash`, `zsh`, `sudo`, `rm`, `curl`, `wget`) are parsed and blocked at the tool argument validator layer before any message reaches the PC agent.
