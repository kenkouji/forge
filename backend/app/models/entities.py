import datetime
from sqlalchemy import String, Integer, Boolean, DateTime, Text, ForeignKey, Float
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base

class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    username: Mapped[str] = mapped_column(String(64), unique=True, index=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(256), nullable=False)
    role: Mapped[str] = mapped_column(String(32), default="admin")
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc)
    )

class Device(Base):
    __tablename__ = "devices"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    os_name: Mapped[str] = mapped_column(String(64), default="unknown")
    os_version: Mapped[str] = mapped_column(String(64), default="")
    agent_version: Mapped[str] = mapped_column(String(32), default="0.1.0")
    is_online: Mapped[bool] = mapped_column(Boolean, default=False)
    last_seen: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc)
    )
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc)
    )

    allowed_directories = relationship("AllowedDirectory", back_populates="device", cascade="all, delete-orphan")
    tasks = relationship("Task", back_populates="device", cascade="all, delete-orphan")

class AllowedDirectory(Base):
    __tablename__ = "allowed_directories"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    device_id: Mapped[str] = mapped_column(String(64), ForeignKey("devices.id"), nullable=False)
    canonical_path: Mapped[str] = mapped_column(String(512), nullable=False)
    display_name: Mapped[str] = mapped_column(String(128), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc)
    )

    device = relationship("Device", back_populates="allowed_directories")

class Task(Base):
    __tablename__ = "tasks"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, index=True)  # uuid
    device_id: Mapped[str] = mapped_column(String(64), ForeignKey("devices.id"), nullable=False)
    tool: Mapped[str] = mapped_column(String(64), nullable=False)
    arguments_json: Mapped[str] = mapped_column(Text, default="{}")
    permission_level: Mapped[str] = mapped_column(String(16), default="LOW")
    status: Mapped[str] = mapped_column(String(32), default="PENDING")  # PENDING, RUNNING, COMPLETED, FAILED, REJECTED
    requires_confirmation: Mapped[bool] = mapped_column(Boolean, default=False)
    is_confirmed: Mapped[bool] = mapped_column(Boolean, default=False)
    result_json: Mapped[str] = mapped_column(Text, nullable=True)
    error_message: Mapped[str] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc)
    )
    completed_at: Mapped[datetime.datetime] = mapped_column(DateTime, nullable=True)

    device = relationship("Device", back_populates="tasks")

class AuditLog(Base):
    __tablename__ = "audit_logs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    timestamp: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc), index=True
    )
    actor: Mapped[str] = mapped_column(String(128), default="web_client")
    device_id: Mapped[str] = mapped_column(String(64), nullable=True, index=True)
    task_id: Mapped[str] = mapped_column(String(64), nullable=True)
    action: Mapped[str] = mapped_column(String(64), nullable=False)
    tool: Mapped[str] = mapped_column(String(64), nullable=True)
    arguments_safe: Mapped[str] = mapped_column(Text, nullable=True)
    permission_level: Mapped[str] = mapped_column(String(16), default="LOW")
    confirmation_status: Mapped[str] = mapped_column(String(32), default="NOT_REQUIRED")
    execution_status: Mapped[str] = mapped_column(String(32), default="SUCCESS")
    details: Mapped[str] = mapped_column(Text, nullable=True)

class Automation(Base):
    __tablename__ = "automations"

    id: Mapped[str] = mapped_column(String(64), primary_key=True, index=True)
    name: Mapped[str] = mapped_column(String(128), nullable=False)
    description: Mapped[str] = mapped_column(String(256), default="")
    trigger_type: Mapped[str] = mapped_column(String(64), default="Schedule (Daily)")
    schedule_time: Mapped[str] = mapped_column(String(64), default="Everyday at 23:00")
    action_tool: Mapped[str] = mapped_column(String(64), default="lock_pc")
    action_arguments_json: Mapped[str] = mapped_column(Text, default="{}")
    target_device_id: Mapped[str] = mapped_column(String(64), default="")
    status: Mapped[str] = mapped_column(String(32), default="ACTIVE")  # ACTIVE, PAUSED, COMPLETED
    last_run_time: Mapped[str] = mapped_column(String(64), nullable=True)
    next_run_time: Mapped[str] = mapped_column(String(64), nullable=True)
    scheduled_epoch: Mapped[float] = mapped_column(Float, default=0.0)
    is_one_shot: Mapped[bool] = mapped_column(Boolean, default=False)
    execution_count: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc)
    )
