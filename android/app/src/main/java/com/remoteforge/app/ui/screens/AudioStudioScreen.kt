package com.remoteforge.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.audio.AudioStreamManager
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.GlassCard
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioStudioScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember { AudioStreamManager(context) }
    val liveAmplitude by audioManager.amplitude.collectAsState()

    var selectedMode by remember { mutableStateOf("sink") } // "sink" (PC->Phone) or "source" (Phone->PC Mic)
    var isStreaming by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableFloatStateOf(24.5f) }
    var sampleRate by remember { mutableIntStateOf(48000) }

    // Live spectrum animation phase
    val infiniteTransition = rememberInfiniteTransition(label = "SpectrumPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    // Load status on launch
    LaunchedEffect(deviceId) {
        val status = repository.getAudioStatus(deviceId)
        if (status != null && status.isActive) {
            isStreaming = true
            selectedMode = status.mode
            sampleRate = status.sampleRate
            latencyMs = status.latencyMs
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.stopPlayback()
        }
    }

    fun toggleStreaming() {
        coroutineScope.launch {
            if (isStreaming) {
                audioManager.stopPlayback()
                repository.stopAudioSession(deviceId)
                isStreaming = false
                Toast.makeText(context, "Audio Session Stopped", Toast.LENGTH_SHORT).show()
            } else {
                val status = repository.startAudioSession(
                    deviceId = deviceId,
                    mode = selectedMode,
                    sampleRate = sampleRate,
                    channels = 2,
                    bitDepth = 16,
                    codec = "pcm"
                )
                if (status != null && status.isActive) {
                    isStreaming = true
                    latencyMs = status.latencyMs

                    // Construct WS URL for raw PCM audio stream
                    val serverUrl = repository.networkConfig.serverUrl
                        .replace("http://", "ws://")
                        .replace("https://", "wss://")
                        .trimEnd('/')
                    val wsUrl = "$serverUrl/ws/audio/$deviceId"

                    if (selectedMode == "sink") {
                        audioManager.startPlayback(wsUrl)
                    }
                    Toast.makeText(context, "Live Audio Streaming Active", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Audio Studio",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (selectedMode == "sink") "PC Sound Output • Wireless Headphones" else "Wireless Studio Mic • Phone to PC",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0C0816))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("sink" to "PC Audio Output", "source" to "Wireless Mic").forEach { (modeVal, label) ->
                    val isSelected = selectedMode == modeVal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) RoyalViolet.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) RoyalViolet else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isStreaming) selectedMode = modeVal
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) TextPrimaryDark else TextMutedDark,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Real-Time Animated Audio Spectrum Visualizer
            GlassCard(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = 32
                        val barWidth = size.width / (barCount * 1.5f)
                        val maxBarHeight = size.height * 0.75f

                        for (i in 0 until barCount) {
                            val x = i * (barWidth * 1.5f) + barWidth * 0.25f
                            val normalizedHeight = if (isStreaming && !isMuted) {
                                val ampFactor = (liveAmplitude * 2.5f).coerceIn(0.18f, 1.0f)
                                val wave1 = sin(phase + i * 0.35f) * 0.5f + 0.5f
                                val wave2 = sin(phase * 1.4f + i * 0.6f) * 0.3f + 0.3f
                                (((wave1 + wave2) / 1.6f) * ampFactor).coerceIn(0.12f, 1.0f)
                            } else {
                                0.08f
                            }
                            val h = maxBarHeight * normalizedHeight
                            val top = (size.height - h) / 2f

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFD4B8FF),
                                        RoyalViolet,
                                        Color(0xFF381E54)
                                    )
                                ),
                                topLeft = Offset(x, top),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }

                    // Status Badge overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isStreaming) "${latencyMs.toInt()}ms • ${sampleRate / 1000}kHz 16-bit" else "IDLE",
                            color = if (isStreaming) Color(0xFF4ECCA3) else TextMutedDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Audio Controls (Volume & Mute)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "AUDIO LEVEL & GAIN",
                        color = LavenderMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = if (isMuted) Color.Red else RoyalViolet
                        )
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                audioManager.setVolume(it)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isMuted
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = if (isMuted) Color.Red else Color(0xFF4ECCA3)
                            )
                            Text("Mute Audio Stream", color = TextPrimaryDark, fontSize = 13.sp)
                        }
                        Switch(
                            checked = isMuted,
                            onCheckedChange = { isMuted = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Stream Toggle Action
            Button(
                onClick = { toggleStreaming() },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isStreaming) "Stop Audio Stream" else "Start Live Audio Stream",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
