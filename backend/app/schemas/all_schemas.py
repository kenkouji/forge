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

# Workflow Step & Definition Schemas
class WorkflowStepSchema(BaseModel):
    step_id: str
    name: str
    type: str = "action"  # action, condition, delay, approval, notification, completion
    tool: Optional[str] = None
    arguments: Optional[Dict[str, Any]] = None
    condition: Optional[Dict[str, Any]] = None
    on_true_step: Optional[str] = None
    on_false_step: Optional[str] = None
    delay_seconds: Optional[float] = 0.0
    timeout_seconds: Optional[float] = 25.0
    retry_count: Optional[int] = 0
    requires_approval: Optional[bool] = False
    notification_message: Optional[str] = None

class WorkflowDefinitionSchema(BaseModel):
    version: str = "2.0"
    id: Optional[str] = None
    name: str
    description: Optional[str] = ""
    trigger_type: Optional[str] = "manual"  # manual, schedule, interval
    schedule_time: Optional[str] = None
    target_device_id: Optional[str] = None
    max_execution_seconds: Optional[float] = 180.0
    steps: List[WorkflowStepSchema] = []

class WorkflowDryRunStepResult(BaseModel):
    step_id: str
    name: str
    type: str
    tool: Optional[str] = None
    risk_level: str = "LOW"
    requires_approval: bool = False
    simulation_note: str

class WorkflowDryRunResponse(BaseModel):
    valid: bool
    name: str
    steps_count: int
    risk_assessment: str
    requires_human_approval: bool
    steps: List[WorkflowDryRunStepResult] = []
    error: Optional[str] = None

class WorkflowExecutionStepLog(BaseModel):
    step_id: str
    step_name: str
    step_type: str
    tool: Optional[str] = None
    status: str = "SUCCESS"  # SUCCESS, FAILED, SKIPPED, WAITING_APPROVAL
    duration_ms: float = 0.0
    details: Optional[str] = None
    result: Optional[Any] = None

class WorkflowExecutionResponse(BaseModel):
    execution_id: str
    workflow_id: str
    workflow_name: str
    target_device_id: str
    status: str = "COMPLETED"  # RUNNING, COMPLETED, FAILED, CANCELLED, WAITING_APPROVAL
    started_at: str
    completed_at: Optional[str] = None
    current_step_id: Optional[str] = None
    step_logs: List[WorkflowExecutionStepLog] = []
    error_message: Optional[str] = None

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
    workflow_definition_json: Optional[str] = None
    workflow_definition: Optional[Dict[str, Any]] = None
    created_at: datetime.datetime

    model_config = ConfigDict(from_attributes=True)

class AutomationCreateRequest(BaseModel):
    name: str
    description: Optional[str] = ""
    trigger_type: Optional[str] = "Schedule (Daily)"
    schedule_time: Optional[str] = "Everyday at 23:00"
    action_tool: Optional[str] = "lock_pc"
    action_arguments_json: Optional[str] = "{}"
    target_device_id: Optional[str] = ""
    scheduled_epoch: Optional[float] = 0.0
    is_one_shot: Optional[bool] = False
    status: Optional[str] = "ACTIVE"
    workflow_definition_json: Optional[str] = None
    workflow_definition: Optional[WorkflowDefinitionSchema] = None

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
    workflow_definition_json: Optional[str] = None

# Screen Streaming Schemas
class StreamControlRequest(BaseModel):
    action: str = "start"  # start, stop, quality
    width: Optional[int] = 960
    quality: Optional[int] = 50
    fps: Optional[int] = 10

class StreamStatusResponse(BaseModel):
    is_active: bool
    width: int = 960
    height: int = 600
    fps: float = 0.0
    latency_ms: float = 0.0
    kbps: float = 0.0

# Guest Access & Co-Control Schemas
class GuestInvitationCreateRequest(BaseModel):
    device_id: str
    capabilities: List[str] = Field(default_factory=lambda: ["screen.view"])
    expiry_seconds: int = 1800
    allowed_staging_dir: Optional[str] = ""

class GuestInvitationResponse(BaseModel):
    id: str
    device_id: str
    invite_secret: Optional[str] = None
    qr_payload: str
    expires_at: datetime.datetime
    capabilities: List[str]
    is_redeemed: bool
    is_revoked: bool

    model_config = ConfigDict(from_attributes=True)

class GuestRedeemRequest(BaseModel):
    invitation_id: str
    invite_secret: str
    guest_name: str

class GuestRedeemResponse(BaseModel):
    session_id: str
    device_id: str
    guest_token: str
    status: str
    expires_at: datetime.datetime
    capabilities: List[str]

class GuestSessionStatusResponse(BaseModel):
    session_id: str
    id: Optional[str] = None
    device_id: str
    guest_name: str
    status: str
    control_mode: str
    capabilities: List[str]
    approved_capabilities: Optional[List[str]] = None
    expires_at: datetime.datetime
    is_screen_paused: bool = False
    is_input_disabled: bool = False
    is_lockdown: bool = False

class GuestApprovalRequest(BaseModel):
    approve: bool = True
    capabilities: Optional[List[str]] = None
    approved_capabilities: Optional[List[str]] = None
    control_mode: Optional[str] = "shared-control"
    duration_minutes: Optional[int] = 30

class GuestSessionResponse(BaseModel):
    id: str
    device_id: str
    guest_name: str
    status: str
    control_mode: str
    approved_capabilities: List[str]
    expires_at: datetime.datetime
    created_at: datetime.datetime
    is_active: bool

    model_config = ConfigDict(from_attributes=True)

class PrivacyShieldUpdateRequest(BaseModel):
    action: Optional[str] = None
    is_lockdown: Optional[bool] = None
    is_screen_paused: Optional[bool] = None
    is_input_disabled: Optional[bool] = None
    is_transfers_stopped: Optional[bool] = None

class PrivacyShieldResponse(BaseModel):
    device_id: str
    is_lockdown: bool
    is_screen_paused: bool
    is_input_disabled: bool
    is_transfers_stopped: bool
    pending_host_sync: bool
    active_guests_count: int = 0
    updated_at: datetime.datetime

    model_config = ConfigDict(from_attributes=True)

class StagedFileResponse(BaseModel):
    id: str
    session_id: str
    device_id: str
    filename: str
    file_size: int
    sha256_hash: str
    status: str
    uploaded_at: datetime.datetime
    promoted_to_path: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class PromoteFileRequest(BaseModel):
    target_directory: str


# ==========================================
# 1. Virtual Extended Display Schemas
# ==========================================
class VirtualDisplayStartRequest(BaseModel):
    device_id: str
    width: int = 1920
    height: int = 1080
    fps: int = 60
    dpi: int = 240
    color_depth: str = "32bit"
    enable_touch: bool = True

class VirtualDisplayStopRequest(BaseModel):
    device_id: str

class VirtualDisplayStatusResponse(BaseModel):
    device_id: str
    is_active: bool
    width: int
    height: int
    fps: int
    current_fps: float = 0.0
    latency_ms: float = 0.0
    touch_enabled: bool = True
    session_id: Optional[str] = None
    started_at: Optional[datetime.datetime] = None

class VirtualDisplayTouchRequest(BaseModel):
    device_id: str
    action: str  # "down", "up", "move", "scroll", "right_click", "double_click"
    norm_x: float  # 0.0 to 1.0
    norm_y: float  # 0.0 to 1.0
    scroll_dx: Optional[float] = 0.0
    scroll_dy: Optional[float] = 0.0


# ==========================================
# 2. Universal Control Edge Mouse Hopping Schemas
# ==========================================
class EdgeControlConfigRequest(BaseModel):
    device_id: str
    edge: str = "RIGHT"  # "LEFT", "RIGHT", "TOP", "BOTTOM"
    sensitivity: float = 1.0
    dwell_delay_ms: int = 80
    enable_keyboard: bool = True
    enable_clipboard_sync: bool = True

class EdgeControlStatusResponse(BaseModel):
    device_id: str
    is_enabled: bool
    configured_edge: str
    is_remote_active: bool
    cursor_x: float = 0.5
    cursor_y: float = 0.5
    last_handoff_at: Optional[datetime.datetime] = None

class EdgeHandoffEvent(BaseModel):
    device_id: str
    event_type: str  # "ENTER_REMOTE", "EXIT_REMOTE", "CURSOR_MOVE", "KEY_EVENT", "CLICK"
    norm_x: float = 0.5
    norm_y: float = 0.5
    button: Optional[str] = None
    key_code: Optional[str] = None
    is_pressed: Optional[bool] = None


# ==========================================
# 3. P2P Mesh Tunneling Schemas
# ==========================================
class P2pConfigResponse(BaseModel):
    stun_servers: List[str]
    turn_servers: List[Dict[str, str]]
    relay_fallback_enabled: bool = True

class P2pSessionCreateRequest(BaseModel):
    device_id: str
    client_type: str = "android"  # "android", "web", "desktop"

class P2pSessionResponse(BaseModel):
    session_id: str
    device_id: str
    status: str  # "INITIATING", "CONNECTED", "FAILED", "DISCONNECTED"
    signaling_channel_ws: str
    created_at: datetime.datetime

class P2pSignalMessage(BaseModel):
    session_id: str
    sender_type: str  # "client" or "agent"
    signal_type: str  # "offer", "answer", "candidate", "ping", "bye"
    payload_json: str


# ==========================================
# 4. Low-Latency Bi-Directional Audio Schemas
# ==========================================
class AudioSessionStartRequest(BaseModel):
    device_id: str
    mode: str = "sink"  # "sink" (PC->Phone), "source" (Phone->PC Mic), "duplex"
    sample_rate: int = 48000
    channels: int = 2
    bit_depth: int = 16
    codec: str = "pcm"  # "pcm", "opus"

class AudioSessionStopRequest(BaseModel):
    device_id: str

class AudioStatusResponse(BaseModel):
    device_id: str
    is_active: bool
    mode: str
    sample_rate: int
    channels: int
    codec: str
    latency_ms: float = 0.0
    packets_transferred: int = 0


