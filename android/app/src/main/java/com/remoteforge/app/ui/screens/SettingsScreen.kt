package com.remoteforge.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.config.NetworkConfig
import com.remoteforge.app.data.model.ConnectionState
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: RemoteForgeRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var serverUrlInput by remember { mutableStateOf(repository.serverUrl) }
    var usernameInput by remember { mutableStateOf(repository.networkConfig.savedUsername ?: "admin") }
    var passwordInput by remember { mutableStateOf(repository.networkConfig.savedPassword ?: "remoteforge_secure_pass") }
    var pingResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Expandable accordion states
    var showServerConfig by remember { mutableStateOf(false) }
    var showDiagnosticsDetails by remember { mutableStateOf(false) }
    var expandedAppSetting by remember { mutableStateOf<String?>(null) }

    val isAuthenticated = repository.authToken != null
    val connectionState by repository.connectionState.collectAsState()
    val isConnected = connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.PC_ONLINE || isAuthenticated
    val selectedDev by repository.selectedDevice.collectAsState()
    val metrics by repository.latencyMetrics.collectAsState()

    val displayHost = repository.serverUrl
        .removePrefix("http://")
        .removePrefix("https://")
        .ifEmpty { "remoteforge.local:8090" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. Header (RemoteForge + Settings + Bell)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RemoteForge",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextRestrainedGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    fontSize = 28.sp
                )
                Text(
                    text = "Preferences & Diagnostics",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextRestrainedGray,
                    fontSize = 12.sp
                )
            }

            // Notification Bell
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceElevated)
                    .border(0.5.dp, ObsidianBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = TextSoftWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ==========================================
        // 2. Server Connection Card
        // ==========================================
        ForgeSectionHeader(title = "Server Connection")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Online / Offline Indicator dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) AccentEmerald else AccentRuby)
                        )

                        Column {
                            Text(
                                text = if (isConnected) "Server connected" else "Server disconnected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPureWhite,
                                fontSize = 14.sp
                            )
                            Text(
                                text = displayHost,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextRestrainedGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Disconnect / Connect Button
                    OutlinedButton(
                        onClick = {
                            if (isConnected) {
                                repository.setAuthToken(null)
                            } else {
                                coroutineScope.launch { repository.autoConnect() }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.8.dp, if (isConnected) ObsidianBorder else AccentCyan),
                        colors = ButtonDefaults.outlinedButtonBorder.let {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isConnected) Color.Transparent else AccentCyan.copy(alpha = 0.15f),
                                contentColor = if (isConnected) TextSoftWhite else AccentCyan
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isConnected) "Disconnect" else "Connect",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Toggle base URL configuration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showServerConfig = !showServerConfig }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showServerConfig) "Hide Connection Presets" else "Configure Server Base URL & Presets",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (showServerConfig) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showServerConfig,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = {
                                serverUrlInput = it
                                repository.setServerUrl(it)
                            },
                            label = { Text("Server Base URL", color = TextDarkMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ObsidianSurfaceElevated,
                                unfocusedContainerColor = ObsidianSurfaceElevated,
                                unfocusedBorderColor = ObsidianBorderSubtle,
                                focusedBorderColor = AccentCyan,
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextPureWhite
                            ),
                            singleLine = true
                        )

                        // Quick presets chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "NetBird" to "http://100.117.133.109:8090",
                                "Local Mac" to "http://127.0.0.1:8090",
                                "Emulator LAN" to "http://10.0.2.2:8090"
                            ).forEach { (presetName, presetUrl) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (serverUrlInput == presetUrl) AccentCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
                                    border = BorderStroke(0.5.dp, if (serverUrlInput == presetUrl) AccentCyan else ObsidianBorderSubtle),
                                    modifier = Modifier.clickable {
                                        serverUrlInput = presetUrl
                                        repository.setServerUrl(presetUrl)
                                    }
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 11.sp,
                                        color = if (serverUrlInput == presetUrl) AccentCyan else TextSoftWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Diagnostics Card
        // ==========================================
        ForgeSectionHeader(title = "Diagnostics")

        val hasIssue = !isConnected
        val diagTitle = if (!hasIssue) "No issues detected" else "Connection issue detected"
        val diagSubtitle = if (!hasIssue) "All systems operational" else "Backend or PC agent unreachable"
        val chevronRotation by animateFloatAsState(
            targetValue = if (showDiagnosticsDetails) 90f else 0f,
            label = "diagChevron"
        )

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp),
            onClick = { showDiagnosticsDetails = !showDiagnosticsDetails }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background((if (!hasIssue) AccentEmerald else AccentRuby).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (!hasIssue) Icons.Rounded.VerifiedUser else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = if (!hasIssue) AccentEmerald else AccentRuby,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = diagTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPureWhite,
                                fontSize = 14.sp
                            )
                            Text(
                                text = diagSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextRestrainedGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Inspect",
                        tint = TextDarkMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }

                AnimatedVisibility(
                    visible = showDiagnosticsDetails,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                        // Diagnostics Item 1: API Health
                        SettingsDetailRow(
                            label = "API Health",
                            value = if (pingResult?.first == true) "OK" else if (isConnected) "CONNECTED" else "CHECKING",
                            statusColor = if (isConnected) AccentEmerald else AccentAmber
                        )

                        // Diagnostics Item 2: WebSocket State
                        SettingsDetailRow(
                            label = "WebSocket State",
                            value = if (connectionState == ConnectionState.PC_ONLINE || connectionState == ConnectionState.CONNECTED) "ACTIVE" else "STANDBY",
                            statusColor = if (connectionState == ConnectionState.PC_ONLINE || connectionState == ConnectionState.CONNECTED) AccentEmerald else TextDarkMuted
                        )

                        // Diagnostics Item 3: Primary Computer
                        SettingsDetailRow(
                            label = "Primary PC",
                            value = if (selectedDev?.isOnline == true) "ONLINE" else "OFFLINE",
                            statusColor = if (selectedDev?.isOnline == true) AccentEmerald else AccentRuby
                        )

                        // Diagnostics Item 4: NetBird Tunnel IP
                        SettingsDetailRow(
                            label = "Active Endpoint",
                            value = displayHost,
                            statusColor = AccentCyan
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isPinging = true
                                        val startTime = System.currentTimeMillis()
                                        val res = repository.pingHealth()
                                        val elapsed = System.currentTimeMillis() - startTime
                                        pingResult = if (res.first) true to "${elapsed}ms latency" else false to (res.second)
                                        isPinging = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isPinging) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentCyan, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Test Ping", color = AccentCyan, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.autoConnect()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reconnect", color = AccentViolet, fontSize = 12.sp)
                            }
                        }

                        pingResult?.let { (success, msg) ->
                            Text(
                                text = "Latency: $msg",
                                color = if (success) AccentEmerald else AccentRuby,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3b. Ultra-Low-Latency Performance Dashboard
        // ==========================================
        ForgeSectionHeader(title = "Network & Latency Diagnostics")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Analytics,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "Performance Dashboard",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPureWhite,
                                fontSize = 14.sp
                            )
                            Text(
                                "Real-time overlay & command pipeline",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextRestrainedGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            metrics.channelType,
                            color = AccentEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                SettingsDetailRow(
                    label = "Active Transport",
                    value = metrics.networkType,
                    statusColor = if (metrics.networkType.contains("Wi-Fi")) AccentCyan else AccentSky
                )

                SettingsDetailRow(
                    label = "NetBird Path",
                    value = metrics.netbirdPathType,
                    statusColor = if (metrics.netbirdPathType.contains("P2P")) AccentEmerald else AccentAmber
                )

                SettingsDetailRow(
                    label = "Overlay RTT",
                    value = metrics.overlayRttMs?.let { "${it}ms" } ?: "--",
                    statusColor = if (metrics.overlayRttMs != null && metrics.overlayRttMs!! < 60) AccentEmerald else AccentAmber
                )

                SettingsDetailRow(
                    label = "Command Latency (Median / p95)",
                    value = if (metrics.medianCommandMs > 0) "${metrics.medianCommandMs.toInt()}ms / ${metrics.p95CommandMs.toInt()}ms" else "--",
                    statusColor = if (metrics.medianCommandMs in 1.0..100.0) AccentEmerald else AccentCyan
                )

                SettingsDetailRow(
                    label = "Last Command Breakdown",
                    value = if (metrics.lastCommandTotalMs != null) {
                        "Net: ${metrics.lastCommandNetworkMs?.toInt()}ms | Srv: ${metrics.lastCommandBackendMs?.toInt()}ms | Mac: ${metrics.lastCommandMacMs?.toInt()}ms"
                    } else {
                        "--"
                    },
                    statusColor = AccentSky
                )

                SettingsDetailRow(
                    label = "Screenshot Pipeline",
                    value = if (metrics.lastScreenshotDurationMs != null) {
                        "${metrics.lastScreenshotDurationMs?.toInt()}ms (${metrics.lastScreenshotSizeKb?.toInt()} KB)"
                    } else {
                        "Preview 1280px (Optimized)"
                    },
                    statusColor = AccentViolet
                )

                SettingsDetailRow(
                    label = "Network Handover Time",
                    value = metrics.lastHandoverDurationMs?.let { "${it}ms" } ?: "Seamless",
                    statusColor = AccentCyan
                )

                if (metrics.netbirdPathType.contains("Relayed")) {
                    Text(
                        text = "ℹ NetBird path is relayed via Mumbai relay (rels://streamline-vl-bom-1.relay.netbird.io:443). Restrictive cellular/NAT CGNAT prevents direct ICE hole-punching.",
                        color = TextDarkMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ==========================================
        // 4. App Settings Grouped Card
        // ==========================================
        ForgeSectionHeader(title = "App Settings")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Column {
                // Item 0: Connection & VPN Lifecycle
                val context = androidx.compose.ui.platform.LocalContext.current
                val isInstalled = repository.vpnManager.isNetBirdInstalled()
                val overlayInfo = repository.vpnManager.detectNetBirdOverlay()
                val isOverlayActive = overlayInfo != null
                val clientOverlayIp = overlayInfo?.second
                val activeIface = overlayInfo?.first
                val vpnTunnelState by repository.vpnManager.status.collectAsState()
                val vpnLatency by repository.vpnManager.lastVerifiedLatencyMs.collectAsState()
                var isVerifyingTransport by remember { mutableStateOf(false) }
                var transportCheckFeedback by remember { mutableStateOf<String?>(null) }
                var currentMode by remember { mutableStateOf(repository.networkConfig.connectionMode) }

                AppSettingsRow(
                    icon = Icons.Rounded.VpnKey,
                    label = "VPN & Mesh Transport",
                    badge = if (isOverlayActive) "Mesh Active" else if (isInstalled) "NetBird Disconnected" else "NetBird Required",
                    badgeColor = if (isOverlayActive) AccentEmerald else AccentAmber,
                    onClick = {
                        expandedAppSetting = if (expandedAppSetting == "VPN") null else "VPN"
                    }
                )

                AnimatedVisibility(
                    visible = expandedAppSetting == "VPN",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "NetBird Transport Diagnostics",
                            color = TextPureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Diagnostics Grid
                        SettingsDetailRow(
                            label = "Companion Client",
                            value = if (isInstalled) "Installed" else "Not Installed",
                            statusColor = if (isInstalled) AccentEmerald else AccentRuby
                        )
                        SettingsDetailRow(
                            label = "Overlay Interface",
                            value = activeIface ?: "None",
                            statusColor = if (isOverlayActive) AccentEmerald else AccentAmber
                        )
                        SettingsDetailRow(
                            label = "Client Overlay IP",
                            value = clientOverlayIp ?: "Not Assigned",
                            statusColor = if (clientOverlayIp != null) AccentCyan else TextDarkMuted
                        )
                        SettingsDetailRow(
                            label = "Mac Overlay Target",
                            value = "100.117.133.109:8090",
                            statusColor = AccentCyan
                        )
                        SettingsDetailRow(
                            label = "Tunnel State",
                            value = vpnTunnelState.name,
                            statusColor = when (vpnTunnelState) {
                                com.remoteforge.app.data.vpn.VpnTunnelState.CONNECTED -> AccentEmerald
                                com.remoteforge.app.data.vpn.VpnTunnelState.CONNECTING, com.remoteforge.app.data.vpn.VpnTunnelState.RECONNECTING -> AccentSky
                                com.remoteforge.app.data.vpn.VpnTunnelState.SETUP_REQUIRED -> AccentAmber
                                else -> AccentRuby
                            }
                        )
                        SettingsDetailRow(
                            label = "Verified TCP Latency",
                            value = vpnLatency?.let { "${it}ms" } ?: "--",
                            statusColor = if (vpnLatency != null) AccentEmerald else TextDarkMuted
                        )

                        transportCheckFeedback?.let { feedback ->
                            Text(
                                text = feedback,
                                color = if (isOverlayActive) AccentEmerald else AccentAmber,
                                fontSize = 11.sp
                            )
                        }

                        // Buttons: Open NetBird & Verify Transport
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val launchIntent = repository.vpnManager.getNetBirdLaunchIntent()
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open NetBird", color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isVerifyingTransport = true
                                        val res = repository.vpnManager.verifyTransportHealth()
                                        transportCheckFeedback = if (res.isMacReachable) {
                                            "Mac reachable via ${res.ifaceName ?: "overlay"} in ${res.latencyMs}ms"
                                        } else {
                                            res.failureReason ?: "Verification failed"
                                        }
                                        isVerifyingTransport = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isVerifyingTransport) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentCyan, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("Verify Path", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                        Text(
                            "Lifecycle Behavior",
                            color = TextPureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Mode 1: App Session (Default)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentMode == NetworkConfig.MODE_APP_SESSION) AccentCyan.copy(alpha = 0.15f) else ObsidianSurfaceElevated,
                            border = BorderStroke(0.5.dp, if (currentMode == NetworkConfig.MODE_APP_SESSION) AccentCyan else ObsidianBorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentMode = NetworkConfig.MODE_APP_SESSION
                                    repository.networkConfig.connectionMode = NetworkConfig.MODE_APP_SESSION
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("App Session (Default)", color = TextPureWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    if (currentMode == NetworkConfig.MODE_APP_SESSION) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text("Keeps connection alive in background. Disconnects session when app is dismissed from Recents.", color = TextRestrainedGray, fontSize = 11.sp)
                            }
                        }

                        // Mode 2: Stay Connected
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentMode == NetworkConfig.MODE_STAY_CONNECTED) AccentEmerald.copy(alpha = 0.15f) else ObsidianSurfaceElevated,
                            border = BorderStroke(0.5.dp, if (currentMode == NetworkConfig.MODE_STAY_CONNECTED) AccentEmerald else ObsidianBorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentMode = NetworkConfig.MODE_STAY_CONNECTED
                                    repository.networkConfig.connectionMode = NetworkConfig.MODE_STAY_CONNECTED
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Stay Connected", color = TextPureWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    if (currentMode == NetworkConfig.MODE_STAY_CONNECTED) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text("Maintains background foreground service persistently even when app is navigated away.", color = TextRestrainedGray, fontSize = 11.sp)
                            }
                        }

                        // Manual Disconnect Button
                        Button(
                            onClick = {
                                repository.disconnectSession(disconnectVpn = true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRuby.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = AccentRuby, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Disconnect Session", color = AccentRuby, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                // Item 1: General
                AppSettingsRow(
                    icon = Icons.Rounded.Settings,
                    label = "General",
                    onClick = {
                        expandedAppSetting = if (expandedAppSetting == "General") null else "General"
                    }
                )

                AnimatedVisibility(
                    visible = expandedAppSetting == "General",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Appearance: Obsidian Liquid Glass (Dark)", color = TextRestrainedGray, fontSize = 12.sp)
                        Text("Haptic Feedback: Enabled", color = TextRestrainedGray, fontSize = 12.sp)
                        Text("Default Download Folder: /storage/emulated/0/Download", color = TextRestrainedGray, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                // Item 2: Security
                AppSettingsRow(
                    icon = Icons.Rounded.Security,
                    label = "Security",
                    badge = if (isAuthenticated) "Authenticated" else "Sign In",
                    badgeColor = if (isAuthenticated) AccentEmerald else AccentAmber,
                    onClick = {
                        expandedAppSetting = if (expandedAppSetting == "Security") null else "Security"
                    }
                )

                AnimatedVisibility(
                    visible = expandedAppSetting == "Security",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isAuthenticated) {
                            Text("Signed in with JWT Session", color = AccentEmerald, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick = { repository.setAuthToken(null) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRuby.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Log Out", color = AccentRuby, fontSize = 12.sp)
                            }
                        } else {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Username", color = TextDarkMuted, fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ObsidianSurfaceElevated,
                                    unfocusedContainerColor = ObsidianSurfaceElevated,
                                    unfocusedBorderColor = ObsidianBorderSubtle,
                                    focusedBorderColor = AccentCyan,
                                    focusedTextColor = TextPureWhite,
                                    unfocusedTextColor = TextPureWhite
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password", color = TextDarkMuted, fontSize = 11.sp) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = ObsidianSurfaceElevated,
                                    unfocusedContainerColor = ObsidianSurfaceElevated,
                                    unfocusedBorderColor = ObsidianBorderSubtle,
                                    focusedBorderColor = AccentCyan,
                                    focusedTextColor = TextPureWhite,
                                    unfocusedTextColor = TextPureWhite
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isAuthenticating = true
                                        authError = null
                                        val (success, err) = repository.login(usernameInput, passwordInput)
                                        if (!success) {
                                            authError = err ?: "Authentication failed"
                                        }
                                        isAuthenticating = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AccentCyan, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Sign In", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            authError?.let { err ->
                                Text(text = err, color = AccentRuby, fontSize = 11.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                // Item 3: Notifications
                AppSettingsRow(
                    icon = Icons.Rounded.Notifications,
                    label = "Notifications",
                    onClick = {
                        expandedAppSetting = if (expandedAppSetting == "Notifications") null else "Notifications"
                    }
                )

                AnimatedVisibility(
                    visible = expandedAppSetting == "Notifications",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Agent Online Alerts: Enabled", color = TextRestrainedGray, fontSize = 12.sp)
                        Text("High Resource Utilization Alerts: Enabled (>90%)", color = TextRestrainedGray, fontSize = 12.sp)
                        Text("File Transfer Status: Enabled", color = TextRestrainedGray, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                // Item 4: About RemoteForge
                AppSettingsRow(
                    icon = Icons.Rounded.Info,
                    label = "About RemoteForge",
                    badge = "v0.1.0",
                    badgeColor = TextRestrainedGray,
                    onClick = {
                        expandedAppSetting = if (expandedAppSetting == "About") null else "About"
                    }
                )

                AnimatedVisibility(
                    visible = expandedAppSetting == "About",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("RemoteForge Android Client", color = TextPureWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Optimized for Samsung Galaxy S21 FE 5G (SM-G990B2)", color = TextRestrainedGray, fontSize = 11.sp)
                        Text("UI Architecture: Obsidian Liquid Glass Native Compose", color = AccentCyan, fontSize = 11.sp)
                        Text("Security: Strict User Directory Sandbox & Bearer JWT", color = AccentEmerald, fontSize = 11.sp)
                    }
                }
            }
        }

        // Clearance space for floating navigation bar
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun AppSettingsRow(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    badgeColor: Color = AccentCyan,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextRestrainedGray,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSoftWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            badge?.let {
                Text(
                    text = it,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextDarkMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsDetailRow(
    label: String,
    value: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextRestrainedGray, fontSize = 11.sp)
        Text(
            text = value,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
