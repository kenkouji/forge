package com.forge.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit, documented, and tested database migrations.
 * Destructive fallback (fallbackToDestructiveMigration) is strictly prohibited.
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN name TEXT NOT NULL DEFAULT 'Empty Workout'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Alter exercises table with Phase 2 provenance and normalized columns
        val existingColumns = mutableSetOf<String>()
        val cursor = db.query("PRAGMA table_info(exercises)")
        while (cursor.moveToNext()) {
            val nameIdx = cursor.getColumnIndex("name")
            if (nameIdx >= 0) existingColumns.add(cursor.getString(nameIdx))
        }
        cursor.close()

        if (!existingColumns.contains("source")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN source TEXT NOT NULL DEFAULT 'forge_legacy'")
        }
        if (!existingColumns.contains("source_id")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN source_id TEXT")
        }
        if (!existingColumns.contains("source_category")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN source_category TEXT")
        }
        if (!existingColumns.contains("source_force")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN source_force TEXT")
        }
        if (!existingColumns.contains("source_level")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN source_level TEXT")
        }
        if (!existingColumns.contains("source_mechanic")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN source_mechanic TEXT")
        }
        if (!existingColumns.contains("source_equipment")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN source_equipment TEXT")
        }
        if (!existingColumns.contains("forge_movement_pattern")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN forge_movement_pattern TEXT NOT NULL DEFAULT 'UNKNOWN'")
        }
        if (!existingColumns.contains("forge_exercise_family_id")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN forge_exercise_family_id TEXT")
        }
        if (!existingColumns.contains("search_tokens")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN search_tokens TEXT NOT NULL DEFAULT ''")
        }
        if (!existingColumns.contains("license")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN license TEXT NOT NULL DEFAULT 'The Unlicense'")
        }

        // 2. Explicit CREATE TABLE and indexes for exercise_aliases (Correction #1)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_aliases (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                exercise_id TEXT NOT NULL,
                alias TEXT NOT NULL,
                is_forge_derived INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        val aliasColumns = mutableSetOf<String>()
        val aliasCursor = db.query("PRAGMA table_info(exercise_aliases)")
        while (aliasCursor.moveToNext()) {
            val nameIdx = aliasCursor.getColumnIndex("name")
            if (nameIdx >= 0) aliasColumns.add(aliasCursor.getString(nameIdx))
        }
        aliasCursor.close()
        if (!aliasColumns.contains("is_forge_derived")) {
            db.execSQL("ALTER TABLE exercise_aliases ADD COLUMN is_forge_derived INTEGER NOT NULL DEFAULT 1")
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_aliases_exercise_id ON exercise_aliases (exercise_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_aliases_alias ON exercise_aliases (alias)")

        // 3. Create muscles table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS muscles (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                body_part TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 4. Create exercise_muscles junction table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_muscles (
                exercise_id TEXT NOT NULL,
                muscle_id TEXT NOT NULL,
                role TEXT NOT NULL,
                is_forge_derived INTEGER NOT NULL,
                PRIMARY KEY(exercise_id, muscle_id),
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(muscle_id) REFERENCES muscles(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_muscles_muscle_id ON exercise_muscles (muscle_id)")

        // 5. Create equipment table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS equipment (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 6. Create exercise_equipment junction table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_equipment (
                exercise_id TEXT NOT NULL,
                equipment_id TEXT NOT NULL,
                is_primary INTEGER NOT NULL,
                PRIMARY KEY(exercise_id, equipment_id),
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(equipment_id) REFERENCES equipment(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_equipment_equipment_id ON exercise_equipment (equipment_id)")

        // 7. Create exercise_attributes table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_attributes (
                exercise_id TEXT NOT NULL,
                attribute TEXT NOT NULL,
                PRIMARY KEY(exercise_id, attribute),
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_attributes_attribute ON exercise_attributes (attribute)")

        // 8. Create exercise_families table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_families (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                primary_pattern TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 9. Create exercise_family_members junction table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_family_members (
                family_id TEXT NOT NULL,
                exercise_id TEXT NOT NULL,
                is_canonical_lead INTEGER NOT NULL,
                PRIMARY KEY(family_id, exercise_id),
                FOREIGN KEY(family_id) REFERENCES exercise_families(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_family_members_exercise_id ON exercise_family_members (exercise_id)")

        // 10. Recreate FTS4 virtual table with updated columns & triggers
        db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_exercises_fts_BEFORE_UPDATE")
        db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_exercises_fts_BEFORE_DELETE")
        db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_exercises_fts_AFTER_UPDATE")
        db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_exercises_fts_AFTER_INSERT")
        db.execSQL("DROP TABLE IF EXISTS exercises_fts")

        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS exercises_fts USING FTS4(
                name TEXT NOT NULL,
                canonical_name TEXT NOT NULL,
                forge_movement_pattern TEXT NOT NULL,
                search_tokens TEXT NOT NULL,
                instructions TEXT NOT NULL,
                tokenize=unicode61,
                content=exercises
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_BEFORE_UPDATE BEFORE UPDATE ON exercises BEGIN
                DELETE FROM exercises_fts WHERE docid=OLD.rowid;
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_BEFORE_DELETE BEFORE DELETE ON exercises BEGIN
                DELETE FROM exercises_fts WHERE docid=OLD.rowid;
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_AFTER_UPDATE AFTER UPDATE ON exercises BEGIN
                INSERT INTO exercises_fts(docid, name, canonical_name, forge_movement_pattern, search_tokens, instructions)
                VALUES (NEW.rowid, NEW.name, NEW.canonical_name, NEW.forge_movement_pattern, NEW.search_tokens, NEW.instructions);
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_AFTER_INSERT AFTER INSERT ON exercises BEGIN
                INSERT INTO exercises_fts(docid, name, canonical_name, forge_movement_pattern, search_tokens, instructions)
                VALUES (NEW.rowid, NEW.name, NEW.canonical_name, NEW.forge_movement_pattern, NEW.search_tokens, NEW.instructions);
            END
            """.trimIndent()
        )
        db.execSQL("INSERT INTO exercises_fts(exercises_fts) VALUES('rebuild')")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Alter exercises table with popularity and favorites
        val existingColumns = mutableSetOf<String>()
        val cursor = db.query("PRAGMA table_info(exercises)")
        while (cursor.moveToNext()) {
            val nameIdx = cursor.getColumnIndex("name")
            if (nameIdx >= 0) existingColumns.add(cursor.getString(nameIdx))
        }
        cursor.close()

        if (!existingColumns.contains("is_popular")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN is_popular INTEGER NOT NULL DEFAULT 0")
        }
        if (!existingColumns.contains("popularity_rank")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN popularity_rank INTEGER NOT NULL DEFAULT 9999")
        }
        if (!existingColumns.contains("is_favorite")) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
        }

        // 2. Create exercise_progression_records table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_progression_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                exercise_id TEXT NOT NULL,
                session_id TEXT NOT NULL,
                recommended_weight_kg REAL NOT NULL,
                recommended_rep_min INTEGER NOT NULL,
                recommended_rep_max INTEGER NOT NULL,
                rationale TEXT NOT NULL,
                is_deload INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_progression_records_exercise_id ON exercise_progression_records (exercise_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_progression_records_session_id ON exercise_progression_records (session_id)")

        // 3. Create exercise_personal_records table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_personal_records (
                exercise_id TEXT PRIMARY KEY NOT NULL,
                max_weight_kg REAL NOT NULL DEFAULT 0.0,
                max_reps_at_max_weight INTEGER NOT NULL DEFAULT 0,
                estimated_1rm_kg REAL NOT NULL DEFAULT 0.0,
                best_set_volume_kg REAL NOT NULL DEFAULT 0.0,
                best_session_volume_kg REAL NOT NULL DEFAULT 0.0,
                achieved_at INTEGER NOT NULL,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // 4. Create user_nutrition_profile table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_nutrition_profile (
                id TEXT PRIMARY KEY NOT NULL,
                maintenance_calories INTEGER NOT NULL DEFAULT 2500,
                is_maintenance_manual INTEGER NOT NULL DEFAULT 0,
                goal TEXT NOT NULL DEFAULT 'MAINTENANCE',
                target_calories INTEGER NOT NULL DEFAULT 2500,
                protein_grams INTEGER NOT NULL DEFAULT 160,
                fat_grams INTEGER NOT NULL DEFAULT 70,
                carbs_grams INTEGER NOT NULL DEFAULT 305,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 5. Create weight_logs table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weight_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                weight_kg REAL NOT NULL,
                logged_date TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_weight_logs_logged_date ON weight_logs (logged_date)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. user_profile
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
                height_cm REAL NOT NULL DEFAULT 175.0,
                weight_kg REAL NOT NULL DEFAULT 75.0,
                age INTEGER,
                sex TEXT,
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

        // 2. training_schedule
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS training_schedule (
                day_of_week INTEGER PRIMARY KEY NOT NULL,
                is_training_day INTEGER NOT NULL,
                focus TEXT NOT NULL,
                template_id TEXT,
                target_duration_min INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 3. workout_templates
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_templates (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                focus TEXT NOT NULL,
                version INTEGER NOT NULL,
                target_muscles TEXT NOT NULL,
                estimated_duration_min INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 4. template_exercises
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS template_exercises (
                id TEXT PRIMARY KEY NOT NULL,
                template_id TEXT NOT NULL,
                exercise_id TEXT NOT NULL,
                order_index INTEGER NOT NULL,
                target_sets INTEGER NOT NULL,
                target_reps_min INTEGER NOT NULL,
                target_reps_max INTEGER NOT NULL,
                target_rir INTEGER NOT NULL,
                target_weight_kg REAL,
                rest_seconds INTEGER NOT NULL,
                is_warmup INTEGER NOT NULL,
                is_drop_set INTEGER NOT NULL,
                notes TEXT,
                FOREIGN KEY(template_id) REFERENCES workout_templates(id) ON DELETE CASCADE,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_template_id ON template_exercises (template_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_template_exercises_exercise_id ON template_exercises (exercise_id)")

        // 5. daily_activity
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_activity (
                date TEXT PRIMARY KEY NOT NULL,
                steps INTEGER NOT NULL,
                active_calories INTEGER NOT NULL,
                total_calories INTEGER NOT NULL,
                distance_meters REAL NOT NULL,
                is_calories_measured INTEGER NOT NULL,
                has_health_connect_sync INTEGER NOT NULL,
                last_sync_timestamp INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 6. transformation_checkins
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transformation_checkins (
                id TEXT PRIMARY KEY NOT NULL,
                week_number INTEGER NOT NULL,
                date TEXT NOT NULL,
                front_encrypted_path TEXT,
                side_encrypted_path TEXT,
                back_encrypted_path TEXT,
                weight_kg REAL,
                notes TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 7. app_settings
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY NOT NULL,
                reduce_motion INTEGER NOT NULL,
                particles_enabled INTEGER NOT NULL,
                haptics_enabled INTEGER NOT NULL,
                health_connect_enabled INTEGER NOT NULL,
                auto_lock_vault_on_background INTEGER NOT NULL,
                notif_workout_reminders INTEGER NOT NULL,
                notif_streak_reminders INTEGER NOT NULL,
                notif_motivation INTEGER NOT NULL,
                notif_pre_workout_alerts INTEGER NOT NULL,
                notif_post_workout_congrats INTEGER NOT NULL,
                notif_nutrition_reminders INTEGER NOT NULL,
                notif_hydration_reminders INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Insert default AppSettings row
        db.execSQL(
            """
            INSERT OR IGNORE INTO app_settings (
                id, reduce_motion, particles_enabled, haptics_enabled,
                health_connect_enabled, auto_lock_vault_on_background,
                notif_workout_reminders, notif_streak_reminders, notif_motivation,
                notif_pre_workout_alerts, notif_post_workout_congrats,
                notif_nutrition_reminders, notif_hydration_reminders, updated_at
            ) VALUES (1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, ${System.currentTimeMillis()})
            """.trimIndent()
        )

        // Upgrade protection: Check if user already has completed workouts
        val cursor = db.query("SELECT COUNT(*) FROM workout_sessions WHERE status = 'COMPLETED'")
        var hasExistingCompleted = false
        if (cursor.moveToFirst()) {
            hasExistingCompleted = cursor.getInt(0) > 0
        }
        cursor.close()

        if (hasExistingCompleted) {
            // Existing user: mark initialized so upgrade doesn't wipe or force onboarding
            db.execSQL(
                """
                INSERT OR IGNORE INTO user_profile (
                    id, name, photo_uri, goal, experience, days_per_week, session_duration_min,
                    equipment, height_cm, weight_kg, age, sex, maintenance_calories, nutrition_goal, target_calories,
                    target_protein_g, target_carbs_g, target_fat_g, transformation_start_date,
                    photo_password_hash, photo_password_salt, is_initialized, initialization_step, updated_at
                ) VALUES (
                    1, 'Athlete', NULL, 'Hypertrophy', 'Intermediate', 4, 60,
                    'Gym,Barbells,Dumbbells,Cables', 175.0, 75.0, NULL, NULL, 2400, 'Maintain', 2400,
                    160, 250, 70, ${System.currentTimeMillis()},
                    NULL, NULL, 1, 12, ${System.currentTimeMillis()}
                )
                """.trimIndent()
            )
        }
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val existingColumns = mutableSetOf<String>()
        val cursor = db.query("PRAGMA table_info(user_profile)")
        while (cursor.moveToNext()) {
            val nameIdx = cursor.getColumnIndex("name")
            if (nameIdx >= 0) existingColumns.add(cursor.getString(nameIdx))
        }
        cursor.close()

        if (!existingColumns.contains("height_cm")) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN height_cm REAL NOT NULL DEFAULT 175.0")
        }
        if (!existingColumns.contains("weight_kg")) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN weight_kg REAL NOT NULL DEFAULT 75.0")
        }
        if (!existingColumns.contains("age")) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN age INTEGER")
        }
        if (!existingColumns.contains("sex")) {
            db.execSQL("ALTER TABLE user_profile ADD COLUMN sex TEXT")
        }
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6
)

