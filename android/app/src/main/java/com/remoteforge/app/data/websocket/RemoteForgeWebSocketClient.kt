package com.remoteforge.app.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.remoteforge.app.data.model.CommandResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class RemoteForgeWebSocketClient(
    private val client: OkHttpClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val isConnecting = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private var lastWsUrl: String? = null
    private var lastToken: String? = null

    // Pending commands waiting for command_result by requestId
    private val pendingCommands = ConcurrentHashMap<String, CompletableDeferred<CommandResponse>>()

    // Callback listeners for non-command broadcast events (e.g. telemetry, tasks)
    var onEventReceived: ((String, Map<String, Any>?) -> Unit)? = null
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    fun connect(serverBaseUrl: String, token: String) {
        val wsUrl = if (serverBaseUrl.startsWith("https://")) {
            serverBaseUrl.replace("https://", "wss://") + "/ws/client"
        } else {
            serverBaseUrl.replace("http://", "ws://") + "/ws/client"
        }

        lastWsUrl = wsUrl
        lastToken = token

        if (_isConnected.value || isConnecting.get()) return

        doConnect(wsUrl, token)
    }

    private fun doConnect(wsUrl: String, token: String) {
        if (!isConnecting.compareAndSet(false, true)) return

        val endpoint = if (wsUrl.contains("?")) "$wsUrl&token=$token" else "$wsUrl?token=$token"
        val request = Request.Builder().url(endpoint).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting.set(false)
                _isConnected.value = true
                Log.i("RemoteForgeWS", "Persistent Command Channel CONNECTED: $wsUrl")
                onConnectionChanged?.invoke(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("RemoteForgeWS", "WebSocket closing ($code): $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("RemoteForgeWS", "WebSocket failure: ${t.message}")
                handleDisconnect()
                scheduleReconnect()
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = gson.fromJson(text, Map::class.java) ?: return
            val msgType = json["type"] as? String

            when (msgType) {
                "command_result" -> {
                    val reqId = json["request_id"] as? String
                    if (reqId != null) {
                        val deferred = pendingCommands.remove(reqId)
                        if (deferred != null && deferred.isActive) {
                            val response = gson.fromJson(text, CommandResponse::class.java)
                            deferred.complete(response)
                        }
                    }
                }
                "pong" -> {
                    // Pong received
                }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    onEventReceived?.invoke(msgType ?: "unknown", json as? Map<String, Any>)
                }
            }
        } catch (e: Exception) {
            Log.e("RemoteForgeWS", "Error parsing WebSocket message: ${e.message}")
        }
    }

    suspend fun sendCommand(
        deviceId: String,
        tool: String,
        arguments: Map<String, Any> = emptyMap(),
        confirmed: Boolean = false,
        timeoutMs: Long = 10000L
    ): CommandResponse = withContext(Dispatchers.IO) {
        val ws = webSocket
        if (ws == null || !_isConnected.value) {
            throw IllegalStateException("Persistent WebSocket channel not connected")
        }

        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<CommandResponse>()
        pendingCommands[requestId] = deferred

        val msg = mapOf(
            "type" to "command",
            "request_id" to requestId,
            "device_id" to deviceId,
            "tool" to tool,
            "arguments" to arguments,
            "confirmed" to confirmed
        )

        val jsonStr = gson.toJson(msg)
        val sent = ws.send(jsonStr)
        if (!sent) {
            pendingCommands.remove(requestId)
            throw IllegalStateException("Failed to enqueue WebSocket frame")
        }

        try {
            withTimeout(timeoutMs) {
                deferred.await()
            }
        } catch (e: Exception) {
            pendingCommands.remove(requestId)
            throw e
        }
    }

    private fun handleDisconnect() {
        isConnecting.set(false)
        _isConnected.value = false
        onConnectionChanged?.invoke(false)

        // Fail active pending commands so caller can fall back to HTTP immediately
        val iterator = pendingCommands.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.value.completeExceptionally(IllegalStateException("WebSocket disconnected while awaiting result"))
            iterator.remove()
        }
    }

    private fun scheduleReconnect() {
        val url = lastWsUrl ?: return
        val token = lastToken ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var delayMs = 1500L
            val maxDelayMs = 12000L
            while (!_isConnected.value) {
                val jitter = (Math.random() * 500 - 250).toLong()
                val actualDelay = (delayMs + jitter).coerceAtLeast(1000L)
                delay(actualDelay)

                Log.d("RemoteForgeWS", "Attempting WebSocket reconnection in ${actualDelay}ms...")
                doConnect(url, token)
                delayMs = (delayMs * 1.5).toLong().coerceAtMost(maxDelayMs)
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        isConnecting.set(false)
        _isConnected.value = false
        webSocket?.close(1000, "App closed")
        webSocket = null
        handleDisconnect()
    }
}
