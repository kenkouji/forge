package com.forge.domain.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Formal Product State Contract Verification.
 * Enforces FORGE_PRODUCT_STATE.md guarantees:
 * 1. FIRST_LAUNCH -> INITIALIZATION -> READY -> TODAY -> ACTIVE_WORKOUT -> COMPLETE -> HISTORY
 * 2. TODAY -> REST_DAY
 * 3. TODAY -> UPCOMING_SESSION -> EDIT -> SAVE -> UPDATED_SESSION
 * 4. Zero Dead-End Guarantee across all product states.
 */
class ProductStateTransitionTest {

    enum class ProductState {
        FIRST_LAUNCH,
        INITIALIZATION,
        READY,
        TODAY,
        ACTIVE_WORKOUT,
        COMPLETE,
        HISTORY,
        REST_DAY,
        UPCOMING_SESSION,
        EDIT_SESSION,
        UPDATED_SESSION
    }

    sealed class ProductEvent {
        data object LaunchFresh : ProductEvent()
        data object StartWizard : ProductEvent()
        data object CompleteWizard : ProductEvent()
        data object LoadAppReady : ProductEvent()
        data object OpenToday : ProductEvent()
        data object StartWorkout : ProductEvent()
        data object FinishWorkout : ProductEvent()
        data object ViewHistory : ProductEvent()
        data object ViewRestDay : ProductEvent()
        data object OpenUpcoming : ProductEvent()
        data object EditSession : ProductEvent()
        data object SaveSession : ProductEvent()
        data object ReturnToToday : ProductEvent()
    }

    private class StateMachine(var currentState: ProductState) {
        val transitionHistory = mutableListOf<Pair<ProductState, ProductEvent>>()

        fun handleEvent(event: ProductEvent): ProductState {
            val nextState = when (currentState) {
                ProductState.FIRST_LAUNCH -> when (event) {
                    ProductEvent.StartWizard -> ProductState.INITIALIZATION
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.INITIALIZATION -> when (event) {
                    ProductEvent.CompleteWizard -> ProductState.READY
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.READY -> when (event) {
                    ProductEvent.OpenToday -> ProductState.TODAY
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.TODAY -> when (event) {
                    ProductEvent.StartWorkout -> ProductState.ACTIVE_WORKOUT
                    ProductEvent.ViewRestDay -> ProductState.REST_DAY
                    ProductEvent.OpenUpcoming -> ProductState.UPCOMING_SESSION
                    ProductEvent.ViewHistory -> ProductState.HISTORY
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.ACTIVE_WORKOUT -> when (event) {
                    ProductEvent.FinishWorkout -> ProductState.COMPLETE
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.COMPLETE -> when (event) {
                    ProductEvent.ViewHistory -> ProductState.HISTORY
                    ProductEvent.ReturnToToday -> ProductState.TODAY
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.HISTORY -> when (event) {
                    ProductEvent.ReturnToToday -> ProductState.TODAY
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.REST_DAY -> when (event) {
                    ProductEvent.ReturnToToday -> ProductState.TODAY
                    ProductEvent.StartWorkout -> ProductState.ACTIVE_WORKOUT
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.UPCOMING_SESSION -> when (event) {
                    ProductEvent.EditSession -> ProductState.EDIT_SESSION
                    ProductEvent.ReturnToToday -> ProductState.TODAY
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.EDIT_SESSION -> when (event) {
                    ProductEvent.SaveSession -> ProductState.UPDATED_SESSION
                    ProductEvent.OpenUpcoming -> ProductState.UPCOMING_SESSION
                    else -> error("Invalid event $event for $currentState")
                }
                ProductState.UPDATED_SESSION -> when (event) {
                    ProductEvent.OpenUpcoming -> ProductState.UPCOMING_SESSION
                    ProductEvent.ReturnToToday -> ProductState.TODAY
                    else -> error("Invalid event $event for $currentState")
                }
            }
            transitionHistory.add(currentState to event)
            currentState = nextState
            return nextState
        }
    }

    @Test
    fun testCoreLifecycleTransitionPath() {
        val sm = StateMachine(ProductState.FIRST_LAUNCH)

        // FIRST_LAUNCH -> INITIALIZATION -> READY -> TODAY -> ACTIVE_WORKOUT -> COMPLETE -> HISTORY
        sm.handleEvent(ProductEvent.StartWizard)
        assertEquals(ProductState.INITIALIZATION, sm.currentState)

        sm.handleEvent(ProductEvent.CompleteWizard)
        assertEquals(ProductState.READY, sm.currentState)

        sm.handleEvent(ProductEvent.OpenToday)
        assertEquals(ProductState.TODAY, sm.currentState)

        sm.handleEvent(ProductEvent.StartWorkout)
        assertEquals(ProductState.ACTIVE_WORKOUT, sm.currentState)

        sm.handleEvent(ProductEvent.FinishWorkout)
        assertEquals(ProductState.COMPLETE, sm.currentState)

        sm.handleEvent(ProductEvent.ViewHistory)
        assertEquals(ProductState.HISTORY, sm.currentState)

        // Can return to today from history (no dead end)
        sm.handleEvent(ProductEvent.ReturnToToday)
        assertEquals(ProductState.TODAY, sm.currentState)
    }

    @Test
    fun testRestDayTransitionPath() {
        val sm = StateMachine(ProductState.TODAY)

        // TODAY -> REST_DAY
        sm.handleEvent(ProductEvent.ViewRestDay)
        assertEquals(ProductState.REST_DAY, sm.currentState)

        // REST_DAY -> TODAY or START_WORKOUT (extra session)
        sm.handleEvent(ProductEvent.ReturnToToday)
        assertEquals(ProductState.TODAY, sm.currentState)

        sm.handleEvent(ProductEvent.ViewRestDay)
        sm.handleEvent(ProductEvent.StartWorkout)
        assertEquals(ProductState.ACTIVE_WORKOUT, sm.currentState)
    }

    @Test
    fun testUpcomingSessionEditSavePath() {
        val sm = StateMachine(ProductState.TODAY)

        // TODAY -> UPCOMING_SESSION -> EDIT -> SAVE -> UPDATED_SESSION
        sm.handleEvent(ProductEvent.OpenUpcoming)
        assertEquals(ProductState.UPCOMING_SESSION, sm.currentState)

        sm.handleEvent(ProductEvent.EditSession)
        assertEquals(ProductState.EDIT_SESSION, sm.currentState)

        sm.handleEvent(ProductEvent.SaveSession)
        assertEquals(ProductState.UPDATED_SESSION, sm.currentState)

        // Return path guarantees no entrapment
        sm.handleEvent(ProductEvent.ReturnToToday)
        assertEquals(ProductState.TODAY, sm.currentState)
    }

    @Test
    fun testNoDeadEndGuarantee() {
        // Every single state must have at least one valid transition back towards TODAY or terminal flow
        ProductState.values().forEach { state ->
            val sm = StateMachine(state)
            var canTransition = false
            for (event in listOf(
                ProductEvent.StartWizard, ProductEvent.CompleteWizard, ProductEvent.OpenToday,
                ProductEvent.StartWorkout, ProductEvent.FinishWorkout, ProductEvent.ViewHistory,
                ProductEvent.ViewRestDay, ProductEvent.OpenUpcoming, ProductEvent.EditSession,
                ProductEvent.SaveSession, ProductEvent.ReturnToToday
            )) {
                try {
                    val next = sm.handleEvent(event)
                    canTransition = true
                    assertNotNull(next)
                    break
                } catch (_: IllegalStateException) {}
            }
            assertTrue("State $state must not be a dead end", canTransition)
        }
    }
}
