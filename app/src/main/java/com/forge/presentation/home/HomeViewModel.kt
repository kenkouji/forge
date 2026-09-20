package com.forge.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.dao.DailyActivityDao
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.dao.TrainingScheduleDao
import com.forge.data.local.dao.TransformationDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.entity.ExercisePersonalRecordEntity
import com.forge.data.local.entity.TrainingScheduleEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.domain.engine.HealthConnectPipeline
import com.forge.domain.engine.HealthFreshnessState
import com.forge.domain.engine.TrainingStreakEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val userName: String = "",
    val fitnessGoal: String = "",
    val todaySchedule: TrainingScheduleEntity? = null,
    val nextTrainingSchedule: TrainingScheduleEntity? = null,
    val isTodayTrainingDay: Boolean = false,
    val activeSession: WorkoutSessionEntity? = null,
    val streakDays: Int = 0,
    val thisWeekSessionsCount: Int = 0,
    val totalVolumeTons: Double = 0.0,
    val healthActivity: HealthFreshnessState = HealthFreshnessState(),
    val totalWorkoutsCount: Int = 0,
    val latestPr: ExercisePersonalRecordEntity? = null,
    val latestPrExerciseName: String? = null,
    val checkInDueWeek: Int? = null
)

class HomeViewModel(
    private val userProfileDao: UserProfileDao,
    private val trainingScheduleDao: TrainingScheduleDao,
    private val workoutDao: WorkoutDao,
    private val dailyActivityDao: DailyActivityDao,
    private val healthPipeline: HealthConnectPipeline? = null,
    private val exerciseDao: ExerciseDao? = null,
    private val transformationDao: TransformationDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                userProfileDao.getUserProfile(),
                trainingScheduleDao.getSchedule(),
                workoutDao.getActiveSession(),
                workoutDao.getAllSessions()
            ) { profile: UserProfileEntity?, schedules: List<TrainingScheduleEntity>, activeSession: WorkoutSessionEntity?, allSessions: List<WorkoutSessionEntity> ->
                val completedSessions = allSessions.filter { it.status == "COMPLETED" }
                val todayDayOfWeek = LocalDate.now().dayOfWeek.value // 1 = Monday .. 7 = Sunday
                val todaySchedule = schedules.find { it.dayOfWeek == todayDayOfWeek }

                // Find next upcoming training day
                val nextTraining = (1..6).asSequence().map { offset ->
                    val nextDay = ((todayDayOfWeek - 1 + offset) % 7) + 1
                    schedules.find { it.dayOfWeek == nextDay && it.isTrainingDay }
                }.filterNotNull().firstOrNull()

                val streak = TrainingStreakEngine.calculateStreak(
                    today = LocalDate.now(),
                    schedules = schedules,
                    completedSessions = completedSessions
                )

                // Sessions completed this calendar week
                val mondayThisWeek = LocalDate.now().minusDays((todayDayOfWeek - 1).toLong())
                val thisWeekSessions = completedSessions.filter { session ->
                    val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    !sessionDate.isBefore(mondayThisWeek)
                }

                val totalVolKg = completedSessions.sumOf { it.totalVolumeKg }
                val totalVolTons = totalVolKg / 1000.0

                // Check transformation milestone due
                var dueWeek: Int? = null
                if (profile != null && profile.transformationStartDate > 0L) {
                    val daysElapsed = ((System.currentTimeMillis() - profile.transformationStartDate) / (1000L * 60 * 60 * 24)).toInt()
                    val calculatedWeek = (daysElapsed / 7) + 1
                    if (calculatedWeek > 1 && (daysElapsed % 7 == 0)) {
                        dueWeek = calculatedWeek
                    }
                }

                HomeUiState(
                    userName = profile?.name ?: "Athlete",
                    fitnessGoal = profile?.goal ?: "Hypertrophy",
                    todaySchedule = todaySchedule,
                    nextTrainingSchedule = nextTraining,
                    isTodayTrainingDay = todaySchedule?.isTrainingDay == true,
                    activeSession = activeSession,
                    streakDays = streak.currentStreakDays,
                    thisWeekSessionsCount = thisWeekSessions.size,
                    totalVolumeTons = totalVolTons,
                    totalWorkoutsCount = completedSessions.size,
                    latestPr = _uiState.value.latestPr,
                    latestPrExerciseName = _uiState.value.latestPrExerciseName,
                    checkInDueWeek = dueWeek
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        exerciseDao?.let { exDao ->
            viewModelScope.launch {
                exDao.getAllPersonalRecords().collect { prs ->
                    val latest = prs.maxByOrNull { it.achievedAt }
                    var exName: String? = null
                    if (latest != null) {
                        val ex = exDao.getExerciseByIdSync(latest.exerciseId)
                        exName = ex?.name
                    }
                    _uiState.value = _uiState.value.copy(
                        latestPr = latest,
                        latestPrExerciseName = exName
                    )
                }
            }
        }

        healthPipeline?.let { pipeline ->
            viewModelScope.launch {
                pipeline.observeTodayActivity().collect { freshness ->
                    _uiState.value = _uiState.value.copy(healthActivity = freshness)
                }
            }
        }
    }

    fun dismissCheckInPrompt() {
        _uiState.value = _uiState.value.copy(checkInDueWeek = null)
    }

    fun syncHealthData() {
        viewModelScope.launch {
            healthPipeline?.syncNow()
        }
    }
}
