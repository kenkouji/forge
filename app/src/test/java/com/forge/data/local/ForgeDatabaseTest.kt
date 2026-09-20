package com.forge.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.forge.data.local.dao.ExerciseDao
import com.forge.data.local.entity.ExerciseAliasEntity
import com.forge.data.local.entity.ExerciseEntity
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
class ForgeDatabaseTest {

    private lateinit var database: ForgeDatabase
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ForgeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exerciseDao = database.exerciseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun database_opensAndInsertsExerciseSuccessfully() = runBlocking {
        val exercise = ExerciseEntity(
            id = "barbell_bench_press",
            name = "Barbell Bench Press",
            canonicalName = "Barbell Bench Press",
            movementPattern = "PUSH",
            mechanic = "COMPOUND",
            forceType = "PUSH",
            experienceLevel = "INTERMEDIATE",
            instructions = "Lie on flat bench, grip bar slightly wider than shoulder width.",
            formCues = "Retract scapulae, touch lower sternum.",
            commonMistakes = "Flaring elbows 90 degrees."
        )

        exerciseDao.insertExercises(listOf(exercise))

        val retrieved = exerciseDao.getExerciseById("barbell_bench_press").first()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.name).isEqualTo("Barbell Bench Press")
        assertThat(retrieved?.movementPattern).isEqualTo("PUSH")
    }

    @Test
    fun database_ftsAndAliasSearchReturnsCorrectExercise() = runBlocking {
        val exercise = ExerciseEntity(
            id = "barbell_bench_press",
            name = "Barbell Bench Press",
            canonicalName = "Barbell Bench Press",
            movementPattern = "PUSH",
            mechanic = "COMPOUND",
            forceType = "PUSH",
            experienceLevel = "INTERMEDIATE",
            instructions = "Lie on flat bench.",
            formCues = "Retract scapulae.",
            commonMistakes = "Flaring elbows."
        )
        val alias = ExerciseAliasEntity(
            exerciseId = "barbell_bench_press",
            alias = "BB Bench"
        )

        exerciseDao.insertExercises(listOf(exercise))
        exerciseDao.insertAliases(listOf(alias))

        // Search by prefix
        val prefixResults = exerciseDao.searchExercises("benc*", "benc").first()
        assertThat(prefixResults).hasSize(1)
        assertThat(prefixResults[0].id).isEqualTo("barbell_bench_press")

        // Search by alias
        val aliasResults = exerciseDao.searchExercises("BB*", "BB Bench").first()
        assertThat(aliasResults).hasSize(1)
        assertThat(aliasResults[0].id).isEqualTo("barbell_bench_press")
    }
}
