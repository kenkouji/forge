package com.forge.domain.engine

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

data class EphemeralConversationContext(
    val lastReferencedDayOfWeek: Int? = null,
    val lastReferencedExerciseName: String? = null,
    val lastReferencedDate: LocalDate? = null,
    val lastPendingAction: PendingAction? = null
)

object ConversationContextResolver {

    fun resolveDayOfWeek(text: String, context: EphemeralConversationContext): Int {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("today") -> LocalDate.now().dayOfWeek.value
            lower.contains("tomorrow") -> LocalDate.now().plusDays(1).dayOfWeek.value
            lower.contains("yesterday") -> LocalDate.now().minusDays(1).dayOfWeek.value
            lower.contains("monday") || lower.contains("mon") -> 1
            lower.contains("tuesday") || lower.contains("tue") -> 2
            lower.contains("wednesday") || lower.contains("wed") -> 3
            lower.contains("thursday") || lower.contains("thu") -> 4
            lower.contains("friday") || lower.contains("fri") -> 5
            lower.contains("saturday") || lower.contains("sat") -> 6
            lower.contains("sunday") || lower.contains("sun") -> 7
            else -> context.lastReferencedDayOfWeek ?: LocalDate.now().dayOfWeek.value
        }
    }

    fun resolveDate(text: String): LocalDate {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("yesterday") -> LocalDate.now().minusDays(1)
            lower.contains("tomorrow") -> LocalDate.now().plusDays(1)
            else -> LocalDate.now()
        }
    }

    fun parseMultiStepMutation(
        text: String,
        toolRegistry: ForgeAiToolRegistry,
        context: EphemeralConversationContext
    ): List<PendingAction> {
        val actions = mutableListOf<PendingAction>()
        val lower = text.lowercase(Locale.ROOT)

        // Sub-clause 1: Move workout (e.g. "Move Friday to Saturday")
        val moveMatch = Regex("(?i)move\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s+(?:workout|session)?\\s*to\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)").find(lower)
        if (moveMatch != null) {
            val fromDay = resolveDayOfWeek(moveMatch.groupValues[1], context)
            val toDay = resolveDayOfWeek(moveMatch.groupValues[2], context)
            // Note: Suspend call will be wrapped in executor
        }

        return actions
    }
}
