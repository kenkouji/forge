import datetime
import json
from typing import Optional, Dict, Any
from sqlalchemy.orm import Session
from app.models.entities import AuditLog

SENSITIVE_KEYS = {"password", "token", "secret", "authorization", "access_token", "jwt", "key", "credential"}

def sanitize_arguments(arguments: Optional[Dict[str, Any]]) -> str:
    if not arguments:
        return "{}"
    sanitized = {}
    for k, v in arguments.items():
        if any(sens in k.lower() for sens in SENSITIVE_KEYS):
            sanitized[k] = "[REDACTED]"
        else:
            sanitized[k] = v
    try:
        return json.dumps(sanitized)
    except Exception:
        return "{}"

MAX_AUDIT_RETENTION_DAYS = 14

def prune_expired_logs(db: Session, retention_days: int = MAX_AUDIT_RETENTION_DAYS) -> int:
    """
    Enforces the strict retention ceiling (max 14 days).
    Deletes any logs older than retention_days.
    """
    effective_days = min(max(1, retention_days), MAX_AUDIT_RETENTION_DAYS)
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=effective_days)
    deleted = db.query(AuditLog).filter(AuditLog.timestamp < cutoff).delete(synchronize_session=False)
    if deleted > 0:
        db.commit()
    return deleted

def clean_audit_logs(db: Session, max_age_days: Optional[int] = None) -> int:
    """
    Cleans audit logs.
    If max_age_days is None or 0: deletes all audit records.
    If max_age_days is specified (>0): deletes records older than max_age_days, capped at 14 days.
    """
    if max_age_days is None or max_age_days <= 0:
        deleted = db.query(AuditLog).delete(synchronize_session=False)
    else:
        effective_days = min(max_age_days, MAX_AUDIT_RETENTION_DAYS)
        cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=effective_days)
        deleted = db.query(AuditLog).filter(AuditLog.timestamp < cutoff).delete(synchronize_session=False)
    db.commit()
    return deleted

def record_audit_log(
    db: Session,
    action: str,
    actor: str = "web_client",
    device_id: Optional[str] = None,
    task_id: Optional[str] = None,
    tool: Optional[str] = None,
    arguments: Optional[Dict[str, Any]] = None,
    permission_level: str = "LOW",
    confirmation_status: str = "NOT_REQUIRED",
    execution_status: str = "SUCCESS",
    details: Optional[str] = None
) -> Optional[AuditLog]:
    # Silent execution: Never write get_system_status polling to audit logs
    if tool == "get_system_status" or (action and "get_system_status" in action.lower()):
        return None

    # Always enforce strict 14-day retention ceiling
    try:
        prune_expired_logs(db, MAX_AUDIT_RETENTION_DAYS)
    except Exception:
        pass

    safe_args = sanitize_arguments(arguments)
    log_entry = AuditLog(
        timestamp=datetime.datetime.now(datetime.timezone.utc),
        actor=actor,
        device_id=device_id,
        task_id=task_id,
        action=action,
        tool=tool,
        arguments_safe=safe_args,
        permission_level=permission_level,
        confirmation_status=confirmation_status,
        execution_status=execution_status,
        details=details
    )
    db.add(log_entry)
    db.commit()
    db.refresh(log_entry)
    return log_entry
