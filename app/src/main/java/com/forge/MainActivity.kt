package com.forge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.data.repository.ExerciseRepositoryImpl
import com.forge.data.repository.WorkoutRepositoryImpl
import com.forge.presentation.exercise.ExerciseDetailViewModel
import com.forge.presentation.exercise.ExerciseLibraryViewModel
import com.forge.presentation.navigation.ForgeAppRoot
import com.forge.presentation.navigation.ForgeNavDestination
import com.forge.presentation.workout.ActiveWorkoutViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ForgeApp
        val workoutRepository = WorkoutRepositoryImpl(app.database.workoutDao())
        val exerciseRepository = ExerciseRepositoryImpl(app.database.exerciseDao())
        val voiceCoachEngine = com.forge.domain.engine.VoiceCoachEngine(this)
        val activeWorkoutViewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository, voiceCoachEngine)
        val exerciseLibraryViewModel = ExerciseLibraryViewModel(exerciseRepository)
        val exerciseDetailViewModel = ExerciseDetailViewModel(exerciseRepository)
        val progressViewModel = com.forge.presentation.progress.ProgressViewModel(workoutRepository, exerciseRepository)

        setContent {
            ForgeTheme {
                val navController = rememberNavController()
                val activeSession by workoutRepository.getActiveSession().collectAsState(initial = null)
                var showRecoveryDialog by remember { mutableStateOf(false) }
                var hasHandledDialog by remember { mutableStateOf(false) }

                // Cold launch check for uncompleted workout session
                if (activeSession != null && !hasHandledDialog) {
                    showRecoveryDialog = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ForgeTheme.colors.background)
                ) {
                    ForgeAppRoot(
                        activeWorkoutViewModel = activeWorkoutViewModel,
                        exerciseLibraryViewModel = exerciseLibraryViewModel,
                        exerciseDetailViewModel = exerciseDetailViewModel,
                        progressViewModel = progressViewModel,
                        navController = navController,
                        hasActiveWorkout = activeSession != null,
                        activeWorkoutTitle = activeSession?.let { "${it.name} (${it.totalVolumeKg} kg logged)" },
                        onResumeWorkoutClick = {
                            navController.navigate(ForgeNavDestination.ActiveWorkout.route)
                        }
                    )

                    if (showRecoveryDialog && activeSession != null) {
                        AlertDialog(
                            onDismissRequest = {
                                showRecoveryDialog = false
                                hasHandledDialog = true
                            },
                            title = {
                                Text(
                                    text = "Resume Active Workout?",
                                    color = ForgeTheme.colors.textPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = "An in-progress workout session (\"${activeSession?.name}\") was detected. Would you like to resume your session where you left off?",
                                    color = ForgeTheme.colors.textSecondary
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showRecoveryDialog = false
                                        hasHandledDialog = true
                                        navController.navigate(ForgeNavDestination.ActiveWorkout.route)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.primary)
                                ) {
                                    Text(
                                        text = "Resume",
                                        color = ForgeTheme.colors.background
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showRecoveryDialog = false
                                        hasHandledDialog = true
                                        activeWorkoutViewModel.discardWorkout {}
                                    }
                                ) {
                                    Text(
                                        text = "Discard",
                                        color = ForgeTheme.colors.error
                                    )
                                }
                            },
                            containerColor = ForgeTheme.colors.surfaceElevated
                        )
                    }
                }
            }
        }
    }
}
