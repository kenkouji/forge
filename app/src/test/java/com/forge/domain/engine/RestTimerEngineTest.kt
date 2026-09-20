package com.forge.domain.engine

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun timer_starts_with_correct_timestamps_and_duration() {
        val engine = RestTimerEngine(scope = testScope)
        engine.startRest(durationSeconds = 90)

        val state = engine.timerState.value
        assertThat(state.isRunning).isTrue()
        assertThat(state.restDurationSeconds).isEqualTo(90)
        assertThat(state.restEndsAt).isGreaterThan(state.restStartedAt)
    }

    @Test
    fun pause_and_resume_preserves_remaining_time() {
        val engine = RestTimerEngine(scope = testScope)
        engine.startRest(durationSeconds = 60)
        engine.pauseRest()

        val pausedState = engine.timerState.value
        assertThat(pausedState.isRunning).isFalse()

        engine.resumeRest()
        assertThat(engine.timerState.value.isRunning).isTrue()
    }

    @Test
    fun timer_survives_process_recovery_using_timestamps() {
        val engine = RestTimerEngine(scope = testScope)
        val now = System.currentTimeMillis()
        val startedAt = now - 30_000 // started 30 seconds ago
        val endsAt = now + 60_000 // 60 seconds left

        engine.restoreState(startedAt = startedAt, durationSeconds = 90, endsAt = endsAt)

        val state = engine.timerState.value
        assertThat(state.isRunning).isTrue()
        assertThat(state.remainingSeconds).isIn(58..62)
    }

    @Test
    fun timer_completes_gracefully_if_expired_while_backgrounded() {
        var completedCalled = false
        val engine = RestTimerEngine(
            scope = testScope,
            onTimerComplete = { completedCalled = true }
        )

        val now = System.currentTimeMillis()
        val startedAt = now - 120_000
        val endsAt = now - 30_000 // expired 30s ago

        engine.restoreState(startedAt = startedAt, durationSeconds = 90, endsAt = endsAt)

        assertThat(engine.timerState.value.isRunning).isFalse()
        assertThat(engine.timerState.value.remainingSeconds).isEqualTo(0)
        assertThat(completedCalled).isTrue()
    }
}
