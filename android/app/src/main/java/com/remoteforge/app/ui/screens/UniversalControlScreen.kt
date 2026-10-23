package com.remoteforge.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.GlassCard
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalControlScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 24/7 Universal Control active by default on RIGHT edge
    var configuredEdge by remember { mutableStateOf("RIGHT") }
    var sensitivity by remember { mutableFloatStateOf(1.0f) }
    var dwellDelayMs by remember { mutableIntStateOf(40) }
    var enableKeyboard by remember { mutableStateOf(true) }
    var enableClipboard by remember { mutableStateOf(true) }
    var isEnabled by remember { mutableStateOf(true) }
    var isRemoteActive by remember { mutableStateOf(false) }

    var cursorNormX by remember { mutableFloatStateOf(0.5f) }
    var cursorNormY by remember { mutableFloatStateOf(0.5f) }
    var isClicking by remember { mutableStateOf(false) }
    var lastActionNotice by remember { mutableStateOf("Ready: Move mouse past laptop right edge") }
    var typedBuffer by remember { mutableStateOf("Type anywhere on your laptop when mouse is on phone...") }

    var edgeWebSocket by remember { mutableStateOf<WebSocket?>(null) }
    val mainScrollState = rememberScrollState()

    // Edge glow infinite animation
    val infiniteTransition = rememberInfiniteTransition(label = "EdgeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Load initial status
    LaunchedEffect(deviceId) {
        val status = repository.getEdgeControlStatus(deviceId)
        if (status != null) {
            configuredEdge = status.configuredEdge
            isEnabled = status.isEnabled
            isRemoteActive = status.isRemoteActive
        }
    }

    // Connect to ultra-low latency WebSocket for 24/7 mouse hopping, clicks, scroll, and keyboard stream
    DisposableEffect(deviceId) {
        val serverUrl = repository.networkConfig.serverUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')
        val wsUrl = "$serverUrl/ws/edge-control/$deviceId"
        val client = repository.networkConfig.okHttpClient
        val request = Request.Builder().url(wsUrl).build()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val obj = JSONObject(text)
                    val type = obj.optString("type")
                    val event = obj.optString("event")

                    if (type == "edge_handoff") {
                        if (event == "ENTER_REMOTE") {
                            isRemoteActive = true
                            cursorNormX = obj.optDouble("norm_x", 0.05).toFloat()
                            cursorNormY = obj.optDouble("norm_y", 0.5).toFloat()
                            lastActionNotice = "Mouse Hopped to Phone!"
                        } else if (event == "EXIT_REMOTE") {
                            isRemoteActive = false
                            isClicking = false
                            lastActionNotice = "Mouse Returned to Laptop"
                        }
                    } else if (type == "pointer_move") {
                        isRemoteActive = true
                        val nx = obj.optDouble("norm_x", -1.0)
                        val ny = obj.optDouble("norm_y", -1.0)
                        if (nx >= 0 && ny >= 0) {
                            cursorNormX = nx.toFloat()
                            cursorNormY = ny.toFloat()
                        } else {
                            val dx = obj.optDouble("dx", 0.0).toFloat()
                            val dy = obj.optDouble("dy", 0.0).toFloat()
                            cursorNormX = (cursorNormX + (dx / 220f)).coerceIn(0.01f, 0.99f)
                            cursorNormY = (cursorNormY + (dy / 220f)).coerceIn(0.01f, 0.99f)
                        }
                    } else if (type == "mouse_click") {
                        val btn = obj.optString("button", "left")
                        val action = obj.optString("action", "click")
                        isClicking = action == "down"
                        lastActionNotice = "${btn.replaceFirstChar { it.uppercase() }} Click"
                    } else if (type == "mouse_scroll") {
                        val sdy = obj.optDouble("dy", 0.0).toFloat()
                        lastActionNotice = "Mouse Scroll: ${if (sdy > 0) "Up" else "Down"}"
                        coroutineScope.launch {
                            mainScrollState.scrollBy((-sdy * 120f))
                        }
                    } else if (type == "keyboard_key") {
                        val key = obj.optString("key", "")
                        val action = obj.optString("action", "press")
                        if (action == "press") {
                            lastActionNotice = "Keystroke: $key"
                            if (key == "Key.backspace" || key.equals("backspace", ignoreCase = true)) {
                                if (typedBuffer.isNotEmpty()) {
                                    typedBuffer = typedBuffer.dropLast(1)
                                }
                            } else if (key == "Key.space" || key.equals("space", ignoreCase = true)) {
                                typedBuffer += " "
                            } else if (key == "Key.enter" || key.equals("enter", ignoreCase = true)) {
                                typedBuffer += "\n"
                            } else if (key == "Key.tab" || key.equals("tab", ignoreCase = true)) {
                                typedBuffer += "    "
                            } else if (key.length == 1) {
                                if (typedBuffer.startsWith("Type anywhere")) {
                                    typedBuffer = ""
                                }
                                typedBuffer += key
                            }
                        }
                    }
                } catch (ignored: Exception) {}
            }
        })
        edgeWebSocket = ws

        onDispose {
            try {
                ws.close(1000, "Screen disposed")
            } catch (ignored: Exception) {}
            edgeWebSocket = null
        }
    }

    fun snapBackToPc() {
        coroutineScope.launch {
            try {
                edgeWebSocket?.send(JSONObject().put("type", "EXIT_REMOTE").toString())
            } catch (ignored: Exception) {}
            repository.sendEdgeHandoff(deviceId, "EXIT_REMOTE", 0.5f, 0.5f)
            isRemoteActive = false
            Toast.makeText(context, "Cursor Snapped Back to Laptop", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyConfiguration() {
        coroutineScope.launch {
            val res = repository.configureEdgeControl(
                deviceId = deviceId,
                edge = configuredEdge,
                sensitivity = sensitivity,
                dwellDelayMs = dwellDelayMs,
                enableKeyboard = enableKeyboard,
                enableClipboardSync = enableClipboard
            )
            if (res != null) {
                isEnabled = res.isEnabled
                Toast.makeText(context, "Universal Control on $configuredEdge Edge", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Universal Control",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF4ECCA3).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF4ECCA3).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "24/7 ACTIVE",
                                    color = Color(0xFF4ECCA3),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Seamless Mouse, Scroll & Keyboard Hopping",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0B18)
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0C0816))
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // Edge Barrier Glow indicating active hop border
            val edgeBrush = remember(configuredEdge, glowAlpha) {
                when (configuredEdge) {
                    "RIGHT" -> Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, RoyalViolet.copy(alpha = glowAlpha))
                    )
                    "LEFT" -> Brush.horizontalGradient(
                        colors = listOf(RoyalViolet.copy(alpha = glowAlpha), Color.Transparent)
                    )
                    "TOP" -> Brush.verticalGradient(
                        colors = listOf(RoyalViolet.copy(alpha = glowAlpha), Color.Transparent)
                    )
                    else -> Brush.verticalGradient(
                        colors = listOf(Color.Transparent, RoyalViolet.copy(alpha = glowAlpha))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when (configuredEdge) {
                            "RIGHT" -> Modifier.align(Alignment.CenterEnd).width(20.dp).fillMaxHeight()
                            "LEFT" -> Modifier.align(Alignment.CenterStart).width(20.dp).fillMaxHeight()
                            "TOP" -> Modifier.align(Alignment.TopCenter).height(20.dp).fillMaxWidth()
                            else -> Modifier.align(Alignment.BottomCenter).height(20.dp).fillMaxWidth()
                        }
                    )
                    .background(edgeBrush)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(mainScrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Dual-Screen Spatial Visualizer
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SPATIAL SCREEN ARRANGEMENT",
                                color = LavenderMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = lastActionNotice,
                                color = if (isRemoteActive) Color(0xFF4ECCA3) else TextMutedDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Visual Monitor Box & Phone Box Diagram
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Laptop Monitor Box
                            Box(
                                modifier = Modifier
                                    .size(130.dp, 80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1430))
                                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Computer, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Laptop / PC", color = TextPrimaryDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Edge Indicator Arrow
                            Icon(
                                imageVector = when (configuredEdge) {
                                    "RIGHT" -> Icons.Rounded.ArrowForward
                                    "LEFT" -> Icons.Rounded.ArrowBack
                                    "TOP" -> Icons.Rounded.ArrowUpward
                                    else -> Icons.Rounded.ArrowDownward
                                },
                                contentDescription = null,
                                tint = if (isRemoteActive) Color(0xFF4ECCA3) else RoyalViolet.copy(alpha = glowAlpha),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .size(28.dp)
                            )

                            // Phone Box
                            Box(
                                modifier = Modifier
                                    .size(55.dp, 95.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2B1A45))
                                    .border(
                                        2.dp,
                                        if (isRemoteActive) Color(0xFF4ECCA3) else RoyalViolet,
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Phone", color = TextPrimaryDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = if (isRemoteActive)
                                "Mouse & Keyboard Active on Phone! Click, scroll wheel, and type freely."
                            else
                                "Move mouse past the $configuredEdge edge of your laptop screen to hop directly onto your phone.",
                            color = if (isRemoteActive) Color(0xFF4ECCA3) else TextMutedDark,
                            fontSize = 12.sp
                        )
                    }
                }

                // Interactive Phone Surface / Test Pad
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INTERACTIVE SURFACE & KEYBOARD RECEIVER",
                                color = LavenderMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (typedBuffer.isNotEmpty()) {
                                Text(
                                    text = "Clear",
                                    color = LavenderAccent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable { typedBuffer = "" }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF140D22))
                                .border(1.dp, if (isRemoteActive) RoyalViolet else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = typedBuffer,
                                color = if (typedBuffer.startsWith("Type anywhere")) TextMutedDark else TextPrimaryDark,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Edge Position Placement Selector
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PHONE PLACEMENT RELATIVE TO LAPTOP",
                            color = LavenderMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("LEFT", "RIGHT", "TOP", "BOTTOM").forEach { edge ->
                                val isSelected = configuredEdge == edge
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RoyalViolet.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) RoyalViolet else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            configuredEdge = edge
                                            applyConfiguration()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = edge,
                                        color = if (isSelected) TextPrimaryDark else TextMutedDark,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Options & Toggles
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Keyboard Input Forwarding", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Forward typing to phone when mouse is hopped", color = TextMutedDark, fontSize = 11.sp)
                            }
                            Switch(
                                checked = enableKeyboard,
                                onCheckedChange = { enableKeyboard = it; applyConfiguration() }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Universal Clipboard Sync", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Copying across screen border auto-syncs", color = TextMutedDark, fontSize = 11.sp)
                            }
                            Switch(
                                checked = enableClipboard,
                                onCheckedChange = { enableClipboard = it; applyConfiguration() }
                            )
                        }
                    }
                }

                if (isRemoteActive) {
                    Button(
                        onClick = { snapBackToPc() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Snap Cursor Back to Laptop", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            isRemoteActive = true
                            cursorNormX = 0.5f
                            cursorNormY = 0.5f
                            lastActionNotice = "Mouse Hopped to Phone!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, RoyalViolet.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Navigation, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(16.dp).rotate(-45f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activate Mouse on Phone (Test)", color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            // Live Floating Mouse Cursor Arrow when hopped to Phone
            if (isRemoteActive) {
                val posX = screenWidth * cursorNormX
                val posY = screenHeight * cursorNormY

                Box(
                    modifier = Modifier
                        .offset(x = posX.coerceAtLeast(0.dp), y = posY.coerceAtLeast(0.dp))
                        .size(44.dp)
                ) {
                    if (isClicking) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ECCA3).copy(alpha = 0.5f))
                                .border(2.dp, Color(0xFF4ECCA3), CircleShape)
                        )
                    }

                    // High contrast black drop shadow so cursor is always clearly visible
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .size(28.dp)
                            .offset(x = 1.5.dp, y = 1.5.dp)
                            .rotate(-45f)
                    )

                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = "Mouse Pointer",
                        tint = if (isClicking) Color(0xFF4ECCA3) else Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .rotate(-45f)
                    )
                }
            }
        }
    }
}
