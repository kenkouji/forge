package com.forge.domain.engine

import com.forge.data.local.entity.TrainingScheduleEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StreakEvaluation(
    val currentStreak: Int,
    val longestStreak: Int,
    val isStreakActive: Boolean,
    val lastCompletedDate: LocalDate?
) {
    val currentStreakDays: Int get() = currentStreak
}

/**
 * TrainingStreakEngine
 * Computes user training streak based on scheduled session compliance:
 * - A rest day does NOT break the streak.
 * - A scheduled training day that is skipped (date is in the past with no completed workout) DOES break the streak.
 * - Bonus sessions completed on planned rest days do not hurt the streak.
 */
class TrainingStreakEngine {

    fun calculateStreak(
        schedule: List<TrainingScheduleEntity>,
        completedWorkoutDates: Set<LocalDate>,
        today: LocalDate = LocalDate.now()
    ): StreakEvaluation {
        if (completedWorkoutDates.isEmpty()) {
            return StreakEvaluation(
                currentStreak = 0,
                longestStreak = 0,
                isStreakActive = false,
                lastCompletedDate = null
            )
        }

        val scheduleMap = schedule.associateBy { it.dayOfWeek } // 1 = Monday ... 7 = Sunday
        val sortedDates = completedWorkoutDates.sorted()
        val earliestDate = sortedDates.first()
        val latestCompleted = sortedDates.last()

        var currentStreak = 0
        var maxStreak = 0
        var tempStreak = 0

        // Iterate forward from earliest date to yesterday
        var cursor = earliestDate
        while (!cursor.isAfter(today)) {
            val dayOfWeek = cursor.dayOfWeek.value // 1 = Mon ... 7 = Sun
            val isScheduledTraining = scheduleMap[dayOfWeek]?.isTrainingDay == true
            val hasCompleted = completedWorkoutDates.contains(cursor)

            if (hasCompleted) {
                tempStreak++
                if (tempStreak > maxStreak) {
                    maxStreak = tempStreak
                }
            } else if (isScheduledTraining && cursor.isBefore(today)) {
                // Scheduled day passed without completion -> streak breaks
                tempStreak = 0
            } else {
                // Rest day or today pending -> streak is preserved intact
            }
            cursor = cursor.plusDays(1)
        }

        currentStreak = tempStreak
        return StreakEvaluation(
            currentStreak = currentStreak,
            longestStreak = maxStreak,
            isStreakActive = currentStreak > 0,
            lastCompletedDate = latestCompleted
        )
    }

    companion object {
        fun calculateStreak(
            schedule: List<TrainingScheduleEntity>,
            completedWorkoutDates: Set<LocalDate>,
            today: LocalDate = LocalDate.now()
        ): StreakEvaluation = TrainingStreakEngine().calculateStreak(schedule, completedWorkoutDates, today)

        fun calculateStreak(
            today: LocalDate = LocalDate.now(),
            schedules: List<TrainingScheduleEntity>,
            completedSessions: List<com.forge.data.local.entity.WorkoutSessionEntity>
        ): StreakEvaluation {
            val dates = completedSessions.map { session ->
                java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }.toSet()
            return calculateStreak(schedules, dates, today)
        }
    }
}
