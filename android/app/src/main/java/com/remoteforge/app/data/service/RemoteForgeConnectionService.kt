package com.remoteforge.app.data.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.remoteforge.app.RemoteForgeApplication
import com.remoteforge.app.data.config.NetworkConfig
import com.remoteforge.app.data.model.ConnectionState
import com.remoteforge.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class RemoteForgeConnectionService : Service() {

    companion object {
        const val TAG = "ConnectionService"
        const val CHANNEL_ID = "remoteforge_connection_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START = "com.remoteforge.app.SERVICE_START"
        const val ACTION_STOP = "com.remoteforge.app.SERVICE_STOP"
        const val ACTION_DISCONNECT = "com.remoteforge.app.SERVICE_DISCONNECT"

        fun start(context: Context) {
            val intent = Intent(context, RemoteForgeConnectionService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start foreground service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RemoteForgeConnectionService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop service: ${e.message}")
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var handoverJob: Job? = null
    private var lastTransportType: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerNetworkCallback()
        observeRepositoryState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP, ACTION_DISCONNECT -> {
                handleExplicitDisconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("RemoteForge Active", "Managing secure PC connection"))
            }
        }
        return START_STICKY
    }

    private fun handleExplicitDisconnect() {
        val app = application as? RemoteForgeApplication ?: return
        val repo = app.repository
        repo.vpnManager.disconnectVpn()
        repo.setAuthToken(null)
    }

    private fun observeRepositoryState() {
        val app = application as? RemoteForgeApplication ?: return
        val repo = app.repository

        serviceScope.launch {
            repo.connectionStatusText.collectLatest { status ->
                val devName = repo.selectedDevice.value?.name ?: "PC"
                val netTag = lastTransportType?.let { " • $it" } ?: ""
                val text = "$devName$netTag • $status"
                val notification = buildNotification("RemoteForge Connection", text)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun registerNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleNetworkTransition(network, "Available")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                handleNetworkTransition(network, "CapabilitiesChanged")
            }

            override fun onLost(network: Network) {
                Log.i(TAG, "Network interface lost. Checking remaining connectivity...")
                val app = application as? RemoteForgeApplication ?: return
                // Cancel existing handover sequence
                handoverJob?.cancel()
                handoverJob = serviceScope.launch {
                    delay(500)
                    val activeNet = connectivityManager?.activeNetwork
                    if (activeNet == null) {
                        lastTransportType = "None"
                        app.repository.onNetworkLost()
                    }
                }
            }
        }

        try {
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun handleNetworkTransition(network: Network, event: String) {
        val caps = connectivityManager?.getNetworkCapabilities(network) ?: return
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return

        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val currentTransport = when {
            isWifi -> "Wi-Fi"
            isCellular -> "Mobile Data"
            else -> "Network"
        }

        if (currentTransport == lastTransportType && event == "CapabilitiesChanged") {
            return
        }

        lastTransportType = currentTransport
        Log.i(TAG, "Network transition to $currentTransport ($event). Debouncing for WireGuard roaming stabilization...")

        val app = application as? RemoteForgeApplication ?: return
        // Cancel obsolete handover attempts
        handoverJob?.cancel()
        handoverJob = serviceScope.launch {
            // Wait 1200ms to allow physical sockets and NetBird WireGuard roaming to settle
            delay(1200)
            app.repository.onNetworkChanged(currentTransport)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val app = application as? RemoteForgeApplication ?: return
        val config = app.networkConfig

        if (config.connectionMode == NetworkConfig.MODE_APP_SESSION) {
            Log.i(TAG, "App dismissed from Recents in APP_SESSION mode. Disconnecting VPN & releasing resources.")
            handleExplicitDisconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            Log.i(TAG, "App dismissed from Recents in STAY_CONNECTED mode. Keeping persistent tunnel active.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        handoverJob?.cancel()
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RemoteForge Connection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays the background connection and VPN status for RemoteForge."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, RemoteForgeConnectionService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
