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
import com.forge.data.local.entity.TransformationCheckInEntity;
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
public final class TransformationDao_Impl implements TransformationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransformationCheckInEntity> __insertionAdapterOfTransformationCheckInEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCheckIn;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public TransformationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransformationCheckInEntity = new EntityInsertionAdapter<TransformationCheckInEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transformation_checkins` (`id`,`week_number`,`date`,`front_encrypted_path`,`side_encrypted_path`,`back_encrypted_path`,`weight_kg`,`notes`,`created_at`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransformationCheckInEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getWeekNumber());
        statement.bindString(3, entity.getDate());
        if (entity.getFrontEncryptedPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFrontEncryptedPath());
        }
        if (entity.getSideEncryptedPath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSideEncryptedPath());
        }
        if (entity.getBackEncryptedPath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getBackEncryptedPath());
        }
        if (entity.getWeightKg() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getWeightKg());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getNotes());
        }
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDeleteCheckIn = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transformation_checkins WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transformation_checkins";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final TransformationCheckInEntity checkIn,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransformationCheckInEntity.insert(checkIn);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCheckIn(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCheckIn.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteCheckIn.release(_stmt);
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
  public Flow<List<TransformationCheckInEntity>> getCheckIns() {
    final String _sql = "SELECT * FROM transformation_checkins ORDER BY week_number ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transformation_checkins"}, new Callable<List<TransformationCheckInEntity>>() {
      @Override
      @NonNull
      public List<TransformationCheckInEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWeekNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "week_number");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfFrontEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "front_encrypted_path");
          final int _cursorIndexOfSideEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "side_encrypted_path");
          final int _cursorIndexOfBackEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "back_encrypted_path");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<TransformationCheckInEntity> _result = new ArrayList<TransformationCheckInEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransformationCheckInEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpWeekNumber;
            _tmpWeekNumber = _cursor.getInt(_cursorIndexOfWeekNumber);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpFrontEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfFrontEncryptedPath)) {
              _tmpFrontEncryptedPath = null;
            } else {
              _tmpFrontEncryptedPath = _cursor.getString(_cursorIndexOfFrontEncryptedPath);
            }
            final String _tmpSideEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfSideEncryptedPath)) {
              _tmpSideEncryptedPath = null;
            } else {
              _tmpSideEncryptedPath = _cursor.getString(_cursorIndexOfSideEncryptedPath);
            }
            final String _tmpBackEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfBackEncryptedPath)) {
              _tmpBackEncryptedPath = null;
            } else {
              _tmpBackEncryptedPath = _cursor.getString(_cursorIndexOfBackEncryptedPath);
            }
            final Double _tmpWeightKg;
            if (_cursor.isNull(_cursorIndexOfWeightKg)) {
              _tmpWeightKg = null;
            } else {
              _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransformationCheckInEntity(_tmpId,_tmpWeekNumber,_tmpDate,_tmpFrontEncryptedPath,_tmpSideEncryptedPath,_tmpBackEncryptedPath,_tmpWeightKg,_tmpNotes,_tmpCreatedAt);
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
  public Object getCheckInsSync(
      final Continuation<? super List<TransformationCheckInEntity>> $completion) {
    final String _sql = "SELECT * FROM transformation_checkins ORDER BY week_number ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransformationCheckInEntity>>() {
      @Override
      @NonNull
      public List<TransformationCheckInEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWeekNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "week_number");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfFrontEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "front_encrypted_path");
          final int _cursorIndexOfSideEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "side_encrypted_path");
          final int _cursorIndexOfBackEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "back_encrypted_path");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<TransformationCheckInEntity> _result = new ArrayList<TransformationCheckInEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransformationCheckInEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpWeekNumber;
            _tmpWeekNumber = _cursor.getInt(_cursorIndexOfWeekNumber);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpFrontEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfFrontEncryptedPath)) {
              _tmpFrontEncryptedPath = null;
            } else {
              _tmpFrontEncryptedPath = _cursor.getString(_cursorIndexOfFrontEncryptedPath);
            }
            final String _tmpSideEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfSideEncryptedPath)) {
              _tmpSideEncryptedPath = null;
            } else {
              _tmpSideEncryptedPath = _cursor.getString(_cursorIndexOfSideEncryptedPath);
            }
            final String _tmpBackEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfBackEncryptedPath)) {
              _tmpBackEncryptedPath = null;
            } else {
              _tmpBackEncryptedPath = _cursor.getString(_cursorIndexOfBackEncryptedPath);
            }
            final Double _tmpWeightKg;
            if (_cursor.isNull(_cursorIndexOfWeightKg)) {
              _tmpWeightKg = null;
            } else {
              _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransformationCheckInEntity(_tmpId,_tmpWeekNumber,_tmpDate,_tmpFrontEncryptedPath,_tmpSideEncryptedPath,_tmpBackEncryptedPath,_tmpWeightKg,_tmpNotes,_tmpCreatedAt);
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
  public Flow<TransformationCheckInEntity> getCheckInForWeek(final int weekNumber) {
    final String _sql = "SELECT * FROM transformation_checkins WHERE week_number = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, weekNumber);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transformation_checkins"}, new Callable<TransformationCheckInEntity>() {
      @Override
      @Nullable
      public TransformationCheckInEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWeekNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "week_number");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfFrontEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "front_encrypted_path");
          final int _cursorIndexOfSideEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "side_encrypted_path");
          final int _cursorIndexOfBackEncryptedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "back_encrypted_path");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weight_kg");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final TransformationCheckInEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpWeekNumber;
            _tmpWeekNumber = _cursor.getInt(_cursorIndexOfWeekNumber);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpFrontEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfFrontEncryptedPath)) {
              _tmpFrontEncryptedPath = null;
            } else {
              _tmpFrontEncryptedPath = _cursor.getString(_cursorIndexOfFrontEncryptedPath);
            }
            final String _tmpSideEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfSideEncryptedPath)) {
              _tmpSideEncryptedPath = null;
            } else {
              _tmpSideEncryptedPath = _cursor.getString(_cursorIndexOfSideEncryptedPath);
            }
            final String _tmpBackEncryptedPath;
            if (_cursor.isNull(_cursorIndexOfBackEncryptedPath)) {
              _tmpBackEncryptedPath = null;
            } else {
              _tmpBackEncryptedPath = _cursor.getString(_cursorIndexOfBackEncryptedPath);
            }
            final Double _tmpWeightKg;
            if (_cursor.isNull(_cursorIndexOfWeightKg)) {
              _tmpWeightKg = null;
            } else {
              _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new TransformationCheckInEntity(_tmpId,_tmpWeekNumber,_tmpDate,_tmpFrontEncryptedPath,_tmpSideEncryptedPath,_tmpBackEncryptedPath,_tmpWeightKg,_tmpNotes,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
