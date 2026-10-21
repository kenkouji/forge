import os
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse

from app.config import settings
from app.database.base import Base
from app.database.session import engine, SessionLocal
from app.models.entities import User, Task
from app.auth.security import get_password_hash

# Routes
from app.api.routes.health import router as health_router
from app.api.routes.auth import router as auth_router
from app.api.routes.devices import router as devices_router
from app.api.routes.tasks import router as tasks_router
from app.api.routes.audit import router as audit_router
from app.api.routes.files import router as files_router
from app.api.routes.ai import router as ai_router
from app.api.routes.automations import router as automations_router
from app.api.routes.stream import router as stream_router
from app.api.routes.clipboard import router as clipboard_router
from app.api.routes.guest import router as guest_router
from app.websocket.router import ws_router


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("remoteforge.main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing RemoteForge database tables...")
    Base.metadata.create_all(bind=engine)

    # Seed initial admin user if not present
    db = SessionLocal()
    try:
        admin = db.query(User).filter(User.username == settings.INITIAL_ADMIN_USERNAME).first()
        if not admin:
            logger.info(f"Seeding default admin user: {settings.INITIAL_ADMIN_USERNAME}")
            hashed_pw = get_password_hash(settings.INITIAL_ADMIN_PASSWORD)
            admin = User(
                username=settings.INITIAL_ADMIN_USERNAME,
                hashed_password=hashed_pw,
                role="admin",
                is_active=True
            )
            db.add(admin)
            db.commit()

        # Ensure schema migrations for automations
        with engine.connect() as conn:
            from sqlalchemy import text
            try:
                cols = [row[1] for row in conn.execute(text("PRAGMA table_info(automations)")).fetchall()]
                if cols and "workflow_definition_json" not in cols:
                    conn.execute(text("ALTER TABLE automations ADD COLUMN workflow_definition_json TEXT"))
                    conn.commit()
                    logger.info("Migrated automations table: added workflow_definition_json column.")
            except Exception as e:
                logger.warning(f"Schema migration check note: {e}")

        # Mark in-flight tasks as INTERRUPTED upon server restart
        import datetime
        interrupted = db.query(Task).filter(Task.status.in_(["RUNNING", "PENDING"])).all()
        for it in interrupted:
            it.status = "INTERRUPTED"
            it.error_message = "Server restarted while task was in progress."
            it.completed_at = datetime.datetime.now(datetime.timezone.utc)
        if interrupted:
            db.commit()
            logger.info(f"Marked {len(interrupted)} in-flight tasks as INTERRUPTED on startup.")
    finally:
        db.close()

    import asyncio
    from app.services.scheduler import run_scheduler_worker
    scheduler_task = asyncio.create_task(run_scheduler_worker())

    logger.info(f"RemoteForge server started on {settings.SERVER_HOST}:{settings.SERVER_PORT}")
    yield
    scheduler_task.cancel()
    logger.info("RemoteForge server shutting down.")

app = FastAPI(
    title="RemoteForge",
    description="Secure Remote AI Computer Control Platform API",
    version="0.1.0",
    lifespan=lifespan
)

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if settings.CORS_ORIGINS == ["*"] else settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory sliding window rate limiter for login protection
login_attempts: dict = {}
MAX_LOGIN_ATTEMPTS = 5
WINDOW_SECONDS = 60.0

@app.middleware("http")
async def rate_limit_middleware(request, call_next):
    if request.url.path == "/api/auth/login" and request.method == "POST":
        import time
        from fastapi.responses import JSONResponse
        # Check CF-Connecting-IP or X-Forwarded-For when behind tunnel/reverse-proxy
        client_ip = (
            request.headers.get("cf-connecting-ip")
            or request.headers.get("x-forwarded-for", "").split(",")[0].strip()
            or (request.client.host if request.client else "unknown")
        )
        now = time.time()
        recent = [t for t in login_attempts.get(client_ip, []) if now - t < WINDOW_SECONDS]
        if len(recent) >= MAX_LOGIN_ATTEMPTS:
            return JSONResponse(
                status_code=429,
                content={"detail": "Too many failed login attempts. Please wait 60 seconds."}
            )
        response = await call_next(request)
        if response.status_code == 401:
            recent.append(now)
            login_attempts[client_ip] = recent
        elif response.status_code == 200:
            login_attempts.pop(client_ip, None)
        return response
    return await call_next(request)

# Mount API routes
app.include_router(health_router, prefix="/api")
app.include_router(auth_router, prefix="/api")
app.include_router(devices_router, prefix="/api")
app.include_router(tasks_router, prefix="/api")
app.include_router(audit_router, prefix="/api")
app.include_router(files_router, prefix="/api")
app.include_router(ai_router, prefix="/api")
app.include_router(automations_router, prefix="/api")
app.include_router(stream_router, prefix="/api")
app.include_router(clipboard_router, prefix="/api")
app.include_router(guest_router, prefix="/api")
app.include_router(ws_router)


# Direct download endpoint for Android Debug APK
@app.get("/app-debug.apk", include_in_schema=True)
@app.get("/download-apk", include_in_schema=True)
async def download_android_apk():
    apk_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "android", "app", "build", "outputs", "apk", "debug", "app-debug.apk")
    if not os.path.exists(apk_path):
        raise HTTPException(status_code=404, detail="APK not found on server.")
    return FileResponse(
        apk_path,
        media_type="application/vnd.android.package-archive",
        filename="remoteforge-debug.apk"
    )

# Mount web client dist if available for single-port convenience
web_dist_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "web", "dist")
if os.path.exists(web_dist_dir):
    app.mount("/assets", StaticFiles(directory=os.path.join(web_dist_dir, "assets")), name="assets")

    @app.get("/{full_path:path}", include_in_schema=False)
    async def serve_spa(full_path: str):
        if full_path.startswith("api/") or full_path.startswith("ws/") or full_path.endswith(".apk"):
            return None
        index_file = os.path.join(web_dist_dir, "index.html")
        if os.path.exists(index_file):
            return FileResponse(index_file)
