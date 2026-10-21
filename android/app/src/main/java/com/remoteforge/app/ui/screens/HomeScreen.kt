package com.remoteforge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.remoteforge.app.data.model.ConnectionState
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: RemoteForgeRepository,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedDevice by repository.selectedDevice.collectAsState()
    val devices by repository.devices.collectAsState()
    val telemetry by repository.telemetry.collectAsState()
    val auditLogs by repository.auditLogs.collectAsState()
    val connectionState by repository.connectionState.collectAsState()
    val activeTask by repository.activeTask.collectAsState()
    val lastLatencyMs by repository.lastLatencyMs.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var showDeviceSwitcher by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(selectedDevice?.id, devices) {
        val dev = selectedDevice ?: devices.firstOrNull()
        if (dev != null) {
            repository.refreshAuditLogs()
            while (isActive) {
                if (dev.isOnline) {
                    repository.fetchTelemetry(dev.id)
                    delay(8000L)
                } else {
                    delay(15000L)
                }
            }
        }
    }

    // Telemetry values
    val cpuVal = (telemetry?.cpu?.percent?.toFloat() ?: 0f).coerceIn(0f, 100f)
    val ramVal = (telemetry?.ram?.percent?.toFloat() ?: 0f).coerceIn(0f, 100f)
    val diskVal = (telemetry?.disk?.percent?.toFloat() ?: 0f).coerceIn(0f, 100f)

    val currentDev = selectedDevice ?: devices.firstOrNull()
    val isConnected = connectionState == ConnectionState.PC_ONLINE && currentDev?.isOnline == true
    val forgeColors = ForgeTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. Connected-Device Island (Capsule pill at top)
            // ==========================================
            ConnectedDeviceIsland(
                device = currentDev,
                connectionState = if (isConnected) "Online" else connectionState.name,
                latencyMs = lastLatencyMs,
                scrollOffset = scrollState.value.toFloat(),
                onTap = {
                    currentDev?.let { dev ->
                        onNavigate(AppScreen.DeviceDetails(dev.id))
                    } ?: onNavigate(AppScreen.Devices)
                },
                onSwitchDevice = {
                    showDeviceSwitcher = true
                }
            )

            // ==========================================
            // 2. Editorial Header
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RemoteForgeEmblem(size = 38.dp)

                    Column {
                        Text(
                            text = "REMOTEFORGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (forgeColors.isDark) LavenderAccent else LightAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Command Center",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = forgeColors.textPrimary,
                            fontSize = 22.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                repository.refreshDevices()
                                currentDev?.id?.let { repository.fetchTelemetry(it) }
                                repository.refreshAuditLogs()
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onNavigate(AppScreen.ActivityLogs) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = "Activity Feed",
                            tint = TextSoftWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Action Feedback banner if present
            actionFeedback?.let { msg ->
                ForgeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = RoyalViolet.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(16.dp))
                        Text(text = msg, color = PearlWhite, fontSize = 12.sp)
                    }
                }
            }

            // ==========================================
            // 3. Proactive Connection Center (When Disconnected)
            // ==========================================
            if (!isConnected) {
                ForgeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AmberWarning.copy(alpha = 0.6f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberWarning.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (connectionState == ConnectionState.SETUP_REQUIRED)
                                        "NetBird Mesh Disconnected"
                                    else
                                        "Workstation Offline",
                                    color = forgeColors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (connectionState == ConnectionState.SETUP_REQUIRED)
                                        "Connect to NetBird VPN to establish encrypted transport to your PC."
                                    else
                                        "Target machine '${currentDev?.name ?: "PC"}' is unreachable. Verify agent is running.",
                                    color = forgeColors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch { repository.autoConnect() }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 9.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = PearlWhite, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry Connection", color = PearlWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showDeviceSwitcher = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LavenderAccent.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(vertical = 9.dp)
                            ) {
                                Icon(Icons.Rounded.Devices, contentDescription = null, tint = LavenderAccent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Switch PC", color = LavenderAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. Hero Workstation Card
            // ==========================================
            val isMac = currentDev?.osName?.contains("Mac", ignoreCase = true) == true || currentDev?.osName?.contains("Darwin", ignoreCase = true) == true

            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isConnected) (if (forgeColors.isDark) LavenderAccent.copy(alpha = 0.5f) else LightAccent.copy(alpha = 0.5f)) else forgeColors.borderSubtle,
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(RoyalViolet.copy(alpha = 0.4f), DeepPlum)
                                        )
                                    )
                                    .border(1.dp, LavenderAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMac) Icons.Rounded.LaptopMac else Icons.Rounded.Computer,
                                    contentDescription = null,
                                    tint = PearlWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = currentDev?.name ?: "No Computer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = forgeColors.textPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (currentDev != null) "${currentDev.osName} • Agent v${currentDev.agentVersion}" else "Register an agent to begin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = forgeColors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        ForgeStatusBadge(
                            text = if (isConnected) "ONLINE" else "OFFLINE",
                            accentColor = if (isConnected) AccentEmerald else RubyOffline,
                            isPulsing = isConnected
                        )
                    }

                    // Telemetry Ring Gauges (CPU, RAM, Disk) - Zero Cyan
                    if (isConnected && telemetry != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ForgeTelemetryGauge(
                                percent = cpuVal,
                                label = "CPU Load",
                                accentColor = RoyalViolet
                            )
                            ForgeTelemetryGauge(
                                percent = ramVal,
                                label = "RAM Used",
                                accentColor = LavenderAccent
                            )
                            ForgeTelemetryGauge(
                                percent = diskVal,
                                label = "Disk Space",
                                accentColor = EmeraldOnline
                            )
                        }
                    } else if (!isConnected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Telemetry paused while offline",
                                color = TextDarkMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Primary Live Workstation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                currentDev?.let { dev ->
                                    onNavigate(AppScreen.LiveScreen(dev.id))
                                }
                            },
                            enabled = isConnected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (forgeColors.isDark) RoyalViolet else LightAccent,
                                disabledContainerColor = (if (forgeColors.isDark) RoyalViolet else LightAccent).copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Live Mirror", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                currentDev?.let { dev ->
                                    onNavigate(AppScreen.RemoteControl(dev.id))
                                }
                            },
                            enabled = isConnected,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, (if (forgeColors.isDark) LavenderAccent else LightAccent).copy(alpha = if (isConnected) 0.5f else 0.2f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.Terminal, contentDescription = null, tint = if (forgeColors.isDark) LavenderAccent else LightAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Remote Command", color = if (forgeColors.isDark) LavenderAccent else LightAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Pending and Active Guest Co-Control Sessions
            currentDev?.let { dev ->
                GuestSessionsCard(
                    deviceId = dev.id,
                    repository = repository
                )
            }

            // ==========================================
            // 5. Contextual Quick Actions Grid
            // ==========================================
            ForgeSectionHeader(
                title = "Workstation Tools",
                subtitle = "Policy-governed control tools"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ForgeQuickActionCard(
                    title = "Files & Sandboxes",
                    subtitle = "User data roots",
                    icon = Icons.Rounded.FolderOpen,
                    accentColor = if (forgeColors.isDark) RoyalViolet else LightAccent,
                    onClick = { onNavigate(AppScreen.Files) },
                    modifier = Modifier.weight(1f)
                )

                ForgeQuickActionCard(
                    title = "Developer Hub",
                    subtitle = "Git, diffs, terminal",
                    icon = Icons.Rounded.Terminal,
                    accentColor = if (forgeColors.isDark) LavenderAccent else LightAccent,
                    onClick = {
                        currentDev?.let { onNavigate(AppScreen.Developer(it.id)) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ForgeQuickActionCard(
                    title = "Universal Clipboard",
                    subtitle = "Cross-device sync",
                    icon = Icons.Rounded.ContentCopy,
                    accentColor = if (forgeColors.isDark) RoyalViolet else LightAccent,
                    onClick = {
                        currentDev?.let { onNavigate(AppScreen.ClipboardHistory(it.id)) }
                    },
                    modifier = Modifier.weight(1f)
                )

                ForgeQuickActionCard(
                    title = "Automations Studio",
                    subtitle = "Workflows & cron",
                    icon = Icons.Rounded.Schedule,
                    accentColor = if (forgeColors.isDark) LavenderAccent else LightAccent,
                    onClick = { onNavigate(AppScreen.Automations) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ForgeQuickActionCard(
                    title = "File Transfers",
                    subtitle = "Chunked pipeline",
                    icon = Icons.Rounded.SwapVert,
                    accentColor = if (forgeColors.isDark) LavenderAccent else LightAccent,
                    onClick = { onNavigate(AppScreen.Transfers) },
                    modifier = Modifier.weight(1f)
                )

                ForgeQuickActionCard(
                    title = "Process Manager",
                    subtitle = "Tasks & resources",
                    icon = Icons.Rounded.Memory,
                    accentColor = if (forgeColors.isDark) RoyalViolet else LightAccent,
                    onClick = {
                        currentDev?.let { onNavigate(AppScreen.TaskManager(it.id)) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // 6. Recent Audit Trail Activity Preview
            // ==========================================
            ForgeSectionHeader(
                title = "Recent Activity",
                subtitle = "Immutable security trail",
                actionLabel = "View All",
                onAction = { onNavigate(AppScreen.ActivityLogs) }
            )

            if (auditLogs.isEmpty()) {
                ForgeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No recent activity recorded",
                            color = TextRestrainedGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                ForgeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    auditLogs.take(5).forEachIndexed { idx, log ->
                        val isSuccess = log.executionStatus == "SUCCESS"
                        ForgeActivityRow(
                            title = "${log.action} (${log.tool ?: "system"})",
                            timestamp = log.timestamp.takeLast(8),
                            icon = if (log.action.contains("AI")) Icons.Rounded.AutoAwesome else Icons.Rounded.Bolt,
                            isSuccess = isSuccess,
                            onClick = { onNavigate(AppScreen.ActivityLogs) }
                        )
                        if (idx < 4 && idx < auditLogs.size - 1) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom floating pill
        }

        // ==========================================
        // Device Switcher Dialog
        // ==========================================
        if (showDeviceSwitcher) {
            Dialog(
                onDismissRequest = { showDeviceSwitcher = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { showDeviceSwitcher = false },
                    contentAlignment = Alignment.Center
                ) {
                    ForgeGlassCard(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clickable(enabled = false) {}, // Prevent dismiss on inner click
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Workstation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PearlWhite,
                                    fontSize = 16.sp
                                )
                                IconButton(
                                    onClick = { showDeviceSwitcher = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextRestrainedGray)
                                }
                            }

                            if (devices.isEmpty()) {
                                Text(
                                    text = "No registered devices found.",
                                    color = TextRestrainedGray,
                                    fontSize = 12.sp
                                )
                            } else {
                                devices.forEach { dev ->
                                    val isCurrent = dev.id == currentDev?.id
                                    ForgeDeviceCard(
                                        device = dev,
                                        isSelected = isCurrent,
                                        onClick = {
                                            repository.selectDevice(dev)
                                            showDeviceSwitcher = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
