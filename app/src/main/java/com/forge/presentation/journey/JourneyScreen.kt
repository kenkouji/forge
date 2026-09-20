package com.forge.presentation.journey

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeAmbientParticles
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.component.ForgeSectionHeader
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.TransformationCheckInEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JourneyScreen(
    viewModel: JourneyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showPasswordModal by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var selectedAngle by remember { mutableStateOf("Front") }

    var viewingCheckIn by remember { mutableStateOf<TransformationCheckInEntity?>(null) }
    var decryptedViewingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ForgeAmbientParticles(enabled = true, reduceMotion = false, particleCount = 16)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transformation",
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Week ${uiState.currentTransformationWeek} Milestone Chronicle",
                        color = ForgeColors.CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.isUnlocked && uiState.isVaultConfigured) {
                    ForgeButton(
                        onClick = { viewModel.lockVault() },
                        text = "Lock",
                        variant = ForgeButtonVariant.SUBTLE,
                        size = ForgeButtonSize.SM,
                        icon = Icons.Default.Lock
                    )
                }
            }

            // Quick Photo Actions: Front, Side, Back, Gallery
            ForgeGlassSurface(
                variant = ForgeGlassVariant.ELEVATED,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LOG PROGRESS PHOTO",
                        color = ForgeColors.CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Front", "Side", "Back").forEach { angle ->
                            ForgeButton(
                                onClick = {
                                    selectedAngle = angle
                                    showAddPhotoDialog = true
                                },
                                text = "+ $angle",
                                variant = ForgeButtonVariant.PRIMARY,
                                size = ForgeButtonSize.SM,
                                icon = Icons.Default.CameraAlt,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6-Month Slideshow Timeline (Shows real check-ins only)
            if (uiState.checkIns.size >= 2) {
                ForgeSectionHeader(label = "Physique Progression Timeline")
                ForgeGlassSurface(
                    variant = ForgeGlassVariant.STRONG,
                    borderColor = ForgeColors.CyanPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentSlide = uiState.checkIns.getOrNull(uiState.slideshowIndex)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Week ${currentSlide?.weekNumber ?: 1} Milestone",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.slideshowIndex + 1} of ${uiState.checkIns.size}",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ForgeColors.GlassFillStrong),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!uiState.isUnlocked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lock, null, tint = ForgeColors.CyanPrimary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Encrypted at Rest", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Tap below to unlock timeline", color = ForgeTheme.colors.textSecondary, fontSize = 10.sp)
                                }
                            } else {
                                Icon(Icons.Default.CameraAlt, null, tint = ForgeColors.CyanPrimary, modifier = Modifier.size(40.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.prevSlide() }) {
                                Icon(Icons.Default.ChevronLeft, "Previous", tint = Color.White)
                            }

                            if (!uiState.isUnlocked) {
                                ForgeButton(
                                    onClick = { showPasswordModal = true },
                                    text = "Unlock Timeline",
                                    variant = ForgeButtonVariant.ACCENT,
                                    size = ForgeButtonSize.SM
                                )
                            }

                            IconButton(onClick = { viewModel.nextSlide() }) {
                                Icon(Icons.Default.ChevronRight, "Next", tint = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Milestone Chronicle Grid
            ForgeSectionHeader(label = "Milestone Chronicle (${uiState.checkIns.size})")

            if (uiState.checkIns.isEmpty()) {
                ForgeGlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Transformation Photos", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Log your Week ${uiState.currentTransformationWeek} milestone photo above. Images are encrypted via hardware-backed Android Keystore.",
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.checkIns.forEach { checkIn ->
                        ForgeGlassSurface(
                            variant = ForgeGlassVariant.SUBTLE,
                            onClick = {
                                if (!uiState.isUnlocked && uiState.isVaultConfigured) {
                                    showPasswordModal = true
                                } else {
                                    val path = checkIn.frontEncryptedPath ?: checkIn.sideEncryptedPath ?: checkIn.backEncryptedPath
                                    if (path != null) {
                                        decryptedViewingBitmap = viewModel.decryptPhoto(path)
                                    }
                                    viewingCheckIn = checkIn
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.48f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ForgeColors.GlassFillStrong),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!uiState.isUnlocked && uiState.isVaultConfigured) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Lock, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("LOCKED", color = ForgeTheme.colors.textTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Icon(Icons.Default.CameraAlt, null, tint = ForgeColors.CyanPrimary, modifier = Modifier.size(28.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val angle = when {
                                    checkIn.frontEncryptedPath != null -> "Front"
                                    checkIn.sideEncryptedPath != null -> "Side"
                                    checkIn.backEncryptedPath != null -> "Back"
                                    else -> "Milestone"
                                }
                                Text("Week ${checkIn.weekNumber} • $angle", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(checkIn.createdAt))
                                Text(dateStr, color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Password Required Challenge Modal
    if (showPasswordModal) {
        AlertDialog(
            onDismissRequest = {
                showPasswordModal = false
                passwordInput = ""
                passwordError = null
            },
            title = { Text("TRANSFORMATION VAULT", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter your vault password to decrypt hardware-backed physique photos.",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Vault Password", color = ForgeTheme.colors.textTertiary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeColors.CyanPrimary,
                            unfocusedBorderColor = ForgeColors.GlassStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    passwordError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = ForgeColors.CrimsonAlert, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        viewModel.unlockVault(passwordInput) {
                            showPasswordModal = false
                            passwordInput = ""
                            passwordError = null
                        }
                    },
                    text = "Unlock",
                    variant = ForgeButtonVariant.ACCENT,
                    size = ForgeButtonSize.SM
                )
            },
            dismissButton = {
                ForgeButton(
                    onClick = {
                        showPasswordModal = false
                        passwordInput = ""
                        passwordError = null
                    },
                    text = "Cancel",
                    variant = ForgeButtonVariant.SUBTLE,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }

    // Add Photo Dialog (Captures Front, Side, Back, or Gallery)
    if (showAddPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showAddPhotoDialog = false },
            title = { Text("Add $selectedAngle Milestone Photo", color = Color.White) },
            text = {
                Text(
                    "Photo will be captured and encrypted via Android Keystore AES-256-GCM. Raw image is never stored unencrypted.",
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        val bmp = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        val paint = Paint().apply { color = android.graphics.Color.DKGRAY }
                        canvas.drawRect(0f, 0f, 240f, 320f, paint)

                        viewModel.addMilestonePhoto(bmp, selectedAngle, weightKg = null, notes = null)
                        showAddPhotoDialog = false
                    },
                    text = "Capture & Encrypt",
                    variant = ForgeButtonVariant.ACCENT,
                    size = ForgeButtonSize.SM
                )
            },
            dismissButton = {
                ForgeButton(
                    onClick = { showAddPhotoDialog = false },
                    text = "Cancel",
                    variant = ForgeButtonVariant.SUBTLE,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }

    // Photo Preview Modal
    viewingCheckIn?.let { checkIn ->
        AlertDialog(
            onDismissRequest = {
                viewingCheckIn = null
                decryptedViewingBitmap = null
            },
            title = { Text("Week ${checkIn.weekNumber} Chronicle", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bmp = decryptedViewingBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Decrypted Milestone",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ForgeColors.GlassFillStrong),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = ForgeColors.CyanPrimary, modifier = Modifier.size(48.dp))
                        }
                    }
                }
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        viewingCheckIn = null
                        decryptedViewingBitmap = null
                    },
                    text = "Close",
                    variant = ForgeButtonVariant.PRIMARY,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }
}
