package com.remoteforge.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.remoteforge.app.data.model.*
import com.remoteforge.app.data.repository.RemoteForgeRepository
import com.remoteforge.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun generateQrCodeBitmap(text: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}

@Composable
fun PrivacyShieldCard(
    deviceId: String,
    repository: RemoteForgeRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var shieldStatus by remember { mutableStateOf<PrivacyShieldResponse?>(null) }
    var isUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(deviceId) {
        shieldStatus = repository.getPrivacyShield(deviceId)
        while (true) {
            delay(3500)
            shieldStatus = repository.getPrivacyShield(deviceId)
        }
    }

    val isLockdown = shieldStatus?.isLockdown == true
    val isScreenPaused = shieldStatus?.isScreenPaused == true
    val isInputDisabled = shieldStatus?.isInputDisabled == true
    val isTransfersStopped = shieldStatus?.isTransfersStopped == true
    val activeGuests = shieldStatus?.activeGuestsCount ?: 0

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isLockdown) Color(0x30EF4444) else Color(0x20BDA0FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLockdown) Icons.Rounded.ShieldMoon else Icons.Rounded.Security,
                            contentDescription = "Shield",
                            tint = if (isLockdown) RubyOffline else RoyalViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PRIVACY SHIELD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isLockdown) RubyOffline else TextPrimaryDark
                            )
                            if (isLockdown) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LOCKDOWN ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RubyOffline,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (activeGuests > 0) "$activeGuests active guest(s)" else "Protected • Defense-in-depth active",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedDark
                        )
                    }
                }

                if (isLockdown) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isUpdating = true
                                shieldStatus = repository.updatePrivacyShield(deviceId, PrivacyShieldUpdateRequest(action = "resume_access"))
                                isUpdating = false
                            }
                        },
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Resume", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isUpdating = true
                                shieldStatus = repository.updatePrivacyShield(deviceId, PrivacyShieldUpdateRequest(action = "emergency_lockdown"))
                                isUpdating = false
                            }
                        },
                        enabled = !isUpdating,
                        colors = ButtonDefaults.buttonColors(containerColor = RubyOffline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Lockdown", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = GlassBorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Action Quick Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pause Screen
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isUpdating = true
                            shieldStatus = repository.updatePrivacyShield(
                                deviceId,
                                PrivacyShieldUpdateRequest(isScreenPaused = !isScreenPaused, action = "Toggle Screen")
                            )
                            isUpdating = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating && !isLockdown,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isScreenPaused) RubyOffline else RoyalViolet
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isScreenPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isScreenPaused) "Resume" else "Pause Screen",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                }

                // Block Input
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isUpdating = true
                            shieldStatus = repository.updatePrivacyShield(
                                deviceId,
                                PrivacyShieldUpdateRequest(isInputDisabled = !isInputDisabled, action = "Toggle Input")
                            )
                            isUpdating = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating && !isLockdown,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isInputDisabled) RubyOffline else RoyalViolet
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isInputDisabled) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isInputDisabled) "Allow Input" else "Block Input",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                }

                // Stop Transfers
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isUpdating = true
                            shieldStatus = repository.updatePrivacyShield(
                                deviceId,
                                PrivacyShieldUpdateRequest(isTransfersStopped = !isTransfersStopped, action = "Toggle Transfers")
                            )
                            isUpdating = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating && !isLockdown,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isTransfersStopped) RubyOffline else RoyalViolet
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTransfersStopped) "Start File" else "Stop File",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InviteGuestDialog(
    deviceId: String,
    repository: RemoteForgeRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var allowInput by remember { mutableStateOf(true) }
    var allowUpload by remember { mutableStateOf(true) }
    var allowDownload by remember { mutableStateOf(true) }
    var durationMinutes by remember { mutableStateOf(15) }
    var isGenerating by remember { mutableStateOf(false) }

    var createdInvitation by remember { mutableStateOf<GuestInvitationResponse?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(GlassSurfaceDark)
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invite Browser Guest",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMutedDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (createdInvitation != null && qrBitmap != null) {
                    // QR Code Presentation
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Guest QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Scan with phone camera or share link",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Single-use token • Fragment secret not logged • Expires in $durationMinutes min",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚡ Hotspot / Wi-Fi Access: Guests must connect to your Wi-Fi or Hotspot to open this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LavenderAccent,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Guest Invitation URL", createdInvitation!!.qrPayload)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Invitation link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Secure Link")
                    }
                } else {
                    // Capabilities Selection Form
                    Text(
                        text = "Select Guest Permissions",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMutedDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { allowInput = !allowInput }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allowInput,
                            onCheckedChange = { allowInput = it },
                            colors = CheckboxDefaults.colors(checkedColor = RoyalViolet)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Co-Control Input", style = MaterialTheme.typography.bodyMedium, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                            Text("Allow mouse clicks and keystrokes", style = MaterialTheme.typography.bodySmall, color = TextMutedDark)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { allowUpload = !allowUpload }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allowUpload,
                            onCheckedChange = { allowUpload = it },
                            colors = CheckboxDefaults.colors(checkedColor = RoyalViolet)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Drop Zone Upload", style = MaterialTheme.typography.bodyMedium, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                            Text("Upload files to staging quarantine", style = MaterialTheme.typography.bodySmall, color = TextMutedDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isGenerating = true
                                val caps = mutableListOf("screen.view")
                                if (allowInput) caps.add("input.control")
                                if (allowUpload) caps.add("files.upload")
                                if (allowDownload) caps.add("files.download")
                                val inv = repository.createGuestInvitation(deviceId, caps, durationMinutes)
                                if (inv != null) {
                                    createdInvitation = inv
                                    qrBitmap = generateQrCodeBitmap(inv.qrPayload, 512)
                                } else {
                                    Toast.makeText(context, "Failed to create invitation", Toast.LENGTH_SHORT).show()
                                }
                                isGenerating = false
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalViolet)
                    ) {
                        Text(if (isGenerating) "Generating..." else "Create QR Code Invitation")
                    }
                }
            }
        }
    }
}

@Composable
fun GuestSessionsCard(
    deviceId: String,
    repository: RemoteForgeRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var sessions by remember { mutableStateOf<List<GuestSessionResponse>>(emptyList()) }

    LaunchedEffect(deviceId) {
        sessions = repository.listGuestSessions(deviceId)
        while (true) {
            delay(3000)
            sessions = repository.listGuestSessions(deviceId)
        }
    }

    val pending = sessions.filter { it.status == "PENDING_APPROVAL" }
    val active = sessions.filter { it.status == "APPROVED" }

    if (sessions.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Pending Requests
        if (pending.isNotEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pending Guest Requests (${pending.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    pending.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(s.guestName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text("Session: ${s.id.take(8)}", style = MaterialTheme.typography.bodySmall, color = TextMutedDark)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = repository.approveGuestSession(s.id, listOf("screen.view", "input.control", "files.upload", "files.download"))
                                            if (res != null) {
                                                Toast.makeText(context, "Guest session approved!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to approve guest session", Toast.LENGTH_SHORT).show()
                                            }
                                            sessions = repository.listGuestSessions(deviceId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldOnline),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Approve", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = repository.revokeGuestSession(s.id, "Rejected by owner")
                                            if (res) {
                                                Toast.makeText(context, "Guest session rejected", Toast.LENGTH_SHORT).show()
                                            }
                                            sessions = repository.listGuestSessions(deviceId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RubyOffline),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Reject", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Sessions
        if (active.isNotEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.People, contentDescription = null, tint = RoyalViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Connected Guests (${active.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    active.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(s.guestName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text("Mode: ${s.controlMode} • Active", style = MaterialTheme.typography.bodySmall, color = TextMutedDark)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.revokeGuestSession(s.id, "Disconnected by owner")
                                        sessions = repository.listGuestSessions(deviceId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RubyOffline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
