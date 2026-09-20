package com.forge.presentation.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.dao.DailyActivityDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.entity.DailyActivityEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class NutritionUiState(
    val targetCalories: Int = 2400,
    val targetProteinGrams: Int = 160,
    val steps: Int = 0,
    val activeCalories: Int = 0,
    val isCaloriesMeasured: Boolean = false,
    val hasHealthConnectSync: Boolean = false,
    val latestWorkoutName: String? = null,
    val latestWorkoutMinutes: Long = 0
)

class NutritionViewModel(
    private val userProfileDao: UserProfileDao,
    private val dailyActivityDao: DailyActivityDao,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        viewModelScope.launch {
            combine(
                userProfileDao.getUserProfile(),
                dailyActivityDao.getActivityForDate(todayStr),
                workoutDao.getAllSessions()
            ) { profile: UserProfileEntity?, daily: DailyActivityEntity?, allSessions: List<WorkoutSessionEntity> ->
                val completed = allSessions.filter { it.status == "COMPLETED" }
                val latest = completed.firstOrNull()
                val minutes = latest?.let {
                    val end = it.endTime ?: it.startTime
                    (end - it.startTime) / (60 * 1000L)
                } ?: 0L

                NutritionUiState(
                    targetCalories = profile?.targetCalories ?: 2400,
                    targetProteinGrams = profile?.targetProteinG ?: 160,
                    steps = daily?.steps ?: 0,
                    activeCalories = daily?.activeCalories ?: 0,
                    isCaloriesMeasured = daily?.isCaloriesMeasured ?: false,
                    hasHealthConnectSync = daily?.hasHealthConnectSync ?: false,
                    latestWorkoutName = latest?.name,
                    latestWorkoutMinutes = minutes
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
