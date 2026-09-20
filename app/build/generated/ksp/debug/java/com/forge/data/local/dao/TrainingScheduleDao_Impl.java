package com.forge.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.forge.data.local.entity.TrainingScheduleEntity;
import java.lang.Class;
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
public final class TrainingScheduleDao_Impl implements TrainingScheduleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrainingScheduleEntity> __insertionAdapterOfTrainingScheduleEntity;

  private final EntityDeletionOrUpdateAdapter<TrainingScheduleEntity> __updateAdapterOfTrainingScheduleEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public TrainingScheduleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrainingScheduleEntity = new EntityInsertionAdapter<TrainingScheduleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `training_schedule` (`day_of_week`,`is_training_day`,`focus`,`template_id`,`target_duration_min`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrainingScheduleEntity entity) {
        statement.bindLong(1, entity.getDayOfWeek());
        final int _tmp = entity.isTrainingDay() ? 1 : 0;
        statement.bindLong(2, _tmp);
        statement.bindString(3, entity.getFocus());
        if (entity.getTemplateId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTemplateId());
        }
        statement.bindLong(5, entity.getTargetDurationMin());
      }
    };
    this.__updateAdapterOfTrainingScheduleEntity = new EntityDeletionOrUpdateAdapter<TrainingScheduleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `training_schedule` SET `day_of_week` = ?,`is_training_day` = ?,`focus` = ?,`template_id` = ?,`target_duration_min` = ? WHERE `day_of_week` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrainingScheduleEntity entity) {
        statement.bindLong(1, entity.getDayOfWeek());
        final int _tmp = entity.isTrainingDay() ? 1 : 0;
        statement.bindLong(2, _tmp);
        statement.bindString(3, entity.getFocus());
        if (entity.getTemplateId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTemplateId());
        }
        statement.bindLong(5, entity.getTargetDurationMin());
        statement.bindLong(6, entity.getDayOfWeek());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM training_schedule";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrReplace(final List<TrainingScheduleEntity> schedule,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTrainingScheduleEntity.insert(schedule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDay(final TrainingScheduleEntity day,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTrainingScheduleEntity.handle(day);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
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
  public Flow<List<TrainingScheduleEntity>> getSchedule() {
    final String _sql = "SELECT * FROM training_schedule ORDER BY day_of_week ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"training_schedule"}, new Callable<List<TrainingScheduleEntity>>() {
      @Override
      @NonNull
      public List<TrainingScheduleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "day_of_week");
          final int _cursorIndexOfIsTrainingDay = CursorUtil.getColumnIndexOrThrow(_cursor, "is_training_day");
          final int _cursorIndexOfFocus = CursorUtil.getColumnIndexOrThrow(_cursor, "focus");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "template_id");
          final int _cursorIndexOfTargetDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "target_duration_min");
          final List<TrainingScheduleEntity> _result = new ArrayList<TrainingScheduleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrainingScheduleEntity _item;
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final boolean _tmpIsTrainingDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsTrainingDay);
            _tmpIsTrainingDay = _tmp != 0;
            final String _tmpFocus;
            _tmpFocus = _cursor.getString(_cursorIndexOfFocus);
            final String _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getString(_cursorIndexOfTemplateId);
            }
            final int _tmpTargetDurationMin;
            _tmpTargetDurationMin = _cursor.getInt(_cursorIndexOfTargetDurationMin);
            _item = new TrainingScheduleEntity(_tmpDayOfWeek,_tmpIsTrainingDay,_tmpFocus,_tmpTemplateId,_tmpTargetDurationMin);
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
  public Object getScheduleSync(
      final Continuation<? super List<TrainingScheduleEntity>> $completion) {
    final String _sql = "SELECT * FROM training_schedule ORDER BY day_of_week ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrainingScheduleEntity>>() {
      @Override
      @NonNull
      public List<TrainingScheduleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "day_of_week");
          final int _cursorIndexOfIsTrainingDay = CursorUtil.getColumnIndexOrThrow(_cursor, "is_training_day");
          final int _cursorIndexOfFocus = CursorUtil.getColumnIndexOrThrow(_cursor, "focus");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "template_id");
          final int _cursorIndexOfTargetDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "target_duration_min");
          final List<TrainingScheduleEntity> _result = new ArrayList<TrainingScheduleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrainingScheduleEntity _item;
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final boolean _tmpIsTrainingDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsTrainingDay);
            _tmpIsTrainingDay = _tmp != 0;
            final String _tmpFocus;
            _tmpFocus = _cursor.getString(_cursorIndexOfFocus);
            final String _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getString(_cursorIndexOfTemplateId);
            }
            final int _tmpTargetDurationMin;
            _tmpTargetDurationMin = _cursor.getInt(_cursorIndexOfTargetDurationMin);
            _item = new TrainingScheduleEntity(_tmpDayOfWeek,_tmpIsTrainingDay,_tmpFocus,_tmpTemplateId,_tmpTargetDurationMin);
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
  public Flow<TrainingScheduleEntity> getScheduleForDay(final int dayOfWeek) {
    final String _sql = "SELECT * FROM training_schedule WHERE day_of_week = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayOfWeek);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"training_schedule"}, new Callable<TrainingScheduleEntity>() {
      @Override
      @Nullable
      public TrainingScheduleEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "day_of_week");
          final int _cursorIndexOfIsTrainingDay = CursorUtil.getColumnIndexOrThrow(_cursor, "is_training_day");
          final int _cursorIndexOfFocus = CursorUtil.getColumnIndexOrThrow(_cursor, "focus");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "template_id");
          final int _cursorIndexOfTargetDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "target_duration_min");
          final TrainingScheduleEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final boolean _tmpIsTrainingDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsTrainingDay);
            _tmpIsTrainingDay = _tmp != 0;
            final String _tmpFocus;
            _tmpFocus = _cursor.getString(_cursorIndexOfFocus);
            final String _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getString(_cursorIndexOfTemplateId);
            }
            final int _tmpTargetDurationMin;
            _tmpTargetDurationMin = _cursor.getInt(_cursorIndexOfTargetDurationMin);
            _result = new TrainingScheduleEntity(_tmpDayOfWeek,_tmpIsTrainingDay,_tmpFocus,_tmpTemplateId,_tmpTargetDurationMin);
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
  public Object getScheduleForDaySync(final int dayOfWeek,
      final Continuation<? super TrainingScheduleEntity> $completion) {
    final String _sql = "SELECT * FROM training_schedule WHERE day_of_week = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayOfWeek);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TrainingScheduleEntity>() {
      @Override
      @Nullable
      public TrainingScheduleEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "day_of_week");
          final int _cursorIndexOfIsTrainingDay = CursorUtil.getColumnIndexOrThrow(_cursor, "is_training_day");
          final int _cursorIndexOfFocus = CursorUtil.getColumnIndexOrThrow(_cursor, "focus");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "template_id");
          final int _cursorIndexOfTargetDurationMin = CursorUtil.getColumnIndexOrThrow(_cursor, "target_duration_min");
          final TrainingScheduleEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final boolean _tmpIsTrainingDay;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsTrainingDay);
            _tmpIsTrainingDay = _tmp != 0;
            final String _tmpFocus;
            _tmpFocus = _cursor.getString(_cursorIndexOfFocus);
            final String _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getString(_cursorIndexOfTemplateId);
            }
            final int _tmpTargetDurationMin;
            _tmpTargetDurationMin = _cursor.getInt(_cursorIndexOfTargetDurationMin);
            _result = new TrainingScheduleEntity(_tmpDayOfWeek,_tmpIsTrainingDay,_tmpFocus,_tmpTemplateId,_tmpTargetDurationMin);
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
