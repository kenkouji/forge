package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "",
    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null,
    val goal: String = "",
    val experience: String = "",
    @ColumnInfo(name = "days_per_week")
    val daysPerWeek: Int = 4,
    @ColumnInfo(name = "session_duration_min")
    val sessionDurationMin: Int = 60,
    val equipment: String = "",
    @ColumnInfo(name = "height_cm")
    val heightCm: Float = 175f,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Float = 75f,
    val age: Int? = null,
    val sex: String? = null,
    @ColumnInfo(name = "maintenance_calories")
    val maintenanceCalories: Int = 2400,
    @ColumnInfo(name = "nutrition_goal")
    val nutritionGoal: String = "Maintain",
    @ColumnInfo(name = "target_calories")
    val targetCalories: Int = 2400,
    @ColumnInfo(name = "target_protein_g")
    val targetProteinG: Int = 150,
    @ColumnInfo(name = "target_carbs_g")
    val targetCarbsG: Int = 250,
    @ColumnInfo(name = "target_fat_g")
    val targetFatG: Int = 65,
    @ColumnInfo(name = "transformation_start_date")
    val transformationStartDate: Long = 0L,
    @ColumnInfo(name = "photo_password_hash")
    val photoPasswordHash: String? = null,
    @ColumnInfo(name = "photo_password_salt")
    val photoPasswordSalt: String? = null,
    @ColumnInfo(name = "is_initialized")
    val isInitialized: Boolean = false,
    @ColumnInfo(name = "initialization_step")
    val initializationStep: Int = 1,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

val UserProfileEntity.fitnessGoal: String get() = goal
val UserProfileEntity.experienceLevel: String get() = experience
val UserProfileEntity.targetProteinGrams: Int get() = targetProteinG
val UserProfileEntity.vaultPasswordHash: String? get() = photoPasswordHash
val UserProfileEntity.vaultSalt: String? get() = photoPasswordSalt
