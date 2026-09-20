package com.forge.data.local

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.entity.EquipmentEntity
import com.forge.data.local.entity.ExerciseAliasEntity
import com.forge.data.local.entity.ExerciseAttributeEntity
import com.forge.data.local.entity.ExerciseEntity
import com.forge.data.local.entity.ExerciseEquipmentEntity
import com.forge.data.local.entity.ExerciseFamilyEntity
import com.forge.data.local.entity.ExerciseFamilyMemberEntity
import com.forge.data.local.entity.ExerciseMuscleEntity
import com.forge.data.local.entity.MuscleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object DatabaseSeedLoader {
    private const val TAG = "FORGE_SeedLoader"
    private const val SEED_FILE = "seed/exercises_seed.json"

    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    private val _lastSeedDurationMs = MutableStateFlow<Long>(-1)
    val lastSeedDurationMs: StateFlow<Long> = _lastSeedDurationMs.asStateFlow()

    suspend fun seedDatabaseIfEmpty(context: Context, exerciseDao: ExerciseDao) {
        withContext(Dispatchers.IO) {
            val count = exerciseDao.getExerciseCount()
            val popularCount = exerciseDao.getPopularExercisesCount()

            // If canonical exercises and popular rankings are already populated, skip
            if (count >= 800 && popularCount > 0) {
                Log.i(TAG, "Database already populated ($count exercises, $popularCount popular). Skipping seed.")
                return@withContext
            }

            if (count >= 800 && popularCount == 0) {
                Log.i(TAG, "Backfilling popular exercise metadata for existing database...")
                try {
                    val jsonString = context.assets.open(SEED_FILE).use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
                    }
                    val root = JSONObject(jsonString)
                    val exercisesArray = root.getJSONArray("exercises")
                    for (i in 0 until exercisesArray.length()) {
                        val obj = exercisesArray.getJSONObject(i)
                        val isPopular = obj.optBoolean("is_popular", false)
                        if (isPopular) {
                            val id = obj.getString("id")
                            val rank = obj.optInt("popularity_rank", 999)
                            val youtubeId = if (obj.has("youtube_video_id") && !obj.isNull("youtube_video_id")) obj.getString("youtube_video_id") else null
                            exerciseDao.updateExercisePopularity(id, true, rank, youtubeId)
                        }
                    }
                    Log.i(TAG, "Successfully backfilled popular exercises!")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed backfilling popular exercises", e)
                }
                return@withContext
            }


            _isSeeding.value = true
            val startTime = SystemClock.elapsedRealtime()
            Log.i(TAG, "Starting asynchronous database seeding from $SEED_FILE...")

            try {
                val jsonString = context.assets.open(SEED_FILE).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
                }

                val root = JSONObject(jsonString)

                // 1. Muscles
                val musclesArray = root.getJSONArray("muscles")
                val muscles = ArrayList<MuscleEntity>(musclesArray.length())
                for (i in 0 until musclesArray.length()) {
                    val obj = musclesArray.getJSONObject(i)
                    muscles.add(
                        MuscleEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            bodyPart = obj.getString("body_part")
                        )
                    )
                }

                // 2. Equipment
                val equipmentArray = root.getJSONArray("equipment")
                val equipment = ArrayList<EquipmentEntity>(equipmentArray.length())
                for (i in 0 until equipmentArray.length()) {
                    val obj = equipmentArray.getJSONObject(i)
                    equipment.add(
                        EquipmentEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name")
                        )
                    )
                }

                // 3. Exercise Families
                val familiesArray = root.getJSONArray("exercise_families")
                val families = ArrayList<ExerciseFamilyEntity>(familiesArray.length())
                for (i in 0 until familiesArray.length()) {
                    val obj = familiesArray.getJSONObject(i)
                    families.add(
                        ExerciseFamilyEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            primaryPattern = obj.optString("primary_pattern", "UNKNOWN")
                        )
                    )
                }

                // 4. Exercises
                val exercisesArray = root.getJSONArray("exercises")
                val exercises = ArrayList<ExerciseEntity>(exercisesArray.length())
                for (i in 0 until exercisesArray.length()) {
                    val obj = exercisesArray.getJSONObject(i)
                    exercises.add(
                        ExerciseEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            canonicalName = obj.optString("canonical_name", obj.getString("name")),
                            movementPattern = obj.optString("forge_movement_pattern", "UNKNOWN"),
                            mechanic = obj.optString("source_mechanic", ""),
                            forceType = obj.optString("source_force", ""),
                            experienceLevel = obj.optString("source_level", ""),
                            instructions = obj.optString("instructions", ""),
                            formCues = obj.optString("form_cues", ""),
                            commonMistakes = obj.optString("common_mistakes", ""),
                            youtubeVideoId = if (obj.isNull("youtube_video_id")) null else obj.optString("youtube_video_id"),
                            isCustom = obj.optBoolean("is_custom", false),
                            source = obj.optString("source", "free_exercise_db"),
                            sourceId = if (obj.isNull("source_id")) null else obj.optString("source_id"),
                            sourceCategory = if (obj.isNull("source_category")) null else obj.optString("source_category"),
                            sourceForce = if (obj.isNull("source_force")) null else obj.optString("source_force"),
                            sourceLevel = if (obj.isNull("source_level")) null else obj.optString("source_level"),
                            sourceMechanic = if (obj.isNull("source_mechanic")) null else obj.optString("source_mechanic"),
                            sourceEquipment = if (obj.isNull("source_equipment")) null else obj.optString("source_equipment"),
                            forgeMovementPattern = obj.optString("forge_movement_pattern", "UNKNOWN"),
                            forgeExerciseFamilyId = if (obj.isNull("forge_exercise_family_id")) null else obj.optString("forge_exercise_family_id"),
                            searchTokens = obj.optString("search_tokens", ""),
                            license = obj.optString("license", "The Unlicense"),
                            isPopular = obj.optBoolean("is_popular", false),
                            popularityRank = obj.optInt("popularity_rank", 9999),
                            isFavorite = obj.optBoolean("is_favorite", false),
                            createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                        )
                    )
                }

                // 5. Exercise Muscles
                val emArray = root.getJSONArray("exercise_muscles")
                val exerciseMuscles = ArrayList<ExerciseMuscleEntity>(emArray.length())
                for (i in 0 until emArray.length()) {
                    val obj = emArray.getJSONObject(i)
                    exerciseMuscles.add(
                        ExerciseMuscleEntity(
                            exerciseId = obj.getString("exercise_id"),
                            muscleId = obj.getString("muscle_id"),
                            role = obj.getString("role"),
                            isForgeDerived = obj.optBoolean("is_forge_derived", false)
                        )
                    )
                }

                // 6. Exercise Equipment
                val eeArray = root.getJSONArray("exercise_equipment")
                val exerciseEquipment = ArrayList<ExerciseEquipmentEntity>(eeArray.length())
                for (i in 0 until eeArray.length()) {
                    val obj = eeArray.getJSONObject(i)
                    exerciseEquipment.add(
                        ExerciseEquipmentEntity(
                            exerciseId = obj.getString("exercise_id"),
                            equipmentId = obj.getString("equipment_id"),
                            isPrimary = obj.optBoolean("is_primary", true)
                        )
                    )
                }

                // 7. Exercise Attributes
                val attrArray = root.getJSONArray("exercise_attributes")
                val exerciseAttributes = ArrayList<ExerciseAttributeEntity>(attrArray.length())
                for (i in 0 until attrArray.length()) {
                    val obj = attrArray.getJSONObject(i)
                    exerciseAttributes.add(
                        ExerciseAttributeEntity(
                            exerciseId = obj.getString("exercise_id"),
                            attribute = obj.getString("attribute")
                        )
                    )
                }

                // 8. Exercise Family Members
                val efmArray = root.getJSONArray("exercise_family_members")
                val familyMembers = ArrayList<ExerciseFamilyMemberEntity>(efmArray.length())
                for (i in 0 until efmArray.length()) {
                    val obj = efmArray.getJSONObject(i)
                    familyMembers.add(
                        ExerciseFamilyMemberEntity(
                            familyId = obj.getString("family_id"),
                            exerciseId = obj.getString("exercise_id"),
                            isCanonicalLead = obj.optBoolean("is_canonical_lead", false)
                        )
                    )
                }

                // 9. Exercise Aliases
                val aliasesArray = root.getJSONArray("exercise_aliases")
                val aliases = ArrayList<ExerciseAliasEntity>(aliasesArray.length())
                for (i in 0 until aliasesArray.length()) {
                    val obj = aliasesArray.getJSONObject(i)
                    aliases.add(
                        ExerciseAliasEntity(
                            exerciseId = obj.getString("exercise_id"),
                            alias = obj.getString("alias"),
                            isForgeDerived = obj.optBoolean("is_forge_derived", true)
                        )
                    )
                }

                // Execute batch inserts in an atomic Room transaction
                exerciseDao.seedRelationalDatabase(
                    muscles = muscles,
                    equipment = equipment,
                    families = families,
                    exercises = exercises,
                    exerciseMuscles = exerciseMuscles,
                    exerciseEquipment = exerciseEquipment,
                    exerciseAttributes = exerciseAttributes,
                    exerciseFamilyMembers = familyMembers,
                    aliases = aliases
                )

                val duration = SystemClock.elapsedRealtime() - startTime
                _lastSeedDurationMs.value = duration
                Log.i(
                    TAG,
                    "Database seeding completed successfully in ${duration}ms: " +
                        "${exercises.size} exercises, ${muscles.size} muscles, ${equipment.size} equipment, " +
                        "${families.size} families, ${exerciseMuscles.size} muscle mappings, " +
                        "${exerciseEquipment.size} equipment mappings, ${aliases.size} aliases."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Database seeding failed", e)
            } finally {
                _isSeeding.value = false
            }
        }
    }
}
