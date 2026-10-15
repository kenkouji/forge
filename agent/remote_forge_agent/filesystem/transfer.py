import os
import base64
import hashlib
import zipfile
import uuid
from typing import Dict, Any, Optional
from agent.remote_forge_agent.security.allowlist import FilesystemSecurityEnforcer, SecurityPolicyException

DEFAULT_CHUNK_SIZE = 256 * 1024  # 256 KB bounded memory chunks

class ChunkedTransferHandler:
    def __init__(self, enforcer: FilesystemSecurityEnforcer):
        self.enforcer = enforcer
        # transfer_id -> { "temp_path": str, "target_path": str, "bytes_written": int }
        self._active_uploads: Dict[str, Dict[str, Any]] = {}

    def get_file_hash(self, target_path: str) -> Dict[str, Any]:
        """
        Calculates SHA-256 checksum of an allowed file without loading it all in memory.
        """
        is_allowed, canonical, msg = self.enforcer.validate_file_access(target_path)
        if not is_allowed:
            raise SecurityPolicyException(msg)

        if not os.path.exists(canonical) or not os.path.isfile(canonical):
            raise FileNotFoundError(f"File not found: '{target_path}'")

        h = hashlib.sha256()
        with open(canonical, "rb") as f:
            while chunk := f.read(65536):
                h.update(chunk)

        return {
            "path": canonical,
            "filename": os.path.basename(canonical),
            "sha256": h.hexdigest(),
            "file_size": os.path.getsize(canonical)
        }

    def zip_directory(self, target_directory: str) -> Dict[str, Any]:
        """
        Archives an allowed directory into a .zip container inside the directory or allowed root.
        """
        is_allowed, canonical_dir, msg = self.enforcer.validate_file_access(target_directory)
        if not is_allowed:
            raise SecurityPolicyException(msg)

        if not os.path.isdir(canonical_dir):
            raise NotADirectoryError(f"Target is not a directory: {target_directory}")

        base_name = os.path.basename(canonical_dir.rstrip("/"))
        zip_filename = f".rf_export_{uuid.uuid4().hex[:8]}_{base_name}.zip"
        zip_path = os.path.join(canonical_dir, zip_filename)

        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
            for root, dirs, files in os.walk(canonical_dir):
                # Skip hidden git or system directories
                dirs[:] = [d for d in dirs if not d.startswith(".")]
                for file in files:
                    if file.startswith(".rf_"):
                        continue
                    full_p = os.path.join(root, file)
                    rel_p = os.path.relpath(full_p, canonical_dir)
                    zf.write(full_p, rel_p)

        file_size = os.path.getsize(zip_path)
        return {
            "zip_path": zip_path,
            "filename": f"{base_name}.zip",
            "file_size": file_size
        }

    def read_chunk(self, target_path: str, offset: int = 0, length: int = DEFAULT_CHUNK_SIZE) -> Dict[str, Any]:
        """
        Reads a bounded chunk from an allowed user-data file.
        Throws SecurityPolicyException if path is restricted or outside allowlist.
        """
        is_allowed, canonical, msg = self.enforcer.validate_file_access(target_path)
        if not is_allowed:
            raise SecurityPolicyException(msg)

        if not os.path.exists(canonical):
            raise FileNotFoundError(f"File not found on target computer: '{target_path}'")

        if os.path.isdir(canonical):
            raise IsADirectoryError(f"Target '{os.path.basename(canonical)}' is a folder, not a downloadable file. Please select a file to download.")

        if not os.path.isfile(canonical):
            raise FileNotFoundError(f"Target is not a regular file: {canonical}")

        file_size = os.path.getsize(canonical)
        if offset >= file_size and file_size > 0:
            return {
                "file_size": file_size,
                "bytes_read": 0,
                "is_eof": True,
                "data_base64": ""
            }

        with open(canonical, "rb") as f:
            f.seek(offset)
            data = f.read(length)

        bytes_read = len(data)
        is_eof = (offset + bytes_read) >= file_size

        return {
            "file_size": file_size,
            "bytes_read": bytes_read,
            "offset": offset,
            "is_eof": is_eof,
            "filename": os.path.basename(canonical),
            "data_base64": base64.b64encode(data).decode("utf-8")
        }

    def write_chunk(
        self,
        target_directory: str,
        filename: str,
        transfer_id: str,
        chunk_data_base64: str,
        offset: int,
        is_first: bool,
        is_last: bool
    ) -> Dict[str, Any]:
        """
        Writes a bounded chunk to a target allowed user-data directory.
        Uses an atomic temporary file until is_last is received.
        """
        # 1. Sanitize filename to avoid path traversal in upload
        clean_name = os.path.basename(filename.strip())
        if not clean_name or clean_name in [".", ".."]:
            raise ValueError(f"Invalid filename for upload: {filename}")

        # 2. Validate target directory containment
        is_allowed, canonical_dir, msg = self.enforcer.validate_file_access(target_directory)
        if not is_allowed:
            raise SecurityPolicyException(msg)

        if not os.path.isdir(canonical_dir):
            raise NotADirectoryError(f"Target is not a directory: {canonical_dir}")

        destination_path = os.path.join(canonical_dir, clean_name)
        # Double check destination file path against allowlist and forbidden secret extensions
        dest_allowed, dest_canonical, dest_msg = self.enforcer.validate_file_access(destination_path)
        if not dest_allowed:
            raise SecurityPolicyException(dest_msg)

        # 3. Setup temporary file for atomic write
        safe_hash = hashlib.sha256(transfer_id.encode("utf-8")).hexdigest()[:12]
        temp_filename = f".rf_upload_{safe_hash}.tmp"
        temp_path = os.path.join(canonical_dir, temp_filename)

        if is_first:
            # Clean up any leftover temp file
            if os.path.exists(temp_path):
                try:
                    os.remove(temp_path)
                except OSError:
                    pass
            self._active_uploads[transfer_id] = {
                "temp_path": temp_path,
                "target_path": dest_canonical,
                "bytes_written": 0
            }

        # Decode chunk
        chunk_bytes = base64.b64decode(chunk_data_base64.encode("utf-8"))

        mode = "wb" if is_first else "r+b"
        if not os.path.exists(temp_path) and not is_first:
            mode = "wb"

        with open(temp_path, mode) as f:
            f.seek(offset)
            f.write(chunk_bytes)

        bytes_written = len(chunk_bytes)
        if transfer_id in self._active_uploads:
            self._active_uploads[transfer_id]["bytes_written"] += bytes_written

        # 4. If this is the final chunk, atomically replace destination
        if is_last:
            if os.path.exists(dest_canonical):
                try:
                    os.remove(dest_canonical)
                except OSError:
                    pass
            os.replace(temp_path, dest_canonical)
            self._active_uploads.pop(transfer_id, None)

            final_size = os.path.getsize(dest_canonical)

            # Calculate final SHA-256 checksum
            h = hashlib.sha256()
            with open(dest_canonical, "rb") as f:
                while c := f.read(65536):
                    h.update(c)
            sha256_hash = h.hexdigest()

            return {
                "success": True,
                "is_complete": True,
                "destination_path": dest_canonical,
                "filename": clean_name,
                "total_bytes": final_size,
                "sha256": sha256_hash
            }

        return {
            "success": True,
            "is_complete": False,
            "bytes_written": bytes_written,
            "transfer_id": transfer_id
        }

    def cancel_upload(self, transfer_id: str):
        record = self._active_uploads.pop(transfer_id, None)
        if record and os.path.exists(record["temp_path"]):
            try:
                os.remove(record["temp_path"])
            except OSError:
                pass
