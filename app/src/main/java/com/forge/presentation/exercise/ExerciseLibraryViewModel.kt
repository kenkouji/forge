package com.forge.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.DatabaseSeedLoader
import com.forge.data.local.entity.EquipmentEntity
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.MuscleEntity
import com.forge.domain.repository.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ExerciseLibraryTab(val title: String) {
    POPULAR("Popular"),
    RECENT("Recent"),
    FAVORITES("Favorites"),
    BROWSE_ALL("Browse All")
}

data class ExerciseLibraryFilterState(
    val searchQuery: String = "",
    val activeTab: ExerciseLibraryTab = ExerciseLibraryTab.POPULAR,
    val selectedMuscleId: String? = null,
    val selectedEquipmentId: String? = null,
    val selectedMovementPattern: String? = null,
    val selectedExperienceLevel: String? = null
)

class ExerciseLibraryViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(ExerciseLibraryFilterState())
    val filterState: StateFlow<ExerciseLibraryFilterState> = _filterState.asStateFlow()

    val isSeeding: StateFlow<Boolean> = DatabaseSeedLoader.isSeeding

    val allMuscles: StateFlow<List<MuscleEntity>> = exerciseRepository.getAllMuscles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEquipment: StateFlow<List<EquipmentEntity>> = exerciseRepository.getAllEquipment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularExercises: StateFlow<List<ExerciseEntity>> = exerciseRepository.getPopularExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentExercises: StateFlow<List<ExerciseEntity>> = exerciseRepository.getRecentlyUsedExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteExercises: StateFlow<List<ExerciseEntity>> = exerciseRepository.getFavoriteExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises: StateFlow<List<ExerciseEntity>> = _filterState
        .flatMapLatest { filter ->
            if (filter.searchQuery.isNotBlank()) {
                // Unlimited Search: Searches entire 876+ exercise database across aliases, muscles, equipment
                exerciseRepository.searchExercises(filter.searchQuery)
            } else {
                when (filter.activeTab) {
                    ExerciseLibraryTab.POPULAR -> exerciseRepository.getPopularExercises()
                    ExerciseLibraryTab.RECENT -> exerciseRepository.getRecentlyUsedExercises()
                    ExerciseLibraryTab.FAVORITES -> exerciseRepository.getFavoriteExercises()
                    ExerciseLibraryTab.BROWSE_ALL -> {
                        exerciseRepository.searchAndFilterExercises(
                            query = null,
                            muscleId = filter.selectedMuscleId,
                            equipmentId = filter.selectedEquipmentId,
                            movementPattern = filter.selectedMovementPattern,
                            experienceLevel = filter.selectedExperienceLevel
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: ExerciseLibraryTab) {
        _filterState.value = _filterState.value.copy(activeTab = tab)
    }

    fun toggleFavorite(exerciseId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            exerciseRepository.toggleFavorite(exerciseId, !currentFavorite)
        }
    }

    fun onSearchQueryChange(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun selectMuscle(muscleId: String?) {
        _filterState.value = _filterState.value.copy(
            selectedMuscleId = if (_filterState.value.selectedMuscleId == muscleId) null else muscleId
        )
    }

    fun selectEquipment(equipmentId: String?) {
        _filterState.value = _filterState.value.copy(
            selectedEquipmentId = if (_filterState.value.selectedEquipmentId == equipmentId) null else equipmentId
        )
    }

    fun selectMovementPattern(pattern: String?) {
        _filterState.value = _filterState.value.copy(
            selectedMovementPattern = if (_filterState.value.selectedMovementPattern == pattern) null else pattern
        )
    }

    fun selectExperienceLevel(level: String?) {
        _filterState.value = _filterState.value.copy(
            selectedExperienceLevel = if (_filterState.value.selectedExperienceLevel == level) null else level
        )
    }

    fun clearFilters() {
        _filterState.value = ExerciseLibraryFilterState()
    }

    fun createCustomExercise(
        name: String,
        primaryMuscleId: String?,
        equipmentId: String?,
        movementPattern: String
    ) {
        viewModelScope.launch {
            val customId = "custom_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val exercise = ExerciseEntity(
                id = customId,
                name = name.trim(),
                canonicalName = name.trim(),
                movementPattern = movementPattern,
                forgeMovementPattern = movementPattern,
                isCustom = true,
                source = "custom",
                license = "User Created",
                searchTokens = "${name.lowercase()} custom ${primaryMuscleId ?: ""} ${equipmentId ?: ""}"
            )
            exerciseRepository.insertCustomExercise(
                exercise = exercise,
                primaryMuscleId = primaryMuscleId,
                equipmentId = equipmentId
            )
        }
    }
}
