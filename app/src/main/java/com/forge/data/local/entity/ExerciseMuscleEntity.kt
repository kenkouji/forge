package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exercise_id", "muscle_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MuscleEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["muscle_id"])
    ]
)
data class ExerciseMuscleEntity(
    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    @ColumnInfo(name = "muscle_id")
    val muscleId: String,

    @ColumnInfo(name = "role")
    val role: String, // PRIMARY, SECONDARY, STABILIZER

    @ColumnInfo(name = "is_forge_derived")
    val isForgeDerived: Boolean = false
)
