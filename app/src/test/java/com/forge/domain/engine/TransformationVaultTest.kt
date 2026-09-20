package com.forge.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TransformationVaultTest {

    @Test
    fun testMilestoneCalculationAnchoredToUserStartDate() {
        // User starts transformation on a Wednesday (Sept 16, 2026 14:00:00 GMT)
        val startDateMillis = 1789567200000L

        // Day 0 (Same day, 2 hours later) -> Must be Week 1
        val sameDayMillis = startDateMillis + TimeUnit.HOURS.toMillis(2)
        assertEquals(1, TransformationVaultManager.calculateTransformationWeek(startDateMillis, sameDayMillis))

        // Day 3 (Saturday, cross-calendar boundary) -> Still Week 1
        val day3Millis = startDateMillis + TimeUnit.DAYS.toMillis(3)
        assertEquals(1, TransformationVaultManager.calculateTransformationWeek(startDateMillis, day3Millis))

        // Day 6 (Next Tuesday, exactly 6 days after start) -> Still Week 1
        val day6Millis = startDateMillis + TimeUnit.DAYS.toMillis(6)
        assertEquals(1, TransformationVaultManager.calculateTransformationWeek(startDateMillis, day6Millis))

        // Day 7 (Next Wednesday, exactly 1 week after start) -> Transitions to Week 2
        val day7Millis = startDateMillis + TimeUnit.DAYS.toMillis(7)
        assertEquals(2, TransformationVaultManager.calculateTransformationWeek(startDateMillis, day7Millis))

        // Day 14 (Two weeks after start) -> Transitions to Week 3
        val day14Millis = startDateMillis + TimeUnit.DAYS.toMillis(14)
        assertEquals(3, TransformationVaultManager.calculateTransformationWeek(startDateMillis, day14Millis))

        // Negative elapsed time / future start date -> Fallback to Week 1
        val pastDateMillis = startDateMillis - TimeUnit.DAYS.toMillis(5)
        assertEquals(1, TransformationVaultManager.calculateTransformationWeek(startDateMillis, pastDateMillis))
    }

    @Test
    fun testSaltedPasswordVerification() {
        val password = "ForgeSecurePassword#2026"
        val wrongPassword = "WrongPassword#2026"

        val (hash, salt) = TransformationVaultManager.createPasswordHashAndSalt(password)

        assertTrue(TransformationVaultManager.verifyPassword(password, hash, salt))
        assertFalse(TransformationVaultManager.verifyPassword(wrongPassword, hash, salt))

        // Generating a second hash with fresh salt produces distinct hash and salt
        val (hash2, salt2) = TransformationVaultManager.createPasswordHashAndSalt(password)
        assertNotEquals(salt, salt2)
        assertNotEquals(hash, hash2)
        assertTrue(TransformationVaultManager.verifyPassword(password, hash2, salt2))
    }
}
