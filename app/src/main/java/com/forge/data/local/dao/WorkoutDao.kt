package com.forge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY start_time DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY start_time DESC LIMIT 1")
    suspend fun getActiveSessionDirect(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY start_time DESC")
    suspend fun getAllSessionsSync(): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSet(set: WorkoutSetEntity)

    @Query("SELECT * FROM workout_sets WHERE session_id = :sessionId ORDER BY set_order ASC")
    fun getSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE session_id = :sessionId ORDER BY set_order ASC")
    suspend fun getSetsForSessionDirect(sessionId: String): List<WorkoutSetEntity>

    @Query(
        """
        UPDATE workout_sessions 
        SET active_exercise_id = :activeExerciseId,
            active_set_index = :activeSetIndex,
            total_volume_kg = total_volume_kg + :volumeDeltaKg,
            last_updated_at = :timestamp
        WHERE id = :sessionId
        """
    )
    suspend fun updateSessionActiveState(
        sessionId: String,
        activeExerciseId: String?,
        activeSetIndex: Int,
        volumeDeltaKg: Double,
        timestamp: Long
    )

    /**
     * Single transactional atomic operation to record a set and update session state.
     * Guarantees single source of truth and crash resilience.
     */
    @Transaction
    suspend fun logSetTransaction(
        set: WorkoutSetEntity,
        activeExerciseId: String?,
        activeSetIndex: Int,
        volumeDeltaKg: Double,
        timestamp: Long = System.currentTimeMillis()
    ) {
        insertOrUpdateSet(set)
        updateSessionActiveState(
            sessionId = set.sessionId,
            activeExerciseId = activeExerciseId,
            activeSetIndex = activeSetIndex,
            volumeDeltaKg = volumeDeltaKg,
            timestamp = timestamp
        )
    }

    @Query("UPDATE workout_sessions SET status = 'COMPLETED', end_time = :endTime, duration_seconds = :durationSeconds, last_updated_at = :endTime WHERE id = :sessionId")
    suspend fun completeSession(sessionId: String, endTime: Long, durationSeconds: Long)

    @Query("UPDATE workout_sessions SET status = 'DISCARDED', last_updated_at = :timestamp WHERE id = :sessionId")
    suspend fun discardSession(sessionId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM workout_sets WHERE exercise_id = :exerciseId ORDER BY id DESC")
    suspend fun getCompletedSetsForExerciseSync(exerciseId: String): List<WorkoutSetEntity>

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAllSets()
}
