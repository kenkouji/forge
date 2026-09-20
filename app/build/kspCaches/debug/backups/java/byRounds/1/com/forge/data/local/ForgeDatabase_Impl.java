package com.forge.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.FtsTableInfo;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.forge.data.local.dao.ExerciseDao;
import com.forge.data.local.dao.ExerciseDao_Impl;
import com.forge.data.local.dao.WorkoutDao;
import com.forge.data.local.dao.WorkoutDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ForgeDatabase_Impl extends ForgeDatabase {
  private volatile ExerciseDao _exerciseDao;

  private volatile WorkoutDao _workoutDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercises` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `canonical_name` TEXT NOT NULL, `movement_pattern` TEXT NOT NULL, `mechanic` TEXT NOT NULL, `force_type` TEXT NOT NULL, `experience_level` TEXT NOT NULL, `instructions` TEXT NOT NULL, `form_cues` TEXT NOT NULL, `common_mistakes` TEXT NOT NULL, `youtube_video_id` TEXT, `is_custom` INTEGER NOT NULL, `source` TEXT NOT NULL, `source_id` TEXT, `source_category` TEXT, `source_force` TEXT, `source_level` TEXT, `source_mechanic` TEXT, `source_equipment` TEXT, `forge_movement_pattern` TEXT NOT NULL, `forge_exercise_family_id` TEXT, `search_tokens` TEXT NOT NULL, `license` TEXT NOT NULL, `is_popular` INTEGER NOT NULL, `popularity_rank` INTEGER NOT NULL, `is_favorite` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `exercises_fts` USING FTS4(`name` TEXT NOT NULL, `canonical_name` TEXT NOT NULL, `forge_movement_pattern` TEXT NOT NULL, `search_tokens` TEXT NOT NULL, `instructions` TEXT NOT NULL, tokenize=unicode61, content=`exercises`)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_BEFORE_UPDATE BEFORE UPDATE ON `exercises` BEGIN DELETE FROM `exercises_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_BEFORE_DELETE BEFORE DELETE ON `exercises` BEGIN DELETE FROM `exercises_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_AFTER_UPDATE AFTER UPDATE ON `exercises` BEGIN INSERT INTO `exercises_fts`(`docid`, `name`, `canonical_name`, `forge_movement_pattern`, `search_tokens`, `instructions`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`canonical_name`, NEW.`forge_movement_pattern`, NEW.`search_tokens`, NEW.`instructions`); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_AFTER_INSERT AFTER INSERT ON `exercises` BEGIN INSERT INTO `exercises_fts`(`docid`, `name`, `canonical_name`, `forge_movement_pattern`, `search_tokens`, `instructions`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`canonical_name`, NEW.`forge_movement_pattern`, NEW.`search_tokens`, NEW.`instructions`); END");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_aliases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` TEXT NOT NULL, `alias` TEXT NOT NULL, `is_forge_derived` INTEGER NOT NULL, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_aliases_exercise_id` ON `exercise_aliases` (`exercise_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_aliases_alias` ON `exercise_aliases` (`alias`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `muscles` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `body_part` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_muscles` (`exercise_id` TEXT NOT NULL, `muscle_id` TEXT NOT NULL, `role` TEXT NOT NULL, `is_forge_derived` INTEGER NOT NULL, PRIMARY KEY(`exercise_id`, `muscle_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`muscle_id`) REFERENCES `muscles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_muscles_muscle_id` ON `exercise_muscles` (`muscle_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `equipment` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_equipment` (`exercise_id` TEXT NOT NULL, `equipment_id` TEXT NOT NULL, `is_primary` INTEGER NOT NULL, PRIMARY KEY(`exercise_id`, `equipment_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`equipment_id`) REFERENCES `equipment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_equipment_equipment_id` ON `exercise_equipment` (`equipment_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_attributes` (`exercise_id` TEXT NOT NULL, `attribute` TEXT NOT NULL, PRIMARY KEY(`exercise_id`, `attribute`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_attributes_attribute` ON `exercise_attributes` (`attribute`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_families` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `primary_pattern` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_family_members` (`family_id` TEXT NOT NULL, `exercise_id` TEXT NOT NULL, `is_canonical_lead` INTEGER NOT NULL, PRIMARY KEY(`family_id`, `exercise_id`), FOREIGN KEY(`family_id`) REFERENCES `exercise_families`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_family_members_exercise_id` ON `exercise_family_members` (`exercise_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` TEXT NOT NULL, `routine_id` TEXT, `name` TEXT NOT NULL, `start_time` INTEGER NOT NULL, `end_time` INTEGER, `status` TEXT NOT NULL, `active_exercise_id` TEXT, `active_set_index` INTEGER NOT NULL, `total_volume_kg` REAL NOT NULL, `duration_seconds` INTEGER NOT NULL, `notes` TEXT, `last_updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_status` ON `workout_sessions` (`status`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_start_time` ON `workout_sessions` (`start_time`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_sets` (`id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `exercise_id` TEXT NOT NULL, `set_order` INTEGER NOT NULL, `set_type` TEXT NOT NULL, `weight_kg` REAL NOT NULL, `reps` INTEGER NOT NULL, `rpe` REAL, `rir` INTEGER, `tempo` TEXT, `rest_seconds_taken` INTEGER, `is_completed` INTEGER NOT NULL, `is_personal_record` INTEGER NOT NULL, `completed_at` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_session_id` ON `workout_sets` (`session_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exercise_id` ON `workout_sets` (`exercise_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_session_id_set_order` ON `workout_sets` (`session_id`, `set_order`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_progression_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `exercise_id` TEXT NOT NULL, `session_id` TEXT NOT NULL, `recommended_weight_kg` REAL NOT NULL, `recommended_rep_min` INTEGER NOT NULL, `recommended_rep_max` INTEGER NOT NULL, `rationale` TEXT NOT NULL, `is_deload` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_progression_records_exercise_id` ON `exercise_progression_records` (`exercise_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_progression_records_session_id` ON `exercise_progression_records` (`session_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercise_personal_records` (`exercise_id` TEXT NOT NULL, `max_weight_kg` REAL NOT NULL, `max_reps_at_max_weight` INTEGER NOT NULL, `estimated_1rm_kg` REAL NOT NULL, `best_set_volume_kg` REAL NOT NULL, `best_session_volume_kg` REAL NOT NULL, `achieved_at` INTEGER NOT NULL, PRIMARY KEY(`exercise_id`), FOREIGN KEY(`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_nutrition_profile` (`id` TEXT NOT NULL, `maintenance_calories` INTEGER NOT NULL, `is_maintenance_manual` INTEGER NOT NULL, `goal` TEXT NOT NULL, `target_calories` INTEGER NOT NULL, `protein_grams` INTEGER NOT NULL, `fat_grams` INTEGER NOT NULL, `carbs_grams` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weight_kg` REAL NOT NULL, `logged_date` TEXT NOT NULL, `created_at` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_weight_logs_logged_date` ON `weight_logs` (`logged_date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '967d9eb0400a0dd8a74575afb906640c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `exercises`");
        db.execSQL("DROP TABLE IF EXISTS `exercises_fts`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_aliases`");
        db.execSQL("DROP TABLE IF EXISTS `muscles`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_muscles`");
        db.execSQL("DROP TABLE IF EXISTS `equipment`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_equipment`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_attributes`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_families`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_family_members`");
        db.execSQL("DROP TABLE IF EXISTS `workout_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `workout_sets`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_progression_records`");
        db.execSQL("DROP TABLE IF EXISTS `exercise_personal_records`");
        db.execSQL("DROP TABLE IF EXISTS `user_nutrition_profile`");
        db.execSQL("DROP TABLE IF EXISTS `weight_logs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_BEFORE_UPDATE BEFORE UPDATE ON `exercises` BEGIN DELETE FROM `exercises_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_BEFORE_DELETE BEFORE DELETE ON `exercises` BEGIN DELETE FROM `exercises_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_AFTER_UPDATE AFTER UPDATE ON `exercises` BEGIN INSERT INTO `exercises_fts`(`docid`, `name`, `canonical_name`, `forge_movement_pattern`, `search_tokens`, `instructions`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`canonical_name`, NEW.`forge_movement_pattern`, NEW.`search_tokens`, NEW.`instructions`); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_exercises_fts_AFTER_INSERT AFTER INSERT ON `exercises` BEGIN INSERT INTO `exercises_fts`(`docid`, `name`, `canonical_name`, `forge_movement_pattern`, `search_tokens`, `instructions`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`canonical_name`, NEW.`forge_movement_pattern`, NEW.`search_tokens`, NEW.`instructions`); END");
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsExercises = new HashMap<String, TableInfo.Column>(28);
        _columnsExercises.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("canonical_name", new TableInfo.Column("canonical_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("movement_pattern", new TableInfo.Column("movement_pattern", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("mechanic", new TableInfo.Column("mechanic", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("force_type", new TableInfo.Column("force_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("experience_level", new TableInfo.Column("experience_level", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("instructions", new TableInfo.Column("instructions", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("form_cues", new TableInfo.Column("form_cues", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("common_mistakes", new TableInfo.Column("common_mistakes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("youtube_video_id", new TableInfo.Column("youtube_video_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("is_custom", new TableInfo.Column("is_custom", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("source_id", new TableInfo.Column("source_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("source_category", new TableInfo.Column("source_category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("source_force", new TableInfo.Column("source_force", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("source_level", new TableInfo.Column("source_level", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("source_mechanic", new TableInfo.Column("source_mechanic", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("source_equipment", new TableInfo.Column("source_equipment", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("forge_movement_pattern", new TableInfo.Column("forge_movement_pattern", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("forge_exercise_family_id", new TableInfo.Column("forge_exercise_family_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("search_tokens", new TableInfo.Column("search_tokens", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("license", new TableInfo.Column("license", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("is_popular", new TableInfo.Column("is_popular", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("popularity_rank", new TableInfo.Column("popularity_rank", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("is_favorite", new TableInfo.Column("is_favorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercises.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExercises = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExercises = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExercises = new TableInfo("exercises", _columnsExercises, _foreignKeysExercises, _indicesExercises);
        final TableInfo _existingExercises = TableInfo.read(db, "exercises");
        if (!_infoExercises.equals(_existingExercises)) {
          return new RoomOpenHelper.ValidationResult(false, "exercises(com.forge.data.local.entity.ExerciseEntity).\n"
                  + " Expected:\n" + _infoExercises + "\n"
                  + " Found:\n" + _existingExercises);
        }
        final HashSet<String> _columnsExercisesFts = new HashSet<String>(6);
        _columnsExercisesFts.add("name");
        _columnsExercisesFts.add("canonical_name");
        _columnsExercisesFts.add("forge_movement_pattern");
        _columnsExercisesFts.add("search_tokens");
        _columnsExercisesFts.add("instructions");
        final FtsTableInfo _infoExercisesFts = new FtsTableInfo("exercises_fts", _columnsExercisesFts, "CREATE VIRTUAL TABLE IF NOT EXISTS `exercises_fts` USING FTS4(`name` TEXT NOT NULL, `canonical_name` TEXT NOT NULL, `forge_movement_pattern` TEXT NOT NULL, `search_tokens` TEXT NOT NULL, `instructions` TEXT NOT NULL, tokenize=unicode61, content=`exercises`)");
        final FtsTableInfo _existingExercisesFts = FtsTableInfo.read(db, "exercises_fts");
        if (!_infoExercisesFts.equals(_existingExercisesFts)) {
          return new RoomOpenHelper.ValidationResult(false, "exercises_fts(com.forge.data.local.entity.ExerciseFtsEntity).\n"
                  + " Expected:\n" + _infoExercisesFts + "\n"
                  + " Found:\n" + _existingExercisesFts);
        }
        final HashMap<String, TableInfo.Column> _columnsExerciseAliases = new HashMap<String, TableInfo.Column>(4);
        _columnsExerciseAliases.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseAliases.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseAliases.put("alias", new TableInfo.Column("alias", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseAliases.put("is_forge_derived", new TableInfo.Column("is_forge_derived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExerciseAliases = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysExerciseAliases.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExerciseAliases = new HashSet<TableInfo.Index>(2);
        _indicesExerciseAliases.add(new TableInfo.Index("index_exercise_aliases_exercise_id", false, Arrays.asList("exercise_id"), Arrays.asList("ASC")));
        _indicesExerciseAliases.add(new TableInfo.Index("index_exercise_aliases_alias", false, Arrays.asList("alias"), Arrays.asList("ASC")));
        final TableInfo _infoExerciseAliases = new TableInfo("exercise_aliases", _columnsExerciseAliases, _foreignKeysExerciseAliases, _indicesExerciseAliases);
        final TableInfo _existingExerciseAliases = TableInfo.read(db, "exercise_aliases");
        if (!_infoExerciseAliases.equals(_existingExerciseAliases)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_aliases(com.forge.data.local.entity.ExerciseAliasEntity).\n"
                  + " Expected:\n" + _infoExerciseAliases + "\n"
                  + " Found:\n" + _existingExerciseAliases);
        }
        final HashMap<String, TableInfo.Column> _columnsMuscles = new HashMap<String, TableInfo.Column>(3);
        _columnsMuscles.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMuscles.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMuscles.put("body_part", new TableInfo.Column("body_part", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMuscles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMuscles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMuscles = new TableInfo("muscles", _columnsMuscles, _foreignKeysMuscles, _indicesMuscles);
        final TableInfo _existingMuscles = TableInfo.read(db, "muscles");
        if (!_infoMuscles.equals(_existingMuscles)) {
          return new RoomOpenHelper.ValidationResult(false, "muscles(com.forge.data.local.entity.MuscleEntity).\n"
                  + " Expected:\n" + _infoMuscles + "\n"
                  + " Found:\n" + _existingMuscles);
        }
        final HashMap<String, TableInfo.Column> _columnsExerciseMuscles = new HashMap<String, TableInfo.Column>(4);
        _columnsExerciseMuscles.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseMuscles.put("muscle_id", new TableInfo.Column("muscle_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseMuscles.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseMuscles.put("is_forge_derived", new TableInfo.Column("is_forge_derived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExerciseMuscles = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysExerciseMuscles.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        _foreignKeysExerciseMuscles.add(new TableInfo.ForeignKey("muscles", "CASCADE", "NO ACTION", Arrays.asList("muscle_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExerciseMuscles = new HashSet<TableInfo.Index>(1);
        _indicesExerciseMuscles.add(new TableInfo.Index("index_exercise_muscles_muscle_id", false, Arrays.asList("muscle_id"), Arrays.asList("ASC")));
        final TableInfo _infoExerciseMuscles = new TableInfo("exercise_muscles", _columnsExerciseMuscles, _foreignKeysExerciseMuscles, _indicesExerciseMuscles);
        final TableInfo _existingExerciseMuscles = TableInfo.read(db, "exercise_muscles");
        if (!_infoExerciseMuscles.equals(_existingExerciseMuscles)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_muscles(com.forge.data.local.entity.ExerciseMuscleEntity).\n"
                  + " Expected:\n" + _infoExerciseMuscles + "\n"
                  + " Found:\n" + _existingExerciseMuscles);
        }
        final HashMap<String, TableInfo.Column> _columnsEquipment = new HashMap<String, TableInfo.Column>(2);
        _columnsEquipment.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEquipment.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEquipment = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEquipment = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEquipment = new TableInfo("equipment", _columnsEquipment, _foreignKeysEquipment, _indicesEquipment);
        final TableInfo _existingEquipment = TableInfo.read(db, "equipment");
        if (!_infoEquipment.equals(_existingEquipment)) {
          return new RoomOpenHelper.ValidationResult(false, "equipment(com.forge.data.local.entity.EquipmentEntity).\n"
                  + " Expected:\n" + _infoEquipment + "\n"
                  + " Found:\n" + _existingEquipment);
        }
        final HashMap<String, TableInfo.Column> _columnsExerciseEquipment = new HashMap<String, TableInfo.Column>(3);
        _columnsExerciseEquipment.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseEquipment.put("equipment_id", new TableInfo.Column("equipment_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseEquipment.put("is_primary", new TableInfo.Column("is_primary", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExerciseEquipment = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysExerciseEquipment.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        _foreignKeysExerciseEquipment.add(new TableInfo.ForeignKey("equipment", "CASCADE", "NO ACTION", Arrays.asList("equipment_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExerciseEquipment = new HashSet<TableInfo.Index>(1);
        _indicesExerciseEquipment.add(new TableInfo.Index("index_exercise_equipment_equipment_id", false, Arrays.asList("equipment_id"), Arrays.asList("ASC")));
        final TableInfo _infoExerciseEquipment = new TableInfo("exercise_equipment", _columnsExerciseEquipment, _foreignKeysExerciseEquipment, _indicesExerciseEquipment);
        final TableInfo _existingExerciseEquipment = TableInfo.read(db, "exercise_equipment");
        if (!_infoExerciseEquipment.equals(_existingExerciseEquipment)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_equipment(com.forge.data.local.entity.ExerciseEquipmentEntity).\n"
                  + " Expected:\n" + _infoExerciseEquipment + "\n"
                  + " Found:\n" + _existingExerciseEquipment);
        }
        final HashMap<String, TableInfo.Column> _columnsExerciseAttributes = new HashMap<String, TableInfo.Column>(2);
        _columnsExerciseAttributes.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseAttributes.put("attribute", new TableInfo.Column("attribute", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExerciseAttributes = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysExerciseAttributes.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExerciseAttributes = new HashSet<TableInfo.Index>(1);
        _indicesExerciseAttributes.add(new TableInfo.Index("index_exercise_attributes_attribute", false, Arrays.asList("attribute"), Arrays.asList("ASC")));
        final TableInfo _infoExerciseAttributes = new TableInfo("exercise_attributes", _columnsExerciseAttributes, _foreignKeysExerciseAttributes, _indicesExerciseAttributes);
        final TableInfo _existingExerciseAttributes = TableInfo.read(db, "exercise_attributes");
        if (!_infoExerciseAttributes.equals(_existingExerciseAttributes)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_attributes(com.forge.data.local.entity.ExerciseAttributeEntity).\n"
                  + " Expected:\n" + _infoExerciseAttributes + "\n"
                  + " Found:\n" + _existingExerciseAttributes);
        }
        final HashMap<String, TableInfo.Column> _columnsExerciseFamilies = new HashMap<String, TableInfo.Column>(4);
        _columnsExerciseFamilies.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseFamilies.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseFamilies.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseFamilies.put("primary_pattern", new TableInfo.Column("primary_pattern", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExerciseFamilies = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExerciseFamilies = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExerciseFamilies = new TableInfo("exercise_families", _columnsExerciseFamilies, _foreignKeysExerciseFamilies, _indicesExerciseFamilies);
        final TableInfo _existingExerciseFamilies = TableInfo.read(db, "exercise_families");
        if (!_infoExerciseFamilies.equals(_existingExerciseFamilies)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_families(com.forge.data.local.entity.ExerciseFamilyEntity).\n"
                  + " Expected:\n" + _infoExerciseFamilies + "\n"
                  + " Found:\n" + _existingExerciseFamilies);
        }
        final HashMap<String, TableInfo.Column> _columnsExerciseFamilyMembers = new HashMap<String, TableInfo.Column>(3);
        _columnsExerciseFamilyMembers.put("family_id", new TableInfo.Column("family_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseFamilyMembers.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseFamilyMembers.put("is_canonical_lead", new TableInfo.Column("is_canonical_lead", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExerciseFamilyMembers = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysExerciseFamilyMembers.add(new TableInfo.ForeignKey("exercise_families", "CASCADE", "NO ACTION", Arrays.asList("family_id"), Arrays.asList("id")));
        _foreignKeysExerciseFamilyMembers.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExerciseFamilyMembers = new HashSet<TableInfo.Index>(1);
        _indicesExerciseFamilyMembers.add(new TableInfo.Index("index_exercise_family_members_exercise_id", false, Arrays.asList("exercise_id"), Arrays.asList("ASC")));
        final TableInfo _infoExerciseFamilyMembers = new TableInfo("exercise_family_members", _columnsExerciseFamilyMembers, _foreignKeysExerciseFamilyMembers, _indicesExerciseFamilyMembers);
        final TableInfo _existingExerciseFamilyMembers = TableInfo.read(db, "exercise_family_members");
        if (!_infoExerciseFamilyMembers.equals(_existingExerciseFamilyMembers)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_family_members(com.forge.data.local.entity.ExerciseFamilyMemberEntity).\n"
                  + " Expected:\n" + _infoExerciseFamilyMembers + "\n"
                  + " Found:\n" + _existingExerciseFamilyMembers);
        }
        final HashMap<String, TableInfo.Column> _columnsWorkoutSessions = new HashMap<String, TableInfo.Column>(12);
        _columnsWorkoutSessions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("routine_id", new TableInfo.Column("routine_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("start_time", new TableInfo.Column("start_time", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("end_time", new TableInfo.Column("end_time", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("active_exercise_id", new TableInfo.Column("active_exercise_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("active_set_index", new TableInfo.Column("active_set_index", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("total_volume_kg", new TableInfo.Column("total_volume_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("duration_seconds", new TableInfo.Column("duration_seconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSessions.put("last_updated_at", new TableInfo.Column("last_updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkoutSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWorkoutSessions = new HashSet<TableInfo.Index>(2);
        _indicesWorkoutSessions.add(new TableInfo.Index("index_workout_sessions_status", false, Arrays.asList("status"), Arrays.asList("ASC")));
        _indicesWorkoutSessions.add(new TableInfo.Index("index_workout_sessions_start_time", false, Arrays.asList("start_time"), Arrays.asList("ASC")));
        final TableInfo _infoWorkoutSessions = new TableInfo("workout_sessions", _columnsWorkoutSessions, _foreignKeysWorkoutSessions, _indicesWorkoutSessions);
        final TableInfo _existingWorkoutSessions = TableInfo.read(db, "workout_sessions");
        if (!_infoWorkoutSessions.equals(_existingWorkoutSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "workout_sessions(com.forge.data.local.entity.WorkoutSessionEntity).\n"
                  + " Expected:\n" + _infoWorkoutSessions + "\n"
                  + " Found:\n" + _existingWorkoutSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsWorkoutSets = new HashMap<String, TableInfo.Column>(14);
        _columnsWorkoutSets.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("session_id", new TableInfo.Column("session_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("set_order", new TableInfo.Column("set_order", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("set_type", new TableInfo.Column("set_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("weight_kg", new TableInfo.Column("weight_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("reps", new TableInfo.Column("reps", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("rpe", new TableInfo.Column("rpe", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("rir", new TableInfo.Column("rir", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("tempo", new TableInfo.Column("tempo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("rest_seconds_taken", new TableInfo.Column("rest_seconds_taken", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("is_completed", new TableInfo.Column("is_completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("is_personal_record", new TableInfo.Column("is_personal_record", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWorkoutSets.put("completed_at", new TableInfo.Column("completed_at", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWorkoutSets = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysWorkoutSets.add(new TableInfo.ForeignKey("workout_sessions", "CASCADE", "NO ACTION", Arrays.asList("session_id"), Arrays.asList("id")));
        _foreignKeysWorkoutSets.add(new TableInfo.ForeignKey("exercises", "RESTRICT", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesWorkoutSets = new HashSet<TableInfo.Index>(3);
        _indicesWorkoutSets.add(new TableInfo.Index("index_workout_sets_session_id", false, Arrays.asList("session_id"), Arrays.asList("ASC")));
        _indicesWorkoutSets.add(new TableInfo.Index("index_workout_sets_exercise_id", false, Arrays.asList("exercise_id"), Arrays.asList("ASC")));
        _indicesWorkoutSets.add(new TableInfo.Index("index_workout_sets_session_id_set_order", false, Arrays.asList("session_id", "set_order"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoWorkoutSets = new TableInfo("workout_sets", _columnsWorkoutSets, _foreignKeysWorkoutSets, _indicesWorkoutSets);
        final TableInfo _existingWorkoutSets = TableInfo.read(db, "workout_sets");
        if (!_infoWorkoutSets.equals(_existingWorkoutSets)) {
          return new RoomOpenHelper.ValidationResult(false, "workout_sets(com.forge.data.local.entity.WorkoutSetEntity).\n"
                  + " Expected:\n" + _infoWorkoutSets + "\n"
                  + " Found:\n" + _existingWorkoutSets);
        }
        final HashMap<String, TableInfo.Column> _columnsExerciseProgressionRecords = new HashMap<String, TableInfo.Column>(9);
        _columnsExerciseProgressionRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("session_id", new TableInfo.Column("session_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("recommended_weight_kg", new TableInfo.Column("recommended_weight_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("recommended_rep_min", new TableInfo.Column("recommended_rep_min", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("recommended_rep_max", new TableInfo.Column("recommended_rep_max", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("rationale", new TableInfo.Column("rationale", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("is_deload", new TableInfo.Column("is_deload", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExerciseProgressionRecords.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExerciseProgressionRecords = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysExerciseProgressionRecords.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExerciseProgressionRecords = new HashSet<TableInfo.Index>(2);
        _indicesExerciseProgressionRecords.add(new TableInfo.Index("index_exercise_progression_records_exercise_id", false, Arrays.asList("exercise_id"), Arrays.asList("ASC")));
        _indicesExerciseProgressionRecords.add(new TableInfo.Index("index_exercise_progression_records_session_id", false, Arrays.asList("session_id"), Arrays.asList("ASC")));
        final TableInfo _infoExerciseProgressionRecords = new TableInfo("exercise_progression_records", _columnsExerciseProgressionRecords, _foreignKeysExerciseProgressionRecords, _indicesExerciseProgressionRecords);
        final TableInfo _existingExerciseProgressionRecords = TableInfo.read(db, "exercise_progression_records");
        if (!_infoExerciseProgressionRecords.equals(_existingExerciseProgressionRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_progression_records(com.forge.data.local.entity.ExerciseProgressionRecordEntity).\n"
                  + " Expected:\n" + _infoExerciseProgressionRecords + "\n"
                  + " Found:\n" + _existingExerciseProgressionRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsExercisePersonalRecords = new HashMap<String, TableInfo.Column>(7);
        _columnsExercisePersonalRecords.put("exercise_id", new TableInfo.Column("exercise_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercisePersonalRecords.put("max_weight_kg", new TableInfo.Column("max_weight_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercisePersonalRecords.put("max_reps_at_max_weight", new TableInfo.Column("max_reps_at_max_weight", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercisePersonalRecords.put("estimated_1rm_kg", new TableInfo.Column("estimated_1rm_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercisePersonalRecords.put("best_set_volume_kg", new TableInfo.Column("best_set_volume_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercisePersonalRecords.put("best_session_volume_kg", new TableInfo.Column("best_session_volume_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExercisePersonalRecords.put("achieved_at", new TableInfo.Column("achieved_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExercisePersonalRecords = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysExercisePersonalRecords.add(new TableInfo.ForeignKey("exercises", "CASCADE", "NO ACTION", Arrays.asList("exercise_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesExercisePersonalRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExercisePersonalRecords = new TableInfo("exercise_personal_records", _columnsExercisePersonalRecords, _foreignKeysExercisePersonalRecords, _indicesExercisePersonalRecords);
        final TableInfo _existingExercisePersonalRecords = TableInfo.read(db, "exercise_personal_records");
        if (!_infoExercisePersonalRecords.equals(_existingExercisePersonalRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "exercise_personal_records(com.forge.data.local.entity.ExercisePersonalRecordEntity).\n"
                  + " Expected:\n" + _infoExercisePersonalRecords + "\n"
                  + " Found:\n" + _existingExercisePersonalRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsUserNutritionProfile = new HashMap<String, TableInfo.Column>(9);
        _columnsUserNutritionProfile.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("maintenance_calories", new TableInfo.Column("maintenance_calories", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("is_maintenance_manual", new TableInfo.Column("is_maintenance_manual", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("goal", new TableInfo.Column("goal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("target_calories", new TableInfo.Column("target_calories", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("protein_grams", new TableInfo.Column("protein_grams", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("fat_grams", new TableInfo.Column("fat_grams", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("carbs_grams", new TableInfo.Column("carbs_grams", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserNutritionProfile.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserNutritionProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserNutritionProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserNutritionProfile = new TableInfo("user_nutrition_profile", _columnsUserNutritionProfile, _foreignKeysUserNutritionProfile, _indicesUserNutritionProfile);
        final TableInfo _existingUserNutritionProfile = TableInfo.read(db, "user_nutrition_profile");
        if (!_infoUserNutritionProfile.equals(_existingUserNutritionProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "user_nutrition_profile(com.forge.data.local.entity.UserNutritionProfileEntity).\n"
                  + " Expected:\n" + _infoUserNutritionProfile + "\n"
                  + " Found:\n" + _existingUserNutritionProfile);
        }
        final HashMap<String, TableInfo.Column> _columnsWeightLogs = new HashMap<String, TableInfo.Column>(4);
        _columnsWeightLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightLogs.put("weight_kg", new TableInfo.Column("weight_kg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightLogs.put("logged_date", new TableInfo.Column("logged_date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightLogs.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWeightLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWeightLogs = new HashSet<TableInfo.Index>(1);
        _indicesWeightLogs.add(new TableInfo.Index("index_weight_logs_logged_date", true, Arrays.asList("logged_date"), Arrays.asList("ASC")));
        final TableInfo _infoWeightLogs = new TableInfo("weight_logs", _columnsWeightLogs, _foreignKeysWeightLogs, _indicesWeightLogs);
        final TableInfo _existingWeightLogs = TableInfo.read(db, "weight_logs");
        if (!_infoWeightLogs.equals(_existingWeightLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "weight_logs(com.forge.data.local.entity.WeightLogEntity).\n"
                  + " Expected:\n" + _infoWeightLogs + "\n"
                  + " Found:\n" + _existingWeightLogs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "967d9eb0400a0dd8a74575afb906640c", "545b65c9992dd31a5a959913a6156aa5");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(1);
    _shadowTablesMap.put("exercises_fts", "exercises");
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "exercises","exercises_fts","exercise_aliases","muscles","exercise_muscles","equipment","exercise_equipment","exercise_attributes","exercise_families","exercise_family_members","workout_sessions","workout_sets","exercise_progression_records","exercise_personal_records","user_nutrition_profile","weight_logs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `exercises`");
      _db.execSQL("DELETE FROM `exercises_fts`");
      _db.execSQL("DELETE FROM `exercise_aliases`");
      _db.execSQL("DELETE FROM `muscles`");
      _db.execSQL("DELETE FROM `exercise_muscles`");
      _db.execSQL("DELETE FROM `equipment`");
      _db.execSQL("DELETE FROM `exercise_equipment`");
      _db.execSQL("DELETE FROM `exercise_attributes`");
      _db.execSQL("DELETE FROM `exercise_families`");
      _db.execSQL("DELETE FROM `exercise_family_members`");
      _db.execSQL("DELETE FROM `workout_sessions`");
      _db.execSQL("DELETE FROM `workout_sets`");
      _db.execSQL("DELETE FROM `exercise_progression_records`");
      _db.execSQL("DELETE FROM `exercise_personal_records`");
      _db.execSQL("DELETE FROM `user_nutrition_profile`");
      _db.execSQL("DELETE FROM `weight_logs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ExerciseDao.class, ExerciseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WorkoutDao.class, WorkoutDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ExerciseDao exerciseDao() {
    if (_exerciseDao != null) {
      return _exerciseDao;
    } else {
      synchronized(this) {
        if(_exerciseDao == null) {
          _exerciseDao = new ExerciseDao_Impl(this);
        }
        return _exerciseDao;
      }
    }
  }

  @Override
  public WorkoutDao workoutDao() {
    if (_workoutDao != null) {
      return _workoutDao;
    } else {
      synchronized(this) {
        if(_workoutDao == null) {
          _workoutDao = new WorkoutDao_Impl(this);
        }
        return _workoutDao;
      }
    }
  }
}
