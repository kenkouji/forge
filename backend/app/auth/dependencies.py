from typing import Optional
from fastapi import Depends, HTTPException, status, Header
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from app.database.session import get_db
from app.models.entities import User
from app.auth.security import decode_access_token
from app.config import settings

security_bearer = HTTPBearer(auto_error=False)

def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_bearer),
    db: Session = Depends(get_db)
) -> User:
    if not credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing authentication credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    payload = decode_access_token(credentials.credentials)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired authentication token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    username: str = payload.get("sub")
    if not username:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token payload",
        )
    user = db.query(User).filter(User.username == username).first()
    if not user or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found or inactive",
        )
    return user

def verify_agent_token_header(x_agent_token: Optional[str] = Header(None)) -> bool:
    if not x_agent_token or x_agent_token != settings.AGENT_TOKEN:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized PC Agent token"
        )
    return True

def get_current_guest_session(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_bearer),
    db: Session = Depends(get_db)
):
    from app.models.entities import GuestSession
    import datetime
    if not credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing guest authentication credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    payload = decode_access_token(credentials.credentials)
    if not payload or payload.get("role") != "guest":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired guest session token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    session_id = payload.get("session_id")
    if not session_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid guest token claims")

    session = db.query(GuestSession).filter(GuestSession.id == session_id).first()
    if not session:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Guest session not found")

    now = datetime.datetime.now(datetime.timezone.utc)
    session_expires = session.expires_at
    if session_expires.tzinfo is None:
        session_expires = session_expires.replace(tzinfo=datetime.timezone.utc)

    if session_expires < now or session.status in ["EXPIRED", "REVOKED"]:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Guest session has expired or been revoked")

    return session

def get_caller_principal(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_bearer),
    db: Session = Depends(get_db)
):
    from app.models.entities import User, GuestSession
    import datetime
    if not credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing authentication credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    payload = decode_access_token(credentials.credentials)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    role = payload.get("role")
    if role == "guest":
        session_id = payload.get("session_id")
        session = db.query(GuestSession).filter(GuestSession.id == session_id).first()
        if not session:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Guest session not found")
        now = datetime.datetime.now(datetime.timezone.utc)
        session_expires = session.expires_at
        if session_expires.tzinfo is None:
            session_expires = session_expires.replace(tzinfo=datetime.timezone.utc)
        if session_expires < now or session.status in ["EXPIRED", "REVOKED"]:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Guest session has expired or been revoked")
        return {"type": "guest", "session": session, "actor": f"guest:{session.id}:{session.guest_name}"}
    else:
        username = payload.get("sub")
        user = db.query(User).filter(User.username == username).first()
        if not user or not user.is_active:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found or inactive")
        return {"type": "user", "user": user, "actor": user.username}

