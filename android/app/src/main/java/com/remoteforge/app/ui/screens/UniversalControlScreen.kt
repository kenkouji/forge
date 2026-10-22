package com.remoteforge.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.GlassCard
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalControlScreen(
    deviceId: String,
    repository: RemoteForgeRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var configuredEdge by remember { mutableStateOf("RIGHT") }
    var sensitivity by remember { mutableFloatStateOf(1.0f) }
    var dwellDelayMs by remember { mutableIntStateOf(80) }
    var enableKeyboard by remember { mutableStateOf(true) }
    var enableClipboard by remember { mutableStateOf(true) }
    var isEnabled by remember { mutableStateOf(true) }
    var isRemoteActive by remember { mutableStateOf(false) }

    // Edge glow infinite animation
    val infiniteTransition = rememberInfiniteTransition(label = "EdgeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
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
                Toast.makeText(context, "Universal Control Active on $configuredEdge Edge", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Universal Control",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Cross-Screen Mouse & Keyboard Hopping",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0C0816))
        ) {
            // Visual Edge Barrier Glow along the active edge
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
                            "RIGHT" -> Modifier.align(Alignment.CenterEnd).width(24.dp).fillMaxHeight()
                            "LEFT" -> Modifier.align(Alignment.CenterStart).width(24.dp).fillMaxHeight()
                            "TOP" -> Modifier.align(Alignment.TopCenter).height(24.dp).fillMaxWidth()
                            else -> Modifier.align(Alignment.BottomCenter).height(24.dp).fillMaxWidth()
                        }
                    )
                    .background(edgeBrush)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                        Text(
                            text = "SPATIAL SCREEN ARRANGEMENT",
                            color = LavenderMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // Visual Monitor Box & Phone Box Diagram
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // PC Monitor Box
                            Box(
                                modifier = Modifier
                                    .size(130.dp, 85.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1430))
                                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Computer, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Primary Mac/PC", color = TextPrimaryDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
                                tint = RoyalViolet.copy(alpha = glowAlpha),
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
                                    .border(2.dp, if (isRemoteActive) Color(0xFF4ECCA3) else RoyalViolet, RoundedCornerShape(10.dp)),
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
                            text = if (isRemoteActive) "Mouse & Keyboard Active on Phone!" else "Move cursor past the $configuredEdge edge of your PC monitor to hop here.",
                            color = if (isRemoteActive) Color(0xFF4ECCA3) else TextMutedDark,
                            fontSize = 12.sp
                        )
                    }
                }

                // Edge Position Selector
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "DEVICE PLACEMENT",
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
                                Text("Typing on PC routes to Phone when active", color = TextMutedDark, fontSize = 11.sp)
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

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                if (isRemoteActive) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.sendEdgeHandoff(deviceId, "EXIT_REMOTE", 0.5f, 0.5f)
                                isRemoteActive = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Snap Cursor Back to PC", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
