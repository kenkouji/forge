package com.forge.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.dao.ExerciseMuscleDetail
import com.forge.data.local.entity.EquipmentEntity
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.ExerciseFamilyEntity
import com.forge.domain.repository.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val muscles: List<ExerciseMuscleDetail> = emptyList(),
    val equipment: List<EquipmentEntity> = emptyList(),
    val attributes: List<String> = emptyList(),
    val family: ExerciseFamilyEntity? = null,
    val familySiblings: List<ExerciseEntity> = emptyList(),
    val isLoading: Boolean = true
)

class ExerciseDetailViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _currentExerciseId = MutableStateFlow<String?>(null)

    fun loadExercise(exerciseId: String) {
        _currentExerciseId.value = exerciseId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercise: StateFlow<ExerciseEntity?> = _currentExerciseId
        .flatMapLatest { id ->
            if (id != null) exerciseRepository.getExerciseById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val muscles: StateFlow<List<ExerciseMuscleDetail>> = _currentExerciseId
        .flatMapLatest { id ->
            if (id != null) exerciseRepository.getMusclesForExercise(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val equipment: StateFlow<List<EquipmentEntity>> = _currentExerciseId
        .flatMapLatest { id ->
            if (id != null) exerciseRepository.getEquipmentForExercise(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val attributes: StateFlow<List<String>> = _currentExerciseId
        .flatMapLatest { id ->
            if (id != null) exerciseRepository.getAttributesForExercise(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val family: StateFlow<ExerciseFamilyEntity?> = _currentExerciseId
        .flatMapLatest { id ->
            if (id != null) exerciseRepository.getFamilyForExercise(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val siblings: StateFlow<List<ExerciseEntity>> = family
        .flatMapLatest { fam ->
            val curId = _currentExerciseId.value
            if (fam != null && curId != null) {
                exerciseRepository.getFamilySiblings(fam.id, curId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val progressionRecord: StateFlow<com.forge.data.local.entity.ExerciseProgressionRecordEntity?> = _currentExerciseId
        .flatMapLatest { id ->
            if (id != null) {
                kotlinx.coroutines.flow.flow<com.forge.data.local.entity.ExerciseProgressionRecordEntity?> {
                    emit(exerciseRepository.getLatestProgressionDirect(id))
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val personalRecord: StateFlow<com.forge.data.local.entity.ExercisePersonalRecordEntity?> = _currentExerciseId
        .flatMapLatest { id ->
            if (id != null) exerciseRepository.getPersonalRecord(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleFavorite() {
        val ex = exercise.value ?: return
        viewModelScope.launch {
            exerciseRepository.toggleFavorite(ex.id, !ex.isFavorite)
        }
    }
}

