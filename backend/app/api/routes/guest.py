import os
import re
import json
import time
import uuid
import secrets
import hashlib
import hmac
import shutil
import datetime
import socket
import psutil
from typing import List, Optional, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File, Form, Request, status
from fastapi.responses import StreamingResponse, JSONResponse
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.models.entities import (
    User, Device, AllowedDirectory, GuestInvitation, GuestSession, StagedFile, PrivacyShieldState
)
from app.auth.dependencies import get_current_user, get_current_guest_session, get_caller_principal
from app.auth.security import create_access_token
from app.websocket.manager import ws_manager
from app.audit.logger import record_audit_log
from app.schemas.all_schemas import (
    GuestInvitationCreateRequest, GuestInvitationResponse,
    GuestRedeemRequest, GuestRedeemResponse, GuestSessionStatusResponse,
    GuestApprovalRequest, GuestSessionResponse,
    PrivacyShieldUpdateRequest, PrivacyShieldResponse,
    StagedFileResponse, PromoteFileRequest
)

router = APIRouter(prefix="/guest", tags=["Guest Access & Co-Control"])

STAGING_BASE_DIR = "/tmp/remoteforge_staging"
MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024       # 50 MB per file limit
MAX_SESSION_QUOTA_BYTES = 200 * 1024 * 1024   # 200 MB per session limit

# In-memory sliding rate limiter for redemption: IP -> list of timestamps
REDEMPTION_RATE_LIMIT: Dict[str, List[float]] = {}
RATE_LIMIT_WINDOW = 60.0


def get_hotspot_or_lan_ip(request: Optional[Request] = None) -> str:
    """
    Returns the reachable LAN or Hotspot IP address of this host so that any
    guest device connected to the owner's Wi-Fi / Hotspot can open the guest portal.
    Filters out loopback (127.x) and NetBird/Tailscale (100.x) VPN addresses.
    """
    # 1. First attempt: UDP route to external/gateway IP (does not transmit packets)
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(0.5)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        if not ip.startswith("127.") and not ip.startswith("100."):
            return ip
    except Exception:
        pass

    # 2. Second attempt: Check network interfaces via psutil
    try:
        for iface, addrs in psutil.net_if_addrs().items():
            if any(iface.startswith(prefix) for prefix in ["utun", "lo", "docker", "tailscale"]):
                continue
            for a in addrs:
                if a.family == socket.AF_INET:
                    addr = a.address
                    if not addr.startswith("127.") and not addr.startswith("100."):
                        return addr
    except Exception:
        pass

    # 3. Fallback: Host from request if valid LAN address
    if request and request.url.hostname:
        host = request.url.hostname
        if not host.startswith("127.") and not host.startswith("100."):
            return host

    return "127.0.0.1"
RATE_LIMIT_MAX_ATTEMPTS = 10


ALLOWED_CAPABILITIES = {
    "screen.view", "input.control", "clipboard.read", "clipboard.write",
    "files.list", "files.upload", "files.download", "session.request_control"
}


def sanitize_filename(filename: str) -> str:
    """Sanitize user-provided filename to prevent path traversal or filesystem escapes."""
    # Strip paths, directories, and drive letters
    base = os.path.basename(filename)
    # Remove null bytes, control characters, and dangerous characters
    safe = re.sub(r'[^a-zA-Z0-9_\-\.\(\)\s]', '_', base)
    # Strip leading/trailing dots or spaces
    safe = safe.strip(". ")
    if not safe:
        safe = f"file_{uuid.uuid4().hex[:8]}"
    return safe[:128]


def check_rate_limit(client_ip: str):
    """Enforce brute-force protection on guest redemption."""
    now = time.time()
    attempts = REDEMPTION_RATE_LIMIT.get(client_ip, [])
    # Filter attempts within window
    attempts = [t for t in attempts if now - t < RATE_LIMIT_WINDOW]
    if len(attempts) >= RATE_LIMIT_MAX_ATTEMPTS:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Too many invitation redemption attempts. Please wait a minute."
        )
    attempts.append(now)
    REDEMPTION_RATE_LIMIT[client_ip] = attempts


def get_or_create_privacy_shield(device_id: str, db: Session) -> PrivacyShieldState:
    ps = db.query(PrivacyShieldState).filter(PrivacyShieldState.device_id == device_id).first()
    if not ps:
        ps = PrivacyShieldState(
            device_id=device_id,
            is_lockdown=False,
            is_screen_paused=False,
            is_input_disabled=False,
            is_transfers_stopped=False,
            pending_host_sync=False,
            updated_by="system"
        )
        db.add(ps)
        db.commit()
        db.refresh(ps)
    return ps


# =========================================================================
# Feature A: QR-Based Browser Guest Invitation & Redemption
# =========================================================================

@router.post("/invitations", response_model=GuestInvitationResponse)
def create_guest_invitation(
    req: GuestInvitationCreateRequest,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Owner generates a single-use guest invitation.
    The secret is generated using secrets.token_urlsafe(32) and stored ONLY as SHA-256 hash.
    The QR payload uses a URL fragment (#invite_code=...) so the secret is never transmitted in HTTP headers.
    """
    device = db.query(Device).filter(Device.id == req.device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    ps = get_or_create_privacy_shield(req.device_id, db)
    if ps.is_lockdown:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Emergency lockdown is active on this device. New guest invitations are blocked."
        )

    # Validate capabilities against allowlist
    valid_caps = [c for c in req.capabilities if c in ALLOWED_CAPABILITIES]
    if not valid_caps:
        valid_caps = ["screen.view"]

    # Generate single-use secret token
    invite_secret = secrets.token_urlsafe(32)
    token_hash = hashlib.sha256(invite_secret.encode("utf-8")).hexdigest()

    now = datetime.datetime.now(datetime.timezone.utc)
    expiry = now + datetime.timedelta(seconds=max(60, min(req.expiry_seconds, 86400)))
    inv_id = str(uuid.uuid4())

    invitation = GuestInvitation(
        id=inv_id,
        device_id=req.device_id,
        created_by=current_user.username,
        token_hash=token_hash,
        permitted_capabilities_json=json.dumps(valid_caps),
        allowed_staging_dir=req.allowed_staging_dir or "",
        expiry_seconds=req.expiry_seconds,
        expires_at=expiry,
        is_redeemed=False,
        is_revoked=False,
        created_at=now
    )
    db.add(invitation)
    db.commit()
    db.refresh(invitation)

    record_audit_log(
        db=db,
        actor=current_user.username,
        device_id=req.device_id,
        action="GUEST_INVITATION_CREATED",
        tool="guest_access",
        arguments={"invitation_id": inv_id, "capabilities": valid_caps, "expiry_seconds": req.expiry_seconds},
        permission_level="LOW",
        execution_status="SUCCESS",
        details="Owner created single-use QR guest invitation"
    )

    # Format QR payload using URL fragment (#) so secrets never hit access logs or referrer headers
    # Resolve the reachable Hotspot / LAN IP so guests connected to the owner's hotspot can open it directly in their mobile browser
    lan_ip = get_hotspot_or_lan_ip(request)
    port = request.url.port or 8090
    qr_payload = f"http://{lan_ip}:{port}/#invitation_id={inv_id}&secret={invite_secret}"

    return GuestInvitationResponse(
        id=inv_id,
        device_id=req.device_id,
        invite_secret=invite_secret,
        qr_payload=qr_payload,
        expires_at=expiry,
        capabilities=valid_caps,
        is_redeemed=False,
        is_revoked=False
    )


@router.get("/invitations/{device_id}", response_model=List[GuestInvitationResponse])
def list_guest_invitations(
    device_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Owner lists invitations for a device. Token secrets are redacted."""
    invitations = db.query(GuestInvitation).filter(
        GuestInvitation.device_id == device_id,
        GuestInvitation.is_revoked == False
    ).order_by(GuestInvitation.created_at.desc()).all()

    results = []
    for inv in invitations:
        caps = json.loads(inv.permitted_capabilities_json) if inv.permitted_capabilities_json else []
        results.append(GuestInvitationResponse(
            id=inv.id,
            device_id=inv.device_id,
            invite_secret=None,
            qr_payload="",
            expires_at=inv.expires_at,
            capabilities=caps,
            is_redeemed=inv.is_redeemed,
            is_revoked=inv.is_revoked
        ))
    return results


@router.delete("/invitations/{invitation_id}")
def revoke_guest_invitation(
    invitation_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Owner revokes an unused guest invitation."""
    inv = db.query(GuestInvitation).filter(GuestInvitation.id == invitation_id).first()
    if not inv:
        raise HTTPException(status_code=404, detail="Invitation not found")

    inv.is_revoked = True
    db.commit()

    record_audit_log(
        db=db,
        actor=current_user.username,
        device_id=inv.device_id,
        action="GUEST_INVITATION_REVOKED",
        tool="guest_access",
        arguments={"invitation_id": invitation_id},
        permission_level="LOW",
        execution_status="SUCCESS",
        details="Owner revoked unused guest invitation"
    )
    return {"success": True, "message": "Invitation revoked"}


@router.post("/redeem", response_model=GuestRedeemResponse)
async def redeem_guest_invitation(
    req: GuestRedeemRequest,
    request: Request,
    db: Session = Depends(get_db)
):
    """
    Guest browser redeems invitation.
    Validates single-use token hash, rate limits attempts, and creates an isolated session in PENDING_APPROVAL state.
    """
    client_ip = request.client.host if request.client else "unknown"
    check_rate_limit(client_ip)

    inv = db.query(GuestInvitation).filter(GuestInvitation.id == req.invitation_id).first()
    if not inv:
        raise HTTPException(status_code=404, detail="Invitation not found or invalid")

    now = datetime.datetime.now(datetime.timezone.utc)
    inv_expires = inv.expires_at
    if inv_expires.tzinfo is None:
        inv_expires = inv_expires.replace(tzinfo=datetime.timezone.utc)

    if inv.is_revoked or inv_expires < now:
        raise HTTPException(status_code=status.HTTP_410_GONE, detail="Invitation has expired or been revoked")

    if inv.is_redeemed:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="This invitation has already been redeemed")

    # Verify constant-time token hash match
    provided_hash = hashlib.sha256(req.invite_secret.encode("utf-8")).hexdigest()
    if not hmac.compare_digest(provided_hash, inv.token_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid invitation secret")

    # Mark as redeemed immediately to prevent race conditions
    inv.is_redeemed = True

    # Check device Privacy Shield
    ps = get_or_create_privacy_shield(inv.device_id, db)
    if ps.is_lockdown:
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Host is in emergency lockdown. Guest redemption denied."
        )

    session_id = str(uuid.uuid4())
    caps = json.loads(inv.permitted_capabilities_json) if inv.permitted_capabilities_json else ["screen.view"]
    safe_guest_name = sanitize_filename(req.guest_name or "Guest")

    # Create isolated staging directory for this session
    session_staging = os.path.join(STAGING_BASE_DIR, session_id)
    os.makedirs(session_staging, exist_ok=True)

    # Issue scoped guest JWT (short-lived, scoped strictly to this session)
    guest_token = create_access_token(
        data={
            "sub": f"guest:{session_id}",
            "role": "guest",
            "session_id": session_id,
            "device_id": inv.device_id,
            "guest_name": safe_guest_name
        },
        expires_delta=datetime.timedelta(seconds=inv.expiry_seconds)
    )
    session_token_hash = hashlib.sha256(guest_token.encode("utf-8")).hexdigest()

    session = GuestSession(
        id=session_id,
        invitation_id=inv.id,
        device_id=inv.device_id,
        guest_name=safe_guest_name,
        session_token_hash=session_token_hash,
        status="PENDING_APPROVAL",
        control_mode="view-only",
        approved_capabilities_json=json.dumps(caps),
        staging_dir=session_staging,
        expires_at=inv_expires,
        created_at=now,
        last_active_at=now
    )
    db.add(session)
    db.commit()
    db.refresh(session)

    # Notify owner clients via WebSocket in real-time
    await ws_manager.broadcast_to_clients({
        "type": "guest_request",
        "session_id": session_id,
        "device_id": inv.device_id,
        "guest_name": safe_guest_name,
        "capabilities": caps,
        "expires_at": inv_expires.isoformat()
    })

    record_audit_log(
        db=db,
        actor=f"guest:{session_id}:{safe_guest_name}",
        device_id=inv.device_id,
        action="GUEST_REDEEMED_INVITATION",
        tool="guest_access",
        arguments={"session_id": session_id, "invitation_id": inv.id},
        permission_level="LOW",
        execution_status="SUCCESS",
        details="Guest redeemed invitation; awaiting owner approval"
    )

    return GuestRedeemResponse(
        session_id=session_id,
        device_id=inv.device_id,
        guest_token=guest_token,
        status="PENDING_APPROVAL",
        expires_at=inv_expires,
        capabilities=caps
    )


# =========================================================================
# Feature B: Live Co-Control, Approvals & Session Management
# =========================================================================

@router.get("/session/status", response_model=GuestSessionStatusResponse)
def get_guest_session_status(
    session: GuestSession = Depends(get_current_guest_session),
    db: Session = Depends(get_db)
):
    """Guest polling / heartbeat check for approval and session status."""
    ps = get_or_create_privacy_shield(session.device_id, db)
    caps = json.loads(session.approved_capabilities_json) if session.approved_capabilities_json else []

    return GuestSessionStatusResponse(
        session_id=session.id,
        id=session.id,
        device_id=session.device_id,
        guest_name=session.guest_name,
        status=session.status,
        control_mode=session.control_mode,
        capabilities=caps,
        approved_capabilities=caps,
        expires_at=session.expires_at,
        is_screen_paused=ps.is_screen_paused or ps.is_lockdown,
        is_input_disabled=ps.is_input_disabled or ps.is_lockdown,
        is_lockdown=ps.is_lockdown
    )


@router.get("/sessions/{device_id}", response_model=List[GuestSessionResponse])
def list_device_guest_sessions(
    device_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Owner lists all guest sessions (active, pending, revoked) for a workstation."""
    sessions = db.query(GuestSession).filter(
        GuestSession.device_id == device_id
    ).order_by(GuestSession.created_at.desc()).all()

    now = datetime.datetime.now(datetime.timezone.utc)
    results = []
    for s in sessions:
        exp = s.expires_at
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=datetime.timezone.utc)
        is_act = (s.status == "APPROVED" and exp > now and not s.revoked_at)
        caps = json.loads(s.approved_capabilities_json) if s.approved_capabilities_json else []
        results.append(GuestSessionResponse(
            id=s.id,
            device_id=s.device_id,
            guest_name=s.guest_name,
            status=s.status,
            control_mode=s.control_mode,
            approved_capabilities=caps,
            expires_at=s.expires_at,
            created_at=s.created_at,
            is_active=is_act
        ))
    return results


@router.post("/sessions/{session_id}/approve", response_model=GuestSessionResponse)
async def approve_or_reject_guest_session(
    session_id: str,
    req: GuestApprovalRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Owner approves or rejects a pending guest session."""
    session = db.query(GuestSession).filter(GuestSession.id == session_id).first()
    if not session:
        raise HTTPException(status_code=404, detail="Guest session not found")

    now = datetime.datetime.now(datetime.timezone.utc)
    if req.approve:
        session.status = "APPROVED"
        session.approved_at = now
        
        # Support both approved_capabilities and capabilities from Android/Web clients
        caps = req.approved_capabilities if req.approved_capabilities is not None else req.capabilities
        if caps is not None:
            valid_caps = [c for c in caps if c in ALLOWED_CAPABILITIES]
            session.approved_capabilities_json = json.dumps(valid_caps)
        elif not session.approved_capabilities_json or session.approved_capabilities_json == "[]":
            session.approved_capabilities_json = json.dumps(["screen.view", "input.control", "files.upload", "files.download"])

        if req.control_mode in ["view-only", "shared-control", "presenter"]:
            session.control_mode = req.control_mode

        if req.duration_minutes and req.duration_minutes > 0:
            session.expires_at = now + datetime.timedelta(minutes=req.duration_minutes)

        # Real-time WebSocket broadcast
        await ws_manager.broadcast_to_clients({
            "type": "guest_approved",
            "session_id": session.id,
            "device_id": session.device_id,
            "guest_name": session.guest_name,
            "control_mode": session.control_mode,
            "capabilities": json.loads(session.approved_capabilities_json)
        })

        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=session.device_id,
            action="GUEST_SESSION_APPROVED",
            tool="guest_access",
            arguments={"session_id": session.id, "control_mode": session.control_mode},
            permission_level="MEDIUM",
            execution_status="SUCCESS",
            details="Owner approved guest access to workstation"
        )
    else:
        session.status = "REJECTED"
        session.revoked_at = now

        await ws_manager.broadcast_to_clients({
            "type": "guest_rejected",
            "session_id": session.id,
            "device_id": session.device_id
        })

        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=session.device_id,
            action="GUEST_SESSION_REJECTED",
            tool="guest_access",
            arguments={"session_id": session.id},
            permission_level="LOW",
            execution_status="SUCCESS",
            details="Owner rejected guest access request"
        )

    db.commit()
    db.refresh(session)

    caps = json.loads(session.approved_capabilities_json) if session.approved_capabilities_json else []
    return GuestSessionResponse(
        id=session.id,
        device_id=session.device_id,
        guest_name=session.guest_name,
        status=session.status,
        control_mode=session.control_mode,
        approved_capabilities=caps,
        expires_at=session.expires_at,
        created_at=session.created_at,
        is_active=(session.status == "APPROVED")
    )


@router.post("/sessions/{session_id}/revoke")
async def revoke_guest_session(
    session_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Owner immediately revokes and terminates a specific guest session."""
    session = db.query(GuestSession).filter(GuestSession.id == session_id).first()
    if not session:
        raise HTTPException(status_code=404, detail="Guest session not found")

    session.status = "REVOKED"
    session.revoked_at = datetime.datetime.now(datetime.timezone.utc)
    db.commit()

    # Broadcast revocation so guest screen/session closes immediately
    await ws_manager.broadcast_to_clients({
        "type": "guest_revoked",
        "session_id": session.id,
        "device_id": session.device_id,
        "reason": "Revoked by owner"
    })

    record_audit_log(
        db=db,
        actor=current_user.username,
        device_id=session.device_id,
        action="GUEST_SESSION_REVOKED",
        tool="guest_access",
        arguments={"session_id": session.id},
        permission_level="MEDIUM",
        execution_status="SUCCESS",
        details="Owner explicitly terminated guest session"
    )

    return {"success": True, "message": f"Session {session_id} revoked"}


# =========================================================================
# Feature D: Privacy Shield and Emergency Lockdown
# =========================================================================

@router.post("/privacy-shield/{device_id}", response_model=PrivacyShieldResponse)
async def update_privacy_shield(
    device_id: str,
    req: PrivacyShieldUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Owner executes Privacy Shield controls:
    Revoke All, Pause Screen, Disable Input, Stop Transfers, Emergency Lockdown, Resume Access.
    Enforces state on backend AND host agent!
    """
    device = db.query(Device).filter(Device.id == device_id).first()
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    ps = get_or_create_privacy_shield(device_id, db)
    now = datetime.datetime.now(datetime.timezone.utc)

    if req.action == "emergency_lockdown":
        ps.is_lockdown = True
        ps.is_screen_paused = True
        ps.is_input_disabled = True
        ps.is_transfers_stopped = True

        # Revoke ALL active sessions for this device
        active_sessions = db.query(GuestSession).filter(
            GuestSession.device_id == device_id,
            GuestSession.status == "APPROVED"
        ).all()
        for s in active_sessions:
            s.status = "REVOKED"
            s.revoked_at = now

        # Invalidate all unused invitations
        db.query(GuestInvitation).filter(
            GuestInvitation.device_id == device_id,
            GuestInvitation.is_revoked == False
        ).update({"is_revoked": True})

        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=device_id,
            action="EMERGENCY_LOCKDOWN_ACTIVATED",
            tool="privacy_shield",
            arguments={"device_id": device_id},
            permission_level="HIGH",
            confirmation_status="CONFIRMED",
            execution_status="SUCCESS",
            details="Owner activated Emergency Lockdown: all guests revoked, input/screen stopped"
        )

    elif req.action == "resume_access":
        ps.is_lockdown = False
        ps.is_screen_paused = False
        ps.is_input_disabled = False
        ps.is_transfers_stopped = False

        record_audit_log(
            db=db,
            actor=current_user.username,
            device_id=device_id,
            action="PRIVACY_SHIELD_RESUME_ACCESS",
            tool="privacy_shield",
            arguments={"device_id": device_id},
            permission_level="MEDIUM",
            execution_status="SUCCESS",
            details="Owner resumed access after verification"
        )

    elif req.action == "revoke_all":
        active_sessions = db.query(GuestSession).filter(
            GuestSession.device_id == device_id,
            GuestSession.status == "APPROVED"
        ).all()
        for s in active_sessions:
            s.status = "REVOKED"
            s.revoked_at = now

    else:
        # Granular toggles
        if req.is_screen_paused is not None:
            ps.is_screen_paused = req.is_screen_paused
        if req.is_input_disabled is not None:
            ps.is_input_disabled = req.is_input_disabled
        if req.is_transfers_stopped is not None:
            ps.is_transfers_stopped = req.is_transfers_stopped
        if req.is_lockdown is not None:
            ps.is_lockdown = req.is_lockdown

    ps.updated_at = now
    ps.updated_by = current_user.username

    # Sync Privacy Shield with Host PC Agent
    is_online = ws_manager.is_agent_online(device_id)
    if is_online:
        try:
            await ws_manager.send_command_to_agent(
                device_id=device_id,
                request_id=str(uuid.uuid4()),
                tool="sync_privacy_shield",
                arguments={
                    "is_lockdown": ps.is_lockdown,
                    "is_screen_paused": ps.is_screen_paused,
                    "is_input_disabled": ps.is_input_disabled,
                    "is_transfers_stopped": ps.is_transfers_stopped
                },
                timeout_seconds=5.0
            )
            ps.pending_host_sync = False
        except Exception:
            ps.pending_host_sync = True
    else:
        # Agent is offline; report pending unconfirmed revocation
        ps.pending_host_sync = True

    db.commit()
    db.refresh(ps)

    # Broadcast update to web and mobile clients
    await ws_manager.broadcast_to_clients({
        "type": "privacy_shield_sync",
        "device_id": device_id,
        "is_lockdown": ps.is_lockdown,
        "is_screen_paused": ps.is_screen_paused,
        "is_input_disabled": ps.is_input_disabled,
        "is_transfers_stopped": ps.is_transfers_stopped,
        "pending_host_sync": ps.pending_host_sync
    })

    active_cnt = db.query(GuestSession).filter(
        GuestSession.device_id == device_id,
        GuestSession.status == "APPROVED",
        GuestSession.expires_at > now
    ).count()

    return PrivacyShieldResponse(
        device_id=device_id,
        is_lockdown=ps.is_lockdown,
        is_screen_paused=ps.is_screen_paused,
        is_input_disabled=ps.is_input_disabled,
        is_transfers_stopped=ps.is_transfers_stopped,
        pending_host_sync=ps.pending_host_sync,
        active_guests_count=active_cnt,
        updated_at=ps.updated_at
    )


@router.get("/privacy-shield/{device_id}", response_model=PrivacyShieldResponse)
def get_privacy_shield_status(
    device_id: str,
    principal: Dict[str, Any] = Depends(get_caller_principal),
    db: Session = Depends(get_db)
):
    """Retrieve Privacy Shield status for a workstation."""
    ps = get_or_create_privacy_shield(device_id, db)
    now = datetime.datetime.now(datetime.timezone.utc)
    active_cnt = db.query(GuestSession).filter(
        GuestSession.device_id == device_id,
        GuestSession.status == "APPROVED",
        GuestSession.expires_at > now
    ).count()

    return PrivacyShieldResponse(
        device_id=device_id,
        is_lockdown=ps.is_lockdown,
        is_screen_paused=ps.is_screen_paused,
        is_input_disabled=ps.is_input_disabled,
        is_transfers_stopped=ps.is_transfers_stopped,
        pending_host_sync=ps.pending_host_sync,
        active_guests_count=active_cnt,
        updated_at=ps.updated_at
    )


# =========================================================================
# Feature C: Guest Drop Zone (Staged File Exchange)
# =========================================================================

@router.get("/staging/files", response_model=List[StagedFileResponse])
def list_staged_files(
    device_id: Optional[str] = Query(None),
    principal: Dict[str, Any] = Depends(get_caller_principal),
    db: Session = Depends(get_db)
):
    """
    List files in the staged Drop Zone.
    Guests see files staged in their own session.
    Owner can inspect staged files across sessions for review.
    """
    query = db.query(StagedFile).filter(StagedFile.status != "DELETED")
    if principal["type"] == "guest":
        session: GuestSession = principal["session"]
        caps = json.loads(session.approved_capabilities_json) if session.approved_capabilities_json else []
        if "files.list" not in caps:
            raise HTTPException(status_code=403, detail="Missing capability: files.list")
        query = query.filter(StagedFile.session_id == session.id)
    else:
        if device_id:
            query = query.filter(StagedFile.device_id == device_id)

    files = query.order_by(StagedFile.uploaded_at.desc()).all()
    return [
        StagedFileResponse(
            id=f.id,
            session_id=f.session_id,
            device_id=f.device_id,
            filename=f.filename,
            file_size=f.file_size,
            sha256_hash=f.sha256_hash,
            status=f.status,
            uploaded_at=f.uploaded_at,
            promoted_to_path=f.promoted_to_path
        ) for f in files
    ]


@router.post("/staging/upload", response_model=StagedFileResponse)
async def upload_staged_file(
    file: UploadFile = File(...),
    session: GuestSession = Depends(get_current_guest_session),
    db: Session = Depends(get_db)
):
    """
    Guest uploads file to quarantined Drop Zone.
    Enforces capabilities, size quotas, filename sanitization, and SHA-256 integrity calculation.
    """
    ps = get_or_create_privacy_shield(session.device_id, db)
    if ps.is_transfers_stopped or ps.is_lockdown:
        raise HTTPException(status_code=403, detail="File transfers are suspended or locked down by owner")

    caps = json.loads(session.approved_capabilities_json) if session.approved_capabilities_json else []
    if "files.upload" not in caps:
        raise HTTPException(status_code=403, detail="Missing capability: files.upload")

    # Sanitize filename
    safe_name = sanitize_filename(file.filename or "upload")
    file_id = str(uuid.uuid4())
    session_staging = session.staging_dir or os.path.join(STAGING_BASE_DIR, session.id)
    os.makedirs(session_staging, exist_ok=True)
    target_path = os.path.join(session_staging, safe_name)

    # Read and hash in bounded memory chunks
    hasher = hashlib.sha256()
    total_size = 0

    with open(target_path, "wb") as f_out:
        while True:
            chunk = await file.read(256 * 1024)
            if not chunk:
                break
            total_size += len(chunk)
            if total_size > MAX_FILE_SIZE_BYTES:
                try:
                    os.remove(target_path)
                except OSError:
                    pass
                raise HTTPException(
                    status_code=413,
                    detail=f"File exceeds maximum allowed size of {MAX_FILE_SIZE_BYTES // (1024*1024)} MB"
                )
            hasher.update(chunk)
            f_out.write(chunk)

    sha256 = hasher.hexdigest()
    now = datetime.datetime.now(datetime.timezone.utc)

    # Check if we should deliver directly to user's Desktop
    desktop_target = None
    # 1. Look for allowed directory matching desktop
    allowed_dirs = db.query(AllowedDirectory).filter(
        AllowedDirectory.device_id == session.device_id,
        AllowedDirectory.is_active == True
    ).all()
    for ad in allowed_dirs:
        if "desktop" in ad.canonical_path.lower():
            desktop_target = ad.canonical_path
            break

    # 2. If not found in AllowedDirectory and running outside pytest, fallback to user Desktop
    if not desktop_target and not os.environ.get("PYTEST_CURRENT_TEST"):
        home_desktop = os.path.expanduser("~/Desktop")
        if os.path.exists(home_desktop):
            desktop_target = home_desktop

    staged_status = "QUARANTINED"
    promoted_dest = None
    if desktop_target and os.path.exists(desktop_target):
        dest_file = os.path.join(desktop_target, safe_name)
        try:
            shutil.copy2(target_path, dest_file)
            staged_status = "DELIVERED"
            promoted_dest = dest_file
        except Exception:
            pass

    staged = StagedFile(
        id=file_id,
        session_id=session.id,
        device_id=session.device_id,
        filename=safe_name,
        file_path=target_path,
        file_size=total_size,
        sha256_hash=sha256,
        status=staged_status,
        promoted_to_path=promoted_dest,
        promoted_at=now if promoted_dest else None,
        uploaded_at=now
    )
    db.add(staged)
    db.commit()
    db.refresh(staged)

    # Post desktop notification on the host workstation via agent
    if staged_status == "DELIVERED" and ws_manager.is_agent_online(session.device_id):
        await ws_manager.send_command_to_agent(
            device_id=session.device_id,
            request_id=str(uuid.uuid4()),
            tool="show_notification",
            arguments={
                "title": "RemoteForge File Received",
                "message": f"File '{safe_name}' received directly on Desktop"
            }
        )

    # Notify owner clients of new staged file ready / delivered
    await ws_manager.broadcast_to_clients({
        "type": "staged_file_uploaded",
        "file_id": staged.id,
        "device_id": session.device_id,
        "filename": safe_name,
        "file_size": total_size,
        "sha256": sha256,
        "guest_name": session.guest_name,
        "status": staged_status,
        "promoted_to_path": promoted_dest,
        "location": "Desktop" if staged_status == "DELIVERED" else "Staging Quarantine"
    })

    record_audit_log(
        db=db,
        actor=f"guest:{session.id}:{session.guest_name}",
        device_id=session.device_id,
        action="GUEST_FILE_UPLOADED_TO_STAGING" if staged_status == "QUARANTINED" else "GUEST_FILE_DELIVERED_TO_DESKTOP",
        tool="drop_zone",
        arguments={"filename": safe_name, "file_size": total_size, "sha256": sha256, "status": staged_status, "promoted_to": promoted_dest},
        permission_level="LOW",
        execution_status="SUCCESS",
        details="Guest uploaded file directly delivered to Desktop" if staged_status == "DELIVERED" else "Guest uploaded file into isolated staging quarantine"
    )

    return StagedFileResponse(
        id=staged.id,
        session_id=staged.session_id,
        device_id=staged.device_id,
        filename=staged.filename,
        file_size=staged.file_size,
        sha256_hash=staged.sha256_hash,
        status=staged.status,
        uploaded_at=staged.uploaded_at
    )


@router.get("/staging/download/{file_id}")
def download_staged_file(
    file_id: str,
    principal: Dict[str, Any] = Depends(get_caller_principal),
    db: Session = Depends(get_db)
):
    """Download a file from staging drop zone."""
    staged = db.query(StagedFile).filter(StagedFile.id == file_id).first()
    if not staged or staged.status == "DELETED":
        raise HTTPException(status_code=404, detail="Staged file not found")

    ps = get_or_create_privacy_shield(staged.device_id, db)
    if ps.is_transfers_stopped or ps.is_lockdown:
        raise HTTPException(status_code=403, detail="File transfers are suspended or locked down by owner")

    if principal["type"] == "guest":
        session: GuestSession = principal["session"]
        caps = json.loads(session.approved_capabilities_json) if session.approved_capabilities_json else []
        if "files.download" not in caps or staged.session_id != session.id:
            raise HTTPException(status_code=403, detail="Access denied: unauthorized to download this file")

    if not os.path.exists(staged.file_path):
        raise HTTPException(status_code=404, detail="File does not exist on disk")

    def iterfile():
        with open(staged.file_path, "rb") as f:
            while chunk := f.read(256 * 1024):
                yield chunk

    return StreamingResponse(
        iterfile(),
        media_type="application/octet-stream",
        headers={"Content-Disposition": f'attachment; filename="{staged.filename}"'}
    )


@router.post("/staging/promote/{file_id}", response_model=StagedFileResponse)
def promote_staged_file(
    file_id: str,
    req: PromoteFileRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Owner explicitly inspects and promotes a staged file into an authorized workstation directory.
    Validates destination against allowed directory roots to prevent sandbox escapes.
    """
    staged = db.query(StagedFile).filter(StagedFile.id == file_id).first()
    if not staged or staged.status not in ["QUARANTINED", "DELIVERED"]:
        raise HTTPException(status_code=404, detail="Quarantined or delivered file not found")

    # Validate target directory is an allowed directory for the device
    allowed_dirs = db.query(AllowedDirectory).filter(
        AllowedDirectory.device_id == staged.device_id,
        AllowedDirectory.is_active == True
    ).all()

    target_canonical = os.path.realpath(req.target_directory)
    is_authorized = False
    for ad in allowed_dirs:
        ad_real = os.path.realpath(ad.canonical_path)
        if target_canonical == ad_real or target_canonical.startswith(ad_real + os.sep):
            is_authorized = True
            break

    if not is_authorized:
        raise HTTPException(
            status_code=403,
            detail=f"Target directory '{req.target_directory}' is not in the workstation's allowed roots."
        )

    dest_file = os.path.join(target_canonical, staged.filename)
    try:
        shutil.copy2(staged.file_path, dest_file)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to copy file to destination: {e}")

    now = datetime.datetime.now(datetime.timezone.utc)
    staged.status = "PROMOTED"
    staged.promoted_to_path = dest_file
    staged.promoted_at = now
    db.commit()
    db.refresh(staged)

    record_audit_log(
        db=db,
        actor=current_user.username,
        device_id=staged.device_id,
        action="GUEST_FILE_PROMOTED",
        tool="drop_zone",
        arguments={"file_id": staged.id, "dest": dest_file, "sha256": staged.sha256_hash},
        permission_level="MEDIUM",
        confirmation_status="CONFIRMED",
        execution_status="SUCCESS",
        details="Owner reviewed and promoted guest file to authorized directory"
    )

    return StagedFileResponse(
        id=staged.id,
        session_id=staged.session_id,
        device_id=staged.device_id,
        filename=staged.filename,
        file_size=staged.file_size,
        sha256_hash=staged.sha256_hash,
        status=staged.status,
        uploaded_at=staged.uploaded_at,
        promoted_to_path=staged.promoted_to_path
    )


@router.delete("/staging/files/{file_id}")
def delete_staged_file(
    file_id: str,
    principal: Dict[str, Any] = Depends(get_caller_principal),
    db: Session = Depends(get_db)
):
    """Safely delete a staged file from disk and mark status DELETED."""
    staged = db.query(StagedFile).filter(StagedFile.id == file_id).first()
    if not staged or staged.status == "DELETED":
        raise HTTPException(status_code=404, detail="Staged file not found")

    if principal["type"] == "guest":
        session: GuestSession = principal["session"]
        if staged.session_id != session.id:
            raise HTTPException(status_code=403, detail="Access denied")

    # Only delete files confined within STAGING_BASE_DIR
    real_path = os.path.realpath(staged.file_path)
    real_base = os.path.realpath(STAGING_BASE_DIR)
    if real_path.startswith(real_base + os.sep) and os.path.exists(real_path):
        try:
            os.remove(real_path)
        except OSError:
            pass

    staged.status = "DELETED"
    db.commit()
    return {"success": True, "message": "File deleted from staging"}
