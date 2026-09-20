package com.forge.data.repository

import com.forge.data.local.dao.ExerciseDao
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
import com.forge.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow

class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<ExerciseEntity>> =
        exerciseDao.getAllExercises()

    override fun getExerciseById(id: String): Flow<ExerciseEntity?> =
        exerciseDao.getExerciseById(id)

    override fun searchExercises(query: String): Flow<List<ExerciseEntity>> {
        val sanitized = query.trim()
        val ftsQuery = if (sanitized.isNotEmpty()) "$sanitized*" else "*"
        return exerciseDao.searchExercises(ftsQuery = ftsQuery, rawQuery = sanitized)
    }

    override fun searchAndFilterExercises(
        query: String?,
        muscleId: String?,
        equipmentId: String?,
        movementPattern: String?,
        experienceLevel: String?,
        familyId: String?
    ): Flow<List<ExerciseEntity>> {
        val sanitized = query?.trim().orEmpty()
        val hasQuery = if (sanitized.isNotEmpty()) 1 else 0
        val ftsQuery = if (sanitized.isNotEmpty()) "$sanitized*" else ""
        return exerciseDao.searchAndFilterExercises(
            hasQuery = hasQuery,
            ftsQuery = ftsQuery,
            rawQuery = sanitized,
            muscleId = muscleId,
            equipmentId = equipmentId,
            movementPattern = movementPattern,
            experienceLevel = experienceLevel,
            familyId = familyId
        )
    }

    override fun getMusclesForExercise(exerciseId: String): Flow<List<ExerciseMuscleDetail>> =
        exerciseDao.getMusclesForExercise(exerciseId)

    override fun getEquipmentForExercise(exerciseId: String): Flow<List<EquipmentEntity>> =
        exerciseDao.getEquipmentForExercise(exerciseId)

    override fun getAttributesForExercise(exerciseId: String): Flow<List<String>> =
        exerciseDao.getAttributesForExercise(exerciseId)

    override fun getFamilyForExercise(exerciseId: String): Flow<ExerciseFamilyEntity?> =
        exerciseDao.getFamilyForExercise(exerciseId)

    override fun getFamilySiblings(familyId: String, currentExerciseId: String): Flow<List<ExerciseEntity>> =
        exerciseDao.getFamilySiblings(familyId, currentExerciseId)

    override fun getAllMuscles(): Flow<List<MuscleEntity>> =
        exerciseDao.getAllMuscles()

    override fun getAllEquipment(): Flow<List<EquipmentEntity>> =
        exerciseDao.getAllEquipment()

    override fun getAllFamilies(): Flow<List<ExerciseFamilyEntity>> =
        exerciseDao.getAllFamilies()

    override suspend fun getExerciseCount(): Int =
        exerciseDao.getExerciseCount()

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) =
        exerciseDao.insertExercises(exercises)

    override suspend fun insertAliases(aliases: List<ExerciseAliasEntity>) =
        exerciseDao.insertAliases(aliases)

    override suspend fun insertCustomExercise(
        exercise: ExerciseEntity,
        primaryMuscleId: String?,
        equipmentId: String?
    ) {
        exerciseDao.insertExercise(exercise)
        if (!primaryMuscleId.isNullOrBlank()) {
            exerciseDao.insertExerciseMuscles(
                listOf(
                    ExerciseMuscleEntity(
                        exerciseId = exercise.id,
                        muscleId = primaryMuscleId,
                        role = "PRIMARY",
                        isForgeDerived = false
                    )
                )
            )
        }
        if (!equipmentId.isNullOrBlank()) {
            exerciseDao.insertExerciseEquipment(
                listOf(
                    ExerciseEquipmentEntity(
                        exerciseId = exercise.id,
                        equipmentId = equipmentId,
                        isPrimary = true
                    )
                )
            )
        }
    }

    override suspend fun seedRelationalDatabase(
        muscles: List<MuscleEntity>,
        equipment: List<EquipmentEntity>,
        families: List<ExerciseFamilyEntity>,
        exercises: List<ExerciseEntity>,
        exerciseMuscles: List<ExerciseMuscleEntity>,
        exerciseEquipment: List<ExerciseEquipmentEntity>,
        exerciseAttributes: List<ExerciseAttributeEntity>,
        exerciseFamilyMembers: List<ExerciseFamilyMemberEntity>,
        aliases: List<ExerciseAliasEntity>
    ) {
        exerciseDao.seedRelationalDatabase(
            muscles = muscles,
            equipment = equipment,
            families = families,
            exercises = exercises,
            exerciseMuscles = exerciseMuscles,
            exerciseEquipment = exerciseEquipment,
            exerciseAttributes = exerciseAttributes,
            exerciseFamilyMembers = exerciseFamilyMembers,
            aliases = aliases
        )
    }

    override fun getPopularExercises(): Flow<List<ExerciseEntity>> {
        return exerciseDao.getPopularExercises()
    }

    override fun getFavoriteExercises(): Flow<List<ExerciseEntity>> {
        return exerciseDao.getFavoriteExercises()
    }

    override fun getRecentlyUsedExercises(limit: Int): Flow<List<ExerciseEntity>> {
        return exerciseDao.getRecentlyUsedExercises(limit)
    }

    override suspend fun toggleFavorite(exerciseId: String, isFavorite: Boolean) {
        exerciseDao.setExerciseFavorite(exerciseId, isFavorite)
    }

    override suspend fun insertProgressionRecord(record: com.forge.data.local.entity.ExerciseProgressionRecordEntity) {
        exerciseDao.insertProgressionRecord(record)
    }

    override fun getProgressionHistory(exerciseId: String): Flow<List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>> {
        return exerciseDao.getProgressionHistory(exerciseId)
    }

    override suspend fun getLatestProgressionDirect(exerciseId: String): com.forge.data.local.entity.ExerciseProgressionRecordEntity? {
        return exerciseDao.getLatestProgressionDirect(exerciseId)
    }

    override suspend fun getRecentProgressionHistoryDirect(exerciseId: String, limit: Int): List<com.forge.data.local.entity.ExerciseProgressionRecordEntity> {
        return exerciseDao.getRecentProgressionHistoryDirect(exerciseId, limit)
    }

    override fun getAllProgressionRecords(): Flow<List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>> {
        return exerciseDao.getAllProgressionRecords()
    }

    override suspend fun insertPersonalRecord(pr: com.forge.data.local.entity.ExercisePersonalRecordEntity) {
        exerciseDao.insertPersonalRecord(pr)
    }

    override fun getPersonalRecord(exerciseId: String): Flow<com.forge.data.local.entity.ExercisePersonalRecordEntity?> {
        return exerciseDao.getPersonalRecord(exerciseId)
    }

    override suspend fun getPersonalRecordDirect(exerciseId: String): com.forge.data.local.entity.ExercisePersonalRecordEntity? {
        return exerciseDao.getPersonalRecordDirect(exerciseId)
    }

    override fun getAllPersonalRecords(): Flow<List<com.forge.data.local.entity.ExercisePersonalRecordEntity>> {
        return exerciseDao.getAllPersonalRecords()
    }
}

