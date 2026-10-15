package com.remoteforge.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.ConnectionState
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: RemoteForgeRepository,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val selectedDevice by repository.selectedDevice.collectAsState()
    val devices by repository.devices.collectAsState()
    val telemetry by repository.telemetry.collectAsState()
    val auditLogs by repository.auditLogs.collectAsState()
    val connectionState by repository.connectionState.collectAsState()
    val activeTask by repository.activeTask.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

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
    val cpuVal = (telemetry?.cpu?.percent?.toFloat() ?: 12f).coerceIn(0f, 100f)
    val ramVal = (telemetry?.ram?.percent?.toFloat() ?: 48f).coerceIn(0f, 100f)
    val diskVal = (telemetry?.disk?.percent?.toFloat() ?: 67f).coerceIn(0f, 100f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. Header (RemoteForge, Home, Overview & Control)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "RemoteForge",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextRestrainedGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    fontSize = 28.sp
                )
                Text(
                    text = "Overview & Control",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextRestrainedGray,
                    fontSize = 12.sp
                )
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
                            selectedDevice?.id?.let { repository.fetchTelemetry(it) }
                            repository.refreshAuditLogs()
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onNavigate(AppScreen.ActivityLogs) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = "Notifications",
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
                borderColor = AccentCyan.copy(alpha = 0.5f),
                contentPadding = PaddingValues(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Text(text = msg, color = AccentCyan, fontSize = 12.sp)
                }
            }
        }

        // ==========================================
        // Connection & Transport Banner
        // ==========================================
        val connectionStatusText by repository.connectionStatusText.collectAsState()
        val lastError by repository.lastError.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current

        if (connectionState == ConnectionState.SETUP_REQUIRED) {
            val isInstalled = repository.vpnManager.isNetBirdInstalled()
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AccentAmber.copy(alpha = 0.6f),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.VpnKey,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!isInstalled) "NetBird VPN Required" else "NetBird VPN Disconnected",
                                color = TextPureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (!isInstalled)
                                    "RemoteForge requires NetBird to reach your Mac (100.117.133.109) over private encrypted mesh."
                                else
                                    "NetBird is disconnected. Connect in NetBird to enable RemoteForge Mac transport.",
                                color = TextRestrainedGray,
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
                                val launchIntent = repository.vpnManager.getNetBirdLaunchIntent()
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (!isInstalled) "Install NetBird" else "Open NetBird", color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.autoConnect()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Connection", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else if (connectionState == ConnectionState.RECONNECTING || connectionState == ConnectionState.CONNECTING) {
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AccentSky.copy(alpha = 0.4f),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AccentSky,
                        strokeWidth = 2.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (connectionState == ConnectionState.RECONNECTING) "Reconnecting..." else "Connecting...",
                            color = TextPureWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = connectionStatusText,
                            color = TextRestrainedGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else if (connectionState == ConnectionState.DEGRADED) {
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AccentRuby.copy(alpha = 0.4f),
                contentPadding = PaddingValues(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = AccentRuby,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connection Degraded",
                            color = AccentRuby,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = lastError ?: "Transport online but Mac agent is unresponsive",
                            color = TextRestrainedGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. Primary Device Card (MacBook Pro)
        // ==========================================
        val currentDevice = selectedDevice ?: devices.firstOrNull()
        if (currentDevice != null) {
            ForgeDeviceCard(
                device = currentDevice,
                isSelected = true,
                lastActiveText = telemetry?.uptimeFormatted?.let { "Uptime $it" } ?: "Last active 2 min ago",
                onClick = {
                    onNavigate(AppScreen.DeviceDetails(currentDevice.id))
                }
            )
        } else {
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                onClick = { onNavigate(AppScreen.Devices) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Computer, contentDescription = null, tint = TextDarkMuted, modifier = Modifier.size(28.dp))
                    Column {
                        Text("No Computer Connected", color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tap to scan or register computers", color = TextRestrainedGray, fontSize = 11.sp)
                    }
                }
            }
        }

        // ==========================================
        // 3. Circular Telemetry Gauges Card (CPU / RAM / Disk)
        // ==========================================
        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. CPU
                ForgeTelemetryGauge(
                    percent = cpuVal,
                    label = "CPU",
                    accentColor = AccentSky,
                    size = 72.dp
                )

                // 2. RAM
                ForgeTelemetryGauge(
                    percent = ramVal,
                    label = "RAM",
                    accentColor = AccentEmerald,
                    size = 72.dp
                )

                // 3. Disk
                ForgeTelemetryGauge(
                    percent = diskVal,
                    label = "Disk",
                    accentColor = AccentCyan,
                    size = 72.dp
                )
            }
        }

        // ==========================================
        // 4. Quick Action Grid (2x2)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Row 1: Lock PC & Screenshot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ForgeQuickActionCard(
                    title = "Lock PC",
                    subtitle = "Secure session",
                    icon = Icons.Rounded.Lock,
                    accentColor = AccentAmber,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val dev = selectedDevice ?: devices.firstOrNull()
                        if (dev != null) {
                            coroutineScope.launch {
                                val res = repository.dispatchQuickAction(dev.id, "lock_pc")
                                actionFeedback = res.second
                            }
                        } else {
                            actionFeedback = "No computer connected to lock"
                        }
                    }
                )

                ForgeQuickActionCard(
                    title = "Screenshot",
                    subtitle = "Capture screen",
                    icon = Icons.Rounded.CameraAlt,
                    accentColor = AccentCyan,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val dev = selectedDevice ?: devices.firstOrNull()
                        if (dev != null) {
                            coroutineScope.launch {
                                val res = repository.dispatchQuickAction(dev.id, "take_screenshot")
                                actionFeedback = res.second
                            }
                        } else {
                            actionFeedback = "No computer connected"
                        }
                    }
                )
            }

            // Row 2: Browse Files & Open Browser
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ForgeQuickActionCard(
                    title = "Browse Files",
                    subtitle = "View & manage",
                    icon = Icons.Rounded.Folder,
                    accentColor = AccentCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppScreen.Files) }
                )

                ForgeQuickActionCard(
                    title = "Open Browser",
                    subtitle = "Launch default",
                    icon = Icons.Rounded.Language,
                    accentColor = AccentCyan,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val dev = selectedDevice ?: devices.firstOrNull()
                        if (dev != null) {
                            coroutineScope.launch {
                                val res = repository.dispatchQuickAction(dev.id, "open_browser")
                                actionFeedback = res.second
                            }
                        } else {
                            actionFeedback = "No computer connected"
                        }
                    }
                )
            }
        }

        // ==========================================
        // 5. Recent Activity
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ForgeSectionHeader(
                title = "Recent Activity",
                actionLabel = "View all",
                onAction = { onNavigate(AppScreen.ActivityLogs) }
            )

            val realActivities = remember(auditLogs) {
                auditLogs
                    .filter { it.tool != "get_system_status" && !it.action.contains("get_system_status", ignoreCase = true) }
                    .take(4)
            }

            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (realActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent activity recorded yet.\nLive commands and transfers will appear here.",
                            color = TextRestrainedGray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        realActivities.forEachIndexed { index, act ->
                            ForgeActivityRow(
                                title = getActivityTitle(act.action, act.tool, act.details),
                                timestamp = formatRelativeTime(act.timestamp),
                                icon = getActivityIcon(act.tool, act.action),
                                isSuccess = act.executionStatus == "SUCCESS",
                                onClick = { onNavigate(AppScreen.ActivityLogs) }
                            )
                            if (index < realActivities.lastIndex) {
                                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        // Clearance space for floating navigation bar
        Spacer(modifier = Modifier.height(84.dp))
    }

    // Live Screenshot Preview Dialog
    val screenshotBase64 by repository.lastScreenshotBase64.collectAsState()
    if (!screenshotBase64.isNullOrBlank()) {
        val bitmap = remember(screenshotBase64) {
            try {
                val decoded = android.util.Base64.decode(screenshotBase64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

        Dialog(
            onDismissRequest = { repository.clearScreenshot() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .wrapContentHeight()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xF20D1322),
                border = BorderStroke(1.dp, GlassBorderHighlight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Live Screenshot",
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(onClick = { repository.clearScreenshot() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondaryDark)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Desktop Screenshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(14.dp))
                                .clickable {
                                    val b64 = screenshotBase64!!
                                    repository.clearScreenshot()
                                    onNavigate(AppScreen.ScreenshotViewer(b64, selectedDevice?.name ?: "MacBook Pro"))
                                },
                            contentScale = ContentScale.Fit
                        )

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val b64 = screenshotBase64!!
                                    repository.clearScreenshot()
                                    onNavigate(AppScreen.ScreenshotViewer(b64, selectedDevice?.name ?: "MacBook Pro"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Rounded.Fullscreen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Full-Screen Viewer", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { repository.clearScreenshot() },
                                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Dismiss", color = TextPureWhite, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Failed to render screenshot bitmap", color = TextSecondaryDark)
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(isoString: String): String {
    return try {
        val clean = isoString.substringBefore('.').substringBefore('Z')
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = format.parse(clean) ?: return "Recent"
        val diffMs = System.currentTimeMillis() - date.time
        val diffSec = (diffMs / 1000).coerceAtLeast(0)
        when {
            diffSec < 60 -> "Just now"
            diffSec < 3600 -> "${diffSec / 60}m ago"
            diffSec < 86400 -> "${diffSec / 3600}h ago"
            else -> "${diffSec / 86400}d ago"
        }
    } catch (_: Exception) {
        "Recent"
    }
}

private fun getActivityTitle(action: String, tool: String?, details: String?): String {
    return when {
        tool == "run_command" -> "Ran Shell Command"
        tool == "take_screenshot" -> "Screenshot captured"
        tool == "lock_pc" -> "Workstation locked"
        tool == "open_browser" || tool == "browser_search" -> "Browser launched"
        tool == "open_application" -> "Opened ${details ?: "application"}"
        tool == "close_application" -> "Closed ${details ?: "application"}"
        tool == "say_text" -> "Spoke text aloud"
        tool == "play_alert" -> "Played alert chime"
        tool == "show_notification" -> "Desktop alert sent"
        tool == "set_volume" -> "Adjusted PC volume"
        tool == "search_files" -> "Searched files"
        tool == "download_file" || action.contains("DOWNLOAD") -> "Downloaded file"
        tool == "upload_file" || action.contains("UPLOAD") -> "Uploaded file"
        else -> action.replace("EXECUTE_", "").replace("AI_TOOL_", "").replace('_', ' ').lowercase().capitalize(java.util.Locale.ROOT)
    }
}

private fun getActivityIcon(tool: String?, action: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        tool == "run_command" -> Icons.Rounded.Terminal
        tool == "take_screenshot" -> Icons.Rounded.CameraAlt
        tool == "lock_pc" -> Icons.Rounded.Lock
        tool == "open_browser" || tool == "browser_search" -> Icons.Rounded.Language
        tool == "open_application" || tool == "close_application" -> Icons.Rounded.Apps
        tool == "say_text" -> Icons.Rounded.RecordVoiceOver
        tool == "play_alert" -> Icons.Rounded.NotificationsActive
        tool == "show_notification" -> Icons.Rounded.NotificationImportant
        tool == "set_volume" -> Icons.Rounded.VolumeUp
        tool == "search_files" || tool == "list_directory" -> Icons.Rounded.Folder
        tool == "download_file" || action.contains("DOWNLOAD") -> Icons.Rounded.Download
        tool == "upload_file" || action.contains("UPLOAD") -> Icons.Rounded.Upload
        else -> Icons.Rounded.CheckCircle
    }
}
