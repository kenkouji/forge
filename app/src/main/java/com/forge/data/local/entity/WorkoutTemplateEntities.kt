package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val focus: String,
    val version: Int = 1,
    @ColumnInfo(name = "target_muscles")
    val targetMuscles: String = "",
    @ColumnInfo(name = "estimated_duration_min")
    val estimatedDurationMin: Int = 50,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("template_id"),
        Index("exercise_id")
    ]
)
data class TemplateExerciseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "template_id")
    val templateId: String,
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "target_sets")
    val targetSets: Int,
    @ColumnInfo(name = "target_reps_min")
    val targetRepsMin: Int,
    @ColumnInfo(name = "target_reps_max")
    val targetRepsMax: Int,
    @ColumnInfo(name = "target_rir")
    val targetRir: Int = 2,
    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Double? = null,
    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int = 90,
    @ColumnInfo(name = "is_warmup")
    val isWarmup: Boolean = false,
    @ColumnInfo(name = "is_drop_set")
    val isDropSet: Boolean = false,
    val notes: String? = null
)
