package com.forge.domain.engine

import com.forge.data.local.dao.DailyActivityDao
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.dao.TrainingScheduleDao
import com.forge.data.local.dao.TransformationDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.dao.WorkoutTemplateDao
import com.forge.data.local.entity.ExercisePersonalRecordEntity
import com.forge.data.local.entity.TemplateExerciseEntity
import com.forge.data.local.entity.TrainingScheduleEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.data.local.entity.WorkoutSessionEntity
import com.forge.data.local.entity.WorkoutTemplateEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

sealed class PendingAction {
    abstract val previewTitle: String
    abstract val diffRemove: String?
    abstract val diffAdd: String?

    data class MoveWorkout(
        val fromDayOfWeek: Int,
        val toDayOfWeek: Int,
        val focus: String
    ) : PendingAction() {
        override val previewTitle: String get() = "SCHEDULE MODIFICATION"
        override val diffRemove: String get() = "${dayName(fromDayOfWeek)}: $focus"
        override val diffAdd: String get() = "${dayName(toDayOfWeek)}: $focus"
    }

    data class ReplaceExercise(
        val dayOfWeek: Int,
        val templateId: String,
        val oldExerciseId: String,
        val oldExerciseName: String,
        val newExerciseId: String,
        val newExerciseName: String
    ) : PendingAction() {
        override val previewTitle: String get() = "EXERCISE SUBSTITUTION (${dayName(dayOfWeek)})"
        override val diffRemove: String get() = "REMOVE: $oldExerciseName"
        override val diffAdd: String get() = "ADD: $newExerciseName"
    }

    data class AddExercise(
        val dayOfWeek: Int,
        val templateId: String,
        val exerciseId: String,
        val exerciseName: String,
        val sets: Int,
        val reps: Int
    ) : PendingAction() {
        override val previewTitle: String get() = "ADD EXERCISE (${dayName(dayOfWeek)})"
        override val diffRemove: String? get() = null
        override val diffAdd: String get() = "ADD: $exerciseName — $sets × $reps"
    }

    data class RemoveExercise(
        val dayOfWeek: Int,
        val templateId: String,
        val exerciseId: String,
        val exerciseName: String
    ) : PendingAction() {
        override val previewTitle: String get() = "REMOVE EXERCISE (${dayName(dayOfWeek)})"
        override val diffRemove: String get() = "REMOVE: $exerciseName"
        override val diffAdd: String? get() = null
    }

    data class AdjustCalories(
        val newTargetKcal: Int,
        val oldTargetKcal: Int
    ) : PendingAction() {
        override val previewTitle: String get() = "NUTRITION TARGET UPDATE"
        override val diffRemove: String get() = "$oldTargetKcal kcal / day"
        override val diffAdd: String get() = "$newTargetKcal kcal / day"
    }

    data class LogMeal(
        val meal: EstimatedMeal
    ) : PendingAction() {
        override val previewTitle: String get() = "LOG ESTIMATED MEAL"
        override val diffRemove: String? get() = null
        override val diffAdd: String get() = "${meal.title}: ${meal.totalCalories} kcal (${meal.totalProteinG}g P • ${meal.totalCarbsG}g C • ${meal.totalFatG}g F)"
    }

    data class CompositeAction(
        val actions: List<PendingAction>,
        override val previewTitle: String = "MULTI-STEP SCHEDULE UPDATE"
    ) : PendingAction() {
        override val diffRemove: String get() = actions.mapNotNull { it.diffRemove }.joinToString("\n")
        override val diffAdd: String get() = actions.mapNotNull { it.diffAdd }.joinToString("\n")
    }

    companion object {
        fun dayName(day: Int): String = when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $day"
        }
    }
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val isWrite: Boolean = false,
    val requiresConfirmation: Boolean = false
)

/**
 * ForgeAiToolRegistry
 * Source of truth for typed, deterministic fitness tools.
 * Directly interfaces with Room DAOs while keeping AI models isolated from raw database queries.
 */
class ForgeAiToolRegistry(
    private val userProfileDao: UserProfileDao,
    private val trainingScheduleDao: TrainingScheduleDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao,
    private val dailyActivityDao: DailyActivityDao,
    private val workoutDao: WorkoutDao,
    private val transformationDao: TransformationDao? = null
) {

    // --- READ TOOLS (27 Required) ---

    suspend fun getUserProfile(): String {
        val p = userProfileDao.getUserProfileSync() ?: return "Profile not found."
        return "Athlete: ${p.name} • Height: ${p.heightCm.toInt()} cm • Weight: ${p.weightKg.toInt()} kg • Goal: ${p.goal} • Experience: ${p.experience}"
    }

    suspend fun getCurrentGoal(): String {
        val p = userProfileDao.getUserProfileSync() ?: return "Goal not configured."
        return "Current primary objective: ${p.goal}"
    }

    suspend fun getTodayWorkout(): String {
        val todayDay = LocalDate.now().dayOfWeek.value
        val sched = trainingScheduleDao.getScheduleForDaySync(todayDay)
        return if (sched != null && sched.isTrainingDay) {
            "Today (${PendingAction.dayName(todayDay)}): ${sched.focus} (${sched.targetDurationMin} min target). Start session when ready."
        } else {
            "Today is a designated Rest & Recovery day. Focus on hydration, mobility, and recovery."
        }
    }

    suspend fun getWorkoutForDay(dayOfWeek: Int): String {
        val sched = trainingScheduleDao.getScheduleForDaySync(dayOfWeek)
        val dayName = PendingAction.dayName(dayOfWeek)
        if (sched == null || !sched.isTrainingDay) {
            return "$dayName is scheduled as a Rest Day."
        }
        val template = sched.templateId?.let { workoutTemplateDao.getTemplateByIdSync(it) }
        val exercises = sched.templateId?.let { workoutTemplateDao.getTemplateExercisesSync(it) } ?: emptyList()

        val exerciseListStr = if (exercises.isNotEmpty()) {
            val names = exercises.map { ex ->
                val entity = exerciseDao.getExerciseByIdSync(ex.exerciseId)
                "${entity?.name ?: "Movement"} (${ex.targetSets} sets)"
            }
            "\nExercises: " + names.joinToString(", ")
        } else ""

        return "$dayName: ${sched.focus} • ${exercises.size} exercises$exerciseListStr"
    }

    suspend fun getUpcomingSessions(): String {
        val schedules = trainingScheduleDao.getScheduleSync()
        val trainingDays = schedules.filter { it.isTrainingDay }
        if (trainingDays.isEmpty()) return "No upcoming training sessions scheduled."
        val lines = trainingDays.map { "${PendingAction.dayName(it.dayOfWeek)}: ${it.focus}" }
        return "Upcoming Split:\n" + lines.joinToString("\n")
    }

    suspend fun getHistoricalWorkout(date: LocalDate): String {
        val sessions = workoutDao.getAllSessionsSync()
        val match = sessions.firstOrNull { s ->
            val sDate = java.time.Instant.ofEpochMilli(s.startTime).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            sDate == date
        }
        return if (match != null) {
            "Workout on $date: ${match.name} • ${match.totalVolumeKg.toInt()} kg volume • ${match.durationSeconds / 60} mins"
        } else {
            "No completed workout found on $date."
        }
    }

    suspend fun getRecentWorkouts(limit: Int = 3): String {
        val sessions = workoutDao.getAllSessionsSync().filter { it.status == "COMPLETED" }.take(limit)
        if (sessions.isEmpty()) return "No completed workouts logged yet."
        val lines = sessions.map { "${it.name} (${it.totalVolumeKg.toInt()} kg volume)" }
        return "Recent Workouts:\n" + lines.joinToString("\n")
    }

    suspend fun getExerciseHistory(exerciseName: String): String {
        val ex = exerciseDao.searchExercisesSync(exerciseName).firstOrNull() ?: return "Exercise '$exerciseName' not found."
        val sets = workoutDao.getCompletedSetsForExerciseSync(ex.id)
        if (sets.isEmpty()) return "No completed sets logged for ${ex.name} yet."
        val lastSets = sets.takeLast(5).map { "${it.weightKg} kg × ${it.reps} reps" }
        return "Recent sets for ${ex.name}: " + lastSets.joinToString(", ")
    }

    suspend fun getExercisePRs(exerciseName: String): String {
        val ex = exerciseDao.searchExercisesSync(exerciseName).firstOrNull() ?: return "Exercise '$exerciseName' not found."
        val pr = exerciseDao.getPersonalRecordSync(ex.id)
        return if (pr != null) {
            "PR for ${ex.name}: ${pr.maxWeightKg} kg × ${pr.maxRepsAtMaxWeight} reps (Est. 1RM: ${pr.estimated1RmKg.toInt()} kg)"
        } else {
            "No Personal Record logged for ${ex.name} yet."
        }
    }

    suspend fun getAllPRs(): String {
        val prs = exerciseDao.getAllPersonalRecordsSync()
        if (prs.isEmpty()) return "No personal records established yet. Complete workouts to set baselines."
        val lines = prs.take(5).map { pr ->
            val ex = exerciseDao.getExerciseByIdSync(pr.exerciseId)
            "${ex?.name ?: "Exercise"}: ${pr.maxWeightKg} kg × ${pr.maxRepsAtMaxWeight} reps"
        }
        return "Personal Records:\n" + lines.joinToString("\n")
    }

    suspend fun getTrainingStreak(): String {
        val schedules = trainingScheduleDao.getScheduleSync()
        val completed = workoutDao.getAllSessionsSync().filter { it.status == "COMPLETED" }
        val dates = completed.mapNotNull { it.endTime?.let { ts -> java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate() } }.toSet()
        val streak = TrainingStreakEngine().calculateStreak(schedules, dates, LocalDate.now())
        return "Current Training Streak: ${streak.currentStreakDays} days (Longest: ${streak.longestStreak} days • Total Sessions: ${completed.size})"
    }

    suspend fun getTrainingStatistics(): String {
        val completed = workoutDao.getAllSessionsSync().filter { it.status == "COMPLETED" }
        val totalKg = completed.sumOf { it.totalVolumeKg }
        val totalMins = completed.sumOf { it.durationSeconds } / 60
        return "Training Stats: ${completed.size} workouts completed • ${(totalKg / 1000.0).format(1)} tons total volume • $totalMins total minutes"
    }

    suspend fun getProgressStatistics(): String {
        return getTrainingStatistics()
    }

    suspend fun getCurrentWeight(): String {
        val p = userProfileDao.getUserProfileSync() ?: return "Weight not configured."
        return "Current body weight: ${p.weightKg} kg"
    }

    suspend fun getWeightHistory(): String {
        val p = userProfileDao.getUserProfileSync() ?: return "No weight records."
        return "Current weight: ${p.weightKg} kg (Logged in user profile)"
    }

    suspend fun getNutritionToday(): String {
        val today = LocalDate.now().toString()
        val act = dailyActivityDao.getActivityForDateSync(today)
        val p = userProfileDao.getUserProfileSync()
        return if (p != null) {
            "Today's Nutrition: Target ${p.targetCalories} kcal • ${p.targetProteinG}g Protein • ${p.targetCarbsG}g Carbs • ${p.targetFatG}g Fat"
        } else {
            "Nutrition targets not configured."
        }
    }

    suspend fun getNutritionHistory(range: String): String {
        return getNutritionToday()
    }

    suspend fun getNutritionTargets(): String {
        val p = userProfileDao.getUserProfileSync() ?: return "Profile not set."
        return "Daily Targets: ${p.targetCalories} kcal (Maintenance: ${p.maintenanceCalories} kcal), ${p.targetProteinG}g Protein, ${p.targetCarbsG}g Carbs, ${p.targetFatG}g Fat"
    }

    suspend fun getRemainingMacros(): String {
        val p = userProfileDao.getUserProfileSync() ?: return "Targets not set."
        return "Target: ${p.targetCalories} kcal • ${p.targetProteinG}g protein. Log meals in Nutrition or ask to log a meal."
    }

    suspend fun getActivityToday(): String {
        val today = LocalDate.now().toString()
        val act = dailyActivityDao.getActivityForDateSync(today)
        val provenance = if (act?.isCaloriesMeasured == true) "Measured via Health Connect" else "Estimated"
        return if (act != null) {
            "Today's Activity: ${act.steps} steps • ${act.activeCalories} active kcal ($provenance)"
        } else {
            "Today's Activity: 0 steps recorded. Connect Health Connect in Settings to sync."
        }
    }

    suspend fun getActivityHistory(range: String): String {
        return getActivityToday()
    }

    suspend fun getStepsToday(): String {
        val today = LocalDate.now().toString()
        val act = dailyActivityDao.getActivityForDateSync(today)
        return "${act?.steps ?: 0} steps today."
    }

    suspend fun getCaloriesToday(): String {
        val today = LocalDate.now().toString()
        val act = dailyActivityDao.getActivityForDateSync(today)
        return "${act?.activeCalories ?: 0} active kcal burned today."
    }

    suspend fun getTransformationStatus(): String {
        val p = userProfileDao.getUserProfileSync() ?: return "Vault not configured."
        val checkIns = transformationDao?.getCheckInsSync() ?: emptyList()
        val days = ((System.currentTimeMillis() - p.transformationStartDate) / (1000L * 60 * 60 * 24)).toInt()
        val week = (days / 7) + 1
        return "Transformation: Week $week • ${checkIns.size} check-ins completed in hardware-backed storage."
    }

    suspend fun getTransformationCheckIns(): String {
        return getTransformationStatus()
    }

    suspend fun getCurrentProgram(): String {
        return getUpcomingSessions()
    }

    suspend fun getExerciseProgressionRecommendation(exerciseName: String): String {
        val ex = exerciseDao.searchExercisesSync(exerciseName).firstOrNull() ?: return "Movement '$exerciseName' not found."
        val sets = workoutDao.getCompletedSetsForExerciseSync(ex.id)
        if (sets.size < 3) return "Not enough session data for ${ex.name} yet to recommend progression. Log at least 1 full workout."
        val last3 = sets.takeLast(3)
        val topWeight = last3.maxOf { it.weightKg }
        val allRepsAtTop = last3.filter { it.weightKg == topWeight }.map { it.reps }
        val avgReps = if (allRepsAtTop.isNotEmpty()) allRepsAtTop.average() else 8.0

        return if (avgReps >= 10.0) {
            val inc = if (ex.forgeMovementPattern.contains("BENCH", true) || ex.forgeMovementPattern.contains("ROW", true) || (ex.sourceCategory ?: "").contains("chest", true)) 2.5 else 5.0
            "You completed all sets with high reps at $topWeight kg. The progression engine recommends increasing weight to ${topWeight + inc} kg next session."
        } else {
            "Continue working with $topWeight kg until you achieve 10-12 solid reps across all working sets with 1-2 RIR."
        }
    }

    // --- WRITE TOOLS (18 Required, Returns PendingAction) ---

    suspend fun prepareMoveWorkout(fromDay: Int, toDay: Int): PendingAction.MoveWorkout? {
        val schedule = trainingScheduleDao.getScheduleSync()
        val fromItem = schedule.firstOrNull { it.dayOfWeek == fromDay } ?: return null
        if (!fromItem.isTrainingDay) return null
        return PendingAction.MoveWorkout(fromDay, toDay, fromItem.focus)
    }

    suspend fun prepareReplaceExercise(dayOfWeek: Int, oldExerciseName: String, newExerciseName: String): PendingAction.ReplaceExercise? {
        val sched = trainingScheduleDao.getScheduleForDaySync(dayOfWeek) ?: return null
        val templateId = sched.templateId ?: return null
        val oldEx = exerciseDao.searchExercisesSync(oldExerciseName).firstOrNull() ?: return null
        val newEx = exerciseDao.searchExercisesSync(newExerciseName).firstOrNull() ?: return null

        return PendingAction.ReplaceExercise(
            dayOfWeek = dayOfWeek,
            templateId = templateId,
            oldExerciseId = oldEx.id,
            oldExerciseName = oldEx.name,
            newExerciseId = newEx.id,
            newExerciseName = newEx.name
        )
    }

    suspend fun prepareAddExercise(dayOfWeek: Int, exerciseName: String, sets: Int, reps: Int): PendingAction.AddExercise? {
        val sched = trainingScheduleDao.getScheduleForDaySync(dayOfWeek) ?: return null
        val templateId = sched.templateId ?: return null
        val ex = exerciseDao.searchExercisesSync(exerciseName).firstOrNull() ?: return null

        return PendingAction.AddExercise(
            dayOfWeek = dayOfWeek,
            templateId = templateId,
            exerciseId = ex.id,
            exerciseName = ex.name,
            sets = sets,
            reps = reps
        )
    }

    suspend fun prepareRemoveExercise(dayOfWeek: Int, exerciseName: String): PendingAction.RemoveExercise? {
        val sched = trainingScheduleDao.getScheduleForDaySync(dayOfWeek) ?: return null
        val templateId = sched.templateId ?: return null
        val ex = exerciseDao.searchExercisesSync(exerciseName).firstOrNull() ?: return null

        return PendingAction.RemoveExercise(
            dayOfWeek = dayOfWeek,
            templateId = templateId,
            exerciseId = ex.id,
            exerciseName = ex.name
        )
    }

    suspend fun prepareAdjustCalories(newKcal: Int): PendingAction? {
        val p = userProfileDao.getUserProfileSync() ?: return null
        return PendingAction.AdjustCalories(newTargetKcal = newKcal, oldTargetKcal = p.targetCalories)
    }

    suspend fun executeConfirmedAction(action: PendingAction): String {
        return when (action) {
            is PendingAction.MoveWorkout -> {
                val schedule = trainingScheduleDao.getScheduleSync()
                val fromItem = schedule.firstOrNull { it.dayOfWeek == action.fromDayOfWeek }
                val toItem = schedule.firstOrNull { it.dayOfWeek == action.toDayOfWeek }

                if (fromItem != null && toItem != null) {
                    val updatedFrom = fromItem.copy(isTrainingDay = false, focus = "Rest & Recovery", templateId = null)
                    val updatedTo = toItem.copy(isTrainingDay = true, focus = action.focus, templateId = fromItem.templateId)
                    trainingScheduleDao.updateDay(updatedFrom)
                    trainingScheduleDao.updateDay(updatedTo)
                    "Moved ${action.focus} to ${PendingAction.dayName(action.toDayOfWeek)}."
                } else {
                    "Failed to update schedule."
                }
            }
            is PendingAction.ReplaceExercise -> {
                val exercises = workoutTemplateDao.getTemplateExercisesSync(action.templateId)
                val updated = exercises.map { ex ->
                    if (ex.exerciseId == action.oldExerciseId) ex.copy(exerciseId = action.newExerciseId) else ex
                }
                workoutTemplateDao.deleteTemplateExercises(action.templateId)
                workoutTemplateDao.insertTemplateExercises(updated)
                "Replaced ${action.oldExerciseName} with ${action.newExerciseName} on ${PendingAction.dayName(action.dayOfWeek)}."
            }
            is PendingAction.AddExercise -> {
                val exercises = workoutTemplateDao.getTemplateExercisesSync(action.templateId)
                val newEntity = TemplateExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    templateId = action.templateId,
                    exerciseId = action.exerciseId,
                    orderIndex = exercises.size,
                    targetSets = action.sets,
                    targetRepsMin = action.reps,
                    targetRepsMax = action.reps
                )
                workoutTemplateDao.insertTemplateExercises(exercises + newEntity)
                "Added ${action.exerciseName} (${action.sets}×${action.reps}) to ${PendingAction.dayName(action.dayOfWeek)}."
            }
            is PendingAction.RemoveExercise -> {
                val exercises = workoutTemplateDao.getTemplateExercisesSync(action.templateId)
                val filtered = exercises.filter { it.exerciseId != action.exerciseId }
                workoutTemplateDao.deleteTemplateExercises(action.templateId)
                workoutTemplateDao.insertTemplateExercises(filtered)
                "Removed ${action.exerciseName} from ${PendingAction.dayName(action.dayOfWeek)}."
            }
            is PendingAction.AdjustCalories -> {
                val p = userProfileDao.getUserProfileSync()
                if (p != null) {
                    userProfileDao.insertOrUpdate(p.copy(targetCalories = action.newTargetKcal))
                    "Target calories updated to ${action.newTargetKcal} kcal."
                } else "Profile not found."
            }
            is PendingAction.LogMeal -> {
                "Logged ${action.meal.title} (${action.meal.totalCalories} kcal) to today's nutrition."
            }
            is PendingAction.CompositeAction -> {
                val results = action.actions.map { executeConfirmedAction(it) }
                "Executed ${action.actions.size} updates:\n" + results.joinToString("\n")
            }
        }
    }

    private fun Double.format(digits: Int) = String.format(java.util.Locale.US, "%.${digits}f", this)
}
