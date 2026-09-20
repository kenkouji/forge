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
import com.forge.data.repository.HealthConnectRepositoryImpl
import com.forge.data.repository.WorkoutRepositoryImpl
import com.forge.domain.engine.AssistantService
import com.forge.domain.engine.HealthConnectPipeline
import com.forge.domain.engine.NotificationScheduler
import com.forge.domain.engine.TransformationVaultManager
import com.forge.domain.engine.VoiceCoachEngine
import com.forge.presentation.exercise.ExerciseDetailViewModel
import com.forge.presentation.exercise.ExerciseLibraryViewModel
import com.forge.presentation.home.HomeViewModel
import com.forge.presentation.journey.JourneyViewModel
import com.forge.presentation.navigation.ForgeAppRoot
import com.forge.presentation.navigation.ForgeNavDestination
import com.forge.presentation.nutrition.NutritionViewModel
import com.forge.presentation.onboarding.InitializationViewModel
import com.forge.presentation.profile.ProfileViewModel
import com.forge.presentation.progress.ProgressViewModel
import com.forge.presentation.workout.ActiveWorkoutViewModel
import com.forge.presentation.workout.WorkoutsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ForgeApp
        val db = app.database

        val userProfileDao = db.userProfileDao()
        val trainingScheduleDao = db.trainingScheduleDao()
        val workoutTemplateDao = db.workoutTemplateDao()
        val dailyActivityDao = db.dailyActivityDao()
        val transformationDao = db.transformationDao()
        val appSettingsDao = db.appSettingsDao()
        val workoutDao = db.workoutDao()
        val exerciseDao = db.exerciseDao()

        val workoutRepository = WorkoutRepositoryImpl(workoutDao)
        val exerciseRepository = ExerciseRepositoryImpl(exerciseDao)
        val healthConnectRepository = HealthConnectRepositoryImpl(this)

        val vaultManager = TransformationVaultManager(this)
        val notificationScheduler = NotificationScheduler(this)
        val healthConnectPipeline = HealthConnectPipeline(this, dailyActivityDao, healthConnectRepository)

        val assistantService = AssistantService(
            userProfileDao = userProfileDao,
            trainingScheduleDao = trainingScheduleDao,
            workoutTemplateDao = workoutTemplateDao,
            exerciseDao = exerciseDao,
            dailyActivityDao = dailyActivityDao,
            workoutDao = workoutDao,
            transformationDao = transformationDao
        )

        val voiceCoachEngine = VoiceCoachEngine(this)
        val activeWorkoutViewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository, voiceCoachEngine)
        val exerciseLibraryViewModel = ExerciseLibraryViewModel(exerciseRepository)
        val exerciseDetailViewModel = ExerciseDetailViewModel(exerciseRepository)
        val homeViewModel = HomeViewModel(
            userProfileDao = userProfileDao,
            trainingScheduleDao = trainingScheduleDao,
            workoutDao = workoutDao,
            dailyActivityDao = dailyActivityDao,
            healthPipeline = healthConnectPipeline,
            exerciseDao = exerciseDao,
            transformationDao = transformationDao
        )
        val workoutsViewModel = WorkoutsViewModel(trainingScheduleDao, workoutTemplateDao, workoutDao, exerciseRepository)
        val progressViewModel = ProgressViewModel(workoutRepository, exerciseRepository)
        val journeyViewModel = JourneyViewModel(transformationDao, userProfileDao, vaultManager)
        val profileViewModel = ProfileViewModel(
            userProfileDao = userProfileDao,
            appSettingsDao = appSettingsDao,
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            trainingScheduleDao = trainingScheduleDao,
            workoutTemplateDao = workoutTemplateDao,
            dailyActivityDao = dailyActivityDao,
            transformationDao = transformationDao
        )
        val nutritionViewModel = NutritionViewModel(userProfileDao, dailyActivityDao, workoutDao)
        val initializationViewModel = InitializationViewModel(userProfileDao, trainingScheduleDao, workoutTemplateDao, vaultManager)

        setContent {
            ForgeTheme {
                val navController = rememberNavController()
                val activeSession by workoutRepository.getActiveSession().collectAsState(initial = null)
                val userProfile by userProfileDao.getUserProfile().collectAsState(initial = null)
                var showRecoveryDialog by remember { mutableStateOf(false) }
                var hasHandledDialog by remember { mutableStateOf(false) }

                // Cold launch check for uncompleted workout session
                if (activeSession != null && !hasHandledDialog) {
                    showRecoveryDialog = true
                }

                val isInitialized = userProfile?.isInitialized == true

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ForgeTheme.colors.background)
                ) {
                    ForgeAppRoot(
                        homeViewModel = homeViewModel,
                        workoutsViewModel = workoutsViewModel,
                        activeWorkoutViewModel = activeWorkoutViewModel,
                        exerciseLibraryViewModel = exerciseLibraryViewModel,
                        exerciseDetailViewModel = exerciseDetailViewModel,
                        progressViewModel = progressViewModel,
                        journeyViewModel = journeyViewModel,
                        profileViewModel = profileViewModel,
                        nutritionViewModel = nutritionViewModel,
                        initializationViewModel = initializationViewModel,
                        assistantService = assistantService,
                        isInitialized = isInitialized,
                        navController = navController,
                        hasActiveWorkout = activeSession != null,
                        activeWorkoutTitle = activeSession?.let { "${it.name} (${it.totalVolumeKg.toInt()} kg logged)" },
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
                                    Text("Resume", color = ForgeTheme.colors.onPrimary)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showRecoveryDialog = false
                                        hasHandledDialog = true
                                    }
                                ) {
                                    Text("Dismiss", color = ForgeTheme.colors.textSecondary)
                                }
                            },
                            containerColor = ForgeTheme.colors.surface
                        )
                    }
                }
            }
        }
    }
}
