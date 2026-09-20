package com.forge.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.dao.TrainingScheduleDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.dao.WorkoutTemplateDao
import com.forge.data.local.entity.TemplateExerciseEntity
import com.forge.data.local.entity.TrainingScheduleEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.data.local.entity.WorkoutTemplateEntity
import com.forge.domain.engine.ParsedWorkout
import com.forge.domain.engine.TransformationVaultManager
import com.forge.domain.engine.WorkoutTextParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class OnboardingState(
    val currentStep: Int = 1,
    // Step 1: Identity
    val name: String = "",
    val photoUri: String? = null,
    // Step 2: Body Information
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,
    val age: Int = 26,
    val sex: String = "Male", // "Male" or "Female"
    // Step 3: Goal
    val goal: String = "Hypertrophy",
    // Step 4: Experience
    val experienceLevel: String = "Intermediate",
    // Step 5: Frequency & Training Days
    val weeklyFrequency: Int = 4,
    val selectedDays: Set<Int> = setOf(1, 2, 4, 5), // 1 = Monday .. 7 = Sunday
    // Step 6: Session Duration
    val sessionDurationMin: Int = 60,
    // Step 7: Equipment
    val selectedEquipment: Set<String> = setOf("Gym", "Barbell", "Dumbbells", "Cables"),
    // Step 8: Existing Program Import
    val hasExistingProgram: Boolean = false,
    val rawWorkoutImportText: String = "",
    val parsedImport: ParsedWorkout? = null,
    val splitPreference: String = "Push / Pull / Legs",
    // Step 9: Training Split
    val trainingTime: String = "17:00",
    // Step 10: Nutrition Targets
    val nutritionStrategy: String = "Maintain", // Cut, Maintain, Recomp, Lean Bulk
    val maintenanceCalories: Int = 2400,
    val targetCalories: Int = 2400,
    val targetProteinGrams: Int = 160,
    val targetCarbsGrams: Int = 250,
    val targetFatGrams: Int = 70,
    // Step 11: Transformation Vault Password
    val vaultPassword: String = "",
    val vaultPasswordConfirm: String = "",
    // Step 12: Permissions Setup
    val healthConnectGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
    val cameraGranted: Boolean = false,
    val isCompleting: Boolean = false
)

class InitializationViewModel(
    private val userProfileDao: UserProfileDao,
    private val trainingScheduleDao: TrainingScheduleDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val vaultManager: TransformationVaultManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    init {
        loadSavedProgress()
        recalculateNutrition()
    }

    private fun loadSavedProgress() {
        viewModelScope.launch {
            val profile = userProfileDao.getUserProfileSync()
            if (profile != null && !profile.isInitialized) {
                _uiState.update {
                    it.copy(
                        currentStep = profile.initializationStep.coerceIn(1, 12),
                        name = profile.name,
                        heightCm = profile.heightCm,
                        weightKg = profile.weightKg,
                        age = profile.age ?: 26,
                        sex = profile.sex ?: "Male",
                        goal = profile.goal.ifEmpty { "Hypertrophy" },
                        experienceLevel = profile.experience.ifEmpty { "Intermediate" },
                        weeklyFrequency = profile.daysPerWeek,
                        sessionDurationMin = profile.sessionDurationMin,
                        maintenanceCalories = profile.maintenanceCalories,
                        targetCalories = profile.targetCalories,
                        targetProteinGrams = profile.targetProteinG,
                        targetCarbsGrams = profile.targetCarbsG,
                        targetFatGrams = profile.targetFatG
                    )
                }
            }
        }
    }

    fun nextStep() {
        if (_uiState.value.currentStep < 12) {
            val newStep = _uiState.value.currentStep + 1
            _uiState.update { it.copy(currentStep = newStep) }
            saveStepProgress(newStep)
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            val newStep = _uiState.value.currentStep - 1
            _uiState.update { it.copy(currentStep = newStep) }
            saveStepProgress(newStep)
        }
    }

    private fun saveStepProgress(step: Int) {
        viewModelScope.launch {
            val current = _uiState.value
            val existing = userProfileDao.getUserProfileSync()
            val updated = UserProfileEntity(
                id = 1,
                name = current.name,
                photoUri = current.photoUri,
                goal = current.goal,
                experience = current.experienceLevel,
                daysPerWeek = current.weeklyFrequency,
                sessionDurationMin = current.sessionDurationMin,
                equipment = current.selectedEquipment.joinToString(","),
                heightCm = current.heightCm,
                weightKg = current.weightKg,
                age = current.age,
                sex = current.sex,
                maintenanceCalories = current.maintenanceCalories,
                nutritionGoal = current.nutritionStrategy,
                targetCalories = current.targetCalories,
                targetProteinG = current.targetProteinGrams,
                targetCarbsG = current.targetCarbsGrams,
                targetFatG = current.targetFatGrams,
                isInitialized = false,
                initializationStep = step,
                transformationStartDate = existing?.transformationStartDate ?: System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdate(updated)
        }
    }

    // Step 1: Identity
    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updatePhotoUri(uri: String?) = _uiState.update { it.copy(photoUri = uri) }

    // Step 2: Body Telemetry
    fun updateBodyStats(heightCm: Float, weightKg: Float, age: Int, sex: String) {
        _uiState.update {
            it.copy(heightCm = heightCm, weightKg = weightKg, age = age, sex = sex)
        }
        recalculateNutrition()
    }

    // Step 3: Goal
    fun updateGoal(goal: String) {
        _uiState.update { it.copy(goal = goal) }
        recalculateNutrition()
    }

    // Step 4: Experience
    fun updateExperience(level: String) = _uiState.update { it.copy(experienceLevel = level) }

    // Step 5: Frequency & Days
    fun updateFrequency(freq: Int) = _uiState.update { it.copy(weeklyFrequency = freq) }
    fun toggleDay(day: Int) {
        _uiState.update { state ->
            val set = state.selectedDays.toMutableSet()
            if (set.contains(day)) set.remove(day) else set.add(day)
            state.copy(selectedDays = set, weeklyFrequency = set.size.coerceAtLeast(1))
        }
    }

    // Step 6: Duration
    fun updateDuration(durationMin: Int) = _uiState.update { it.copy(sessionDurationMin = durationMin) }

    // Step 7: Equipment
    fun toggleEquipment(equipment: String) {
        _uiState.update { state ->
            val set = state.selectedEquipment.toMutableSet()
            if (set.contains(equipment)) set.remove(equipment) else set.add(equipment)
            state.copy(selectedEquipment = set)
        }
    }

    // Step 8: Program Import
    fun setHasExistingProgram(hasProgram: Boolean) = _uiState.update { it.copy(hasExistingProgram = hasProgram) }
    fun parseImportText(text: String) {
        val parsed = WorkoutTextParser.parse(text)
        _uiState.update { it.copy(rawWorkoutImportText = text, parsedImport = parsed) }
    }
    fun clearImport() = _uiState.update { it.copy(rawWorkoutImportText = "", parsedImport = null) }

    // Step 9: Training Split
    fun updateSplit(split: String) = _uiState.update { it.copy(splitPreference = split) }

    // Step 10: Nutrition
    fun updateNutritionStrategy(strategy: String) {
        _uiState.update { it.copy(nutritionStrategy = strategy) }
        recalculateNutrition()
    }

    fun updateManualMacros(calories: Int, protein: Int, carbs: Int, fat: Int) {
        _uiState.update {
            it.copy(targetCalories = calories, targetProteinGrams = protein, targetCarbsGrams = carbs, targetFatGrams = fat)
        }
    }

    private fun recalculateNutrition() {
        val state = _uiState.value
        // Mifflin-St Jeor formula
        val isMale = state.sex.equals("Male", ignoreCase = true)
        val s = if (isMale) 5 else -161
        val bmr = (10 * state.weightKg) + (6.25f * state.heightCm) - (5 * state.age) + s
        val tdee = (bmr * 1.4f).toInt().coerceAtLeast(1400)

        val targetCal = when (state.nutritionStrategy) {
            "Cut", "Fat Loss" -> tdee - 400
            "Lean Bulk", "Hypertrophy" -> tdee + 250
            "Recomp", "Recomposition" -> tdee - 150
            else -> tdee
        }

        val proteinG = (state.weightKg * 2.0f).toInt().coerceIn(100, 260)
        val fatG = (state.weightKg * 0.9f).toInt().coerceIn(45, 110)
        val carbCal = (targetCal - (proteinG * 4 + fatG * 9)).coerceAtLeast(200)
        val carbsG = carbCal / 4

        _uiState.update {
            it.copy(
                maintenanceCalories = tdee,
                targetCalories = targetCal,
                targetProteinGrams = proteinG,
                targetCarbsGrams = carbsG,
                targetFatGrams = fatG
            )
        }
    }

    // Step 11: Vault Password
    fun updateVaultPassword(password: String, confirm: String) =
        _uiState.update { it.copy(vaultPassword = password, vaultPasswordConfirm = confirm) }

    // Step 12: Permissions
    fun updatePermissions(healthConnect: Boolean, notifications: Boolean, camera: Boolean) {
        _uiState.update {
            it.copy(healthConnectGranted = healthConnect, notificationsGranted = notifications, cameraGranted = camera)
        }
    }

    fun finalizeInitialization(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }
            val state = _uiState.value

            // 1. Password hash & salt
            var passwordHash: String? = null
            var salt: String? = null
            if (state.vaultPassword.isNotBlank() && state.vaultPassword == state.vaultPasswordConfirm) {
                val pair = TransformationVaultManager.createPasswordHashAndSalt(state.vaultPassword)
                passwordHash = pair.first
                salt = pair.second
            }

            val now = System.currentTimeMillis()
            val userProfile = UserProfileEntity(
                id = 1,
                name = state.name.ifBlank { "Athlete" },
                photoUri = state.photoUri,
                goal = state.goal,
                experience = state.experienceLevel,
                daysPerWeek = state.weeklyFrequency,
                sessionDurationMin = state.sessionDurationMin,
                equipment = state.selectedEquipment.joinToString(","),
                heightCm = state.heightCm,
                weightKg = state.weightKg,
                age = state.age,
                sex = state.sex,
                maintenanceCalories = state.maintenanceCalories,
                nutritionGoal = state.nutritionStrategy,
                targetCalories = state.targetCalories,
                targetProteinG = state.targetProteinGrams,
                targetCarbsG = state.targetCarbsGrams,
                targetFatG = state.targetFatGrams,
                isInitialized = true,
                initializationStep = 12,
                transformationStartDate = now,
                photoPasswordHash = passwordHash,
                photoPasswordSalt = salt,
                updatedAt = now
            )
            userProfileDao.insertOrUpdate(userProfile)

            // 2. Generate training schedule and templates
            generateSchedulesAndTemplates(state)

            _uiState.update { it.copy(isCompleting = false) }
            onSuccess()
        }
    }

    private suspend fun generateSchedulesAndTemplates(state: OnboardingState) {
        val templates = mutableListOf<WorkoutTemplateEntity>()
        val schedules = mutableListOf<TrainingScheduleEntity>()

        val split = state.splitPreference

        when {
            split.contains("Push", ignoreCase = true) -> {
                val pushId = UUID.randomUUID().toString()
                val pullId = UUID.randomUUID().toString()
                val legsId = UUID.randomUUID().toString()

                templates.add(WorkoutTemplateEntity(pushId, "Push (Chest, Delts, Triceps)", "Push", 1))
                templates.add(WorkoutTemplateEntity(pullId, "Pull (Back, Biceps)", "Pull", 1))
                templates.add(WorkoutTemplateEntity(legsId, "Legs (Quads, Hamstrings, Calves)", "Legs", 1))

                val cycle = listOf(pushId to "Push", pullId to "Pull", legsId to "Legs")
                var cycleIdx = 0
                for (day in 1..7) {
                    if (state.selectedDays.contains(day)) {
                        val (tmplId, focus) = cycle[cycleIdx % cycle.size]
                        cycleIdx++
                        schedules.add(TrainingScheduleEntity(day, isTrainingDay = true, focus = focus, templateId = tmplId, targetDurationMin = state.sessionDurationMin))
                    } else {
                        schedules.add(TrainingScheduleEntity(day, isTrainingDay = false, focus = "Rest & Recovery", templateId = null, targetDurationMin = 0))
                    }
                }
            }
            split.contains("Upper", ignoreCase = true) -> {
                val upperId = UUID.randomUUID().toString()
                val lowerId = UUID.randomUUID().toString()

                templates.add(WorkoutTemplateEntity(upperId, "Upper Body Power", "Upper", 1))
                templates.add(WorkoutTemplateEntity(lowerId, "Lower Body Power", "Lower", 1))

                val cycle = listOf(upperId to "Upper Body", lowerId to "Lower Body")
                var cycleIdx = 0
                for (day in 1..7) {
                    if (state.selectedDays.contains(day)) {
                        val (tmplId, focus) = cycle[cycleIdx % cycle.size]
                        cycleIdx++
                        schedules.add(TrainingScheduleEntity(day, isTrainingDay = true, focus = focus, templateId = tmplId, targetDurationMin = state.sessionDurationMin))
                    } else {
                        schedules.add(TrainingScheduleEntity(day, isTrainingDay = false, focus = "Rest & Recovery", templateId = null, targetDurationMin = 0))
                    }
                }
            }
            else -> {
                val fullBodyId = UUID.randomUUID().toString()
                templates.add(WorkoutTemplateEntity(fullBodyId, "Full Body Compound", "Full Body", 1))

                for (day in 1..7) {
                    if (state.selectedDays.contains(day)) {
                        schedules.add(TrainingScheduleEntity(day, isTrainingDay = true, focus = "Full Body", templateId = fullBodyId, targetDurationMin = state.sessionDurationMin))
                    } else {
                        schedules.add(TrainingScheduleEntity(day, isTrainingDay = false, focus = "Rest & Recovery", templateId = null, targetDurationMin = 0))
                    }
                }
            }
        }

        trainingScheduleDao.insertOrReplace(schedules)
        templates.forEach { workoutTemplateDao.insertTemplate(it) }
    }
}
