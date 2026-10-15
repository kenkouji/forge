# RemoteForge Filesystem Security Model

## 1. Zero-Trust User-Data Boundary

RemoteForge is designed from the ground up as a **User-Data-Only** system. It enforces a strict security boundary preventing the AI, server, or client from ever reading, indexing, searching, or transferring operating-system files, system configuration, or credentials.

```text
PC Storage
│
├── Allowed User Roots (Configurable Allowlist)
│   ├── ~/Desktop
│   ├── ~/Documents
│   ├── ~/Downloads
│   ├── ~/Pictures
│   ├── ~/Projects
│   └── ~/Developer
│
└── Forbidden System & Secret Targets (Hard Blocked)
    ├── Root Filesystem: / or C:\
    ├── OS Core: /System, /Library, /private, /usr, /bin, /etc, C:\Windows, System32
    ├── User Secrets: ~/.ssh, ~/.aws, ~/.azure, ~/.kube, .env files
    ├── Keys & Creds: *.key, *.pem, *.p12, login.keychain
    └── Shell History: .bash_history, .zsh_history
```

---

## 2. Six-Step Allowlist Enforcement Algorithm

Before any file operation (`list_directory`, `search_files`, `get_file_metadata`, `download_file`) is performed, the PC agent executes the following verification pipeline:

1. **Path Expansion**: Resolves home shortcuts (`~` and environment variables).
2. **Canonicalization**: Computes the strict real path using `os.path.realpath(os.path.abspath(path))`, fully resolving all symbolic links and relative segments (`..`).
3. **Root System Check**: Rejects any path matching `/`, `C:\`, or system root mounts.
4. **Platform Prefix Verification**: Rejects any canonical path starting with operating-system forbidden prefixes (`/System`, `/Library`, `/private`, `/usr`, `/etc`, `C:\Windows`, `C:\Program Files`, etc.).
5. **Containment in Allowed Roots**: Verifies that the canonical path is equal to or a subdirectory of at least one explicitly authorized user root directory.
6. **Credential & Secret Pattern Filter**: Evaluates each path component against forbidden secret patterns (`.ssh`, `id_rsa`, `.aws`, `.env`, `keychain`). If any component matches, access is rejected.

---

## 3. Threat Vector Mitigation

| Threat Vector | Attack Scenario | RemoteForge Defense |
|---|---|---|
| **Path Traversal** | `~/Documents/../../Library/Preferences` | `os.path.realpath` resolves the real destination; rejected at step 4/5. |
| **Symlink Escape** | `~/Documents/system_link -> /etc` | Canonicalization resolves to `/etc`, rejected as restricted system directory. |
| **Credential Exfiltration** | `~/Documents/.ssh/id_rsa` | Sensitive pattern check triggers; rejected with credential safety alert. |
| **Broad Scan** | "Scan everything on my PC" | Filesystem scanner strictly iterates only allowed roots; never root `/`. |
| **Arbitrary Shell Execution** | `POST /shell` or command injection | Arbitrary shell endpoints do not exist; inputs are strictly parsed tool arguments. |

---

## 4. Zero Persistent File Storage Guarantee

- The RemoteForge server **never permanently stores user file contents**.
- File transfers stream directly through an ephemeral encrypted connection from the PC agent to the requesting client.
- The server database records only lightweight metadata (`filename`, `size`, `timestamp`, `audit_id`).
