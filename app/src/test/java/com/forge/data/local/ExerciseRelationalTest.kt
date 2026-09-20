package com.forge.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import com.forge.data.repository.ExerciseRepositoryImpl
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseRelationalTest {

    private lateinit var db: ForgeDatabase
    private lateinit var dao: ExerciseDao
    private lateinit var repository: ExerciseRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ForgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.exerciseDao()
        repository = ExerciseRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun relationalSeeding_andQueries_workAccurately() = runBlocking {
        // Setup taxonomy
        dao.insertMuscles(
            listOf(
                MuscleEntity("chest", "Chest", "CHEST"),
                MuscleEntity("triceps", "Triceps", "ARMS"),
                MuscleEntity("quadriceps", "Quadriceps", "LEGS")
            )
        )
        dao.insertEquipment(
            listOf(
                EquipmentEntity("barbell", "Barbell"),
                EquipmentEntity("dumbbell", "Dumbbell")
            )
        )
        dao.insertExerciseFamilies(
            listOf(
                ExerciseFamilyEntity(
                    id = "bench_press_family",
                    name = "Bench Press",
                    description = "Horizontal pressing variations",
                    primaryPattern = "HORIZONTAL_PUSH"
                )
            )
        )

        // Insert exercises
        val bench = ExerciseEntity(
            id = "barbell_bench_press",
            name = "Barbell Bench Press",
            canonicalName = "Barbell Bench Press",
            forgeMovementPattern = "HORIZONTAL_PUSH",
            forgeExerciseFamilyId = "bench_press_family",
            source = "free_exercise_db",
            sourceLevel = "intermediate",
            searchTokens = "barbell bench press flat chest push",
            instructions = "Lower barbell to sternum and press."
        )
        val inclineDumbbell = ExerciseEntity(
            id = "incline_dumbbell_press",
            name = "Incline Dumbbell Press",
            canonicalName = "Incline Dumbbell Press",
            forgeMovementPattern = "HORIZONTAL_PUSH",
            forgeExerciseFamilyId = "bench_press_family",
            source = "free_exercise_db",
            sourceLevel = "intermediate",
            searchTokens = "incline dumbbell press chest push",
            instructions = "Press dumbbells at 30 deg incline."
        )
        val squat = ExerciseEntity(
            id = "barbell_squat",
            name = "Barbell Back Squat",
            canonicalName = "Barbell Back Squat",
            forgeMovementPattern = "SQUAT",
            source = "free_exercise_db",
            sourceLevel = "beginner",
            searchTokens = "barbell back squat legs quads glutes",
            instructions = "Squat below parallel."
        )

        dao.insertExercises(listOf(bench, inclineDumbbell, squat))

        // Relational mappings
        dao.insertExerciseMuscles(
            listOf(
                ExerciseMuscleEntity("barbell_bench_press", "chest", "PRIMARY", false),
                ExerciseMuscleEntity("barbell_bench_press", "triceps", "SECONDARY", false),
                ExerciseMuscleEntity("incline_dumbbell_press", "chest", "PRIMARY", false),
                ExerciseMuscleEntity("barbell_squat", "quadriceps", "PRIMARY", false)
            )
        )
        dao.insertExerciseEquipment(
            listOf(
                ExerciseEquipmentEntity("barbell_bench_press", "barbell", true),
                ExerciseEquipmentEntity("incline_dumbbell_press", "dumbbell", true),
                ExerciseEquipmentEntity("barbell_squat", "barbell", true)
            )
        )
        dao.insertExerciseFamilyMembers(
            listOf(
                ExerciseFamilyMemberEntity("bench_press_family", "barbell_bench_press", isCanonicalLead = true),
                ExerciseFamilyMemberEntity("bench_press_family", "incline_dumbbell_press", isCanonicalLead = false)
            )
        )
        dao.insertAliases(
            listOf(
                ExerciseAliasEntity(exerciseId = "barbell_bench_press", alias = "Flat Bench", isForgeDerived = true)
            )
        )

        // 1. Verify getMusclesForExercise
        val musclesForBench = dao.getMusclesForExercise("barbell_bench_press").first()
        assertThat(musclesForBench).hasSize(2)
        assertThat(musclesForBench[0].muscleId).isEqualTo("chest")
        assertThat(musclesForBench[0].role).isEqualTo("PRIMARY")
        assertThat(musclesForBench[1].muscleId).isEqualTo("triceps")
        assertThat(musclesForBench[1].role).isEqualTo("SECONDARY")

        // 2. Verify getEquipmentForExercise
        val eqForBench = dao.getEquipmentForExercise("barbell_bench_press").first()
        assertThat(eqForBench).hasSize(1)
        assertThat(eqForBench[0].id).isEqualTo("barbell")

        // 3. Verify family & family siblings
        val family = dao.getFamilyForExercise("barbell_bench_press").first()
        assertThat(family).isNotNull()
        assertThat(family?.id).isEqualTo("bench_press_family")

        val siblings = dao.getFamilySiblings("bench_press_family", currentExerciseId = "barbell_bench_press").first()
        assertThat(siblings).hasSize(1)
        assertThat(siblings[0].id).isEqualTo("incline_dumbbell_press")

        // 4. Test Search with Relational Filtering (Correction #3)
        // a. Filter by muscle = chest
        val chestExercises = repository.searchAndFilterExercises(muscleId = "chest").first()
        assertThat(chestExercises).hasSize(2)
        assertThat(chestExercises.map { it.id }).containsExactly("barbell_bench_press", "incline_dumbbell_press")

        // b. Combined search: text "Bench" + equipment "barbell"
        val benchBarbell = repository.searchAndFilterExercises(
            query = "Bench",
            equipmentId = "barbell"
        ).first()
        assertThat(benchBarbell).hasSize(1)
        assertThat(benchBarbell[0].id).isEqualTo("barbell_bench_press")

        // c. Filter by movement pattern = SQUAT
        val squats = repository.searchAndFilterExercises(movementPattern = "SQUAT").first()
        assertThat(squats).hasSize(1)
        assertThat(squats[0].id).isEqualTo("barbell_squat")

        // d. Search via Alias "Flat Bench"
        val aliasResults = repository.searchAndFilterExercises(query = "Flat Bench").first()
        assertThat(aliasResults).hasSize(1)
        assertThat(aliasResults[0].id).isEqualTo("barbell_bench_press")
    }

    @Test
    fun customExercise_coexistsWithCanonicalData_andIsSearchable() = runBlocking {
        // Canonical exercise
        val canonical = ExerciseEntity(
            id = "barbell_bench_press",
            name = "Barbell Bench Press",
            canonicalName = "Barbell Bench Press",
            source = "free_exercise_db",
            isCustom = false
        )
        dao.insertExercises(listOf(canonical))

        // Custom exercise creation
        val custom = ExerciseEntity(
            id = "custom_ring_dips",
            name = "Custom Gymnastic Ring Dips",
            canonicalName = "Custom Gymnastic Ring Dips",
            source = "custom",
            license = "User Created",
            forgeMovementPattern = "VERTICAL_PUSH",
            isCustom = true
        )
        dao.insertMuscles(listOf(MuscleEntity("chest", "Chest", "CHEST")))
        dao.insertEquipment(listOf(EquipmentEntity("other", "Other / Specialized")))

        repository.insertCustomExercise(
            exercise = custom,
            primaryMuscleId = "chest",
            equipmentId = "other"
        )

        // Verify both coexist
        val all = repository.getAllExercises().first()
        assertThat(all).hasSize(2)
        assertThat(all.map { it.id }).containsExactly("barbell_bench_press", "custom_ring_dips")

        // Verify custom exercise is searchable
        val search = repository.searchAndFilterExercises(query = "Ring Dips").first()
        assertThat(search).hasSize(1)
        assertThat(search[0].id).isEqualTo("custom_ring_dips")
        assertThat(search[0].isCustom).isTrue()

        // Verify custom muscle mapping
        val customMuscles = repository.getMusclesForExercise("custom_ring_dips").first()
        assertThat(customMuscles).hasSize(1)
        assertThat(customMuscles[0].muscleId).isEqualTo("chest")
    }

    @Test
    fun databaseSeedLoader_loadsFullSeedAssetSuccessfully() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DatabaseSeedLoader.seedDatabaseIfEmpty(context, dao)

        val count = dao.getExerciseCount()
        assertThat(count).isEqualTo(876)

        val duration = DatabaseSeedLoader.lastSeedDurationMs.value
        assertThat(duration).isAtLeast(0)

        // Verify sample canonical record provenance (Correction #4 & #8)
        val bench = dao.getExerciseById("barbell_bench_press___medium_grip").first()
        assertThat(bench).isNotNull()
        assertThat(bench?.source).isEqualTo("free_exercise_db")
        assertThat(bench?.license).isEqualTo("The Unlicense")
        assertThat(bench?.forgeMovementPattern).isEqualTo("HORIZONTAL_PUSH")

        // Verify muscles & equipment populated
        val muscles = dao.getAllMuscles().first()
        assertThat(muscles.size).isAtLeast(20)

        val equipment = dao.getAllEquipment().first()
        assertThat(equipment.size).isAtLeast(10)

        // Verify families populated
        val families = dao.getAllFamilies().first()
        assertThat(families.size).isAtLeast(10)
    }
}
