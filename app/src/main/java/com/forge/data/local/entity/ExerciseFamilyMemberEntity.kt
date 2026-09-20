package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_family_members",
    primaryKeys = ["family_id", "exercise_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseFamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["family_id"],
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
        Index(value = ["exercise_id"])
    ]
)
data class ExerciseFamilyMemberEntity(
    @ColumnInfo(name = "family_id")
    val familyId: String,

    @ColumnInfo(name = "exercise_id")
    val exerciseId: String,

    @ColumnInfo(name = "is_canonical_lead")
    val isCanonicalLead: Boolean = false
)
