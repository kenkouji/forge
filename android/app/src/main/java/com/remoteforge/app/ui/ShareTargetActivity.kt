package com.remoteforge.app.ui

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteforge.app.RemoteForgeApplication
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class SharedItemInfo(
    val uri: Uri?,
    val textContent: String?,
    val filename: String,
    val sizeBytes: Long
)

class ShareTargetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as RemoteForgeApplication
        val repository = app.repository
        val sharedItems = extractSharedItems(intent)

        setContent {
            RemoteForgeTheme(themePreference = "DARK", reducedMotion = false) {
                ShareTargetModal(
                    items = sharedItems,
                    onDismiss = { finish() },
                    onUpload = { deviceId, items, onStatusUpdate ->
                        uploadItemsToDesktop(deviceId, items, onStatusUpdate)
                    }
                )
            }
        }
    }

    private fun extractSharedItems(intent: Intent): List<SharedItemInfo> {
        val result = mutableListOf<SharedItemInfo>()
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            val streamUri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                ?: intent.data

            if (streamUri != null) {
                val (name, size) = queryFileInfo(contentResolver, streamUri)
                result.add(SharedItemInfo(streamUri, null, name, size))
            } else {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrBlank()) {
                    val safeFilename = "shared_text_${System.currentTimeMillis() / 1000}.txt"
                    result.add(SharedItemInfo(null, text, safeFilename, text.toByteArray().size.toLong()))
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            val uriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            uriList?.forEach { uri ->
                val (name, size) = queryFileInfo(contentResolver, uri)
                result.add(SharedItemInfo(uri, null, name, size))
            }
        }
        return result
    }

    private fun queryFileInfo(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = "shared_${System.currentTimeMillis()}"
        var size = 0L
        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}
        return Pair(name, size)
    }

    private suspend fun uploadItemsToDesktop(
        deviceId: String,
        items: List<SharedItemInfo>,
        onProgress: (index: Int, total: Int, success: Boolean?, error: String?) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val repository = (application as RemoteForgeApplication).repository
        var allSucceeded = true

        items.forEachIndexed { idx, item ->
            onProgress(idx + 1, items.size, null, null)
            val tempFile = File(cacheDir, item.filename)
            try {
                if (item.uri != null) {
                    contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else if (item.textContent != null) {
                    tempFile.writeText(item.textContent)
                }

                val uploaded = repository.uploadFile(deviceId, "~/Desktop", tempFile)
                tempFile.delete()

                if (!uploaded) {
                    allSucceeded = false
                    onProgress(idx + 1, items.size, false, "Upload rejected by computer agent")
                    return@withContext false
                }
            } catch (e: Exception) {
                tempFile.delete()
                allSucceeded = false
                onProgress(idx + 1, items.size, false, e.localizedMessage ?: "File read error")
                return@withContext false
            }
        }

        onProgress(items.size, items.size, true, null)
        allSucceeded
    }
}

@Composable
fun ShareTargetModal(
    items: List<SharedItemInfo>,
    onDismiss: () -> Unit,
    onUpload: suspend (deviceId: String, items: List<SharedItemInfo>, onProgress: (Int, Int, Boolean?, String?) -> Unit) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as RemoteForgeApplication
    val repository = app.repository
    val devices by repository.devices.collectAsState()
    val onlineDevices = devices.filter { it.isOnline }

    var selectedDeviceId by remember {
        mutableStateOf(onlineDevices.firstOrNull()?.id ?: devices.firstOrNull()?.id ?: "")
    }

    var uploadState by remember { mutableStateOf<String>("IDLE") } // IDLE, UPLOADING, SUCCESS, ERROR
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(devices) {
        if (selectedDeviceId.isEmpty() && devices.isNotEmpty()) {
            selectedDeviceId = onlineDevices.firstOrNull()?.id ?: devices.first().id
        }
    }

    LaunchedEffect(Unit) {
        repository.autoConnect()
    }

    val totalSize = items.sumOf { it.sizeBytes }
    val formattedSize = remember(totalSize) {
        when {
            totalSize > 1024 * 1024 -> String.format("%.1f MB", totalSize / (1024.0 * 1024.0))
            totalSize > 1024 -> String.format("%.0f KB", totalSize / 1024.0)
            else -> "$totalSize B"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC090510))
            .clickable { if (uploadState != "UPLOADING") onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(28.dp),
            color = DeepPlum.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassBorderHighlight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LavenderMuted.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Header with glowing icon badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(RoyalViolet, DeepPlum)
                                )
                            )
                            .border(1.dp, LavenderAccent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = LavenderAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Beam to Workstation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = PearlWhite
                        )
                        Text(
                            text = "One-tap native desktop drop",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = uploadState != "UPLOADING"
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextMutedDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content summary card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianBase.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RoyalViolet.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.InsertDriveFile,
                                    contentDescription = null,
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (items.size == 1) items[0].filename else "${items.size} files ready to beam",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PearlWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$formattedSize • Destination: ~/Desktop",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LavenderAccent.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target computer selection
                Text(
                    text = "TARGET WORKSTATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (devices.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = AmberWarning.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "No workstations online. Make sure RemoteForge is open on your computer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberWarning
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        devices.forEach { dev ->
                            val isSelected = dev.id == selectedDeviceId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = uploadState == "IDLE") {
                                        selectedDeviceId = dev.id
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) RoyalViolet.copy(alpha = 0.35f) else ObsidianBase.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LavenderAccent else GlassBorderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Computer,
                                        contentDescription = null,
                                        tint = if (isSelected) LavenderAccent else TextMutedDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dev.name.ifEmpty { dev.id },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = PearlWhite
                                        )
                                        Text(
                                            text = "${dev.osName} • ${if (dev.isOnline) "Active" else "Offline"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (dev.isOnline) EmeraldOnline else TextMutedDark
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = LavenderAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = RubyOffline,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action Button
                when (uploadState) {
                    "SUCCESS" -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = EmeraldOnline.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldOnline)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldOnline)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Beamed to Desktop!",
                                    color = EmeraldOnline,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    "UPLOADING" -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = RoyalViolet.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LavenderAccent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = LavenderAccent,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    statusText.ifEmpty { "Streaming to PC Desktop..." },
                                    color = PearlWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                if (selectedDeviceId.isEmpty()) return@Button
                                uploadState = "UPLOADING"
                                errorMessage = null
                                coroutineScope.launch {
                                    val ok = onUpload(selectedDeviceId, items) { cur, tot, isSuccess, err ->
                                        if (isSuccess == false) {
                                            errorMessage = err ?: "Upload failed"
                                        } else {
                                            statusText = "Streaming $cur of $tot to Desktop..."
                                        }
                                    }
                                    if (ok) {
                                        uploadState = "SUCCESS"
                                        delay(1300)
                                        onDismiss()
                                    } else {
                                        uploadState = "ERROR"
                                    }
                                }
                            },
                            enabled = selectedDeviceId.isNotEmpty() && items.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalViolet,
                                disabledContainerColor = ObsidianBase.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Beam to Desktop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
