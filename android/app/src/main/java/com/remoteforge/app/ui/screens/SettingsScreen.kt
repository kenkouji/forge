package com.remoteforge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: RemoteForgeRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val networkConfig = repository.networkConfig

    // Appearance State (Collected from reactive StateFlows)
    val appearanceTheme by networkConfig.appearanceThemeFlow.collectAsState()
    val animationIntensity by networkConfig.animationIntensityFlow.collectAsState()
    val reducedMotion by networkConfig.reducedMotionFlow.collectAsState()
    var soundEffects by remember { mutableStateOf(networkConfig.soundEffects) }
    val forgeColors = ForgeTheme.colors

    // Connection & Auth State
    var serverUrlInput by remember { mutableStateOf(repository.serverUrl) }
    var usernameInput by remember { mutableStateOf(networkConfig.savedUsername ?: "admin") }
    var passwordInput by remember { mutableStateOf(networkConfig.savedPassword ?: "remoteforge_secure_pass") }
    var pingResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var currentMode by remember { mutableStateOf(networkConfig.connectionMode) }
    var isVerifyingTransport by remember { mutableStateOf(false) }
    var transportVerificationMsg by remember { mutableStateOf<String?>(null) }

    // Expandable states
    var showServerConfig by remember { mutableStateOf(false) }
    var showDiagnosticsDetails by remember { mutableStateOf(false) }

    val isAuthenticated = repository.authToken != null
    val connectionState by repository.connectionState.collectAsState()
    val isConnected = connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.PC_ONLINE || isAuthenticated
    val selectedDev by repository.selectedDevice.collectAsState()

    val displayHost = repository.serverUrl
        .removePrefix("http://")
        .removePrefix("https://")
        .ifEmpty { "remoteforge.local:8090" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. Header with Custom Emblem
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
                        text = "Preferences",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = forgeColors.textPrimary,
                        fontSize = 24.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(forgeColors.surfaceSecondary)
                    .border(0.5.dp, forgeColors.borderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("v0.1.0", color = if (forgeColors.isDark) LavenderAccent else LightAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ==========================================
        // 2. Group 1: Appearance (Apple-style grouped)
        // ==========================================
        ForgeSectionHeader(title = "Appearance", subtitle = "Theme, motion, and ambient visual identity")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Theme Selector: Dark, Light, System
                Text("Theme Mode", color = forgeColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DARK" to "Dark Mode", "LIGHT" to "Light Mode", "SYSTEM" to "System").forEach { (mode, label) ->
                        val isSelected = appearanceTheme.uppercase() == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    networkConfig.appearanceTheme = mode
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                if (forgeColors.isDark) RoyalViolet.copy(alpha = 0.35f) else LightAccentSoft.copy(alpha = 0.45f)
                            } else forgeColors.surfaceSecondary,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) (if (forgeColors.isDark) LavenderAccent else LightAccent) else forgeColors.borderSubtle
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) forgeColors.textPrimary else forgeColors.textSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = forgeColors.borderSubtle, thickness = 0.5.dp)

                // Background Animation Intensity: Full, Subtle, Off
                Text("Background Ambient Glow", color = forgeColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FULL" to "Breathing Full", "SUBTLE" to "Subtle", "OFF" to "Disabled").forEach { (intensity, label) ->
                        val isSelected = animationIntensity.uppercase() == intensity
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    networkConfig.animationIntensity = intensity
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                if (forgeColors.isDark) RoyalViolet.copy(alpha = 0.35f) else LightAccentSoft.copy(alpha = 0.45f)
                            } else forgeColors.surfaceSecondary,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) (if (forgeColors.isDark) LavenderAccent else LightAccent) else forgeColors.borderSubtle
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) forgeColors.textPrimary else forgeColors.textSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = forgeColors.borderSubtle, thickness = 0.5.dp)

                // Reduced Motion Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced Motion", color = forgeColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Disable transition morphing and animated effects", color = forgeColors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = reducedMotion,
                        onCheckedChange = {
                            networkConfig.reducedMotion = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (forgeColors.isDark) RoyalViolet else LightAccent
                        )
                    )
                }

                HorizontalDivider(color = forgeColors.borderSubtle, thickness = 0.5.dp)

                // Sound Effects Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sound Effects", color = forgeColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Optional chime on critical command completions", color = forgeColors.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = soundEffects,
                        onCheckedChange = {
                            soundEffects = it
                            networkConfig.soundEffects = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (forgeColors.isDark) RoyalViolet else LightAccent
                        )
                    )
                }
            }
        }

        // ==========================================
        // 3. Group 2: Devices & Connection
        // ==========================================
        ForgeSectionHeader(title = "Devices & Connection", subtitle = "NetBird VPN and server transport settings")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) AccentEmerald else AccentRuby)
                        )
                        Column {
                            Text(
                                text = if (isConnected) "Server Connected" else "Server Disconnected",
                                color = TextPureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = displayHost,
                                color = TextRestrainedGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (isConnected) {
                                repository.setAuthToken(null)
                            } else {
                                coroutineScope.launch { repository.autoConnect() }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.8.dp, if (isConnected) ObsidianBorder else RoyalViolet),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isConnected) Color.Transparent else RoyalViolet.copy(alpha = 0.2f),
                            contentColor = if (isConnected) TextSoftWhite else LavenderAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(text = if (isConnected) "Disconnect" else "Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Server URL input toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showServerConfig = !showServerConfig }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showServerConfig) "Hide Server URL Presets" else "Configure Server Base URL & Presets",
                        fontSize = 11.sp,
                        color = LavenderAccent,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (showServerConfig) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = LavenderAccent,
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
                                focusedBorderColor = RoyalViolet,
                                focusedTextColor = TextPureWhite,
                                unfocusedTextColor = TextPureWhite
                            ),
                            singleLine = true
                        )

                        // Quick Presets
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
                                    color = if (serverUrlInput == presetUrl) RoyalViolet.copy(alpha = 0.25f) else ObsidianSurfaceElevated,
                                    border = BorderStroke(0.5.dp, if (serverUrlInput == presetUrl) LavenderAccent else ObsidianBorderSubtle),
                                    modifier = Modifier.clickable {
                                        serverUrlInput = presetUrl
                                        repository.setServerUrl(presetUrl)
                                    }
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 11.sp,
                                        color = if (serverUrlInput == presetUrl) LavenderAccent else TextSoftWhite,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                // 5-Point Transport Verification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transport Diagnostics", color = TextPureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Probe NetBird interface, TCP socket, and agent health", color = TextRestrainedGray, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isVerifyingTransport = true
                                val res = repository.vpnManager.verifyTransportHealth()
                                transportVerificationMsg = if (res.isMacReachable) "5/5 checks passed: reachable (${res.latencyMs}ms)" else res.failureReason ?: "Transport probe unreachable"
                                isVerifyingTransport = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isVerifyingTransport) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PearlWhite, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Verify Route", color = PearlWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                transportVerificationMsg?.let { msg ->
                    Text(text = msg, color = if (msg.contains("passed")) AccentEmerald else AccentAmber, fontSize = 11.sp)
                }
            }
        }

        // ==========================================
        // 4. Group 3: Security & Privacy
        // ==========================================
        ForgeSectionHeader(title = "Security & Privacy", subtitle = "JWT credentials and sandbox boundary")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isAuthenticated) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Signed In as Admin", color = TextPureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Active Bearer JWT Session", color = AccentEmerald, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { repository.setAuthToken(null) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentRuby.copy(alpha = 0.5f))
                        ) {
                            Text("Log Out", color = AccentRuby, fontSize = 11.sp)
                        }
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
                            focusedBorderColor = RoyalViolet,
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
                            focusedBorderColor = RoyalViolet,
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
                                if (!success) authError = err ?: "Login failed"
                                isAuthenticating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PearlWhite, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Sign In", color = PearlWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    authError?.let { Text(it, color = AccentRuby, fontSize = 11.sp) }
                }

                HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                Text("Sandbox Allowlist Roots", color = TextPureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Desktop, Documents, Downloads, Pictures, Projects (Zero root / OS access)", color = TextRestrainedGray, fontSize = 11.sp)
                Text("Zero Persistent Server Storage: File transfers stream directly via encrypted in-memory chunks.", color = LavenderAccent.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }

        // ==========================================
        // 5. Group 4: Performance & Streaming
        // ==========================================
        ForgeSectionHeader(title = "Performance", subtitle = "Screen capture pipeline and hardware parameters")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Screen Mirroring Quality", color = TextPureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("1280x800 nominal 1x CoreGraphics capture. Dynamically drops stale frames to preserve <250ms latency.", color = TextRestrainedGray, fontSize = 11.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Frame Rate", color = TextSoftWhite, fontSize = 12.sp)
                    Text("15 FPS (Optimal over VPN)", color = LavenderAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("In-Flight Delta Accumulator", color = TextSoftWhite, fontSize = 12.sp)
                    Text("Active (Zero gliding lag)", color = AccentEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==========================================
        // 6. Group 5: About
        // ==========================================
        ForgeSectionHeader(title = "About", subtitle = "Architecture and platform specifications")

        ForgeGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RemoteForgeEmblem(size = 32.dp)
                    Column {
                        Text("RemoteForge Native Android", color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Apple Liquid Glass × Obsidian Design System", color = LavenderAccent, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Target Device: Samsung Galaxy S21 FE 5G (SM-G990B2)", color = TextRestrainedGray, fontSize = 11.sp)
                Text("Controlled Workstations: MacBook Pro (macOS) & Acer Aspire 7 (Windows 11)", color = TextRestrainedGray, fontSize = 11.sp)
                Text("Network: NetBird Overlay Mesh VPN (RFC 6598 Carrier-Grade NAT)", color = TextRestrainedGray, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}
