package com.remoteforge.app.data.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

class RemoteForgeVpnService : VpnService() {

    companion object {
        const val TAG = "RemoteForgeVpnService"
        const val ACTION_CONNECT = "com.remoteforge.app.ACTION_CONNECT_VPN"
        const val ACTION_DISCONNECT = "com.remoteforge.app.ACTION_DISCONNECT_VPN"

        private val _tunnelState = MutableStateFlow(VpnTunnelState.DISCONNECTED)
        val tunnelState: StateFlow<VpnTunnelState> = _tunnelState

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                handleConnect()
            }
            ACTION_DISCONNECT -> {
                disconnectTunnel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleConnect() {
        // RemoteForge integrates with the official NetBird client (io.netbird.client).
        // Establishing an empty TUN without userspace packet processing loop would blackhole
        // traffic and revoke the interface from NetBird.
        Log.i(TAG, "RemoteForge delegates encrypted mesh networking to the NetBird transport engine.")
        _tunnelState.value = VpnTunnelState.CONNECTING
        _lastError.value = null
    }

    private fun disconnectTunnel() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            _tunnelState.value = VpnTunnelState.DISCONNECTED
            Log.i(TAG, "RemoteForge VPN service stopped.")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing VPN descriptor", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectTunnel()
    }
}
