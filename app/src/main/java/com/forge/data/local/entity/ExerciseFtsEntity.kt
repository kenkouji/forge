package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

@Fts4(
    contentEntity = ExerciseEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61
)
@Entity(tableName = "exercises_fts")
data class ExerciseFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "canonical_name")
    val canonicalName: String,

    @ColumnInfo(name = "forge_movement_pattern")
    val forgeMovementPattern: String,

    @ColumnInfo(name = "search_tokens")
    val searchTokens: String,

    @ColumnInfo(name = "instructions")
    val instructions: String
)
