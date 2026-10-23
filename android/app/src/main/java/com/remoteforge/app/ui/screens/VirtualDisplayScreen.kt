package com.remoteforge.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.GlassCard
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualDisplayScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSessionActive by remember { mutableStateOf(false) }
    var selectedRes by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableIntStateOf(60) }
    var latencyMs by remember { mutableFloatStateOf(12.5f) }
    var currentFps by remember { mutableFloatStateOf(60.0f) }
    var showControls by remember { mutableStateOf(true) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Start virtual display on entry
    LaunchedEffect(deviceId, selectedRes, selectedFps) {
        val (w, h) = when (selectedRes) {
            "720p" -> 1280 to 720
            "Phone Native" -> 1080 to 2400
            else -> 1920 to 1080
        }
        val status = repository.startVirtualDisplay(
            deviceId = deviceId,
            width = w,
            height = h,
            fps = selectedFps,
            dpi = 240,
            enableTouch = true
        )
        if (status != null && status.isActive) {
            isSessionActive = true
            latencyMs = status.latencyMs
            currentFps = status.currentFps
        }
    }

    // High performance frame decode loop
    LaunchedEffect(isSessionActive, selectedFps) {
        if (!isSessionActive) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val serverUrl = repository.networkConfig.serverUrl.trimEnd('/')
            val token = repository.networkConfig.authToken ?: ""
            val streamUrl = "$serverUrl/api/virtual-display/feed/$deviceId?fps=$selectedFps&token=$token"
            val okHttpClient = repository.networkConfig.okHttpClient

            while (isActive && isSessionActive) {
                var response: okhttp3.Response? = null
                try {
                    val request = okhttp3.Request.Builder()
                        .url(streamUrl)
                        .header("Authorization", "Bearer $token")
                        .build()

                    response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful && response.body != null) {
                        val inputStream: InputStream = BufferedInputStream(response.body!!.byteStream(), 256 * 1024)
                        val buffer = ByteArray(1024 * 1024)

                        while (isActive && isSessionActive) {
                            var contentLength = -1
                            while (isActive && isSessionActive) {
                                val line = readVirtualDisplayHeaderLine(inputStream) ?: break
                                if (line.isEmpty() && contentLength > 0) {
                                    break // Empty line precedes the JPEG payload
                                }
                                val lower = line.lowercase()
                                if (lower.contains("content-length:")) {
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
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                }
                                val bmp = BitmapFactory.decodeByteArray(buffer, 0, contentLength, decodeOptions)
                                if (bmp != null) {
                                    withContext(Dispatchers.Main) {
                                        currentBitmap = bmp
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    delay(500)
                } finally {
                    try { response?.close() } catch (ignored: Exception) {}
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                repository.stopVirtualDisplay(deviceId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Virtual Display",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$selectedRes • ${selectedFps}Hz Extended Monitor",
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
                actions = {
                    IconButton(onClick = { showControls = !showControls }) {
                        Icon(
                            imageVector = if (showControls) Icons.Rounded.Fullscreen else Icons.Rounded.Tune,
                            contentDescription = "Toggle Controls",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0A0710))
        ) {
            // Live Extended Screen Canvas with Gesture Detection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentBitmap) {
                        detectTapGestures(
                            onTap = { offset ->
                                coroutineScope.launch {
                                    val bmp = currentBitmap
                                    val (normX, normY) = if (bmp != null && size.width > 0 && size.height > 0) {
                                        val scale = minOf(size.width.toFloat() / bmp.width, size.height.toFloat() / bmp.height)
                                        val renW = bmp.width * scale
                                        val renH = bmp.height * scale
                                        val offX = (size.width - renW) / 2f
                                        val offY = (size.height - renH) / 2f
                                        (((offset.x - offX) / renW).coerceIn(0f, 1f)) to (((offset.y - offY) / renH).coerceIn(0f, 1f))
                                    } else {
                                        (offset.x / size.width).coerceIn(0f, 1f) to (offset.y / size.height).coerceIn(0f, 1f)
                                    }
                                    repository.injectVirtualDisplayTouch(deviceId, "click", normX, normY)
                                }
                            },
                            onDoubleTap = { offset ->
                                coroutineScope.launch {
                                    val bmp = currentBitmap
                                    val (normX, normY) = if (bmp != null && size.width > 0 && size.height > 0) {
                                        val scale = minOf(size.width.toFloat() / bmp.width, size.height.toFloat() / bmp.height)
                                        val renW = bmp.width * scale
                                        val renH = bmp.height * scale
                                        val offX = (size.width - renW) / 2f
                                        val offY = (size.height - renH) / 2f
                                        (((offset.x - offX) / renW).coerceIn(0f, 1f)) to (((offset.y - offY) / renH).coerceIn(0f, 1f))
                                    } else {
                                        (offset.x / size.width).coerceIn(0f, 1f) to (offset.y / size.height).coerceIn(0f, 1f)
                                    }
                                    repository.injectVirtualDisplayTouch(deviceId, "double_click", normX, normY)
                                }
                            },
                            onLongPress = { offset ->
                                coroutineScope.launch {
                                    val bmp = currentBitmap
                                    val (normX, normY) = if (bmp != null && size.width > 0 && size.height > 0) {
                                        val scale = minOf(size.width.toFloat() / bmp.width, size.height.toFloat() / bmp.height)
                                        val renW = bmp.width * scale
                                        val renH = bmp.height * scale
                                        val offX = (size.width - renW) / 2f
                                        val offY = (size.height - renH) / 2f
                                        (((offset.x - offX) / renW).coerceIn(0f, 1f)) to (((offset.y - offY) / renH).coerceIn(0f, 1f))
                                    } else {
                                        (offset.x / size.width).coerceIn(0f, 1f) to (offset.y / size.height).coerceIn(0f, 1f)
                                    }
                                    repository.injectVirtualDisplayTouch(deviceId, "right_click", normX, normY)
                                }
                            }
                        )
                    }
                    .pointerInput(currentBitmap) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                coroutineScope.launch {
                                    val bmp = currentBitmap
                                    val (normX, normY) = if (bmp != null && size.width > 0 && size.height > 0) {
                                        val scale = minOf(size.width.toFloat() / bmp.width, size.height.toFloat() / bmp.height)
                                        val renW = bmp.width * scale
                                        val renH = bmp.height * scale
                                        val offX = (size.width - renW) / 2f
                                        val offY = (size.height - renH) / 2f
                                        (((offset.x - offX) / renW).coerceIn(0f, 1f)) to (((offset.y - offY) / renH).coerceIn(0f, 1f))
                                    } else {
                                        (offset.x / size.width).coerceIn(0f, 1f) to (offset.y / size.height).coerceIn(0f, 1f)
                                    }
                                    repository.injectVirtualDisplayTouch(deviceId, "drag_start", normX, normY)
                                }
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    repository.injectVirtualDisplayTouch(deviceId, "drag_end", 0.5f, 0.5f)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val bmp = currentBitmap
                                    val (normX, normY) = if (bmp != null && size.width > 0 && size.height > 0) {
                                        val scale = minOf(size.width.toFloat() / bmp.width, size.height.toFloat() / bmp.height)
                                        val renW = bmp.width * scale
                                        val renH = bmp.height * scale
                                        val offX = (size.width - renW) / 2f
                                        val offY = (size.height - renH) / 2f
                                        (((change.position.x - offX) / renW).coerceIn(0f, 1f)) to (((change.position.y - offY) / renH).coerceIn(0f, 1f))
                                    } else {
                                        (change.position.x / size.width).coerceIn(0f, 1f) to (change.position.y / size.height).coerceIn(0f, 1f)
                                    }
                                    repository.injectVirtualDisplayTouch(
                                        deviceId = deviceId,
                                        action = "move",
                                        normX = normX,
                                        normY = normY,
                                        scrollDx = dragAmount.x,
                                        scrollDy = dragAmount.y
                                    )
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "Virtual Display Stream",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RoyalViolet, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Connecting to Virtual Display Stream...", color = TextSecondaryDark, fontSize = 12.sp)
                    }
                }
            }

            // Floating Liquid Glass HUD
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF4ECCA3), CircleShape)
                                )
                                Text(
                                    text = "SECONDARY DISPLAY LIVE",
                                    color = TextPrimaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "${currentFps.toInt()} FPS",
                                    color = LavenderAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${latencyMs.toInt()}ms Latency",
                                    color = TextMutedDark,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Resolution Switcher Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1080p", "720p", "Phone Native").forEach { res ->
                                val isSelected = selectedRes == res
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
                                        .clickable { selectedRes = res }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = res,
                                        color = if (isSelected) TextPrimaryDark else TextMutedDark,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Frame Rate Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(30 to "30Hz Save", 60 to "60Hz Smooth", 120 to "120Hz Pro").forEach { (fpsVal, label) ->
                                val isSelected = selectedFps == fpsVal
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
                                        .clickable { selectedFps = fpsVal }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) TextPrimaryDark else TextMutedDark,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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

private fun readVirtualDisplayHeaderLine(inputStream: InputStream): String? {
    val baos = ByteArrayOutputStream()
    var prev = -1
    while (true) {
        val b = inputStream.read()
        if (b == -1) {
            return if (baos.size() == 0) null else baos.toString("UTF-8")
        }
        if (prev == '\r'.code && b == '\n'.code) {
            val arr = baos.toByteArray()
            return if (arr.isNotEmpty()) String(arr, 0, arr.size - 1, Charsets.UTF_8) else ""
        }
        baos.write(b)
        prev = b
    }
}
