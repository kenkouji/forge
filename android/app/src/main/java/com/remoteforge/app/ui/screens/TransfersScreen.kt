package com.remoteforge.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.TransferRecord
import com.remoteforge.app.data.model.TransferStatus
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*

@Composable
fun TransfersScreen(
    repository: RemoteForgeRepository,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val transfers by repository.transfers.collectAsState()

    val activeTransfers = transfers.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED || it.status == TransferStatus.PAUSED }
    val completedTransfers = transfers.filter { it.status == TransferStatus.COMPLETED || it.status == TransferStatus.FAILED || it.status == TransferStatus.CANCELLED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transfer Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "${transfers.size} total transfer session(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedDark
                )
            }

            Button(
                onClick = { onNavigate(AppScreen.UniversalTransfer()) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (transfers.isEmpty()) {
            EmptyState(
                title = "No Transfer Activity",
                description = "You have no active or completed file transfers. Stream files between your phone and registered computers.",
                icon = Icons.Rounded.SwapVert,
                actionLabel = "Start Universal Transfer",
                onAction = { onNavigate(AppScreen.UniversalTransfer()) }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (activeTransfers.isNotEmpty()) {
                    item {
                        Text(
                            text = "ACTIVE SESSIONS (${activeTransfers.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }

                    items(activeTransfers, key = { it.id }) { transfer ->
                        TransferCard(
                            transfer = transfer,
                            onCancel = { repository.cancelTransfer(transfer.id) },
                            onPause = { repository.pauseTransfer(transfer.id) },
                            onResume = { repository.resumeTransfer(transfer.id) }
                        )
                    }
                }

                if (completedTransfers.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "TRANSFER HISTORY (${completedTransfers.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark
                        )
                    }

                    items(completedTransfers, key = { it.id }) { transfer ->
                        val devId = transfer.deviceId ?: repository.selectedDevice.value?.id ?: ""
                        val targetPath = transfer.remotePath ?: transfer.filename
                        TransferCard(
                            transfer = transfer,
                            onCancel = { repository.cancelTransfer(transfer.id) },
                            onResume = if (transfer.status == TransferStatus.FAILED) {
                                { repository.resumeTransfer(transfer.id) }
                            } else null,
                            onOpenFile = if (transfer.localPath != null) {
                                { repository.openFile(transfer.localPath) }
                            } else null,
                            onClick = if (devId.isNotBlank()) {
                                { onNavigate(AppScreen.FilePreview(devId, targetPath, transfer.filename)) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
