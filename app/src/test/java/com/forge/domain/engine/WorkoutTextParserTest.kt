package com.forge.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTextParserTest {

    @Test
    fun testZeroWeightInventionRule() {
        // When weight is not specified, it MUST remain null (never invented as 0kg or bodyweight)
        val input = "Pull-ups 3x10 RIR 2"
        val result = WorkoutTextParser.parse(input)

        assertEquals(1, result.exercises.size)
        val exercise = result.exercises[0]
        assertEquals(3, exercise.sets.size)
        exercise.sets.forEach { set ->
            assertEquals(10, set.reps)
            assertNull("Weight must be null when unspecified; never invent zero or default", set.weightKg)
            assertEquals(2, set.rir)
        }
    }

    @Test
    fun testFullExerciseSetWeightRirRpeParsing() {
        val input = """
            Barbell Bench Press: 3x8 80kg RIR 2, 90s rest
            Incline Dumbbell Press: 1x12 28kg RPE 8
            Dips: 3x15
        """.trimIndent()

        val result = WorkoutTextParser.parse(input)
        assertEquals(3, result.exercises.size)

        val bench = result.exercises[0]
        assertEquals("Barbell Bench Press", bench.exerciseName)
        assertEquals(3, bench.sets.size)
        assertEquals(80.0, bench.sets[0].weightKg!!, 0.001)
        assertEquals(8, bench.sets[0].reps)
        assertEquals(2, bench.sets[0].rir)
        assertEquals(90, bench.sets[0].restSeconds)

        val incline = result.exercises[1]
        assertEquals("Incline Dumbbell Press", incline.exerciseName)
        assertEquals(1, incline.sets.size)
        assertEquals(28.0, incline.sets[0].weightKg!!, 0.001)
        assertEquals(12, incline.sets[0].reps)
        assertEquals(8.0, incline.sets[0].rpe!!, 0.001)

        val dips = result.exercises[2]
        assertEquals("Dips", dips.exerciseName)
        assertEquals(3, dips.sets.size)
        assertNull(dips.sets[0].weightKg)
        assertEquals(15, dips.sets[0].reps)
    }

    @Test
    fun testWarmupAndDropsetParsing() {
        val input = """
            Deadlift:
            1x5 60kg warmup
            1x3 100kg warmup
            3x5 140kg working
            1x8 100kg drop
        """.trimIndent()

        val result = WorkoutTextParser.parse(input)
        assertEquals(1, result.exercises.size)
        val dl = result.exercises[0]
        assertEquals(6, dl.sets.size) // 1 + 1 + 3 + 1 = 6 sets

        assertEquals("WARMUP", dl.sets[0].setType)
        assertEquals("WARMUP", dl.sets[1].setType)
        assertEquals("NORMAL", dl.sets[2].setType)
        assertEquals("DROP_SET", dl.sets[5].setType)
    }
}
