package com.remoteforge.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.data.model.ClipboardItem
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.GlassCard
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

enum class ClipboardFilter {
    ALL, TEXT, IMAGES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(
    deviceId: String?,
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val history by repository.clipboardHistory.collectAsState()
    val isAutoSync by repository.isClipboardAutoSyncEnabled.collectAsState()
    val devices by repository.devices.collectAsState()
    val targetDevice = devices.firstOrNull { it.id == deviceId } ?: devices.firstOrNull { it.isOnline } ?: devices.firstOrNull()

    var activeFilter by remember { mutableStateOf(ClipboardFilter.ALL) }
    var manualText by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Fetch initial history on mount
    LaunchedEffect(Unit) {
        repository.refreshClipboardHistory()
    }

    // Gallery Picker for Sharing Images across devices
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val base64 = withContext(Dispatchers.IO) {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes() ?: return@withContext null
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                    if (base64 != null) {
                        val ok = repository.syncLocalClipboard(
                            contentType = "image",
                            imageBase64 = base64,
                            preview = "Shared Image from Phone"
                        )
                        if (ok) {
                            Toast.makeText(context, "Image sent to Mac clipboard!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to send image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading image: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            Text(
                                "Universal Clipboard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = RoyalViolet.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isAutoSync) "SYNC ON" else "PAUSED",
                                    color = if (isAutoSync) RoyalViolet else TextMutedDark,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (targetDevice != null) "Linked with ${targetDevice.name}" else "Linked with Mac & Phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                repository.refreshClipboardHistory()
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isRefreshing) RoyalViolet else TextPrimaryDark
                        )
                    }
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = TextMutedDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GlassBackgroundDark.copy(alpha = 0.95f)
                )
            )
        },
        containerColor = GlassBackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // AUTO-SYNC CONTROL CARD
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(RoyalViolet.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SyncAlt,
                                    contentDescription = null,
                                    tint = RoyalViolet,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Phone Link Auto-Sync",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    "Copies on Mac instantly land on Phone, and vice versa.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMutedDark
                                )
                            }
                        }
                        Switch(
                            checked = isAutoSync,
                            onCheckedChange = { repository.isClipboardAutoSyncEnabled.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = RoyalViolet,
                                uncheckedThumbColor = TextMutedDark,
                                uncheckedTrackColor = GlassSurfaceDark
                            )
                        )
                    }
                }
            }

            // QUICK INPUT / SHARE ACTIONS
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Quick Send to Mac Clipboard",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoyalViolet
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualText,
                                onValueChange = { manualText = it },
                                placeholder = {
                                    Text("Type text to push to Mac...", fontSize = 13.sp, color = TextMutedDark)
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
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
                                    if (manualText.isNotBlank()) {
                                        val textToSend = manualText
                                        manualText = ""
                                        coroutineScope.launch {
                                            val ok = repository.syncLocalClipboard(
                                                contentType = "text",
                                                text = textToSend
                                            )
                                            if (ok) {
                                                Toast.makeText(context, "Copied to Mac clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Icon(Icons.Rounded.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Send Image Button
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalViolet),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoyalViolet.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Image / Photo to Mac Clipboard", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // FILTER TABS & COUNTS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val textCount = history.count { it.contentType == "text" }
                    val imageCount = history.count { it.contentType == "image" }

                    FilterChip(
                        selected = activeFilter == ClipboardFilter.ALL,
                        onClick = { activeFilter = ClipboardFilter.ALL },
                        label = { Text("All (${history.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalViolet.copy(alpha = 0.2f),
                            selectedLabelColor = RoyalViolet,
                            containerColor = GlassSurfaceDark,
                            labelColor = TextMutedDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activeFilter == ClipboardFilter.ALL,
                            borderColor = GlassBorderSubtle,
                            selectedBorderColor = RoyalViolet
                        )
                    )

                    FilterChip(
                        selected = activeFilter == ClipboardFilter.TEXT,
                        onClick = { activeFilter = ClipboardFilter.TEXT },
                        label = { Text("Text ($textCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalViolet.copy(alpha = 0.2f),
                            selectedLabelColor = RoyalViolet,
                            containerColor = GlassSurfaceDark,
                            labelColor = TextMutedDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activeFilter == ClipboardFilter.TEXT,
                            borderColor = GlassBorderSubtle,
                            selectedBorderColor = RoyalViolet
                        )
                    )

                    FilterChip(
                        selected = activeFilter == ClipboardFilter.IMAGES,
                        onClick = { activeFilter = ClipboardFilter.IMAGES },
                        label = { Text("Images ($imageCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalViolet.copy(alpha = 0.2f),
                            selectedLabelColor = RoyalViolet,
                            containerColor = GlassSurfaceDark,
                            labelColor = TextMutedDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activeFilter == ClipboardFilter.IMAGES,
                            borderColor = GlassBorderSubtle,
                            selectedBorderColor = RoyalViolet
                        )
                    )
                }
            }

            // FILTERED HISTORY LIST
            val filteredList = when (activeFilter) {
                ClipboardFilter.ALL -> history
                ClipboardFilter.TEXT -> history.filter { it.contentType == "text" }
                ClipboardFilter.IMAGES -> history.filter { it.contentType == "image" }
            }

            if (filteredList.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                tint = RoyalViolet.copy(alpha = 0.6f),
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "No Clipboard Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Copy any text or image on your Mac or Phone to sync automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedDark,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    ClipboardItemCard(
                        item = item,
                        targetDeviceId = targetDevice?.id,
                        onCopyToPhone = { repository.copyToPhoneClipboard(item) },
                        onSendToMac = {
                            if (targetDevice != null) {
                                coroutineScope.launch {
                                    val ok = repository.copyClipboardToDevice(
                                        deviceId = targetDevice.id,
                                        contentType = item.contentType,
                                        text = item.textContent,
                                        imageBase64 = item.imageBase64
                                    )
                                    if (ok) {
                                        Toast.makeText(context, "Sent to ${targetDevice.name} clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                repository.deleteClipboardItem(item.id)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Clipboard History?") },
            text = { Text("This will remove all stored text snippets and synced images from RemoteForge history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        coroutineScope.launch {
                            repository.clearClipboardHistory()
                        }
                    }
                ) {
                    Text("Clear All", color = RubyOffline, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = TextMutedDark)
                }
            },
            containerColor = GlassSurfaceDark,
            titleContentColor = TextPrimaryDark,
            textContentColor = TextMutedDark
        )
    }
}

@Composable
fun ClipboardItemCard(
    item: ClipboardItem,
    targetDeviceId: String?,
    onCopyToPhone: () -> Unit,
    onSendToMac: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFromMac = item.sourceDevice.contains("mac", ignoreCase = true) || item.sourceDevice.contains("laptop", ignoreCase = true)
    val badgeColor = if (isFromMac) RoyalViolet else NeonIndigo
    val badgeLabel = if (isFromMac) "💻 MacBook Pro" else "📱 Phone"

    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Source Badge & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sizeLabel = if (item.sizeBytes > 1024) "${round(item.sizeBytes / 1024.0, 1)} KB" else "${item.sizeBytes} B"
                    Text(
                        text = sizeLabel,
                        fontSize = 10.sp,
                        color = TextMutedDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Delete",
                            tint = TextMutedDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Content Body
            if (item.contentType == "image" && !item.imageBase64.isNullOrBlank()) {
                val bitmap = remember(item.imageBase64) {
                    try {
                        val decoded = Base64.decode(item.imageBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GlassBackgroundDark)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Clipboard Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text("Image data (${item.preview})", style = MaterialTheme.typography.bodySmall, color = TextMutedDark)
                }
            } else {
                // Text Content
                val displayTxt = item.textContent ?: item.preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassBackgroundDark.copy(alpha = 0.8f))
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = displayTxt,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimaryDark,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.contentType == "text") {
                    TextButton(
                        onClick = onCopyToPhone,
                        colors = ButtonDefaults.textButtonColors(contentColor = RoyalViolet),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy to Phone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (targetDeviceId != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = onSendToMac,
                        colors = ButtonDefaults.textButtonColors(contentColor = if (isFromMac) TextMutedDark else RoyalViolet),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.LaptopMac, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Push to Mac", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun round(value: Double, decimals: Int): Double {
    var p = 1.0
    for (i in 0 until decimals) p *= 10.0
    return kotlin.math.round(value * p) / p
}
