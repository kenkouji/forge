package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey
    val date: String, // "YYYY-MM-DD" local time
    val steps: Int = 0,
    @ColumnInfo(name = "active_calories")
    val activeCalories: Int = 0,
    @ColumnInfo(name = "total_calories")
    val totalCalories: Int = 0,
    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double = 0.0,
    @ColumnInfo(name = "is_calories_measured")
    val isCaloriesMeasured: Boolean = false,
    @ColumnInfo(name = "has_health_connect_sync")
    val hasHealthConnectSync: Boolean = false,
    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
