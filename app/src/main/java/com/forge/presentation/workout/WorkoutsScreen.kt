package com.forge.presentation.workout

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
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
import com.forge.core.designsystem.component.ForgeSegmentedControl
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    workoutsViewModel: WorkoutsViewModel,
    modifier: Modifier = Modifier,
    onStartEmptyWorkout: () -> Unit = {},
    onStartWorkoutForSchedule: ((focus: String, templateId: String?) -> Unit)? = null,
    onOpenExerciseLibrary: () -> Unit = {}
) {
    val uiState by workoutsViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Schedule, 1 = Recent, 2 = Templates

    var editingDay by remember { mutableStateOf<ScheduledDayUiModel?>(null) }
    var editingTemplateId by remember { mutableStateOf<String?>(null) }
    var editingTemplateTitle by remember { mutableStateOf("") }
    var editingExercises by remember { mutableStateOf<List<EditableExerciseItem>>(emptyList()) }
    var showEditorSheet by remember { mutableStateOf(false) }

    fun openEditorForDay(day: ScheduledDayUiModel) {
        val tmplId = day.templateId ?: "custom_${day.dayOfWeek}"
        workoutsViewModel.loadTemplateExercisesForEditing(tmplId) { items ->
            editingTemplateId = tmplId
            editingTemplateTitle = day.focus
            editingExercises = items
            showEditorSheet = true
        }
    }

    fun openEditorForTemplate(templateId: String, templateTitle: String) {
        workoutsViewModel.loadTemplateExercisesForEditing(templateId) { items ->
            editingTemplateId = templateId
            editingTemplateTitle = templateTitle
            editingExercises = items
            showEditorSheet = true
        }
    }

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
                Text(
                    text = "Workouts",
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                ForgeButton(
                    onClick = onStartEmptyWorkout,
                    text = "+ Quick Workout",
                    variant = ForgeButtonVariant.ACCENT,
                    size = ForgeButtonSize.SM
                )
            }

            // 3-Tab Segmented Control
            ForgeSegmentedControl(
                options = listOf("Schedule", "Recent", "Templates"),
                selectedIndex = selectedTabIndex,
                onSelectIndex = { selectedTabIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            when (selectedTabIndex) {
                0 -> {
                    // Schedule Tab
                    ForgeSectionHeader(label = "Weekly Cycle (Mon – Sun)")

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.weekSchedule.forEach { dayModel ->
                            val isToday = dayModel.isToday
                            val isRest = !dayModel.isTrainingDay

                            ForgeGlassSurface(
                                variant = if (isToday) ForgeGlassVariant.ELEVATED else ForgeGlassVariant.SUBTLE,
                                borderColor = if (isToday) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Day Badge
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isToday) ForgeColors.CyanPrimary.copy(alpha = 0.2f)
                                                    else ForgeColors.GlassFillSubtle
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isToday) ForgeColors.CyanPrimary else ForgeColors.GlassStroke,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayModel.dayName.uppercase(),
                                                color = if (isToday) ForgeColors.CyanPrimary else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = dayModel.focus,
                                                    color = if (isRest) ForgeTheme.colors.textSecondary else Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (dayModel.isCompleted) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Completed",
                                                        tint = ForgeColors.CyanPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${dayModel.dateStr} • ${if (isRest) "Active recovery & rest" else "Scheduled session"}",
                                                color = ForgeTheme.colors.textTertiary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (dayModel.isTrainingDay) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { openEditorForDay(dayModel) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Session",
                                                    tint = ForgeTheme.colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            if (isToday && !dayModel.isCompleted) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        onStartWorkoutForSchedule?.invoke(dayModel.focus, dayModel.templateId)
                                                            ?: onStartEmptyWorkout()
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(ForgeColors.CyanPrimary)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Start",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Recent Tab (Actual completed workout structures)
                    ForgeSectionHeader(label = "Recently Completed Sessions (${uiState.completedSessions.size})")

                    if (uiState.completedSessions.isEmpty()) {
                        ForgeGlassSurface(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.FitnessCenter, null, tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No Recent Workouts", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Completed workout logs will appear here.", color = ForgeTheme.colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.completedSessions.take(15).forEach { session ->
                                ForgeGlassSurface(
                                    variant = ForgeGlassVariant.SUBTLE,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(session.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US).format(Date(session.startTime))
                                            Text(
                                                text = "$dateStr • ${session.totalVolumeKg.toInt()} kg logged",
                                                color = ForgeTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ForgeButton(
                                                onClick = {
                                                    onStartWorkoutForSchedule?.invoke(session.name, null)
                                                        ?: onStartEmptyWorkout()
                                                },
                                                text = "Start",
                                                variant = ForgeButtonVariant.PRIMARY,
                                                size = ForgeButtonSize.SM,
                                                icon = Icons.Default.PlayArrow
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Templates Tab (Actual saved templates)
                    ForgeSectionHeader(label = "Routine Templates (${uiState.templates.size})")

                    if (uiState.templates.isEmpty()) {
                        ForgeGlassSurface(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Routine Templates Found",
                                    color = ForgeTheme.colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Templates will appear here after initialization or when you create a routine.",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.templates.forEach { tmpl ->
                                ForgeGlassSurface(
                                    variant = ForgeGlassVariant.SUBTLE,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tmpl.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "Focus: ${tmpl.focus} • v${tmpl.version}",
                                                color = ForgeTheme.colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Edit
                                            IconButton(
                                                onClick = { openEditorForTemplate(tmpl.id, tmpl.name) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, "Edit", tint = ForgeTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                                            }
                                            // Duplicate
                                            IconButton(
                                                onClick = { workoutsViewModel.duplicateTemplate(tmpl.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, "Duplicate", tint = ForgeColors.CyanPrimary, modifier = Modifier.size(18.dp))
                                            }
                                            // Delete
                                            IconButton(
                                                onClick = { workoutsViewModel.deleteTemplate(tmpl.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, "Delete", tint = ForgeColors.CrimsonAlert, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            // Start
                                            ForgeButton(
                                                onClick = {
                                                    onStartWorkoutForSchedule?.invoke(tmpl.name, tmpl.id)
                                                        ?: onStartEmptyWorkout()
                                                },
                                                text = "Start",
                                                variant = ForgeButtonVariant.PRIMARY,
                                                size = ForgeButtonSize.SM,
                                                icon = Icons.Default.PlayArrow
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Session Editor Modal
    if (showEditorSheet && editingTemplateId != null) {
        SessionEditorSheet(
            title = editingTemplateTitle,
            initialExercises = editingExercises,
            availableExercises = uiState.availableExercises,
            onDismiss = { showEditorSheet = false },
            onSave = { newTitle, updatedExercises ->
                workoutsViewModel.saveTemplateChanges(editingTemplateId!!, newTitle, updatedExercises)
                showEditorSheet = false
            }
        )
    }
}
