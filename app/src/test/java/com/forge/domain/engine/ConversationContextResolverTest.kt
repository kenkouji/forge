package com.forge.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ConversationContextResolverTest {

    @Test
    fun resolveDayOfWeek_explicitDayName_returnsCorrectInt() {
        val context = EphemeralConversationContext()
        assertEquals(1, ConversationContextResolver.resolveDayOfWeek("What do I train on Monday?", context))
        assertEquals(2, ConversationContextResolver.resolveDayOfWeek("Show me Tuesday's schedule", context))
        assertEquals(3, ConversationContextResolver.resolveDayOfWeek("Is Wednesday rest?", context))
        assertEquals(4, ConversationContextResolver.resolveDayOfWeek("thursday workout", context))
        assertEquals(5, ConversationContextResolver.resolveDayOfWeek("friday push session", context))
        assertEquals(6, ConversationContextResolver.resolveDayOfWeek("saturday legs", context))
        assertEquals(7, ConversationContextResolver.resolveDayOfWeek("sunday mobility", context))
    }

    @Test
    fun resolveDayOfWeek_relativeTerms_resolvesAgainstToday() {
        val today = LocalDate.now().dayOfWeek.value
        val context = EphemeralConversationContext()
        assertEquals(today, ConversationContextResolver.resolveDayOfWeek("workout today", context))

        val tomorrow = LocalDate.now().plusDays(1).dayOfWeek.value
        assertEquals(tomorrow, ConversationContextResolver.resolveDayOfWeek("what is scheduled tomorrow?", context))
    }

    @Test
    fun resolveDayOfWeek_ambiguousPronoun_usesLastReferencedContext() {
        val context = EphemeralConversationContext(lastReferencedDayOfWeek = 4) // Thursday
        assertEquals(4, ConversationContextResolver.resolveDayOfWeek("how many sets in that session?", context))
    }

    @Test
    fun resolveDate_relativeWords_resolvesCorrectLocalDate() {
        val today = LocalDate.now()
        assertEquals(today, ConversationContextResolver.resolveDate("how many calories today"))
        assertEquals(today.minusDays(1), ConversationContextResolver.resolveDate("steps yesterday"))
        assertEquals(today.plusDays(1), ConversationContextResolver.resolveDate("schedule tomorrow"))
    }
}
