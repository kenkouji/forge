import os
import datetime
from pathlib import Path
from typing import Dict, Any, List, Optional
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer, SecurityPolicyException

class UserFilesystemService:
    def __init__(self, security_enforcer: FilesystemSecurityEnforcer):
        self.enforcer = security_enforcer

    def list_directory(self, target_path: Optional[str] = None) -> Dict[str, Any]:
        """
        Lists files within an allowed user directory.
        If target_path is not specified, returns the top-level allowed user roots.
        """
        if not target_path:
            roots = self.enforcer.get_allowed_roots()
            items = []
            for r in roots:
                items.append({
                    "name": os.path.basename(r) or r,
                    "path": r,
                    "is_directory": True,
                    "is_root": True
                })
            return {"current_path": "", "items": items}

        is_allowed, canonical, msg = self.enforcer.validate_file_access(target_path)
        if not is_allowed:
            raise SecurityPolicyException(msg)

        if not os.path.isdir(canonical):
            raise ValueError(f"Path '{canonical}' is not a directory.")

        items = []
        try:
            with os.scandir(canonical) as it:
                for entry in it:
                    # Filter out hidden system files and verify each child against sensitive pattern filter
                    if entry.name.startswith("."):
                        continue
                    child_allowed, _, _ = self.enforcer.validate_file_access(entry.path)
                    if not child_allowed:
                        continue

                    try:
                        stat = entry.stat()
                        items.append({
                            "name": entry.name,
                            "path": entry.path,
                            "is_directory": entry.is_dir(),
                            "size_bytes": stat.st_size if not entry.is_dir() else 0,
                            "modified_at": datetime.datetime.fromtimestamp(stat.st_mtime).isoformat()
                        })
                    except (PermissionError, OSError):
                        continue
        except PermissionError:
            raise PermissionError(f"Permission denied reading directory: {canonical}")

        return {
            "current_path": canonical,
            "items": sorted(items, key=lambda x: (not x["is_directory"], x["name"].lower()))
        }

    def search_files(self, query: str, start_directory: Optional[str] = None, max_results: int = 50) -> Dict[str, Any]:
        """
        Searches strictly within allowed user directories for filenames matching query.
        NEVER scans root or system folders.
        """
        query_lower = query.lower().strip()
        if not query_lower:
            return {"query": query, "matches": []}

        search_roots = []
        if start_directory:
            is_allowed, canonical, msg = self.enforcer.validate_file_access(start_directory)
            if not is_allowed:
                raise SecurityPolicyException(msg)
            search_roots.append(canonical)
        else:
            search_roots = self.enforcer.get_allowed_roots()

        matches: List[Dict[str, Any]] = []

        for root in search_roots:
            if len(matches) >= max_results:
                break
            for dirpath, dirnames, filenames in os.walk(root, topdown=True):
                # Prune hidden or forbidden directories in-place
                dirnames[:] = [
                    d for d in dirnames
                    if not d.startswith(".") and self.enforcer.validate_file_access(os.path.join(dirpath, d))[0]
                ]

                for fname in filenames:
                    if fname.startswith("."):
                        continue
                    if query_lower in fname.lower():
                        full_path = os.path.join(dirpath, fname)
                        allowed, canon, _ = self.enforcer.validate_file_access(full_path)
                        if allowed:
                            try:
                                stat = os.stat(canon)
                                matches.append({
                                    "filename": fname,
                                    "name": fname,
                                    "is_directory": False,
                                    "isDir": False,
                                    "path": canon,
                                    "size_bytes": stat.st_size,
                                    "modified_at": datetime.datetime.fromtimestamp(stat.st_mtime).isoformat()
                                })
                            except (PermissionError, OSError):
                                continue
                        if len(matches) >= max_results:
                            break
                if len(matches) >= max_results:
                    break

        return {
            "query": query,
            "count": len(matches),
            "matches": matches
        }

    def get_file_metadata(self, target_path: str) -> Dict[str, Any]:
        is_allowed, canonical, msg = self.enforcer.validate_file_access(target_path)
        if not is_allowed:
            raise SecurityPolicyException(msg)

        if not os.path.exists(canonical):
            raise FileNotFoundError(f"File '{target_path}' does not exist.")

        stat = os.stat(canonical)
        return {
            "path": canonical,
            "filename": os.path.basename(canonical),
            "size_bytes": stat.st_size,
            "is_directory": os.path.isdir(canonical),
            "modified_at": datetime.datetime.fromtimestamp(stat.st_mtime).isoformat(),
            "created_at": datetime.datetime.fromtimestamp(stat.st_ctime).isoformat()
        }
