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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Add Exercise Bottom Sheet
    if (uiState.showAddExerciseSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.setAddExerciseSheetVisible(false) },
            sheetState = sheetState,
            containerColor = ForgeTheme.colors.surfaceElevated
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
                val totalSets = uiState.exercisesWithSets.sumOf { it.sets.count { s -> s.isCompleted } }
                Text(
                    text = "Great session! You completed $totalSets sets in ${formatElapsedTime(uiState.elapsedSeconds)}. Save workout & update progressive overload?",
                    color = ForgeTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finishWorkout(onFinished = onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.primary)
                ) {
                    Text("Save & Finish", color = ForgeTheme.colors.background, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel", color = ForgeTheme.colors.textSecondary)
                }
            },
            containerColor = ForgeTheme.colors.surfaceElevated
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
                    text = "Are you sure you want to discard this workout? All logged sets for this session will be permanently dropped.",
                    color = ForgeTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout(onDiscarded = onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.error)
                ) {
                    Text("Discard Workout", color = ForgeTheme.colors.background, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Training", color = ForgeTheme.colors.textSecondary)
                }
            },
            containerColor = ForgeTheme.colors.surfaceElevated
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
    ) {
        // Top Gym Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForgeTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "←",
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onNavigateBack)
                        .padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                )
                Column {
                    Text(
                        text = sessionName,
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatElapsedTime(elapsedSeconds),
                        color = ForgeTheme.colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDiscardClick,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = "DISCARD",
                        color = ForgeTheme.colors.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onFinishClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "FINISH",
                        color = ForgeTheme.colors.background,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Real-Time Rest Timer Bar (Liquid Glass)
        AnimatedVisibility(
            visible = restTimerState.isRunning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .liquidGlass(
                        backgroundColor = Color(0x2210B981),
                        borderColor = Color(0x6610B981),
                        cornerRadius = 12.dp
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REST TIMER",
                            color = ForgeTheme.colors.success,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formatRestSeconds(restTimerState.remainingSeconds),
                            color = ForgeTheme.colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "-15s",
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1FFFFFFF))
                                .clickable { onAddRestSeconds(-15) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        Text(
                            text = "+30s",
                            color = ForgeTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1FFFFFFF))
                                .clickable { onAddRestSeconds(30) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        Text(
                            text = "SKIP",
                            color = ForgeTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x22FFFFFF))
                                .clickable { onSkipRest() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Real-Time PR Banner
        AnimatedVisibility(
            visible = latestPrNotification != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .liquidGlass(
                        backgroundColor = Color(0x33F59E0B),
                        borderColor = Color(0x88F59E0B),
                        cornerRadius = 10.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏆", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = latestPrNotification ?: "",
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Exercises List or Empty State
        if (exercisesWithSets.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO EXERCISES YET",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your first exercise to start recording sets.",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAddExerciseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+ ADD EXERCISE",
                            color = ForgeTheme.colors.background,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(exercisesWithSets, key = { it.exercise.id }) { item ->
                    ExerciseCard(
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
                    Button(
                        onClick = onAddExerciseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.surfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "+ ADD ANOTHER EXERCISE",
                            color = ForgeTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseEntity,
    sets: List<WorkoutSetEntity>,
    progressionRecord: com.forge.data.local.entity.ExerciseProgressionRecordEntity?,
    personalRecord: com.forge.data.local.entity.ExercisePersonalRecordEntity?,
    onAddSetClick: () -> Unit,
    onSetUpdate: (WorkoutSetEntity, Double, Int, Int?) -> Unit,
    onToggleSetCompleted: (WorkoutSetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 14.dp)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    color = ForgeTheme.colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = exercise.forgeMovementPattern.replace("_", " "),
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            // Target Progressive Overload Banner
            if (progressionRecord != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 TARGET: ${progressionRecord.recommendedWeightKg} kg × ${progressionRecord.recommendedRepMin}–${progressionRecord.recommendedRepMax} reps",
                        color = ForgeTheme.colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Current PR Banner
            if (personalRecord != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🏆 PR: ${personalRecord.maxWeightKg} kg × ${personalRecord.maxRepsAtMaxWeight} (Est. 1RM: ${personalRecord.estimated1RmKg} kg)",
                    color = ForgeTheme.colors.textMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SET", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                Text("KG", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("REPS", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("RIR", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                Text("DONE", color = ForgeTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(8.dp))

            sets.forEachIndexed { index, set ->
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    onUpdate = { weight, reps, rir -> onSetUpdate(set, weight, reps, rir) },
                    onToggleComplete = { onToggleSetCompleted(set) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onAddSetClick,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = "+ ADD SET",
                    color = ForgeTheme.colors.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SetRow(
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

    val backgroundColor = if (set.isCompleted) {
        Color(0x2210B981)
    } else {
        ForgeTheme.colors.background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setNumber.toString(),
            color = ForgeTheme.colors.textSecondary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp)
        )

        // Weight Input Field
        OutlinedTextField(
            value = weightText,
            onValueChange = {
                weightText = it
                val weight = it.toDoubleOrNull() ?: 0.0
                val reps = repsText.toIntOrNull() ?: 0
                val rir = rirText.toIntOrNull()
                onUpdate(weight, reps, rir)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ForgeTheme.colors.textPrimary,
                unfocusedTextColor = ForgeTheme.colors.textPrimary,
                focusedBorderColor = ForgeTheme.colors.primary,
                unfocusedBorderColor = ForgeTheme.colors.divider
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(horizontal = 2.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )

        // Reps Input Field
        OutlinedTextField(
            value = repsText,
            onValueChange = {
                repsText = it
                val weight = weightText.toDoubleOrNull() ?: 0.0
                val reps = it.toIntOrNull() ?: 0
                val rir = rirText.toIntOrNull()
                onUpdate(weight, reps, rir)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ForgeTheme.colors.textPrimary,
                unfocusedTextColor = ForgeTheme.colors.textPrimary,
                focusedBorderColor = ForgeTheme.colors.primary,
                unfocusedBorderColor = ForgeTheme.colors.divider
            ),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(horizontal = 2.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        )

        // RIR Input Field
        OutlinedTextField(
            value = rirText,
            onValueChange = {
                rirText = it
                val weight = weightText.toDoubleOrNull() ?: 0.0
                val reps = repsText.toIntOrNull() ?: 0
                val rir = it.toIntOrNull()
                onUpdate(weight, reps, rir)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ForgeTheme.colors.textSecondary,
                unfocusedTextColor = ForgeTheme.colors.textMuted,
                focusedBorderColor = ForgeTheme.colors.primary,
                unfocusedBorderColor = ForgeTheme.colors.divider
            ),
            modifier = Modifier
                .width(44.dp)
                .height(48.dp)
                .padding(horizontal = 2.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Checkmark Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (set.isCompleted) ForgeTheme.colors.success else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (set.isCompleted) ForgeTheme.colors.success else ForgeTheme.colors.textSecondary,
                    shape = CircleShape
                )
                .clickable { onToggleComplete() },
            contentAlignment = Alignment.Center
        ) {
            if (set.isCompleted) {
                Text(
                    text = "✓",
                    color = ForgeTheme.colors.background,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
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
    onCreateCustomExercise: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "ADD EXERCISE",
            color = ForgeTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearchQueryChange(it)
            },
            placeholder = { Text("Search 876+ movements, aliases...", color = ForgeTheme.colors.textSecondary, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ForgeTheme.colors.textPrimary,
                unfocusedTextColor = ForgeTheme.colors.textPrimary,
                focusedBorderColor = ForgeTheme.colors.primary,
                unfocusedBorderColor = ForgeTheme.colors.divider
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Tabs when not actively typing query
        if (query.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExerciseLibraryTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ForgeTheme.colors.primary else Color.Transparent)
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color(0xFF090A0D) else ForgeTheme.colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (searchResults.isEmpty() && query.isNotBlank()) {
            Button(
                onClick = { onCreateCustomExercise(query) },
                colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.secondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ CREATE \"$query\" AS CUSTOM EXERCISE", color = ForgeTheme.colors.background, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.id }) { exercise ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(cornerRadius = 10.dp)
                            .clickable { onSelectExercise(exercise) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = exercise.name,
                                        color = ForgeTheme.colors.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (exercise.isPopular) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "POPULAR",
                                            color = ForgeTheme.colors.accent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x22F59E0B))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${exercise.forgeMovementPattern.replace("_", " ")}${if (!exercise.sourceEquipment.isNullOrBlank()) " • " + exercise.sourceEquipment else ""}",
                                    color = ForgeTheme.colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "+ ADD",
                                color = ForgeTheme.colors.primary,
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

@Preview(name = "Active Workout - Empty")
@Composable
fun ActiveWorkoutEmptyPreview() {
    ForgeTheme {
        ActiveWorkoutContent(
            sessionName = "Empty Workout",
            elapsedSeconds = 45,
            exercisesWithSets = emptyList(),
            restTimerState = RestTimerState(),
            currentRestExerciseName = "",
            latestPrNotification = null,
            onNavigateBack = {},
            onFinishClick = {},
            onDiscardClick = {},
            onAddExerciseClick = {},
            onAddSetClick = {},
            onSetUpdate = { _, _, _, _ -> },
            onToggleSetCompleted = { _, _ -> },
            onAddRestSeconds = {},
            onSkipRest = {}
        )
    }
}
