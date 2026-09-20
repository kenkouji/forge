package com.forge.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    @Test
    fun database_allMigrations_arrayIsValid() {
        assertThat(ALL_MIGRATIONS).isNotEmpty()
        assertThat(MIGRATION_1_2.startVersion).isEqualTo(1)
        assertThat(MIGRATION_1_2.endVersion).isEqualTo(2)
        assertThat(MIGRATION_2_3.startVersion).isEqualTo(2)
        assertThat(MIGRATION_2_3.endVersion).isEqualTo(3)
        assertThat(MIGRATION_3_4.startVersion).isEqualTo(3)
        assertThat(MIGRATION_3_4.endVersion).isEqualTo(4)
        assertThat(MIGRATION_4_5.startVersion).isEqualTo(4)
        assertThat(MIGRATION_4_5.endVersion).isEqualTo(5)
        assertThat(MIGRATION_5_6.startVersion).isEqualTo(5)
        assertThat(MIGRATION_5_6.endVersion).isEqualTo(6)
    }

    @Test
    fun database_buildsWithAllMigrationsConfigured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ForgeDatabase::class.java)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

        assertThat(db.isOpen).isFalse()
        val version = db.openHelper.readableDatabase.version
        assertThat(version).isEqualTo(6)
        db.close()
    }

    @Test
    fun migration_1_to_2_executesSqlWithoutError() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_v1_migration.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS workout_sessions (
                                id TEXT PRIMARY KEY NOT NULL,
                                routine_id TEXT,
                                start_time INTEGER NOT NULL,
                                end_time INTEGER,
                                status TEXT NOT NULL,
                                active_exercise_id TEXT,
                                active_set_index INTEGER NOT NULL DEFAULT 0,
                                total_volume_kg REAL NOT NULL DEFAULT 0.0,
                                duration_seconds INTEGER NOT NULL DEFAULT 0,
                                notes TEXT,
                                last_updated_at INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = sqliteHelper.writableDatabase

        // Execute MIGRATION_1_2 on the v1 database
        MIGRATION_1_2.migrate(db)

        // Verify that the 'name' column was added and is accessible
        val cursor = db.query("PRAGMA table_info(workout_sessions)")
        var foundNameColumn = false
        while (cursor.moveToNext()) {
            val colIndex = cursor.getColumnIndex("name")
            if (colIndex >= 0 && cursor.getString(colIndex) == "name") {
                foundNameColumn = true
                break
            }
        }
        cursor.close()
        assertThat(foundNameColumn).isTrue()
        db.close()
    }

    @Test
    fun migration_2_to_3_preservesWorkoutData_andCreatesRelationalSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_v2_migration.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // v2 Schema: exercises, exercises_fts, exercise_aliases, workout_sessions, workout_sets
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS exercises (
                                id TEXT PRIMARY KEY NOT NULL,
                                name TEXT NOT NULL,
                                canonical_name TEXT NOT NULL,
                                movement_pattern TEXT NOT NULL,
                                mechanic TEXT NOT NULL,
                                force_type TEXT NOT NULL,
                                experience_level TEXT NOT NULL,
                                instructions TEXT NOT NULL,
                                form_cues TEXT NOT NULL,
                                common_mistakes TEXT NOT NULL,
                                youtube_video_id TEXT,
                                is_custom INTEGER NOT NULL,
                                created_at INTEGER NOT NULL,
                                updated_at INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )

                        db.execSQL(
                            """
                            CREATE VIRTUAL TABLE IF NOT EXISTS exercises_fts USING FTS4(
                                name TEXT NOT NULL,
                                canonical_name TEXT NOT NULL,
                                movement_pattern TEXT NOT NULL,
                                instructions TEXT NOT NULL,
                                tokenize=unicode61,
                                content=exercises
                            )
                            """.trimIndent()
                        )

                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS exercise_aliases (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                exercise_id TEXT NOT NULL,
                                alias TEXT NOT NULL,
                                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )

                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS workout_sessions (
                                id TEXT PRIMARY KEY NOT NULL,
                                name TEXT NOT NULL DEFAULT 'Empty Workout',
                                routine_id TEXT,
                                start_time INTEGER NOT NULL,
                                end_time INTEGER,
                                status TEXT NOT NULL,
                                active_exercise_id TEXT,
                                active_set_index INTEGER NOT NULL DEFAULT 0,
                                total_volume_kg REAL NOT NULL DEFAULT 0.0,
                                duration_seconds INTEGER NOT NULL DEFAULT 0,
                                notes TEXT,
                                last_updated_at INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )

                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS workout_sets (
                                id TEXT PRIMARY KEY NOT NULL,
                                session_id TEXT NOT NULL,
                                exercise_id TEXT NOT NULL,
                                set_order INTEGER NOT NULL,
                                set_type TEXT NOT NULL,
                                weight_kg REAL NOT NULL,
                                reps INTEGER NOT NULL,
                                rpe REAL,
                                rir INTEGER,
                                tempo TEXT,
                                rest_seconds_taken INTEGER,
                                is_completed INTEGER NOT NULL,
                                is_personal_record INTEGER NOT NULL,
                                completed_at INTEGER,
                                FOREIGN KEY(session_id) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                            )
                            """.trimIndent()
                        )

                        // Insert Phase 1 baseline data to verify non-destructive migration (Correction #7)
                        // 1. Canonical Phase 1 exercise
                        db.execSQL(
                            """
                            INSERT INTO exercises (id, name, canonical_name, movement_pattern, mechanic, force_type, experience_level, instructions, form_cues, common_mistakes, is_custom, created_at, updated_at)
                            VALUES ('barbell_bench_press', 'Barbell Bench Press', 'Barbell Bench Press', 'PUSH', 'COMPOUND', 'PUSH', 'INTERMEDIATE', 'Press the bar', '', '', 0, 1000, 1000)
                            """.trimIndent()
                        )
                        // 2. Custom exercise
                        db.execSQL(
                            """
                            INSERT INTO exercises (id, name, canonical_name, movement_pattern, mechanic, force_type, experience_level, instructions, form_cues, common_mistakes, is_custom, created_at, updated_at)
                            VALUES ('custom_cable_crossover', 'Custom Cable Crossover', 'Custom Cable Crossover', 'ISOLATION', 'ISOLATION', 'PUSH', 'BEGINNER', 'Custom instructions', '', '', 1, 2000, 2000)
                            """.trimIndent()
                        )
                        // 3. Existing alias
                        db.execSQL("INSERT INTO exercise_aliases (exercise_id, alias) VALUES ('barbell_bench_press', 'Flat Bench')")

                        // 4. In-progress workout session (crash recovery state)
                        db.execSQL(
                            """
                            INSERT INTO workout_sessions (id, name, start_time, status, active_exercise_id, active_set_index, total_volume_kg, duration_seconds, last_updated_at)
                            VALUES ('active_sess_001', 'Chest & Arms Push', 5000, 'IN_PROGRESS', 'barbell_bench_press', 1, 100.0, 120, 5120)
                            """.trimIndent()
                        )

                        // 5. Logged workout set
                        db.execSQL(
                            """
                            INSERT INTO workout_sets (id, session_id, exercise_id, set_order, set_type, weight_kg, reps, is_completed, is_personal_record, completed_at)
                            VALUES ('set_001', 'active_sess_001', 'barbell_bench_press', 0, 'NORMAL', 100.0, 5, 1, 1, 5060)
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = sqliteHelper.writableDatabase

        // Execute MIGRATION_2_3
        MIGRATION_2_3.migrate(db)

        // 1. Verify exercise_aliases migration and is_forge_derived column (Correction #1)
        val aliasTableCursor = db.query("PRAGMA table_info(exercise_aliases)")
        var foundIsForgeDerived = false
        while (aliasTableCursor.moveToNext()) {
            val nameCol = aliasTableCursor.getString(aliasTableCursor.getColumnIndexOrThrow("name"))
            if (nameCol == "is_forge_derived") {
                foundIsForgeDerived = true
                break
            }
        }
        aliasTableCursor.close()
        assertThat(foundIsForgeDerived).isTrue()

        // 2. Verify legacy exercises preserved and defaulted to 'UNKNOWN' for forge_movement_pattern (Correction #6 & #7)
        val exerciseCursor = db.query("SELECT id, name, is_custom, forge_movement_pattern, source FROM exercises ORDER BY id")
        val exerciseList = mutableListOf<Map<String, Any>>()
        while (exerciseCursor.moveToNext()) {
            exerciseList.add(
                mapOf(
                    "id" to exerciseCursor.getString(0),
                    "name" to exerciseCursor.getString(1),
                    "is_custom" to exerciseCursor.getInt(2),
                    "forge_movement_pattern" to exerciseCursor.getString(3),
                    "source" to exerciseCursor.getString(4)
                )
            )
        }
        exerciseCursor.close()

        assertThat(exerciseList).hasSize(2)
        // Check Phase 1 canonical exercise
        val bench = exerciseList.first { it["id"] == "barbell_bench_press" }
        assertThat(bench["name"]).isEqualTo("Barbell Bench Press")
        assertThat(bench["is_custom"]).isEqualTo(0)
        assertThat(bench["forge_movement_pattern"]).isEqualTo("UNKNOWN") // Correction #6 verified!

        // Check custom exercise preserved
        val custom = exerciseList.first { it["id"] == "custom_cable_crossover" }
        assertThat(custom["name"]).isEqualTo("Custom Cable Crossover")
        assertThat(custom["is_custom"]).isEqualTo(1)
        assertThat(custom["forge_movement_pattern"]).isEqualTo("UNKNOWN") // Not defaulted to ISOLATION!

        // 3. Verify in-progress active workout session preserved intact (Correction #7)
        val sessionCursor = db.query("SELECT id, name, status, active_exercise_id, active_set_index FROM workout_sessions WHERE id = 'active_sess_001'")
        assertThat(sessionCursor.moveToFirst()).isTrue()
        assertThat(sessionCursor.getString(0)).isEqualTo("active_sess_001")
        assertThat(sessionCursor.getString(1)).isEqualTo("Chest & Arms Push")
        assertThat(sessionCursor.getString(2)).isEqualTo("IN_PROGRESS")
        assertThat(sessionCursor.getString(3)).isEqualTo("barbell_bench_press")
        assertThat(sessionCursor.getInt(4)).isEqualTo(1)
        sessionCursor.close()

        // 4. Verify logged sets preserved intact (Correction #7)
        val setCursor = db.query("SELECT id, session_id, exercise_id, weight_kg, reps, is_completed FROM workout_sets WHERE id = 'set_001'")
        assertThat(setCursor.moveToFirst()).isTrue()
        assertThat(setCursor.getString(0)).isEqualTo("set_001")
        assertThat(setCursor.getString(1)).isEqualTo("active_sess_001")
        assertThat(setCursor.getString(2)).isEqualTo("barbell_bench_press")
        assertThat(setCursor.getDouble(3)).isEqualTo(100.0)
        assertThat(setCursor.getInt(4)).isEqualTo(5)
        assertThat(setCursor.getInt(5)).isEqualTo(1)
        setCursor.close()

        // 5. Verify all new relational tables exist and can accept records
        db.execSQL("INSERT INTO muscles (id, name, body_part) VALUES ('chest', 'Chest', 'CHEST')")
        db.execSQL("INSERT INTO equipment (id, name) VALUES ('barbell', 'Barbell')")
        db.execSQL("INSERT INTO exercise_families (id, name, description, primary_pattern) VALUES ('bench_press_family', 'Bench Press', 'Pressing variations', 'HORIZONTAL_PUSH')")
        db.execSQL("INSERT INTO exercise_muscles (exercise_id, muscle_id, role, is_forge_derived) VALUES ('barbell_bench_press', 'chest', 'PRIMARY', 0)")
        db.execSQL("INSERT INTO exercise_equipment (exercise_id, equipment_id, is_primary) VALUES ('barbell_bench_press', 'barbell', 1)")
        db.execSQL("INSERT INTO exercise_attributes (exercise_id, attribute) VALUES ('barbell_bench_press', 'BILATERAL')")
        db.execSQL("INSERT INTO exercise_family_members (family_id, exercise_id, is_canonical_lead) VALUES ('bench_press_family', 'barbell_bench_press', 1)")

        // Verify relational mapping count
        val relCheckCursor = db.query("SELECT COUNT(*) FROM exercise_muscles WHERE exercise_id = 'barbell_bench_press'")
        assertThat(relCheckCursor.moveToFirst()).isTrue()
        assertThat(relCheckCursor.getInt(0)).isEqualTo(1)
        relCheckCursor.close()

        // 6. Verify FTS rebuild index contains migrated exercises
        val ftsCursor = db.query("SELECT rowid, name FROM exercises_fts WHERE exercises_fts MATCH 'Bench*'")
        assertThat(ftsCursor.moveToFirst()).isTrue()
        assertThat(ftsCursor.getString(1)).isEqualTo("Barbell Bench Press")
        ftsCursor.close()

        db.close()
    }

    @Test
    fun migration_4_to_5_createsUserProfileWithBodyTelemetry_andAllEntities() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_v4_to_v5_migration.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Minimal v4 schema needed before migrating to v5
                        db.execSQL("CREATE TABLE IF NOT EXISTS workout_sessions (id TEXT PRIMARY KEY NOT NULL, status TEXT NOT NULL, startTime INTEGER NOT NULL, durationSeconds INTEGER NOT NULL, totalVolumeKg REAL NOT NULL, name TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS exercises (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = sqliteHelper.writableDatabase
        MIGRATION_4_5.migrate(db)

        // Verify user_profile columns including body telemetry
        val profileCursor = db.query("PRAGMA table_info(user_profile)")
        val columns = mutableSetOf<String>()
        while (profileCursor.moveToNext()) {
            val nameIdx = profileCursor.getColumnIndex("name")
            if (nameIdx >= 0) columns.add(profileCursor.getString(nameIdx))
        }
        profileCursor.close()

        assertThat(columns).contains("height_cm")
        assertThat(columns).contains("weight_kg")
        assertThat(columns).contains("age")
        assertThat(columns).contains("sex")
        assertThat(columns).contains("photo_password_hash")
        assertThat(columns).contains("is_initialized")

        // Verify other tables created
        val tablesCursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val tables = mutableSetOf<String>()
        while (tablesCursor.moveToNext()) {
            tables.add(tablesCursor.getString(0))
        }
        tablesCursor.close()

        assertThat(tables).contains("training_schedule")
        assertThat(tables).contains("workout_templates")
        assertThat(tables).contains("template_exercises")
        assertThat(tables).contains("daily_activity")
        assertThat(tables).contains("transformation_checkins")
        assertThat(tables).contains("app_settings")

        db.close()
    }

    @Test
    fun migration_5_to_6_addsMissingBodyTelemetryColumnsToUserProfile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_v5_to_v6_migration.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // v5 user_profile without height_cm, weight_kg, age, sex
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS user_profile (
                                id INTEGER PRIMARY KEY NOT NULL,
                                name TEXT NOT NULL,
                                photo_uri TEXT,
                                goal TEXT NOT NULL,
                                experience TEXT NOT NULL,
                                days_per_week INTEGER NOT NULL,
                                session_duration_min INTEGER NOT NULL,
                                equipment TEXT NOT NULL,
                                maintenance_calories INTEGER NOT NULL,
                                nutrition_goal TEXT NOT NULL,
                                target_calories INTEGER NOT NULL,
                                target_protein_g INTEGER NOT NULL,
                                target_carbs_g INTEGER NOT NULL,
                                target_fat_g INTEGER NOT NULL,
                                transformation_start_date INTEGER NOT NULL,
                                photo_password_hash TEXT,
                                photo_password_salt TEXT,
                                is_initialized INTEGER NOT NULL,
                                initialization_step INTEGER NOT NULL,
                                updated_at INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = sqliteHelper.writableDatabase
        MIGRATION_5_6.migrate(db)

        val profileCursor = db.query("PRAGMA table_info(user_profile)")
        val columns = mutableSetOf<String>()
        while (profileCursor.moveToNext()) {
            val nameIdx = profileCursor.getColumnIndex("name")
            if (nameIdx >= 0) columns.add(profileCursor.getString(nameIdx))
        }
        profileCursor.close()

        assertThat(columns).contains("height_cm")
        assertThat(columns).contains("weight_kg")
        assertThat(columns).contains("age")
        assertThat(columns).contains("sex")

        db.close()
    }
}

