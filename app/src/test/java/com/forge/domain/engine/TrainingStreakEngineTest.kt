package com.forge.domain.engine

import com.forge.data.local.entity.TrainingScheduleEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TrainingStreakEngineTest {

    private val defaultSchedule = listOf(
        TrainingScheduleEntity(dayOfWeek = 1, isTrainingDay = true, focus = "Push"), // Monday
        TrainingScheduleEntity(dayOfWeek = 2, isTrainingDay = true, focus = "Pull"), // Tuesday
        TrainingScheduleEntity(dayOfWeek = 3, isTrainingDay = false, focus = "Rest"), // Wednesday (Rest)
        TrainingScheduleEntity(dayOfWeek = 4, isTrainingDay = true, focus = "Legs"), // Thursday
        TrainingScheduleEntity(dayOfWeek = 5, isTrainingDay = true, focus = "Upper"), // Friday
        TrainingScheduleEntity(dayOfWeek = 6, isTrainingDay = false, focus = "Rest"), // Saturday (Rest)
        TrainingScheduleEntity(dayOfWeek = 7, isTrainingDay = false, focus = "Rest")  // Sunday (Rest)
    )

    private fun createSession(date: LocalDate): WorkoutSessionEntity {
        val epoch = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return WorkoutSessionEntity(
            id = "sess_${date}",
            name = "Session",
            startTime = epoch,
            endTime = epoch + 3600000L,
            durationSeconds = 3600,
            status = "COMPLETED"
        )
    }

    @Test
    fun testRestDaysDoNotBreakStreak() {
        // Today is Thursday (Day 4)
        // User completed Monday (Day 1) and Tuesday (Day 2).
        // Wednesday (Day 3) was a scheduled rest day.
        // The streak should NOT be broken by Wednesday!
        val monday = LocalDate.of(2026, 9, 14) // Monday
        val tuesday = LocalDate.of(2026, 9, 15) // Tuesday
        val wednesday = LocalDate.of(2026, 9, 16) // Wednesday (Rest)
        val thursday = LocalDate.of(2026, 9, 17) // Thursday

        val sessions = listOf(
            createSession(monday),
            createSession(tuesday)
        )

        // Streak evaluated on Wednesday (scheduled rest)
        val wedResult = TrainingStreakEngine.calculateStreak(
            today = wednesday,
            schedules = defaultSchedule,
            completedSessions = sessions
        )
        // Streak is preserved through rest day
        assertTrue(wedResult.currentStreakDays >= 2)
    }

    @Test
    fun testMissedTrainingDayBreaksStreak() {
        // User trained on Monday, but missed Tuesday (scheduled training day).
        // On Thursday, streak should be 0 or reset.
        val monday = LocalDate.of(2026, 9, 14) // Monday (trained)
        val tuesday = LocalDate.of(2026, 9, 15) // Tuesday (missed scheduled training day)
        val thursday = LocalDate.of(2026, 9, 17) // Thursday

        val sessions = listOf(
            createSession(monday)
        )

        val result = TrainingStreakEngine.calculateStreak(
            today = thursday,
            schedules = defaultSchedule,
            completedSessions = sessions
        )

        // Tuesday was missed, so current streak leading up to today is broken
        assertEquals(0, result.currentStreakDays)
    }
}
