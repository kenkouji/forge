package com.forge.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.forge.data.local.entity.EquipmentEntity;
import com.forge.data.local.entity.ExerciseAliasEntity;
import com.forge.data.local.entity.ExerciseAttributeEntity;
import com.forge.data.local.entity.ExerciseEntity;
import com.forge.data.local.entity.ExerciseEquipmentEntity;
import com.forge.data.local.entity.ExerciseFamilyEntity;
import com.forge.data.local.entity.ExerciseFamilyMemberEntity;
import com.forge.data.local.entity.ExerciseMuscleEntity;
import com.forge.data.local.entity.ExercisePersonalRecordEntity;
import com.forge.data.local.entity.ExerciseProgressionRecordEntity;
import com.forge.data.local.entity.MuscleEntity;
import com.forge.data.local.entity.UserNutritionProfileEntity;
import com.forge.data.local.entity.WeightLogEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ExerciseDao_Impl implements ExerciseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ExerciseEntity> __insertionAdapterOfExerciseEntity;

  private final EntityInsertionAdapter<ExerciseAliasEntity> __insertionAdapterOfExerciseAliasEntity;

  private final EntityInsertionAdapter<MuscleEntity> __insertionAdapterOfMuscleEntity;

  private final EntityInsertionAdapter<ExerciseMuscleEntity> __insertionAdapterOfExerciseMuscleEntity;

  private final EntityInsertionAdapter<EquipmentEntity> __insertionAdapterOfEquipmentEntity;

  private final EntityInsertionAdapter<ExerciseEquipmentEntity> __insertionAdapterOfExerciseEquipmentEntity;

  private final EntityInsertionAdapter<ExerciseAttributeEntity> __insertionAdapterOfExerciseAttributeEntity;

  private final EntityInsertionAdapter<ExerciseFamilyEntity> __insertionAdapterOfExerciseFamilyEntity;

  private final EntityInsertionAdapter<ExerciseFamilyMemberEntity> __insertionAdapterOfExerciseFamilyMemberEntity;

  private final EntityInsertionAdapter<ExerciseProgressionRecordEntity> __insertionAdapterOfExerciseProgressionRecordEntity;

  private final EntityInsertionAdapter<ExercisePersonalRecordEntity> __insertionAdapterOfExercisePersonalRecordEntity;

  private final EntityInsertionAdapter<UserNutritionProfileEntity> __insertionAdapterOfUserNutritionProfileEntity;

  private final EntityInsertionAdapter<WeightLogEntity> __insertionAdapterOfWeightLogEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateExercisePopularity;

  private final SharedSQLiteStatement __preparedStmtOfSetExerciseFavorite;

  public ExerciseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExerciseEntity = new EntityInsertionAdapter<ExerciseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercises` (`id`,`name`,`canonical_name`,`movement_pattern`,`mechanic`,`force_type`,`experience_level`,`instructions`,`form_cues`,`common_mistakes`,`youtube_video_id`,`is_custom`,`source`,`source_id`,`source_category`,`source_force`,`source_level`,`source_mechanic`,`source_equipment`,`forge_movement_pattern`,`forge_exercise_family_id`,`search_tokens`,`license`,`is_popular`,`popularity_rank`,`is_favorite`,`created_at`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getCanonicalName());
        statement.bindString(4, entity.getMovementPattern());
        statement.bindString(5, entity.getMechanic());
        statement.bindString(6, entity.getForceType());
        statement.bindString(7, entity.getExperienceLevel());
        statement.bindString(8, entity.getInstructions());
        statement.bindString(9, entity.getFormCues());
        statement.bindString(10, entity.getCommonMistakes());
        if (entity.getYoutubeVideoId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getYoutubeVideoId());
        }
        final int _tmp = entity.isCustom() ? 1 : 0;
        statement.bindLong(12, _tmp);
        statement.bindString(13, entity.getSource());
        if (entity.getSourceId() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getSourceId());
        }
        if (entity.getSourceCategory() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getSourceCategory());
        }
        if (entity.getSourceForce() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getSourceForce());
        }
        if (entity.getSourceLevel() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getSourceLevel());
        }
        if (entity.getSourceMechanic() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getSourceMechanic());
        }
        if (entity.getSourceEquipment() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getSourceEquipment());
        }
        statement.bindString(20, entity.getForgeMovementPattern());
        if (entity.getForgeExerciseFamilyId() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getForgeExerciseFamilyId());
        }
        statement.bindString(22, entity.getSearchTokens());
        statement.bindString(23, entity.getLicense());
        final int _tmp_1 = entity.isPopular() ? 1 : 0;
        statement.bindLong(24, _tmp_1);
        statement.bindLong(25, entity.getPopularityRank());
        final int _tmp_2 = entity.isFavorite() ? 1 : 0;
        statement.bindLong(26, _tmp_2);
        statement.bindLong(27, entity.getCreatedAt());
        statement.bindLong(28, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfExerciseAliasEntity = new EntityInsertionAdapter<ExerciseAliasEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_aliases` (`id`,`exercise_id`,`alias`,`is_forge_derived`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseAliasEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getExerciseId());
        statement.bindString(3, entity.getAlias());
        final int _tmp = entity.isForgeDerived() ? 1 : 0;
        statement.bindLong(4, _tmp);
      }
    };
    this.__insertionAdapterOfMuscleEntity = new EntityInsertionAdapter<MuscleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `muscles` (`id`,`name`,`body_part`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MuscleEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getBodyPart());
      }
    };
    this.__insertionAdapterOfExerciseMuscleEntity = new EntityInsertionAdapter<ExerciseMuscleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_muscles` (`exercise_id`,`muscle_id`,`role`,`is_forge_derived`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseMuscleEntity entity) {
        statement.bindString(1, entity.getExerciseId());
        statement.bindString(2, entity.getMuscleId());
        statement.bindString(3, entity.getRole());
        final int _tmp = entity.isForgeDerived() ? 1 : 0;
        statement.bindLong(4, _tmp);
      }
    };
    this.__insertionAdapterOfEquipmentEntity = new EntityInsertionAdapter<EquipmentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `equipment` (`id`,`name`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EquipmentEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
      }
    };
    this.__insertionAdapterOfExerciseEquipmentEntity = new EntityInsertionAdapter<ExerciseEquipmentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_equipment` (`exercise_id`,`equipment_id`,`is_primary`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseEquipmentEntity entity) {
        statement.bindString(1, entity.getExerciseId());
        statement.bindString(2, entity.getEquipmentId());
        final int _tmp = entity.isPrimary() ? 1 : 0;
        statement.bindLong(3, _tmp);
      }
    };
    this.__insertionAdapterOfExerciseAttributeEntity = new EntityInsertionAdapter<ExerciseAttributeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_attributes` (`exercise_id`,`attribute`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseAttributeEntity entity) {
        statement.bindString(1, entity.getExerciseId());
        statement.bindString(2, entity.getAttribute());
      }
    };
    this.__insertionAdapterOfExerciseFamilyEntity = new EntityInsertionAdapter<ExerciseFamilyEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_families` (`id`,`name`,`description`,`primary_pattern`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseFamilyEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getPrimaryPattern());
      }
    };
    this.__insertionAdapterOfExerciseFamilyMemberEntity = new EntityInsertionAdapter<ExerciseFamilyMemberEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_family_members` (`family_id`,`exercise_id`,`is_canonical_lead`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseFamilyMemberEntity entity) {
        statement.bindString(1, entity.getFamilyId());
        statement.bindString(2, entity.getExerciseId());
        final int _tmp = entity.isCanonicalLead() ? 1 : 0;
        statement.bindLong(3, _tmp);
      }
    };
    this.__insertionAdapterOfExerciseProgressionRecordEntity = new EntityInsertionAdapter<ExerciseProgressionRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_progression_records` (`id`,`exercise_id`,`session_id`,`recommended_weight_kg`,`recommended_rep_min`,`recommended_rep_max`,`rationale`,`is_deload`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExerciseProgressionRecordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getExerciseId());
        statement.bindString(3, entity.getSessionId());
        statement.bindDouble(4, entity.getRecommendedWeightKg());
        statement.bindLong(5, entity.getRecommendedRepMin());
        statement.bindLong(6, entity.getRecommendedRepMax());
        statement.bindString(7, entity.getRationale());
        final int _tmp = entity.isDeload() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfExercisePersonalRecordEntity = new EntityInsertionAdapter<ExercisePersonalRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exercise_personal_records` (`exercise_id`,`max_weight_kg`,`max_reps_at_max_weight`,`estimated_1rm_kg`,`best_set_volume_kg`,`best_session_volume_kg`,`achieved_at`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExercisePersonalRecordEntity entity) {
        statement.bindString(1, entity.getExerciseId());
        statement.bindDouble(2, entity.getMaxWeightKg());
        statement.bindLong(3, entity.getMaxRepsAtMaxWeight());
        statement.bindDouble(4, entity.getEstimated1RmKg());
        statement.bindDouble(5, entity.getBestSetVolumeKg());
        statement.bindDouble(6, entity.getBestSessionVolumeKg());
        statement.bindLong(7, entity.getAchievedAt());
      }
    };
    this.__insertionAdapterOfUserNutritionProfileEntity = new EntityInsertionAdapter<UserNutritionProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_nutrition_profile` (`id`,`maintenance_calories`,`is_maintenance_manual`,`goal`,`target_calories`,`protein_grams`,`fat_grams`,`carbs_grams`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserNutritionProfileEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getMaintenanceCalories());
        final int _tmp = entity.isMaintenanceManual() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getGoal());
        statement.bindLong(5, entity.getTargetCalories());
        statement.bindLong(6, entity.getProteinGrams());
        statement.bindLong(7, entity.getFatGrams());
        statement.bindLong(8, entity.getCarbsGrams());
        statement.bindLong(9, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfWeightLogEntity = new EntityInsertionAdapter<WeightLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `weight_logs` (`id`,`weight_kg`,`logged_date`,`created_at`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WeightLogEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getWeightKg());
        statement.bindString(3, entity.getLoggedDate());
        statement.bindLong(4, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfUpdateExercisePopularity = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE exercises SET is_popular = ?, popularity_rank = ?, youtube_video_id = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetExerciseFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE exercises SET is_favorite = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertExercises(final List<ExerciseEntity> exercises,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseEntity.insert(exercises);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExercise(final ExerciseEntity exercise,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseEntity.insert(exercise);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAliases(final List<ExerciseAliasEntity> aliases,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseAliasEntity.insert(aliases);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMuscles(final List<MuscleEntity> muscles,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMuscleEntity.insert(muscles);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExerciseMuscles(final List<ExerciseMuscleEntity> exerciseMuscles,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseMuscleEntity.insert(exerciseMuscles);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertEquipment(final List<EquipmentEntity> equipment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEquipmentEntity.insert(equipment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExerciseEquipment(final List<ExerciseEquipmentEntity> exerciseEquipment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseEquipmentEntity.insert(exerciseEquipment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExerciseAttributes(final List<ExerciseAttributeEntity> attributes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseAttributeEntity.insert(attributes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExerciseFamilies(final List<ExerciseFamilyEntity> families,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseFamilyEntity.insert(families);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertExerciseFamilyMembers(final List<ExerciseFamilyMemberEntity> members,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseFamilyMemberEntity.insert(members);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertProgressionRecord(final ExerciseProgressionRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExerciseProgressionRecordEntity.insert(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPersonalRecord(final ExercisePersonalRecordEntity pr,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExercisePersonalRecordEntity.insert(pr);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertOrUpdateNutritionProfile(final UserNutritionProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserNutritionProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertWeightLog(final WeightLogEntity log,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWeightLogEntity.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object seedRelationalDatabase(final List<MuscleEntity> muscles,
      final List<EquipmentEntity> equipment, final List<ExerciseFamilyEntity> families,
      final List<ExerciseEntity> exercises, final List<ExerciseMuscleEntity> exerciseMuscles,
      final List<ExerciseEquipmentEntity> exerciseEquipment,
      final List<ExerciseAttributeEntity> exerciseAttributes,
      final List<ExerciseFamilyMemberEntity> exerciseFamilyMembers,
      final List<ExerciseAliasEntity> aliases, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> ExerciseDao.DefaultImpls.seedRelationalDatabase(ExerciseDao_Impl.this, muscles, equipment, families, exercises, exerciseMuscles, exerciseEquipment, exerciseAttributes, exerciseFamilyMembers, aliases, __cont), $completion);
  }

  @Override
  public Object updateExercisePopularity(final String exerciseId, final boolean isPopular,
      final int rank, final String youtubeId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateExercisePopularity.acquire();
        int _argIndex = 1;
        final int _tmp = isPopular ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, rank);
        _argIndex = 3;
        if (youtubeId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, youtubeId);
        }
        _argIndex = 4;
        _stmt.bindString(_argIndex, exerciseId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateExercisePopularity.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setExerciseFavorite(final String exerciseId, final boolean isFavorite,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetExerciseFavorite.acquire();
        int _argIndex = 1;
        final int _tmp = isFavorite ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, exerciseId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetExerciseFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExerciseEntity>> getAllExercises() {
    final String _sql = "SELECT * FROM exercises ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercises"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
          final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
          final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
          final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
          final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
          final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
          final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
          final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
          final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
          final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
          final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
          final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
          final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
          final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
          final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
          final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
          final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
          final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCanonicalName;
            _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
            final String _tmpMovementPattern;
            _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
            final String _tmpMechanic;
            _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
            final String _tmpForceType;
            _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
            final String _tmpExperienceLevel;
            _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpFormCues;
            _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
            final String _tmpCommonMistakes;
            _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
            final String _tmpYoutubeVideoId;
            if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
              _tmpYoutubeVideoId = null;
            } else {
              _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
            }
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpSourceId;
            if (_cursor.isNull(_cursorIndexOfSourceId)) {
              _tmpSourceId = null;
            } else {
              _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            }
            final String _tmpSourceCategory;
            if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
              _tmpSourceCategory = null;
            } else {
              _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
            }
            final String _tmpSourceForce;
            if (_cursor.isNull(_cursorIndexOfSourceForce)) {
              _tmpSourceForce = null;
            } else {
              _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
            }
            final String _tmpSourceLevel;
            if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
              _tmpSourceLevel = null;
            } else {
              _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
            }
            final String _tmpSourceMechanic;
            if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
              _tmpSourceMechanic = null;
            } else {
              _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
            }
            final String _tmpSourceEquipment;
            if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
              _tmpSourceEquipment = null;
            } else {
              _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
            }
            final String _tmpForgeMovementPattern;
            _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
            final String _tmpForgeExerciseFamilyId;
            if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
              _tmpForgeExerciseFamilyId = null;
            } else {
              _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
            }
            final String _tmpSearchTokens;
            _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
            final String _tmpLicense;
            _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
            final boolean _tmpIsPopular;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
            _tmpIsPopular = _tmp_1 != 0;
            final int _tmpPopularityRank;
            _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<ExerciseEntity> getExerciseById(final String id) {
    final String _sql = "SELECT * FROM exercises WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercises"}, new Callable<ExerciseEntity>() {
      @Override
      @Nullable
      public ExerciseEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
          final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
          final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
          final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
          final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
          final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
          final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
          final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
          final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
          final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
          final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
          final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
          final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
          final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
          final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
          final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
          final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
          final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final ExerciseEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCanonicalName;
            _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
            final String _tmpMovementPattern;
            _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
            final String _tmpMechanic;
            _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
            final String _tmpForceType;
            _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
            final String _tmpExperienceLevel;
            _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpFormCues;
            _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
            final String _tmpCommonMistakes;
            _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
            final String _tmpYoutubeVideoId;
            if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
              _tmpYoutubeVideoId = null;
            } else {
              _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
            }
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpSourceId;
            if (_cursor.isNull(_cursorIndexOfSourceId)) {
              _tmpSourceId = null;
            } else {
              _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            }
            final String _tmpSourceCategory;
            if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
              _tmpSourceCategory = null;
            } else {
              _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
            }
            final String _tmpSourceForce;
            if (_cursor.isNull(_cursorIndexOfSourceForce)) {
              _tmpSourceForce = null;
            } else {
              _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
            }
            final String _tmpSourceLevel;
            if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
              _tmpSourceLevel = null;
            } else {
              _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
            }
            final String _tmpSourceMechanic;
            if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
              _tmpSourceMechanic = null;
            } else {
              _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
            }
            final String _tmpSourceEquipment;
            if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
              _tmpSourceEquipment = null;
            } else {
              _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
            }
            final String _tmpForgeMovementPattern;
            _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
            final String _tmpForgeExerciseFamilyId;
            if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
              _tmpForgeExerciseFamilyId = null;
            } else {
              _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
            }
            final String _tmpSearchTokens;
            _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
            final String _tmpLicense;
            _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
            final boolean _tmpIsPopular;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
            _tmpIsPopular = _tmp_1 != 0;
            final int _tmpPopularityRank;
            _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getExerciseCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM exercises";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExerciseEntity>> searchExercises(final String ftsQuery, final String rawQuery) {
    final String _sql = "\n"
            + "        SELECT * FROM exercises WHERE rowid IN (\n"
            + "            SELECT rowid FROM exercises_fts WHERE exercises_fts MATCH ?\n"
            + "        )\n"
            + "        UNION\n"
            + "        SELECT exercises.* FROM exercises\n"
            + "        INNER JOIN exercise_aliases ON exercises.id = exercise_aliases.exercise_id\n"
            + "        WHERE exercise_aliases.alias LIKE '%' || ? || '%'\n"
            + "        UNION\n"
            + "        SELECT * FROM exercises\n"
            + "        WHERE name LIKE '%' || ? || '%'\n"
            + "        ORDER BY name ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, ftsQuery);
    _argIndex = 2;
    _statement.bindString(_argIndex, rawQuery);
    _argIndex = 3;
    _statement.bindString(_argIndex, rawQuery);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"exercises", "exercises_fts",
        "exercise_aliases"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
            final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
            final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
            final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
            final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
            final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
            final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
            final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
            final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
            final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
            final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
            final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
            final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
            final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
            final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
            final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
            final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
            final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
            final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
            final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
            final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
            final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
            final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
            final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final ExerciseEntity _item;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final String _tmpCanonicalName;
              _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
              final String _tmpMovementPattern;
              _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
              final String _tmpMechanic;
              _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
              final String _tmpForceType;
              _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
              final String _tmpExperienceLevel;
              _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
              final String _tmpInstructions;
              _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
              final String _tmpFormCues;
              _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
              final String _tmpCommonMistakes;
              _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
              final String _tmpYoutubeVideoId;
              if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
                _tmpYoutubeVideoId = null;
              } else {
                _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
              }
              final boolean _tmpIsCustom;
              final int _tmp;
              _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
              _tmpIsCustom = _tmp != 0;
              final String _tmpSource;
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
              final String _tmpSourceId;
              if (_cursor.isNull(_cursorIndexOfSourceId)) {
                _tmpSourceId = null;
              } else {
                _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
              }
              final String _tmpSourceCategory;
              if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
                _tmpSourceCategory = null;
              } else {
                _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
              }
              final String _tmpSourceForce;
              if (_cursor.isNull(_cursorIndexOfSourceForce)) {
                _tmpSourceForce = null;
              } else {
                _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
              }
              final String _tmpSourceLevel;
              if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
                _tmpSourceLevel = null;
              } else {
                _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
              }
              final String _tmpSourceMechanic;
              if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
                _tmpSourceMechanic = null;
              } else {
                _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
              }
              final String _tmpSourceEquipment;
              if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
                _tmpSourceEquipment = null;
              } else {
                _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
              }
              final String _tmpForgeMovementPattern;
              _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
              final String _tmpForgeExerciseFamilyId;
              if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
                _tmpForgeExerciseFamilyId = null;
              } else {
                _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
              }
              final String _tmpSearchTokens;
              _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
              final String _tmpLicense;
              _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
              final boolean _tmpIsPopular;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
              _tmpIsPopular = _tmp_1 != 0;
              final int _tmpPopularityRank;
              _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
              final boolean _tmpIsFavorite;
              final int _tmp_2;
              _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
              _tmpIsFavorite = _tmp_2 != 0;
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpUpdatedAt;
              _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
              _item = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExerciseEntity>> searchAndFilterExercises(final int hasQuery,
      final String ftsQuery, final String rawQuery, final String muscleId, final String equipmentId,
      final String movementPattern, final String experienceLevel, final String familyId) {
    final String _sql = "\n"
            + "        SELECT DISTINCT e.* FROM exercises e\n"
            + "        LEFT JOIN exercise_muscles em ON e.id = em.exercise_id\n"
            + "        LEFT JOIN exercise_equipment eq ON e.id = eq.exercise_id\n"
            + "        LEFT JOIN exercise_family_members efm ON e.id = efm.exercise_id\n"
            + "        WHERE (? = 0 OR e.rowid IN (SELECT rowid FROM exercises_fts WHERE exercises_fts MATCH ?) OR e.id IN (SELECT exercise_id FROM exercise_aliases WHERE alias LIKE '%' || ? || '%'))\n"
            + "          AND (? IS NULL OR em.muscle_id = ?)\n"
            + "          AND (? IS NULL OR eq.equipment_id = ?)\n"
            + "          AND (? IS NULL OR e.forge_movement_pattern = ?)\n"
            + "          AND (? IS NULL OR e.source_level = ?)\n"
            + "          AND (? IS NULL OR efm.family_id = ?)\n"
            + "        ORDER BY e.name ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 13);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, hasQuery);
    _argIndex = 2;
    _statement.bindString(_argIndex, ftsQuery);
    _argIndex = 3;
    _statement.bindString(_argIndex, rawQuery);
    _argIndex = 4;
    if (muscleId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, muscleId);
    }
    _argIndex = 5;
    if (muscleId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, muscleId);
    }
    _argIndex = 6;
    if (equipmentId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, equipmentId);
    }
    _argIndex = 7;
    if (equipmentId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, equipmentId);
    }
    _argIndex = 8;
    if (movementPattern == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, movementPattern);
    }
    _argIndex = 9;
    if (movementPattern == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, movementPattern);
    }
    _argIndex = 10;
    if (experienceLevel == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, experienceLevel);
    }
    _argIndex = 11;
    if (experienceLevel == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, experienceLevel);
    }
    _argIndex = 12;
    if (familyId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, familyId);
    }
    _argIndex = 13;
    if (familyId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, familyId);
    }
    return CoroutinesRoom.createFlow(__db, true, new String[] {"exercises", "exercise_muscles",
        "exercise_equipment", "exercise_family_members", "exercises_fts",
        "exercise_aliases"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
            final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
            final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
            final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
            final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
            final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
            final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
            final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
            final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
            final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
            final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
            final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
            final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
            final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
            final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
            final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
            final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
            final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
            final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
            final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
            final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
            final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
            final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
            final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
            final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final ExerciseEntity _item;
              final String _tmpId;
              _tmpId = _cursor.getString(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final String _tmpCanonicalName;
              _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
              final String _tmpMovementPattern;
              _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
              final String _tmpMechanic;
              _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
              final String _tmpForceType;
              _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
              final String _tmpExperienceLevel;
              _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
              final String _tmpInstructions;
              _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
              final String _tmpFormCues;
              _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
              final String _tmpCommonMistakes;
              _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
              final String _tmpYoutubeVideoId;
              if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
                _tmpYoutubeVideoId = null;
              } else {
                _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
              }
              final boolean _tmpIsCustom;
              final int _tmp;
              _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
              _tmpIsCustom = _tmp != 0;
              final String _tmpSource;
              _tmpSource = _cursor.getString(_cursorIndexOfSource);
              final String _tmpSourceId;
              if (_cursor.isNull(_cursorIndexOfSourceId)) {
                _tmpSourceId = null;
              } else {
                _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
              }
              final String _tmpSourceCategory;
              if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
                _tmpSourceCategory = null;
              } else {
                _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
              }
              final String _tmpSourceForce;
              if (_cursor.isNull(_cursorIndexOfSourceForce)) {
                _tmpSourceForce = null;
              } else {
                _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
              }
              final String _tmpSourceLevel;
              if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
                _tmpSourceLevel = null;
              } else {
                _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
              }
              final String _tmpSourceMechanic;
              if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
                _tmpSourceMechanic = null;
              } else {
                _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
              }
              final String _tmpSourceEquipment;
              if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
                _tmpSourceEquipment = null;
              } else {
                _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
              }
              final String _tmpForgeMovementPattern;
              _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
              final String _tmpForgeExerciseFamilyId;
              if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
                _tmpForgeExerciseFamilyId = null;
              } else {
                _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
              }
              final String _tmpSearchTokens;
              _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
              final String _tmpLicense;
              _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
              final boolean _tmpIsPopular;
              final int _tmp_1;
              _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
              _tmpIsPopular = _tmp_1 != 0;
              final int _tmpPopularityRank;
              _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
              final boolean _tmpIsFavorite;
              final int _tmp_2;
              _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
              _tmpIsFavorite = _tmp_2 != 0;
              final long _tmpCreatedAt;
              _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
              final long _tmpUpdatedAt;
              _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
              _item = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExerciseMuscleDetail>> getMusclesForExercise(final String exerciseId) {
    final String _sql = "\n"
            + "        SELECT em.muscle_id AS muscleId, m.name AS muscleName, m.body_part AS bodyPart, em.role AS role, em.is_forge_derived AS isForgeDerived\n"
            + "        FROM exercise_muscles em\n"
            + "        JOIN muscles m ON em.muscle_id = m.id\n"
            + "        WHERE em.exercise_id = ?\n"
            + "        ORDER BY CASE WHEN em.role = 'PRIMARY' THEN 0 ELSE 1 END, m.name ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_muscles",
        "muscles"}, new Callable<List<ExerciseMuscleDetail>>() {
      @Override
      @NonNull
      public List<ExerciseMuscleDetail> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMuscleId = 0;
          final int _cursorIndexOfMuscleName = 1;
          final int _cursorIndexOfBodyPart = 2;
          final int _cursorIndexOfRole = 3;
          final int _cursorIndexOfIsForgeDerived = 4;
          final List<ExerciseMuscleDetail> _result = new ArrayList<ExerciseMuscleDetail>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseMuscleDetail _item;
            final String _tmpMuscleId;
            _tmpMuscleId = _cursor.getString(_cursorIndexOfMuscleId);
            final String _tmpMuscleName;
            _tmpMuscleName = _cursor.getString(_cursorIndexOfMuscleName);
            final String _tmpBodyPart;
            _tmpBodyPart = _cursor.getString(_cursorIndexOfBodyPart);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final boolean _tmpIsForgeDerived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsForgeDerived);
            _tmpIsForgeDerived = _tmp != 0;
            _item = new ExerciseMuscleDetail(_tmpMuscleId,_tmpMuscleName,_tmpBodyPart,_tmpRole,_tmpIsForgeDerived);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<EquipmentEntity>> getEquipmentForExercise(final String exerciseId) {
    final String _sql = "\n"
            + "        SELECT eq.* FROM equipment eq\n"
            + "        JOIN exercise_equipment ee ON eq.id = ee.equipment_id\n"
            + "        WHERE ee.exercise_id = ?\n"
            + "        ORDER BY eq.name ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"equipment",
        "exercise_equipment"}, new Callable<List<EquipmentEntity>>() {
      @Override
      @NonNull
      public List<EquipmentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final List<EquipmentEntity> _result = new ArrayList<EquipmentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EquipmentEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            _item = new EquipmentEntity(_tmpId,_tmpName);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<String>> getAttributesForExercise(final String exerciseId) {
    final String _sql = "SELECT attribute FROM exercise_attributes WHERE exercise_id = ? ORDER BY attribute ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_attributes"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<ExerciseFamilyEntity> getFamilyForExercise(final String exerciseId) {
    final String _sql = "\n"
            + "        SELECT ef.* FROM exercise_families ef\n"
            + "        JOIN exercise_family_members efm ON ef.id = efm.family_id\n"
            + "        WHERE efm.exercise_id = ?\n"
            + "        LIMIT 1\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_families",
        "exercise_family_members"}, new Callable<ExerciseFamilyEntity>() {
      @Override
      @Nullable
      public ExerciseFamilyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPrimaryPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "primary_pattern");
          final ExerciseFamilyEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpPrimaryPattern;
            _tmpPrimaryPattern = _cursor.getString(_cursorIndexOfPrimaryPattern);
            _result = new ExerciseFamilyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPrimaryPattern);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExerciseEntity>> getFamilySiblings(final String familyId,
      final String currentExerciseId) {
    final String _sql = "\n"
            + "        SELECT e.* FROM exercises e\n"
            + "        JOIN exercise_family_members efm ON e.id = efm.exercise_id\n"
            + "        WHERE efm.family_id = ? AND e.id != ?\n"
            + "        ORDER BY e.name ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, familyId);
    _argIndex = 2;
    _statement.bindString(_argIndex, currentExerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercises",
        "exercise_family_members"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
          final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
          final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
          final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
          final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
          final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
          final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
          final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
          final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
          final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
          final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
          final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
          final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
          final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
          final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
          final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
          final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
          final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCanonicalName;
            _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
            final String _tmpMovementPattern;
            _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
            final String _tmpMechanic;
            _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
            final String _tmpForceType;
            _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
            final String _tmpExperienceLevel;
            _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpFormCues;
            _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
            final String _tmpCommonMistakes;
            _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
            final String _tmpYoutubeVideoId;
            if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
              _tmpYoutubeVideoId = null;
            } else {
              _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
            }
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpSourceId;
            if (_cursor.isNull(_cursorIndexOfSourceId)) {
              _tmpSourceId = null;
            } else {
              _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            }
            final String _tmpSourceCategory;
            if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
              _tmpSourceCategory = null;
            } else {
              _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
            }
            final String _tmpSourceForce;
            if (_cursor.isNull(_cursorIndexOfSourceForce)) {
              _tmpSourceForce = null;
            } else {
              _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
            }
            final String _tmpSourceLevel;
            if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
              _tmpSourceLevel = null;
            } else {
              _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
            }
            final String _tmpSourceMechanic;
            if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
              _tmpSourceMechanic = null;
            } else {
              _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
            }
            final String _tmpSourceEquipment;
            if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
              _tmpSourceEquipment = null;
            } else {
              _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
            }
            final String _tmpForgeMovementPattern;
            _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
            final String _tmpForgeExerciseFamilyId;
            if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
              _tmpForgeExerciseFamilyId = null;
            } else {
              _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
            }
            final String _tmpSearchTokens;
            _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
            final String _tmpLicense;
            _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
            final boolean _tmpIsPopular;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
            _tmpIsPopular = _tmp_1 != 0;
            final int _tmpPopularityRank;
            _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<MuscleEntity>> getAllMuscles() {
    final String _sql = "SELECT * FROM muscles ORDER BY body_part ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"muscles"}, new Callable<List<MuscleEntity>>() {
      @Override
      @NonNull
      public List<MuscleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBodyPart = CursorUtil.getColumnIndexOrThrow(_cursor, "body_part");
          final List<MuscleEntity> _result = new ArrayList<MuscleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MuscleEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBodyPart;
            _tmpBodyPart = _cursor.getString(_cursorIndexOfBodyPart);
            _item = new MuscleEntity(_tmpId,_tmpName,_tmpBodyPart);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<EquipmentEntity>> getAllEquipment() {
    final String _sql = "SELECT * FROM equipment ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"equipment"}, new Callable<List<EquipmentEntity>>() {
      @Override
      @NonNull
      public List<EquipmentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final List<EquipmentEntity> _result = new ArrayList<EquipmentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EquipmentEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            _item = new EquipmentEntity(_tmpId,_tmpName);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExerciseFamilyEntity>> getAllFamilies() {
    final String _sql = "SELECT * FROM exercise_families ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_families"}, new Callable<List<ExerciseFamilyEntity>>() {
      @Override
      @NonNull
      public List<ExerciseFamilyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPrimaryPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "primary_pattern");
          final List<ExerciseFamilyEntity> _result = new ArrayList<ExerciseFamilyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseFamilyEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpPrimaryPattern;
            _tmpPrimaryPattern = _cursor.getString(_cursorIndexOfPrimaryPattern);
            _item = new ExerciseFamilyEntity(_tmpId,_tmpName,_tmpDescription,_tmpPrimaryPattern);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExerciseEntity>> getPopularExercises() {
    final String _sql = "SELECT * FROM exercises WHERE is_popular = 1 ORDER BY popularity_rank ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercises"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
          final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
          final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
          final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
          final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
          final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
          final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
          final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
          final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
          final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
          final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
          final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
          final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
          final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
          final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
          final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
          final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
          final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCanonicalName;
            _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
            final String _tmpMovementPattern;
            _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
            final String _tmpMechanic;
            _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
            final String _tmpForceType;
            _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
            final String _tmpExperienceLevel;
            _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpFormCues;
            _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
            final String _tmpCommonMistakes;
            _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
            final String _tmpYoutubeVideoId;
            if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
              _tmpYoutubeVideoId = null;
            } else {
              _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
            }
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpSourceId;
            if (_cursor.isNull(_cursorIndexOfSourceId)) {
              _tmpSourceId = null;
            } else {
              _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            }
            final String _tmpSourceCategory;
            if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
              _tmpSourceCategory = null;
            } else {
              _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
            }
            final String _tmpSourceForce;
            if (_cursor.isNull(_cursorIndexOfSourceForce)) {
              _tmpSourceForce = null;
            } else {
              _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
            }
            final String _tmpSourceLevel;
            if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
              _tmpSourceLevel = null;
            } else {
              _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
            }
            final String _tmpSourceMechanic;
            if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
              _tmpSourceMechanic = null;
            } else {
              _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
            }
            final String _tmpSourceEquipment;
            if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
              _tmpSourceEquipment = null;
            } else {
              _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
            }
            final String _tmpForgeMovementPattern;
            _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
            final String _tmpForgeExerciseFamilyId;
            if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
              _tmpForgeExerciseFamilyId = null;
            } else {
              _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
            }
            final String _tmpSearchTokens;
            _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
            final String _tmpLicense;
            _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
            final boolean _tmpIsPopular;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
            _tmpIsPopular = _tmp_1 != 0;
            final int _tmpPopularityRank;
            _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPopularExercisesCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM exercises WHERE is_popular = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExerciseEntity>> getFavoriteExercises() {
    final String _sql = "SELECT * FROM exercises WHERE is_favorite = 1 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercises"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
          final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
          final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
          final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
          final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
          final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
          final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
          final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
          final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
          final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
          final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
          final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
          final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
          final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
          final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
          final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
          final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
          final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCanonicalName;
            _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
            final String _tmpMovementPattern;
            _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
            final String _tmpMechanic;
            _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
            final String _tmpForceType;
            _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
            final String _tmpExperienceLevel;
            _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpFormCues;
            _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
            final String _tmpCommonMistakes;
            _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
            final String _tmpYoutubeVideoId;
            if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
              _tmpYoutubeVideoId = null;
            } else {
              _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
            }
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpSourceId;
            if (_cursor.isNull(_cursorIndexOfSourceId)) {
              _tmpSourceId = null;
            } else {
              _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            }
            final String _tmpSourceCategory;
            if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
              _tmpSourceCategory = null;
            } else {
              _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
            }
            final String _tmpSourceForce;
            if (_cursor.isNull(_cursorIndexOfSourceForce)) {
              _tmpSourceForce = null;
            } else {
              _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
            }
            final String _tmpSourceLevel;
            if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
              _tmpSourceLevel = null;
            } else {
              _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
            }
            final String _tmpSourceMechanic;
            if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
              _tmpSourceMechanic = null;
            } else {
              _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
            }
            final String _tmpSourceEquipment;
            if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
              _tmpSourceEquipment = null;
            } else {
              _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
            }
            final String _tmpForgeMovementPattern;
            _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
            final String _tmpForgeExerciseFamilyId;
            if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
              _tmpForgeExerciseFamilyId = null;
            } else {
              _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
            }
            final String _tmpSearchTokens;
            _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
            final String _tmpLicense;
            _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
            final boolean _tmpIsPopular;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
            _tmpIsPopular = _tmp_1 != 0;
            final int _tmpPopularityRank;
            _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExerciseEntity>> getRecentlyUsedExercises(final int limit) {
    final String _sql = "\n"
            + "        SELECT DISTINCT e.* FROM exercises e\n"
            + "        INNER JOIN workout_sets ws ON e.id = ws.exercise_id\n"
            + "        INNER JOIN workout_sessions s ON ws.session_id = s.id\n"
            + "        WHERE s.status = 'COMPLETED'\n"
            + "        ORDER BY s.end_time DESC\n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercises", "workout_sets",
        "workout_sessions"}, new Callable<List<ExerciseEntity>>() {
      @Override
      @NonNull
      public List<ExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCanonicalName = CursorUtil.getColumnIndexOrThrow(_cursor, "canonical_name");
          final int _cursorIndexOfMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "movement_pattern");
          final int _cursorIndexOfMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "mechanic");
          final int _cursorIndexOfForceType = CursorUtil.getColumnIndexOrThrow(_cursor, "force_type");
          final int _cursorIndexOfExperienceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "experience_level");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfFormCues = CursorUtil.getColumnIndexOrThrow(_cursor, "form_cues");
          final int _cursorIndexOfCommonMistakes = CursorUtil.getColumnIndexOrThrow(_cursor, "common_mistakes");
          final int _cursorIndexOfYoutubeVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "youtube_video_id");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "is_custom");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfSourceId = CursorUtil.getColumnIndexOrThrow(_cursor, "source_id");
          final int _cursorIndexOfSourceCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "source_category");
          final int _cursorIndexOfSourceForce = CursorUtil.getColumnIndexOrThrow(_cursor, "source_force");
          final int _cursorIndexOfSourceLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "source_level");
          final int _cursorIndexOfSourceMechanic = CursorUtil.getColumnIndexOrThrow(_cursor, "source_mechanic");
          final int _cursorIndexOfSourceEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "source_equipment");
          final int _cursorIndexOfForgeMovementPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_movement_pattern");
          final int _cursorIndexOfForgeExerciseFamilyId = CursorUtil.getColumnIndexOrThrow(_cursor, "forge_exercise_family_id");
          final int _cursorIndexOfSearchTokens = CursorUtil.getColumnIndexOrThrow(_cursor, "search_tokens");
          final int _cursorIndexOfLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "license");
          final int _cursorIndexOfIsPopular = CursorUtil.getColumnIndexOrThrow(_cursor, "is_popular");
          final int _cursorIndexOfPopularityRank = CursorUtil.getColumnIndexOrThrow(_cursor, "popularity_rank");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "is_favorite");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<ExerciseEntity> _result = new ArrayList<ExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCanonicalName;
            _tmpCanonicalName = _cursor.getString(_cursorIndexOfCanonicalName);
            final String _tmpMovementPattern;
            _tmpMovementPattern = _cursor.getString(_cursorIndexOfMovementPattern);
            final String _tmpMechanic;
            _tmpMechanic = _cursor.getString(_cursorIndexOfMechanic);
            final String _tmpForceType;
            _tmpForceType = _cursor.getString(_cursorIndexOfForceType);
            final String _tmpExperienceLevel;
            _tmpExperienceLevel = _cursor.getString(_cursorIndexOfExperienceLevel);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpFormCues;
            _tmpFormCues = _cursor.getString(_cursorIndexOfFormCues);
            final String _tmpCommonMistakes;
            _tmpCommonMistakes = _cursor.getString(_cursorIndexOfCommonMistakes);
            final String _tmpYoutubeVideoId;
            if (_cursor.isNull(_cursorIndexOfYoutubeVideoId)) {
              _tmpYoutubeVideoId = null;
            } else {
              _tmpYoutubeVideoId = _cursor.getString(_cursorIndexOfYoutubeVideoId);
            }
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final String _tmpSourceId;
            if (_cursor.isNull(_cursorIndexOfSourceId)) {
              _tmpSourceId = null;
            } else {
              _tmpSourceId = _cursor.getString(_cursorIndexOfSourceId);
            }
            final String _tmpSourceCategory;
            if (_cursor.isNull(_cursorIndexOfSourceCategory)) {
              _tmpSourceCategory = null;
            } else {
              _tmpSourceCategory = _cursor.getString(_cursorIndexOfSourceCategory);
            }
            final String _tmpSourceForce;
            if (_cursor.isNull(_cursorIndexOfSourceForce)) {
              _tmpSourceForce = null;
            } else {
              _tmpSourceForce = _cursor.getString(_cursorIndexOfSourceForce);
            }
            final String _tmpSourceLevel;
            if (_cursor.isNull(_cursorIndexOfSourceLevel)) {
              _tmpSourceLevel = null;
            } else {
              _tmpSourceLevel = _cursor.getString(_cursorIndexOfSourceLevel);
            }
            final String _tmpSourceMechanic;
            if (_cursor.isNull(_cursorIndexOfSourceMechanic)) {
              _tmpSourceMechanic = null;
            } else {
              _tmpSourceMechanic = _cursor.getString(_cursorIndexOfSourceMechanic);
            }
            final String _tmpSourceEquipment;
            if (_cursor.isNull(_cursorIndexOfSourceEquipment)) {
              _tmpSourceEquipment = null;
            } else {
              _tmpSourceEquipment = _cursor.getString(_cursorIndexOfSourceEquipment);
            }
            final String _tmpForgeMovementPattern;
            _tmpForgeMovementPattern = _cursor.getString(_cursorIndexOfForgeMovementPattern);
            final String _tmpForgeExerciseFamilyId;
            if (_cursor.isNull(_cursorIndexOfForgeExerciseFamilyId)) {
              _tmpForgeExerciseFamilyId = null;
            } else {
              _tmpForgeExerciseFamilyId = _cursor.getString(_cursorIndexOfForgeExerciseFamilyId);
            }
            final String _tmpSearchTokens;
            _tmpSearchTokens = _cursor.getString(_cursorIndexOfSearchTokens);
            final String _tmpLicense;
            _tmpLicense = _cursor.getString(_cursorIndexOfLicense);
            final boolean _tmpIsPopular;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPopular);
            _tmpIsPopular = _tmp_1 != 0;
            final int _tmpPopularityRank;
            _tmpPopularityRank = _cursor.getInt(_cursorIndexOfPopularityRank);
            final boolean _tmpIsFavorite;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_2 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ExerciseEntity(_tmpId,_tmpName,_tmpCanonicalName,_tmpMovementPattern,_tmpMechanic,_tmpForceType,_tmpExperienceLevel,_tmpInstructions,_tmpFormCues,_tmpCommonMistakes,_tmpYoutubeVideoId,_tmpIsCustom,_tmpSource,_tmpSourceId,_tmpSourceCategory,_tmpSourceForce,_tmpSourceLevel,_tmpSourceMechanic,_tmpSourceEquipment,_tmpForgeMovementPattern,_tmpForgeExerciseFamilyId,_tmpSearchTokens,_tmpLicense,_tmpIsPopular,_tmpPopularityRank,_tmpIsFavorite,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ExerciseProgressionRecordEntity>> getProgressionHistory(
      final String exerciseId) {
    final String _sql = "SELECT * FROM exercise_progression_records WHERE exercise_id = ? ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_progression_records"}, new Callable<List<ExerciseProgressionRecordEntity>>() {
      @Override
      @NonNull
      public List<ExerciseProgressionRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "session_id");
          final int _cursorIndexOfRecommendedWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_weight_kg");
          final int _cursorIndexOfRecommendedRepMin = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_min");
          final int _cursorIndexOfRecommendedRepMax = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_max");
          final int _cursorIndexOfRationale = CursorUtil.getColumnIndexOrThrow(_cursor, "rationale");
          final int _cursorIndexOfIsDeload = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deload");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ExerciseProgressionRecordEntity> _result = new ArrayList<ExerciseProgressionRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseProgressionRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final double _tmpRecommendedWeightKg;
            _tmpRecommendedWeightKg = _cursor.getDouble(_cursorIndexOfRecommendedWeightKg);
            final int _tmpRecommendedRepMin;
            _tmpRecommendedRepMin = _cursor.getInt(_cursorIndexOfRecommendedRepMin);
            final int _tmpRecommendedRepMax;
            _tmpRecommendedRepMax = _cursor.getInt(_cursorIndexOfRecommendedRepMax);
            final String _tmpRationale;
            _tmpRationale = _cursor.getString(_cursorIndexOfRationale);
            final boolean _tmpIsDeload;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeload);
            _tmpIsDeload = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ExerciseProgressionRecordEntity(_tmpId,_tmpExerciseId,_tmpSessionId,_tmpRecommendedWeightKg,_tmpRecommendedRepMin,_tmpRecommendedRepMax,_tmpRationale,_tmpIsDeload,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLatestProgressionDirect(final String exerciseId,
      final Continuation<? super ExerciseProgressionRecordEntity> $completion) {
    final String _sql = "SELECT * FROM exercise_progression_records WHERE exercise_id = ? ORDER BY created_at DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ExerciseProgressionRecordEntity>() {
      @Override
      @Nullable
      public ExerciseProgressionRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "session_id");
          final int _cursorIndexOfRecommendedWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_weight_kg");
          final int _cursorIndexOfRecommendedRepMin = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_min");
          final int _cursorIndexOfRecommendedRepMax = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_max");
          final int _cursorIndexOfRationale = CursorUtil.getColumnIndexOrThrow(_cursor, "rationale");
          final int _cursorIndexOfIsDeload = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deload");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final ExerciseProgressionRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final double _tmpRecommendedWeightKg;
            _tmpRecommendedWeightKg = _cursor.getDouble(_cursorIndexOfRecommendedWeightKg);
            final int _tmpRecommendedRepMin;
            _tmpRecommendedRepMin = _cursor.getInt(_cursorIndexOfRecommendedRepMin);
            final int _tmpRecommendedRepMax;
            _tmpRecommendedRepMax = _cursor.getInt(_cursorIndexOfRecommendedRepMax);
            final String _tmpRationale;
            _tmpRationale = _cursor.getString(_cursorIndexOfRationale);
            final boolean _tmpIsDeload;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeload);
            _tmpIsDeload = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new ExerciseProgressionRecordEntity(_tmpId,_tmpExerciseId,_tmpSessionId,_tmpRecommendedWeightKg,_tmpRecommendedRepMin,_tmpRecommendedRepMax,_tmpRationale,_tmpIsDeload,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecentProgressionHistoryDirect(final String exerciseId, final int limit,
      final Continuation<? super List<ExerciseProgressionRecordEntity>> $completion) {
    final String _sql = "SELECT * FROM exercise_progression_records WHERE exercise_id = ? ORDER BY created_at DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ExerciseProgressionRecordEntity>>() {
      @Override
      @NonNull
      public List<ExerciseProgressionRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "session_id");
          final int _cursorIndexOfRecommendedWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_weight_kg");
          final int _cursorIndexOfRecommendedRepMin = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_min");
          final int _cursorIndexOfRecommendedRepMax = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_max");
          final int _cursorIndexOfRationale = CursorUtil.getColumnIndexOrThrow(_cursor, "rationale");
          final int _cursorIndexOfIsDeload = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deload");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ExerciseProgressionRecordEntity> _result = new ArrayList<ExerciseProgressionRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseProgressionRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final double _tmpRecommendedWeightKg;
            _tmpRecommendedWeightKg = _cursor.getDouble(_cursorIndexOfRecommendedWeightKg);
            final int _tmpRecommendedRepMin;
            _tmpRecommendedRepMin = _cursor.getInt(_cursorIndexOfRecommendedRepMin);
            final int _tmpRecommendedRepMax;
            _tmpRecommendedRepMax = _cursor.getInt(_cursorIndexOfRecommendedRepMax);
            final String _tmpRationale;
            _tmpRationale = _cursor.getString(_cursorIndexOfRationale);
            final boolean _tmpIsDeload;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeload);
            _tmpIsDeload = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ExerciseProgressionRecordEntity(_tmpId,_tmpExerciseId,_tmpSessionId,_tmpRecommendedWeightKg,_tmpRecommendedRepMin,_tmpRecommendedRepMax,_tmpRationale,_tmpIsDeload,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExerciseProgressionRecordEntity>> getAllProgressionRecords() {
    final String _sql = "SELECT * FROM exercise_progression_records";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_progression_records"}, new Callable<List<ExerciseProgressionRecordEntity>>() {
      @Override
      @NonNull
      public List<ExerciseProgressionRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "session_id");
          final int _cursorIndexOfRecommendedWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_weight_kg");
          final int _cursorIndexOfRecommendedRepMin = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_min");
          final int _cursorIndexOfRecommendedRepMax = CursorUtil.getColumnIndexOrThrow(_cursor, "recommended_rep_max");
          final int _cursorIndexOfRationale = CursorUtil.getColumnIndexOrThrow(_cursor, "rationale");
          final int _cursorIndexOfIsDeload = CursorUtil.getColumnIndexOrThrow(_cursor, "is_deload");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ExerciseProgressionRecordEntity> _result = new ArrayList<ExerciseProgressionRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExerciseProgressionRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final double _tmpRecommendedWeightKg;
            _tmpRecommendedWeightKg = _cursor.getDouble(_cursorIndexOfRecommendedWeightKg);
            final int _tmpRecommendedRepMin;
            _tmpRecommendedRepMin = _cursor.getInt(_cursorIndexOfRecommendedRepMin);
            final int _tmpRecommendedRepMax;
            _tmpRecommendedRepMax = _cursor.getInt(_cursorIndexOfRecommendedRepMax);
            final String _tmpRationale;
            _tmpRationale = _cursor.getString(_cursorIndexOfRationale);
            final boolean _tmpIsDeload;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeload);
            _tmpIsDeload = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ExerciseProgressionRecordEntity(_tmpId,_tmpExerciseId,_tmpSessionId,_tmpRecommendedWeightKg,_tmpRecommendedRepMin,_tmpRecommendedRepMax,_tmpRationale,_tmpIsDeload,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<ExercisePersonalRecordEntity> getPersonalRecord(final String exerciseId) {
    final String _sql = "SELECT * FROM exercise_personal_records WHERE exercise_id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_personal_records"}, new Callable<ExercisePersonalRecordEntity>() {
      @Override
      @Nullable
      public ExercisePersonalRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfMaxWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "max_weight_kg");
          final int _cursorIndexOfMaxRepsAtMaxWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "max_reps_at_max_weight");
          final int _cursorIndexOfEstimated1RmKg = CursorUtil.getColumnIndexOrThrow(_cursor, "estimated_1rm_kg");
          final int _cursorIndexOfBestSetVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "best_set_volume_kg");
          final int _cursorIndexOfBestSessionVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "best_session_volume_kg");
          final int _cursorIndexOfAchievedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "achieved_at");
          final ExercisePersonalRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final double _tmpMaxWeightKg;
            _tmpMaxWeightKg = _cursor.getDouble(_cursorIndexOfMaxWeightKg);
            final int _tmpMaxRepsAtMaxWeight;
            _tmpMaxRepsAtMaxWeight = _cursor.getInt(_cursorIndexOfMaxRepsAtMaxWeight);
            final double _tmpEstimated1RmKg;
            _tmpEstimated1RmKg = _cursor.getDouble(_cursorIndexOfEstimated1RmKg);
            final double _tmpBestSetVolumeKg;
            _tmpBestSetVolumeKg = _cursor.getDouble(_cursorIndexOfBestSetVolumeKg);
            final double _tmpBestSessionVolumeKg;
            _tmpBestSessionVolumeKg = _cursor.getDouble(_cursorIndexOfBestSessionVolumeKg);
            final long _tmpAchievedAt;
            _tmpAchievedAt = _cursor.getLong(_cursorIndexOfAchievedAt);
            _result = new ExercisePersonalRecordEntity(_tmpExerciseId,_tmpMaxWeightKg,_tmpMaxRepsAtMaxWeight,_tmpEstimated1RmKg,_tmpBestSetVolumeKg,_tmpBestSessionVolumeKg,_tmpAchievedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPersonalRecordDirect(final String exerciseId,
      final Continuation<? super ExercisePersonalRecordEntity> $completion) {
    final String _sql = "SELECT * FROM exercise_personal_records WHERE exercise_id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ExercisePersonalRecordEntity>() {
      @Override
      @Nullable
      public ExercisePersonalRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfMaxWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "max_weight_kg");
          final int _cursorIndexOfMaxRepsAtMaxWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "max_reps_at_max_weight");
          final int _cursorIndexOfEstimated1RmKg = CursorUtil.getColumnIndexOrThrow(_cursor, "estimated_1rm_kg");
          final int _cursorIndexOfBestSetVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "best_set_volume_kg");
          final int _cursorIndexOfBestSessionVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "best_session_volume_kg");
          final int _cursorIndexOfAchievedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "achieved_at");
          final ExercisePersonalRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final double _tmpMaxWeightKg;
            _tmpMaxWeightKg = _cursor.getDouble(_cursorIndexOfMaxWeightKg);
            final int _tmpMaxRepsAtMaxWeight;
            _tmpMaxRepsAtMaxWeight = _cursor.getInt(_cursorIndexOfMaxRepsAtMaxWeight);
            final double _tmpEstimated1RmKg;
            _tmpEstimated1RmKg = _cursor.getDouble(_cursorIndexOfEstimated1RmKg);
            final double _tmpBestSetVolumeKg;
            _tmpBestSetVolumeKg = _cursor.getDouble(_cursorIndexOfBestSetVolumeKg);
            final double _tmpBestSessionVolumeKg;
            _tmpBestSessionVolumeKg = _cursor.getDouble(_cursorIndexOfBestSessionVolumeKg);
            final long _tmpAchievedAt;
            _tmpAchievedAt = _cursor.getLong(_cursorIndexOfAchievedAt);
            _result = new ExercisePersonalRecordEntity(_tmpExerciseId,_tmpMaxWeightKg,_tmpMaxRepsAtMaxWeight,_tmpEstimated1RmKg,_tmpBestSetVolumeKg,_tmpBestSessionVolumeKg,_tmpAchievedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ExercisePersonalRecordEntity>> getAllPersonalRecords() {
    final String _sql = "SELECT * FROM exercise_personal_records ORDER BY achieved_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"exercise_personal_records"}, new Callable<List<ExercisePersonalRecordEntity>>() {
      @Override
      @NonNull
      public List<ExercisePersonalRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfMaxWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "max_weight_kg");
          final int _cursorIndexOfMaxRepsAtMaxWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "max_reps_at_max_weight");
          final int _cursorIndexOfEstimated1RmKg = CursorUtil.getColumnIndexOrThrow(_cursor, "estimated_1rm_kg");
          final int _cursorIndexOfBestSetVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "best_set_volume_kg");
          final int _cursorIndexOfBestSessionVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "best_session_volume_kg");
          final int _cursorIndexOfAchievedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "achieved_at");
          final List<ExercisePersonalRecordEntity> _result = new ArrayList<ExercisePersonalRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExercisePersonalRecordEntity _item;
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final double _tmpMaxWeightKg;
            _tmpMaxWeightKg = _cursor.getDouble(_cursorIndexOfMaxWeightKg);
            final int _tmpMaxRepsAtMaxWeight;
            _tmpMaxRepsAtMaxWeight = _cursor.getInt(_cursorIndexOfMaxRepsAtMaxWeight);
            final double _tmpEstimated1RmKg;
            _tmpEstimated1RmKg = _cursor.getDouble(_cursorIndexOfEstimated1RmKg);
            final double _tmpBestSetVolumeKg;
            _tmpBestSetVolumeKg = _cursor.getDouble(_cursorIndexOfBestSetVolumeKg);
            final double _tmpBestSessionVolumeKg;
            _tmpBestSessionVolumeKg = _cursor.getDouble(_cursorIndexOfBestSessionVolumeKg);
            final long _tmpAchievedAt;
            _tmpAchievedAt = _cursor.getLong(_cursorIndexOfAchievedAt);
            _item = new ExercisePersonalRecordEntity(_tmpExerciseId,_tmpMaxWeightKg,_tmpMaxRepsAtMaxWeight,_tmpEstimated1RmKg,_tmpBestSetVolumeKg,_tmpBestSessionVolumeKg,_tmpAchievedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<UserNutritionProfileEntity> getNutritionProfile() {
    final String _sql = "SELECT * FROM user_nutrition_profile WHERE id = 'primary_profile' LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_nutrition_profile"}, new Callable<UserNutritionProfileEntity>() {
      @Override
      @Nullable
      public UserNutritionProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMaintenanceCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "maintenance_calories");
          final int _cursorIndexOfIsMaintenanceManual = CursorUtil.getColumnIndexOrThrow(_cursor, "is_maintenance_manual");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfTargetCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "target_calories");
          final int _cursorIndexOfProteinGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "protein_grams");
          final int _cursorIndexOfFatGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "fat_grams");
          final int _cursorIndexOfCarbsGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "carbs_grams");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final UserNutritionProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpMaintenanceCalories;
            _tmpMaintenanceCalories = _cursor.getInt(_cursorIndexOfMaintenanceCalories);
            final boolean _tmpIsMaintenanceManual;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsMaintenanceManual);
            _tmpIsMaintenanceManual = _tmp != 0;
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final int _tmpTargetCalories;
            _tmpTargetCalories = _cursor.getInt(_cursorIndexOfTargetCalories);
            final int _tmpProteinGrams;
            _tmpProteinGrams = _cursor.getInt(_cursorIndexOfProteinGrams);
            final int _tmpFatGrams;
            _tmpFatGrams = _cursor.getInt(_cursorIndexOfFatGrams);
            final int _tmpCarbsGrams;
            _tmpCarbsGrams = _cursor.getInt(_cursorIndexOfCarbsGrams);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserNutritionProfileEntity(_tmpId,_tmpMaintenanceCalories,_tmpIsMaintenanceManual,_tmpGoal,_tmpTargetCalories,_tmpProteinGrams,_tmpFatGrams,_tmpCarbsGrams,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<WeightLogEntity>> getWeightLogs(final int limit) {
    final String _sql = "SELECT * FROM weight_logs ORDER BY logged_date DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"weight_logs"}, new Callable<List<WeightLogEntity>>() {
      @Override
      @NonNull
      public List<WeightLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfLoggedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "logged_date");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<WeightLogEntity> _result = new ArrayList<WeightLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WeightLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final String _tmpLoggedDate;
            _tmpLoggedDate = _cursor.getString(_cursorIndexOfLoggedDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new WeightLogEntity(_tmpId,_tmpWeightKg,_tmpLoggedDate,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
