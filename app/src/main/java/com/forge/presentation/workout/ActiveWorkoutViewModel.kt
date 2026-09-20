package com.forge.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.ExercisePersonalRecordEntity
import com.forge.data.local.entity.ExerciseProgressionRecordEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity
import com.forge.domain.engine.ProgressionEngine
import com.forge.domain.engine.RestTimerEngine
import com.forge.domain.engine.RestTimerState
import com.forge.domain.engine.VoiceCoachEngine
import com.forge.domain.repository.ExerciseRepository
import com.forge.domain.repository.WorkoutRepository
import com.forge.presentation.exercise.ExerciseLibraryTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class ExerciseWithSets(
    val exercise: ExerciseEntity,
    val sets: List<WorkoutSetEntity>,
    val progressionRecord: ExerciseProgressionRecordEntity? = null,
    val personalRecord: ExercisePersonalRecordEntity? = null
)

data class ActiveWorkoutUiState(
    val session: WorkoutSessionEntity? = null,
    val exercisesWithSets: List<ExerciseWithSets> = emptyList(),
    val elapsedSeconds: Long = 0,
    val showAddExerciseSheet: Boolean = false,
    val addExerciseTab: ExerciseLibraryTab = ExerciseLibraryTab.POPULAR,
    val searchResults: List<ExerciseEntity> = emptyList(),
    val restTimerState: RestTimerState = RestTimerState(),
    val currentRestExerciseName: String = "",
    val latestPrNotification: String? = null,
    val isFinished: Boolean = false
)

class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val voiceCoachEngine: VoiceCoachEngine? = null,
    private val restTimerEngine: RestTimerEngine = RestTimerEngine(
        onTickSecond = { remaining -> voiceCoachEngine?.onCountdownTick(remaining) },
        onTimerComplete = { voiceCoachEngine?.onRestComplete() }
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val searchQueryFlow = MutableStateFlow("")
    private val addExerciseTabFlow = MutableStateFlow(ExerciseLibraryTab.POPULAR)

    init {
        observeActiveWorkout()
        observeExerciseSearch()
        observeRestTimer()
    }

    private fun observeRestTimer() {
        viewModelScope.launch {
            restTimerEngine.timerState.collect { timerState ->
                _uiState.update { it.copy(restTimerState = timerState) }
            }
        }
    }

    private fun observeActiveWorkout() {
        viewModelScope.launch {
            workoutRepository.getActiveSession().collect { session ->
                if (session == null) {
                    timerJob?.cancel()
                    _uiState.update { it.copy(session = null, exercisesWithSets = emptyList(), isFinished = true) }
                } else {
                    _uiState.update { it.copy(session = session, isFinished = false) }
                    startElapsedTimer(session.startTime)
                    loadSetsForSession(session.id)
                }
            }
        }
    }

    private fun loadSetsForSession(sessionId: String) {
        viewModelScope.launch {
            combine(
                workoutRepository.getSetsForSession(sessionId),
                exerciseRepository.getAllExercises()
            ) { sets, allExercises ->
                val exerciseMap = allExercises.associateBy { it.id }
                val groupedSets = sets.groupBy { it.exerciseId }

                groupedSets.mapNotNull { (exerciseId, exerciseSets) ->
                    val exercise = exerciseMap[exerciseId] ?: ExerciseEntity(
                        id = exerciseId,
                        name = exerciseId.replace("_", " ").replaceFirstChar { it.uppercase() },
                        canonicalName = exerciseId,
                        movementPattern = "UNKNOWN",
                        forgeMovementPattern = "UNKNOWN",
                        mechanic = "",
                        forceType = "",
                        experienceLevel = "",
                        instructions = "",
                        formCues = "",
                        commonMistakes = ""
                    )
                    val prog = exerciseRepository.getLatestProgressionDirect(exerciseId)
                    val pr = exerciseRepository.getPersonalRecordDirect(exerciseId)
                    ExerciseWithSets(
                        exercise = exercise,
                        sets = exerciseSets,
                        progressionRecord = prog,
                        personalRecord = pr
                    )
                }
            }.collect { list ->
                _uiState.update { it.copy(exercisesWithSets = list) }
            }
        }
    }

    private fun observeExerciseSearch() {
        viewModelScope.launch {
            combine(searchQueryFlow, addExerciseTabFlow) { query, tab ->
                query to tab
            }.flatMapLatest { (query, tab) ->
                if (query.isNotBlank()) {
                    exerciseRepository.searchExercises(query)
                } else {
                    when (tab) {
                        ExerciseLibraryTab.POPULAR -> exerciseRepository.getPopularExercises()
                        ExerciseLibraryTab.RECENT -> exerciseRepository.getRecentlyUsedExercises()
                        ExerciseLibraryTab.FAVORITES -> exerciseRepository.getFavoriteExercises()
                        ExerciseLibraryTab.BROWSE_ALL -> exerciseRepository.getAllExercises()
                    }
                }
            }.collect { results ->
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun setAddExerciseTab(tab: ExerciseLibraryTab) {
        addExerciseTabFlow.value = tab
        _uiState.update { it.copy(addExerciseTab = tab) }
    }

    private fun startElapsedTimer(startTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = maxOf(0L, (now - startTime) / 1000)
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
                delay(1000)
            }
        }
    }

    fun startEmptyWorkout(onStarted: (String) -> Unit = {}) {
        viewModelScope.launch {
            val existing = workoutRepository.getActiveSessionDirect()
            if (existing != null) {
                _uiState.update { it.copy(session = existing, isFinished = false) }
                startElapsedTimer(existing.startTime)
                onStarted(existing.id)
                return@launch
            }

            val newSessionId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val session = WorkoutSessionEntity(
                id = newSessionId,
                name = "Empty Workout",
                startTime = now,
                status = "IN_PROGRESS",
                lastUpdatedAt = now
            )
            workoutRepository.startWorkout(session)
            _uiState.update { it.copy(session = session, isFinished = false) }
            startElapsedTimer(now)
            voiceCoachEngine?.onWorkoutStart()
            onStarted(newSessionId)
        }
    }

    fun setAddExerciseSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddExerciseSheet = visible) }
        if (visible) {
            searchQueryFlow.value = ""
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun addExerciseToWorkout(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val currentSession = _uiState.value.session ?: workoutRepository.getActiveSessionDirect() ?: return@launch
            val existingSets = workoutRepository.getSetsForSessionDirect(currentSession.id)
            val currentExerciseSets = existingSets.filter { it.exerciseId == exercise.id }
            val nextSetOrder = existingSets.size

            val initialSet = WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                sessionId = currentSession.id,
                exerciseId = exercise.id,
                setOrder = nextSetOrder,
                setType = "NORMAL",
                weightKg = 0.0,
                reps = 0,
                isCompleted = false
            )

            workoutRepository.logSetTransaction(
                set = initialSet,
                activeExerciseId = exercise.id,
                activeSetIndex = currentExerciseSets.size,
                volumeDeltaKg = 0.0
            )

            voiceCoachEngine?.onNextExercise(exercise.name)
            setAddExerciseSheetVisible(false)
        }
    }

    fun createAndAddCustomExercise(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val currentSession = _uiState.value.session ?: workoutRepository.getActiveSessionDirect() ?: return@launch
            val id = "custom_" + name.trim().lowercase().replace("\\s+".toRegex(), "_")
            val customExercise = ExerciseEntity(
                id = id,
                name = name.trim(),
                canonicalName = name.trim(),
                movementPattern = "ISOLATION",
                mechanic = "ISOLATION",
                forceType = "PUSH",
                experienceLevel = "BEGINNER",
                instructions = "Custom user exercise",
                formCues = "",
                commonMistakes = "",
                isCustom = true
            )
            exerciseRepository.insertExercises(listOf(customExercise))
            addExerciseToWorkout(customExercise)
        }
    }

    fun addSet(exerciseId: String) {
        viewModelScope.launch {
            val currentSession = _uiState.value.session ?: workoutRepository.getActiveSessionDirect() ?: return@launch
            val existingSets = workoutRepository.getSetsForSessionDirect(currentSession.id)
            val exerciseSets = existingSets.filter { it.exerciseId == exerciseId }
            val lastSet = exerciseSets.lastOrNull()

            val newSet = WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                sessionId = currentSession.id,
                exerciseId = exerciseId,
                setOrder = existingSets.size,
                setType = "NORMAL",
                weightKg = lastSet?.weightKg ?: 0.0,
                reps = lastSet?.reps ?: 0,
                isCompleted = false
            )

            workoutRepository.logSetTransaction(
                set = newSet,
                activeExerciseId = exerciseId,
                activeSetIndex = exerciseSets.size,
                volumeDeltaKg = 0.0
            )
        }
    }

    fun updateSetValues(set: WorkoutSetEntity, weightKg: Double, reps: Int, rpe: Double?, rir: Int?) {
        viewModelScope.launch {
            val updated = set.copy(
                weightKg = weightKg,
                reps = reps,
                rpe = rpe,
                rir = rir
            )
            workoutRepository.logSetTransaction(
                set = updated,
                activeExerciseId = set.exerciseId,
                activeSetIndex = set.setOrder,
                volumeDeltaKg = 0.0
            )
        }
    }

    fun toggleSetCompleted(set: WorkoutSetEntity, exerciseName: String = "") {
        viewModelScope.launch {
            val currentSession = _uiState.value.session ?: workoutRepository.getActiveSessionDirect() ?: return@launch
            val newCompleted = !set.isCompleted
            val completedTime = if (newCompleted) System.currentTimeMillis() else null
            val updated = set.copy(
                isCompleted = newCompleted,
                completedAt = completedTime
            )
            val volumeDelta = if (newCompleted) (updated.weightKg * updated.reps) else -(set.weightKg * set.reps)

            workoutRepository.logSetTransaction(
                set = updated,
                activeExerciseId = set.exerciseId,
                activeSetIndex = set.setOrder + 1,
                volumeDeltaKg = volumeDelta
            )

            if (newCompleted && updated.weightKg > 0 && updated.reps > 0) {
                // Check if set broke a PR
                val existingPr = exerciseRepository.getPersonalRecordDirect(set.exerciseId)
                val prDetected = ProgressionEngine.evaluatePersonalRecord(set.exerciseId, updated, existingPr)
                if (prDetected != null) {
                    _uiState.update { it.copy(latestPrNotification = prDetected.summary) }
                }

                // Trigger Rest Timer and Voice Coach
                val restDuration = 90
                _uiState.update { it.copy(currentRestExerciseName = exerciseName) }
                restTimerEngine.startRest(restDuration)
                voiceCoachEngine?.onSetComplete(restDuration)
            }
        }
    }

    fun addRestSeconds(seconds: Int) {
        restTimerEngine.addSeconds(seconds)
    }

    fun skipRestTimer() {
        restTimerEngine.cancelRest()
    }

    fun finishWorkout(onFinished: () -> Unit) {
        viewModelScope.launch {
            val currentSession = _uiState.value.session ?: workoutRepository.getActiveSessionDirect()
            if (currentSession == null) {
                onFinished()
                return@launch
            }
            timerJob?.cancel()
            restTimerEngine.cancelRest()

            val now = System.currentTimeMillis()
            val durationSeconds = maxOf(0L, (now - currentSession.startTime) / 1000)
            workoutRepository.completeWorkout(currentSession.id, now, durationSeconds)

            // Save PRs and Progressive Overload Recommendations
            val exercisesWithSets = _uiState.value.exercisesWithSets
            for (item in exercisesWithSets) {
                val completedSets = item.sets.filter { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
                if (completedSets.isEmpty()) continue

                val existingPr = exerciseRepository.getPersonalRecordDirect(item.exercise.id)
                val bestSet = completedSets.maxByOrNull { ProgressionEngine.calculateEstimated1Rm(it.weightKg, it.reps) }
                if (bestSet != null) {
                    val prEvaluation = ProgressionEngine.evaluatePersonalRecord(item.exercise.id, bestSet, existingPr)
                    if (prEvaluation != null) {
                        val newPr = ExercisePersonalRecordEntity(
                            exerciseId = item.exercise.id,
                            maxWeightKg = maxOf(existingPr?.maxWeightKg ?: 0.0, completedSets.maxOf { it.weightKg }),
                            maxRepsAtMaxWeight = bestSet.reps,
                            estimated1RmKg = maxOf(existingPr?.estimated1RmKg ?: 0.0, prEvaluation.estimated1RmKg),
                            bestSetVolumeKg = maxOf(existingPr?.bestSetVolumeKg ?: 0.0, completedSets.maxOf { it.weightKg * it.reps }),
                            bestSessionVolumeKg = maxOf(existingPr?.bestSessionVolumeKg ?: 0.0, completedSets.sumOf { it.weightKg * it.reps }),
                            achievedAt = now
                        )
                        exerciseRepository.insertPersonalRecord(newPr)
                    }
                }

                // Deterministic Double Progression
                val recommendation = ProgressionEngine.computeRecommendation(
                    exerciseId = item.exercise.id,
                    targetRepRange = 8..12,
                    recentSessionsSets = listOf(completedSets),
                    equipmentId = item.exercise.sourceEquipment
                )

                val progressionRecord = ExerciseProgressionRecordEntity(
                    exerciseId = item.exercise.id,
                    sessionId = currentSession.id,
                    recommendedWeightKg = recommendation.recommendedWeightKg,
                    recommendedRepMin = recommendation.recommendedRepRange.first,
                    recommendedRepMax = recommendation.recommendedRepRange.last,
                    rationale = recommendation.rationale,
                    isDeload = recommendation.isDeloadRecommended,
                    createdAt = now
                )
                exerciseRepository.insertProgressionRecord(progressionRecord)
            }

            voiceCoachEngine?.onWorkoutComplete()

            _uiState.update { it.copy(session = null, isFinished = true, exercisesWithSets = emptyList(), latestPrNotification = null) }
            onFinished()
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            val currentSession = _uiState.value.session ?: workoutRepository.getActiveSessionDirect()
            if (currentSession == null) {
                onDiscarded()
                return@launch
            }
            timerJob?.cancel()
            restTimerEngine.cancelRest()
            workoutRepository.discardWorkout(currentSession.id)
            _uiState.update { it.copy(session = null, isFinished = true, exercisesWithSets = emptyList(), latestPrNotification = null) }
            onDiscarded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        restTimerEngine.cancelRest()
        voiceCoachEngine?.release()
    }
}
