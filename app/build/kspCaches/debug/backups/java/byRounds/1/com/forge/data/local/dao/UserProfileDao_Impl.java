package com.forge.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.forge.data.local.entity.UserProfileEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserProfileDao_Impl implements UserProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserProfileEntity> __insertionAdapterOfUserProfileEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateInitializationStep;

  private final SharedSQLiteStatement __preparedStmtOfSetInitialized;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public UserProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserProfileEntity = new EntityInsertionAdapter<UserProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_profile` (`id`,`name`,`photo_uri`,`goal`,`experience`,`days_per_week`,`session_duration_min`,`equipment`,`height_cm`,`weight_kg`,`age`,`sex`,`maintenance_calories`,`nutrition_goal`,`target_calories`,`target_protein_g`,`target_carbs_g`,`target_fat_g`,`transformation_start_date`,`photo_password_hash`,`photo_password_salt`,`is_initialized`,`initialization_step`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProfileEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getPhotoUri() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPhotoUri());
        }
        statement.bindString(4, entity.getGoal());
        statement.bindString(5, entity.getExperience());
        statement.bindLong(6, entity.getDaysPerWeek());
        statement.bindLong(7, entity.getSessionDurationMin());
        statement.bindString(8, entity.getEquipment());
        statement.bindDouble(9, entity.getHeightCm());
        statement.bindDouble(10, entity.getWeightKg());
        if (entity.getAge() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getAge());
        }
        if (entity.getSex() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getSex());
        }
        statement.bindLong(13, entity.getMaintenanceCalories());
        statement.bindString(14, entity.getNutritionGoal());
        statement.bindLong(15, entity.getTargetCalories());
        statement.bindLong(16, entity.getTargetProteinG());
        statement.bindLong(17, entity.getTargetCarbsG());
        statement.bindLong(18, entity.getTargetFatG());
        statement.bindLong(19, entity.getTransformationStartDate());
        if (entity.getPhotoPasswordHash() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getPhotoPasswordHash());
        }
        if (entity.getPhotoPasswordSalt() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getPhotoPasswordSalt());
        }
        final int _tmp = entity.isInitialized() ? 1 : 0;
        statement.bindLong(22, _tmp);
        statement.bindLong(23, entity.getInitializationStep());
        statement.bindLong(24, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfUpdateInitializationStep = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_profile SET initialization_step = ?, updated_at = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfSetInitialized = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_profile SET is_initialized = ?, updated_at = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM user_profile";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final UserProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateInitializationStep(final int step, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateInitializationStep.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, step);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, now);
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
          __preparedStmtOfUpdateInitializationStep.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setInitialized(final boolean isInitialized, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetInitialized.acquire();
        int _argIndex = 1;
        final int _tmp = isInitialized ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, now);
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
          __preparedStmtOfSetInitialized.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserProfileEntity> getUserProfile() {
    final String _sql = "SELECT * FROM user_profile WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_profile"}, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_uri");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfExperience = CursorUtil.getColumnIndexOrThrow(_cursor, "experience");
          final int _cursorIndexOfDaysPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "days_per_week");
          final int _cursorIndexOfSessionDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "session_duration_min");
          final int _cursorIndexOfEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "equipment");
          final int _cursorIndexOfHeightCm = CursorUtil.getColumnIndexOrThrow(_cursor, "height_cm");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfSex = CursorUtil.getColumnIndexOrThrow(_cursor, "sex");
          final int _cursorIndexOfMaintenanceCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "maintenance_calories");
          final int _cursorIndexOfNutritionGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "nutrition_goal");
          final int _cursorIndexOfTargetCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "target_calories");
          final int _cursorIndexOfTargetProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "target_protein_g");
          final int _cursorIndexOfTargetCarbsG = CursorUtil.getColumnIndexOrThrow(_cursor, "target_carbs_g");
          final int _cursorIndexOfTargetFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "target_fat_g");
          final int _cursorIndexOfTransformationStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "transformation_start_date");
          final int _cursorIndexOfPhotoPasswordHash = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_password_hash");
          final int _cursorIndexOfPhotoPasswordSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_password_salt");
          final int _cursorIndexOfIsInitialized = CursorUtil.getColumnIndexOrThrow(_cursor, "is_initialized");
          final int _cursorIndexOfInitializationStep = CursorUtil.getColumnIndexOrThrow(_cursor, "initialization_step");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpExperience;
            _tmpExperience = _cursor.getString(_cursorIndexOfExperience);
            final int _tmpDaysPerWeek;
            _tmpDaysPerWeek = _cursor.getInt(_cursorIndexOfDaysPerWeek);
            final int _tmpSessionDurationMin;
            _tmpSessionDurationMin = _cursor.getInt(_cursorIndexOfSessionDurationMin);
            final String _tmpEquipment;
            _tmpEquipment = _cursor.getString(_cursorIndexOfEquipment);
            final float _tmpHeightCm;
            _tmpHeightCm = _cursor.getFloat(_cursorIndexOfHeightCm);
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final Integer _tmpAge;
            if (_cursor.isNull(_cursorIndexOfAge)) {
              _tmpAge = null;
            } else {
              _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            }
            final String _tmpSex;
            if (_cursor.isNull(_cursorIndexOfSex)) {
              _tmpSex = null;
            } else {
              _tmpSex = _cursor.getString(_cursorIndexOfSex);
            }
            final int _tmpMaintenanceCalories;
            _tmpMaintenanceCalories = _cursor.getInt(_cursorIndexOfMaintenanceCalories);
            final String _tmpNutritionGoal;
            _tmpNutritionGoal = _cursor.getString(_cursorIndexOfNutritionGoal);
            final int _tmpTargetCalories;
            _tmpTargetCalories = _cursor.getInt(_cursorIndexOfTargetCalories);
            final int _tmpTargetProteinG;
            _tmpTargetProteinG = _cursor.getInt(_cursorIndexOfTargetProteinG);
            final int _tmpTargetCarbsG;
            _tmpTargetCarbsG = _cursor.getInt(_cursorIndexOfTargetCarbsG);
            final int _tmpTargetFatG;
            _tmpTargetFatG = _cursor.getInt(_cursorIndexOfTargetFatG);
            final long _tmpTransformationStartDate;
            _tmpTransformationStartDate = _cursor.getLong(_cursorIndexOfTransformationStartDate);
            final String _tmpPhotoPasswordHash;
            if (_cursor.isNull(_cursorIndexOfPhotoPasswordHash)) {
              _tmpPhotoPasswordHash = null;
            } else {
              _tmpPhotoPasswordHash = _cursor.getString(_cursorIndexOfPhotoPasswordHash);
            }
            final String _tmpPhotoPasswordSalt;
            if (_cursor.isNull(_cursorIndexOfPhotoPasswordSalt)) {
              _tmpPhotoPasswordSalt = null;
            } else {
              _tmpPhotoPasswordSalt = _cursor.getString(_cursorIndexOfPhotoPasswordSalt);
            }
            final boolean _tmpIsInitialized;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsInitialized);
            _tmpIsInitialized = _tmp != 0;
            final int _tmpInitializationStep;
            _tmpInitializationStep = _cursor.getInt(_cursorIndexOfInitializationStep);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserProfileEntity(_tmpId,_tmpName,_tmpPhotoUri,_tmpGoal,_tmpExperience,_tmpDaysPerWeek,_tmpSessionDurationMin,_tmpEquipment,_tmpHeightCm,_tmpWeightKg,_tmpAge,_tmpSex,_tmpMaintenanceCalories,_tmpNutritionGoal,_tmpTargetCalories,_tmpTargetProteinG,_tmpTargetCarbsG,_tmpTargetFatG,_tmpTransformationStartDate,_tmpPhotoPasswordHash,_tmpPhotoPasswordSalt,_tmpIsInitialized,_tmpInitializationStep,_tmpUpdatedAt);
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
  public Object getUserProfileSync(final Continuation<? super UserProfileEntity> $completion) {
    final String _sql = "SELECT * FROM user_profile WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_uri");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfExperience = CursorUtil.getColumnIndexOrThrow(_cursor, "experience");
          final int _cursorIndexOfDaysPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "days_per_week");
          final int _cursorIndexOfSessionDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "session_duration_min");
          final int _cursorIndexOfEquipment = CursorUtil.getColumnIndexOrThrow(_cursor, "equipment");
          final int _cursorIndexOfHeightCm = CursorUtil.getColumnIndexOrThrow(_cursor, "height_cm");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfSex = CursorUtil.getColumnIndexOrThrow(_cursor, "sex");
          final int _cursorIndexOfMaintenanceCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "maintenance_calories");
          final int _cursorIndexOfNutritionGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "nutrition_goal");
          final int _cursorIndexOfTargetCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "target_calories");
          final int _cursorIndexOfTargetProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "target_protein_g");
          final int _cursorIndexOfTargetCarbsG = CursorUtil.getColumnIndexOrThrow(_cursor, "target_carbs_g");
          final int _cursorIndexOfTargetFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "target_fat_g");
          final int _cursorIndexOfTransformationStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "transformation_start_date");
          final int _cursorIndexOfPhotoPasswordHash = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_password_hash");
          final int _cursorIndexOfPhotoPasswordSalt = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_password_salt");
          final int _cursorIndexOfIsInitialized = CursorUtil.getColumnIndexOrThrow(_cursor, "is_initialized");
          final int _cursorIndexOfInitializationStep = CursorUtil.getColumnIndexOrThrow(_cursor, "initialization_step");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final String _tmpExperience;
            _tmpExperience = _cursor.getString(_cursorIndexOfExperience);
            final int _tmpDaysPerWeek;
            _tmpDaysPerWeek = _cursor.getInt(_cursorIndexOfDaysPerWeek);
            final int _tmpSessionDurationMin;
            _tmpSessionDurationMin = _cursor.getInt(_cursorIndexOfSessionDurationMin);
            final String _tmpEquipment;
            _tmpEquipment = _cursor.getString(_cursorIndexOfEquipment);
            final float _tmpHeightCm;
            _tmpHeightCm = _cursor.getFloat(_cursorIndexOfHeightCm);
            final float _tmpWeightKg;
            _tmpWeightKg = _cursor.getFloat(_cursorIndexOfWeightKg);
            final Integer _tmpAge;
            if (_cursor.isNull(_cursorIndexOfAge)) {
              _tmpAge = null;
            } else {
              _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            }
            final String _tmpSex;
            if (_cursor.isNull(_cursorIndexOfSex)) {
              _tmpSex = null;
            } else {
              _tmpSex = _cursor.getString(_cursorIndexOfSex);
            }
            final int _tmpMaintenanceCalories;
            _tmpMaintenanceCalories = _cursor.getInt(_cursorIndexOfMaintenanceCalories);
            final String _tmpNutritionGoal;
            _tmpNutritionGoal = _cursor.getString(_cursorIndexOfNutritionGoal);
            final int _tmpTargetCalories;
            _tmpTargetCalories = _cursor.getInt(_cursorIndexOfTargetCalories);
            final int _tmpTargetProteinG;
            _tmpTargetProteinG = _cursor.getInt(_cursorIndexOfTargetProteinG);
            final int _tmpTargetCarbsG;
            _tmpTargetCarbsG = _cursor.getInt(_cursorIndexOfTargetCarbsG);
            final int _tmpTargetFatG;
            _tmpTargetFatG = _cursor.getInt(_cursorIndexOfTargetFatG);
            final long _tmpTransformationStartDate;
            _tmpTransformationStartDate = _cursor.getLong(_cursorIndexOfTransformationStartDate);
            final String _tmpPhotoPasswordHash;
            if (_cursor.isNull(_cursorIndexOfPhotoPasswordHash)) {
              _tmpPhotoPasswordHash = null;
            } else {
              _tmpPhotoPasswordHash = _cursor.getString(_cursorIndexOfPhotoPasswordHash);
            }
            final String _tmpPhotoPasswordSalt;
            if (_cursor.isNull(_cursorIndexOfPhotoPasswordSalt)) {
              _tmpPhotoPasswordSalt = null;
            } else {
              _tmpPhotoPasswordSalt = _cursor.getString(_cursorIndexOfPhotoPasswordSalt);
            }
            final boolean _tmpIsInitialized;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsInitialized);
            _tmpIsInitialized = _tmp != 0;
            final int _tmpInitializationStep;
            _tmpInitializationStep = _cursor.getInt(_cursorIndexOfInitializationStep);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserProfileEntity(_tmpId,_tmpName,_tmpPhotoUri,_tmpGoal,_tmpExperience,_tmpDaysPerWeek,_tmpSessionDurationMin,_tmpEquipment,_tmpHeightCm,_tmpWeightKg,_tmpAge,_tmpSex,_tmpMaintenanceCalories,_tmpNutritionGoal,_tmpTargetCalories,_tmpTargetProteinG,_tmpTargetCarbsG,_tmpTargetFatG,_tmpTransformationStartDate,_tmpPhotoPasswordHash,_tmpPhotoPasswordSalt,_tmpIsInitialized,_tmpInitializationStep,_tmpUpdatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
