package com.forge.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
    onNavigateToNutrition: () -> Unit = {},
    onResetCompleted: () -> Unit = {}
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetConfirmation1 by remember { mutableStateOf(false) }
    var showResetConfirmation2 by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ForgeAmbientParticles(
            enabled = uiState.appSettings.particlesEnabled,
            reduceMotion = uiState.appSettings.reduceMotion,
            particleCount = 16
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Text(
                text = "Profile",
                color = ForgeTheme.colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // User Identity Card with Edit Action
            ForgeGlassSurface(
                variant = ForgeGlassVariant.ELEVATED,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(ForgeColors.GlassFillSubtle)
                                .border(1.dp, ForgeColors.CyanPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ForgeColors.CyanPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = uiState.userProfile?.name?.ifBlank { "Athlete" } ?: "Athlete",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.userProfile?.goal ?: "Hypertrophy"} • ${uiState.userProfile?.experience ?: "Intermediate"}",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                            val bodyText = "${uiState.userProfile?.heightCm?.toInt() ?: 175} cm • ${uiState.userProfile?.weightKg?.toInt() ?: 75} kg"
                            Text(
                                text = bodyText,
                                color = ForgeColors.CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    ForgeButton(
                        onClick = { showEditProfileSheet = true },
                        text = "EDIT",
                        variant = ForgeButtonVariant.ACCENT,
                        size = ForgeButtonSize.SM,
                        icon = Icons.Default.Edit
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Athletic Passport
            ForgeSectionHeader(label = "Athletic Passport")
            ForgeGlassSurface(
                variant = ForgeGlassVariant.SUBTLE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PassportRow("Target Calories", "${uiState.userProfile?.targetCalories ?: 2400} kcal")
                    PassportRow("Target Protein", "${uiState.userProfile?.targetProteinG ?: 160}g")
                    PassportRow("Workouts Completed", "${uiState.totalWorkoutsCompleted}")
                    PassportRow("Personal Records", "${uiState.personalRecords.size}")
                    PassportRow("Weekly Training Split", "${uiState.userProfile?.daysPerWeek ?: 4} days / week")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Training & Nutrition Settings
            ForgeSectionHeader(label = "Training & Nutrition")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingNavigationRow(
                    icon = Icons.Default.Restaurant,
                    title = "Nutrition Targets & Macros",
                    subtitle = "${uiState.userProfile?.targetCalories ?: 2400} kcal • ${uiState.userProfile?.targetProteinG ?: 160}g Protein",
                    onClick = onNavigateToNutrition
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Health & System Integrations
            ForgeSectionHeader(label = "Health & System")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingToggleRow(
                    icon = Icons.Default.DirectionsWalk,
                    title = "Health Connect Sync",
                    subtitle = if (uiState.appSettings.healthConnectEnabled) "Syncing verified daily activity" else "Disconnected",
                    checked = uiState.appSettings.healthConnectEnabled,
                    onCheckedChange = { profileViewModel.toggleHealthConnect(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notifications
            ForgeSectionHeader(label = "Notifications & Reminders")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Workout Reminders",
                    subtitle = "Morning alerts on scheduled training days",
                    checked = uiState.appSettings.notifWorkoutReminders,
                    onCheckedChange = { profileViewModel.toggleWorkoutReminders(it) }
                )
                SettingToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Pre-Workout Alerts",
                    subtitle = "30-minute advance prep notifications",
                    checked = uiState.appSettings.notifPreWorkoutAlerts,
                    onCheckedChange = { profileViewModel.togglePreWorkoutAlerts(it) }
                )
                SettingToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Streak & PR Alerts",
                    subtitle = "Celebrations on new records and milestones",
                    checked = uiState.appSettings.notifStreakReminders,
                    onCheckedChange = { profileViewModel.toggleStreakReminders(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Security
            ForgeSectionHeader(label = "Privacy & Security")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingNavigationRow(
                    icon = Icons.Default.Lock,
                    title = "Change Vault Password",
                    subtitle = "Update PBKDF2 hash protecting physique photos",
                    onClick = { showPasswordChangeDialog = true }
                )
                SettingToggleRow(
                    icon = Icons.Default.Lock,
                    title = "Auto-Lock On Background",
                    subtitle = "Require password when app loses focus",
                    checked = uiState.appSettings.autoLockVaultOnBackground,
                    onCheckedChange = { profileViewModel.toggleAutoLock(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Appearance & Haptics
            ForgeSectionHeader(label = "Appearance & Motion")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingToggleRow(
                    icon = Icons.Default.Visibility,
                    title = "Reduce Motion",
                    subtitle = "Disables heavy particle movement and animations",
                    checked = uiState.appSettings.reduceMotion,
                    onCheckedChange = { profileViewModel.toggleReduceMotion(it) }
                )
                SettingToggleRow(
                    icon = Icons.Default.Visibility,
                    title = "Ambient Particles",
                    subtitle = "Liquid Glass floating particle canvas",
                    checked = uiState.appSettings.particlesEnabled,
                    onCheckedChange = { profileViewModel.toggleParticles(it) }
                )
                SettingToggleRow(
                    icon = Icons.Default.FitnessCenter,
                    title = "Haptic Feedback",
                    subtitle = "Tactile confirmation for set logs and timers",
                    checked = uiState.appSettings.hapticsEnabled,
                    onCheckedChange = { profileViewModel.toggleHaptics(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Data Management
            ForgeSectionHeader(label = "Data Management")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingNavigationRow(
                    icon = Icons.Default.Download,
                    title = "Export User Data",
                    subtitle = "Export profile, workout sessions, and PRs to JSON",
                    onClick = {
                        profileViewModel.exportUserData()
                        showExportDialog = true
                    }
                )
                SettingNavigationRow(
                    icon = Icons.Default.Delete,
                    title = "Reset All Data",
                    subtitle = "Destructive reset of local database and vault",
                    onClick = { showResetConfirmation1 = true },
                    isDestructive = true
                )
            }
        }
    }

    // Modal Edit Profile Sheet
    if (showEditProfileSheet) {
        EditProfileSheet(
            userProfile = uiState.userProfile,
            onDismiss = { showEditProfileSheet = false },
            onSave = { name, h, w, a, s, g, exp, days, dur, eq ->
                profileViewModel.updateUserProfile(name, h, w, a, s, g, exp, days, dur, eq)
            }
        )
    }

    // Change Password Dialog
    if (showPasswordChangeDialog) {
        var oldP by remember { mutableStateOf("") }
        var newP by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            title = { Text("Change Vault Password", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldP,
                        onValueChange = { oldP = it },
                        label = { Text("Current Password", color = ForgeTheme.colors.textSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeColors.CyanPrimary,
                            unfocusedBorderColor = ForgeColors.GlassStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = newP,
                        onValueChange = { newP = it },
                        label = { Text("New Password", color = ForgeTheme.colors.textSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeColors.CyanPrimary,
                            unfocusedBorderColor = ForgeColors.GlassStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    err?.let { Text(it, color = ForgeColors.CrimsonAlert, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        profileViewModel.changeVaultPassword(oldP, newP) { success, msg ->
                            if (success) {
                                showPasswordChangeDialog = false
                            } else {
                                err = msg
                            }
                        }
                    },
                    text = "Update Password",
                    variant = ForgeButtonVariant.ACCENT,
                    size = ForgeButtonSize.SM
                )
            },
            dismissButton = {
                ForgeButton(
                    onClick = { showPasswordChangeDialog = false },
                    text = "Cancel",
                    variant = ForgeButtonVariant.SUBTLE,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }

    // Export Data Dialog
    if (showExportDialog && uiState.dataExportText != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export User Data", color = Color.White) },
            text = {
                Column {
                    Text("Clean JSON export of your personal FORGE data:", color = ForgeTheme.colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(ForgeColors.GlassFillStrong)
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(uiState.dataExportText.orEmpty(), color = ForgeColors.CyanPrimary, fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                ForgeButton(
                    onClick = { showExportDialog = false },
                    text = "Done",
                    variant = ForgeButtonVariant.PRIMARY,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }

    // Reset Confirmation 1
    if (showResetConfirmation1) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation1 = false },
            title = { Text("Reset All Data?", color = ForgeColors.CrimsonAlert, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will delete your workout history, PRs, schedules, and custom settings. This action cannot be undone.",
                    color = Color.White,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        showResetConfirmation1 = false
                        showResetConfirmation2 = true
                    },
                    text = "Continue to Reset",
                    variant = ForgeButtonVariant.DANGER,
                    size = ForgeButtonSize.SM
                )
            },
            dismissButton = {
                ForgeButton(
                    onClick = { showResetConfirmation1 = false },
                    text = "Cancel",
                    variant = ForgeButtonVariant.SUBTLE,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }

    // Reset Confirmation 2 (Final Destructive Gate)
    if (showResetConfirmation2) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation2 = false },
            title = { Text("FINAL CONFIRMATION", color = ForgeColors.CrimsonAlert, fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Are you absolutely sure you want to wipe all local data and reset FORGE to a fresh install?",
                    color = Color.White,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        showResetConfirmation2 = false
                        profileViewModel.resetAllData {
                            onResetCompleted()
                        }
                    },
                    text = "WIPE & RESET",
                    variant = ForgeButtonVariant.DANGER,
                    size = ForgeButtonSize.SM
                )
            },
            dismissButton = {
                ForgeButton(
                    onClick = { showResetConfirmation2 = false },
                    text = "Abort",
                    variant = ForgeButtonVariant.SUBTLE,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }
}

@Composable
private fun PassportRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = ForgeTheme.colors.textSecondary, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ForgeGlassSurface(
        variant = ForgeGlassVariant.SUBTLE,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = ForgeTheme.colors.textTertiary, fontSize = 11.sp)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = ForgeColors.CyanPrimary,
                    uncheckedThumbColor = ForgeTheme.colors.textSecondary,
                    uncheckedTrackColor = ForgeColors.GlassFillSubtle
                )
            )
        }
    }
}

@Composable
private fun SettingNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    ForgeGlassSurface(
        variant = ForgeGlassVariant.SUBTLE,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) ForgeColors.CrimsonAlert else ForgeColors.CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column {
                    Text(
                        text = title,
                        color = if (isDestructive) ForgeColors.CrimsonAlert else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(subtitle, color = ForgeTheme.colors.textTertiary, fontSize = 11.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = ForgeTheme.colors.textSecondary)
        }
    }
}
