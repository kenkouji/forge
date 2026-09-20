package com.forge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // e.g. "barbell_bench_press"

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "canonical_name")
    val canonicalName: String,

    @ColumnInfo(name = "movement_pattern")
    val movementPattern: String = "UNKNOWN", // legacy v1 column, kept for backward compatibility

    @ColumnInfo(name = "mechanic")
    val mechanic: String = "", // legacy v1 column

    @ColumnInfo(name = "force_type")
    val forceType: String = "", // legacy v1 column

    @ColumnInfo(name = "experience_level")
    val experienceLevel: String = "", // legacy v1 column

    @ColumnInfo(name = "instructions")
    val instructions: String = "",

    @ColumnInfo(name = "form_cues")
    val formCues: String = "",

    @ColumnInfo(name = "common_mistakes")
    val commonMistakes: String = "",

    @ColumnInfo(name = "youtube_video_id")
    val youtubeVideoId: String? = null,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false,

    @ColumnInfo(name = "source")
    val source: String = "free_exercise_db",

    @ColumnInfo(name = "source_id")
    val sourceId: String? = null,

    @ColumnInfo(name = "source_category")
    val sourceCategory: String? = null,

    @ColumnInfo(name = "source_force")
    val sourceForce: String? = null,

    @ColumnInfo(name = "source_level")
    val sourceLevel: String? = null,

    @ColumnInfo(name = "source_mechanic")
    val sourceMechanic: String? = null,

    @ColumnInfo(name = "source_equipment")
    val sourceEquipment: String? = null,

    @ColumnInfo(name = "forge_movement_pattern")
    val forgeMovementPattern: String = "UNKNOWN", // Default to UNKNOWN per Correction #6

    @ColumnInfo(name = "forge_exercise_family_id")
    val forgeExerciseFamilyId: String? = null,

    @ColumnInfo(name = "search_tokens")
    val searchTokens: String = "",

    @ColumnInfo(name = "license")
    val license: String = "The Unlicense",

    @ColumnInfo(name = "is_popular")
    val isPopular: Boolean = false,

    @ColumnInfo(name = "popularity_rank")
    val popularityRank: Int = 9999,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
