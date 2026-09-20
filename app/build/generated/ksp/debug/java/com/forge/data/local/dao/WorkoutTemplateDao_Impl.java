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
import com.forge.data.local.entity.TemplateExerciseEntity;
import com.forge.data.local.entity.WorkoutTemplateEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
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
public final class WorkoutTemplateDao_Impl implements WorkoutTemplateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkoutTemplateEntity> __insertionAdapterOfWorkoutTemplateEntity;

  private final EntityInsertionAdapter<TemplateExerciseEntity> __insertionAdapterOfTemplateExerciseEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTemplateExercises;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTemplate;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllTemplates;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllTemplateExercises;

  public WorkoutTemplateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkoutTemplateEntity = new EntityInsertionAdapter<WorkoutTemplateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `workout_templates` (`id`,`name`,`focus`,`version`,`target_muscles`,`estimated_duration_min`,`created_at`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutTemplateEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getFocus());
        statement.bindLong(4, entity.getVersion());
        statement.bindString(5, entity.getTargetMuscles());
        statement.bindLong(6, entity.getEstimatedDurationMin());
        statement.bindLong(7, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfTemplateExerciseEntity = new EntityInsertionAdapter<TemplateExerciseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `template_exercises` (`id`,`template_id`,`exercise_id`,`order_index`,`target_sets`,`target_reps_min`,`target_reps_max`,`target_rir`,`target_weight_kg`,`rest_seconds`,`is_warmup`,`is_drop_set`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TemplateExerciseEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTemplateId());
        statement.bindString(3, entity.getExerciseId());
        statement.bindLong(4, entity.getOrderIndex());
        statement.bindLong(5, entity.getTargetSets());
        statement.bindLong(6, entity.getTargetRepsMin());
        statement.bindLong(7, entity.getTargetRepsMax());
        statement.bindLong(8, entity.getTargetRir());
        if (entity.getTargetWeightKg() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getTargetWeightKg());
        }
        statement.bindLong(10, entity.getRestSeconds());
        final int _tmp = entity.isWarmup() ? 1 : 0;
        statement.bindLong(11, _tmp);
        final int _tmp_1 = entity.isDropSet() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        if (entity.getNotes() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getNotes());
        }
      }
    };
    this.__preparedStmtOfDeleteTemplateExercises = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM template_exercises WHERE template_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTemplate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workout_templates WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllTemplates = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workout_templates";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllTemplateExercises = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM template_exercises";
        return _query;
      }
    };
  }

  @Override
  public Object insertTemplate(final WorkoutTemplateEntity template,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWorkoutTemplateEntity.insert(template);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTemplateExercises(final List<TemplateExerciseEntity> exercises,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTemplateExerciseEntity.insert(exercises);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replaceTemplateWithExercises(final WorkoutTemplateEntity template,
      final List<TemplateExerciseEntity> exercises, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> WorkoutTemplateDao.DefaultImpls.replaceTemplateWithExercises(WorkoutTemplateDao_Impl.this, template, exercises, __cont), $completion);
  }

  @Override
  public Object deleteTemplateExercises(final String templateId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTemplateExercises.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, templateId);
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
          __preparedStmtOfDeleteTemplateExercises.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTemplate(final String templateId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTemplate.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, templateId);
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
          __preparedStmtOfDeleteTemplate.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllTemplates(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllTemplates.acquire();
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
          __preparedStmtOfDeleteAllTemplates.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllTemplateExercises(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllTemplateExercises.acquire();
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
          __preparedStmtOfDeleteAllTemplateExercises.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WorkoutTemplateEntity>> getTemplates() {
    final String _sql = "SELECT * FROM workout_templates ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_templates"}, new Callable<List<WorkoutTemplateEntity>>() {
      @Override
      @NonNull
      public List<WorkoutTemplateEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFocus = CursorUtil.getColumnIndexOrThrow(_cursor, "focus");
          final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
          final int _cursorIndexOfTargetMuscles = CursorUtil.getColumnIndexOrThrow(_cursor, "target_muscles");
          final int _cursorIndexOfEstimatedDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "estimated_duration_min");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<WorkoutTemplateEntity> _result = new ArrayList<WorkoutTemplateEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutTemplateEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFocus;
            _tmpFocus = _cursor.getString(_cursorIndexOfFocus);
            final int _tmpVersion;
            _tmpVersion = _cursor.getInt(_cursorIndexOfVersion);
            final String _tmpTargetMuscles;
            _tmpTargetMuscles = _cursor.getString(_cursorIndexOfTargetMuscles);
            final int _tmpEstimatedDurationMin;
            _tmpEstimatedDurationMin = _cursor.getInt(_cursorIndexOfEstimatedDurationMin);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new WorkoutTemplateEntity(_tmpId,_tmpName,_tmpFocus,_tmpVersion,_tmpTargetMuscles,_tmpEstimatedDurationMin,_tmpCreatedAt);
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
  public Flow<WorkoutTemplateEntity> getTemplateById(final String id) {
    final String _sql = "SELECT * FROM workout_templates WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_templates"}, new Callable<WorkoutTemplateEntity>() {
      @Override
      @Nullable
      public WorkoutTemplateEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFocus = CursorUtil.getColumnIndexOrThrow(_cursor, "focus");
          final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
          final int _cursorIndexOfTargetMuscles = CursorUtil.getColumnIndexOrThrow(_cursor, "target_muscles");
          final int _cursorIndexOfEstimatedDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "estimated_duration_min");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final WorkoutTemplateEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFocus;
            _tmpFocus = _cursor.getString(_cursorIndexOfFocus);
            final int _tmpVersion;
            _tmpVersion = _cursor.getInt(_cursorIndexOfVersion);
            final String _tmpTargetMuscles;
            _tmpTargetMuscles = _cursor.getString(_cursorIndexOfTargetMuscles);
            final int _tmpEstimatedDurationMin;
            _tmpEstimatedDurationMin = _cursor.getInt(_cursorIndexOfEstimatedDurationMin);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new WorkoutTemplateEntity(_tmpId,_tmpName,_tmpFocus,_tmpVersion,_tmpTargetMuscles,_tmpEstimatedDurationMin,_tmpCreatedAt);
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
  public Object getTemplateByIdSync(final String id,
      final Continuation<? super WorkoutTemplateEntity> $completion) {
    final String _sql = "SELECT * FROM workout_templates WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WorkoutTemplateEntity>() {
      @Override
      @Nullable
      public WorkoutTemplateEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFocus = CursorUtil.getColumnIndexOrThrow(_cursor, "focus");
          final int _cursorIndexOfVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "version");
          final int _cursorIndexOfTargetMuscles = CursorUtil.getColumnIndexOrThrow(_cursor, "target_muscles");
          final int _cursorIndexOfEstimatedDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "estimated_duration_min");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final WorkoutTemplateEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFocus;
            _tmpFocus = _cursor.getString(_cursorIndexOfFocus);
            final int _tmpVersion;
            _tmpVersion = _cursor.getInt(_cursorIndexOfVersion);
            final String _tmpTargetMuscles;
            _tmpTargetMuscles = _cursor.getString(_cursorIndexOfTargetMuscles);
            final int _tmpEstimatedDurationMin;
            _tmpEstimatedDurationMin = _cursor.getInt(_cursorIndexOfEstimatedDurationMin);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new WorkoutTemplateEntity(_tmpId,_tmpName,_tmpFocus,_tmpVersion,_tmpTargetMuscles,_tmpEstimatedDurationMin,_tmpCreatedAt);
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
  public Flow<List<TemplateExerciseEntity>> getTemplateExercises(final String templateId) {
    final String _sql = "SELECT * FROM template_exercises WHERE template_id = ? ORDER BY order_index ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, templateId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"template_exercises"}, new Callable<List<TemplateExerciseEntity>>() {
      @Override
      @NonNull
      public List<TemplateExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "template_id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "order_index");
          final int _cursorIndexOfTargetSets = CursorUtil.getColumnIndexOrThrow(_cursor, "target_sets");
          final int _cursorIndexOfTargetRepsMin = CursorUtil.getColumnIndexOrThrow(_cursor, "target_reps_min");
          final int _cursorIndexOfTargetRepsMax = CursorUtil.getColumnIndexOrThrow(_cursor, "target_reps_max");
          final int _cursorIndexOfTargetRir = CursorUtil.getColumnIndexOrThrow(_cursor, "target_rir");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "target_weight_kg");
          final int _cursorIndexOfRestSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "rest_seconds");
          final int _cursorIndexOfIsWarmup = CursorUtil.getColumnIndexOrThrow(_cursor, "is_warmup");
          final int _cursorIndexOfIsDropSet = CursorUtil.getColumnIndexOrThrow(_cursor, "is_drop_set");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<TemplateExerciseEntity> _result = new ArrayList<TemplateExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TemplateExerciseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTemplateId;
            _tmpTemplateId = _cursor.getString(_cursorIndexOfTemplateId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final int _tmpTargetSets;
            _tmpTargetSets = _cursor.getInt(_cursorIndexOfTargetSets);
            final int _tmpTargetRepsMin;
            _tmpTargetRepsMin = _cursor.getInt(_cursorIndexOfTargetRepsMin);
            final int _tmpTargetRepsMax;
            _tmpTargetRepsMax = _cursor.getInt(_cursorIndexOfTargetRepsMax);
            final int _tmpTargetRir;
            _tmpTargetRir = _cursor.getInt(_cursorIndexOfTargetRir);
            final Double _tmpTargetWeightKg;
            if (_cursor.isNull(_cursorIndexOfTargetWeightKg)) {
              _tmpTargetWeightKg = null;
            } else {
              _tmpTargetWeightKg = _cursor.getDouble(_cursorIndexOfTargetWeightKg);
            }
            final int _tmpRestSeconds;
            _tmpRestSeconds = _cursor.getInt(_cursorIndexOfRestSeconds);
            final boolean _tmpIsWarmup;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWarmup);
            _tmpIsWarmup = _tmp != 0;
            final boolean _tmpIsDropSet;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDropSet);
            _tmpIsDropSet = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new TemplateExerciseEntity(_tmpId,_tmpTemplateId,_tmpExerciseId,_tmpOrderIndex,_tmpTargetSets,_tmpTargetRepsMin,_tmpTargetRepsMax,_tmpTargetRir,_tmpTargetWeightKg,_tmpRestSeconds,_tmpIsWarmup,_tmpIsDropSet,_tmpNotes);
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
  public Object getTemplateExercisesSync(final String templateId,
      final Continuation<? super List<TemplateExerciseEntity>> $completion) {
    final String _sql = "SELECT * FROM template_exercises WHERE template_id = ? ORDER BY order_index ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, templateId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TemplateExerciseEntity>>() {
      @Override
      @NonNull
      public List<TemplateExerciseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "template_id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "order_index");
          final int _cursorIndexOfTargetSets = CursorUtil.getColumnIndexOrThrow(_cursor, "target_sets");
          final int _cursorIndexOfTargetRepsMin = CursorUtil.getColumnIndexOrThrow(_cursor, "target_reps_min");
          final int _cursorIndexOfTargetRepsMax = CursorUtil.getColumnIndexOrThrow(_cursor, "target_reps_max");
          final int _cursorIndexOfTargetRir = CursorUtil.getColumnIndexOrThrow(_cursor, "target_rir");
          final int _cursorIndexOfTargetWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "target_weight_kg");
          final int _cursorIndexOfRestSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "rest_seconds");
          final int _cursorIndexOfIsWarmup = CursorUtil.getColumnIndexOrThrow(_cursor, "is_warmup");
          final int _cursorIndexOfIsDropSet = CursorUtil.getColumnIndexOrThrow(_cursor, "is_drop_set");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<TemplateExerciseEntity> _result = new ArrayList<TemplateExerciseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TemplateExerciseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTemplateId;
            _tmpTemplateId = _cursor.getString(_cursorIndexOfTemplateId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final int _tmpTargetSets;
            _tmpTargetSets = _cursor.getInt(_cursorIndexOfTargetSets);
            final int _tmpTargetRepsMin;
            _tmpTargetRepsMin = _cursor.getInt(_cursorIndexOfTargetRepsMin);
            final int _tmpTargetRepsMax;
            _tmpTargetRepsMax = _cursor.getInt(_cursorIndexOfTargetRepsMax);
            final int _tmpTargetRir;
            _tmpTargetRir = _cursor.getInt(_cursorIndexOfTargetRir);
            final Double _tmpTargetWeightKg;
            if (_cursor.isNull(_cursorIndexOfTargetWeightKg)) {
              _tmpTargetWeightKg = null;
            } else {
              _tmpTargetWeightKg = _cursor.getDouble(_cursorIndexOfTargetWeightKg);
            }
            final int _tmpRestSeconds;
            _tmpRestSeconds = _cursor.getInt(_cursorIndexOfRestSeconds);
            final boolean _tmpIsWarmup;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsWarmup);
            _tmpIsWarmup = _tmp != 0;
            final boolean _tmpIsDropSet;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDropSet);
            _tmpIsDropSet = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new TemplateExerciseEntity(_tmpId,_tmpTemplateId,_tmpExerciseId,_tmpOrderIndex,_tmpTargetSets,_tmpTargetRepsMin,_tmpTargetRepsMax,_tmpTargetRir,_tmpTargetWeightKg,_tmpRestSeconds,_tmpIsWarmup,_tmpIsDropSet,_tmpNotes);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
