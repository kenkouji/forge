package com.remoteforge.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DevicesScreen(
    repository: RemoteForgeRepository,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val devices by repository.devices.collectAsState()
    val selectedDevice by repository.selectedDevice.collectAsState()
    val telemetry by repository.telemetry.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var useCompactView by remember { mutableStateOf(false) }

    val onlineDevices = devices.filter { it.isOnline }
    val offlineDevices = devices.filter { !it.isOnline }

    LaunchedEffect(Unit) {
        repository.refreshDevices()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RemoteForgeEmblem(size = 36.dp)

                Column {
                    Text(
                        text = "REMOTEFORGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = LavenderAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Workstations",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite,
                        fontSize = 22.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View toggle: Compact vs Expanded
                if (devices.size > 2) {
                    IconButton(
                        onClick = { useCompactView = !useCompactView },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (useCompactView) Icons.Rounded.ViewAgenda else Icons.Rounded.List,
                            contentDescription = "Toggle View",
                            tint = LavenderAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            isRefreshing = true
                            repository.refreshDevices()
                            selectedDevice?.let { repository.fetchTelemetry(it.id) }
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = LavenderAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (devices.isEmpty()) {
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DesktopWindows,
                        contentDescription = null,
                        tint = TextDarkMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("No Workstations Discovered", color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Start the RemoteForge agent on your computer to connect automatically over NetBird VPN or local network.",
                        color = TextRestrainedGray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Online Workstations
                if (onlineDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "ACTIVE & ONLINE (${onlineDevices.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    items(onlineDevices, key = { it.id }) { dev ->
                        val isSelected = selectedDevice?.id == dev.id
                        EnhancedDeviceCard(
                            device = dev,
                            isSelected = isSelected,
                            isCompact = useCompactView,
                            telemetrySummary = if (isSelected && telemetry != null) "${telemetry?.cpu?.percent?.toInt()}% CPU • ${telemetry?.ram?.percent?.toInt()}% RAM" else null,
                            onSelect = {
                                repository.selectDevice(dev)
                            },
                            onInspect = {
                                repository.selectDevice(dev)
                                onNavigate(AppScreen.DeviceDetails(dev.id))
                            }
                        )
                    }
                }

                // Offline Workstations
                if (offlineDevices.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "OFFLINE (${offlineDevices.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDarkMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    items(offlineDevices, key = { it.id }) { dev ->
                        val isSelected = selectedDevice?.id == dev.id
                        EnhancedDeviceCard(
                            device = dev,
                            isSelected = isSelected,
                            isCompact = useCompactView,
                            telemetrySummary = null,
                            onSelect = {
                                repository.selectDevice(dev)
                            },
                            onInspect = {
                                repository.selectDevice(dev)
                                onNavigate(AppScreen.DeviceDetails(dev.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedDeviceCard(
    device: Device,
    isSelected: Boolean,
    isCompact: Boolean,
    telemetrySummary: String?,
    onSelect: () -> Unit,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMac = device.osName.contains("Mac", ignoreCase = true) || device.osName.contains("Darwin", ignoreCase = true)
    val isWin = device.osName.contains("Windows", ignoreCase = true)

    ForgeGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        borderColor = if (isSelected) LavenderAccent.copy(alpha = 0.8f) else ObsidianBorder,
        borderBrush = if (isSelected) {
            Brush.verticalGradient(
                listOf(
                    LavenderAccent.copy(alpha = 0.9f),
                    RoyalViolet.copy(alpha = 0.6f),
                    Color(0x10FFFFFF)
                )
            )
        } else null,
        backgroundColor = if (isSelected) Color(0xF02A1644) else ObsidianSurface,
        contentPadding = PaddingValues(14.dp),
        onClick = onInspect
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 36.dp else 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(RoyalViolet.copy(alpha = 0.45f), DeepPlum)
                                )
                            )
                            .border(
                                1.dp,
                                if (isSelected) LavenderAccent else Color(0x25FFFFFF),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isMac -> Icons.Rounded.LaptopMac
                                isWin -> Icons.Rounded.LaptopWindows
                                else -> Icons.Rounded.Computer
                            },
                            contentDescription = device.name,
                            tint = if (isSelected) PearlWhite else TextSoftWhite,
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPureWhite,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RoyalViolet.copy(alpha = 0.4f))
                                        .border(0.5.dp, LavenderAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", color = LavenderAccent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (device.isOnline) AccentEmerald else AccentRuby)
                            )
                            Text(
                                text = if (device.isOnline) {
                                    telemetrySummary ?: "${device.osName} • Agent v${device.agentVersion}"
                                } else {
                                    "Offline"
                                },
                                color = TextRestrainedGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isSelected && device.isOnline) {
                        OutlinedButton(
                            onClick = onSelect,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RoyalViolet.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Select", color = LavenderAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = onInspect,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Inspect",
                            tint = if (isSelected) LavenderAccent else TextDarkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
