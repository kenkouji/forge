package com.forge.data.repository

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.forge.domain.repository.DailyActivitySummary
import com.forge.domain.repository.HealthConnectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectRepositoryImpl(
    private val context: Context
) : HealthConnectRepository {

    private val TAG = "FORGE_HealthConnect"

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    private val healthConnectClient by lazy {
        try {
            if (isAvailable()) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to instantiate HealthConnectClient", e)
            null
        }
    }

    override fun isAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.w(TAG, "Health Connect SDK status check failed", e)
            false
        }
    }

    override suspend fun checkPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Health Connect permissions", e)
            false
        }
    }

    override suspend fun getTodayActivitySummary(): DailyActivitySummary {
        val client = healthConnectClient
        if (client == null || !isAvailable()) {
            return DailyActivitySummary(isHealthConnectAvailable = false, hasPermissions = false)
        }

        return try {
            val hasPerms = checkPermissions()
            if (!hasPerms) {
                return DailyActivitySummary(isHealthConnectAvailable = true, hasPermissions = false)
            }

            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now().atStartOfDay(zone).toInstant()
            val now = Instant.now()

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val calories = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

            DailyActivitySummary(
                steps = steps,
                activeCaloriesKcal = calories,
                isHealthConnectAvailable = true,
                hasPermissions = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to aggregate daily activity from Health Connect", e)
            DailyActivitySummary(
                isHealthConnectAvailable = true,
                hasPermissions = false
            )
        }
    }

    override fun observeTodayActivity(): Flow<DailyActivitySummary> = flow {
        emit(getTodayActivitySummary())
    }
}
