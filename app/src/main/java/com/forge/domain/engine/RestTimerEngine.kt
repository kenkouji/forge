package com.forge.domain.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RestTimerState(
    val isRunning: Boolean = false,
    val restDurationSeconds: Int = 90,
    val restStartedAt: Long = 0L,
    val restEndsAt: Long = 0L,
    val remainingSeconds: Int = 0,
    val lastAnnouncedSecond: Int = -1
)

/**
 * Smart Rest Timer Engine.
 * Persists timestamps (restStartedAt, restDurationSeconds, restEndsAt) to prevent drift
 * and allow seamless recovery across process recreations, screen locks, and app switching.
 */
class RestTimerEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val onTickSecond: ((remainingSeconds: Int) -> Unit)? = null,
    private val onTimerComplete: (() -> Unit)? = null
) {
    private val _timerState = MutableStateFlow(RestTimerState())
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Starts a new rest countdown using epoch timestamps.
     */
    fun startRest(durationSeconds: Int = 90) {
        val now = System.currentTimeMillis()
        val endsAt = now + (durationSeconds * 1000L)

        _timerState.value = RestTimerState(
            isRunning = true,
            restDurationSeconds = durationSeconds,
            restStartedAt = now,
            restEndsAt = endsAt,
            remainingSeconds = durationSeconds,
            lastAnnouncedSecond = -1
        )

        startTicker()
    }

    /**
     * Pauses the current rest timer while preserving remaining time.
     */
    fun pauseRest() {
        countdownJob?.cancel()
        val remaining = calculateRemainingSeconds()
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            remainingSeconds = remaining
        )
    }

    /**
     * Resumes a paused timer with the current remaining seconds.
     */
    fun resumeRest() {
        val remaining = _timerState.value.remainingSeconds
        if (remaining > 0) {
            startRest(remaining)
        }
    }

    /**
     * Cancels the active rest countdown.
     */
    fun cancelRest() {
        countdownJob?.cancel()
        _timerState.value = RestTimerState()
    }

    /**
     * Dynamically adds or subtracts seconds from the running rest timer.
     */
    fun addSeconds(deltaSeconds: Int) {
        if (!_timerState.value.isRunning) return
        val now = System.currentTimeMillis()
        val newEndsAt = maxOf(now + 1000L, _timerState.value.restEndsAt + (deltaSeconds * 1000L))
        val remaining = maxOf(1, ((newEndsAt - now) / 1000L).toInt())
        _timerState.value = _timerState.value.copy(
            restEndsAt = newEndsAt,
            remainingSeconds = remaining
        )
    }


    /**
     * Reconstructs or adjusts timer state after process recovery or app resume.
     */
    fun restoreState(startedAt: Long, durationSeconds: Int, endsAt: Long) {
        val now = System.currentTimeMillis()
        if (now >= endsAt) {
            // Already expired while app was backgrounded
            _timerState.value = RestTimerState(
                isRunning = false,
                restDurationSeconds = durationSeconds,
                remainingSeconds = 0
            )
            onTimerComplete?.invoke()
        } else {
            val remaining = ((endsAt - now) / 1000L).toInt()
            _timerState.value = RestTimerState(
                isRunning = true,
                restDurationSeconds = durationSeconds,
                restStartedAt = startedAt,
                restEndsAt = endsAt,
                remainingSeconds = remaining
            )
            startTicker()
        }
    }

    private fun calculateRemainingSeconds(): Int {
        val endsAt = _timerState.value.restEndsAt
        val now = System.currentTimeMillis()
        return if (endsAt > now) {
            ((endsAt - now) / 1000L).toInt()
        } else {
            0
        }
    }

    private fun startTicker() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (isActive && _timerState.value.isRunning) {
                val remaining = calculateRemainingSeconds()
                val current = _timerState.value

                _timerState.value = current.copy(remainingSeconds = remaining)

                if (remaining != current.lastAnnouncedSecond) {
                    _timerState.value = _timerState.value.copy(lastAnnouncedSecond = remaining)
                    onTickSecond?.invoke(remaining)
                }

                if (remaining <= 0) {
                    _timerState.value = _timerState.value.copy(isRunning = false, remainingSeconds = 0)
                    onTimerComplete?.invoke()
                    break
                }

                delay(250) // High-precision polling against timestamps to avoid drift
            }
        }
    }
}
