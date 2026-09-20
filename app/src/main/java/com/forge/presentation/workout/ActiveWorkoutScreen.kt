package com.forge.presentation.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.component.ForgeChip
import com.forge.core.designsystem.component.ForgeIconButton
import com.forge.core.designsystem.component.ForgeProgressBar
import com.forge.core.designsystem.component.ForgeRing
import com.forge.core.designsystem.component.GlassVariant
import com.forge.core.designsystem.component.liquidGlass
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.WorkoutSetEntity
import com.forge.domain.engine.RestTimerState
import com.forge.presentation.exercise.ExerciseLibraryTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session

    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ActiveWorkoutContent(
            sessionName = session?.name ?: "Empty Workout",
            elapsedSeconds = uiState.elapsedSeconds,
            exercisesWithSets = uiState.exercisesWithSets,
            restTimerState = uiState.restTimerState,
            currentRestExerciseName = uiState.currentRestExerciseName,
            latestPrNotification = uiState.latestPrNotification,
            onNavigateBack = onNavigateBack,
            onFinishClick = { showFinishDialog = true },
            onDiscardClick = { showDiscardDialog = true },
            onAddExerciseClick = { viewModel.setAddExerciseSheetVisible(true) },
            onAddSetClick = { exerciseId -> viewModel.addSet(exerciseId) },
            onSetUpdate = { set, weight, reps, rir ->
                viewModel.updateSetValues(set, weight, reps, set.rpe, rir)
            },
            onToggleSetCompleted = { set, exerciseName -> viewModel.toggleSetCompleted(set, exerciseName) },
            onAddRestSeconds = { seconds -> viewModel.addRestSeconds(seconds) },
            onSkipRest = { viewModel.skipRestTimer() }
        )

        uiState.latestPrNotification?.let { prKey ->
            com.forge.core.designsystem.component.ForgeDisintegrationEffect(
                triggerKey = prKey,
                particleCount = 50,
                accentColor = com.forge.core.designsystem.theme.ForgeColors.CyanPrimary
            )
        }
    }

    // Add Exercise Bottom Sheet
    if (uiState.showAddExerciseSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.setAddExerciseSheetVisible(false) },
            sheetState = sheetState,
            containerColor = Color(0xFF0F0F14)
        ) {
            AddExerciseSheetContent(
                activeTab = uiState.addExerciseTab,
                onSelectTab = { viewModel.setAddExerciseTab(it) },
                searchResults = uiState.searchResults,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onSelectExercise = { viewModel.addExerciseToWorkout(it) },
                onCreateCustomExercise = { viewModel.createAndAddCustomExercise(it) }
            )
        }
    }

    // Finish Workout Confirmation Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    text = "Finish Workout?",
                    color = ForgeTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "All completed sets will be recorded to your history and progressive overload progress updated.",
                    color = ForgeTheme.colors.textSecondary
                )
            },
            confirmButton = {
                ForgeButton(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finishWorkout { onNavigateBack() }
                    },
                    variant = ForgeButtonVariant.ACCENT,
                    size = ForgeButtonSize.SM,
                    text = "Finish & Save"
                )
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continue Workout", color = ForgeTheme.colors.textSecondary)
                }
            },
            containerColor = Color(0xFF131318)
        )
    }

    // Discard Workout Confirmation Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "Discard Workout?",
                    color = ForgeTheme.colors.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure? This workout session will be deleted and no sets will be saved.",
                    color = ForgeTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout { onNavigateBack() }
                    }
                ) {
                    Text("Discard", color = ForgeTheme.colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = ForgeTheme.colors.textSecondary)
                }
            },
            containerColor = Color(0xFF131318)
        )
    }
}

@Composable
fun ActiveWorkoutContent(
    sessionName: String,
    elapsedSeconds: Long,
    exercisesWithSets: List<ExerciseWithSets>,
    restTimerState: RestTimerState,
    currentRestExerciseName: String,
    latestPrNotification: String?,
    onNavigateBack: () -> Unit,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onAddSetClick: (String) -> Unit,
    onSetUpdate: (WorkoutSetEntity, Double, Int, Int?) -> Unit,
    onToggleSetCompleted: (WorkoutSetEntity, String) -> Unit,
    onAddRestSeconds: (Int) -> Unit,
    onSkipRest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSets = exercisesWithSets.flatMap { it.sets }.size
    val doneSets = exercisesWithSets.flatMap { it.sets }.count { it.isCompleted }
    val progressPct = if (totalSets > 0) doneSets.toFloat() / totalSets.toFloat() else 0f

    var isRestMinimized by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Top Bar matching proto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xD908080C))
                    .padding(top = 16.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ForgeIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onNavigateBack,
                            size = 42.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ELAPSED",
                                color = ForgeTheme.colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = formatElapsedTime(elapsedSeconds),
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onDiscardClick) {
                                Text("Discard", color = ForgeTheme.colors.textMuted, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            ForgeButton(
                                onClick = onFinishClick,
                                variant = ForgeButtonVariant.PRIMARY,
                                size = ForgeButtonSize.SM,
                                text = "Finish"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    ForgeProgressBar(progress = progressPct, height = 4.dp)
                }
            }

            // Exercises Scroll List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (exercisesWithSets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(variant = GlassVariant.SUBTLE, cornerRadius = 24.dp)
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Ready to train",
                                    color = ForgeTheme.colors.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add your first movement to begin tracking sets",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                ForgeButton(
                                    onClick = onAddExerciseClick,
                                    variant = ForgeButtonVariant.ACCENT,
                                    size = ForgeButtonSize.MD,
                                    icon = Icons.Filled.Add,
                                    text = "Add Exercise"
                                )
                            }
                        }
                    }
                } else {
                    items(exercisesWithSets, key = { it.exercise.id }) { item ->
                        ExerciseBlock(
                            exercise = item.exercise,
                            sets = item.sets,
                            progressionRecord = item.progressionRecord,
                            personalRecord = item.personalRecord,
                            onAddSetClick = { onAddSetClick(item.exercise.id) },
                            onSetUpdate = onSetUpdate,
                            onToggleSetCompleted = { set -> onToggleSetCompleted(set, item.exercise.name) }
                        )
                    }

                    item {
                        ForgeButton(
                            onClick = onAddExerciseClick,
                            variant = ForgeButtonVariant.GLASS,
                            size = ForgeButtonSize.MD,
                            icon = Icons.Filled.Add,
                            text = "Add Another Exercise",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Fullscreen Rest Countdown Overlay matching proto ActiveWorkout.jsx
        if (restTimerState.isRunning && !isRestMinimized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6060609))
                    .clickable { /* absorb taps */ },
                contentAlignment = Alignment.Center
            ) {
                // Dismiss / minimize button in top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 28.dp, end = 20.dp)
                ) {
                    ForgeIconButton(
                        icon = Icons.Filled.Close,
                        onClick = { isRestMinimized = true },
                        size = 44.dp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "REST",
                        color = ForgeTheme.colors.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val totalDuration = if (restTimerState.restDurationSeconds > 0) restTimerState.restDurationSeconds else 90
                    val fraction = (restTimerState.remainingSeconds.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                    ForgeRing(
                        progress = fraction,
                        size = 240.dp,
                        strokeWidth = 14.dp,
                        progressColor = ForgeTheme.colors.accent,
                        trackColor = Color(0x14FFFFFF)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatRestSeconds(restTimerState.remainingSeconds),
                                color = ForgeTheme.colors.textPrimary,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (restTimerState.remainingSeconds <= 10) "Get ready." else "Next set: ${currentRestExerciseName.ifBlank { "Next exercise" }}",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ForgeButton(
                            onClick = { onAddRestSeconds(15) },
                            variant = ForgeButtonVariant.GLASS,
                            size = ForgeButtonSize.MD,
                            text = "+15s"
                        )
                        ForgeButton(
                            onClick = onSkipRest,
                            variant = ForgeButtonVariant.ACCENT,
                            size = ForgeButtonSize.MD,
                            text = "Skip"
                        )
                    }
                }
            }
        } else if (restTimerState.isRunning && isRestMinimized) {
            // Minimized pill in bottom corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 24.dp)
                    .liquidGlass(variant = GlassVariant.STRONG, cornerRadius = 24.dp)
                    .clickable { isRestMinimized = false }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "REST: ${formatRestSeconds(restTimerState.remainingSeconds)}",
                        color = ForgeTheme.colors.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SKIP",
                        color = ForgeTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSkipRest() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseBlock(
    exercise: ExerciseEntity,
    sets: List<WorkoutSetEntity>,
    progressionRecord: com.forge.data.local.entity.ExerciseProgressionRecordEntity?,
    personalRecord: com.forge.data.local.entity.ExercisePersonalRecordEntity?,
    onAddSetClick: () -> Unit,
    onSetUpdate: (WorkoutSetEntity, Double, Int, Int?) -> Unit,
    onToggleSetCompleted: (WorkoutSetEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(variant = GlassVariant.DEFAULT, cornerRadius = 24.dp)
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = exercise.name,
                color = ForgeTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${exercise.forgeMovementPattern.replace("_", " ")}${if (!exercise.sourceEquipment.isNullOrBlank()) " · " + exercise.sourceEquipment else ""}",
                color = ForgeTheme.colors.textSecondary,
                fontSize = 12.sp
            )

            // Progression recommendation banner if available
            if (progressionRecord != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(variant = GlassVariant.SUBTLE, cornerRadius = 12.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Target: ${progressionRecord.recommendedWeightKg} kg × ${progressionRecord.recommendedRepMin}–${progressionRecord.recommendedRepMax}",
                        color = ForgeTheme.colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sets Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SET", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                Text("KG", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("REPS", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("RIR", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(44.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            sets.forEachIndexed { index, set ->
                SetItemRow(
                    setNumber = index + 1,
                    set = set,
                    onUpdate = { weight, reps, rir -> onSetUpdate(set, weight, reps, rir) },
                    onToggleComplete = { onToggleSetCompleted(set) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            ForgeButton(
                onClick = onAddSetClick,
                variant = ForgeButtonVariant.SUBTLE,
                size = ForgeButtonSize.SM,
                icon = Icons.Filled.Add,
                text = "Add Set",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SetItemRow(
    setNumber: Int,
    set: WorkoutSetEntity,
    onUpdate: (Double, Int, Int?) -> Unit,
    onToggleComplete: () -> Unit
) {
    var weightText by remember(set.weightKg) {
        mutableStateOf(if (set.weightKg > 0) set.weightKg.toString() else "")
    }
    var repsText by remember(set.reps) {
        mutableStateOf(if (set.reps > 0) set.reps.toString() else "")
    }
    var rirText by remember(set.rir) {
        mutableStateOf(set.rir?.toString() ?: "2")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                variant = if (set.isCompleted) GlassVariant.STRONG else GlassVariant.SUBTLE,
                cornerRadius = 16.dp
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$setNumber",
                color = ForgeTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(36.dp)
            )

            // Weight Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0EFFFFFF))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = weightText,
                    onValueChange = {
                        weightText = it
                        val w = it.toDoubleOrNull() ?: 0.0
                        onUpdate(w, set.reps, set.rir)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(ForgeTheme.colors.primary),
                    decorationBox = { innerTextField ->
                        if (weightText.isEmpty()) {
                            Text("-", color = ForgeTheme.colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Reps Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0EFFFFFF))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = repsText,
                    onValueChange = {
                        repsText = it
                        val r = it.toIntOrNull() ?: 0
                        onUpdate(set.weightKg, r, set.rir)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(ForgeTheme.colors.primary),
                    decorationBox = { innerTextField ->
                        if (repsText.isEmpty()) {
                            Text("-", color = ForgeTheme.colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // RIR Input
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0EFFFFFF))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = rirText,
                    onValueChange = {
                        rirText = it
                        val rir = it.toIntOrNull()
                        onUpdate(set.weightKg, set.reps, rir)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(ForgeTheme.colors.primary),
                    decorationBox = { innerTextField ->
                        if (rirText.isEmpty()) {
                            Text("2", color = ForgeTheme.colors.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Emerald Check Button matching proto
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (set.isCompleted) Color(0xFF10B981) else Color(0x14FFFFFF),
                        CircleShape
                    )
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (set.isCompleted) Color.White else Color(0x66FFFFFF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddExerciseSheetContent(
    activeTab: ExerciseLibraryTab,
    onSelectTab: (ExerciseLibraryTab) -> Unit,
    searchResults: List<ExerciseEntity>,
    onSearchQueryChange: (String) -> Unit,
    onSelectExercise: (ExerciseEntity) -> Unit,
    onCreateCustomExercise: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Add Exercise",
            color = ForgeTheme.colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar matching proto
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(CircleShape)
                .background(Color(0x0EFFFFFF), CircleShape)
                .border(1.dp, Color(0x14FFFFFF), CircleShape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = ForgeTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearchQueryChange(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(ForgeTheme.colors.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text("Search 876 exercises", color = ForgeTheme.colors.textSecondary, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs if not searching
        if (query.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair(ExerciseLibraryTab.POPULAR, "Popular"),
                    Pair(ExerciseLibraryTab.RECENT, "Recent"),
                    Pair(ExerciseLibraryTab.FAVORITES, "Favorites"),
                    Pair(ExerciseLibraryTab.BROWSE_ALL, "Browse All")
                ).forEach { (tab, label) ->
                    ForgeChip(
                        selected = activeTab == tab,
                        onClick = { onSelectTab(tab) },
                        label = label
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (searchResults.isEmpty() && query.isNotBlank()) {
            ForgeButton(
                onClick = { onCreateCustomExercise(query) },
                variant = ForgeButtonVariant.ACCENT,
                size = ForgeButtonSize.MD,
                text = "+ Create \"$query\" as Custom Exercise",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.id }) { exercise ->
                    val muscleLabel = exercise.sourceCategory ?: exercise.forgeMovementPattern.replace("_", " ")
                    val muscleColor = ForgeTheme.colors.getMuscleColor(muscleLabel)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(variant = GlassVariant.DEFAULT, cornerRadius = 18.dp)
                            .clickable { onSelectExercise(exercise) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Colored Initial Badge matching proto
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(muscleColor.copy(alpha = 0.15f))
                                    .border(1.dp, muscleColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = muscleLabel.take(1).uppercase(java.util.Locale.ROOT),
                                    color = muscleColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    color = ForgeTheme.colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$muscleLabel · ${exercise.sourceEquipment ?: "Bodyweight"}",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            if (exercise.isFavorite) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = ForgeTheme.colors.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "+ ADD",
                                color = ForgeTheme.colors.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatElapsedTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

fun formatRestSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
