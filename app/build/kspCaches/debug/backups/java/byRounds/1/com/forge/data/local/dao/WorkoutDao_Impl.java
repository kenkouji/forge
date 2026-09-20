package com.forge.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.forge.data.local.entity.WorkoutSessionEntity;
import com.forge.data.local.entity.WorkoutSetEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class WorkoutDao_Impl implements WorkoutDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorkoutSessionEntity> __insertionAdapterOfWorkoutSessionEntity;

  private final EntityInsertionAdapter<WorkoutSetEntity> __insertionAdapterOfWorkoutSetEntity;

  private final EntityDeletionOrUpdateAdapter<WorkoutSessionEntity> __updateAdapterOfWorkoutSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSessionActiveState;

  private final SharedSQLiteStatement __preparedStmtOfCompleteSession;

  private final SharedSQLiteStatement __preparedStmtOfDiscardSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllSessions;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllSets;

  public WorkoutDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorkoutSessionEntity = new EntityInsertionAdapter<WorkoutSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `workout_sessions` (`id`,`routine_id`,`name`,`start_time`,`end_time`,`status`,`active_exercise_id`,`active_set_index`,`total_volume_kg`,`duration_seconds`,`notes`,`last_updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSessionEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getRoutineId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getRoutineId());
        }
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getEndTime());
        }
        statement.bindString(6, entity.getStatus());
        if (entity.getActiveExerciseId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getActiveExerciseId());
        }
        statement.bindLong(8, entity.getActiveSetIndex());
        statement.bindDouble(9, entity.getTotalVolumeKg());
        statement.bindLong(10, entity.getDurationSeconds());
        if (entity.getNotes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getNotes());
        }
        statement.bindLong(12, entity.getLastUpdatedAt());
      }
    };
    this.__insertionAdapterOfWorkoutSetEntity = new EntityInsertionAdapter<WorkoutSetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `workout_sets` (`id`,`session_id`,`exercise_id`,`set_order`,`set_type`,`weight_kg`,`reps`,`rpe`,`rir`,`tempo`,`rest_seconds_taken`,`is_completed`,`is_personal_record`,`completed_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSetEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSessionId());
        statement.bindString(3, entity.getExerciseId());
        statement.bindLong(4, entity.getSetOrder());
        statement.bindString(5, entity.getSetType());
        statement.bindDouble(6, entity.getWeightKg());
        statement.bindLong(7, entity.getReps());
        if (entity.getRpe() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getRpe());
        }
        if (entity.getRir() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getRir());
        }
        if (entity.getTempo() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getTempo());
        }
        if (entity.getRestSecondsTaken() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getRestSecondsTaken());
        }
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(12, _tmp);
        final int _tmp_1 = entity.isPersonalRecord() ? 1 : 0;
        statement.bindLong(13, _tmp_1);
        if (entity.getCompletedAt() == null) {
          statement.bindNull(14);
        } else {
          statement.bindLong(14, entity.getCompletedAt());
        }
      }
    };
    this.__updateAdapterOfWorkoutSessionEntity = new EntityDeletionOrUpdateAdapter<WorkoutSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `workout_sessions` SET `id` = ?,`routine_id` = ?,`name` = ?,`start_time` = ?,`end_time` = ?,`status` = ?,`active_exercise_id` = ?,`active_set_index` = ?,`total_volume_kg` = ?,`duration_seconds` = ?,`notes` = ?,`last_updated_at` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorkoutSessionEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getRoutineId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getRoutineId());
        }
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getEndTime());
        }
        statement.bindString(6, entity.getStatus());
        if (entity.getActiveExerciseId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getActiveExerciseId());
        }
        statement.bindLong(8, entity.getActiveSetIndex());
        statement.bindDouble(9, entity.getTotalVolumeKg());
        statement.bindLong(10, entity.getDurationSeconds());
        if (entity.getNotes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getNotes());
        }
        statement.bindLong(12, entity.getLastUpdatedAt());
        statement.bindString(13, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateSessionActiveState = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE workout_sessions \n"
                + "        SET active_exercise_id = ?,\n"
                + "            active_set_index = ?,\n"
                + "            total_volume_kg = total_volume_kg + ?,\n"
                + "            last_updated_at = ?\n"
                + "        WHERE id = ?\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfCompleteSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE workout_sessions SET status = 'COMPLETED', end_time = ?, duration_seconds = ?, last_updated_at = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDiscardSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE workout_sessions SET status = 'DISCARDED', last_updated_at = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllSessions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workout_sessions";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllSets = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM workout_sets";
        return _query;
      }
    };
  }

  @Override
  public Object insertSession(final WorkoutSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWorkoutSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSets(final List<WorkoutSetEntity> sets,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWorkoutSetEntity.insert(sets);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertOrUpdateSet(final WorkoutSetEntity set,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWorkoutSetEntity.insert(set);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSession(final WorkoutSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfWorkoutSessionEntity.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object logSetTransaction(final WorkoutSetEntity set, final String activeExerciseId,
      final int activeSetIndex, final double volumeDeltaKg, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> WorkoutDao.DefaultImpls.logSetTransaction(WorkoutDao_Impl.this, set, activeExerciseId, activeSetIndex, volumeDeltaKg, timestamp, __cont), $completion);
  }

  @Override
  public Object updateSessionActiveState(final String sessionId, final String activeExerciseId,
      final int activeSetIndex, final double volumeDeltaKg, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSessionActiveState.acquire();
        int _argIndex = 1;
        if (activeExerciseId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, activeExerciseId);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, activeSetIndex);
        _argIndex = 3;
        _stmt.bindDouble(_argIndex, volumeDeltaKg);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 5;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfUpdateSessionActiveState.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object completeSession(final String sessionId, final long endTime,
      final long durationSeconds, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCompleteSession.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, endTime);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, durationSeconds);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, endTime);
        _argIndex = 4;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfCompleteSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object discardSession(final String sessionId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDiscardSession.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfDiscardSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllSessions(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllSessions.acquire();
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
          __preparedStmtOfDeleteAllSessions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllSets(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllSets.acquire();
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
          __preparedStmtOfDeleteAllSets.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<WorkoutSessionEntity> getActiveSession() {
    final String _sql = "SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY start_time DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_sessions"}, new Callable<WorkoutSessionEntity>() {
      @Override
      @Nullable
      public WorkoutSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoutineId = CursorUtil.getColumnIndexOrThrow(_cursor, "routine_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActiveExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "active_exercise_id");
          final int _cursorIndexOfActiveSetIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "active_set_index");
          final int _cursorIndexOfTotalVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "total_volume_kg");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_seconds");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfLastUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated_at");
          final WorkoutSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRoutineId;
            if (_cursor.isNull(_cursorIndexOfRoutineId)) {
              _tmpRoutineId = null;
            } else {
              _tmpRoutineId = _cursor.getString(_cursorIndexOfRoutineId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpActiveExerciseId;
            if (_cursor.isNull(_cursorIndexOfActiveExerciseId)) {
              _tmpActiveExerciseId = null;
            } else {
              _tmpActiveExerciseId = _cursor.getString(_cursorIndexOfActiveExerciseId);
            }
            final int _tmpActiveSetIndex;
            _tmpActiveSetIndex = _cursor.getInt(_cursorIndexOfActiveSetIndex);
            final double _tmpTotalVolumeKg;
            _tmpTotalVolumeKg = _cursor.getDouble(_cursorIndexOfTotalVolumeKg);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpLastUpdatedAt;
            _tmpLastUpdatedAt = _cursor.getLong(_cursorIndexOfLastUpdatedAt);
            _result = new WorkoutSessionEntity(_tmpId,_tmpRoutineId,_tmpName,_tmpStartTime,_tmpEndTime,_tmpStatus,_tmpActiveExerciseId,_tmpActiveSetIndex,_tmpTotalVolumeKg,_tmpDurationSeconds,_tmpNotes,_tmpLastUpdatedAt);
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
  public Object getActiveSessionDirect(
      final Continuation<? super WorkoutSessionEntity> $completion) {
    final String _sql = "SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY start_time DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WorkoutSessionEntity>() {
      @Override
      @Nullable
      public WorkoutSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoutineId = CursorUtil.getColumnIndexOrThrow(_cursor, "routine_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActiveExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "active_exercise_id");
          final int _cursorIndexOfActiveSetIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "active_set_index");
          final int _cursorIndexOfTotalVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "total_volume_kg");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_seconds");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfLastUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated_at");
          final WorkoutSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRoutineId;
            if (_cursor.isNull(_cursorIndexOfRoutineId)) {
              _tmpRoutineId = null;
            } else {
              _tmpRoutineId = _cursor.getString(_cursorIndexOfRoutineId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpActiveExerciseId;
            if (_cursor.isNull(_cursorIndexOfActiveExerciseId)) {
              _tmpActiveExerciseId = null;
            } else {
              _tmpActiveExerciseId = _cursor.getString(_cursorIndexOfActiveExerciseId);
            }
            final int _tmpActiveSetIndex;
            _tmpActiveSetIndex = _cursor.getInt(_cursorIndexOfActiveSetIndex);
            final double _tmpTotalVolumeKg;
            _tmpTotalVolumeKg = _cursor.getDouble(_cursorIndexOfTotalVolumeKg);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpLastUpdatedAt;
            _tmpLastUpdatedAt = _cursor.getLong(_cursorIndexOfLastUpdatedAt);
            _result = new WorkoutSessionEntity(_tmpId,_tmpRoutineId,_tmpName,_tmpStartTime,_tmpEndTime,_tmpStatus,_tmpActiveExerciseId,_tmpActiveSetIndex,_tmpTotalVolumeKg,_tmpDurationSeconds,_tmpNotes,_tmpLastUpdatedAt);
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
  public Object getSessionById(final String sessionId,
      final Continuation<? super WorkoutSessionEntity> $completion) {
    final String _sql = "SELECT * FROM workout_sessions WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WorkoutSessionEntity>() {
      @Override
      @Nullable
      public WorkoutSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoutineId = CursorUtil.getColumnIndexOrThrow(_cursor, "routine_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActiveExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "active_exercise_id");
          final int _cursorIndexOfActiveSetIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "active_set_index");
          final int _cursorIndexOfTotalVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "total_volume_kg");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_seconds");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfLastUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated_at");
          final WorkoutSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRoutineId;
            if (_cursor.isNull(_cursorIndexOfRoutineId)) {
              _tmpRoutineId = null;
            } else {
              _tmpRoutineId = _cursor.getString(_cursorIndexOfRoutineId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpActiveExerciseId;
            if (_cursor.isNull(_cursorIndexOfActiveExerciseId)) {
              _tmpActiveExerciseId = null;
            } else {
              _tmpActiveExerciseId = _cursor.getString(_cursorIndexOfActiveExerciseId);
            }
            final int _tmpActiveSetIndex;
            _tmpActiveSetIndex = _cursor.getInt(_cursorIndexOfActiveSetIndex);
            final double _tmpTotalVolumeKg;
            _tmpTotalVolumeKg = _cursor.getDouble(_cursorIndexOfTotalVolumeKg);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpLastUpdatedAt;
            _tmpLastUpdatedAt = _cursor.getLong(_cursorIndexOfLastUpdatedAt);
            _result = new WorkoutSessionEntity(_tmpId,_tmpRoutineId,_tmpName,_tmpStartTime,_tmpEndTime,_tmpStatus,_tmpActiveExerciseId,_tmpActiveSetIndex,_tmpTotalVolumeKg,_tmpDurationSeconds,_tmpNotes,_tmpLastUpdatedAt);
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
  public Flow<List<WorkoutSessionEntity>> getAllSessions() {
    final String _sql = "SELECT * FROM workout_sessions ORDER BY start_time DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_sessions"}, new Callable<List<WorkoutSessionEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoutineId = CursorUtil.getColumnIndexOrThrow(_cursor, "routine_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActiveExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "active_exercise_id");
          final int _cursorIndexOfActiveSetIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "active_set_index");
          final int _cursorIndexOfTotalVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "total_volume_kg");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_seconds");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfLastUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated_at");
          final List<WorkoutSessionEntity> _result = new ArrayList<WorkoutSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSessionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRoutineId;
            if (_cursor.isNull(_cursorIndexOfRoutineId)) {
              _tmpRoutineId = null;
            } else {
              _tmpRoutineId = _cursor.getString(_cursorIndexOfRoutineId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpActiveExerciseId;
            if (_cursor.isNull(_cursorIndexOfActiveExerciseId)) {
              _tmpActiveExerciseId = null;
            } else {
              _tmpActiveExerciseId = _cursor.getString(_cursorIndexOfActiveExerciseId);
            }
            final int _tmpActiveSetIndex;
            _tmpActiveSetIndex = _cursor.getInt(_cursorIndexOfActiveSetIndex);
            final double _tmpTotalVolumeKg;
            _tmpTotalVolumeKg = _cursor.getDouble(_cursorIndexOfTotalVolumeKg);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpLastUpdatedAt;
            _tmpLastUpdatedAt = _cursor.getLong(_cursorIndexOfLastUpdatedAt);
            _item = new WorkoutSessionEntity(_tmpId,_tmpRoutineId,_tmpName,_tmpStartTime,_tmpEndTime,_tmpStatus,_tmpActiveExerciseId,_tmpActiveSetIndex,_tmpTotalVolumeKg,_tmpDurationSeconds,_tmpNotes,_tmpLastUpdatedAt);
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
  public Object getAllSessionsSync(
      final Continuation<? super List<WorkoutSessionEntity>> $completion) {
    final String _sql = "SELECT * FROM workout_sessions ORDER BY start_time DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkoutSessionEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoutineId = CursorUtil.getColumnIndexOrThrow(_cursor, "routine_id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "start_time");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "end_time");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActiveExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "active_exercise_id");
          final int _cursorIndexOfActiveSetIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "active_set_index");
          final int _cursorIndexOfTotalVolumeKg = CursorUtil.getColumnIndexOrThrow(_cursor, "total_volume_kg");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_seconds");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfLastUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated_at");
          final List<WorkoutSessionEntity> _result = new ArrayList<WorkoutSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSessionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpRoutineId;
            if (_cursor.isNull(_cursorIndexOfRoutineId)) {
              _tmpRoutineId = null;
            } else {
              _tmpRoutineId = _cursor.getString(_cursorIndexOfRoutineId);
            }
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpActiveExerciseId;
            if (_cursor.isNull(_cursorIndexOfActiveExerciseId)) {
              _tmpActiveExerciseId = null;
            } else {
              _tmpActiveExerciseId = _cursor.getString(_cursorIndexOfActiveExerciseId);
            }
            final int _tmpActiveSetIndex;
            _tmpActiveSetIndex = _cursor.getInt(_cursorIndexOfActiveSetIndex);
            final double _tmpTotalVolumeKg;
            _tmpTotalVolumeKg = _cursor.getDouble(_cursorIndexOfTotalVolumeKg);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpLastUpdatedAt;
            _tmpLastUpdatedAt = _cursor.getLong(_cursorIndexOfLastUpdatedAt);
            _item = new WorkoutSessionEntity(_tmpId,_tmpRoutineId,_tmpName,_tmpStartTime,_tmpEndTime,_tmpStatus,_tmpActiveExerciseId,_tmpActiveSetIndex,_tmpTotalVolumeKg,_tmpDurationSeconds,_tmpNotes,_tmpLastUpdatedAt);
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
  public Flow<List<WorkoutSetEntity>> getSetsForSession(final String sessionId) {
    final String _sql = "SELECT * FROM workout_sets WHERE session_id = ? ORDER BY set_order ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"workout_sets"}, new Callable<List<WorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "session_id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfSetOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "set_order");
          final int _cursorIndexOfSetType = CursorUtil.getColumnIndexOrThrow(_cursor, "set_type");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final int _cursorIndexOfRir = CursorUtil.getColumnIndexOrThrow(_cursor, "rir");
          final int _cursorIndexOfTempo = CursorUtil.getColumnIndexOrThrow(_cursor, "tempo");
          final int _cursorIndexOfRestSecondsTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "rest_seconds_taken");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_completed");
          final int _cursorIndexOfIsPersonalRecord = CursorUtil.getColumnIndexOrThrow(_cursor, "is_personal_record");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completed_at");
          final List<WorkoutSetEntity> _result = new ArrayList<WorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final int _tmpSetOrder;
            _tmpSetOrder = _cursor.getInt(_cursorIndexOfSetOrder);
            final String _tmpSetType;
            _tmpSetType = _cursor.getString(_cursorIndexOfSetType);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            final Integer _tmpRir;
            if (_cursor.isNull(_cursorIndexOfRir)) {
              _tmpRir = null;
            } else {
              _tmpRir = _cursor.getInt(_cursorIndexOfRir);
            }
            final String _tmpTempo;
            if (_cursor.isNull(_cursorIndexOfTempo)) {
              _tmpTempo = null;
            } else {
              _tmpTempo = _cursor.getString(_cursorIndexOfTempo);
            }
            final Integer _tmpRestSecondsTaken;
            if (_cursor.isNull(_cursorIndexOfRestSecondsTaken)) {
              _tmpRestSecondsTaken = null;
            } else {
              _tmpRestSecondsTaken = _cursor.getInt(_cursorIndexOfRestSecondsTaken);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final boolean _tmpIsPersonalRecord;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPersonalRecord);
            _tmpIsPersonalRecord = _tmp_1 != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new WorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpSetOrder,_tmpSetType,_tmpWeightKg,_tmpReps,_tmpRpe,_tmpRir,_tmpTempo,_tmpRestSecondsTaken,_tmpIsCompleted,_tmpIsPersonalRecord,_tmpCompletedAt);
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
  public Object getSetsForSessionDirect(final String sessionId,
      final Continuation<? super List<WorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM workout_sets WHERE session_id = ? ORDER BY set_order ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "session_id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfSetOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "set_order");
          final int _cursorIndexOfSetType = CursorUtil.getColumnIndexOrThrow(_cursor, "set_type");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final int _cursorIndexOfRir = CursorUtil.getColumnIndexOrThrow(_cursor, "rir");
          final int _cursorIndexOfTempo = CursorUtil.getColumnIndexOrThrow(_cursor, "tempo");
          final int _cursorIndexOfRestSecondsTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "rest_seconds_taken");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_completed");
          final int _cursorIndexOfIsPersonalRecord = CursorUtil.getColumnIndexOrThrow(_cursor, "is_personal_record");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completed_at");
          final List<WorkoutSetEntity> _result = new ArrayList<WorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final int _tmpSetOrder;
            _tmpSetOrder = _cursor.getInt(_cursorIndexOfSetOrder);
            final String _tmpSetType;
            _tmpSetType = _cursor.getString(_cursorIndexOfSetType);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            final Integer _tmpRir;
            if (_cursor.isNull(_cursorIndexOfRir)) {
              _tmpRir = null;
            } else {
              _tmpRir = _cursor.getInt(_cursorIndexOfRir);
            }
            final String _tmpTempo;
            if (_cursor.isNull(_cursorIndexOfTempo)) {
              _tmpTempo = null;
            } else {
              _tmpTempo = _cursor.getString(_cursorIndexOfTempo);
            }
            final Integer _tmpRestSecondsTaken;
            if (_cursor.isNull(_cursorIndexOfRestSecondsTaken)) {
              _tmpRestSecondsTaken = null;
            } else {
              _tmpRestSecondsTaken = _cursor.getInt(_cursorIndexOfRestSecondsTaken);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final boolean _tmpIsPersonalRecord;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPersonalRecord);
            _tmpIsPersonalRecord = _tmp_1 != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new WorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpSetOrder,_tmpSetType,_tmpWeightKg,_tmpReps,_tmpRpe,_tmpRir,_tmpTempo,_tmpRestSecondsTaken,_tmpIsCompleted,_tmpIsPersonalRecord,_tmpCompletedAt);
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
  public Object getCompletedSetsForExerciseSync(final String exerciseId,
      final Continuation<? super List<WorkoutSetEntity>> $completion) {
    final String _sql = "SELECT * FROM workout_sets WHERE exercise_id = ? ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, exerciseId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WorkoutSetEntity>>() {
      @Override
      @NonNull
      public List<WorkoutSetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "session_id");
          final int _cursorIndexOfExerciseId = CursorUtil.getColumnIndexOrThrow(_cursor, "exercise_id");
          final int _cursorIndexOfSetOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "set_order");
          final int _cursorIndexOfSetType = CursorUtil.getColumnIndexOrThrow(_cursor, "set_type");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfReps = CursorUtil.getColumnIndexOrThrow(_cursor, "reps");
          final int _cursorIndexOfRpe = CursorUtil.getColumnIndexOrThrow(_cursor, "rpe");
          final int _cursorIndexOfRir = CursorUtil.getColumnIndexOrThrow(_cursor, "rir");
          final int _cursorIndexOfTempo = CursorUtil.getColumnIndexOrThrow(_cursor, "tempo");
          final int _cursorIndexOfRestSecondsTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "rest_seconds_taken");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "is_completed");
          final int _cursorIndexOfIsPersonalRecord = CursorUtil.getColumnIndexOrThrow(_cursor, "is_personal_record");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completed_at");
          final List<WorkoutSetEntity> _result = new ArrayList<WorkoutSetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorkoutSetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpExerciseId;
            _tmpExerciseId = _cursor.getString(_cursorIndexOfExerciseId);
            final int _tmpSetOrder;
            _tmpSetOrder = _cursor.getInt(_cursorIndexOfSetOrder);
            final String _tmpSetType;
            _tmpSetType = _cursor.getString(_cursorIndexOfSetType);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final int _tmpReps;
            _tmpReps = _cursor.getInt(_cursorIndexOfReps);
            final Double _tmpRpe;
            if (_cursor.isNull(_cursorIndexOfRpe)) {
              _tmpRpe = null;
            } else {
              _tmpRpe = _cursor.getDouble(_cursorIndexOfRpe);
            }
            final Integer _tmpRir;
            if (_cursor.isNull(_cursorIndexOfRir)) {
              _tmpRir = null;
            } else {
              _tmpRir = _cursor.getInt(_cursorIndexOfRir);
            }
            final String _tmpTempo;
            if (_cursor.isNull(_cursorIndexOfTempo)) {
              _tmpTempo = null;
            } else {
              _tmpTempo = _cursor.getString(_cursorIndexOfTempo);
            }
            final Integer _tmpRestSecondsTaken;
            if (_cursor.isNull(_cursorIndexOfRestSecondsTaken)) {
              _tmpRestSecondsTaken = null;
            } else {
              _tmpRestSecondsTaken = _cursor.getInt(_cursorIndexOfRestSecondsTaken);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final boolean _tmpIsPersonalRecord;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPersonalRecord);
            _tmpIsPersonalRecord = _tmp_1 != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _item = new WorkoutSetEntity(_tmpId,_tmpSessionId,_tmpExerciseId,_tmpSetOrder,_tmpSetType,_tmpWeightKg,_tmpReps,_tmpRpe,_tmpRir,_tmpTempo,_tmpRestSecondsTaken,_tmpIsCompleted,_tmpIsPersonalRecord,_tmpCompletedAt);
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
