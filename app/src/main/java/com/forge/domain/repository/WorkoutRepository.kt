package com.forge.domain.repository

import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getActiveSession(): Flow<WorkoutSessionEntity?>
    suspend fun getActiveSessionDirect(): WorkoutSessionEntity?
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>
    fun getSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>>
    suspend fun getSetsForSessionDirect(sessionId: String): List<WorkoutSetEntity>
    suspend fun startWorkout(session: WorkoutSessionEntity, initialSets: List<WorkoutSetEntity> = emptyList())
    suspend fun logSetTransaction(
        set: WorkoutSetEntity,
        activeExerciseId: String?,
        activeSetIndex: Int,
        volumeDeltaKg: Double
    )
    suspend fun completeWorkout(sessionId: String, endTime: Long, durationSeconds: Long)
    suspend fun discardWorkout(sessionId: String)
}
