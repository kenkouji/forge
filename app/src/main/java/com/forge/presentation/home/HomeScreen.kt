package com.forge.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeAmbientParticles
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.component.ForgeSectionHeader
import com.forge.core.designsystem.component.ForgeStatTile
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.domain.engine.AssistantService
import com.forge.presentation.assistant.AssistantSheet
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    assistantService: AssistantService? = null,
    hasActiveWorkout: Boolean = false,
    activeWorkoutTitle: String? = null,
    onStartWorkoutClick: () -> Unit = {},
    onResumeWorkoutClick: () -> Unit = {},
    onExploreExercisesClick: () -> Unit = {},
    onLogWeightClick: () -> Unit = {},
    onProgressPhotoClick: () -> Unit = {},
    onViewAllProgressClick: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showAssistantSheet by remember { mutableStateOf(false) }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ForgeAmbientParticles(enabled = true, reduceMotion = false, particleCount = 18)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // 1. Header & Greeting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = uiState.userName.ifBlank { "Athlete" },
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // 2. TODAY (Active Workout / Scheduled Workout / Rest Day)
            ForgeSectionHeader(label = "Today")
            ForgeGlassSurface(
                variant = ForgeGlassVariant.ELEVATED,
                borderColor = if (hasActiveWorkout) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val badgeText = when {
                                hasActiveWorkout -> "ACTIVE SESSION"
                                uiState.isTodayTrainingDay -> "SCHEDULED SESSION"
                                else -> "REST & RECOVERY"
                            }
                            val badgeColor = when {
                                hasActiveWorkout -> ForgeColors.CyanPrimary
                                uiState.isTodayTrainingDay -> ForgeColors.AmberGlow
                                else -> ForgeTheme.colors.textSecondary
                            }
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val title = when {
                                hasActiveWorkout -> activeWorkoutTitle ?: "In Progress"
                                uiState.isTodayTrainingDay -> uiState.todaySchedule?.focus ?: "Training Session"
                                else -> "Recovery Day"
                            }
                            Text(
                                text = title,
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            val sub = when {
                                hasActiveWorkout -> "Session in progress — resume to log sets"
                                uiState.isTodayTrainingDay -> "Ready to train • Target your focal volume"
                                uiState.nextTrainingSchedule != null -> "Next: ${uiState.nextTrainingSchedule?.focus ?: "Upcoming Session"}"
                                else -> "Focus on hydration, protein intake, and mobility"
                            }
                            Text(
                                text = sub,
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ForgeColors.GlassFillSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isTodayTrainingDay || hasActiveWorkout) Icons.Filled.FitnessCenter else Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = if (hasActiveWorkout) ForgeColors.CyanPrimary else ForgeTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (hasActiveWorkout) {
                        ForgeButton(
                            onClick = onResumeWorkoutClick,
                            variant = ForgeButtonVariant.ACCENT,
                            size = ForgeButtonSize.MD,
                            icon = Icons.Filled.PlayArrow,
                            text = "Resume Workout",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (uiState.isTodayTrainingDay) {
                        ForgeButton(
                            onClick = onStartWorkoutClick,
                            variant = ForgeButtonVariant.PRIMARY,
                            size = ForgeButtonSize.MD,
                            icon = Icons.Filled.PlayArrow,
                            text = "Start ${uiState.todaySchedule?.focus ?: "Session"}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        ForgeButton(
                            onClick = onStartWorkoutClick,
                            variant = ForgeButtonVariant.SUBTLE,
                            size = ForgeButtonSize.SM,
                            icon = Icons.Filled.Add,
                            text = "Log Extra Session",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. PERSONALIZED ASSISTANT (Context-aware card between Today and Training Snapshot)
            if (assistantService != null) {
                ForgeGlassSurface(
                    variant = ForgeGlassVariant.STRONG,
                    borderColor = ForgeColors.CyanPrimary.copy(alpha = 0.5f),
                    onClick = { showAssistantSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ForgeColors.CyanPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ForgeColors.CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Personalized Assistant",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ask about schedule, meals, PRs, or modify routines",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ForgeColors.CyanPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ASK",
                                color = ForgeColors.CyanPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 4. TRAINING SNAPSHOT (Streak, weekly sessions, volume, goal)
            ForgeSectionHeader(label = "Training Snapshot")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ForgeStatTile(
                        icon = Icons.Filled.LocalFireDepartment,
                        label = "Streak",
                        value = "${uiState.streakDays}",
                        unit = if (uiState.streakDays == 1) "day" else "days",
                        modifier = Modifier.weight(1f)
                    )
                    ForgeStatTile(
                        icon = Icons.Filled.FitnessCenter,
                        label = "This Week",
                        value = "${uiState.thisWeekSessionsCount}",
                        unit = "sessions",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ForgeStatTile(
                        icon = Icons.Filled.Speed,
                        label = "Volume",
                        value = String.format(java.util.Locale.US, "%.1f", uiState.totalVolumeTons),
                        unit = "t",
                        modifier = Modifier.weight(1f)
                    )
                    ForgeStatTile(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Program",
                        value = uiState.fitnessGoal.take(6),
                        unit = null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. ACTIVITY (Zero Fake Real-time Data with Explicit Provenance Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForgeSectionHeader(label = "Activity")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val provenance = if (uiState.healthActivity.activeCalories > 0) "MEASURED" else "ESTIMATED"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ForgeColors.GlassFillSubtle)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = provenance,
                            color = ForgeColors.CyanPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { homeViewModel.syncHealthData() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = ForgeTheme.colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            ForgeGlassSurface(
                variant = ForgeGlassVariant.SUBTLE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = ForgeTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.healthActivity.steps > 0) "${uiState.healthActivity.steps}" else "—",
                            color = ForgeTheme.colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "steps", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(ForgeColors.GlassStroke)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = ForgeTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.healthActivity.activeCalories > 0) "${uiState.healthActivity.activeCalories}" else "—",
                            color = ForgeTheme.colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "active kcal", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }

            // 6. PROGRESS / RECENT PR (Naturally collapses when no PR exists)
            if (uiState.latestPr != null) {
                Spacer(modifier = Modifier.height(14.dp))
                ForgeSectionHeader(label = "Recent Personal Record")
                ForgeGlassSurface(
                    variant = ForgeGlassVariant.STRONG,
                    borderColor = ForgeColors.CyanPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ForgeColors.CyanPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = ForgeColors.CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = uiState.latestPrExerciseName ?: "Compound Movement",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${uiState.latestPr?.maxRepsAtMaxWeight ?: 1} reps achieved",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "${uiState.latestPr?.maxWeightKg ?: 0.0} kg",
                            color = ForgeColors.CyanPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. CONTEXTUAL QUICK ACTIONS
            ForgeSectionHeader(label = "Quick Actions")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ForgeGlassSurface(
                    variant = ForgeGlassVariant.SUBTLE,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExploreExercisesClick() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = ForgeColors.CyanPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Library", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                ForgeGlassSurface(
                    variant = ForgeGlassVariant.SUBTLE,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProgressPhotoClick() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, tint = ForgeColors.AmberGlow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Chronicle", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                ForgeGlassSurface(
                    variant = ForgeGlassVariant.SUBTLE,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewAllProgressClick() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Analytics", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Transformation Milestone Check-In Dialog
    uiState.checkInDueWeek?.let { weekNum ->
        AlertDialog(
            onDismissRequest = { homeViewModel.dismissCheckInPrompt() },
            title = { Text("YOUR TRANSFORMATION CHECK-IN", color = ForgeColors.CyanPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp) },
            text = {
                Text(
                    text = "Week $weekNum check-in is due today. Document your physique progression in hardware-backed storage.",
                    color = Color.White,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        homeViewModel.dismissCheckInPrompt()
                        onProgressPhotoClick()
                    },
                    text = "Add Photos",
                    variant = ForgeButtonVariant.ACCENT,
                    size = ForgeButtonSize.SM
                )
            },
            dismissButton = {
                ForgeButton(
                    onClick = { homeViewModel.dismissCheckInPrompt() },
                    text = "Remind Me Later",
                    variant = ForgeButtonVariant.SUBTLE,
                    size = ForgeButtonSize.SM
                )
            },
            containerColor = ForgeTheme.colors.surface
        )
    }

    if (showAssistantSheet && assistantService != null) {
        AssistantSheet(
            assistantService = assistantService,
            onDismiss = { showAssistantSheet = false }
        )
    }
}
