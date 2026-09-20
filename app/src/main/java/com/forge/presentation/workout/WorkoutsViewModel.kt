package com.forge.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.dao.TrainingScheduleDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.dao.WorkoutTemplateDao
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.TemplateExerciseEntity
import com.forge.data.local.entity.TrainingScheduleEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutTemplateEntity
import com.forge.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class ScheduledDayUiModel(
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val dayName: String,
    val dateStr: String,
    val isToday: Boolean,
    val isTrainingDay: Boolean,
    val focus: String,
    val templateId: String?,
    val isCompleted: Boolean = false
)

data class WorkoutsUiState(
    val weekSchedule: List<ScheduledDayUiModel> = emptyList(),
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val completedSessions: List<WorkoutSessionEntity> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList()
)

class WorkoutsViewModel(
    private val trainingScheduleDao: TrainingScheduleDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val workoutDao: WorkoutDao,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutsUiState())
    val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _uiState.update { it.copy(availableExercises = exercises) }
            }
        }

        viewModelScope.launch {
            combine(
                trainingScheduleDao.getSchedule(),
                workoutTemplateDao.getTemplates(),
                workoutDao.getAllSessions()
            ) { schedules: List<TrainingScheduleEntity>, templates: List<WorkoutTemplateEntity>, allSessions: List<WorkoutSessionEntity> ->
                val completedSessions = allSessions.filter { it.status == "COMPLETED" }
                val today = LocalDate.now()
                val currentDayOfWeek = today.dayOfWeek.value // 1..7
                val mondayDate = today.minusDays((currentDayOfWeek - 1).toLong())

                val dayModels = (1..7).map { dayNum ->
                    val date = mondayDate.plusDays((dayNum - 1).toLong())
                    val sched = schedules.find { it.dayOfWeek == dayNum }
                    val dayName = DayOfWeek.of(dayNum).name.take(3).lowercase().replaceFirstChar { it.uppercase() }

                    val completedOnThisDay = completedSessions.any { session ->
                        val sDate = java.time.Instant.ofEpochMilli(session.startTime)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        sDate == date
                    }

                    ScheduledDayUiModel(
                        dayOfWeek = dayNum,
                        dayName = dayName,
                        dateStr = "${date.month.name.take(3)} ${date.dayOfMonth}",
                        isToday = dayNum == currentDayOfWeek,
                        isTrainingDay = sched?.isTrainingDay == true,
                        focus = sched?.focus ?: if (sched?.isTrainingDay == false) "Rest & Recovery" else "Training",
                        templateId = sched?.templateId,
                        isCompleted = completedOnThisDay
                    )
                }

                WorkoutsUiState(
                    weekSchedule = dayModels,
                    templates = templates,
                    completedSessions = completedSessions,
                    availableExercises = _uiState.value.availableExercises
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(availableExercises = current.availableExercises)
                }
            }
        }
    }

    fun loadTemplateExercisesForEditing(templateId: String, onLoaded: (List<EditableExerciseItem>) -> Unit) {
        viewModelScope.launch {
            val entities = workoutTemplateDao.getTemplateExercisesSync(templateId)
            val items = entities.map { entity ->
                val ex = _uiState.value.availableExercises.find { it.id == entity.exerciseId }
                val repStr = if (entity.targetRepsMin != entity.targetRepsMax) {
                    "${entity.targetRepsMin}-${entity.targetRepsMax}"
                } else {
                    "${entity.targetRepsMax}"
                }
                EditableExerciseItem(
                    exerciseId = entity.exerciseId,
                    exerciseName = ex?.name ?: "Exercise",
                    targetSets = entity.targetSets,
                    targetReps = repStr,
                    targetRepsMin = entity.targetRepsMin,
                    targetRepsMax = entity.targetRepsMax,
                    targetWeightKg = entity.targetWeightKg,
                    targetRir = entity.targetRir,
                    restSeconds = entity.restSeconds,
                    isWarmup = entity.isWarmup,
                    isDropSet = entity.isDropSet,
                    notes = entity.notes
                )
            }
            onLoaded(items)
        }
    }

    fun saveTemplateChanges(
        templateId: String,
        newTitle: String,
        exercises: List<EditableExerciseItem>
    ) {
        viewModelScope.launch {
            val existing = workoutTemplateDao.getTemplateByIdSync(templateId)
            val newVersion = (existing?.version ?: 1) + 1

            val updatedTemplate = existing?.copy(
                name = newTitle,
                version = newVersion
            ) ?: WorkoutTemplateEntity(
                id = templateId,
                name = newTitle,
                focus = newTitle,
                version = newVersion
            )

            val entities = exercises.mapIndexed { order, ex ->
                TemplateExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    templateId = templateId,
                    exerciseId = ex.exerciseId,
                    orderIndex = order,
                    targetSets = ex.targetSets,
                    targetRepsMin = ex.targetRepsMin,
                    targetRepsMax = ex.targetRepsMax,
                    targetRir = ex.targetRir,
                    targetWeightKg = ex.targetWeightKg,
                    restSeconds = ex.restSeconds,
                    isWarmup = ex.isWarmup,
                    isDropSet = ex.isDropSet,
                    notes = ex.notes
                )
            }

            workoutTemplateDao.replaceTemplateWithExercises(updatedTemplate, entities)
        }
    }

    fun duplicateTemplate(templateId: String) {
        viewModelScope.launch {
            val existing = workoutTemplateDao.getTemplateByIdSync(templateId) ?: return@launch
            val exercises = workoutTemplateDao.getTemplateExercisesSync(templateId)

            val newId = UUID.randomUUID().toString()
            val newTemplate = existing.copy(
                id = newId,
                name = "${existing.name} (Copy)",
                createdAt = System.currentTimeMillis(),
                version = 1
            )

            val newExercises = exercises.map { ex ->
                ex.copy(
                    id = UUID.randomUUID().toString(),
                    templateId = newId
                )
            }

            workoutTemplateDao.insertTemplate(newTemplate)
            workoutTemplateDao.insertTemplateExercises(newExercises)
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            workoutTemplateDao.deleteTemplate(templateId)
        }
    }
}
