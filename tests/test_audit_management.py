import datetime
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.config import settings
from app.models.entities import AuditLog
from app.database.session import SessionLocal

@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c

@pytest.fixture
def auth_headers(client):
    res = client.post("/api/auth/login", json={
        "username": settings.INITIAL_ADMIN_USERNAME,
        "password": settings.INITIAL_ADMIN_PASSWORD
    })
    token = res.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

def test_audit_logs_clean_and_stats(client, auth_headers):
    db = SessionLocal()
    # Insert test logs: some success, some bugs/errors, across multiple dates
    now = datetime.datetime.now(datetime.timezone.utc)
    log_success = AuditLog(
        timestamp=now,
        actor="web_client",
        action="TEST_ACTION_SUCCESS",
        tool="get_system_status",
        execution_status="SUCCESS",
        details="All good"
    )
    log_bug = AuditLog(
        timestamp=now,
        actor="web_client",
        action="TEST_ACTION_FAIL",
        tool="restart_pc",
        execution_status="FAILED",
        details="Restart failed"
    )
    log_old = AuditLog(
        timestamp=now - datetime.timedelta(days=5),
        actor="web_client",
        action="OLD_ACTION",
        tool="search_files",
        execution_status="REJECTED",
        details="Old rejection"
    )
    # Log exceeding 14 days
    log_ancient = AuditLog(
        timestamp=now - datetime.timedelta(days=20),
        actor="web_client",
        action="ANCIENT_ACTION",
        tool="browser_search",
        execution_status="SUCCESS",
        details="Ancient"
    )
    db.add_all([log_success, log_bug, log_old, log_ancient])
    db.commit()
    db.close()

    # 1. Test Stats endpoint
    stats_res = client.get("/api/audit/stats?days=7", headers=auth_headers)
    assert stats_res.status_code == 200
    stats_data = stats_res.json()
    assert "total_logs" in stats_data
    assert "total_bugs" in stats_data
    assert stats_data["retention_limit_days"] == 14
    assert len(stats_data["days_breakdown"]) >= 7

    # 2. Test Category filter: Bugs only
    bugs_res = client.get("/api/audit?category=bugs", headers=auth_headers)
    assert bugs_res.status_code == 200
    bugs = bugs_res.json()
    for b in bugs:
        assert b["execution_status"] != "SUCCESS" or "REJECTED" in b["action"]

    # 3. Test Category filter: Success only
    success_res = client.get("/api/audit?category=success", headers=auth_headers)
    assert success_res.status_code == 200
    for s in success_res.json():
        assert s["execution_status"] == "SUCCESS"

    # 4. Test Clean logs endpoint (clean older than 2 days)
    clean_partial = client.post("/api/audit/clean", json={"days": 2}, headers=auth_headers)
    assert clean_partial.status_code == 200
    assert clean_partial.json()["retention_days"] == 14

    # 5. Test Clean all logs
    clean_all = client.post("/api/audit/clean", json={"days": 0}, headers=auth_headers)
    assert clean_all.status_code == 200
    assert "Cleaned all" in clean_all.json()["message"]

    # Verify no logs left
    empty_res = client.get("/api/audit", headers=auth_headers)
    assert empty_res.status_code == 200
    assert len(empty_res.json()) == 0
