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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val devices by repository.devices.collectAsState()
    val telemetry by repository.telemetry.collectAsState()
    val device = devices.firstOrNull { it.id == deviceId } ?: repository.selectedDevice.collectAsState().value

    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var isRunningAction by remember { mutableStateOf(false) }

    // Dialog state for remote command execution
    var showCommandDialog by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
    var commandOutput by remember { mutableStateOf<String?>(null) }

    // Dialog state for Close App & Fun commands
    var showCloseAppDialog by remember { mutableStateOf(false) }
    var closeAppName by remember { mutableStateOf("") }
    var showSayTextDialog by remember { mutableStateOf(false) }
    var sayTextInput by remember { mutableStateOf("") }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notifMessageInput by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }

    // Refresh telemetry on launch
    LaunchedEffect(deviceId) {
        repository.fetchTelemetry(deviceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = device?.name ?: "Device Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (device?.isOnline == true) "Online • Low latency" else "Offline • Standby",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (device?.isOnline == true) EmeraldOnline else RubyOffline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = RoyalViolet
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            repository.fetchTelemetry(deviceId)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Telemetry",
                            tint = RoyalViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (device?.isOnline == true) EmeraldOnline.copy(alpha = 0.4f) else RubyOffline.copy(alpha = 0.3f),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val icon = if (device?.osName?.contains("Windows", ignoreCase = true) == true) {
                            Icons.Rounded.LaptopWindows
                        } else if (device?.osName?.contains("Mac", ignoreCase = true) == true ||
                            device?.osName?.contains("Darwin", ignoreCase = true) == true
                        ) {
                            Icons.Rounded.LaptopMac
                        } else {
                            Icons.Rounded.Computer
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(RoyalViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = RoyalViolet,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = device?.name ?: "Unknown Host",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "${device?.osName ?: "Unknown OS"} ${device?.osVersion ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    StatusIndicator(
                        text = if (device?.isOnline == true) "ONLINE" else "OFFLINE",
                        accentColor = if (device?.isOnline == true) EmeraldOnline else RubyOffline,
                        isPulsing = device?.isOnline == true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = GlassBorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("AGENT VERSION", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                        Text(
                            text = "v${device?.agentVersion ?: "1.0.0"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column {
                        Text("DEVICE ID", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                        Text(
                            text = device?.id?.take(12) ?: "---",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column {
                        Text("STATUS", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                        Text(
                            text = if (device?.isOnline == true) "Active" else "Idle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (device?.isOnline == true) EmeraldOnline else TextMutedDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Real-Time Telemetry HUD Section
            Text(
                text = "HARDWARE TELEMETRY & RESOURCES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (telemetry != null) {
                    val t = telemetry!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryRingGauge(
                            title = "CPU",
                            percent = t.cpu.percent,
                            subtitle = "${t.cpu.logicalCores} Cores",
                            accentColor = RoyalViolet
                        )
                        TelemetryRingGauge(
                            title = "RAM",
                            percent = t.ram.percent,
                            subtitle = "${String.format("%.1f", t.ram.usedGb)}/${String.format("%.0f", t.ram.totalGb)} GB",
                            accentColor = NeonIndigo
                        )
                        TelemetryRingGauge(
                            title = "STORAGE",
                            percent = t.disk.percent,
                            subtitle = "${String.format("%.0f", t.disk.usedGb)}/${String.format("%.0f", t.disk.totalGb)} GB",
                            accentColor = EmeraldOnline
                        )
                    }

                    if (t.battery != null || t.network != null || t.thermal != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = GlassBorderSubtle)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Battery
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (t.battery?.powerPlugged == true) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                                    contentDescription = null,
                                    tint = if (t.battery?.powerPlugged == true) EmeraldOnline else AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${t.battery?.percent ?: 0}% ${if (t.battery?.powerPlugged == true) "AC" else "Batt"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Thermals
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Thermostat,
                                    contentDescription = null,
                                    tint = if (t.thermal?.status == "Nominal") EmeraldOnline else RubyOffline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${t.thermal?.status ?: "Nominal"} (${t.thermal?.cpuSpeedLimit ?: 100}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryDark
                                )
                            }

                            // Network throughput
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = RoyalViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "↓${t.network?.rxKbps ?: 0.0} ↑${t.network?.txKbps ?: 0.0} KB/s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (t.uptimeFormatted.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "System Uptime: ${t.uptimeFormatted}",
                                color = TextMutedDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (device?.isOnline == true) "Telemetry ready to poll" else "Host offline. Telemetry unavailable.",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                        if (device?.isOnline == true) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.fetchTelemetry(deviceId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.2f))
                            ) {
                                Text("Query Telemetry HUD", color = RoyalViolet)
                            }
                        }
                    }
                }
            }

            // Privacy Shield & Emergency Lockdown
            PrivacyShieldCard(
                deviceId = deviceId,
                repository = repository
            )

            // Guest Co-Control & Sessions
            GuestSessionsCard(
                deviceId = deviceId,
                repository = repository
            )

            // Quick Actions & Controls Grid
            Text(
                text = "REMOTE CONTROL & OPERATIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            // Invite Browser Guest Banner Action
            QuickActionCard(
                title = "Invite Guest to Workstation",
                subtitle = "Generate single-use QR code for browser co-control",
                icon = Icons.Rounded.QrCode,
                accentColor = RoyalViolet,
                modifier = Modifier.fillMaxWidth()
            ) {
                showInviteDialog = true
            }

            // Primary Trackpad & Live Screen Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Trackpad & Keys",
                    subtitle = "Mouse & shortcuts",
                    icon = Icons.Rounded.TouchApp,
                    accentColor = RoyalViolet,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        onNavigate(AppScreen.RemoteControl(device.id))
                    }
                }

                QuickActionCard(
                    title = "Take Screenshot",
                    subtitle = "Capture live screen",
                    icon = Icons.Rounded.CameraAlt,
                    accentColor = NeonIndigo,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        coroutineScope.launch {
                            isRunningAction = true
                            val res = repository.dispatchQuickAction(device.id, "take_screenshot")
                            actionFeedback = res.second
                            isRunningAction = false
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Lock PC",
                    subtitle = "Secure workstation",
                    icon = Icons.Rounded.Lock,
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        coroutineScope.launch {
                            isRunningAction = true
                            val res = repository.dispatchQuickAction(device.id, "lock_pc")
                            actionFeedback = res.second
                            isRunningAction = false
                        }
                    }
                }

                QuickActionCard(
                    title = "Remote Command",
                    subtitle = "Run terminal tool",
                    icon = Icons.Rounded.Terminal,
                    accentColor = NeonIndigo,
                    modifier = Modifier.weight(1f)
                ) {
                    showCommandDialog = true
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Universal Transfer",
                    subtitle = "Send files to PC",
                    icon = Icons.Rounded.SwapVert,
                    accentColor = EmeraldOnline,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(AppScreen.UniversalTransfer(preselectedSourceDevice = device?.id))
                }

                QuickActionCard(
                    title = "Clipboard History",
                    subtitle = "Auto-sync text & images",
                    icon = Icons.Rounded.ContentPaste,
                    accentColor = RoyalViolet,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        onNavigate(AppScreen.ClipboardHistory(device.id))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Task Manager",
                    subtitle = "Durable task queue",
                    icon = Icons.Rounded.Assignment,
                    accentColor = NeonIndigo,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(AppScreen.TaskManager(device?.id))
                }

                QuickActionCard(
                    title = "Live Screen Mirror",
                    subtitle = "Low-latency stream",
                    icon = Icons.Rounded.ScreenShare,
                    accentColor = RubyOffline,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        onNavigate(AppScreen.LiveScreen(device.id))
                    }
                }
            }

            // Multimedia & Hardware KVM
            Text(
                text = "MULTIMEDIA & HARDWARE KVM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Virtual Display",
                    subtitle = "Secondary touch monitor",
                    icon = Icons.Rounded.Tv,
                    accentColor = RoyalViolet,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        onNavigate(AppScreen.VirtualDisplay(device.id))
                    }
                }

                QuickActionCard(
                    title = "Universal Control",
                    subtitle = "Edge cursor hopping",
                    icon = Icons.Rounded.NearMe,
                    accentColor = NeonIndigo,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        onNavigate(AppScreen.UniversalControl(device.id))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Audio Studio",
                    subtitle = "PC sound & wireless mic",
                    icon = Icons.Rounded.GraphicEq,
                    accentColor = EmeraldOnline,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        onNavigate(AppScreen.AudioStudio(device.id))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }


            // Fun & App Management Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Close App",
                    subtitle = "Quit running app",
                    icon = Icons.Rounded.Close,
                    accentColor = RubyOffline,
                    modifier = Modifier.weight(1f)
                ) {
                    showCloseAppDialog = true
                }

                QuickActionCard(
                    title = "Speak Text",
                    subtitle = "Voice TTS on PC",
                    icon = Icons.Rounded.RecordVoiceOver,
                    accentColor = AccentViolet,
                    modifier = Modifier.weight(1f)
                ) {
                    showSayTextDialog = true
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Play Chime",
                    subtitle = "Alert ping sound",
                    icon = Icons.Rounded.NotificationsActive,
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (device != null) {
                        coroutineScope.launch {
                            val res = repository.dispatchQuickAction(device.id, "play_alert", mapOf("sound" to "Ping"))
                            actionFeedback = if (res.first) "Played chime sound on workstation" else res.second
                        }
                    }
                }

                QuickActionCard(
                    title = "Desktop Alert",
                    subtitle = "Show screen banner",
                    icon = Icons.Rounded.NotificationImportant,
                    accentColor = RoyalViolet,
                    modifier = Modifier.weight(1f)
                ) {
                    showNotificationDialog = true
                }
            }

            // Action Feedback
            actionFeedback?.let { msg ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = RoyalViolet.copy(alpha = 0.4f),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = RoyalViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            color = TextPrimaryDark,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Set as active selected device
            if (device != null && repository.selectedDevice.collectAsState().value?.id != device.id) {
                Button(
                    onClick = {
                        repository.selectDevice(device)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set as Primary Controlling Device", color = RoyalViolet, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Remote Terminal Command Dialog
    if (showCommandDialog) {
        AlertDialog(
            onDismissRequest = { showCommandDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, tint = NeonIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute Remote Tool", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Dispatch a safe sandboxed command to ${device?.name ?: "agent"}:",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )

                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = { Text("e.g. echo 'RemoteForge'", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (device != null && commandInput.isNotBlank()) {
                                coroutineScope.launch {
                                    val res = repository.dispatchQuickAction(
                                        device.id,
                                        "run_command",
                                        mapOf("command" to commandInput)
                                    )
                                    commandOutput = if (res.first) "Output: ${res.second}" else "Error: ${res.second}"
                                }
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalViolet,
                            unfocusedBorderColor = GlassBorderHighlight,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    commandOutput?.let { out ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = GlassSurfaceElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = out,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (device != null && commandInput.isNotBlank()) {
                            coroutineScope.launch {
                                val res = repository.dispatchQuickAction(
                                    device.id,
                                    "run_command",
                                    mapOf("command" to commandInput)
                                )
                                commandOutput = if (res.first) {
                                    "Output: ${res.second}"
                                } else {
                                    "Error: ${res.second}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
                ) {
                    Text("Run Command", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommandDialog = false }) {
                    Text("Close", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF111827),
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Close Application Dialog
    if (showCloseAppDialog) {
        AlertDialog(
            onDismissRequest = { showCloseAppDialog = false },
            title = {
                Text("Close Application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter application name to close on ${device?.name ?: "PC"}:", fontSize = 12.sp, color = TextSecondaryDark)
                    OutlinedTextField(
                        value = closeAppName,
                        onValueChange = { closeAppName = it },
                        placeholder = { Text("e.g. Google Chrome, Spotify, Notes", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RubyOffline,
                            unfocusedBorderColor = GlassBorderHighlight,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (device != null && closeAppName.isNotBlank()) {
                            coroutineScope.launch {
                                val res = repository.dispatchQuickAction(device.id, "close_application", mapOf("app_name" to closeAppName))
                                actionFeedback = if (res.first) "Requested '${closeAppName}' to close" else res.second
                                showCloseAppDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyOffline)
                ) {
                    Text("Close App", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseAppDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF111827),
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Speak Text (TTS) Dialog
    if (showSayTextDialog) {
        AlertDialog(
            onDismissRequest = { showSayTextDialog = false },
            title = {
                Text("Speak Aloud on PC", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Type words or sentence to speak through PC speakers:", fontSize = 12.sp, color = TextSecondaryDark)
                    OutlinedTextField(
                        value = sayTextInput,
                        onValueChange = { sayTextInput = it },
                        placeholder = { Text("e.g. Hello from RemoteForge phone!", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentViolet,
                            unfocusedBorderColor = GlassBorderHighlight,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (device != null && sayTextInput.isNotBlank()) {
                            coroutineScope.launch {
                                val res = repository.dispatchQuickAction(device.id, "say_text", mapOf("text" to sayTextInput))
                                actionFeedback = if (res.first) "Spoke text on workstation" else res.second
                                showSayTextDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)
                ) {
                    Text("Speak", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSayTextDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF111827),
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Desktop Banner Notification Dialog
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = {
                Text("Desktop Notification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter notification banner message to display on workstation screen:", fontSize = 12.sp, color = TextSecondaryDark)
                    OutlinedTextField(
                        value = notifMessageInput,
                        onValueChange = { notifMessageInput = it },
                        placeholder = { Text("e.g. Meeting starting in 5 minutes!", color = TextMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalViolet,
                            unfocusedBorderColor = GlassBorderHighlight,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (device != null && notifMessageInput.isNotBlank()) {
                            coroutineScope.launch {
                                val res = repository.dispatchQuickAction(device.id, "show_notification", mapOf("title" to "RemoteForge", "message" to notifMessageInput))
                                actionFeedback = if (res.first) "Notification shown on PC" else res.second
                                showNotificationDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
                ) {
                    Text("Send Alert", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF111827),
            shape = RoundedCornerShape(22.dp)
        )
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
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(20.dp))
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
                                    onNavigate(AppScreen.ScreenshotViewer(b64, device?.name ?: "MacBook Pro"))
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
                                    onNavigate(AppScreen.ScreenshotViewer(b64, device?.name ?: "MacBook Pro"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
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

    if (showInviteDialog) {
        InviteGuestDialog(
            deviceId = deviceId,
            repository = repository,
            onDismiss = { showInviteDialog = false }
        )
    }
}
