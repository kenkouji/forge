from typing import Dict, Any, Optional, List
import datetime
from pydantic import BaseModel, Field, ConfigDict

# Auth Schemas
class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    username: str
    role: str

class TokenData(BaseModel):
    username: Optional[str] = None
    role: Optional[str] = None

class LoginRequest(BaseModel):
    username: str
    password: str

class UserResponse(BaseModel):
    id: int
    username: str
    role: str
    is_active: bool
    created_at: datetime.datetime

    model_config = ConfigDict(from_attributes=True)

# Device Schemas
class DeviceResponse(BaseModel):
    id: str
    name: str
    os_name: str
    os_version: str
    agent_version: str
    is_online: bool
    last_seen: datetime.datetime
    created_at: datetime.datetime

    model_config = ConfigDict(from_attributes=True)

class AllowedDirectoryResponse(BaseModel):
    id: int
    device_id: str
    canonical_path: str
    display_name: str
    is_active: bool

    model_config = ConfigDict(from_attributes=True)

class AddDirectoryRequest(BaseModel):
    canonical_path: str
    display_name: str

# Command & WebSocket Schemas
class CommandRequest(BaseModel):
    tool: str
    arguments: Dict[str, Any] = Field(default_factory=dict)
    confirmed: bool = False

class CommandResponse(BaseModel):
    request_id: str
    device_id: str
    tool: str
    permission_level: str
    requires_confirmation: bool
    status: str
    result: Optional[Dict[str, Any]] = None
    error: Optional[str] = None
    mac_execution_ms: Optional[float] = None
    backend_duration_ms: Optional[float] = None

# Task Schemas
class TaskResponse(BaseModel):
    id: str
    device_id: str
    tool: str
    permission_level: str
    status: str
    requires_confirmation: bool
    is_confirmed: bool
    result_json: Optional[str] = None
    error_message: Optional[str] = None
    created_at: datetime.datetime
    completed_at: Optional[datetime.datetime] = None

    model_config = ConfigDict(from_attributes=True)

class TaskConfirmRequest(BaseModel):
    confirmed: bool

# Audit Schemas
class AuditLogResponse(BaseModel):
    id: int
    timestamp: datetime.datetime
    actor: str
    device_id: Optional[str] = None
    task_id: Optional[str] = None
    action: str
    tool: Optional[str] = None
    arguments_safe: Optional[str] = None
    permission_level: str
    confirmation_status: str
    execution_status: str
    details: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class AuditCleanRequest(BaseModel):
    days: Optional[int] = None  # None or 0 to clear all logs, or integer for older than N days (max 14)

class AuditCleanResponse(BaseModel):
    deleted_count: int
    message: str
    retention_days: int = 14

class AuditDayStat(BaseModel):
    date: str
    total: int
    bugs: int
    success: int

class AuditStatsResponse(BaseModel):
    total_logs: int
    total_bugs: int
    total_success: int
    retention_limit_days: int = 14
    days_breakdown: List[AuditDayStat]

# Automation Schemas
class AutomationResponse(BaseModel):
    id: str
    name: str
    description: str
    trigger_type: str
    schedule_time: str
    action_tool: str
    action_arguments_json: Optional[str] = "{}"
    target_device_id: str
    status: str
    last_run_time: Optional[str] = None
    next_run_time: Optional[str] = None
    scheduled_epoch: Optional[float] = 0.0
    is_one_shot: Optional[bool] = False
    execution_count: int = 0
    created_at: datetime.datetime

    model_config = ConfigDict(from_attributes=True)

class AutomationCreateRequest(BaseModel):
    name: str
    description: Optional[str] = ""
    trigger_type: Optional[str] = "Schedule (Daily)"
    schedule_time: Optional[str] = "Everyday at 23:00"
    action_tool: str
    action_arguments_json: Optional[str] = "{}"
    target_device_id: Optional[str] = ""
    scheduled_epoch: Optional[float] = 0.0
    is_one_shot: Optional[bool] = False
    status: Optional[str] = "ACTIVE"

class AutomationUpdateRequest(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    trigger_type: Optional[str] = None
    schedule_time: Optional[str] = None
    action_tool: Optional[str] = None
    action_arguments_json: Optional[str] = None
    target_device_id: Optional[str] = None
    scheduled_epoch: Optional[float] = None
    is_one_shot: Optional[bool] = None
    status: Optional[str] = None
