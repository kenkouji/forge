package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_attributes",
    primaryKeys = ["exercise_id", "attribute"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["attribute"])
    ]
)
data class ExerciseAttributeEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    @ColumnInfo(name = "attribute")
    val attribute: String // INCLINE, DECLINE, FLAT, UNILATERAL, BILATERAL, CLOSE_GRIP, WIDE_GRIP, DEFICIT, PAUSED, ASSISTED
)
