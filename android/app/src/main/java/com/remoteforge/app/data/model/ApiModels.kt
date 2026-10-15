package com.remoteforge.app.data.model

import com.google.gson.annotations.SerializedName

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    PC_ONLINE,
    PC_OFFLINE,
    RECONNECTING,
    DEGRADED,
    SETUP_REQUIRED,
    ERROR;

    val isConnected: Boolean
        get() = this == CONNECTED || this == PC_ONLINE
}

data class HealthResponse(
    val status: String,
    val server: String,
    val version: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val username: String,
    val role: String
)

data class Device(
    val id: String,
    val name: String,
    @SerializedName("os_name") val osName: String,
    @SerializedName("os_version") val osVersion: String,
    @SerializedName("agent_version") val agentVersion: String,
    @SerializedName("is_online") val isOnline: Boolean,
    @SerializedName("last_seen") val lastSeen: String,
    @SerializedName("created_at") val createdAt: String
)

data class CommandRequest(
    val tool: String,
    val arguments: Map<String, Any> = emptyMap(),
    val confirmed: Boolean = false
)

data class CommandResponse(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("device_id") val deviceId: String,
    val tool: String,
    @SerializedName("permission_level") val permissionLevel: String,
    @SerializedName("requires_confirmation") val requiresConfirmation: Boolean,
    val status: String,
    val result: Map<String, Any>?,
    val error: String?,
    @SerializedName("mac_execution_ms") val macExecutionMs: Double? = null,
    @SerializedName("backend_duration_ms") val backendDurationMs: Double? = null
)

data class LatencyMetrics(
    val networkType: String = "Wi-Fi",
    val netbirdPathType: String = "Relayed (Mumbai)",
    val overlayRttMs: Long? = null,
    val medianCommandMs: Double = 0.0,
    val p95CommandMs: Double = 0.0,
    val lastCommandNetworkMs: Double? = null,
    val lastCommandBackendMs: Double? = null,
    val lastCommandMacMs: Double? = null,
    val lastCommandTotalMs: Double? = null,
    val lastScreenshotDurationMs: Double? = null,
    val lastScreenshotSizeKb: Double? = null,
    val lastHandoverDurationMs: Long? = null,
    val channelType: String = "WebSocket (Persistent)"
)

data class BatteryInfo(
    val percent: Int = 0,
    @SerializedName("power_plugged") val powerPlugged: Boolean = false,
    @SerializedName("secs_left") val secsLeft: Long? = null,
    val status: String = "Discharging"
)

data class ThermalInfo(
    val status: String = "Nominal",
    @SerializedName("cpu_speed_limit") val cpuSpeedLimit: Int = 100
)

data class NetworkThroughputInfo(
    @SerializedName("rx_kbps") val rxKbps: Double = 0.0,
    @SerializedName("tx_kbps") val txKbps: Double = 0.0
)

data class ProcessInfo(
    val pid: Int = 0,
    val name: String = "",
    @SerializedName("cpu_percent") val cpuPercent: Double = 0.0,
    @SerializedName("memory_percent") val memoryPercent: Double = 0.0,
    val status: String = "running"
)

data class SystemStatusResult(
    @SerializedName("os_name") val osName: String,
    @SerializedName("os_version") val osVersion: String,
    @SerializedName("machine_architecture") val machineArch: String,
    val cpu: CpuInfo,
    val ram: RamInfo,
    val disk: DiskInfo,
    val battery: BatteryInfo? = null,
    val thermal: ThermalInfo? = null,
    val network: NetworkThroughputInfo? = null,
    @SerializedName("top_processes") val topProcesses: List<ProcessInfo> = emptyList(),
    @SerializedName("uptime_seconds") val uptimeSeconds: Long,
    @SerializedName("uptime_formatted") val uptimeFormatted: String,
    @SerializedName("agent_version") val agentVersion: String,
    val timestamp: String
)

data class CpuInfo(
    val percent: Double,
    @SerializedName("logical_cores") val logicalCores: Int,
    @SerializedName("physical_cores") val physicalCores: Int
)

data class RamInfo(
    @SerializedName("total_gb") val totalGb: Double,
    @SerializedName("used_gb") val usedGb: Double,
    val percent: Double
)

data class DiskInfo(
    @SerializedName("total_gb") val totalGb: Double,
    @SerializedName("used_gb") val usedGb: Double,
    val percent: Double
)

data class FileItem(
    val name: String,
    val path: String,
    @SerializedName("is_directory") val isDirectory: Boolean = false,
    @SerializedName("is_dir") val isDirAlt: Boolean = false,
    @SerializedName("size_bytes") val sizeBytes: Long = 0L,
    @SerializedName("modified_at") val modifiedAt: String? = null
) {
    val isDir: Boolean
        get() = isDirectory || isDirAlt
}

data class FileListResult(
    @SerializedName("current_path") val currentPath: String? = null,
    @SerializedName("canonical_path") val canonicalPath: String? = null,
    val path: String? = null,
    val items: List<FileItem> = emptyList()
) {
    val resolvedPath: String
        get() = canonicalPath ?: currentPath ?: path ?: ""
}

data class FileListResponse(
    val success: Boolean,
    val result: FileListResult?,
    val error: String?
)

data class FileSearchResult(
    val query: String,
    val count: Int,
    val matches: List<FileItem> = emptyList()
)

data class FileSearchResponse(
    val success: Boolean,
    val result: FileSearchResult?,
    val error: String?
)

data class FilePreviewResponse(
    @SerializedName("preview_supported") val previewSupported: Boolean,
    @SerializedName("preview_type") val previewType: String? = null,
    val filename: String? = null,
    @SerializedName("file_size") val fileSize: Long = 0L,
    val content: String? = null,
    @SerializedName("image_data_base64") val imageDataBase64: String? = null,
    @SerializedName("pdf_data_base64") val pdfDataBase64: String? = null,
    @SerializedName("is_truncated") val isTruncated: Boolean = false,
    val message: String? = null
)

data class FileUploadResult(
    val success: Boolean,
    val filename: String?,
    @SerializedName("bytes_transferred") val bytesTransferred: Long?,
    @SerializedName("target_directory") val targetDirectory: String?,
    val error: String?
)

data class AuditLogEntry(
    val id: Int,
    val timestamp: String,
    val actor: String,
    @SerializedName("device_id") val deviceId: String?,
    val action: String,
    val tool: String?,
    @SerializedName("permission_level") val permissionLevel: String,
    @SerializedName("execution_status") val executionStatus: String,
    val details: String?
)

data class AuditCleanRequest(
    val days: Int? = null
)

data class AuditCleanResponse(
    @SerializedName("deleted_count") val deletedCount: Int,
    val message: String,
    @SerializedName("retention_days") val retentionDays: Int = 14
)

data class AuditDayStat(
    val date: String,
    val total: Int,
    val bugs: Int,
    val success: Int
)

data class AuditStatsResponse(
    @SerializedName("total_logs") val totalLogs: Int,
    @SerializedName("total_bugs") val totalBugs: Int,
    @SerializedName("total_success") val totalSuccess: Int,
    @SerializedName("retention_limit_days") val retentionLimitDays: Int = 14,
    @SerializedName("days_breakdown") val daysBreakdown: List<AuditDayStat>
)

enum class TransferDirection {
    MAC_TO_PHONE,
    PHONE_TO_MAC
}

enum class TransferStatus {
    QUEUED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class TransferRecord(
    val id: String,
    val filename: String,
    val direction: TransferDirection,
    val totalBytes: Long,
    val transferredBytes: Long,
    val speedBps: Long = 0,
    val status: TransferStatus = TransferStatus.IN_PROGRESS,
    val errorMessage: String? = null,
    val remotePath: String? = null,
    val deviceId: String? = null,
    val localPath: String? = null,
    val sha256: String? = null,
    val isPaused: Boolean = false,
    val isResumable: Boolean = true
)

// AI Assistant Models
data class AIChatRequest(
    val message: String,
    @SerializedName("device_id") val deviceId: String? = null,
    val confirmed: Boolean = false,
    @SerializedName("pending_tool") val pendingTool: String? = null,
    @SerializedName("pending_arguments") val pendingArguments: Map<String, Any>? = null
)

data class AIChatResponse(
    val reply: String,
    @SerializedName("tool_called") val toolCalled: String? = null,
    @SerializedName("tool_arguments") val toolArguments: Map<String, Any>? = null,
    @SerializedName("tool_result") val toolResult: Any? = null,
    @SerializedName("requires_confirmation") val requiresConfirmation: Boolean = false,
    @SerializedName("confirmation_prompt") val confirmationPrompt: String? = null,
    val error: String? = null
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val toolCalled: String? = null,
    val toolResultPreview: String? = null,
    val requiresConfirmation: Boolean = false,
    val pendingTool: String? = null,
    val pendingArguments: Map<String, Any>? = null,
    val matchedFiles: List<FileItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

// Task Models
data class TaskItem(
    val id: String = "",
    @SerializedName("device_id") val deviceId: String? = "",
    val tool: String? = "task",
    @SerializedName("arguments_json") val argumentsJson: String? = "{}",
    @SerializedName("permission_level") val permissionLevel: String? = "LOW",
    val status: String = "PENDING", // PENDING, RUNNING, COMPLETED, FAILED, REJECTED
    @SerializedName("requires_confirmation") val requiresConfirmation: Boolean = false,
    @SerializedName("is_confirmed") val isConfirmed: Boolean = false,
    @SerializedName("result_json") val resultJson: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null
)

// Automation Models
data class AutomationItem(
    val id: String,
    val name: String,
    val description: String = "",
    @SerializedName("trigger_type") val triggerType: String = "Schedule (Daily)",
    @SerializedName("schedule_time") val scheduleTime: String = "Everyday at 23:00",
    @SerializedName("action_tool") val actionTool: String = "lock_pc",
    @SerializedName("target_device_id") val targetDeviceId: String = "",
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED
    @SerializedName("last_run_time") val lastRunTime: String? = null,
    @SerializedName("next_run_time") val nextRunTime: String? = null,
    @SerializedName("execution_count") val executionCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("action_arguments_json") val actionArgumentsJson: String? = null,
    @SerializedName("scheduled_epoch") val scheduledEpoch: Double? = null,
    @SerializedName("is_one_shot") val isOneShot: Boolean = false
)

data class AutomationCreateRequest(
    val name: String,
    val description: String = "",
    @SerializedName("trigger_type") val triggerType: String = "Schedule (Daily)",
    @SerializedName("schedule_time") val scheduleTime: String = "Everyday at 23:00",
    @SerializedName("action_tool") val actionTool: String = "lock_pc",
    @SerializedName("target_device_id") val targetDeviceId: String = "",
    val status: String = "ACTIVE",
    @SerializedName("action_arguments_json") val actionArgumentsJson: String? = null,
    @SerializedName("scheduled_epoch") val scheduledEpoch: Double? = null,
    @SerializedName("is_one_shot") val isOneShot: Boolean = false
)

data class AutomationUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    @SerializedName("trigger_type") val triggerType: String? = null,
    @SerializedName("schedule_time") val scheduleTime: String? = null,
    @SerializedName("action_tool") val actionTool: String? = null,
    @SerializedName("target_device_id") val targetDeviceId: String? = null,
    val status: String? = null,
    @SerializedName("action_arguments_json") val actionArgumentsJson: String? = null,
    @SerializedName("scheduled_epoch") val scheduledEpoch: Double? = null,
    @SerializedName("is_one_shot") val isOneShot: Boolean? = null
)

data class AutomationHistoryItem(
    val id: String,
    @SerializedName("automation_id") val automationId: String = "",
    @SerializedName("automation_name") val automationName: String,
    val timestamp: String,
    val status: String,
    val details: String
)
