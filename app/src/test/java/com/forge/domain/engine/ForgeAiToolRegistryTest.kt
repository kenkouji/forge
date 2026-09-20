package com.forge.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeAiToolRegistryTest {

    @Test
    fun dayName_mapsDayIntsCorrectly() {
        assertEquals("Monday", PendingAction.dayName(1))
        assertEquals("Tuesday", PendingAction.dayName(2))
        assertEquals("Wednesday", PendingAction.dayName(3))
        assertEquals("Thursday", PendingAction.dayName(4))
        assertEquals("Friday", PendingAction.dayName(5))
        assertEquals("Saturday", PendingAction.dayName(6))
        assertEquals("Sunday", PendingAction.dayName(7))
    }

    @Test
    fun moveWorkout_formatsDiffAccurately() {
        val action = PendingAction.MoveWorkout(
            fromDayOfWeek = 5,
            toDayOfWeek = 6,
            focus = "Hypertrophy Push"
        )
        assertEquals("SCHEDULE MODIFICATION", action.previewTitle)
        assertEquals("Friday: Hypertrophy Push", action.diffRemove)
        assertEquals("Saturday: Hypertrophy Push", action.diffAdd)
    }

    @Test
    fun replaceExercise_formatsDiffAccurately() {
        val action = PendingAction.ReplaceExercise(
            dayOfWeek = 2,
            templateId = "tmpl-1",
            oldExerciseId = "ex-1",
            oldExerciseName = "Barbell Bench Press",
            newExerciseId = "ex-2",
            newExerciseName = "Dumbbell Incline Press"
        )
        assertEquals("EXERCISE SUBSTITUTION (Tuesday)", action.previewTitle)
        assertEquals("REMOVE: Barbell Bench Press", action.diffRemove)
        assertEquals("ADD: Dumbbell Incline Press", action.diffAdd)
    }

    @Test
    fun addExercise_formatsDiffAccurately() {
        val action = PendingAction.AddExercise(
            dayOfWeek = 4,
            templateId = "tmpl-1",
            exerciseId = "ex-pullup",
            exerciseName = "Weighted Pull-Up",
            sets = 4,
            reps = 8
        )
        assertEquals("ADD EXERCISE (Thursday)", action.previewTitle)
        assertNull(action.diffRemove)
        assertEquals("ADD: Weighted Pull-Up — 4 × 8", action.diffAdd)
    }

    @Test
    fun removeExercise_formatsDiffAccurately() {
        val action = PendingAction.RemoveExercise(
            dayOfWeek = 3,
            templateId = "tmpl-1",
            exerciseId = "ex-dip",
            exerciseName = "Chest Dips"
        )
        assertEquals("REMOVE EXERCISE (Wednesday)", action.previewTitle)
        assertEquals("REMOVE: Chest Dips", action.diffRemove)
        assertNull(action.diffAdd)
    }

    @Test
    fun adjustCalories_formatsDiffAccurately() {
        val action = PendingAction.AdjustCalories(
            newTargetKcal = 2800,
            oldTargetKcal = 2500
        )
        assertEquals("NUTRITION TARGET UPDATE", action.previewTitle)
        assertEquals("2500 kcal / day", action.diffRemove)
        assertEquals("2800 kcal / day", action.diffAdd)
    }

    @Test
    fun compositeAction_aggregatesChildDiffs() {
        val action1 = PendingAction.MoveWorkout(1, 2, "Upper Body")
        val action2 = PendingAction.AdjustCalories(3000, 2700)
        val composite = PendingAction.CompositeAction(listOf(action1, action2))

        assertTrue(composite.diffRemove.contains("Monday: Upper Body"))
        assertTrue(composite.diffRemove.contains("2700 kcal / day"))
        assertTrue(composite.diffAdd.contains("Tuesday: Upper Body"))
        assertTrue(composite.diffAdd.contains("3000 kcal / day"))
    }
}
