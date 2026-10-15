package com.remoteforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    var isRefreshing by remember { mutableStateOf(false) }

    val onlineDevices = devices.filter { it.isOnline }
    val offlineDevices = devices.filter { !it.isOnline }

    LaunchedEffect(Unit) {
        repository.refreshDevices()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
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
                    text = "Devices",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    fontSize = 28.sp
                )
                Text(
                    text = "Manage your devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextRestrainedGray,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        isRefreshing = true
                        repository.refreshDevices()
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
        }

        if (devices.isEmpty()) {
            ForgeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp)
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
                        modifier = Modifier.size(36.dp)
                    )
                    Text("No Devices Connected", color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Start the RemoteForge agent on your Mac or PC to establish an authenticated connection.",
                        color = TextRestrainedGray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Online Group
                if (onlineDevices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Online (${onlineDevices.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    items(onlineDevices, key = { it.id }) { dev ->
                        val isSelected = selectedDevice?.id == dev.id
                        ForgeDeviceCard(
                            device = dev,
                            isSelected = isSelected,
                            lastActiveText = "Last active 2 min ago",
                            onClick = {
                                repository.selectDevice(dev)
                                onNavigate(AppScreen.DeviceDetails(dev.id))
                            }
                        )
                    }
                }

                // Offline Group
                if (offlineDevices.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Offline (${offlineDevices.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDarkMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    items(offlineDevices, key = { it.id }) { dev ->
                        ForgeDeviceCard(
                            device = dev,
                            isSelected = false,
                            lastActiveText = "Last seen offline",
                            onClick = {
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
