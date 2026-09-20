package com.forge.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.dao.AppSettingsDao
import com.forge.data.local.dao.DailyActivityDao
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.dao.TrainingScheduleDao
import com.forge.data.local.dao.TransformationDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.dao.WorkoutTemplateDao
import com.forge.data.local.entity.AppSettingsEntity
import com.forge.data.local.entity.ExercisePersonalRecordEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.domain.engine.TransformationVaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ProfileUiState(
    val userProfile: UserProfileEntity? = null,
    val appSettings: AppSettingsEntity = AppSettingsEntity(),
    val totalWorkoutsCompleted: Int = 0,
    val personalRecords: List<ExercisePersonalRecordEntity> = emptyList(),
    val dataExportText: String? = null,
    val actionMessage: String? = null
)

class ProfileViewModel(
    private val userProfileDao: UserProfileDao,
    private val appSettingsDao: AppSettingsDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val trainingScheduleDao: TrainingScheduleDao? = null,
    private val workoutTemplateDao: WorkoutTemplateDao? = null,
    private val dailyActivityDao: DailyActivityDao? = null,
    private val transformationDao: TransformationDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                userProfileDao.getUserProfile(),
                appSettingsDao.getSettings(),
                workoutDao.getAllSessions(),
                exerciseDao.getAllPersonalRecords()
            ) { profile: UserProfileEntity?, settings: AppSettingsEntity?, allSessions: List<WorkoutSessionEntity>, prs: List<ExercisePersonalRecordEntity> ->
                val completedSessions = allSessions.filter { it.status == "COMPLETED" }
                ProfileUiState(
                    userProfile = profile,
                    appSettings = settings ?: AppSettingsEntity(),
                    totalWorkoutsCompleted = completedSessions.size,
                    personalRecords = prs,
                    dataExportText = _uiState.value.dataExportText,
                    actionMessage = _uiState.value.actionMessage
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateUserProfile(
        name: String,
        heightCm: Float,
        weightKg: Float,
        age: Int?,
        sex: String?,
        goal: String,
        experience: String,
        daysPerWeek: Int,
        durationMin: Int,
        equipment: String
    ) {
        viewModelScope.launch {
            val current = _uiState.value.userProfile ?: UserProfileEntity()

            // Recalculate BMR / TDEE
            val isMale = sex.equals("Male", ignoreCase = true)
            val s = if (isMale) 5 else -161
            val userAge = age ?: 26
            val bmr = (10 * weightKg) + (6.25f * heightCm) - (5 * userAge) + s
            val tdee = (bmr * 1.4f).toInt().coerceAtLeast(1400)

            val targetCal = when {
                goal.contains("Cut", ignoreCase = true) || goal.contains("Fat", ignoreCase = true) -> tdee - 400
                goal.contains("Bulk", ignoreCase = true) || goal.contains("Hypertrophy", ignoreCase = true) -> tdee + 250
                goal.contains("Recomp", ignoreCase = true) -> tdee - 150
                else -> tdee
            }

            val proteinG = (weightKg * 2.0f).toInt().coerceIn(100, 260)
            val fatG = (weightKg * 0.9f).toInt().coerceIn(45, 110)
            val carbCal = (targetCal - (proteinG * 4 + fatG * 9)).coerceAtLeast(200)
            val carbsG = carbCal / 4

            val updated = current.copy(
                name = name,
                heightCm = heightCm,
                weightKg = weightKg,
                age = age,
                sex = sex,
                goal = goal,
                experience = experience,
                daysPerWeek = daysPerWeek,
                sessionDurationMin = durationMin,
                equipment = equipment,
                maintenanceCalories = tdee,
                targetCalories = targetCal,
                targetProteinG = proteinG,
                targetCarbsG = carbsG,
                targetFatG = fatG,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdate(updated)
            _uiState.value = _uiState.value.copy(actionMessage = "Profile updated successfully.")
        }
    }

    // Appearance & Settings Toggles
    fun toggleReduceMotion(enabled: Boolean) = updateSettings { it.copy(reduceMotion = enabled) }
    fun toggleParticles(enabled: Boolean) = updateSettings { it.copy(particlesEnabled = enabled) }
    fun toggleHaptics(enabled: Boolean) = updateSettings { it.copy(hapticsEnabled = enabled) }
    fun toggleAutoLock(enabled: Boolean) = updateSettings { it.copy(autoLockVaultOnBackground = enabled) }
    fun toggleHealthConnect(enabled: Boolean) = updateSettings { it.copy(healthConnectEnabled = enabled) }

    // Notification Category Toggles
    fun toggleWorkoutReminders(enabled: Boolean) = updateSettings { it.copy(notifWorkoutReminders = enabled) }
    fun togglePreWorkoutAlerts(enabled: Boolean) = updateSettings { it.copy(notifPreWorkoutAlerts = enabled) }
    fun togglePostWorkoutCongrats(enabled: Boolean) = updateSettings { it.copy(notifPostWorkoutCongrats = enabled) }
    fun toggleStreakReminders(enabled: Boolean) = updateSettings { it.copy(notifStreakReminders = enabled) }
    fun toggleMotivation(enabled: Boolean) = updateSettings { it.copy(notifMotivation = enabled) }
    fun toggleNutritionReminders(enabled: Boolean) = updateSettings { it.copy(notifNutritionReminders = enabled) }
    fun toggleHydrationReminders(enabled: Boolean) = updateSettings { it.copy(notifHydrationReminders = enabled) }

    private fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            val current = _uiState.value.appSettings
            appSettingsDao.insertOrUpdate(transform(current))
        }
    }

    // Change Transformation Vault Password
    fun changeVaultPassword(oldPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val profile = _uiState.value.userProfile
            if (profile == null) {
                onResult(false, "Profile not found.")
                return@launch
            }

            // Verify old password if one was set
            if (profile.photoPasswordHash != null && profile.photoPasswordSalt != null) {
                val isOldValid = TransformationVaultManager.verifyPassword(
                    oldPass,
                    profile.photoPasswordHash,
                    profile.photoPasswordSalt
                )
                if (!isOldValid) {
                    onResult(false, "Incorrect current password.")
                    return@launch
                }
            }

            // Hash new password
            val (hash, salt) = TransformationVaultManager.createPasswordHashAndSalt(newPass)
            val updated = profile.copy(
                photoPasswordHash = hash,
                photoPasswordSalt = salt,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdate(updated)
            onResult(true, "Vault password updated successfully.")
        }
    }

    // Export Data to clean JSON
    fun exportUserData(): String {
        val profile = _uiState.value.userProfile
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val pObj = JSONObject()
        pObj.put("name", profile?.name.orEmpty())
        pObj.put("goal", profile?.goal.orEmpty())
        pObj.put("experience", profile?.experience.orEmpty())
        pObj.put("heightCm", profile?.heightCm ?: 175f)
        pObj.put("weightKg", profile?.weightKg ?: 75f)
        pObj.put("daysPerWeek", profile?.daysPerWeek ?: 4)
        pObj.put("targetCalories", profile?.targetCalories ?: 2400)
        pObj.put("targetProteinG", profile?.targetProteinG ?: 160)
        root.put("profile", pObj)

        val prArray = org.json.JSONArray()
        _uiState.value.personalRecords.forEach { pr ->
            val o = JSONObject()
            o.put("exerciseId", pr.exerciseId)
            o.put("maxWeightKg", pr.maxWeightKg)
            o.put("reps", pr.maxRepsAtMaxWeight)
            prArray.put(o)
        }
        root.put("personalRecords", prArray)

        val jsonStr = root.toString(2)
        _uiState.value = _uiState.value.copy(dataExportText = jsonStr)
        return jsonStr
    }

    fun clearExportText() {
        _uiState.value = _uiState.value.copy(dataExportText = null)
    }

    // Destructive Full Data Reset (Double Confirmation)
    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Delete user profile and set to uninitialized
            userProfileDao.deleteAll()
            val resetProfile = UserProfileEntity(
                id = 1,
                name = "",
                isInitialized = false,
                initializationStep = 1,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdate(resetProfile)

            // Reset settings
            appSettingsDao.insertOrUpdate(AppSettingsEntity(id = 1))

            _uiState.value = _uiState.value.copy(actionMessage = "All user data has been reset.")
            onComplete()
        }
    }
}
