import uuid
import time
import datetime
import logging
from typing import List, Dict, Any, Optional

logger = logging.getLogger("remoteforge.clipboard")

class ClipboardItem:
    def __init__(
        self,
        id: str,
        content_type: str,
        text_content: Optional[str],
        image_base64: Optional[str],
        preview: str,
        source_device: str,
        size_bytes: int,
        created_at: str
    ):
        self.id = id
        self.content_type = content_type
        self.text_content = text_content
        self.image_base64 = image_base64
        self.preview = preview
        self.source_device = source_device
        self.size_bytes = size_bytes
        self.created_at = created_at

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "content_type": self.content_type,
            "text_content": self.text_content,
            "image_base64": self.image_base64,
            "preview": self.preview,
            "source_device": self.source_device,
            "size_bytes": self.size_bytes,
            "created_at": self.created_at
        }


class UniversalClipboardService:
    def __init__(self, max_items: int = 50):
        self.max_items = max_items
        self._history: List[ClipboardItem] = []
        self._last_content_hash: Optional[str] = None

    def add_item(
        self,
        content_type: str,
        text: Optional[str] = None,
        image_base64: Optional[str] = None,
        source_device: str = "laptop",
        preview: Optional[str] = None
    ) -> Dict[str, Any]:
        """Adds a newly copied clipboard item and deduplicates identical consecutive copies."""
        content_type = content_type.lower()
        if content_type == "text" and not text:
            raise ValueError("Text content cannot be empty.")
        if content_type == "image" and not image_base64:
            raise ValueError("Image base64 payload cannot be empty.")

        # Compute simple deduplication key
        import hashlib
        dedup_seed = (text or "") if content_type == "text" else (image_base64[:200] if image_base64 else "")
        content_hash = hashlib.sha256(dedup_seed.encode("utf-8")).hexdigest()

        if self._history and self._last_content_hash == content_hash:
            # Update source and timestamp of existing top item
            top = self._history[0]
            top.source_device = source_device
            top.created_at = datetime.datetime.now(datetime.timezone.utc).isoformat()
            return top.to_dict()

        item_id = str(uuid.uuid4())
        now_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()

        if content_type == "text":
            size_b = len((text or "").encode("utf-8"))
            item_preview = preview or (text[:120].replace("\n", " ").strip() if text else "Text item")
            item = ClipboardItem(
                id=item_id,
                content_type="text",
                text_content=text,
                image_base64=None,
                preview=item_preview,
                source_device=source_device,
                size_bytes=size_b,
                created_at=now_iso
            )
        else:
            raw_len = int(len(image_base64 or "") * 0.75)
            item_preview = preview or f"Image ({round(raw_len / 1024.0, 1)} KB)"
            item = ClipboardItem(
                id=item_id,
                content_type="image",
                text_content=None,
                image_base64=image_base64,
                preview=item_preview,
                source_device=source_device,
                size_bytes=raw_len,
                created_at=now_iso
            )

        self._history.insert(0, item)
        self._last_content_hash = content_hash

        # Maintain max capacity
        if len(self._history) > self.max_items:
            self._history = self._history[:self.max_items]

        return item.to_dict()

    def get_history(self, limit: int = 50, content_type: Optional[str] = None) -> List[Dict[str, Any]]:
        items = self._history
        if content_type:
            items = [it for it in items if it.content_type == content_type]
        return [it.to_dict() for it in items[:limit]]

    def delete_item(self, item_id: str) -> bool:
        initial_len = len(self._history)
        self._history = [it for it in self._history if it.id != item_id]
        return len(self._history) < initial_len

    def clear_history(self) -> int:
        count = len(self._history)
        self._history.clear()
        self._last_content_hash = None
        return count


clipboard_service = UniversalClipboardService()
