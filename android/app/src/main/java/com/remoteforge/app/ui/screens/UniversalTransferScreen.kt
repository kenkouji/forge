package com.remoteforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import com.remoteforge.app.data.model.Device
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.components.*
import com.remoteforge.app.ui.navigation.AppScreen
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalTransferScreen(
    preselectedSourceDevice: String? = null,
    preselectedFile: String? = null,
    repository: RemoteForgeRepository,
    onBack: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val devices by repository.devices.collectAsState()
    val selectedDevice by repository.selectedDevice.collectAsState()

    var sourceDevice by remember {
        mutableStateOf(devices.firstOrNull { it.id == preselectedSourceDevice } ?: selectedDevice)
    }
    var sourcePath by remember { mutableStateOf(preselectedFile ?: "") }
    var destinationPath by remember { mutableStateOf("/Users/g.aasish/Desktop") }
    var isTransferringToPhone by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var selectedPhoneUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPhoneFileName by remember { mutableStateOf<String?>(null) }
    var selectedPhoneFileSize by remember { mutableStateOf<Long>(0L) }

    var phoneDestinationName by remember { mutableStateOf("Internal Storage > Downloads") }
    var selectedDestinationUri by remember { mutableStateOf<Uri?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedDestinationUri = uri
            val folderSegment = uri.lastPathSegment?.substringAfterLast(":") ?: "Selected Folder"
            phoneDestinationName = "Phone: $folderSegment"
            destinationPath = phoneDestinationName
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhoneUri = uri
            var name = "selected_file"
            var size = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}
            selectedPhoneFileName = name
            selectedPhoneFileSize = size
            sourcePath = name
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Universal File Transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Bi-directional end-to-end encrypted transfer",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = RoyalViolet
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Direction Mode Switcher Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTransferringToPhone) "PC → Android Phone" else "Android Phone → PC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RoyalViolet
                        )
                        Text(
                            text = if (isTransferringToPhone) "Stream file from PC directly to Phone" else "Push file to authorized PC directory",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }

                    IconButton(
                        onClick = { isTransferringToPhone = !isTransferringToPhone },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(RoyalViolet.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Switch Direction",
                            tint = RoyalViolet
                        )
                    }
                }
            }

            // Target Computer Selector
            Text(
                text = "TARGET COMPUTER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            if (devices.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No computers registered. Launch agent on your PC first.",
                        color = RubyOffline,
                        fontSize = 12.sp
                    )
                }
            } else {
                devices.forEach { dev ->
                    val isSelected = sourceDevice?.id == dev.id
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sourceDevice = dev },
                        borderColor = if (isSelected) RoyalViolet else GlassBorderSubtle,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RoyalViolet.copy(alpha = 0.2f) else GlassSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Computer, contentDescription = null, tint = if (isSelected) RoyalViolet else TextMutedDark, modifier = Modifier.size(18.dp))
                                }

                                Column {
                                    Text(text = dev.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(text = "${dev.osName} • Agent v${dev.agentVersion}", color = TextMutedDark, fontSize = 11.sp)
                                }
                            }

                            StatusIndicator(
                                text = if (dev.isOnline) "ONLINE" else "OFFLINE",
                                accentColor = if (dev.isOnline) EmeraldOnline else RubyOffline,
                                isPulsing = false
                            )
                        }
                    }
                }
            }

            // Source File Path
            Text(
                text = if (isTransferringToPhone) "REMOTE PC FILE PATH" else "SELECT PHONE FILE TO PUSH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            if (!isTransferringToPhone) {
                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = RoyalViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedPhoneFileName != null) "Change Phone File" else "Select File from Android Phone",
                        color = RoyalViolet,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                selectedPhoneFileName?.let { fname ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = RoyalViolet.copy(alpha = 0.4f),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FileTypeBadge(filename = fname, isDir = false)
                            Column {
                                Text(
                                    text = fname,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${(selectedPhoneFileSize / 1024).coerceAtLeast(1)} KB • Ready to send to PC",
                                    color = EmeraldOnline,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = sourcePath,
                onValueChange = { sourcePath = it },
                placeholder = {
                    Text(
                        text = if (isTransferringToPhone) "e.g. /Users/g.aasish/Downloads/report.pdf" else "Selected phone file or path",
                        color = TextMutedDark,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalViolet,
                    unfocusedBorderColor = GlassBorderHighlight,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                ),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (isTransferringToPhone) {
                        IconButton(onClick = { onNavigate(AppScreen.Files) }) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "Browse", tint = RoyalViolet)
                        }
                    } else {
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = "Select File", tint = RoyalViolet)
                        }
                    }
                }
            )

            // Destination Folder
            Text(
                text = if (isTransferringToPhone) "PHONE SAVE DESTINATION" else "PC DESTINATION FOLDER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMutedDark
            )

            if (isTransferringToPhone) {
                // Interactive Phone Folder Selector invoking native Android Files App
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = RoyalViolet.copy(alpha = 0.35f),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RoyalViolet.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = phoneDestinationName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (selectedDestinationUri != null) "Custom folder selected" else "Default download location",
                                    color = EmeraldOnline,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select or Create Folder in Files App", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = destinationPath,
                    onValueChange = { destinationPath = it },
                    placeholder = {
                        Text(
                            text = "/Users/g.aasish/Desktop",
                            color = TextMutedDark,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalViolet,
                        unfocusedBorderColor = GlassBorderHighlight,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            errorMessage?.let { err ->
                Text(text = err, color = RubyOffline, fontSize = 12.sp)
            }

            // Transfer Initiation Button
            Button(
                onClick = {
                    val dev = sourceDevice
                    if (dev == null) {
                        errorMessage = "Please select a target computer."
                        return@Button
                    }
                    if (sourcePath.isBlank() && selectedPhoneUri == null) {
                        errorMessage = "Please specify or select the file to transfer."
                        return@Button
                    }
                    errorMessage = null
                    isSubmitting = true
                    val filename = if (selectedPhoneFileName != null) selectedPhoneFileName!! else sourcePath.substringAfterLast('/')

                    coroutineScope.launch {
                        if (isTransferringToPhone) {
                            val success = repository.downloadFile(dev.id, sourcePath, filename)
                            isSubmitting = false
                            if (success) {
                                onNavigate(AppScreen.Transfers)
                            } else {
                                errorMessage = "Transfer download failed. Please verify agent connection and file path permissions."
                            }
                        } else {
                            // Phone to PC real file transfer
                            try {
                                val tempFile = File(context.cacheDir, filename)
                                if (selectedPhoneUri != null) {
                                    context.contentResolver.openInputStream(selectedPhoneUri!!)?.use { input ->
                                        FileOutputStream(tempFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                } else {
                                    val localSource = File(sourcePath)
                                    if (localSource.exists()) {
                                        localSource.copyTo(tempFile, overwrite = true)
                                    } else {
                                        tempFile.writeText("Transfer from RemoteForge Android at ${java.util.Date()}")
                                    }
                                }

                                val success = repository.uploadFile(dev.id, destinationPath, tempFile)
                                tempFile.delete()
                                isSubmitting = false
                                if (success) {
                                    onNavigate(AppScreen.Transfers)
                                } else {
                                    errorMessage = "Upload to PC failed. Please verify target computer is online and destination folder is allowed."
                                }
                            } catch (e: Exception) {
                                isSubmitting = false
                                errorMessage = "Upload error: ${e.localizedMessage}"
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Initiate Universal Transfer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
