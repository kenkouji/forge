package com.forge.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.forge.data.local.entity.AppSettingsEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class AppSettingsDao_Impl implements AppSettingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AppSettingsEntity> __insertionAdapterOfAppSettingsEntity;

  public AppSettingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAppSettingsEntity = new EntityInsertionAdapter<AppSettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_settings` (`id`,`reduce_motion`,`particles_enabled`,`haptics_enabled`,`health_connect_enabled`,`auto_lock_vault_on_background`,`notif_workout_reminders`,`notif_streak_reminders`,`notif_motivation`,`notif_pre_workout_alerts`,`notif_post_workout_congrats`,`notif_nutrition_reminders`,`notif_hydration_reminders`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AppSettingsEntity entity) {
        statement.bindLong(1, entity.getId());
        final int _tmp = entity.getReduceMotion() ? 1 : 0;
        statement.bindLong(2, _tmp);
        final int _tmp_1 = entity.getParticlesEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp_1);
        final int _tmp_2 = entity.getHapticsEnabled() ? 1 : 0;
        statement.bindLong(4, _tmp_2);
        final int _tmp_3 = entity.getHealthConnectEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp_3);
        final int _tmp_4 = entity.getAutoLockVaultOnBackground() ? 1 : 0;
        statement.bindLong(6, _tmp_4);
        final int _tmp_5 = entity.getNotifWorkoutReminders() ? 1 : 0;
        statement.bindLong(7, _tmp_5);
        final int _tmp_6 = entity.getNotifStreakReminders() ? 1 : 0;
        statement.bindLong(8, _tmp_6);
        final int _tmp_7 = entity.getNotifMotivation() ? 1 : 0;
        statement.bindLong(9, _tmp_7);
        final int _tmp_8 = entity.getNotifPreWorkoutAlerts() ? 1 : 0;
        statement.bindLong(10, _tmp_8);
        final int _tmp_9 = entity.getNotifPostWorkoutCongrats() ? 1 : 0;
        statement.bindLong(11, _tmp_9);
        final int _tmp_10 = entity.getNotifNutritionReminders() ? 1 : 0;
        statement.bindLong(12, _tmp_10);
        final int _tmp_11 = entity.getNotifHydrationReminders() ? 1 : 0;
        statement.bindLong(13, _tmp_11);
        statement.bindLong(14, entity.getUpdatedAt());
      }
    };
  }

  @Override
  public Object insertOrUpdate(final AppSettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAppSettingsEntity.insert(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<AppSettingsEntity> getSettings() {
    final String _sql = "SELECT * FROM app_settings WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"app_settings"}, new Callable<AppSettingsEntity>() {
      @Override
      @Nullable
      public AppSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfReduceMotion = CursorUtil.getColumnIndexOrThrow(_cursor, "reduce_motion");
          final int _cursorIndexOfParticlesEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "particles_enabled");
          final int _cursorIndexOfHapticsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "haptics_enabled");
          final int _cursorIndexOfHealthConnectEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "health_connect_enabled");
          final int _cursorIndexOfAutoLockVaultOnBackground = CursorUtil.getColumnIndexOrThrow(_cursor, "auto_lock_vault_on_background");
          final int _cursorIndexOfNotifWorkoutReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_workout_reminders");
          final int _cursorIndexOfNotifStreakReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_streak_reminders");
          final int _cursorIndexOfNotifMotivation = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_motivation");
          final int _cursorIndexOfNotifPreWorkoutAlerts = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_pre_workout_alerts");
          final int _cursorIndexOfNotifPostWorkoutCongrats = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_post_workout_congrats");
          final int _cursorIndexOfNotifNutritionReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_nutrition_reminders");
          final int _cursorIndexOfNotifHydrationReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_hydration_reminders");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final AppSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final boolean _tmpReduceMotion;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfReduceMotion);
            _tmpReduceMotion = _tmp != 0;
            final boolean _tmpParticlesEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfParticlesEnabled);
            _tmpParticlesEnabled = _tmp_1 != 0;
            final boolean _tmpHapticsEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfHapticsEnabled);
            _tmpHapticsEnabled = _tmp_2 != 0;
            final boolean _tmpHealthConnectEnabled;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfHealthConnectEnabled);
            _tmpHealthConnectEnabled = _tmp_3 != 0;
            final boolean _tmpAutoLockVaultOnBackground;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAutoLockVaultOnBackground);
            _tmpAutoLockVaultOnBackground = _tmp_4 != 0;
            final boolean _tmpNotifWorkoutReminders;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfNotifWorkoutReminders);
            _tmpNotifWorkoutReminders = _tmp_5 != 0;
            final boolean _tmpNotifStreakReminders;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfNotifStreakReminders);
            _tmpNotifStreakReminders = _tmp_6 != 0;
            final boolean _tmpNotifMotivation;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfNotifMotivation);
            _tmpNotifMotivation = _tmp_7 != 0;
            final boolean _tmpNotifPreWorkoutAlerts;
            final int _tmp_8;
            _tmp_8 = _cursor.getInt(_cursorIndexOfNotifPreWorkoutAlerts);
            _tmpNotifPreWorkoutAlerts = _tmp_8 != 0;
            final boolean _tmpNotifPostWorkoutCongrats;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfNotifPostWorkoutCongrats);
            _tmpNotifPostWorkoutCongrats = _tmp_9 != 0;
            final boolean _tmpNotifNutritionReminders;
            final int _tmp_10;
            _tmp_10 = _cursor.getInt(_cursorIndexOfNotifNutritionReminders);
            _tmpNotifNutritionReminders = _tmp_10 != 0;
            final boolean _tmpNotifHydrationReminders;
            final int _tmp_11;
            _tmp_11 = _cursor.getInt(_cursorIndexOfNotifHydrationReminders);
            _tmpNotifHydrationReminders = _tmp_11 != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new AppSettingsEntity(_tmpId,_tmpReduceMotion,_tmpParticlesEnabled,_tmpHapticsEnabled,_tmpHealthConnectEnabled,_tmpAutoLockVaultOnBackground,_tmpNotifWorkoutReminders,_tmpNotifStreakReminders,_tmpNotifMotivation,_tmpNotifPreWorkoutAlerts,_tmpNotifPostWorkoutCongrats,_tmpNotifNutritionReminders,_tmpNotifHydrationReminders,_tmpUpdatedAt);
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
  public Object getSettingsSync(final Continuation<? super AppSettingsEntity> $completion) {
    final String _sql = "SELECT * FROM app_settings WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AppSettingsEntity>() {
      @Override
      @Nullable
      public AppSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfReduceMotion = CursorUtil.getColumnIndexOrThrow(_cursor, "reduce_motion");
          final int _cursorIndexOfParticlesEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "particles_enabled");
          final int _cursorIndexOfHapticsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "haptics_enabled");
          final int _cursorIndexOfHealthConnectEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "health_connect_enabled");
          final int _cursorIndexOfAutoLockVaultOnBackground = CursorUtil.getColumnIndexOrThrow(_cursor, "auto_lock_vault_on_background");
          final int _cursorIndexOfNotifWorkoutReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_workout_reminders");
          final int _cursorIndexOfNotifStreakReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_streak_reminders");
          final int _cursorIndexOfNotifMotivation = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_motivation");
          final int _cursorIndexOfNotifPreWorkoutAlerts = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_pre_workout_alerts");
          final int _cursorIndexOfNotifPostWorkoutCongrats = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_post_workout_congrats");
          final int _cursorIndexOfNotifNutritionReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_nutrition_reminders");
          final int _cursorIndexOfNotifHydrationReminders = CursorUtil.getColumnIndexOrThrow(_cursor, "notif_hydration_reminders");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final AppSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final boolean _tmpReduceMotion;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfReduceMotion);
            _tmpReduceMotion = _tmp != 0;
            final boolean _tmpParticlesEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfParticlesEnabled);
            _tmpParticlesEnabled = _tmp_1 != 0;
            final boolean _tmpHapticsEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfHapticsEnabled);
            _tmpHapticsEnabled = _tmp_2 != 0;
            final boolean _tmpHealthConnectEnabled;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfHealthConnectEnabled);
            _tmpHealthConnectEnabled = _tmp_3 != 0;
            final boolean _tmpAutoLockVaultOnBackground;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAutoLockVaultOnBackground);
            _tmpAutoLockVaultOnBackground = _tmp_4 != 0;
            final boolean _tmpNotifWorkoutReminders;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfNotifWorkoutReminders);
            _tmpNotifWorkoutReminders = _tmp_5 != 0;
            final boolean _tmpNotifStreakReminders;
            final int _tmp_6;
            _tmp_6 = _cursor.getInt(_cursorIndexOfNotifStreakReminders);
            _tmpNotifStreakReminders = _tmp_6 != 0;
            final boolean _tmpNotifMotivation;
            final int _tmp_7;
            _tmp_7 = _cursor.getInt(_cursorIndexOfNotifMotivation);
            _tmpNotifMotivation = _tmp_7 != 0;
            final boolean _tmpNotifPreWorkoutAlerts;
            final int _tmp_8;
            _tmp_8 = _cursor.getInt(_cursorIndexOfNotifPreWorkoutAlerts);
            _tmpNotifPreWorkoutAlerts = _tmp_8 != 0;
            final boolean _tmpNotifPostWorkoutCongrats;
            final int _tmp_9;
            _tmp_9 = _cursor.getInt(_cursorIndexOfNotifPostWorkoutCongrats);
            _tmpNotifPostWorkoutCongrats = _tmp_9 != 0;
            final boolean _tmpNotifNutritionReminders;
            final int _tmp_10;
            _tmp_10 = _cursor.getInt(_cursorIndexOfNotifNutritionReminders);
            _tmpNotifNutritionReminders = _tmp_10 != 0;
            final boolean _tmpNotifHydrationReminders;
            final int _tmp_11;
            _tmp_11 = _cursor.getInt(_cursorIndexOfNotifHydrationReminders);
            _tmpNotifHydrationReminders = _tmp_11 != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new AppSettingsEntity(_tmpId,_tmpReduceMotion,_tmpParticlesEnabled,_tmpHapticsEnabled,_tmpHealthConnectEnabled,_tmpAutoLockVaultOnBackground,_tmpNotifWorkoutReminders,_tmpNotifStreakReminders,_tmpNotifMotivation,_tmpNotifPreWorkoutAlerts,_tmpNotifPostWorkoutCongrats,_tmpNotifNutritionReminders,_tmpNotifHydrationReminders,_tmpUpdatedAt);
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
