package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["start_time"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "routine_id")
    val routineId: String? = null,

    @ColumnInfo(name = "name")
    val name: String = "Empty Workout",

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    @ColumnInfo(name = "status")
    val status: String, // "IN_PROGRESS", "COMPLETED", "DISCARDED"

    @ColumnInfo(name = "active_exercise_id")
    val activeExerciseId: String? = null,

    @ColumnInfo(name = "active_set_index")
    val activeSetIndex: Int = 0,

    @ColumnInfo(name = "total_volume_kg")
    val totalVolumeKg: Double = 0.0,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
