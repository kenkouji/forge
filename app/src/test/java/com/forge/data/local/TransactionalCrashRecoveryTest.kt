package com.forge.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TransactionalCrashRecoveryTest {

    private lateinit var database: ForgeDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ForgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = database.workoutDao()
        exerciseDao = database.exerciseDao()

        // Insert required exercise reference
        runBlocking {
            exerciseDao.insertExercises(
                listOf(
                    ExerciseEntity(
                        id = "barbell_bench_press",
                        name = "Barbell Bench Press",
                        canonicalName = "Barbell Bench Press",
                        movementPattern = "PUSH",
                        mechanic = "COMPOUND",
                        forceType = "PUSH",
                        experienceLevel = "INTERMEDIATE",
                        instructions = "Lie on flat bench.",
                        formCues = "Retract scapulae.",
                        commonMistakes = "Flaring elbows."
                    )
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun activeSession_transactionalSetLogging_survivesAndReconstructsAccurately() = runBlocking {
        val sessionId = "session_test_101"
        val startTime = System.currentTimeMillis()

        // 1. User starts a workout session
        val session = WorkoutSessionEntity(
            id = sessionId,
            startTime = startTime,
            status = "IN_PROGRESS",
            activeExerciseId = "barbell_bench_press",
            activeSetIndex = 0,
            totalVolumeKg = 0.0
        )
        workoutDao.insertSession(session)

        // 2. User logs Set 1 (80 kg x 8 reps = 640 kg volume)
        val set1 = WorkoutSetEntity(
            id = "set_1",
            sessionId = sessionId,
            exerciseId = "barbell_bench_press",
            setOrder = 0,
            setType = "NORMAL",
            weightKg = 80.0,
            reps = 8,
            rpe = 8.0,
            rir = 2,
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )

        workoutDao.logSetTransaction(
            set = set1,
            activeExerciseId = "barbell_bench_press",
            activeSetIndex = 1,
            volumeDeltaKg = 80.0 * 8.0
        )

        // 3. User logs Set 2 (80 kg x 7 reps = 560 kg volume)
        val set2 = WorkoutSetEntity(
            id = "set_2",
            sessionId = sessionId,
            exerciseId = "barbell_bench_press",
            setOrder = 1,
            setType = "NORMAL",
            weightKg = 80.0,
            reps = 7,
            rpe = 9.0,
            rir = 1,
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )

        workoutDao.logSetTransaction(
            set = set2,
            activeExerciseId = "barbell_bench_press",
            activeSetIndex = 2,
            volumeDeltaKg = 80.0 * 7.0
        )

        // 4. Simulate App Process Kill / Cold Relaunch Recovery Check
        val recoveredSession = workoutDao.getActiveSessionDirect()
        assertThat(recoveredSession).isNotNull()
        assertThat(recoveredSession?.id).isEqualTo(sessionId)
        assertThat(recoveredSession?.status).isEqualTo("IN_PROGRESS")
        assertThat(recoveredSession?.activeExerciseId).isEqualTo("barbell_bench_press")
        assertThat(recoveredSession?.activeSetIndex).isEqualTo(2)
        assertThat(recoveredSession?.totalVolumeKg).isEqualTo(1200.0)

        // Verify sets were persisted accurately
        val sets = workoutDao.getSetsForSessionDirect(sessionId)
        assertThat(sets).hasSize(2)
        assertThat(sets[0].isCompleted).isTrue()
        assertThat(sets[0].reps).isEqualTo(8)
        assertThat(sets[1].isCompleted).isTrue()
        assertThat(sets[1].reps).isEqualTo(7)

        // 5. Complete Session
        workoutDao.completeSession(sessionId, endTime = System.currentTimeMillis(), durationSeconds = 1800)
        val afterCompletion = workoutDao.getActiveSessionDirect()
        assertThat(afterCompletion).isNull()
    }
}
