package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_schedule")
data class TrainingScheduleEntity(
    @PrimaryKey
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    @ColumnInfo(name = "is_training_day")
    val isTrainingDay: Boolean,
    val focus: String, // e.g. "Chest + Biceps", "Rest"
    @ColumnInfo(name = "template_id")
    val templateId: String? = null,
    @ColumnInfo(name = "target_duration_min")
    val targetDurationMin: Int = 60
)
