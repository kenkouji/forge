package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "reduce_motion")
    val reduceMotion: Boolean = false,
    @ColumnInfo(name = "particles_enabled")
    val particlesEnabled: Boolean = true,
    @ColumnInfo(name = "haptics_enabled")
    val hapticsEnabled: Boolean = true,
    @ColumnInfo(name = "health_connect_enabled")
    val healthConnectEnabled: Boolean = false,
    @ColumnInfo(name = "auto_lock_vault_on_background")
    val autoLockVaultOnBackground: Boolean = true,
    @ColumnInfo(name = "notif_workout_reminders")
    val notifWorkoutReminders: Boolean = true,
    @ColumnInfo(name = "notif_streak_reminders")
    val notifStreakReminders: Boolean = true,
    @ColumnInfo(name = "notif_motivation")
    val notifMotivation: Boolean = true,
    @ColumnInfo(name = "notif_pre_workout_alerts")
    val notifPreWorkoutAlerts: Boolean = true,
    @ColumnInfo(name = "notif_post_workout_congrats")
    val notifPostWorkoutCongrats: Boolean = true,
    @ColumnInfo(name = "notif_nutrition_reminders")
    val notifNutritionReminders: Boolean = false,
    @ColumnInfo(name = "notif_hydration_reminders")
    val notifHydrationReminders: Boolean = false,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

val AppSettingsEntity.ambientParticlesEnabled: Boolean get() = particlesEnabled
val AppSettingsEntity.workoutRemindersEnabled: Boolean get() = notifWorkoutReminders
