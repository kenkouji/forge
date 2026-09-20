package com.forge.presentation.workout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.forge.data.local.ForgeDatabase
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.repository.ExerciseRepositoryImpl
import com.forge.data.repository.WorkoutRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActiveWorkoutFlowTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var dbExecutor: ExecutorService
    private lateinit var database: ForgeDatabase
    private lateinit var workoutRepository: WorkoutRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl

    private val benchPress = ExerciseEntity(
        id = "barbell_bench_press",
        name = "Barbell Bench Press",
        canonicalName = "Barbell Bench Press",
        movementPattern = "PUSH",
        mechanic = "COMPOUND",
        forceType = "PUSH",
        experienceLevel = "INTERMEDIATE",
        instructions = "Lie on bench.",
        formCues = "Retract scapulae.",
        commonMistakes = "Flaring elbows."
    )

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        dbExecutor = Executors.newSingleThreadExecutor()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ForgeDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(dbExecutor)
            .setTransactionExecutor(dbExecutor)
            .build()
        workoutRepository = WorkoutRepositoryImpl(database.workoutDao())
        exerciseRepository = ExerciseRepositoryImpl(database.exerciseDao())

        runBlocking {
            exerciseRepository.insertExercises(listOf(benchPress))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
        dbExecutor.shutdown()
    }

    private fun flush() {
        repeat(3) {
            testDispatcher.scheduler.advanceUntilIdle()
            dbExecutor.submit {}.get(3, TimeUnit.SECONDS)
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun tapping_empty_workout_creates_active_workout_with_zero_exercises() = runTest(testDispatcher) {
        val viewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository)
        var startedSessionId: String? = null
        viewModel.startEmptyWorkout { id ->
            startedSessionId = id
        }
        flush()

        // 1. Session is created in Room with IN_PROGRESS status and name "Empty Workout"
        assertThat(startedSessionId).isNotNull()
        val activeSession = workoutRepository.getActiveSessionDirect()
        assertThat(activeSession).isNotNull()
        assertThat(activeSession?.id).isEqualTo(startedSessionId)
        assertThat(activeSession?.status).isEqualTo("IN_PROGRESS")
        assertThat(activeSession?.name).isEqualTo("Empty Workout")

        // 2. Newly created workout initially contains zero exercises
        val sets = workoutRepository.getSetsForSessionDirect(startedSessionId!!)
        assertThat(sets).isEmpty()
    }

    @Test
    fun workout_can_subsequently_add_an_exercise_and_sets() = runTest(testDispatcher) {
        val viewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository)
        var sessionId: String? = null
        viewModel.startEmptyWorkout { id -> sessionId = id }
        flush()

        // Add exercise to workout
        viewModel.addExerciseToWorkout(benchPress)
        flush()

        val setsAfterExercise = workoutRepository.getSetsForSessionDirect(sessionId!!)
        assertThat(setsAfterExercise).hasSize(1)
        val firstSet = setsAfterExercise[0]
        assertThat(firstSet.exerciseId).isEqualTo(benchPress.id)
        assertThat(firstSet.isCompleted).isFalse()

        // Add another set
        viewModel.addSet(benchPress.id)
        flush()

        val setsAfterSecond = workoutRepository.getSetsForSessionDirect(sessionId!!)
        assertThat(setsAfterSecond).hasSize(2)

        // Log and complete first set with 80kg x 8 reps
        viewModel.updateSetValues(firstSet, weightKg = 80.0, reps = 8, rpe = 8.0, rir = 2)
        flush()
        viewModel.toggleSetCompleted(firstSet.copy(weightKg = 80.0, reps = 8))
        flush()

        val updatedSets = workoutRepository.getSetsForSessionDirect(sessionId!!)
        assertThat(updatedSets[0].isCompleted).isTrue()
        assertThat(updatedSets[0].weightKg).isEqualTo(80.0)
        assertThat(updatedSets[0].reps).isEqualTo(8)

        // Verify session volume is updated
        val updatedSession = workoutRepository.getActiveSessionDirect()
        assertThat(updatedSession?.totalVolumeKg).isEqualTo(640.0)
    }

    @Test
    fun active_workout_survives_process_recreation_and_relaunch() = runTest(testDispatcher) {
        val viewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository)
        var sessionId: String? = null
        viewModel.startEmptyWorkout { id -> sessionId = id }
        flush()

        viewModel.addExerciseToWorkout(benchPress)
        flush()

        // Simulate process death and cold relaunch by creating a brand new ViewModel instance
        val reloadedViewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository)
        flush()

        // Reconstructed state has identical session and sets
        val active = workoutRepository.getActiveSessionDirect()
        assertThat(active).isNotNull()
        assertThat(active?.id).isEqualTo(sessionId)
        assertThat(active?.status).isEqualTo("IN_PROGRESS")

        val sets = workoutRepository.getSetsForSessionDirect(sessionId!!)
        assertThat(sets).hasSize(1)
        assertThat(sets[0].exerciseId).isEqualTo(benchPress.id)
    }

    @Test
    fun finishing_the_workout_changes_status_to_completed() = runTest(testDispatcher) {
        val viewModel = ActiveWorkoutViewModel(workoutRepository, exerciseRepository)
        var sessionId: String? = null
        viewModel.startEmptyWorkout { id -> sessionId = id }
        flush()

        var finished = false
        viewModel.finishWorkout { finished = true }
        flush()

        assertThat(finished).isTrue()

        // Active session is no longer returned
        val active = workoutRepository.getActiveSessionDirect()
        assertThat(active).isNull()

        // Stored session has status COMPLETED
        val completedSession = database.workoutDao().getSessionById(sessionId!!)
        assertThat(completedSession).isNotNull()
        assertThat(completedSession?.status).isEqualTo("COMPLETED")
        assertThat(completedSession?.endTime).isNotNull()
    }
}
