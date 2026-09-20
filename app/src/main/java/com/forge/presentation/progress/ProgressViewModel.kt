package com.forge.presentation.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.entity.ExercisePersonalRecordEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.domain.repository.ExerciseRepository
import com.forge.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class PrWithExercise(
    val pr: ExercisePersonalRecordEntity,
    val exerciseName: String
)

data class ProgressUiState(
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalTrainingMinutes: Long = 0,
    val completedSessions: List<WorkoutSessionEntity> = emptyList(),
    val personalRecords: List<PrWithExercise> = emptyList(),
    val isLoading: Boolean = false
)

class ProgressViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState(isLoading = true))
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgressData()
    }

    private fun loadProgressData() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAllSessions(),
                exerciseRepository.getAllPersonalRecords(),
                exerciseRepository.getAllExercises()
            ) { sessions, prs, exercises ->
                val completed = sessions.filter { it.status == "COMPLETED" }
                val exerciseMap = exercises.associateBy { it.id }

                val prsWithNames = prs.map { pr ->
                    val name = exerciseMap[pr.exerciseId]?.name
                        ?: pr.exerciseId.replace("_", " ").replaceFirstChar { it.uppercase() }
                    PrWithExercise(pr = pr, exerciseName = name)
                }

                val totalVol = completed.sumOf { it.totalVolumeKg }
                val totalMins = completed.sumOf { it.durationSeconds } / 60

                ProgressUiState(
                    totalWorkouts = completed.size,
                    totalVolumeKg = totalVol,
                    totalTrainingMinutes = totalMins,
                    completedSessions = completed,
                    personalRecords = prsWithNames,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun loadSessionSets(sessionId: String, onLoaded: (List<com.forge.data.local.entity.WorkoutSetEntity>) -> Unit) {
        viewModelScope.launch {
            val sets = workoutRepository.getSetsForSessionDirect(sessionId)
            onLoaded(sets)
        }
    }

    suspend fun getExerciseName(exerciseId: String): String {
        val ex = exerciseRepository.getExerciseById(exerciseId).firstOrNull()
        return ex?.name ?: exerciseId.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

