import pytest
import base64
from fastapi.testclient import TestClient

from app.main import app
from app.services.clipboard_service import clipboard_service

client = TestClient(app)

def test_clipboard_add_and_get_history():
    clipboard_service.clear_history()

    # 1. Add text clipboard item
    item1 = clipboard_service.add_item(
        content_type="text",
        text="Hello World from Mac",
        source_device="macbook-pro"
    )
    assert item1["content_type"] == "text"
    assert item1["text_content"] == "Hello World from Mac"
    assert item1["source_device"] == "macbook-pro"

    # 2. Add image clipboard item
    fake_png = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15c4"
    b64_img = base64.b64encode(fake_png).decode("utf-8")
    item2 = clipboard_service.add_item(
        content_type="image",
        image_base64=b64_img,
        source_device="phone"
    )
    assert item2["content_type"] == "image"
    assert item2["image_base64"] == b64_img
    assert item2["source_device"] == "phone"

    # 3. Retrieve history
    history = clipboard_service.get_history()
    assert len(history) == 2
    assert history[0]["id"] == item2["id"]  # Most recent first
    assert history[1]["id"] == item1["id"]

    # 4. Filter by type
    text_only = clipboard_service.get_history(content_type="text")
    assert len(text_only) == 1
    assert text_only[0]["content_type"] == "text"

    img_only = clipboard_service.get_history(content_type="image")
    assert len(img_only) == 1
    assert img_only[0]["content_type"] == "image"

def test_clipboard_deduplication():
    clipboard_service.clear_history()

    # Consecutive identical texts should not create multiple entries
    it1 = clipboard_service.add_item("text", text="Duplicate Test")
    it2 = clipboard_service.add_item("text", text="Duplicate Test")

    assert it1["id"] == it2["id"]
    assert len(clipboard_service.get_history()) == 1

def test_clipboard_delete_and_clear():
    clipboard_service.clear_history()

    it1 = clipboard_service.add_item("text", text="Item 1")
    it2 = clipboard_service.add_item("text", text="Item 2")

    ok = clipboard_service.delete_item(it1["id"])
    assert ok is True
    assert len(clipboard_service.get_history()) == 1

    cleared = clipboard_service.clear_history()
    assert cleared == 1
    assert len(clipboard_service.get_history()) == 0
