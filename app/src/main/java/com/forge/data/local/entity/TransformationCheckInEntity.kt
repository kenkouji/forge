package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transformation_checkins")
data class TransformationCheckInEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "week_number")
    val weekNumber: Int,
    val date: String, // "YYYY-MM-DD"
    @ColumnInfo(name = "front_encrypted_path")
    val frontEncryptedPath: String? = null,
    @ColumnInfo(name = "side_encrypted_path")
    val sideEncryptedPath: String? = null,
    @ColumnInfo(name = "back_encrypted_path")
    val backEncryptedPath: String? = null,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

val TransformationCheckInEntity.viewAngle: String
    get() = when {
        frontEncryptedPath != null -> "Front"
        sideEncryptedPath != null -> "Side"
        backEncryptedPath != null -> "Back"
        else -> "Milestone"
    }

val TransformationCheckInEntity.timestamp: Long
    get() = createdAt
