import os
import sys
from pathlib import Path
from typing import List, Tuple, Optional

# Sensitive patterns that must NEVER be accessed, even if inside an allowed folder
FORBIDDEN_NAME_PATTERNS = {
    ".ssh", "id_rsa", "id_ed25519", "id_dsa", "id_ecdsa",
    ".aws", ".azure", ".gcloud", ".kube",
    ".env", ".env.local", ".env.production", ".env.secret",
    "keychain", "login.keychain", "login.keychain-db",
    ".bash_history", ".zsh_history", ".sh_history",
    "credentials", "secrets", "master.key", "keystore"
}

# Forbidden system paths across platforms
MAC_FORBIDDEN_PREFIXES = [
    "/System", "/Library", "/Applications", "/private", "/usr",
    "/bin", "/sbin", "/var", "/etc", "/opt", "/dev", "/Volumes"
]

WIN_FORBIDDEN_PREFIXES = [
    "C:\\Windows", "C:\\Program Files", "C:\\Program Files (x86)",
    "C:\\ProgramData", "C:\\Recovery", "C:\\System Volume Information"
]

LINUX_FORBIDDEN_PREFIXES = [
    "/root", "/etc", "/bin", "/sbin", "/usr", "/var", "/dev",
    "/sys", "/proc", "/boot", "/lib", "/lib64", "/opt"
]

class SecurityPolicyException(PermissionError):
    pass

class FilesystemSecurityEnforcer:
    def __init__(self, custom_roots: Optional[List[str]] = None):
        self.allowed_roots: List[str] = []
        self._initialize_default_roots(custom_roots)

    def _initialize_default_roots(self, custom_roots: Optional[List[str]] = None):
        home = str(Path.home().resolve())
        defaults = []

        if sys.platform == "darwin":
            # macOS default user data directories
            candidates = ["Desktop", "Documents", "Downloads", "Pictures", "Movies", "Music", "Projects", "Developer"]
            for c in candidates:
                p = os.path.join(home, c)
                if os.path.exists(p):
                    defaults.append(os.path.realpath(p))
        elif sys.platform == "win32":
            # Windows default user data directories
            candidates = ["Desktop", "Documents", "Downloads", "Pictures", "Videos", "Music"]
            for c in candidates:
                p = os.path.join(home, c)
                if os.path.exists(p):
                    defaults.append(os.path.realpath(p))
        else:
            # Linux default user directories
            candidates = ["Desktop", "Documents", "Downloads", "Pictures", "Videos", "Music", "Projects"]
            for c in candidates:
                p = os.path.join(home, c)
                if os.path.exists(p):
                    defaults.append(os.path.realpath(p))

        self.allowed_roots = list(set(defaults))

        if custom_roots:
            for r in custom_roots:
                self.add_allowed_root(r)

    def get_allowed_roots(self) -> List[str]:
        return sorted(list(self.allowed_roots))

    def add_allowed_root(self, path: str) -> str:
        expanded = os.path.expanduser(path)
        canonical = os.path.realpath(os.path.abspath(expanded))

        # Check if canonical is within system forbidden prefixes
        self._verify_not_system_directory(canonical)

        # Ensure directory actually exists and is readable
        if not os.path.isdir(canonical):
            raise SecurityPolicyException(f"Path '{path}' is not an existing directory.")

        if canonical not in self.allowed_roots:
            self.allowed_roots.append(canonical)
        return canonical

    def remove_allowed_root(self, path: str) -> bool:
        canonical = os.path.realpath(os.path.abspath(os.path.expanduser(path)))
        if canonical in self.allowed_roots:
            self.allowed_roots.remove(canonical)
            return True
        return False

    def _verify_not_system_directory(self, canonical_path: str):
        # 1. Reject root directory itself
        if canonical_path in ["/", "C:\\", "C:/"]:
            raise SecurityPolicyException("Access denied: root filesystem cannot be an allowed user directory.")

        # 2. Reject platform forbidden prefixes
        forbidden = MAC_FORBIDDEN_PREFIXES if sys.platform == "darwin" else (
            WIN_FORBIDDEN_PREFIXES if sys.platform == "win32" else LINUX_FORBIDDEN_PREFIXES
        )

        for pref in forbidden:
            pref_norm = os.path.normpath(pref)
            if canonical_path == pref_norm or canonical_path.startswith(pref_norm + os.sep):
                raise SecurityPolicyException(f"Access denied: '{canonical_path}' is a restricted system directory.")

    def validate_file_access(self, target_path: str) -> Tuple[bool, str, str]:
        """
        Validates target_path against security allowlist.
        Returns (is_allowed, canonical_path, message)
        """
        if not target_path or not target_path.strip():
            return False, "", "Access denied: empty path provided."

        raw = target_path.strip()
        expanded = os.path.expanduser(raw)

        # Resolve relative user paths (e.g. "Documents", "Downloads/file.pdf") against user home
        if not os.path.isabs(expanded) and not raw.startswith("~"):
            home = str(Path.home().resolve())
            candidate = os.path.join(home, expanded)
            norm_cand = os.path.realpath(candidate)
            for root in self.allowed_roots:
                norm_root = os.path.normpath(root)
                if norm_cand == norm_root or norm_cand.startswith(norm_root + os.sep):
                    expanded = candidate
                    break

        canonical = os.path.realpath(os.path.abspath(expanded))

        # 1. Enforce system directory rejection
        try:
            self._verify_not_system_directory(canonical)
        except SecurityPolicyException as e:
            return False, canonical, str(e)

        # 2. Check if within ANY allowed root
        is_in_allowed_root = False
        for root in self.allowed_roots:
            norm_root = os.path.normpath(root)
            if canonical == norm_root or canonical.startswith(norm_root + os.sep):
                is_in_allowed_root = True
                break

        if not is_in_allowed_root:
            return False, canonical, "Access denied: target is outside allowed user-data directories."

        # 3. Check for sensitive file / directory patterns
        parts = Path(canonical).parts
        for part in parts:
            if part.lower() in FORBIDDEN_NAME_PATTERNS or any(part.lower().endswith(ext) for ext in [".key", ".pem", ".p12"]):
                return False, canonical, f"Access denied: access to sensitive credential '{part}' is strictly forbidden."

        # 4. Reject symlink escapes (os.path.realpath already resolved target, which was checked above)
        return True, canonical, "Access granted."

    def assert_path_allowed(self, target_path: str) -> str:
        """
        Validates target_path against security allowlist.
        Returns canonical path if granted, or raises SecurityPolicyException.
        """
        allowed, canonical, msg = self.validate_file_access(target_path)
        if not allowed:
            raise SecurityPolicyException(msg)
        return canonical

