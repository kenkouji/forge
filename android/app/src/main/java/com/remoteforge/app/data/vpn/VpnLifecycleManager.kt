package com.remoteforge.app.data.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

enum class VpnTunnelState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DEGRADED,
    SETUP_REQUIRED,
    ERROR
}

data class TransportVerificationResult(
    val isInstalled: Boolean,
    val isOverlayActive: Boolean,
    val clientOverlayIp: String?,
    val ifaceName: String?,
    val isMacReachable: Boolean,
    val latencyMs: Long,
    val failureReason: String? = null
)

class VpnLifecycleManager(private val context: Context) {

    companion object {
        const val TAG = "VpnLifecycleManager"
        const val NETBIRD_PACKAGE = "io.netbird.client"
        const val DEFAULT_MAC_OVERLAY_HOST = "100.117.133.109"
        const val DEFAULT_PORT = 8090
        const val DEFAULT_TIMEOUT_MS = 2500
    }

    private val _status = MutableStateFlow(VpnTunnelState.DISCONNECTED)
    val status: StateFlow<VpnTunnelState> = _status

    private val _clientOverlayIp = MutableStateFlow<String?>(null)
    val clientOverlayIp: StateFlow<String?> = _clientOverlayIp

    private val _activeInterfaceName = MutableStateFlow<String?>(null)
    val activeInterfaceName: StateFlow<String?> = _activeInterfaceName

    private val _lastVerifiedLatencyMs = MutableStateFlow<Long?>(null)
    val lastVerifiedLatencyMs: StateFlow<Long?> = _lastVerifiedLatencyMs

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage

    /**
     * Checks if the official NetBird companion client package is installed on the device.
     */
    fun isNetBirdInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(NETBIRD_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns an intent to launch the NetBird client, or market link if not installed.
     */
    fun getNetBirdLaunchIntent(): Intent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(NETBIRD_PACKAGE)
        if (launchIntent != null) {
            return launchIntent
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$NETBIRD_PACKAGE")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Detects active NetBird/WireGuard CGNAT overlay interface.
     * RFC 6598 designates 100.64.0.0/10 (100.64.0.0 to 100.127.255.255) for Carrier-Grade NAT overlays.
     * NetBird exclusively assigns peer addresses within this prefix.
     */
    fun detectNetBirdOverlay(): Pair<String, String>? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses) {
                    if (addr.isLoopbackAddress) continue
                    val host = addr.hostAddress ?: continue
                    if (isNetBirdCgnatIp(host)) {
                        return Pair(iface.name, host)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error enumerating network interfaces: ${e.message}")
        }
        return null
    }

    /**
     * Verifies that the IP falls into the 100.64.0.0/10 prefix (100.64.0.0 - 100.127.255.255).
     */
    fun isNetBirdCgnatIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        val first = parts[0].toIntOrNull() ?: return false
        val second = parts[1].toIntOrNull() ?: return false
        return first == 100 && second in 64..127
    }

    /**
     * Rigorous multi-stage transport verification:
     * 1. NetBird companion client presence
     * 2. Active NetBird overlay TUN interface (100.64.0.0/10)
     * 3. Direct TCP reachability to target Mac overlay endpoint (100.117.133.109:8090)
     */
    suspend fun verifyTransportHealth(
        targetMacOverlayHost: String = DEFAULT_MAC_OVERLAY_HOST,
        port: Int = DEFAULT_PORT,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): TransportVerificationResult = withContext(Dispatchers.IO) {
        val installed = isNetBirdInstalled()
        val overlayInfo = detectNetBirdOverlay()
        val overlayActive = overlayInfo != null

        _activeInterfaceName.value = overlayInfo?.first
        _clientOverlayIp.value = overlayInfo?.second

        if (!installed) {
            _status.value = VpnTunnelState.SETUP_REQUIRED
            _lastErrorMessage.value = "NetBird companion client is not installed."
            return@withContext TransportVerificationResult(
                isInstalled = false,
                isOverlayActive = false,
                clientOverlayIp = null,
                ifaceName = null,
                isMacReachable = false,
                latencyMs = -1L,
                failureReason = "NetBird companion client is not installed."
            )
        }

        if (!overlayActive) {
            _status.value = VpnTunnelState.SETUP_REQUIRED
            _lastErrorMessage.value = "NetBird VPN is disconnected. Open NetBird to connect."
            return@withContext TransportVerificationResult(
                isInstalled = true,
                isOverlayActive = false,
                clientOverlayIp = null,
                ifaceName = null,
                isMacReachable = false,
                latencyMs = -1L,
                failureReason = "NetBird VPN is disconnected."
            )
        }

        // Test direct TCP reachability to Mac overlay
        val startTime = System.currentTimeMillis()
        var reachable = false
        var errMsg: String? = null

        try {
            Socket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(targetMacOverlayHost, port), timeoutMs)
                reachable = true
            }
        } catch (e: Exception) {
            // Give WireGuard roaming a quick second attempt to allow socket rebinding to settle
            try {
                Thread.sleep(600)
                Socket().use { socket ->
                    socket.soTimeout = timeoutMs
                    socket.connect(InetSocketAddress(targetMacOverlayHost, port), timeoutMs)
                    reachable = true
                }
            } catch (e2: Exception) {
                errMsg = "Cannot reach Mac at $targetMacOverlayHost:$port (${e2.localizedMessage})"
                Log.w(TAG, errMsg)
            }
        }

        val latency = System.currentTimeMillis() - startTime
        _lastVerifiedLatencyMs.value = if (reachable) latency else null

        if (reachable) {
            _status.value = VpnTunnelState.CONNECTED
            _lastErrorMessage.value = null
        } else {
            _status.value = VpnTunnelState.RECONNECTING
            _lastErrorMessage.value = errMsg
        }

        TransportVerificationResult(
            isInstalled = true,
            isOverlayActive = true,
            clientOverlayIp = overlayInfo?.second,
            ifaceName = overlayInfo?.first,
            isMacReachable = reachable,
            latencyMs = latency,
            failureReason = errMsg
        )
    }

    /**
     * Fast non-blocking check whether the NetBird overlay interface is currently up.
     */
    fun isOverlayInterfacePresent(): Boolean {
        val overlay = detectNetBirdOverlay()
        return overlay != null
    }

    /**
     * Checks if VPN consent is already granted by Android OS if internal VpnService is invoked.
     */
    fun checkVpnConsentRequired(): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Initiates transport connection or guides user to NetBird setup if needed.
     */
    fun connectVpn() {
        val overlay = detectNetBirdOverlay()
        if (overlay != null) {
            _status.value = VpnTunnelState.CONNECTING
            _clientOverlayIp.value = overlay.second
            _activeInterfaceName.value = overlay.first
            return
        }

        if (!isNetBirdInstalled()) {
            _status.value = VpnTunnelState.SETUP_REQUIRED
            _lastErrorMessage.value = "NetBird client not installed."
        } else {
            _status.value = VpnTunnelState.SETUP_REQUIRED
            _lastErrorMessage.value = "NetBird VPN is disconnected. Tap to open NetBird."
        }
    }

    /**
     * Resets transport state to DISCONNECTED cleanly.
     */
    fun disconnectVpn() {
        _status.value = VpnTunnelState.DISCONNECTED
        _lastErrorMessage.value = null
    }

    fun updateStatus(newState: VpnTunnelState, errorMsg: String? = null) {
        _status.value = newState
        _lastErrorMessage.value = errorMsg
    }

    /**
     * Identifies current active physical transport (Wi-Fi, Mobile Data, Ethernet).
     */
    fun detectActiveNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Unknown"
        val activeNet = cm.activeNetwork ?: return "No Network"
        val caps = cm.getNetworkCapabilities(activeNet) ?: return "Unknown"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Overlay"
            else -> "Unknown"
        }
    }
}
