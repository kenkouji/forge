package com.forge.domain.engine

import android.content.Context
import android.util.Log
import com.forge.data.local.dao.DailyActivityDao
import com.forge.data.local.entity.DailyActivityEntity
import com.forge.domain.repository.DailyActivitySummary
import com.forge.domain.repository.HealthConnectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HealthFreshnessState(
    val steps: Int = 0,
    val activeCalories: Int = 0,
    val totalCalories: Int = 0,
    val isCaloriesMeasured: Boolean = false,
    val hasHealthConnectSync: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val freshnessLabel: String = "No sync yet"
)

class HealthConnectPipeline(
    private val context: Context,
    private val dailyActivityDao: DailyActivityDao,
    private val healthConnectRepository: HealthConnectRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "HealthConnectPipeline"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /**
     * Observes today's real activity with freshness semantics.
     * Guaranteed no fake real-time seconds.
     */
    fun observeTodayActivity(): Flow<HealthFreshnessState> {
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        return dailyActivityDao.getActivityForDate(todayStr).map { entity ->
            if (entity != null) {
                HealthFreshnessState(
                    steps = entity.steps,
                    activeCalories = entity.activeCalories,
                    totalCalories = entity.totalCalories,
                    isCaloriesMeasured = entity.isCaloriesMeasured,
                    hasHealthConnectSync = entity.hasHealthConnectSync,
                    lastSyncTimestamp = entity.lastSyncTimestamp,
                    freshnessLabel = formatFreshness(entity.lastSyncTimestamp)
                )
            } else {
                HealthFreshnessState(
                    freshnessLabel = "Awaiting sync"
                )
            }
        }
    }

    /**
     * Passive sync: fetches from Health Connect provider, writes to Room.
     * Does not invent data if Health Connect is unavailable or empty.
     */
    suspend fun syncNow(): Result<DailyActivitySummary> {
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        return try {
            val isAvail = healthConnectRepository.isAvailable()
            if (!isAvail) {
                ensureDayRowExists(todayStr, hasSync = false)
                return Result.failure(IllegalStateException("Health Connect not available"))
            }

            val summary = healthConnectRepository.getTodayActivitySummary()
            val now = System.currentTimeMillis()

            val existing = dailyActivityDao.getActivityForDateSync(todayStr)
            val updated = DailyActivityEntity(
                date = todayStr,
                steps = summary.steps.toInt(),
                activeCalories = summary.activeCaloriesKcal.toInt(),
                totalCalories = (existing?.totalCalories ?: 0).coerceAtLeast(summary.activeCaloriesKcal.toInt()),
                distanceMeters = existing?.distanceMeters ?: 0.0,
                isCaloriesMeasured = summary.activeCaloriesKcal > 0,
                hasHealthConnectSync = summary.hasPermissions,
                lastSyncTimestamp = now,
                updatedAt = now
            )
            dailyActivityDao.insertOrUpdate(updated)
            Log.d(TAG, "Successfully synced Health Connect for $todayStr: ${summary.steps} steps, ${summary.activeCaloriesKcal} kcal")
            Result.success(summary)
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing Health Connect data", e)
            ensureDayRowExists(todayStr, hasSync = false)
            Result.failure(e)
        }
    }

    /**
     * Handles midnight date rollover: ensures an empty/clean row exists for the new day.
     */
    suspend fun checkMidnightRollover() {
        val todayStr = LocalDate.now().format(DATE_FORMATTER)
        ensureDayRowExists(todayStr, hasSync = false)
    }

    private suspend fun ensureDayRowExists(dateStr: String, hasSync: Boolean) {
        val existing = dailyActivityDao.getActivityForDateSync(dateStr)
        if (existing == null) {
            dailyActivityDao.insertOrUpdate(
                DailyActivityEntity(
                    date = dateStr,
                    steps = 0,
                    activeCalories = 0,
                    totalCalories = 0,
                    distanceMeters = 0.0,
                    isCaloriesMeasured = false,
                    hasHealthConnectSync = hasSync,
                    lastSyncTimestamp = 0L,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun formatFreshness(timestamp: Long): String {
        if (timestamp <= 0L) return "Not synced today"
        val elapsedMs = System.currentTimeMillis() - timestamp
        val minutes = elapsedMs / (60 * 1000L)
        return when {
            minutes < 1 -> "Synced just now"
            minutes < 60 -> "Synced ${minutes}m ago"
            else -> {
                val hours = minutes / 60
                "Synced ${hours}h ago"
            }
        }
    }
}
