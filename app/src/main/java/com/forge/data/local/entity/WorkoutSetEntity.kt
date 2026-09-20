package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["exercise_id"]),
        Index(value = ["session_id", "set_order"])
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    @ColumnInfo(name = "set_order")
    val setOrder: Int,

    @ColumnInfo(name = "set_type")
    val setType: String, // "WARMUP", "NORMAL", "DROPSET", "FAILURE", "MYOREP"

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "reps")
    val reps: Int,

    @ColumnInfo(name = "rpe")
    val rpe: Double? = null,

    @ColumnInfo(name = "rir")
    val rir: Int? = null,

    @ColumnInfo(name = "tempo")
    val tempo: String? = null,

    @ColumnInfo(name = "rest_seconds_taken")
    val restSecondsTaken: Int? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "is_personal_record")
    val isPersonalRecord: Boolean = false,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)
