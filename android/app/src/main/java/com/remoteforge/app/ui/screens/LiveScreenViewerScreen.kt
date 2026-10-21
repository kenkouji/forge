package com.remoteforge.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreenViewerScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val devices by repository.devices.collectAsState()
    val device = devices.firstOrNull { it.id == deviceId } ?: repository.selectedDevice.collectAsState().value

    // Streaming state
    var isStreaming by remember { mutableStateOf(true) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var streamFps by remember { mutableStateOf(0) }
    var streamLatencyMs by remember { mutableStateOf(0L) }
    var streamResolution by remember { mutableStateOf("800x500") }
    var isDegraded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Stream Quality configuration (Native 1:1 nominal 1280x800 sharp mapping at 15 FPS)
    var targetWidth by remember { mutableStateOf(1280) }
    var targetQuality by remember { mutableStateOf(65) }
    var targetFps by remember { mutableStateOf(15) }

    // Transformation controls
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isFitToScreen by remember { mutableStateOf(true) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // PC Control Mode: "touch" (direct screen tap) or "trackpad" (glide cursor)
    var controlMode by remember { mutableStateOf("touch") }
    var isDragLock by remember { mutableStateOf(false) }
    var trackpadSensitivity by remember { mutableStateOf(0.42f) }

    // Trackpad In-Flight Delta Accumulator (Eliminates Gliding / Request Backlog)
    var pendingDx by remember { mutableStateOf(0f) }
    var pendingDy by remember { mutableStateOf(0f) }
    var isSendingMove by remember { mutableStateOf(false) }

    fun flushTrackpadMove() {
        if (isSendingMove) return
        val mx = pendingDx
        val my = pendingDy
        if (kotlin.math.abs(mx) < 0.2f && kotlin.math.abs(my) < 0.2f) return
        pendingDx = 0f
        pendingDy = 0f
        isSendingMove = true
        coroutineScope.launch {
            try {
                val action = if (isDragLock) "mouse_drag" else "mouse_move_relative"
                repository.sendInputEvent(
                    deviceId,
                    mapOf(
                        "action" to action,
                        "dx" to mx.toDouble(),
                        "dy" to my.toDouble(),
                        "timestamp" to (System.currentTimeMillis() / 1000.0)
                    )
                )
            } finally {
                isSendingMove = false
                if (kotlin.math.abs(pendingDx) >= 0.3f || kotlin.math.abs(pendingDy) >= 0.3f) {
                    flushTrackpadMove()
                }
            }
        }
    }

    // Input Control overlay
    var showInputControls by remember { mutableStateOf(true) }
    var showKeyboardBar by remember { mutableStateOf(false) }
    var cmdActive by remember { mutableStateOf(false) }
    var optActive by remember { mutableStateOf(false) }
    var ctrlActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var actionNotice by remember { mutableStateOf<String?>(null) }
    var accessibilityNotice by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

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
            val (ok, err) = repository.sendInputEvent(deviceId, payload)
            if (!ok && err != null) {
                if (err.contains("accessibility", ignoreCase = true)) {
                    accessibilityNotice = true
                }
                actionNotice = err
            }
        }
    }

    // Precise Coordinate Translation from Screen Tap to Mac Coordinates
    fun calculateRelativeCoordinates(tapOffset: Offset): Pair<Double, Double>? {
        val bmp = currentBitmap ?: return null
        if (containerSize.width <= 0 || containerSize.height <= 0) return null

        val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
        val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat()

        val (drawWidth, drawHeight) = if (containerRatio > bmpRatio) {
            (containerSize.height * bmpRatio) to containerSize.height.toFloat()
        } else {
            containerSize.width.toFloat() to (containerSize.width / bmpRatio)
        }

        val left = (containerSize.width - drawWidth) / 2f
        val top = (containerSize.height - drawHeight) / 2f

        // Reverse zoom and pan transforms
        val centerX = containerSize.width / 2f
        val centerY = containerSize.height / 2f
        val untransformedX = (tapOffset.x - centerX - offset.x) / scale + centerX
        val untransformedY = (tapOffset.y - centerY - offset.y) / scale + centerY

        val relX = ((untransformedX - left) / drawWidth).coerceIn(0f, 1f).toDouble()
        val relY = ((untransformedY - top) / drawHeight).coerceIn(0f, 1f).toDouble()
        return Pair(relX, relY)
    }

    // Ultra-Fast, Low-Latency Screen Frame Receiver Loop
    LaunchedEffect(isStreaming, targetWidth, targetQuality, targetFps) {
        if (!isStreaming) return@LaunchedEffect

        var frameCounter = 0
        var lastFpsCheck = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            val streamUrl = repository.getStreamUrl(deviceId) + "&width=$targetWidth&quality=$targetQuality&fps=$targetFps"
            val okHttpClient = repository.networkConfig.okHttpClient

            while (isActive && isStreaming) {
                var response: okhttp3.Response? = null

                try {
                    val request = okhttp3.Request.Builder()
                        .url(streamUrl)
                        .header("Authorization", "Bearer ${repository.authToken ?: ""}")
                        .build()

                    response = okHttpClient.newCall(request).execute()

                    if (response.isSuccessful && response.body != null) {
                        val inputStream: InputStream = BufferedInputStream(response.body!!.byteStream(), 256 * 1024)
                        val buffer = ByteArray(1024 * 1024)

                        while (isActive && isStreaming) {
                            val t0 = System.currentTimeMillis()

                            // Robust boundary & Content-Length header parsing
                            var contentLength = -1
                            while (isActive && isStreaming) {
                                val line = readHeaderLine(inputStream) ?: break
                                if (line.isEmpty() && contentLength > 0) {
                                    break // Empty line separates headers from binary JPEG
                                }
                                val lower = line.lowercase()
                                if (lower.startsWith("content-length:")) {
                                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: -1
                                }
                            }

                            if (contentLength <= 0 || contentLength > buffer.size) {
                                continue
                            }

                            var bytesRead = 0
                            while (bytesRead < contentLength) {
                                val r = inputStream.read(buffer, bytesRead, contentLength - bytesRead)
                                if (r == -1) break
                                bytesRead += r
                            }

                            if (bytesRead == contentLength) {
                                val decodeOptions = BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.RGB_565 // 2x faster decode, optimal for mobile
                                }
                                val bmp = BitmapFactory.decodeByteArray(buffer, 0, contentLength, decodeOptions)
                                if (bmp != null) {
                                    val elapsed = System.currentTimeMillis() - t0
                                    withContext(Dispatchers.Main) {
                                        currentBitmap = bmp
                                        streamResolution = "${bmp.width}x${bmp.height}"
                                        streamLatencyMs = elapsed
                                        isDegraded = false
                                        errorMessage = null
                                    }
                                    frameCounter++
                                }
                            }

                            val now = System.currentTimeMillis()
                            if (now - lastFpsCheck >= 1000) {
                                withContext(Dispatchers.Main) {
                                    streamFps = frameCounter
                                }
                                frameCounter = 0
                                lastFpsCheck = now
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isDegraded = true
                            errorMessage = "Stream disconnected (HTTP ${response.code})"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isDegraded = true
                        errorMessage = e.localizedMessage ?: "Stream connection error"
                    }
                } finally {
                    try { response?.close() } catch (_: Exception) {}
                }

                if (isActive && isStreaming) {
                    delay(1000)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (!isDegraded && currentBitmap != null) EmeraldOnline else RubyOffline)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = device?.name ?: "Remote Mac Screen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$streamFps FPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (streamFps >= 10) EmeraldOnline else AmberWarning,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${streamLatencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalViolet,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = streamResolution,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedDark
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = RoyalViolet)
                    }
                },
                actions = {
                    // Control Mode Switch (Direct Touch vs Trackpad Drag)
                    IconButton(onClick = {
                        controlMode = if (controlMode == "touch") "trackpad" else "touch"
                        actionNotice = if (controlMode == "touch") "Direct Touch Mode: Tap to click anywhere" else "Trackpad Mode: Drag to glide cursor"
                        triggerHaptic()
                    }) {
                        Icon(
                            imageVector = if (controlMode == "touch") Icons.Rounded.TouchApp else Icons.Rounded.Navigation,
                            contentDescription = "Toggle Control Mode",
                            tint = if (controlMode == "touch") RoyalViolet else NeonIndigo
                        )
                    }

                    // Virtual Keyboard Toggle
                    IconButton(onClick = {
                        showKeyboardBar = !showKeyboardBar
                        triggerHaptic()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = "Toggle Keyboard Bar",
                            tint = if (showKeyboardBar) RoyalViolet else TextMutedDark
                        )
                    }

                    // Fit to screen toggle
                    IconButton(onClick = {
                        isFitToScreen = !isFitToScreen
                        scale = 1f
                        offset = Offset.Zero
                        triggerHaptic()
                    }) {
                        Icon(
                            imageVector = if (isFitToScreen) Icons.Rounded.FitScreen else Icons.Rounded.ZoomOutMap,
                            contentDescription = "Fit/Fill",
                            tint = if (isFitToScreen) RoyalViolet else TextMutedDark
                        )
                    }

                    // Quality Preset Toggle (Cycles 640p -> 800p -> 960p)
                    IconButton(onClick = {
                        when (targetWidth) {
                            640 -> {
                                targetWidth = 800
                                targetQuality = 40
                                targetFps = 12
                                actionNotice = "Stream Quality: Balanced (800p, 12 FPS)"
                            }
                            800 -> {
                                targetWidth = 960
                                targetQuality = 55
                                targetFps = 15
                                actionNotice = "Stream Quality: High Definition (960p, 15 FPS)"
                            }
                            else -> {
                                targetWidth = 640
                                targetQuality = 30
                                targetFps = 12
                                actionNotice = "Stream Quality: Ultra-Low Latency / Mesh (640p)"
                            }
                        }
                        triggerHaptic()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "Quality Preset",
                            tint = RoyalViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBackground)
            )
        },
        containerColor = ObsidianBackground
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // STREAM CANVAS WITH INTERACTIVE CONTROLS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(controlMode, containerSize, isDragLock, trackpadSensitivity) {
                        if (controlMode == "touch") {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    triggerHaptic()
                                    val coords = calculateRelativeCoordinates(tapOffset)
                                    if (coords != null) {
                                        sendInput(
                                            "mouse_click",
                                            mapOf("rel_x" to coords.first, "rel_y" to coords.second, "button" to "left")
                                        )
                                    }
                                },
                                onDoubleTap = { tapOffset ->
                                    triggerHaptic()
                                    val coords = calculateRelativeCoordinates(tapOffset)
                                    if (coords != null) {
                                        sendInput(
                                            "mouse_double_click",
                                            mapOf("rel_x" to coords.first, "rel_y" to coords.second, "button" to "left")
                                        )
                                    }
                                },
                                onLongPress = { tapOffset ->
                                    triggerHaptic()
                                    val coords = calculateRelativeCoordinates(tapOffset)
                                    if (coords != null) {
                                        sendInput(
                                            "mouse_click",
                                            mapOf("rel_x" to coords.first, "rel_y" to coords.second, "button" to "right")
                                        )
                                    }
                                }
                            )
                        } else {
                            // Unified Trackpad Mode: Smooth Drag to Glide + Tap to Click (Zero Latency, Zero Gliding Backlog)
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var isDrag = false
                                var totalDistance = 0f
                                val startTime = System.currentTimeMillis()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        break
                                    }

                                    val delta = change.positionChange()
                                    totalDistance += kotlin.math.hypot(delta.x, delta.y)

                                    if (totalDistance > 6f) {
                                        isDrag = true
                                        change.consume()
                                        pendingDx += delta.x * trackpadSensitivity
                                        pendingDy += delta.y * trackpadSensitivity
                                        flushTrackpadMove()
                                    }
                                }

                                pendingDx = 0f
                                pendingDy = 0f

                                // Tap to left-click if finger lifted without dragging
                                if (!isDrag && totalDistance <= 6f && (System.currentTimeMillis() - startTime) < 400L) {
                                    triggerHaptic()
                                    sendInput("mouse_click", mapOf("button" to "left"))
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "Live Mac Screen",
                        contentScale = if (isFitToScreen) ContentScale.Fit else ContentScale.None,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = RoyalViolet, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (errorMessage != null) errorMessage!! else "Connecting to live video pipeline...",
                            color = TextMutedDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // TOP STATUS & NOTICE BANNER
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (actionNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ObsidianSurface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, GlassBorderHighlight)
                    ) {
                        Text(
                            text = actionNotice!!,
                            color = RoyalViolet,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                if (accessibilityNotice) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmberWarning.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AmberWarning)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "macOS Accessibility needed for clicks & keys",
                                color = AmberWarning,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    sendInput("open_accessibility")
                                    actionNotice = "Opened Accessibility Settings on Mac"
                                    triggerHaptic()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Open on Mac", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // BOTTOM CONTROL DOCK & OVERLAY
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TRACKPAD BOTTOM BAR (When in Trackpad Mode)
                AnimatedVisibility(
                    visible = controlMode == "trackpad",
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface.copy(alpha = 0.92f))
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Sensitivity quick selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sensitivity",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedDark,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0.3f to "0.3x", 0.42f to "0.4x", 0.7f to "0.7x", 1.0f to "1.0x").forEach { (speed, label) ->
                                    val isSelected = trackpadSensitivity == speed
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) RoyalViolet.copy(alpha = 0.2f) else ObsidianBackground)
                                            .border(1.dp, if (isSelected) RoyalViolet else GlassBorderSubtle, RoundedCornerShape(6.dp))
                                            .clickable {
                                                trackpadSensitivity = speed
                                                triggerHaptic()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) RoyalViolet else TextMutedDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        Button(
                            onClick = {
                                triggerHaptic()
                                sendInput("mouse_click", mapOf("button" to "left"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBackground),
                            border = BorderStroke(1.dp, GlassBorderHighlight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("LEFT CLICK", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                isDragLock = !isDragLock
                                triggerHaptic()
                                actionNotice = if (isDragLock) "Drag lock ON: Cursor holds mouse button" else "Drag lock OFF"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDragLock) AmberWarning.copy(alpha = 0.25f) else ObsidianBackground
                            ),
                            border = BorderStroke(1.dp, if (isDragLock) AmberWarning else GlassBorderSubtle),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DRAG", color = if (isDragLock) AmberWarning else TextMutedDark, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                triggerHaptic()
                                sendInput("mouse_click", mapOf("button" to "right"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBackground),
                            border = BorderStroke(1.dp, GlassBorderHighlight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("RIGHT CLICK", fontWeight = FontWeight.Bold, color = RoyalViolet, fontSize = 12.sp)
                        }
                    }
                }
            }

                // VIRTUAL KEYBOARD & QUICK SHORTCUTS DOCK
                AnimatedVisibility(
                    visible = showKeyboardBar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(ObsidianSurface.copy(alpha = 0.95f))
                            .border(1.dp, GlassBorderHighlight, RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Text input field for direct typing to Mac
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Type text to send to Mac...", fontSize = 13.sp, color = TextMutedDark) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (textInput.isNotEmpty()) {
                                        sendInput("type_text", mapOf("text" to textInput))
                                        textInput = ""
                                        triggerHaptic()
                                    }
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalViolet,
                                    unfocusedBorderColor = GlassBorderSubtle,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Button(
                                onClick = {
                                    if (textInput.isNotEmpty()) {
                                        sendInput("type_text", mapOf("text" to textInput))
                                        textInput = ""
                                        triggerHaptic()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Send", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Modifier Keys Row (Cmd, Opt, Ctrl, Shift)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ModifierChip("⌘ Cmd", cmdActive, RoyalViolet, Modifier.weight(1f)) {
                                cmdActive = !cmdActive
                                triggerHaptic()
                            }
                            ModifierChip("⌥ Opt", optActive, AmberWarning, Modifier.weight(1f)) {
                                optActive = !optActive
                                triggerHaptic()
                            }
                            ModifierChip("⌃ Ctrl", ctrlActive, NeonIndigo, Modifier.weight(1f)) {
                                ctrlActive = !ctrlActive
                                triggerHaptic()
                            }
                            ModifierChip("⇧ Shift", shiftActive, EmeraldOnline, Modifier.weight(1f)) {
                                shiftActive = !shiftActive
                                triggerHaptic()
                            }
                        }

                        // Essential Mac Navigation Keys
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KeyChip("Esc", Modifier.weight(1f)) { sendInput("key_tap", mapOf("key" to "escape")) }
                            KeyChip("Tab", Modifier.weight(1f)) { sendInput("key_tap", mapOf("key" to "tab")) }
                            KeyChip("Space", Modifier.weight(1.2f)) { sendInput("key_tap", mapOf("key" to "space")) }
                            KeyChip("⌫ Del", Modifier.weight(1f)) { sendInput("key_tap", mapOf("key" to "backspace")) }
                            KeyChip("⏎ Enter", Modifier.weight(1.2f)) { sendInput("key_tap", mapOf("key" to "enter")) }
                        }

                        // Quick Mac Productivity Shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KeyChip("⌘ Space (Search)", Modifier.weight(1.5f)) {
                                sendInput("shortcut", mapOf("name" to "Cmd+Space"))
                                actionNotice = "Triggered Spotlight Search"
                            }
                            KeyChip("⌘ Tab (Apps)", Modifier.weight(1.2f)) {
                                sendInput("shortcut", mapOf("name" to "Cmd+Tab"))
                                actionNotice = "Triggered App Switcher"
                            }
                            KeyChip("Copy", Modifier.weight(1f)) { sendInput("shortcut", mapOf("name" to "Cmd+C")) }
                            KeyChip("Paste", Modifier.weight(1f)) { sendInput("shortcut", mapOf("name" to "Cmd+V")) }
                            KeyChip("Undo", Modifier.weight(1f)) { sendInput("shortcut", mapOf("name" to "Cmd+Z")) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierChip(
    label: String,
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) color.copy(alpha = 0.28f) else ObsidianBackground)
            .border(1.dp, if (active) color else GlassBorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) color else TextPrimaryDark
        )
    }
}

@Composable
private fun KeyChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBackground)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimaryDark,
            fontSize = 11.sp
        )
    }
}

private fun readHeaderLine(inputStream: InputStream): String? {
    val sb = StringBuilder()
    var c: Int
    while (inputStream.read().also { c = it } != -1) {
        if (c == '\n'.code) break
        if (c != '\r'.code) sb.append(c.toChar())
    }
    return if (sb.isEmpty() && c == -1) null else sb.toString()
}
