package com.forge.domain.engine

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.forge.MainActivity
import com.forge.data.local.entity.AppSettingsEntity
import com.forge.data.local.entity.TrainingScheduleEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class ForgeNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "FORGE Training"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Time to hit the iron."
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: CHANNEL_WORKOUTS

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (channelId == CHANNEL_WORKOUTS) "Workout Reminders" else "Milestone Check-ins",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "FORGE Scheduled Reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val CHANNEL_WORKOUTS = "forge_channel_workouts"
        const val CHANNEL_MILESTONES = "forge_channel_milestones"
    }
}

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "NotificationScheduler"
        private const val BASE_REQUEST_CODE_WORKOUT = 2000
        private const val REQUEST_CODE_MILESTONE = 3000
    }

    /**
     * Reschedules or cancels alarms to strictly reflect schedule modifications.
     * Survives moving days, deleting workouts, changing times, disabling categories, and timezone shifts.
     */
    fun syncScheduleAlarms(
        schedules: List<TrainingScheduleEntity>,
        appSettings: AppSettingsEntity,
        preferredTime: LocalTime = LocalTime.of(17, 0) // Default 5:00 PM if unspecified
    ) {
        if (!appSettings.notifWorkoutReminders) {
            // Category disabled: cancel all workout alarms
            for (day in 1..7) {
                cancelWorkoutAlarm(day)
            }
            Log.d(TAG, "Workout reminders disabled. Cancelled all daily alarms.")
            return
        }

        // For each day of the week (1 = Monday, 7 = Sunday)
        for (dayOfWeek in 1..7) {
            val scheduleForDay = schedules.find { it.dayOfWeek == dayOfWeek }
            if (scheduleForDay != null && scheduleForDay.isTrainingDay) {
                // Cancel existing to prevent stale/duplicate alarms, then schedule new
                cancelWorkoutAlarm(dayOfWeek)
                scheduleWorkoutAlarm(
                    dayOfWeek = dayOfWeek,
                    focus = scheduleForDay.focus,
                    time = preferredTime
                )
            } else {
                // Rest day or deleted session: ensure alarm is cancelled
                cancelWorkoutAlarm(dayOfWeek)
            }
        }
    }

    /**
     * Specifically handles moving a workout from oldDay to newDay.
     */
    fun onWorkoutMoved(
        oldDayOfWeek: Int,
        newDayOfWeek: Int,
        focus: String,
        preferredTime: LocalTime = LocalTime.of(17, 0)
    ) {
        cancelWorkoutAlarm(oldDayOfWeek)
        scheduleWorkoutAlarm(newDayOfWeek, focus, preferredTime)
        Log.d(TAG, "Moved workout alarm from day $oldDayOfWeek to day $newDayOfWeek ($focus)")
    }

    /**
     * Cancel an alarm for a specific day of week (1..7).
     */
    fun cancelWorkoutAlarm(dayOfWeek: Int) {
        val intent = Intent(context, ForgeNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BASE_REQUEST_CODE_WORKOUT + dayOfWeek,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled workout alarm for day $dayOfWeek")
        }
    }

    private fun scheduleWorkoutAlarm(dayOfWeek: Int, focus: String, time: LocalTime) {
        val targetDay = DayOfWeek.of(dayOfWeek)
        val now = LocalDateTime.now()
        var targetDateTime = now.with(TemporalAdjusters.nextOrSame(targetDay)).with(time)

        // If today is the target day but the preferred time has already passed today, advance to next week
        if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
            targetDateTime = now.with(TemporalAdjusters.next(targetDay)).with(time)
        }

        val triggerEpochMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, ForgeNotificationReceiver::class.java).apply {
            putExtra(ForgeNotificationReceiver.EXTRA_TITLE, "Upcoming Session: $focus")
            putExtra(ForgeNotificationReceiver.EXTRA_MESSAGE, "Your $focus session is scheduled for today. Ready to forge?")
            putExtra(ForgeNotificationReceiver.EXTRA_CHANNEL_ID, ForgeNotificationReceiver.CHANNEL_WORKOUTS)
            putExtra(ForgeNotificationReceiver.EXTRA_NOTIFICATION_ID, BASE_REQUEST_CODE_WORKOUT + dayOfWeek)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BASE_REQUEST_CODE_WORKOUT + dayOfWeek,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm for day $dayOfWeek ($focus) at $targetDateTime")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted; falling back to inexact alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerEpochMillis, pendingIntent)
        }
    }

    /**
     * Reschedules milestone check-in reminder based on actual transformation timeline.
     */
    fun scheduleMilestoneCheckIn(nextCheckInEpochMillis: Long, weekNumber: Int, enabled: Boolean) {
        val intent = Intent(context, ForgeNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MILESTONE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        if (!enabled) return

        val newIntent = Intent(context, ForgeNotificationReceiver::class.java).apply {
            putExtra(ForgeNotificationReceiver.EXTRA_TITLE, "Week $weekNumber Milestone Check-in")
            putExtra(ForgeNotificationReceiver.EXTRA_MESSAGE, "Time for your Week $weekNumber physique photo & progress log.")
            putExtra(ForgeNotificationReceiver.EXTRA_CHANNEL_ID, ForgeNotificationReceiver.CHANNEL_MILESTONES)
            putExtra(ForgeNotificationReceiver.EXTRA_NOTIFICATION_ID, REQUEST_CODE_MILESTONE)
        }

        val newPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MILESTONE,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextCheckInEpochMillis, newPendingIntent)
            Log.d(TAG, "Scheduled milestone check-in for week $weekNumber at $nextCheckInEpochMillis")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule milestone check-in", e)
        }
    }
}
