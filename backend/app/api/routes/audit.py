import datetime
from collections import defaultdict
from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import AuditLog, User
from app.schemas.all_schemas import (
    AuditLogResponse,
    AuditCleanRequest,
    AuditCleanResponse,
    AuditStatsResponse,
    AuditDayStat
)
from app.auth.dependencies import get_current_user
from app.audit.logger import clean_audit_logs, prune_expired_logs, MAX_AUDIT_RETENTION_DAYS

router = APIRouter(prefix="/audit", tags=["Audit"])

def parse_timeframe_days(timeframe: Optional[str], days: Optional[int]) -> int:
    """
    Parses timeframe string or days into an integer.
    Strictly caps at MAX_AUDIT_RETENTION_DAYS (14 days).
    """
    if days is not None:
        return min(max(1, days), MAX_AUDIT_RETENTION_DAYS)
    
    if not timeframe:
        return MAX_AUDIT_RETENTION_DAYS
    
    tf = timeframe.strip().lower()
    if tf in ("1d", "today", "day", "per_day"):
        return 1
    elif tf in ("3d", "3days"):
        return 3
    elif tf in ("7d", "1w", "week", "per_week"):
        return 7
    elif tf in ("14d", "2w", "2weeks", "month", "per_month"):
        # Explicit user requirement: "if month then month but it should not go over a 2 weeks as we dont need logs over 2 weeks tbh"
        return MAX_AUDIT_RETENTION_DAYS
    try:
        val = int(tf.replace("d", ""))
        return min(max(1, val), MAX_AUDIT_RETENTION_DAYS)
    except ValueError:
        return MAX_AUDIT_RETENTION_DAYS

@router.get("", response_model=List[AuditLogResponse])
@router.get("/", response_model=List[AuditLogResponse])
def get_audit_logs(
    device_id: Optional[str] = None,
    category: Optional[str] = Query("all", description="all, bugs, issues, success"),
    timeframe: Optional[str] = Query(None, description="today, 1d, 3d, 7d, 1w, 14d, 2w"),
    days: Optional[int] = Query(None, description="Number of days (max 14)"),
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    # Auto-prune any logs older than 14 days
    try:
        prune_expired_logs(db, MAX_AUDIT_RETENTION_DAYS)
    except Exception:
        pass

    effective_days = parse_timeframe_days(timeframe, days)
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=effective_days)

    query = db.query(AuditLog).filter(
        AuditLog.timestamp >= cutoff,
        AuditLog.tool != "get_system_status",
        ~AuditLog.action.contains("get_system_status")
    )

    if device_id:
        query = query.filter(AuditLog.device_id == device_id)

    cat = (category or "all").strip().lower()
    if cat in ("bugs", "issues", "error", "failed"):
        # Bugs / Issues: any non-SUCCESS status or rejected/error actions
        query = query.filter(
            or_(
                AuditLog.execution_status.in_(["FAILED", "ERROR", "REJECTED", "SECURITY_ALERT", "EXCEPTION"]),
                AuditLog.execution_status != "SUCCESS",
                AuditLog.action.contains("REJECTED"),
                AuditLog.action.contains("ERROR")
            )
        )
    elif cat == "success":
        query = query.filter(AuditLog.execution_status == "SUCCESS")

    logs = query.order_by(AuditLog.timestamp.desc()).limit(limit).all()
    return logs

@router.get("/stats", response_model=AuditStatsResponse)
def get_audit_stats(
    device_id: Optional[str] = None,
    timeframe: Optional[str] = Query(None),
    days: Optional[int] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    try:
        prune_expired_logs(db, MAX_AUDIT_RETENTION_DAYS)
    except Exception:
        pass

    effective_days = parse_timeframe_days(timeframe, days)
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=effective_days)

    query = db.query(AuditLog).filter(
        AuditLog.timestamp >= cutoff,
        AuditLog.tool != "get_system_status",
        ~AuditLog.action.contains("get_system_status")
    )
    if device_id:
        query = query.filter(AuditLog.device_id == device_id)

    all_logs = query.all()

    total_logs = len(all_logs)
    total_bugs = 0
    total_success = 0

    # Group by date YYYY-MM-DD
    day_groups = defaultdict(lambda: {"total": 0, "bugs": 0, "success": 0})

    # Initialize all dates in range so empty days have 0 counts
    today = datetime.datetime.now(datetime.timezone.utc).date()
    for i in range(effective_days):
        d_str = (today - datetime.timedelta(days=i)).isoformat()
        day_groups[d_str] = {"total": 0, "bugs": 0, "success": 0}

    for log in all_logs:
        date_str = log.timestamp.strftime("%Y-%m-%d")
        day_groups[date_str]["total"] += 1
        is_bug = (
            log.execution_status in ("FAILED", "ERROR", "REJECTED", "SECURITY_ALERT", "EXCEPTION")
            or log.execution_status != "SUCCESS"
            or "REJECTED" in (log.action or "")
            or "ERROR" in (log.action or "")
        )
        if is_bug:
            total_bugs += 1
            day_groups[date_str]["bugs"] += 1
        else:
            total_success += 1
            day_groups[date_str]["success"] += 1

    # Sort descending by date
    breakdown = [
        AuditDayStat(
            date=d,
            total=data["total"],
            bugs=data["bugs"],
            success=data["success"]
        )
        for d, data in sorted(day_groups.items(), key=lambda x: x[0], reverse=True)
    ]

    return AuditStatsResponse(
        total_logs=total_logs,
        total_bugs=total_bugs,
        total_success=total_success,
        retention_limit_days=MAX_AUDIT_RETENTION_DAYS,
        days_breakdown=breakdown
    )

@router.post("/clean", response_model=AuditCleanResponse)
@router.delete("", response_model=AuditCleanResponse)
@router.delete("/", response_model=AuditCleanResponse)
def clean_logs_endpoint(
    request: Optional[AuditCleanRequest] = None,
    days: Optional[int] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    target_days = None
    if request and request.days is not None:
        target_days = request.days
    elif days is not None:
        target_days = days

    deleted_count = clean_audit_logs(db, max_age_days=target_days)
    if target_days and target_days > 0:
        msg = f"Cleaned {deleted_count} logs older than {target_days} days. Retention ceiling is {MAX_AUDIT_RETENTION_DAYS} days."
    else:
        msg = f"Cleaned all {deleted_count} audit logs. Log trail reset."

    return AuditCleanResponse(
        deleted_count=deleted_count,
        message=msg,
        retention_days=MAX_AUDIT_RETENTION_DAYS
    )
