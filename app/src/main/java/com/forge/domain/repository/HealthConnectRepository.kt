package com.forge.domain.repository

import kotlinx.coroutines.flow.Flow

data class DailyActivitySummary(
    val steps: Long = 0,
    val activeCaloriesKcal: Double = 0.0,
    val isHealthConnectAvailable: Boolean = false,
    val hasPermissions: Boolean = false
)

interface HealthConnectRepository {
    fun isAvailable(): Boolean
    suspend fun checkPermissions(): Boolean
    suspend fun getTodayActivitySummary(): DailyActivitySummary
    fun observeTodayActivity(): Flow<DailyActivitySummary>
}
