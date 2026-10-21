import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.database.session import SessionLocal
from app.models.entities import User
from app.auth.security import get_password_hash, create_access_token
from agent.remote_forge_agent.system.screen_streamer import screen_streamer

client = TestClient(app)

@pytest.fixture(scope="module")
def auth_headers():
    db = SessionLocal()
    try:
        user = db.query(User).filter(User.username == "test_stream_admin").first()
        if not user:
            user = User(
                username="test_stream_admin",
                hashed_password=get_password_hash("testpass123"),
                role="admin",
                is_active=True
            )
            db.add(user)
            db.commit()
        token = create_access_token(data={"sub": user.username, "role": user.role})
        return {"Authorization": f"Bearer {token}"}
    finally:
        db.close()

def test_screen_streamer_service_unit():
    """Verifies that the CoreGraphics / ImageGrab screen streamer captures frames quickly."""
    screen_streamer.configure(target_width=720, quality=45, fps=10)
    status = screen_streamer.get_status()
    assert status["target_width"] == 720
    assert status["target_quality"] == 45
    assert status["target_fps"] == 10

    jpeg_bytes, w, h, dur = screen_streamer.capture_frame(target_width=720, quality=45)
    assert jpeg_bytes is not None
    assert len(jpeg_bytes) > 1000
    assert w <= 720
    assert dur > 0.0

def test_stream_status_unauthenticated():
    """Unauthenticated requests to streaming endpoints must be rejected."""
    res = client.get("/api/stream/status/test_device")
    assert res.status_code == 401

def test_stream_status_authenticated(auth_headers):
    """Authenticated requests to stream status succeed with valid metrics."""
    res = client.get("/api/stream/status/test_device", headers=auth_headers)
    assert res.status_code == 200
    data = res.json()
    assert "is_active" in data
    assert "width" in data
    assert "height" in data
    assert "fps" in data
    assert "latency_ms" in data
