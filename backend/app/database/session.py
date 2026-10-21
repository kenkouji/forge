from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from typing import Generator
from app.config import settings

is_sqlite = settings.DATABASE_URL.startswith("sqlite")
connect_args = {"check_same_thread": False} if is_sqlite else {}

engine = create_engine(
    settings.DATABASE_URL,
    connect_args=connect_args,
    echo=False
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def ensure_sqlite_migrations(target_engine):
    if not is_sqlite:
        return
    with target_engine.connect() as conn:
        try:
            cols = [row[1] for row in conn.execute(text("PRAGMA table_info(automations)")).fetchall()]
            if cols:
                needed = [
                    ("action_arguments_json", "TEXT DEFAULT '{}'"),
                    ("scheduled_epoch", "FLOAT DEFAULT 0.0"),
                    ("is_one_shot", "BOOLEAN DEFAULT 0"),
                    ("workflow_definition_json", "TEXT")
                ]
                for col_name, col_type in needed:
                    if col_name not in cols:
                        conn.execute(text(f"ALTER TABLE automations ADD COLUMN {col_name} {col_type}"))
                conn.commit()
        except Exception:
            pass

ensure_sqlite_migrations(engine)

def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
