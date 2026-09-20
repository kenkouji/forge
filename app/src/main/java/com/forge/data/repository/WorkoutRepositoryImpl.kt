package com.forge.data.repository

import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity
import com.forge.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow

class WorkoutRepositoryImpl(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun getActiveSession(): Flow<WorkoutSessionEntity?> =
        workoutDao.getActiveSession()

    override suspend fun getActiveSessionDirect(): WorkoutSessionEntity? =
        workoutDao.getActiveSessionDirect()

    override fun getAllSessions(): Flow<List<WorkoutSessionEntity>> =
        workoutDao.getAllSessions()

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>> =
        workoutDao.getSetsForSession(sessionId)

    override suspend fun getSetsForSessionDirect(sessionId: String): List<WorkoutSetEntity> =
        workoutDao.getSetsForSessionDirect(sessionId)

    override suspend fun startWorkout(
        session: WorkoutSessionEntity,
        initialSets: List<WorkoutSetEntity>
    ) {
        workoutDao.insertSession(session)
        if (initialSets.isNotEmpty()) {
            workoutDao.insertSets(initialSets)
        }
    }

    override suspend fun logSetTransaction(
        set: WorkoutSetEntity,
        activeExerciseId: String?,
        activeSetIndex: Int,
        volumeDeltaKg: Double
    ) {
        workoutDao.logSetTransaction(
            set = set,
            activeExerciseId = activeExerciseId,
            activeSetIndex = activeSetIndex,
            volumeDeltaKg = volumeDeltaKg
        )
    }

    override suspend fun completeWorkout(
        sessionId: String,
        endTime: Long,
        durationSeconds: Long
    ) {
        workoutDao.completeSession(sessionId, endTime, durationSeconds)
    }

    override suspend fun discardWorkout(sessionId: String) {
        workoutDao.discardSession(sessionId)
    }
}
