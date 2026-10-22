package com.remoteforge.app.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.remoteforge.app.data.config.NetworkConfig
import com.remoteforge.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider

import android.util.Log
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.remoteforge.app.data.vpn.VpnLifecycleManager
import com.remoteforge.app.data.service.RemoteForgeConnectionService

class RemoteForgeRepository(val networkConfig: NetworkConfig, private val context: Context) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeTransferJobs = ConcurrentHashMap<String, Job>()
    val vpnManager = VpnLifecycleManager(context)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectionStatusText = MutableStateFlow("Initializing...")
    val connectionStatusText: StateFlow<String> = _connectionStatusText

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice

    private val _telemetry = MutableStateFlow<SystemStatusResult?>(null)
    val telemetry: StateFlow<SystemStatusResult?> = _telemetry

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs

    private val _auditStats = MutableStateFlow<AuditStatsResponse?>(null)
    val auditStats: StateFlow<AuditStatsResponse?> = _auditStats

    private val _transfers = MutableStateFlow<List<TransferRecord>>(emptyList())
    val transfers: StateFlow<List<TransferRecord>> = _transfers

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks

    private val _activeTask = MutableStateFlow<TaskItem?>(null)
    val activeTask: StateFlow<TaskItem?> = _activeTask

    private val _automations = MutableStateFlow<List<AutomationItem>>(emptyList())
    val automations: StateFlow<List<AutomationItem>> = _automations

    private val _automationHistory = MutableStateFlow<List<AutomationHistoryItem>>(emptyList())
    val automationHistory: StateFlow<List<AutomationHistoryItem>> = _automationHistory

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "Hello! I am RemoteForge AI. I can search your user folders (Documents, Downloads, Desktop, Projects), check real-time RAM/CPU telemetry, launch browser searches, or lock your PC. What would you like to do?"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs

    private val _serverVersion = MutableStateFlow<String?>("0.1.0")
    val serverVersion: StateFlow<String?> = _serverVersion

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private val _lastScreenshotBase64 = MutableStateFlow<String?>(null)
    val lastScreenshotBase64: StateFlow<String?> = _lastScreenshotBase64

    fun clearScreenshot() {
        _lastScreenshotBase64.value = null
    }

    val serverUrl: String
        get() = networkConfig.serverUrl

    val authToken: String?
        get() = networkConfig.authToken

    val wsClient = com.remoteforge.app.data.websocket.RemoteForgeWebSocketClient(networkConfig.okHttpClient, repositoryScope)

    private val _latencyMetrics = MutableStateFlow(LatencyMetrics())
    val latencyMetrics: StateFlow<LatencyMetrics> = _latencyMetrics

    private val commandDurations = java.util.Collections.synchronizedList(mutableListOf<Double>())
    private var handoverStartTime: Long? = null

    private var connectJob: Job? = null
    private var reconnectJob: Job? = null

    init {
        wsClient.onConnectionChanged = { connected ->
            Log.i("RemoteForgeRepository", "WebSocket channel status: connected=$connected")
            val current = _latencyMetrics.value
            _latencyMetrics.value = current.copy(
                channelType = if (connected) "WebSocket (Persistent)" else "HTTP/1.1 Pool"
            )
        }
        // Kick off automatic background connection on launch
        triggerAutoConnect()
        setupClipboardSyncChannel()
    }

    private val _clipboardHistory = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardHistory: StateFlow<List<ClipboardItem>> = _clipboardHistory

    val isClipboardAutoSyncEnabled = MutableStateFlow(true)
    private var lastLocalClipboardHash: String? = null

    fun triggerAutoConnect() {
        connectJob?.cancel()
        reconnectJob?.cancel()
        connectJob = repositoryScope.launch {
            autoConnect()
        }
    }

    fun setServerUrl(url: String) {
        networkConfig.serverUrl = url
        triggerAutoConnect()
    }

    fun setAuthToken(token: String?) {
        networkConfig.authToken = token
    }

    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        networkConfig.primaryDeviceId = device.id
        if (device.isOnline) {
            _connectionState.value = ConnectionState.PC_ONLINE
            _connectionStatusText.value = "${device.name}: ONLINE"
            repositoryScope.launch {
                fetchTelemetry(device.id)
            }
        } else {
            _connectionState.value = ConnectionState.PC_OFFLINE
            _connectionStatusText.value = "${device.name}: OFFLINE"
        }
    }

    suspend fun onNetworkChanged(transport: String) = withContext(Dispatchers.IO) {
        handoverStartTime = System.currentTimeMillis()
        Log.i("RemoteForgeRepository", "Network handover to $transport. Re-verifying tunnel & Mac agent...")
        reconnectJob?.cancel()
        connectJob?.cancel()

        if (_connectionState.value == ConnectionState.PC_ONLINE || _connectionState.value == ConnectionState.CONNECTED) {
            _connectionState.value = ConnectionState.RECONNECTING
            _connectionStatusText.value = "Switching to $transport..."
        }

        // Execute full 5-point verification
        autoConnect()

        val handoverDuration = handoverStartTime?.let { System.currentTimeMillis() - it }
        _latencyMetrics.value = _latencyMetrics.value.copy(
            networkType = transport,
            lastHandoverDurationMs = handoverDuration
        )
    }

    suspend fun onNetworkLost() = withContext(Dispatchers.IO) {
        Log.w("RemoteForgeRepository", "All network interfaces lost.")
        reconnectJob?.cancel()
        connectJob?.cancel()
        wsClient.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectionStatusText.value = "No network available"
    }

    fun disconnectSession(disconnectVpn: Boolean = true) {
        repositoryScope.launch {
            reconnectJob?.cancel()
            connectJob?.cancel()
            wsClient.disconnect()
            if (disconnectVpn) {
                vpnManager.disconnectVpn()
            }
            try {
                RemoteForgeConnectionService.stop(context)
            } catch (e: Exception) {
                Log.w("RemoteForgeRepository", "Error stopping service: ${e.message}")
            }
            _connectionState.value = ConnectionState.DISCONNECTED
            _connectionStatusText.value = "Disconnected"
        }
    }

    private fun getTargetHostAndPort(): Pair<String, Int> {
        return try {
            val uri = java.net.URI(networkConfig.serverUrl)
            val host = uri.host ?: VpnLifecycleManager.DEFAULT_MAC_OVERLAY_HOST
            val port = if (uri.port != -1) uri.port else VpnLifecycleManager.DEFAULT_PORT
            Pair(host, port)
        } catch (e: Exception) {
            Pair(VpnLifecycleManager.DEFAULT_MAC_OVERLAY_HOST, VpnLifecycleManager.DEFAULT_PORT)
        }
    }

    suspend fun autoConnect() = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.CONNECTING
        _connectionStatusText.value = "Verifying VPN & Transport..."

        // Step 1: Real Transport & Overlay Verification (RFC 6598 CGNAT 100.64.0.0/10)
        val (targetHost, targetPort) = getTargetHostAndPort()
        val transportResult = vpnManager.verifyTransportHealth(targetHost, targetPort)

        val activeNetType = vpnManager.detectActiveNetworkType()
        _latencyMetrics.value = _latencyMetrics.value.copy(
            networkType = activeNetType,
            overlayRttMs = if (transportResult.isMacReachable) transportResult.latencyMs else null,
            netbirdPathType = "Relayed (Mumbai)"
        )

        if (!transportResult.isInstalled) {
            _connectionState.value = ConnectionState.SETUP_REQUIRED
            _connectionStatusText.value = "NetBird companion app required"
            _lastError.value = transportResult.failureReason
            return@withContext
        }

        if (!transportResult.isOverlayActive) {
            _connectionState.value = ConnectionState.SETUP_REQUIRED
            _connectionStatusText.value = "NetBird VPN is disconnected. Open NetBird to connect."
            _lastError.value = transportResult.failureReason
            return@withContext
        }

        if (!transportResult.isMacReachable) {
            _connectionState.value = ConnectionState.RECONNECTING
            _connectionStatusText.value = "Mac overlay unreachable at $targetHost:$targetPort. Retrying..."
            _lastError.value = transportResult.failureReason
            scheduleAutoReconnect()
            return@withContext
        }

        _connectionStatusText.value = "Connecting to private Mac gateway..."

        // Step 2: Gateway Health Ping (Direct private overlay)
        var (healthOk, healthMsg) = pingHealth()
        if (!healthOk && networkConfig.serverUrl != networkConfig.stableVpnUrl) {
            val prevUrl = networkConfig.serverUrl
            networkConfig.serverUrl = networkConfig.stableVpnUrl
            val (altOk, altMsg) = pingHealth()
            if (altOk) {
                healthOk = true
                healthMsg = altMsg
                Log.i("RemoteForgeRepository", "Switched to stable VPN overlay URL: ${networkConfig.stableVpnUrl}")
            } else {
                networkConfig.serverUrl = prevUrl
            }
        }

        if (!healthOk) {
            _connectionState.value = ConnectionState.RECONNECTING
            _connectionStatusText.value = "Gateway unreachable ($healthMsg). Retrying..."
            _lastError.value = healthMsg
            scheduleAutoReconnect()
            return@withContext
        }

        // Step 3: Authenticate using saved session if needed
        var currentToken = networkConfig.authToken
        if (currentToken == null) {
            _connectionState.value = ConnectionState.AUTHENTICATING
            _connectionStatusText.value = "Authenticating..."
            val u = networkConfig.savedUsername ?: "admin"
            val p = networkConfig.savedPassword ?: "remoteforge_secure_pass"
            val (loginOk, loginErr) = login(u, p)
            if (!loginOk) {
                _connectionState.value = ConnectionState.ERROR
                _connectionStatusText.value = "Auth failed: $loginErr"
                _lastError.value = loginErr
                return@withContext
            }
            currentToken = networkConfig.authToken
        }

        if (currentToken != null) {
            wsClient.connect(networkConfig.serverUrl, currentToken)
        }

        // Step 4: Fetch Registered Devices & Live Mac Agent Verification
        val devList = refreshDevices()
        if (devList.isEmpty()) {
            _connectionState.value = ConnectionState.CONNECTED
            _connectionStatusText.value = "Connected (No Mac agent registered)"
            return@withContext
        }

        val savedId = networkConfig.primaryDeviceId
        val targetPc = devList.firstOrNull { it.id == savedId }
            ?: devList.firstOrNull { it.isOnline }
            ?: devList.first()

        _selectedDevice.value = targetPc
        networkConfig.primaryDeviceId = targetPc.id

        if (targetPc.isOnline) {
            // Step 5: Live authenticated test with the Mac Agent (fetching live telemetry probe)
            _connectionStatusText.value = "Verifying Mac Agent (${targetPc.name})..."
            val telemetryResult = fetchTelemetry(targetPc.id)
            if (telemetryResult != null) {
                _connectionState.value = ConnectionState.PC_ONLINE
                _connectionStatusText.value = "${targetPc.name}: ONLINE"
                _lastError.value = null
            } else {
                _connectionState.value = ConnectionState.DEGRADED
                _connectionStatusText.value = "${targetPc.name}: AGENT UNRESPONSIVE"
                _lastError.value = "Agent did not respond to live telemetry probe"
            }
        } else {
            _connectionState.value = ConnectionState.PC_OFFLINE
            _connectionStatusText.value = "${targetPc.name}: OFFLINE"
        }

        refreshAuditLogs()
        refreshTasks()
        refreshAutomations()
        refreshAutomationHistory()

        // Maintain persistent foreground service and network observation
        try {
            RemoteForgeConnectionService.start(context)
        } catch (e: Exception) {
            Log.w("RemoteForgeRepository", "Could not start connection foreground service: ${e.message}")
        }
    }

    private fun scheduleAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = repositoryScope.launch {
            var delayMs = 2000L
            val maxDelayMs = 15000L
            while (_connectionState.value == ConnectionState.RECONNECTING) {
                val jitter = (Math.random() * 600 - 300).toLong() // +/- 300ms jitter
                val actualDelay = (delayMs + jitter).coerceAtLeast(1000L)
                delay(actualDelay)

                Log.d("RemoteForgeRepository", "Retrying connection after backoff (${actualDelay}ms)...")
                val (targetHost, targetPort) = getTargetHostAndPort()
                val verification = vpnManager.verifyTransportHealth(targetHost, targetPort)
                if (verification.isMacReachable) {
                    autoConnect()
                    break
                }
                delayMs = (delayMs * 1.5).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }

    suspend fun pingHealth(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val service = networkConfig.apiService ?: return@withContext Pair(false, "Service uninitialized")
        val startTime = System.currentTimeMillis()
        try {
            val response = service.getHealth()
            val latency = System.currentTimeMillis() - startTime
            _lastLatencyMs.value = latency
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                _serverVersion.value = b.version
                Pair(true, "Connected in ${latency}ms")
            } else {
                Pair(false, "Server HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Connection error")
        }
    }

    suspend fun login(u: String, p: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val service = networkConfig.apiService ?: return@withContext Pair(false, "Service uninitialized")
        try {
            val res = service.login(LoginRequest(u, p))
            if (res.isSuccessful && res.body() != null) {
                val token = res.body()!!.accessToken
                networkConfig.authToken = token
                networkConfig.savedUsername = u
                networkConfig.savedPassword = p
                wsClient.connect(networkConfig.serverUrl, token)
                refreshDevices()
                refreshAuditLogs()
                Pair(true, null)
            } else {
                val err = res.errorBody()?.string() ?: "Login failed (${res.code()})"
                Pair(false, err)
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Network error")
        }
    }

    fun logout() {
        wsClient.disconnect()
        networkConfig.clearSession()
        _selectedDevice.value = null
        _devices.value = emptyList()
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectionStatusText.value = "Logged Out"
    }

    suspend fun refreshDevices(): List<Device> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getDevices("Bearer $token")
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                _devices.value = list
                val current = _selectedDevice.value
                if (current != null) {
                    val updated = list.firstOrNull { it.id == current.id }
                    if (updated != null) {
                        _selectedDevice.value = updated
                        if (updated.isOnline && _connectionState.value != ConnectionState.PC_ONLINE) {
                            _connectionState.value = ConnectionState.PC_ONLINE
                            _connectionStatusText.value = "${updated.name}: ONLINE"
                        } else if (!updated.isOnline && _connectionState.value == ConnectionState.PC_ONLINE) {
                            _connectionState.value = ConnectionState.PC_OFFLINE
                            _connectionStatusText.value = "${updated.name}: OFFLINE"
                        }
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Executes a command with persistent WebSocket channel as primary and HTTP/1.1 pool as fallback.
     * Records end-to-end phone latency, network RTT, backend processing, and Mac agent execution metrics.
     */
    suspend fun executeCommand(
        deviceId: String,
        tool: String,
        args: Map<String, Any> = emptyMap(),
        confirmed: Boolean = false
    ): CommandResponse = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        var response: CommandResponse? = null
        var usedChannel = "WebSocket"

        // 1. Try persistent WebSocket command channel first
        if (wsClient.isConnected.value) {
            try {
                response = wsClient.sendCommand(deviceId, tool, args, confirmed, timeoutMs = 8000L)
            } catch (e: Exception) {
                Log.w("RemoteForgeRepository", "WebSocket command dispatch failed ($e), falling back to HTTP pool")
                response = null
            }
        }

        // 2. HTTP Fallback with connection pooling
        if (response == null) {
            usedChannel = "HTTP/1.1 Pool"
            val token = networkConfig.authToken ?: throw IllegalStateException("Not authenticated")
            val service = networkConfig.apiService ?: throw IllegalStateException("API service uninitialized")
            val httpRes = service.dispatchCommand(
                authHeader = "Bearer $token",
                deviceId = deviceId,
                request = CommandRequest(tool = tool, arguments = args, confirmed = confirmed)
            )
            if (httpRes.isSuccessful && httpRes.body() != null) {
                response = httpRes.body()!!
            } else {
                val err = httpRes.errorBody()?.string() ?: "HTTP ${httpRes.code()}"
                throw RuntimeException("Command execution failed: $err")
            }
        }

        val totalMs = (System.currentTimeMillis() - t0).toDouble()
        val backendMs = response.backendDurationMs ?: (totalMs * 0.3)
        val macMs = response.macExecutionMs ?: (backendMs * 0.7)
        val networkMs = (totalMs - backendMs).coerceAtLeast(0.0)

        // Maintain rolling 30-item window of command latency
        synchronized(commandDurations) {
            commandDurations.add(totalMs)
            if (commandDurations.size > 30) {
                commandDurations.removeAt(0)
            }
        }

        val sorted = synchronized(commandDurations) { commandDurations.sorted() }
        val median = if (sorted.isNotEmpty()) sorted[sorted.size / 2] else totalMs
        val p95 = if (sorted.isNotEmpty()) sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)] else totalMs

        val currentMetrics = _latencyMetrics.value
        _latencyMetrics.value = currentMetrics.copy(
            channelType = if (wsClient.isConnected.value) "WebSocket (Persistent)" else "HTTP/1.1 Pool",
            lastCommandTotalMs = totalMs,
            lastCommandNetworkMs = networkMs,
            lastCommandBackendMs = backendMs,
            lastCommandMacMs = macMs,
            medianCommandMs = median,
            p95CommandMs = p95
        )

        response
    }

    suspend fun fetchTelemetry(deviceId: String): SystemStatusResult? = withContext(Dispatchers.IO) {
        try {
            val res = executeCommand(deviceId, "get_system_status")
            if (res.result != null) {
                val r = res.result
                val cpuMap = r["cpu"] as? Map<*, *>
                val ramMap = r["ram"] as? Map<*, *>
                val diskMap = r["disk"] as? Map<*, *>

                val status = SystemStatusResult(
                    osName = r["os_name"] as? String ?: "Unknown OS",
                    osVersion = r["os_version"] as? String ?: "",
                    machineArch = r["machine_architecture"] as? String ?: "",
                    cpu = CpuInfo(
                        percent = (cpuMap?.get("percent") as? Number)?.toDouble() ?: 0.0,
                        logicalCores = (cpuMap?.get("logical_cores") as? Number)?.toInt() ?: 4,
                        physicalCores = (cpuMap?.get("physical_cores") as? Number)?.toInt() ?: 4
                    ),
                    ram = RamInfo(
                        totalGb = (ramMap?.get("total_gb") as? Number)?.toDouble() ?: 16.0,
                        usedGb = (ramMap?.get("used_gb") as? Number)?.toDouble() ?: 8.0,
                        percent = (ramMap?.get("percent") as? Number)?.toDouble() ?: 50.0
                    ),
                    disk = DiskInfo(
                        totalGb = (diskMap?.get("total_gb") as? Number)?.toDouble() ?: 512.0,
                        usedGb = (diskMap?.get("used_gb") as? Number)?.toDouble() ?: 256.0,
                        percent = (diskMap?.get("percent") as? Number)?.toDouble() ?: 50.0
                    ),
                    uptimeSeconds = (r["uptime_seconds"] as? Number)?.toLong() ?: 0L,
                    uptimeFormatted = r["uptime_formatted"] as? String ?: "",
                    agentVersion = r["agent_version"] as? String ?: "0.1.0",
                    timestamp = r["timestamp"] as? String ?: ""
                )
                _telemetry.value = status
                status
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun dispatchQuickAction(deviceId: String, tool: String, args: Map<String, Any> = emptyMap()): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        try {
            val safeArgs = if (tool == "take_screenshot" && !args.containsKey("mode")) {
                args + mapOf("mode" to "preview")
            } else {
                args
            }
            val b = executeCommand(deviceId, tool, safeArgs)
            if (b.status == "SUCCESS" || b.status == "COMPLETED") {
                refreshAuditLogs()
                if (tool == "take_screenshot" && b.result != null) {
                    val b64 = b.result["data_base64"] as? String
                    if (!b64.isNullOrBlank()) {
                        _lastScreenshotBase64.value = b64
                        val elapsed = (System.currentTimeMillis() - t0).toDouble()
                        val sizeKb = (b64.length * 3.0 / 4.0) / 1024.0
                        _latencyMetrics.value = _latencyMetrics.value.copy(
                            lastScreenshotDurationMs = elapsed,
                            lastScreenshotSizeKb = sizeKb
                        )
                    }
                }
                Pair(true, "Executed $tool successfully")
            } else {
                Pair(false, b.error ?: "Action execution failed")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Execution error")
        }
    }

    suspend fun listFiles(deviceId: String, path: String? = null): FileListResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.listFiles("Bearer $token", deviceId, path)
            res.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchFiles(deviceId: String, query: String): FileSearchResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.searchFiles("Bearer $token", deviceId, query)
            res.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun previewFile(deviceId: String, path: String): FilePreviewResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.previewFile("Bearer $token", deviceId, path)
            res.body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendInputEvent(
        deviceId: String,
        inputPayload: Map<String, Any>
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val res = executeCommand(deviceId, "send_input_event", inputPayload)
        if (res.status == "SUCCESS" || res.status == "COMPLETED") {
            Pair(true, null)
        } else {
            Pair(false, res.error)
        }
    }

    private fun computeSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun openFile(localPath: String): Boolean {
        return try {
            val file = File(localPath)
            if (!file.exists()) return false
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("RemoteForgeRepository", "Error opening file: ${e.message}")
            false
        }
    }

    suspend fun downloadFile(
        deviceId: String,
        remotePath: String,
        filename: String,
        resume: Boolean = false,
        asZip: Boolean = false,
        existingTransferId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        val transferId = existingTransferId ?: UUID.randomUUID().toString()

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.takeIf { it.exists() || it.mkdirs() }
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val safeFilename = if (asZip && !filename.endsWith(".zip")) "$filename.zip" else filename
        val targetFile = File(downloadsDir, safeFilename)

        val existingBytes = if (resume && targetFile.exists()) targetFile.length() else 0L

        if (existingTransferId == null) {
            val transfer = TransferRecord(
                id = transferId,
                filename = safeFilename,
                direction = TransferDirection.MAC_TO_PHONE,
                totalBytes = 0,
                transferredBytes = existingBytes,
                status = TransferStatus.IN_PROGRESS,
                remotePath = remotePath,
                deviceId = deviceId,
                localPath = targetFile.absolutePath,
                isPaused = false
            )
            _transfers.value = listOf(transfer) + _transfers.value.filter { it.id != transferId }
        } else {
            updateTransferStatus(transferId, TransferStatus.IN_PROGRESS)
        }

        val rangeHeader = if (existingBytes > 0) "bytes=$existingBytes-" else null
        val startTime = System.currentTimeMillis()

        val job = coroutineContext[Job]
        if (job != null) {
            activeTransferJobs[transferId] = job
        }

        try {
            val res = service.downloadFile(
                authHeader = "Bearer $token",
                deviceId = deviceId,
                path = remotePath,
                range = rangeHeader,
                asZip = asZip
            )

            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                val responseLength = body.contentLength()
                val totalLength = if (existingBytes > 0 && responseLength > 0) existingBytes + responseLength else responseLength

                body.byteStream().use { input ->
                    FileOutputStream(targetFile, existingBytes > 0).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        var downloaded = existingBytes

                        while (input.read(buffer).also { read = it } != -1) {
                            if (!coroutineContext.isActive) {
                                break
                            }
                            output.write(buffer, 0, read)
                            downloaded += read
                            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            val speed = ((downloaded - existingBytes) * 1000) / elapsed

                            updateTransferProgress(transferId, downloaded, totalLength, speed)
                        }
                    }
                }

                if (!coroutineContext.isActive) {
                    updateTransferStatus(transferId, TransferStatus.PAUSED)
                    return@withContext false
                }

                try {
                    MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
                } catch (_: Exception) {}

                val fileHash = try {
                    computeSha256(targetFile)
                } catch (_: Exception) {
                    null
                }

                _transfers.value = _transfers.value.map {
                    if (it.id == transferId) it.copy(
                        status = TransferStatus.COMPLETED,
                        localPath = targetFile.absolutePath,
                        sha256 = fileHash
                    )
                    else it
                }

                refreshAuditLogs()
                true
            } else {
                val err = when (res.code()) {
                    400 -> "Selected path is a folder, not a file."
                    403 -> "Access denied: file is outside allowed user folders."
                    404 -> "File not found on target PC."
                    else -> "Download failed (HTTP ${res.code()})"
                }
                updateTransferStatus(transferId, TransferStatus.FAILED, err)
                false
            }
        } catch (ce: CancellationException) {
            updateTransferStatus(transferId, TransferStatus.PAUSED)
            false
        } catch (e: Exception) {
            updateTransferStatus(transferId, TransferStatus.FAILED, e.localizedMessage ?: "Download interrupted")
            false
        } finally {
            activeTransferJobs.remove(transferId)
        }
    }

    fun pauseTransfer(transferId: String) {
        val job = activeTransferJobs.remove(transferId)
        job?.cancel()
        updateTransferStatus(transferId, TransferStatus.PAUSED)
    }

    fun resumeTransfer(transferId: String) {
        val record = _transfers.value.firstOrNull { it.id == transferId } ?: return
        if (record.direction == TransferDirection.MAC_TO_PHONE && record.deviceId != null && record.remotePath != null) {
            repositoryScope.launch {
                downloadFile(
                    deviceId = record.deviceId,
                    remotePath = record.remotePath,
                    filename = record.filename,
                    resume = true,
                    existingTransferId = transferId
                )
            }
        }
    }

    suspend fun uploadFile(deviceId: String, targetDir: String, file: File): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        val transferId = UUID.randomUUID().toString()
        val targetRemotePath = if (targetDir.endsWith("/")) "$targetDir${file.name}" else "$targetDir/${file.name}"

        val transfer = TransferRecord(
            id = transferId,
            filename = file.name,
            direction = TransferDirection.PHONE_TO_MAC,
            totalBytes = file.length(),
            transferredBytes = 0,
            status = TransferStatus.IN_PROGRESS,
            remotePath = targetRemotePath,
            deviceId = deviceId,
            localPath = file.absolutePath
        )
        _transfers.value = listOf(transfer) + _transfers.value

        try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val devIdPart = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
            val targetDirPart = targetDir.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = service.uploadFile("Bearer $token", devIdPart, targetDirPart, filePart)
            if (res.isSuccessful && res.body()?.success == true) {
                val sha = try { computeSha256(file) } catch (_: Exception) { null }
                updateTransferProgress(transferId, file.length(), file.length(), 0)
                _transfers.value = _transfers.value.map {
                    if (it.id == transferId) it.copy(
                        status = TransferStatus.COMPLETED,
                        localPath = file.absolutePath,
                        sha256 = sha
                    ) else it
                }
                refreshAuditLogs()
                true
            } else {
                val err = res.body()?.error ?: "Upload rejected (HTTP ${res.code()})"
                updateTransferStatus(transferId, TransferStatus.FAILED, err)
                false
            }
        } catch (e: Exception) {
            updateTransferStatus(transferId, TransferStatus.FAILED, e.localizedMessage ?: "Upload error")
            false
        }
    }

    fun cancelTransfer(transferId: String) {
        val job = activeTransferJobs.remove(transferId)
        job?.cancel()
        updateTransferStatus(transferId, TransferStatus.CANCELLED)
    }

    private fun updateTransferProgress(id: String, transferred: Long, total: Long, speed: Long) {
        _transfers.value = _transfers.value.map {
            if (it.id == id) it.copy(transferredBytes = transferred, totalBytes = if (total > 0) total else it.totalBytes, speedBps = speed)
            else it
        }
    }

    private fun updateTransferStatus(id: String, status: TransferStatus, error: String? = null, localPath: String? = null) {
        _transfers.value = _transfers.value.map {
            if (it.id == id) it.copy(
                status = status,
                errorMessage = error,
                localPath = localPath ?: it.localPath
            )
            else it
        }
    }

    suspend fun refreshAuditLogs(
        category: String = "all",
        timeframe: String = "14d",
        days: Int? = null
    ): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getAuditLogs(
                authHeader = "Bearer $token",
                category = category,
                timeframe = timeframe,
                days = days,
                limit = 100
            )
            if (res.isSuccessful && res.body() != null) {
                val logs = res.body()!!
                _auditLogs.value = logs
                logs
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun refreshAuditStats(days: Int = 14): AuditStatsResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.getAuditStats("Bearer $token", days = days)
            if (res.isSuccessful && res.body() != null) {
                _auditStats.value = res.body()
                res.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cleanAuditLogs(days: Int? = null): Result<AuditCleanResponse> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
        val service = networkConfig.apiService ?: return@withContext Result.failure(Exception("Service unavailable"))
        try {
            val res = service.cleanAuditLogs("Bearer $token", AuditCleanRequest(days = days))
            if (res.isSuccessful && res.body() != null) {
                refreshAuditLogs()
                refreshAuditStats()
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to clean logs: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Task Management
    suspend fun refreshTasks(): List<TaskItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getTasks("Bearer $token", limit = 50)
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                _tasks.value = list
                _activeTask.value = list.firstOrNull { it.status == "RUNNING" || it.status == "PENDING" }
                list
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun cancelTask(taskId: String): Result<TaskItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        val service = networkConfig.apiService ?: return@withContext Result.failure(IllegalStateException("API service uninitialized"))
        try {
            val res = service.cancelTask("Bearer $token", taskId)
            if (res.isSuccessful && res.body() != null) {
                val updated = res.body()!!
                _tasks.value = _tasks.value.map { if (it.id == taskId) updated else it }
                Result.success(updated)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                Result.failure(RuntimeException(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun retryTask(taskId: String): Result<TaskItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        val service = networkConfig.apiService ?: return@withContext Result.failure(IllegalStateException("API service uninitialized"))
        try {
            val res = service.retryTask("Bearer $token", taskId)
            if (res.isSuccessful && res.body() != null) {
                val newTask = res.body()!!
                _tasks.value = listOf(newTask) + _tasks.value
                Result.success(newTask)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                Result.failure(RuntimeException(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cleanupTasks(keepCount: Int = 50): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        val service = networkConfig.apiService ?: return@withContext Result.failure(IllegalStateException("API service uninitialized"))
        try {
            val res = service.cleanupTasks("Bearer $token", keepCount)
            if (res.isSuccessful && res.body() != null) {
                refreshTasks()
                Result.success(res.body()!!)
            } else {
                val err = res.errorBody()?.string() ?: "HTTP ${res.code()}"
                Result.failure(RuntimeException(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Developer Workstation Operations
    suspend fun getGitStatus(deviceId: String, projectPath: String? = null): Result<GitStatusInfo> = withContext(Dispatchers.IO) {
        try {
            val args = mutableMapOf<String, Any>()
            if (!projectPath.isNullOrBlank()) {
                args["path"] = projectPath
            }
            val res = executeCommand(deviceId = deviceId, tool = "git_status", args = args)
            if (res.status == "COMPLETED" && res.result != null) {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(res.result)
                val status = gson.fromJson(json, GitStatusInfo::class.java)
                Result.success(status)
            } else {
                Result.failure(RuntimeException(res.error ?: "Failed to retrieve Git status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGitDiff(
        deviceId: String,
        projectPath: String? = null,
        filePath: String? = null,
        staged: Boolean = false
    ): Result<GitDiffInfo> = withContext(Dispatchers.IO) {
        try {
            val args = mutableMapOf<String, Any>("staged" to staged)
            if (!projectPath.isNullOrBlank()) args["path"] = projectPath
            if (!filePath.isNullOrBlank()) args["file_path"] = filePath

            val res = executeCommand(deviceId = deviceId, tool = "git_diff", args = args)
            if (res.status == "COMPLETED" && res.result != null) {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(res.result)
                val diffInfo = gson.fromJson(json, GitDiffInfo::class.java)
                Result.success(diffInfo)
            } else {
                Result.failure(RuntimeException(res.error ?: "Failed to retrieve Git diff"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buildProject(
        deviceId: String,
        projectPath: String? = null,
        taskType: String = "build",
        customCmd: String? = null,
        timeoutSec: Int = 60
    ): Result<ProjectBuildResult> = withContext(Dispatchers.IO) {
        try {
            val args = mutableMapOf<String, Any>(
                "task_type" to taskType,
                "timeout_seconds" to timeoutSec
            )
            if (!projectPath.isNullOrBlank()) args["path"] = projectPath
            if (!customCmd.isNullOrBlank()) args["command"] = customCmd

            val res = executeCommand(deviceId = deviceId, tool = "build_project", args = args)
            if (res.status == "COMPLETED" && res.result != null) {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(res.result)
                val buildResult = gson.fromJson(json, ProjectBuildResult::class.java)
                Result.success(buildResult)
            } else {
                Result.failure(RuntimeException(res.error ?: "Build task failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMacClipboard(deviceId: String): Result<ClipboardInfo> = withContext(Dispatchers.IO) {
        try {
            val res = executeCommand(deviceId = deviceId, tool = "get_clipboard", args = emptyMap())
            if (res.status == "COMPLETED" && res.result != null) {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(res.result)
                val clip = gson.fromJson(json, ClipboardInfo::class.java)
                Result.success(clip)
            } else {
                Result.failure(RuntimeException(res.error ?: "Failed to read clipboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setMacClipboard(deviceId: String, text: String): Result<ClipboardInfo> = withContext(Dispatchers.IO) {
        try {
            val res = executeCommand(deviceId = deviceId, tool = "set_clipboard", args = mapOf("text" to text))
            if (res.status == "COMPLETED" && res.result != null) {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(res.result)
                val clip = gson.fromJson(json, ClipboardInfo::class.java)
                Result.success(clip)
            } else {
                Result.failure(RuntimeException(res.error ?: "Failed to set clipboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Universal Clipboard Operations
    private fun setupClipboardSyncChannel() {
        val originalHandler = wsClient.onEventReceived
        wsClient.onEventReceived = { type, payload ->
            originalHandler?.invoke(type, payload)
            if (type == "clipboard_sync" && payload != null) {
                handleIncomingClipboardSync(payload)
            }
        }

        // Listen for phone clipboard copies
        try {
            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipMgr?.addPrimaryClipChangedListener {
                if (!isClipboardAutoSyncEnabled.value) return@addPrimaryClipChangedListener
                val clip = clipMgr.primaryClip ?: return@addPrimaryClipChangedListener
                if (clip.itemCount > 0) {
                    val item = clip.getItemAt(0)
                    val text = item.text?.toString() ?: item.coerceToText(context)?.toString()
                    if (!text.isNullOrBlank()) {
                        val hash = text.hashCode().toString()
                        if (hash != lastLocalClipboardHash) {
                            lastLocalClipboardHash = hash
                            repositoryScope.launch {
                                syncLocalClipboard("text", text = text)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("RemoteForgeRepository", "Could not attach phone clipboard listener: ${e.message}")
        }
    }

    private fun handleIncomingClipboardSync(payload: Map<String, Any>) {
        try {
            val rawItem = payload["item"] as? Map<String, Any> ?: return
            val gson = com.google.gson.Gson()
            val json = gson.toJson(rawItem)
            val item = gson.fromJson(json, ClipboardItem::class.java)

            // Deduplicate and update history
            val current = _clipboardHistory.value.toMutableList()
            current.removeAll { it.id == item.id }
            current.add(0, item)
            _clipboardHistory.value = current

            // If auto-sync is enabled and item came from laptop/mac, copy to phone's clipboard
            if (isClipboardAutoSyncEnabled.value && item.sourceDevice != "phone") {
                if (item.contentType == "text" && !item.textContent.isNullOrBlank()) {
                    lastLocalClipboardHash = item.textContent.hashCode().toString()
                    Handler(Looper.getMainLooper()).post {
                        try {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("RemoteForge Sync", item.textContent)
                            clipMgr?.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied from Mac to Phone", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.w("RemoteForgeRepository", "Failed setting phone clipboard: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RemoteForgeRepository", "Error handling clipboard_sync: ${e.message}")
        }
    }

    suspend fun refreshClipboardHistory(type: String? = null): List<ClipboardItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getClipboardHistory("Bearer $token", limit = 50, type = type)
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                _clipboardHistory.value = list
                list
            } else {
                _clipboardHistory.value
            }
        } catch (e: Exception) {
            Log.e("RemoteForgeRepository", "Error fetching clipboard history: ${e.message}")
            _clipboardHistory.value
        }
    }

    suspend fun syncLocalClipboard(
        contentType: String,
        text: String? = null,
        imageBase64: String? = null,
        preview: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val req = ClipboardSyncRequest(
                contentType = contentType,
                text = text,
                imageBase64 = imageBase64,
                sourceDevice = "phone",
                preview = preview
            )
            val res = service.syncClipboardItem("Bearer $token", req)
            if (res.isSuccessful) {
                refreshClipboardHistory()
                true
            } else false
        } catch (e: Exception) {
            Log.e("RemoteForgeRepository", "Failed to sync local clipboard: ${e.message}")
            false
        }
    }

    suspend fun copyClipboardToDevice(
        deviceId: String,
        contentType: String,
        text: String? = null,
        imageBase64: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val req = ClipboardCopyRequest(
                deviceId = deviceId,
                contentType = contentType,
                text = text,
                imageBase64 = imageBase64
            )
            val res = service.copyClipboardToDevice("Bearer $token", req)
            if (res.isSuccessful) {
                refreshClipboardHistory()
                true
            } else false
        } catch (e: Exception) {
            Log.e("RemoteForgeRepository", "Failed to copy to device: ${e.message}")
            false
        }
    }

    suspend fun clearClipboardHistory(): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.clearClipboardHistory("Bearer $token")
            if (res.isSuccessful) {
                _clipboardHistory.value = emptyList()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteClipboardItem(itemId: String): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.deleteClipboardItem("Bearer $token", itemId)
            if (res.isSuccessful) {
                _clipboardHistory.value = _clipboardHistory.value.filter { it.id != itemId }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun copyToPhoneClipboard(item: ClipboardItem) {
        if (item.contentType == "text" && !item.textContent.isNullOrBlank()) {
            lastLocalClipboardHash = item.textContent.hashCode().toString()
            Handler(Looper.getMainLooper()).post {
                try {
                    val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("RemoteForge", item.textContent)
                    clipMgr?.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to phone clipboard", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.w("RemoteForgeRepository", "Failed setting phone clipboard: ${e.message}")
                }
            }
        } else if (item.contentType == "image" && !item.imageBase64.isNullOrBlank()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Image item saved in history", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Automations Management
    suspend fun refreshAutomations(): List<AutomationItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getAutomations("Bearer $token")
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                _automations.value = list
                list
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun refreshAutomationHistory(): List<AutomationHistoryItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getAutomationHistory("Bearer $token")
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!
                _automationHistory.value = list
                list
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleAutomation(id: String, active: Boolean): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        val newStatus = if (active) "ACTIVE" else "PAUSED"
        try {
            val res = service.updateAutomation("Bearer $token", id, AutomationUpdateRequest(status = newStatus))
            if (res.isSuccessful && res.body() != null) {
                refreshAutomations()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun discardAutomation(id: String): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.discardAutomation("Bearer $token", id)
            if (res.isSuccessful) {
                refreshAutomations()
                refreshAutomationHistory()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun runAutomationNow(id: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext Pair(false, "Not authenticated")
        val service = networkConfig.apiService ?: return@withContext Pair(false, "Service uninitialized")
        try {
            val res = service.runAutomationNow("Bearer $token", id)
            if (res.isSuccessful) {
                refreshAutomations()
                refreshAutomationHistory()
                refreshAuditLogs()
                Pair(true, "Automation executed successfully")
            } else {
                Pair(false, "Execution failed (${res.code()})")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Network error")
        }
    }

    suspend fun createAutomation(
        name: String,
        tool: String,
        scheduleTime: String,
        description: String,
        deviceId: String = "",
        actionArgumentsJson: String? = null,
        scheduledEpoch: Double? = null,
        isOneShot: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val targetDev = if (deviceId.isNotBlank()) deviceId else _selectedDevice.value?.id ?: ""
            val req = AutomationCreateRequest(
                name = name,
                description = description,
                scheduleTime = scheduleTime,
                actionTool = tool,
                targetDeviceId = targetDev,
                actionArgumentsJson = actionArgumentsJson,
                scheduledEpoch = scheduledEpoch,
                isOneShot = isOneShot
            )
            val res = service.createAutomation("Bearer $token", req)
            if (res.isSuccessful) {
                refreshAutomations()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun scheduleTask(
        name: String,
        tool: String,
        actionArguments: Map<String, Any>,
        delayMinutes: Int,
        description: String = "",
        deviceId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val delaySec = delayMinutes * 60
        val epoch = (System.currentTimeMillis() / 1000.0) + delaySec
        val timingLabel = if (delayMinutes < 60) "In $delayMinutes mins" else "In ${delayMinutes / 60} hour(s)"
        val jsonArgs = com.google.gson.Gson().toJson(actionArguments)
        createAutomation(
            name = name,
            tool = tool,
            scheduleTime = timingLabel,
            description = if (description.isNotBlank()) description else "Execute $name $timingLabel",
            deviceId = deviceId,
            actionArgumentsJson = jsonArgs,
            scheduledEpoch = epoch,
            isOneShot = true
        )
    }

    // AI Assistant Messaging
    suspend fun sendAIMessage(
        text: String,
        confirmed: Boolean = false,
        pendingTool: String? = null,
        pendingArgs: Map<String, Any>? = null
    ) = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken
        val service = networkConfig.apiService
        val dev = _selectedDevice.value

        val userMsgId = UUID.randomUUID().toString()
        _chatMessages.value = _chatMessages.value + ChatMessage(
            id = userMsgId,
            isUser = true,
            text = text
        )

        if (token == null || service == null) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "Please authenticate in Settings first before chatting with RemoteForge AI."
            )
            return@withContext
        }

        _isAILoading.value = true
        try {
            val req = AIChatRequest(
                message = text,
                deviceId = dev?.id,
                confirmed = confirmed,
                pendingTool = pendingTool,
                pendingArguments = pendingArgs
            )

            val res = service.chatWithAI("Bearer $token", req)
            if (res.isSuccessful && res.body() != null) {
                val b = res.body()!!

                var files: List<FileItem> = emptyList()
                if (b.toolCalled == "search_files" && b.toolResult is Map<*, *>) {
                    val rawMatches = (b.toolResult as Map<*, *>)["matches"] as? List<*>
                    if (rawMatches != null) {
                        files = rawMatches.filterIsInstance<Map<*, *>>().map { m ->
                            FileItem(
                                name = m["name"] as? String ?: "File",
                                path = m["path"] as? String ?: "",
                                isDirectory = (m["is_directory"] as? Boolean) ?: false,
                                sizeBytes = ((m["size_bytes"] as? Number)?.toLong()) ?: 0L
                            )
                        }
                    }
                }

                _chatMessages.value = _chatMessages.value + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = b.reply,
                    toolCalled = b.toolCalled,
                    requiresConfirmation = b.requiresConfirmation,
                    pendingTool = if (b.requiresConfirmation) b.toolCalled else null,
                    pendingArguments = if (b.requiresConfirmation) b.toolArguments else null,
                    matchedFiles = files
                )
                refreshAuditLogs()
            } else {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "AI error: ${res.errorBody()?.string() ?: "Server error (${res.code()})"}"
                )
            }
        } catch (e: Exception) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "Network error communicating with AI: ${e.localizedMessage ?: "Unknown"}"
            )
        } finally {
            _isAILoading.value = false
        }
    }

    // Workflow Engine & Templates
    suspend fun getWorkflowTemplates(): List<WorkflowDefinitionItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getWorkflowTemplates("Bearer $token")
            if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun dryRunWorkflow(workflow: Map<String, Any>): WorkflowDryRunResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.dryRunWorkflow("Bearer $token", workflow)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun dryRunSavedAutomation(id: String): WorkflowDryRunResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.dryRunSavedAutomation("Bearer $token", id)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cancelWorkflowExecution(id: String): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.cancelWorkflowExecution("Bearer $token", id)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWorkflowExecutions(): List<WorkflowExecutionItem> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.getWorkflowExecutions("Bearer $token")
            if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createCustomWorkflow(
        name: String,
        description: String,
        triggerType: String,
        scheduleTime: String,
        deviceId: String,
        steps: List<WorkflowStepItem>
    ): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val targetDev = if (deviceId.isNotBlank()) deviceId else _selectedDevice.value?.id ?: ""
            val wfDef = WorkflowDefinitionItem(
                name = name,
                description = description,
                triggerType = triggerType,
                scheduleTime = scheduleTime,
                targetDeviceId = targetDev,
                steps = steps
            )
            val wfJson = com.google.gson.Gson().toJson(wfDef)
            val req = AutomationCreateRequest(
                name = name,
                description = description,
                scheduleTime = scheduleTime,
                triggerType = triggerType,
                actionTool = steps.firstOrNull { it.type == "action" }?.tool ?: "lock_pc",
                targetDeviceId = targetDev,
                workflowDefinitionJson = wfJson
            )
            val res = service.createAutomation("Bearer $token", req)
            if (res.isSuccessful) {
                refreshAutomations()
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // Screen Streaming
    fun getStreamUrl(deviceId: String): String {
        val base = networkConfig.serverUrl.removeSuffix("/")
        val token = networkConfig.authToken ?: ""
        return "$base/api/stream/live/$deviceId?token=$token"
    }

    suspend fun controlScreenStream(deviceId: String, action: String, width: Int? = null, quality: Int? = null, fps: Int? = null): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.controlScreenStream("Bearer $token", deviceId, StreamControlRequest(action, width, quality, fps))
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getScreenStreamStatus(deviceId: String): StreamStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.getScreenStreamStatus("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    // --- Guest Access, Co-Control & Privacy Shield ---
    suspend fun createGuestInvitation(deviceId: String, capabilities: List<String>, expiresInMinutes: Int = 15): GuestInvitationResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.createGuestInvitation("Bearer $token", GuestInvitationCreateRequest(deviceId, capabilities, expiresInMinutes))
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun listGuestInvitations(deviceId: String): List<GuestInvitationResponse> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.listGuestInvitations("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun revokeGuestInvitation(invitationId: String): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.revokeGuestInvitation("Bearer $token", invitationId)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun listGuestSessions(deviceId: String): List<GuestSessionResponse> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.listGuestSessions("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun approveGuestSession(sessionId: String, capabilities: List<String>, controlMode: String = "shared-control", durationMinutes: Int = 30): GuestSessionResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.approveGuestSession(
                "Bearer $token",
                sessionId,
                GuestApprovalRequest(
                    approve = true,
                    approvedCapabilities = capabilities,
                    capabilities = capabilities,
                    controlMode = controlMode,
                    durationMinutes = durationMinutes
                )
            )
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun revokeGuestSession(sessionId: String, reason: String = "Revoked by owner"): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.revokeGuestSession("Bearer $token", sessionId, reason)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPrivacyShield(deviceId: String): PrivacyShieldResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.getPrivacyShield("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updatePrivacyShield(deviceId: String, update: PrivacyShieldUpdateRequest): PrivacyShieldResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.updatePrivacyShield("Bearer $token", deviceId, update)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun listStagedFiles(deviceId: String? = null): List<StagedFileResponse> = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext emptyList()
        val service = networkConfig.apiService ?: return@withContext emptyList()
        try {
            val res = service.listStagedFiles("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun promoteStagedFile(fileId: String, destinationDir: String): StagedFileResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.promoteStagedFile("Bearer $token", fileId, PromoteFileRequest(destinationDir))
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteStagedFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.deleteStagedFile("Bearer $token", fileId)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // Virtual Extended Display Repository APIs
    // ==========================================
    suspend fun startVirtualDisplay(
        deviceId: String,
        width: Int = 1920,
        height: Int = 1080,
        fps: Int = 60,
        dpi: Int = 240,
        enableTouch: Boolean = true
    ): VirtualDisplayStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.startVirtualDisplay(
                "Bearer $token",
                VirtualDisplayStartRequest(deviceId, width, height, fps, dpi, "32bit", enableTouch)
            )
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun stopVirtualDisplay(deviceId: String): VirtualDisplayStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.stopVirtualDisplay("Bearer $token", VirtualDisplayStopRequest(deviceId))
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getVirtualDisplayStatus(deviceId: String): VirtualDisplayStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.getVirtualDisplayStatus("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun injectVirtualDisplayTouch(
        deviceId: String,
        action: String,
        normX: Float,
        normY: Float,
        scrollDx: Float = 0f,
        scrollDy: Float = 0f
    ): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.injectVirtualDisplayTouch(
                "Bearer $token",
                VirtualDisplayTouchRequest(deviceId, action, normX, normY, scrollDx, scrollDy)
            )
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // Universal Control Edge Hopping Repository APIs
    // ==========================================
    suspend fun configureEdgeControl(
        deviceId: String,
        edge: String = "RIGHT",
        sensitivity: Float = 1.0f,
        dwellDelayMs: Int = 80,
        enableKeyboard: Boolean = true,
        enableClipboardSync: Boolean = true
    ): EdgeControlStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.configureEdgeControl(
                "Bearer $token",
                EdgeControlConfigRequest(deviceId, edge, sensitivity, dwellDelayMs, enableKeyboard, enableClipboardSync)
            )
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun disableEdgeControl(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.disableEdgeControl("Bearer $token", deviceId)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getEdgeControlStatus(deviceId: String): EdgeControlStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.getEdgeControlStatus("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendEdgeHandoff(
        deviceId: String,
        eventType: String,
        normX: Float,
        normY: Float,
        button: String? = null,
        keyCode: String? = null,
        isPressed: Boolean? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.sendEdgeHandoff(
                "Bearer $token",
                EdgeHandoffEvent(deviceId, eventType, normX, normY, button, keyCode, isPressed)
            )
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // Zero-Config P2P Mesh Repository APIs
    // ==========================================
    suspend fun getP2pConfig(): P2pConfigResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.getP2pConfig("Bearer $token")
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createP2pSession(deviceId: String): P2pSessionResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.createP2pSession("Bearer $token", P2pSessionCreateRequest(deviceId))
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun postP2pSignal(sessionId: String, senderType: String, signalType: String, payloadJson: String): Boolean = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext false
        val service = networkConfig.apiService ?: return@withContext false
        try {
            val res = service.postP2pSignal(
                "Bearer $token",
                sessionId,
                P2pSignalMessage(sessionId, senderType, signalType, payloadJson)
            )
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // Bi-Directional Audio Repository APIs
    // ==========================================
    suspend fun startAudioSession(
        deviceId: String,
        mode: String = "sink",
        sampleRate: Int = 48000,
        channels: Int = 2,
        bitDepth: Int = 16,
        codec: String = "pcm"
    ): AudioStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.startAudioSession(
                "Bearer $token",
                AudioSessionStartRequest(deviceId, mode, sampleRate, channels, bitDepth, codec)
            )
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun stopAudioSession(deviceId: String): AudioStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.stopAudioSession("Bearer $token", AudioSessionStopRequest(deviceId))
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAudioStatus(deviceId: String): AudioStatusResponse? = withContext(Dispatchers.IO) {
        val token = networkConfig.authToken ?: return@withContext null
        val service = networkConfig.apiService ?: return@withContext null
        try {
            val res = service.getAudioStatus("Bearer $token", deviceId)
            if (res.isSuccessful) res.body() else null
        } catch (e: Exception) {
            null
        }
    }
}
