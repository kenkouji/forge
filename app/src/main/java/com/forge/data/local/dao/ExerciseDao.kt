package com.forge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

data class ExerciseMuscleDetail(
    val muscleId: String,
    val muscleName: String,
    val bodyPart: String,
    val role: String,
    val isForgeDerived: Boolean
)

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(aliases: List<ExerciseAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscles(muscles: List<MuscleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseMuscles(exerciseMuscles: List<ExerciseMuscleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseEquipment(exerciseEquipment: List<ExerciseEquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseAttributes(attributes: List<ExerciseAttributeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseFamilies(families: List<ExerciseFamilyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseFamilyMembers(members: List<ExerciseFamilyMemberEntity>)

    @Transaction
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
    ) {
        insertMuscles(muscles)
        insertEquipment(equipment)
        insertExerciseFamilies(families)
        insertExercises(exercises)
        insertExerciseMuscles(exerciseMuscles)
        insertExerciseEquipment(exerciseEquipment)
        insertExerciseAttributes(exerciseAttributes)
        insertExerciseFamilyMembers(exerciseFamilyMembers)
        insertAliases(aliases)
    }

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun getExerciseById(id: String): Flow<ExerciseEntity?>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Transaction
    @Query(
        """
        SELECT * FROM exercises WHERE rowid IN (
            SELECT rowid FROM exercises_fts WHERE exercises_fts MATCH :ftsQuery
        )
        UNION
        SELECT exercises.* FROM exercises
        INNER JOIN exercise_aliases ON exercises.id = exercise_aliases.exercise_id
        WHERE exercise_aliases.alias LIKE '%' || :rawQuery || '%'
        UNION
        SELECT * FROM exercises
        WHERE name LIKE '%' || :rawQuery || '%'
        ORDER BY name ASC
        """
    )
    fun searchExercises(ftsQuery: String, rawQuery: String): Flow<List<ExerciseEntity>>

    /**
     * Combined search and relational filtering (Correction #3).
     * FTS / alias matching produces candidate exercises, relational SQL applies exact filters.
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        LEFT JOIN exercise_muscles em ON e.id = em.exercise_id
        LEFT JOIN exercise_equipment eq ON e.id = eq.exercise_id
        LEFT JOIN exercise_family_members efm ON e.id = efm.exercise_id
        WHERE (:hasQuery = 0 OR e.rowid IN (SELECT rowid FROM exercises_fts WHERE exercises_fts MATCH :ftsQuery) OR e.id IN (SELECT exercise_id FROM exercise_aliases WHERE alias LIKE '%' || :rawQuery || '%'))
          AND (:muscleId IS NULL OR em.muscle_id = :muscleId)
          AND (:equipmentId IS NULL OR eq.equipment_id = :equipmentId)
          AND (:movementPattern IS NULL OR e.forge_movement_pattern = :movementPattern)
          AND (:experienceLevel IS NULL OR e.source_level = :experienceLevel)
          AND (:familyId IS NULL OR efm.family_id = :familyId)
        ORDER BY e.name ASC
        """
    )
    fun searchAndFilterExercises(
        hasQuery: Int,
        ftsQuery: String,
        rawQuery: String,
        muscleId: String?,
        equipmentId: String?,
        movementPattern: String?,
        experienceLevel: String?,
        familyId: String?
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT em.muscle_id AS muscleId, m.name AS muscleName, m.body_part AS bodyPart, em.role AS role, em.is_forge_derived AS isForgeDerived
        FROM exercise_muscles em
        JOIN muscles m ON em.muscle_id = m.id
        WHERE em.exercise_id = :exerciseId
        ORDER BY CASE WHEN em.role = 'PRIMARY' THEN 0 ELSE 1 END, m.name ASC
        """
    )
    fun getMusclesForExercise(exerciseId: String): Flow<List<ExerciseMuscleDetail>>

    @Query(
        """
        SELECT eq.* FROM equipment eq
        JOIN exercise_equipment ee ON eq.id = ee.equipment_id
        WHERE ee.exercise_id = :exerciseId
        ORDER BY eq.name ASC
        """
    )
    fun getEquipmentForExercise(exerciseId: String): Flow<List<EquipmentEntity>>

    @Query("SELECT attribute FROM exercise_attributes WHERE exercise_id = :exerciseId ORDER BY attribute ASC")
    fun getAttributesForExercise(exerciseId: String): Flow<List<String>>

    @Query(
        """
        SELECT ef.* FROM exercise_families ef
        JOIN exercise_family_members efm ON ef.id = efm.family_id
        WHERE efm.exercise_id = :exerciseId
        LIMIT 1
        """
    )
    fun getFamilyForExercise(exerciseId: String): Flow<ExerciseFamilyEntity?>

    @Query(
        """
        SELECT e.* FROM exercises e
        JOIN exercise_family_members efm ON e.id = efm.exercise_id
        WHERE efm.family_id = :familyId AND e.id != :currentExerciseId
        ORDER BY e.name ASC
        """
    )
    fun getFamilySiblings(familyId: String, currentExerciseId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM muscles ORDER BY body_part ASC, name ASC")
    fun getAllMuscles(): Flow<List<MuscleEntity>>

    @Query("SELECT * FROM equipment ORDER BY name ASC")
    fun getAllEquipment(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM exercise_families ORDER BY name ASC")
    fun getAllFamilies(): Flow<List<ExerciseFamilyEntity>>

    // Popular Exercise Queries (Part A)
    @Query("SELECT * FROM exercises WHERE is_popular = 1 ORDER BY popularity_rank ASC, name ASC")
    fun getPopularExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises WHERE is_popular = 1")
    suspend fun getPopularExercisesCount(): Int

    @Query("UPDATE exercises SET is_popular = :isPopular, popularity_rank = :rank, youtube_video_id = :youtubeId WHERE id = :exerciseId")
    suspend fun updateExercisePopularity(exerciseId: String, isPopular: Boolean, rank: Int, youtubeId: String?)

    @Query("SELECT * FROM exercises WHERE is_favorite = 1 ORDER BY name ASC")
    fun getFavoriteExercises(): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT DISTINCT e.* FROM exercises e
        INNER JOIN workout_sets ws ON e.id = ws.exercise_id
        INNER JOIN workout_sessions s ON ws.session_id = s.id
        WHERE s.status = 'COMPLETED'
        ORDER BY s.end_time DESC
        LIMIT :limit
        """
    )
    fun getRecentlyUsedExercises(limit: Int = 20): Flow<List<ExerciseEntity>>

    @Query("UPDATE exercises SET is_favorite = :isFavorite WHERE id = :exerciseId")
    suspend fun setExerciseFavorite(exerciseId: String, isFavorite: Boolean)

    // Progression & PR Queries (Part D)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressionRecord(record: com.forge.data.local.entity.ExerciseProgressionRecordEntity)

    @Query("SELECT * FROM exercise_progression_records WHERE exercise_id = :exerciseId ORDER BY created_at DESC")
    fun getProgressionHistory(exerciseId: String): Flow<List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>>

    @Query("SELECT * FROM exercise_progression_records WHERE exercise_id = :exerciseId ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestProgressionDirect(exerciseId: String): com.forge.data.local.entity.ExerciseProgressionRecordEntity?

    @Query("SELECT * FROM exercise_progression_records WHERE exercise_id = :exerciseId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentProgressionHistoryDirect(exerciseId: String, limit: Int = 5): List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>

    @Query("SELECT * FROM exercise_progression_records")
    fun getAllProgressionRecords(): Flow<List<com.forge.data.local.entity.ExerciseProgressionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(pr: com.forge.data.local.entity.ExercisePersonalRecordEntity)

    @Query("SELECT * FROM exercise_personal_records WHERE exercise_id = :exerciseId LIMIT 1")
    fun getPersonalRecord(exerciseId: String): Flow<com.forge.data.local.entity.ExercisePersonalRecordEntity?>

    @Query("SELECT * FROM exercise_personal_records WHERE exercise_id = :exerciseId LIMIT 1")
    suspend fun getPersonalRecordDirect(exerciseId: String): com.forge.data.local.entity.ExercisePersonalRecordEntity?

    @Query("SELECT * FROM exercise_personal_records ORDER BY achieved_at DESC")
    fun getAllPersonalRecords(): Flow<List<com.forge.data.local.entity.ExercisePersonalRecordEntity>>


    // Nutrition & Weight Queries (Part E)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNutritionProfile(profile: com.forge.data.local.entity.UserNutritionProfileEntity)

    @Query("SELECT * FROM user_nutrition_profile WHERE id = 'primary_profile' LIMIT 1")
    fun getNutritionProfile(): Flow<com.forge.data.local.entity.UserNutritionProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: com.forge.data.local.entity.WeightLogEntity)

    @Query("SELECT * FROM weight_logs ORDER BY logged_date DESC LIMIT :limit")
    fun getWeightLogs(limit: Int = 30): Flow<List<com.forge.data.local.entity.WeightLogEntity>>
}
