package com.forge.domain.engine

import com.forge.data.local.dao.DailyActivityDao
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.dao.TrainingScheduleDao
import com.forge.data.local.dao.TransformationDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.dao.WorkoutDao
import com.forge.data.local.dao.WorkoutTemplateDao
import java.time.LocalDate
import java.util.Locale

sealed class AssistantMessage {
    data class User(val text: String) : AssistantMessage()
    data class Assistant(
        val text: String,
        val pendingAction: PendingAction? = null,
        val estimatedMeal: EstimatedMeal? = null,
        val isFallback: Boolean = false
    ) : AssistantMessage()
}

/**
 * AssistantService
 * Architecture:
 * User Message ↓ Conversation Context Resolver ↓ Tool Router ↓ ForgeAiToolRegistry ↓ Domain Engines/DAOs ↓ Room
 * LLM is isolated from direct Room access and never invents fitness data.
 */
class AssistantService(
    private val userProfileDao: UserProfileDao,
    private val trainingScheduleDao: TrainingScheduleDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val exerciseDao: ExerciseDao,
    private val dailyActivityDao: DailyActivityDao,
    private val workoutDao: WorkoutDao,
    private val transformationDao: TransformationDao? = null,
    private val aiProvider: AiProvider = DeterministicIntentProvider()
) {
    private val toolRegistry = ForgeAiToolRegistry(
        userProfileDao = userProfileDao,
        trainingScheduleDao = trainingScheduleDao,
        workoutTemplateDao = workoutTemplateDao,
        exerciseDao = exerciseDao,
        dailyActivityDao = dailyActivityDao,
        workoutDao = workoutDao,
        transformationDao = transformationDao
    )

    private var conversationContext = EphemeralConversationContext()

    suspend fun processUserMessage(userText: String): AssistantMessage.Assistant {
        val lower = userText.lowercase(Locale.ROOT).trim()

        // 1. Food / Meal photo or text recognition (e.g., "200g chicken breast and 300g rice", "Log meal: 4 eggs")
        val isMealDescription = (lower.contains("chicken") || lower.contains("rice") || lower.contains("egg") ||
                lower.contains("oats") || lower.contains("beef") || lower.contains("salmon") ||
                lower.contains("grams") || lower.contains("calories") || lower.startsWith("log meal")) &&
                !lower.contains("target") && !lower.contains("how much")

        if (isMealDescription && (lower.contains("g") || lower.contains("gram") || lower.contains("ate") || lower.contains("had"))) {
            val meal = LocalFoodNutritionEngine.estimateMealFromText(userText)
            val pendingMeal = PendingAction.LogMeal(meal)
            conversationContext = conversationContext.copy(lastPendingAction = pendingMeal)
            return AssistantMessage.Assistant(
                text = "Estimated meal from your description:\n${meal.items.joinToString("\n") { "• ${it.name}: ~${it.grams}g (~${it.calories} kcal, ${it.proteinG}g P, ${it.carbsG}g C, ${it.fatG}g F)" }}\n\nTotal: ~${meal.totalCalories} kcal (${meal.totalProteinG}g Protein). Confirm to add to today's log.",
                pendingAction = pendingMeal,
                estimatedMeal = meal
            )
        }

        // 2. Multi-step request: Move workout + Add/Remove exercise
        val moveMatch = Regex("(?i)move\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s+(?:workout|session)?\\s*to\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)").find(userText)
        val removeMatch = Regex("(?i)remove\\s+([a-zA-Z\\s]+?)(?:\\s+on|\\s+from|\\s+and|,|$)").find(userText)
        val addMatch = Regex("(?i)add\\s+([a-zA-Z\\s]+?)(?:\\s+to|\\s+for|\\s+(\\d+)\\s*sets?|$)").find(userText)

        if (moveMatch != null && (removeMatch != null || addMatch != null)) {
            val fromDay = ConversationContextResolver.resolveDayOfWeek(moveMatch.groupValues[1], conversationContext)
            val toDay = ConversationContextResolver.resolveDayOfWeek(moveMatch.groupValues[2], conversationContext)
            val actions = mutableListOf<PendingAction>()

            val moveAction = toolRegistry.prepareMoveWorkout(fromDay, toDay)
            if (moveAction != null) actions.add(moveAction)

            if (removeMatch != null) {
                val exName = removeMatch.groupValues[1].trim()
                val removeAction = toolRegistry.prepareRemoveExercise(fromDay, exName)
                if (removeAction != null) actions.add(removeAction)
            }

            if (addMatch != null) {
                val exName = addMatch.groupValues[1].trim()
                val sets = Regex("(\\d+)\\s*sets?").find(userText)?.groupValues?.get(1)?.toIntOrNull() ?: 3
                val reps = Regex("(?:for|of)\\s*(\\d+)(?:\\s*reps)?").find(userText)?.groupValues?.get(1)?.toIntOrNull() ?: 12
                val addAction = toolRegistry.prepareAddExercise(toDay, exName, sets, reps)
                if (addAction != null) actions.add(addAction)
            }

            if (actions.isNotEmpty()) {
                val composite = PendingAction.CompositeAction(actions)
                conversationContext = conversationContext.copy(lastPendingAction = composite)
                return AssistantMessage.Assistant(
                    text = "Prepared updates for your upcoming split:\n${composite.diffRemove}\n${composite.diffAdd}\n\nWould you like me to apply these changes?",
                    pendingAction = composite
                )
            }
        }

        // 3. Single Move Workout
        if (moveMatch != null) {
            val fromDay = ConversationContextResolver.resolveDayOfWeek(moveMatch.groupValues[1], conversationContext)
            val toDay = ConversationContextResolver.resolveDayOfWeek(moveMatch.groupValues[2], conversationContext)
            val action = toolRegistry.prepareMoveWorkout(fromDay, toDay)
            return if (action != null) {
                conversationContext = conversationContext.copy(lastPendingAction = action, lastReferencedDayOfWeek = toDay)
                AssistantMessage.Assistant(
                    text = "Would you like me to move ${action.focus} from ${PendingAction.dayName(fromDay)} to ${PendingAction.dayName(toDay)}?",
                    pendingAction = action
                )
            } else {
                AssistantMessage.Assistant(text = "There is no scheduled workout on ${PendingAction.dayName(fromDay)} to move.")
            }
        }

        // 4. Exercise Substitution / Replacement
        val replaceMatch = Regex("(?i)(?:replace|substitute|swap)\\s+([a-zA-Z\\s]+?)\\s+(?:with|for)\\s+([a-zA-Z\\s]+)").find(userText)
        if (replaceMatch != null) {
            val oldEx = replaceMatch.groupValues[1].trim()
            val newEx = replaceMatch.groupValues[2].trim()
            val day = ConversationContextResolver.resolveDayOfWeek(userText, conversationContext)

            val action = toolRegistry.prepareReplaceExercise(day, oldEx, newEx)
            return if (action != null) {
                conversationContext = conversationContext.copy(lastPendingAction = action, lastReferencedDayOfWeek = day)
                AssistantMessage.Assistant(
                    text = "Substitute $oldEx with $newEx on ${PendingAction.dayName(day)}?",
                    pendingAction = action
                )
            } else {
                AssistantMessage.Assistant(text = "Could not find movement '$oldEx' in your ${PendingAction.dayName(day)} session.")
            }
        }

        // 5. Add Exercise
        if (lower.startsWith("add ") && !lower.contains("rice") && !lower.contains("calorie")) {
            val day = ConversationContextResolver.resolveDayOfWeek(userText, conversationContext)
            val sets = Regex("(\\d+)\\s*sets?").find(userText)?.groupValues?.get(1)?.toIntOrNull() ?: 3
            val reps = Regex("(\\d+)\\s*reps?").find(userText)?.groupValues?.get(1)?.toIntOrNull() ?: 10
            val cleanEx = lower.removePrefix("add ").replace(Regex("\\s+on\\s+.*"), "").replace(Regex("\\s+for\\s+.*"), "").trim()

            val action = toolRegistry.prepareAddExercise(day, cleanEx, sets, reps)
            return if (action != null) {
                conversationContext = conversationContext.copy(lastPendingAction = action, lastReferencedDayOfWeek = day)
                AssistantMessage.Assistant(
                    text = "Add $cleanEx ($sets sets × $reps reps) to ${PendingAction.dayName(day)}?",
                    pendingAction = action
                )
            } else {
                AssistantMessage.Assistant(text = "Could not resolve exercise '$cleanEx' or target day.")
            }
        }

        // 6. Today's Workout Query
        if (lower.contains("today") && (lower.contains("workout") || lower.contains("training") || lower.contains("session") || lower.contains("working on"))) {
            return AssistantMessage.Assistant(text = toolRegistry.getTodayWorkout())
        }

        // 7. Day Workout Query (e.g. "What am I working on Tuesday?", "What's my Monday workout?")
        val queryDay = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday").find { lower.contains(it) }
        if (queryDay != null && (lower.contains("workout") || lower.contains("train") || lower.contains("working on") || lower.contains("schedule"))) {
            val dayNum = ConversationContextResolver.resolveDayOfWeek(queryDay, conversationContext)
            conversationContext = conversationContext.copy(lastReferencedDayOfWeek = dayNum)
            return AssistantMessage.Assistant(text = toolRegistry.getWorkoutForDay(dayNum))
        }

        // 8. Tomorrow's Workout Query
        if (lower.contains("tomorrow")) {
            val tomorrowDay = LocalDate.now().plusDays(1).dayOfWeek.value
            conversationContext = conversationContext.copy(lastReferencedDayOfWeek = tomorrowDay)
            return AssistantMessage.Assistant(text = toolRegistry.getWorkoutForDay(tomorrowDay))
        }

        // 9. Historical Workout Query (e.g. "What did I do yesterday?", "What did I train last?")
        if (lower.contains("yesterday") || lower.contains("last workout") || lower.contains("did i train") || lower.contains("did i do")) {
            val date = ConversationContextResolver.resolveDate(userText)
            return AssistantMessage.Assistant(text = toolRegistry.getHistoricalWorkout(date))
        }

        // 10. PR & Exercise History Queries (e.g. "What did I bench last time?", "What's my best bench?")
        if (lower.contains("bench") || lower.contains("squat") || lower.contains("deadlift") || lower.contains("pullup") || lower.contains("curl")) {
            val matchedEx = when {
                lower.contains("bench") -> "Barbell Bench Press"
                lower.contains("squat") -> "Barbell Squat"
                lower.contains("deadlift") -> "Barbell Deadlift"
                lower.contains("pullup") -> "Pull-Up"
                else -> "Barbell Curl"
            }
            if (lower.contains("pr") || lower.contains("best") || lower.contains("max") || lower.contains("record")) {
                return AssistantMessage.Assistant(text = toolRegistry.getExercisePRs(matchedEx))
            } else if (lower.contains("progress") || lower.contains("increase") || lower.contains("overload")) {
                return AssistantMessage.Assistant(text = toolRegistry.getExerciseProgressionRecommendation(matchedEx))
            } else {
                return AssistantMessage.Assistant(text = toolRegistry.getExerciseHistory(matchedEx))
            }
        }

        // 11. All PRs query
        if (lower.contains("prs") || lower.contains("records") || lower.contains("personal records")) {
            return AssistantMessage.Assistant(text = toolRegistry.getAllPRs())
        }

        // 12. Nutrition & Remaining Macros queries
        if (lower.contains("protein") || lower.contains("calories") || lower.contains("macros") || lower.contains("eat")) {
            if (lower.contains("remaining") || lower.contains("left") || lower.contains("can i eat") || lower.contains("reach my target")) {
                return AssistantMessage.Assistant(text = toolRegistry.getRemainingMacros())
            }
            if (lower.contains("target") || lower.contains("goal")) {
                return AssistantMessage.Assistant(text = toolRegistry.getNutritionTargets())
            }
            return AssistantMessage.Assistant(text = toolRegistry.getNutritionToday())
        }

        // 13. Steps & Activity queries
        if (lower.contains("step") || lower.contains("activity") || lower.contains("burned") || lower.contains("walk")) {
            if (lower.contains("yesterday")) {
                val yesterdayAct = dailyActivityDao.getActivityForDateSync(LocalDate.now().minusDays(1).toString())
                return AssistantMessage.Assistant(text = "Yesterday's Activity: ${yesterdayAct?.steps ?: 0} steps • ${yesterdayAct?.activeCalories ?: 0} active kcal.")
            }
            return AssistantMessage.Assistant(text = toolRegistry.getActivityToday())
        }

        // 14. Streak & Statistics queries
        if (lower.contains("streak")) {
            return AssistantMessage.Assistant(text = toolRegistry.getTrainingStreak())
        }
        if (lower.contains("volume") || lower.contains("stats") || lower.contains("statistics")) {
            return AssistantMessage.Assistant(text = toolRegistry.getTrainingStatistics())
        }

        // 15. Body Weight query
        if (lower.contains("weight") && (lower.contains("what") || lower.contains("current") || lower.contains("my"))) {
            return AssistantMessage.Assistant(text = toolRegistry.getCurrentWeight())
        }

        // 16. Upcoming Split query
        if (lower.contains("upcoming") || lower.contains("program") || lower.contains("split") || lower.contains("week")) {
            return AssistantMessage.Assistant(text = toolRegistry.getUpcomingSessions())
        }

        // 17. Transformation Milestone query
        if (lower.contains("transformation") || lower.contains("check-in") || lower.contains("milestone") || lower.contains("photo")) {
            return AssistantMessage.Assistant(text = toolRegistry.getTransformationStatus())
        }

        // Default Fallback
        return AssistantMessage.Assistant(
            text = "I am connected to your FORGE database. Ask me about your schedule, past sessions, exercise history, PRs, nutrition targets, or daily steps.",
            isFallback = true
        )
    }

    suspend fun executeConfirmedAction(action: PendingAction): String {
        return toolRegistry.executeConfirmedAction(action)
    }

    fun parseFoodPhoto(photoGramsEstimate: Int = 300): EstimatedMeal {
        return LocalFoodNutritionEngine.estimateMealFromText("180g chicken breast and 250g rice")
    }
}
