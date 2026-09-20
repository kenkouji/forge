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
import com.forge.data.local.entity.DailyActivityEntity;
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
public final class DailyActivityDao_Impl implements DailyActivityDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyActivityEntity> __insertionAdapterOfDailyActivityEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public DailyActivityDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyActivityEntity = new EntityInsertionAdapter<DailyActivityEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_activity` (`date`,`steps`,`active_calories`,`total_calories`,`distance_meters`,`is_calories_measured`,`has_health_connect_sync`,`last_sync_timestamp`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyActivityEntity entity) {
        statement.bindString(1, entity.getDate());
        statement.bindLong(2, entity.getSteps());
        statement.bindLong(3, entity.getActiveCalories());
        statement.bindLong(4, entity.getTotalCalories());
        statement.bindDouble(5, entity.getDistanceMeters());
        final int _tmp = entity.isCaloriesMeasured() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.getHasHealthConnectSync() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindLong(8, entity.getLastSyncTimestamp());
        statement.bindLong(9, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_activity";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final DailyActivityEntity activity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyActivityEntity.insert(activity);
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
  public Flow<DailyActivityEntity> getActivityForDate(final String date) {
    final String _sql = "SELECT * FROM daily_activity WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_activity"}, new Callable<DailyActivityEntity>() {
      @Override
      @Nullable
      public DailyActivityEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfActiveCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "active_calories");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "total_calories");
          final int _cursorIndexOfDistanceMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "distance_meters");
          final int _cursorIndexOfIsCaloriesMeasured = CursorUtil.getColumnIndexOrThrow(_cursor, "is_calories_measured");
          final int _cursorIndexOfHasHealthConnectSync = CursorUtil.getColumnIndexOrThrow(_cursor, "has_health_connect_sync");
          final int _cursorIndexOfLastSyncTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "last_sync_timestamp");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final DailyActivityEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final int _tmpActiveCalories;
            _tmpActiveCalories = _cursor.getInt(_cursorIndexOfActiveCalories);
            final int _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getInt(_cursorIndexOfTotalCalories);
            final double _tmpDistanceMeters;
            _tmpDistanceMeters = _cursor.getDouble(_cursorIndexOfDistanceMeters);
            final boolean _tmpIsCaloriesMeasured;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCaloriesMeasured);
            _tmpIsCaloriesMeasured = _tmp != 0;
            final boolean _tmpHasHealthConnectSync;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasHealthConnectSync);
            _tmpHasHealthConnectSync = _tmp_1 != 0;
            final long _tmpLastSyncTimestamp;
            _tmpLastSyncTimestamp = _cursor.getLong(_cursorIndexOfLastSyncTimestamp);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new DailyActivityEntity(_tmpDate,_tmpSteps,_tmpActiveCalories,_tmpTotalCalories,_tmpDistanceMeters,_tmpIsCaloriesMeasured,_tmpHasHealthConnectSync,_tmpLastSyncTimestamp,_tmpUpdatedAt);
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
  public Object getActivityForDateSync(final String date,
      final Continuation<? super DailyActivityEntity> $completion) {
    final String _sql = "SELECT * FROM daily_activity WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailyActivityEntity>() {
      @Override
      @Nullable
      public DailyActivityEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfActiveCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "active_calories");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "total_calories");
          final int _cursorIndexOfDistanceMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "distance_meters");
          final int _cursorIndexOfIsCaloriesMeasured = CursorUtil.getColumnIndexOrThrow(_cursor, "is_calories_measured");
          final int _cursorIndexOfHasHealthConnectSync = CursorUtil.getColumnIndexOrThrow(_cursor, "has_health_connect_sync");
          final int _cursorIndexOfLastSyncTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "last_sync_timestamp");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final DailyActivityEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final int _tmpActiveCalories;
            _tmpActiveCalories = _cursor.getInt(_cursorIndexOfActiveCalories);
            final int _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getInt(_cursorIndexOfTotalCalories);
            final double _tmpDistanceMeters;
            _tmpDistanceMeters = _cursor.getDouble(_cursorIndexOfDistanceMeters);
            final boolean _tmpIsCaloriesMeasured;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCaloriesMeasured);
            _tmpIsCaloriesMeasured = _tmp != 0;
            final boolean _tmpHasHealthConnectSync;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasHealthConnectSync);
            _tmpHasHealthConnectSync = _tmp_1 != 0;
            final long _tmpLastSyncTimestamp;
            _tmpLastSyncTimestamp = _cursor.getLong(_cursorIndexOfLastSyncTimestamp);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new DailyActivityEntity(_tmpDate,_tmpSteps,_tmpActiveCalories,_tmpTotalCalories,_tmpDistanceMeters,_tmpIsCaloriesMeasured,_tmpHasHealthConnectSync,_tmpLastSyncTimestamp,_tmpUpdatedAt);
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
  public Flow<List<DailyActivityEntity>> getRecentActivities(final int limit) {
    final String _sql = "SELECT * FROM daily_activity ORDER BY date DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_activity"}, new Callable<List<DailyActivityEntity>>() {
      @Override
      @NonNull
      public List<DailyActivityEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfActiveCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "active_calories");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "total_calories");
          final int _cursorIndexOfDistanceMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "distance_meters");
          final int _cursorIndexOfIsCaloriesMeasured = CursorUtil.getColumnIndexOrThrow(_cursor, "is_calories_measured");
          final int _cursorIndexOfHasHealthConnectSync = CursorUtil.getColumnIndexOrThrow(_cursor, "has_health_connect_sync");
          final int _cursorIndexOfLastSyncTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "last_sync_timestamp");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<DailyActivityEntity> _result = new ArrayList<DailyActivityEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyActivityEntity _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final int _tmpActiveCalories;
            _tmpActiveCalories = _cursor.getInt(_cursorIndexOfActiveCalories);
            final int _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getInt(_cursorIndexOfTotalCalories);
            final double _tmpDistanceMeters;
            _tmpDistanceMeters = _cursor.getDouble(_cursorIndexOfDistanceMeters);
            final boolean _tmpIsCaloriesMeasured;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCaloriesMeasured);
            _tmpIsCaloriesMeasured = _tmp != 0;
            final boolean _tmpHasHealthConnectSync;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasHealthConnectSync);
            _tmpHasHealthConnectSync = _tmp_1 != 0;
            final long _tmpLastSyncTimestamp;
            _tmpLastSyncTimestamp = _cursor.getLong(_cursorIndexOfLastSyncTimestamp);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new DailyActivityEntity(_tmpDate,_tmpSteps,_tmpActiveCalories,_tmpTotalCalories,_tmpDistanceMeters,_tmpIsCaloriesMeasured,_tmpHasHealthConnectSync,_tmpLastSyncTimestamp,_tmpUpdatedAt);
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
  public Object getAllActivitiesSync(
      final Continuation<? super List<DailyActivityEntity>> $completion) {
    final String _sql = "SELECT * FROM daily_activity ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyActivityEntity>>() {
      @Override
      @NonNull
      public List<DailyActivityEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfActiveCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "active_calories");
          final int _cursorIndexOfTotalCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "total_calories");
          final int _cursorIndexOfDistanceMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "distance_meters");
          final int _cursorIndexOfIsCaloriesMeasured = CursorUtil.getColumnIndexOrThrow(_cursor, "is_calories_measured");
          final int _cursorIndexOfHasHealthConnectSync = CursorUtil.getColumnIndexOrThrow(_cursor, "has_health_connect_sync");
          final int _cursorIndexOfLastSyncTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "last_sync_timestamp");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final List<DailyActivityEntity> _result = new ArrayList<DailyActivityEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyActivityEntity _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpSteps;
            _tmpSteps = _cursor.getInt(_cursorIndexOfSteps);
            final int _tmpActiveCalories;
            _tmpActiveCalories = _cursor.getInt(_cursorIndexOfActiveCalories);
            final int _tmpTotalCalories;
            _tmpTotalCalories = _cursor.getInt(_cursorIndexOfTotalCalories);
            final double _tmpDistanceMeters;
            _tmpDistanceMeters = _cursor.getDouble(_cursorIndexOfDistanceMeters);
            final boolean _tmpIsCaloriesMeasured;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCaloriesMeasured);
            _tmpIsCaloriesMeasured = _tmp != 0;
            final boolean _tmpHasHealthConnectSync;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasHealthConnectSync);
            _tmpHasHealthConnectSync = _tmp_1 != 0;
            final long _tmpLastSyncTimestamp;
            _tmpLastSyncTimestamp = _cursor.getLong(_cursorIndexOfLastSyncTimestamp);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new DailyActivityEntity(_tmpDate,_tmpSteps,_tmpActiveCalories,_tmpTotalCalories,_tmpDistanceMeters,_tmpIsCaloriesMeasured,_tmpHasHealthConnectSync,_tmpLastSyncTimestamp,_tmpUpdatedAt);
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
