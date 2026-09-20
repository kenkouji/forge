package com.forge.domain.repository

import com.forge.data.local.dao.ExerciseMuscleDetail
import com.forge.data.local.entity.EquipmentEntity
import com.forge.data.local.entity.ExerciseAliasEntity
import com.forge.data.local.entity.ExerciseAttributeEntity
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.ExerciseEquipmentEntity
import com.forge.data.local.entity.ExerciseFamilyEntity
import com.forge.data.local.entity.ExerciseFamilyMemberEntity
import com.forge.data.local.entity.ExerciseMuscleEntity
import com.forge.data.local.entity.MuscleEntity
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<ExerciseEntity>>
    fun getExerciseById(id: String): Flow<ExerciseEntity?>
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>
    fun searchAndFilterExercises(
        query: String? = null,
        muscleId: String? = null,
        equipmentId: String? = null,
        movementPattern: String? = null,
        experienceLevel: String? = null,
        familyId: String? = null
    ): Flow<List<ExerciseEntity>>
    fun getMusclesForExercise(exerciseId: String): Flow<List<ExerciseMuscleDetail>>
    fun getEquipmentForExercise(exerciseId: String): Flow<List<EquipmentEntity>>
    fun getAttributesForExercise(exerciseId: String): Flow<List<String>>
    fun getFamilyForExercise(exerciseId: String): Flow<ExerciseFamilyEntity?>
    fun getFamilySiblings(familyId: String, currentExerciseId: String): Flow<List<ExerciseEntity>>
    fun getAllMuscles(): Flow<List<MuscleEntity>>
    fun getAllEquipment(): Flow<List<EquipmentEntity>>
    fun getAllFamilies(): Flow<List<ExerciseFamilyEntity>>
    suspend fun getExerciseCount(): Int
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
    suspend fun insertAliases(aliases: List<ExerciseAliasEntity>)
    suspend fun insertCustomExercise(
        exercise: ExerciseEntity,
        primaryMuscleId: String? = null,
        equipmentId: String? = null
    )
    fun getPopularExercises(): Flow<List<ExerciseEntity>>
    fun getFavoriteExercises(): Flow<List<ExerciseEntity>>
    fun getRecentlyUsedExercises(limit: Int = 20): Flow<List<ExerciseEntity>>
    suspend fun toggleFavorite(exerciseId: String, isFavorite: Boolean)

    suspend fun seedRelationalDatabase(
        muscles: List<MuscleEntity>,
        equipment: List<EquipmentEntity>,
        families: List<ExerciseFamilyEntity>,
        exercises: List<ExerciseEntity>,
        exerciseMuscles: List<ExerciseMuscleEntity>,
        exerciseEquipment: List<ExerciseEquipmentEntity>,
        exerciseAttributes: List<ExerciseAttributeEntity>,
        exerciseFamilyMembers: List<ExerciseFamilyMemberEntity>,
        aliases: List<ExerciseAliasEntity>
    )

    // Progression & PR
    suspend fun insertProgressionRecord(record: com.forge.data.local.entity.ExerciseProgressionRecordEntity)
    fun getProgressionHistory(exerciseId: String): Flow<List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>>
    suspend fun getLatestProgressionDirect(exerciseId: String): com.forge.data.local.entity.ExerciseProgressionRecordEntity?
    suspend fun getRecentProgressionHistoryDirect(exerciseId: String, limit: Int = 5): List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>
    fun getAllProgressionRecords(): Flow<List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>>
    suspend fun insertPersonalRecord(pr: com.forge.data.local.entity.ExercisePersonalRecordEntity)
    fun getPersonalRecord(exerciseId: String): Flow<com.forge.data.local.entity.ExercisePersonalRecordEntity?>
    suspend fun getPersonalRecordDirect(exerciseId: String): com.forge.data.local.entity.ExercisePersonalRecordEntity?
    fun getAllPersonalRecords(): Flow<List<com.forge.data.local.entity.ExercisePersonalRecordEntity>>
}

