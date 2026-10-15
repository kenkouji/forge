package com.remoteforge.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val devices by repository.devices.collectAsState()
    val device = devices.firstOrNull { it.id == deviceId } ?: repository.selectedDevice.collectAsState().value

    // Modifier Key States
    var cmdActive by remember { mutableStateOf(false) }
    var optActive by remember { mutableStateOf(false) }
    var ctrlActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }

    // Touchpad Settings
    var sensitivity by remember { mutableStateOf(1.2f) }
    var isDragLock by remember { mutableStateOf(false) }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var showScrollOverlay by remember { mutableStateOf(false) }

    fun currentModifiersList(): List<String> {
        val list = mutableListOf<String>()
        if (cmdActive) list.add("cmd")
        if (optActive) list.add("opt")
        if (ctrlActive) list.add("ctrl")
        if (shiftActive) list.add("shift")
        return list
    }

    fun sendInput(action: String, extra: Map<String, Any> = emptyMap()) {
        val payload = mutableMapOf<String, Any>(
            "action" to action,
            "modifiers" to currentModifiersList()
        )
        payload.putAll(extra)
        coroutineScope.launch {
            repository.sendInputEvent(deviceId, payload)
        }
    }

    fun triggerHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device?.name ?: "Remote Workstation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (device?.isOnline == true) EmeraldOnline else RubyOffline)
                            )
                        }
                        Text(
                            text = "Live Trackpad & Keyboard Control",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricCyan
                        )
                    }
                },
                actions = {
                    // Emergency Stop / Reset button
                    IconButton(
                        onClick = {
                            cmdActive = false
                            optActive = false
                            ctrlActive = false
                            shiftActive = false
                            isDragLock = false
                            sendInput("reset")
                            actionNotice = "Inputs and modifiers reset"
                            triggerHaptic()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset Inputs",
                            tint = RubyOffline
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notice banner if action performed
            actionNotice?.let { notice ->
                Surface(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(notice, color = ElectricCyan, style = MaterialTheme.typography.bodySmall)
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = ElectricCyan,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { actionNotice = null }
                        )
                    }
                }
            }

            // Touchpad Mode Bar & Sensitivity Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag-Lock toggle
                FilterChip(
                    selected = isDragLock,
                    onClick = {
                        isDragLock = !isDragLock
                        triggerHaptic()
                    },
                    label = {
                        Text(
                            text = if (isDragLock) "DRAG LOCK: ON" else "DRAG LOCK: OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.25f),
                        selectedLabelColor = ElectricCyan,
                        containerColor = ObsidianSurface,
                        labelColor = TextMutedDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isDragLock,
                        borderColor = GlassBorderSubtle,
                        selectedBorderColor = ElectricCyan
                    )
                )

                // Sensitivity selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.8f to "0.8x", 1.2f to "1.2x", 1.8f to "1.8x", 2.4f to "2.4x").forEach { (speed, label) ->
                        val isSelected = sensitivity == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else ObsidianSurface)
                                .border(1.dp, if (isSelected) ElectricCyan else GlassBorderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    sensitivity = speed
                                    triggerHaptic()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) ElectricCyan else TextMutedDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // PRIMARY TOUCHPAD SURFACE
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    triggerHaptic()
                                    sendInput("mouse_click", mapOf("button" to "left"))
                                },
                                onDoubleTap = {
                                    triggerHaptic()
                                    sendInput("mouse_double_click", mapOf("button" to "left"))
                                },
                                onLongPress = {
                                    triggerHaptic()
                                    sendInput("mouse_click", mapOf("button" to "right"))
                                }
                            )
                        }
                        .pointerInput(sensitivity, isDragLock) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x * sensitivity
                                val dy = dragAmount.y * sensitivity
                                val action = if (isDragLock) "mouse_drag" else "mouse_move_relative"
                                sendInput(action, mapOf("dx" to dx.toDouble(), "dy" to dy.toDouble()))
                            }
                        }
                ) {
                    // Center watermark icon
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = GlassBorderHighlight,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Liquid Glass Trackpad Surface",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark
                        )
                        Text(
                            text = "Tap: Left Click • Double Tap: Double Click • Long Press: Right Click",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }

                    // Bottom Physical Left & Right Click Bars
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(ObsidianSurface.copy(alpha = 0.85f))
                            .border(BorderStroke(1.dp, GlassBorderSubtle))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    triggerHaptic()
                                    sendInput("mouse_click", mapOf("button" to "left"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LEFT CLICK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(GlassBorderSubtle)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    triggerHaptic()
                                    sendInput("mouse_click", mapOf("button" to "right"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RIGHT CLICK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }
            }

            // Two-Finger Scroll Bar
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fast Page Scroll", style = MaterialTheme.typography.bodyMedium, color = TextPrimaryDark)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                triggerHaptic()
                                sendInput("mouse_scroll", mapOf("dy" to 5.0, "dx" to 0.0))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, GlassBorderSubtle),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Scroll Up", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Up", color = TextPrimaryDark, style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                triggerHaptic()
                                sendInput("mouse_scroll", mapOf("dy" to -5.0, "dx" to 0.0))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                            border = BorderStroke(1.dp, GlassBorderSubtle),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Scroll Down", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Down", color = TextPrimaryDark, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // MODIFIER KEYS ROW (Command, Option, Control, Shift)
            Text(
                text = "MAC MODIFIER KEYS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Command
                ModifierKeyButton(
                    label = "⌘ Cmd",
                    isActive = cmdActive,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    cmdActive = !cmdActive
                    triggerHaptic()
                }

                // Option
                ModifierKeyButton(
                    label = "⌥ Opt",
                    isActive = optActive,
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                ) {
                    optActive = !optActive
                    triggerHaptic()
                }

                // Control
                ModifierKeyButton(
                    label = "⌃ Ctrl",
                    isActive = ctrlActive,
                    accentColor = NeonIndigo,
                    modifier = Modifier.weight(1f)
                ) {
                    ctrlActive = !ctrlActive
                    triggerHaptic()
                }

                // Shift
                ModifierKeyButton(
                    label = "⇧ Shift",
                    isActive = shiftActive,
                    accentColor = EmeraldOnline,
                    modifier = Modifier.weight(1f)
                ) {
                    shiftActive = !shiftActive
                    triggerHaptic()
                }
            }

            // QUICK SYSTEM SHORTCUTS
            Text(
                text = "PRODUCTIVITY SHORTCUTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShortcutPill(name = "Copy", shortcut = "Cmd+C", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("shortcut", mapOf("name" to "Cmd+C"))
                    actionNotice = "Executed Cmd+C (Copy)"
                }
                ShortcutPill(name = "Paste", shortcut = "Cmd+V", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("shortcut", mapOf("name" to "Cmd+V"))
                    actionNotice = "Executed Cmd+V (Paste)"
                }
                ShortcutPill(name = "Undo", shortcut = "Cmd+Z", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("shortcut", mapOf("name" to "Cmd+Z"))
                    actionNotice = "Executed Cmd+Z (Undo)"
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShortcutPill(name = "App Switcher", shortcut = "Cmd+Tab", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("shortcut", mapOf("name" to "Cmd+Tab"))
                    actionNotice = "Executed Cmd+Tab"
                }
                ShortcutPill(name = "Spotlight", shortcut = "Cmd+Space", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("shortcut", mapOf("name" to "Cmd+Space"))
                    actionNotice = "Launched macOS Spotlight"
                }
                ShortcutPill(name = "Escape", shortcut = "Esc", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("shortcut", mapOf("name" to "Esc"))
                    actionNotice = "Sent Escape key"
                }
            }

            // NAVIGATION ARROW KEYS & ENTER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShortcutPill(name = "Enter", shortcut = "⏎ Return", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("key_tap", mapOf("key" to "enter"))
                }
                ShortcutPill(name = "Backspace", shortcut = "⌫ Del", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("key_tap", mapOf("key" to "backspace"))
                }
                ShortcutPill(name = "Space", shortcut = "Space", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("key_tap", mapOf("key" to "space"))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShortcutPill(name = "Left", shortcut = "←", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("key_tap", mapOf("key" to "left"))
                }
                ShortcutPill(name = "Up", shortcut = "↑", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("key_tap", mapOf("key" to "up"))
                }
                ShortcutPill(name = "Down", shortcut = "↓", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("key_tap", mapOf("key" to "down"))
                }
                ShortcutPill(name = "Right", shortcut = "→", modifier = Modifier.weight(1f)) {
                    triggerHaptic()
                    sendInput("key_tap", mapOf("key" to "right"))
                }
            }

            // QUICK UNICODE TEXT ENTRY FIELD
            Text(
                text = "TYPE TEXT TO MAC",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type words or sentence to insert...", color = TextMutedDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            cursorColor = ElectricCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    triggerHaptic()
                                    sendInput("type_text", mapOf("text" to textInput))
                                    actionNotice = "Typed text on workstation"
                                    textInput = ""
                                }
                            }
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    triggerHaptic()
                                    sendInput("type_text", mapOf("text" to textInput))
                                    actionNotice = "Typed text on workstation"
                                    textInput = ""
                                }
                            },
                            enabled = textInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Type on Mac", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ModifierKeyButton(
    label: String,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.28f) else ObsidianSurface)
            .border(
                1.dp,
                if (isActive) accentColor else GlassBorderSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) accentColor else TextPrimaryDark
        )
    }
}

@Composable
private fun ShortcutPill(
    name: String,
    shortcut: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = shortcut,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedDark,
                fontSize = 10.sp
            )
        }
    }
}
