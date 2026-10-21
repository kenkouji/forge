import os
import uuid
import base64
from typing import Dict, Any, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File, Form, Request, status
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import User, Device
from app.auth.dependencies import get_current_user
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log

router = APIRouter(prefix="/files", tags=["Files"])

MAX_UPLOAD_SIZE_BYTES = 500 * 1024 * 1024  # 500 MB limit
STREAM_CHUNK_SIZE = 256 * 1024             # 256 KB bounded memory chunks

@router.get("/search")
async def search_user_files(
    device_id: str,
    query: str = Query(..., min_length=1),
    directory: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail="Device is offline.")

    request_id = str(uuid.uuid4())
    try:
        args = {"query": query}
        if directory:
            args["directory"] = directory
        res = await ws_manager.send_command_to_agent(
            device_id=device_id,
            request_id=request_id,
            tool="search_files",
            arguments=args,
            timeout_seconds=20.0
        )
        if res and res.get("success") and isinstance(res.get("result"), dict):
            r = res["result"]
            matches = r.get("matches", [])
            for item in matches:
                if not item.get("name") and item.get("filename"):
                    item["name"] = item["filename"]
                is_d = bool(item.get("is_directory", item.get("is_dir", False)))
                item["is_directory"] = is_d
                item["is_dir"] = is_d
                if "size_bytes" not in item:
                    item["size_bytes"] = 0
                if "modified_at" in item:
                    item["modified_at"] = str(item["modified_at"])
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/list")
async def list_user_directory(
    device_id: str,
    path: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail="Device is offline.")

    request_id = str(uuid.uuid4())
    try:
        res = await ws_manager.send_command_to_agent(
            device_id=device_id,
            request_id=request_id,
            tool="list_directory",
            arguments={"path": path} if path else {},
            timeout_seconds=20.0
        )
        if res and res.get("success") and isinstance(res.get("result"), dict):
            r = res["result"]
            curr = r.get("current_path", "")
            r["canonical_path"] = curr
            r["path"] = curr
            for item in r.get("items", []):
                is_d = bool(item.get("is_directory", item.get("is_dir", False)))
                item["is_directory"] = is_d
                item["is_dir"] = is_d
                if "size_bytes" not in item:
                    item["size_bytes"] = 0
                if "modified_at" in item:
                    item["modified_at"] = str(item["modified_at"])
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/metadata")
async def get_user_file_metadata(
    device_id: str,
    path: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail="Device is offline.")

    request_id = str(uuid.uuid4())
    try:
        res = await ws_manager.send_command_to_agent(
            device_id=device_id,
            request_id=request_id,
            tool="get_file_metadata",
            arguments={"path": path},
            timeout_seconds=15.0
        )
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

TEXT_CODE_EXTENSIONS = {
    ".txt", ".json", ".csv", ".tsv", ".log", ".py", ".js", ".ts", ".tsx", ".jsx",
    ".html", ".htm", ".css", ".scss", ".xml", ".yaml", ".yml", ".sh", ".bash",
    ".zsh", ".bat", ".ps1", ".java", ".kt", ".kts", ".rs", ".go", ".c", ".cpp",
    ".cc", ".cxx", ".h", ".hpp", ".rb", ".php", ".sql", ".toml", ".ini", ".env",
    ".properties", ".gradle", ".conf", ".cfg", ".mjs", ".cjs"
}
MARKDOWN_EXTENSIONS = {".md", ".markdown"}
IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".bmp", ".ico"}
AUDIO_EXTENSIONS = {".mp3", ".wav", ".m4a", ".ogg", ".aac", ".flac"}
VIDEO_EXTENSIONS = {".mp4", ".mkv", ".webm", ".mov", ".3gp"}

text_exts = TEXT_CODE_EXTENSIONS
markdown_exts = MARKDOWN_EXTENSIONS
image_exts = IMAGE_EXTENSIONS
audio_exts = AUDIO_EXTENSIONS
video_exts = VIDEO_EXTENSIONS

@router.get("/preview")
async def preview_user_file(
    device_id: str,
    path: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Safely previews supported text and image files (up to 512 KB bounded).
    """
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail="Device is offline.")

    ext = os.path.splitext(path)[1].lower()
    is_supported = (
        ext in TEXT_CODE_EXTENSIONS or ext in MARKDOWN_EXTENSIONS or ext in IMAGE_EXTENSIONS or
        ext in AUDIO_EXTENSIONS or ext in VIDEO_EXTENSIONS or ext == ".pdf"
    )

    if not is_supported:
        return {
            "preview_supported": False,
            "filename": os.path.basename(path),
            "file_size": 0,
            "message": "This file cannot be previewed in-app. Use Download to save it locally."
        }

    # For binary media (images, audio, video, PDFs), allow up to 10 MB bounded chunk
    is_media = ext in image_exts or ext in audio_exts or ext in video_exts or ext == ".pdf"
    preview_length = 10 * 1024 * 1024 if is_media else (512 * 1024)

    preview_req_id = str(uuid.uuid4())
    res = await ws_manager.send_command_to_agent(
        device_id=device_id,
        request_id=preview_req_id,
        tool="download_file_chunk",
        arguments={"path": path, "offset": 0, "length": preview_length},
        timeout_seconds=25.0
    )

    if not res.get("success"):
        err = res.get("error", "Access denied")
        if "folder" in err.lower() or "directory" in err.lower():
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=err)

    chunk_info = res.get("result", {})
    raw_b64 = chunk_info.get("data_base64", "")
    file_size = chunk_info.get("file_size", 0)
    filename = chunk_info.get("filename", os.path.basename(path))

    if ext in markdown_exts:
        try:
            raw_bytes = base64.b64decode(raw_b64.encode("utf-8"))
            text_content = raw_bytes.decode("utf-8", errors="replace")
            return {
                "preview_supported": True,
                "preview_type": "markdown",
                "filename": filename,
                "file_size": file_size,
                "content": text_content,
                "is_truncated": file_size > 512 * 1024
            }
        except Exception:
            pass

    if ext in text_exts:
        try:
            raw_bytes = base64.b64decode(raw_b64.encode("utf-8"))
            text_content = raw_bytes.decode("utf-8", errors="replace")
            return {
                "preview_supported": True,
                "preview_type": "text",
                "filename": filename,
                "file_size": file_size,
                "content": text_content,
                "is_truncated": file_size > 512 * 1024
            }
        except Exception:
            pass

    if ext in image_exts:
        mime = f"image/{ext.replace('.', '')}"
        if ext == ".jpg":
            mime = "image/jpeg"
        elif ext == ".svg":
            mime = "image/svg+xml"
        return {
            "preview_supported": True,
            "preview_type": "image",
            "filename": filename,
            "file_size": file_size,
            "mime_type": mime,
            "image_data_base64": raw_b64
        }

    if ext in audio_exts or ext in video_exts:
        ptype = "audio" if ext in audio_exts else "video"
        mime = f"audio/{ext.replace('.', '')}" if ext in audio_exts else f"video/{ext.replace('.', '')}"
        if ext == ".mp3":
            mime = "audio/mpeg"
        elif ext == ".mov":
            mime = "video/quicktime"
        return {
            "preview_supported": True,
            "preview_type": ptype,
            "filename": filename,
            "file_size": file_size,
            "mime_type": mime,
            "media_data_base64": raw_b64
        }

    if ext == ".pdf":
        return {
            "preview_supported": True,
            "preview_type": "pdf",
            "filename": filename,
            "file_size": file_size,
            "pdf_data_base64": raw_b64
        }

    return {
        "preview_supported": False,
        "filename": filename,
        "file_size": file_size,
        "message": "This file cannot be previewed in-app. Use Download to save it locally."
    }

@router.get("/hash")
async def get_file_hash_endpoint(
    device_id: str,
    path: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Computes SHA-256 checksum of an authorized user file.
    """
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail="Device is offline.")

    req_id = str(uuid.uuid4())
    try:
        res = await ws_manager.send_command_to_agent(
            device_id=device_id,
            request_id=req_id,
            tool="get_file_hash",
            arguments={"path": path},
            timeout_seconds=30.0
        )
        if not res.get("success"):
            err = res.get("error", "Failed to calculate file hash.")
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)
        return res.get("result", {})
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/download")
async def download_file_stream(
    request: Request,
    device_id: str,
    path: str = Query(...),
    as_zip: bool = Query(False),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Streams a file from the remote PC agent to the client in bounded memory chunks.
    Supports HTTP Range requests (206 Partial Content) for resumable transfers and directory zip archiving.
    """
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail="Device is offline.")

    # Check if directory archiving is requested
    target_path = path
    if as_zip:
        zip_req_id = str(uuid.uuid4())
        try:
            zip_res = await ws_manager.send_command_to_agent(
                device_id=device_id,
                request_id=zip_req_id,
                tool="zip_directory",
                arguments={"path": path},
                timeout_seconds=60.0
            )
            if zip_res.get("success"):
                target_path = zip_res.get("result", {}).get("zip_path", path)
        except Exception:
            pass

    # 1. Parse Range Header if present
    range_header = request.headers.get("Range")
    start_offset = 0
    end_offset: Optional[int] = None
    if range_header and range_header.startswith("bytes="):
        try:
            parts = range_header[6:].split("-")
            start_offset = int(parts[0])
            if len(parts) > 1 and parts[1].strip():
                end_offset = int(parts[1].strip())
        except Exception:
            start_offset = 0

    # 2. Fetch first chunk from requested offset
    init_request_id = str(uuid.uuid4())
    try:
        init_res = await ws_manager.send_command_to_agent(
            device_id=device_id,
            request_id=init_request_id,
            tool="download_file_chunk",
            arguments={"path": target_path, "offset": start_offset, "length": STREAM_CHUNK_SIZE},
            timeout_seconds=25.0
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to initiate file transfer: {str(e)}")

    if not init_res.get("success"):
        err = init_res.get("error", "Access denied or file not found")
        # If target is a folder and as_zip wasn't initially requested, automatically zip it
        if ("is a folder" in err.lower() or "is a directory" in err.lower()) and not as_zip:
            try:
                zip_res = await ws_manager.send_command_to_agent(
                    device_id=device_id,
                    request_id=str(uuid.uuid4()),
                    tool="zip_directory",
                    arguments={"path": path},
                    timeout_seconds=60.0
                )
                if zip_res.get("success"):
                    target_path = zip_res.get("result", {}).get("zip_path", path)
                    init_res = await ws_manager.send_command_to_agent(
                        device_id=device_id,
                        request_id=str(uuid.uuid4()),
                        tool="download_file_chunk",
                        arguments={"path": target_path, "offset": start_offset, "length": STREAM_CHUNK_SIZE},
                        timeout_seconds=25.0
                    )
            except Exception:
                pass

        if not init_res.get("success"):
            err = init_res.get("error", "Access denied or file not found")
            if "access denied" in err.lower() or "outside allowed" in err.lower() or "restricted system" in err.lower():
                record_audit_log(
                    db=db,
                    actor=current_user.username,
                    device_id=device_id,
                    action="FILE_DOWNLOAD_REJECTED",
                    tool="download_file",
                    arguments={"path": path},
                    permission_level="MEDIUM",
                    execution_status="REJECTED",
                    details=err
                )
                raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=err)
            if "is a folder" in err.lower() or "is a directory" in err.lower():
                raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=err)
            if "not found" in err.lower() or "not a regular file" in err.lower():
                raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=err)
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=err)

    chunk_info = init_res.get("result", {})
    file_size = chunk_info.get("file_size", 0)
    filename = chunk_info.get("filename", os.path.basename(target_path))

    # Log transfer start in audit
    record_audit_log(
        db=db,
        actor=current_user.username,
        device_id=device_id,
        action="FILE_DOWNLOAD_START",
        tool="download_file",
        arguments={"path": path, "size": file_size, "offset": start_offset},
        permission_level="MEDIUM",
        execution_status="SUCCESS",
        details=f"Streaming download initiated for '{filename}' ({file_size} bytes, offset {start_offset})"
    )

    is_partial = (start_offset > 0 or end_offset is not None)
    actual_end = (file_size - 1) if (end_offset is None or end_offset >= file_size) else end_offset
    total_to_send = max(0, actual_end - start_offset + 1) if file_size > 0 else 0

    async def file_chunk_generator():
        offset = start_offset
        current_chunk = chunk_info
        sent_bytes = 0

        while True:
            raw_b64 = current_chunk.get("data_base64", "")
            if raw_b64:
                raw_bytes = base64.b64decode(raw_b64.encode("utf-8"))
                if end_offset is not None and (sent_bytes + len(raw_bytes)) > total_to_send:
                    slice_len = total_to_send - sent_bytes
                    yield raw_bytes[:slice_len]
                    break
                else:
                    yield raw_bytes
                    sent_bytes += len(raw_bytes)

            if current_chunk.get("is_eof", True) or (end_offset is not None and sent_bytes >= total_to_send):
                break

            bytes_read = current_chunk.get("bytes_read", 0)
            offset += bytes_read

            req_id = str(uuid.uuid4())
            next_res = await ws_manager.send_command_to_agent(
                device_id=device_id,
                request_id=req_id,
                tool="download_file_chunk",
                arguments={"path": target_path, "offset": offset, "length": STREAM_CHUNK_SIZE},
                timeout_seconds=25.0
            )

            if not next_res.get("success"):
                break

            current_chunk = next_res.get("result", {})

    status_code = status.HTTP_206_PARTIAL_CONTENT if is_partial else status.HTTP_200_OK
    headers = {
        "Content-Disposition": f'attachment; filename="{filename}"',
        "Accept-Ranges": "bytes",
        "X-RemoteForge-Transfer-Type": "stream-bounded"
    }

    if is_partial and file_size > 0:
        headers["Content-Range"] = f"bytes {start_offset}-{actual_end}/{file_size}"
        headers["Content-Length"] = str(total_to_send)
    else:
        headers["Content-Length"] = str(file_size)

    return StreamingResponse(
        file_chunk_generator(),
        status_code=status_code,
        media_type="application/octet-stream",
        headers=headers
    )

@router.post("/upload")
async def upload_file_stream(
    device_id: str = Form(...),
    target_directory: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Streams an uploaded file chunk-by-chunk to the remote PC agent.
    Never buffers the entire file in server memory.
    """
    if not ws_manager.is_agent_online(device_id):
        raise HTTPException(status_code=503, detail="Device is offline.")

    transfer_id = str(uuid.uuid4())
    filename = file.filename or "uploaded_file"
    safe_filename = os.path.basename(filename)

    total_bytes = 0
    offset = 0
    is_first = True

    try:
        while True:
            chunk = await file.read(STREAM_CHUNK_SIZE)
            chunk_len = len(chunk)
            total_bytes += chunk_len

            if total_bytes > MAX_UPLOAD_SIZE_BYTES:
                # Cancel upload on agent
                await ws_manager.send_command_to_agent(
                    device_id=device_id,
                    request_id=str(uuid.uuid4()),
                    tool="cancel_file_upload",
                    arguments={"transfer_id": transfer_id}
                )
                raise HTTPException(
                    status_code=413,
                    detail=f"File exceeds maximum allowed limit of {MAX_UPLOAD_SIZE_BYTES // (1024*1024)} MB."
                )

            # Peek to check if EOF
            is_last = chunk_len < STREAM_CHUNK_SIZE
            chunk_b64 = base64.b64encode(chunk).decode("utf-8")

            req_id = str(uuid.uuid4())
            agent_res = await ws_manager.send_command_to_agent(
                device_id=device_id,
                request_id=req_id,
                tool="upload_file_chunk",
                arguments={
                    "target_directory": target_directory,
                    "filename": safe_filename,
                    "transfer_id": transfer_id,
                    "chunk_data_base64": chunk_b64,
                    "offset": offset,
                    "is_first": is_first,
                    "is_last": is_last
                },
                timeout_seconds=30.0
            )

            if not agent_res.get("success"):
                err = agent_res.get("error", "Upload failed on PC agent.")
                raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=err)

            is_first = False
            offset += chunk_len

            if is_last:
                break

        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=device_id,
            action="FILE_UPLOAD_COMPLETE",
            tool="upload_file",
            arguments={"target_directory": target_directory, "filename": safe_filename, "size": total_bytes},
            permission_level="MEDIUM",
            execution_status="SUCCESS",
            details=f"Uploaded '{safe_filename}' ({total_bytes} bytes) to '{target_directory}'"
        )

        sha256_hash = agent_res.get("result", {}).get("sha256", "") if agent_res else ""

        # Post host notification and broadcast to dashboard
        try:
            loc_label = "Desktop" if "desktop" in target_directory.lower() else target_directory
            if ws_manager.is_agent_online(device_id):
                await ws_manager.send_command_to_agent(
                    device_id=device_id,
                    request_id=str(uuid.uuid4()),
                    tool="show_notification",
                    arguments={
                        "title": "RemoteForge File Received",
                        "message": f"File '{safe_filename}' received from {current_user.username} on {loc_label}"
                    }
                )
            await ws_manager.broadcast_to_clients({
                "type": "file_transferred",
                "device_id": device_id,
                "filename": safe_filename,
                "bytes_transferred": total_bytes,
                "target_directory": target_directory,
                "location": loc_label
            })
        except Exception:
            pass

        return {
            "success": True,
            "filename": safe_filename,
            "bytes_transferred": total_bytes,
            "target_directory": target_directory,
            "sha256": sha256_hash
        }

    except Exception as e:
        try:
            await ws_manager.send_command_to_agent(
                device_id=device_id,
                request_id=str(uuid.uuid4()),
                tool="cancel_file_upload",
                arguments={"transfer_id": transfer_id}
            )
        except Exception:
            pass
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(status_code=500, detail=f"File upload error: {str(e)}")
