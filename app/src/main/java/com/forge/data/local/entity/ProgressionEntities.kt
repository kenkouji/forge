package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit log of deterministic progressive overload recommendations.
 * Enables FORGE to explain what it recommended, when, and the rationale.
 */
@Entity(
    tableName = "exercise_progression_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("exercise_id"),
        Index("session_id")
    ]
)
data class ExerciseProgressionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "recommended_weight_kg")
    val recommendedWeightKg: Double,

    @ColumnInfo(name = "recommended_rep_min")
    val recommendedRepMin: Int,

    @ColumnInfo(name = "recommended_rep_max")
    val recommendedRepMax: Int,

    @ColumnInfo(name = "rationale")
    val rationale: String,

    @ColumnInfo(name = "is_deload")
    val isDeload: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Tracked Personal Records for completed, valid sets.
 */
@Entity(
    tableName = "exercise_personal_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExercisePersonalRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    @ColumnInfo(name = "max_weight_kg")
    val maxWeightKg: Double = 0.0,

    @ColumnInfo(name = "max_reps_at_max_weight")
    val maxRepsAtMaxWeight: Int = 0,

    @ColumnInfo(name = "estimated_1rm_kg")
    val estimated1RmKg: Double = 0.0,

    @ColumnInfo(name = "best_set_volume_kg")
    val bestSetVolumeKg: Double = 0.0,

    @ColumnInfo(name = "best_session_volume_kg")
    val bestSessionVolumeKg: Double = 0.0,

    @ColumnInfo(name = "achieved_at")
    val achievedAt: Long = System.currentTimeMillis()
)

/**
 * User nutrition profile and targets.
 */
@Entity(tableName = "user_nutrition_profile")
data class UserNutritionProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = "primary_profile",

    @ColumnInfo(name = "maintenance_calories")
    val maintenanceCalories: Int = 2500,

    @ColumnInfo(name = "is_maintenance_manual")
    val isMaintenanceManual: Boolean = false,

    @ColumnInfo(name = "goal")
    val goal: String = "MAINTENANCE", // CUT, LEAN_BULK, RECOMPOSITION, MAINTENANCE

    @ColumnInfo(name = "target_calories")
    val targetCalories: Int = 2500,

    @ColumnInfo(name = "protein_grams")
    val proteinGrams: Int = 160,

    @ColumnInfo(name = "fat_grams")
    val fatGrams: Int = 70,

    @ColumnInfo(name = "carbs_grams")
    val carbsGrams: Int = 305,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Daily weight log for rolling trend detection.
 */
@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["logged_date"], unique = true)]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "logged_date")
    val loggedDate: String, // YYYY-MM-DD format

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
