package com.ncert7.aitutorandlab.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val PERIODIC_WORK_NAME = "notification_eval_sweep"
    private const val DAILY_ALARM_REQUEST_CODE = 9001
    private val zone = ZoneId.of("Asia/Kolkata")

    fun scheduleAll(context: Context) {
        if (!GamificationFeatureFlags.isGamifiedHomeEnabled(context)) {
            cancelAll(context)
            return
        }

        val prefs = SharedPreferenceUtils(context)
        if (!prefs.areNotificationsEnabled()) {
            cancelAll(context)
            return
        }
        if (NotificationReminderMode.fromStored(prefs.getReminderMode()) == NotificationReminderMode.OFF) {
            cancelAll(context)
            return
        }

        schedulePeriodicSweep(context)
        scheduleDailyReminderAlarm(context)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        cancelDailyReminderAlarm(context)
    }

    fun schedulePeriodicSweep(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<NotificationEvalWorker>(30, TimeUnit.MINUTES)
                .setInputData(
                    workDataOf(
                        NotificationEvalWorker.KEY_TRIGGER to
                            NotificationEvalTrigger.PERIODIC_SWEEP.name,
                    ),
                )
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        DebugLogger.debugLog(TAG, "Periodic notification sweep scheduled (30 min)")
    }

    fun scheduleDailyReminderAlarm(context: Context) {
        val prefs = SharedPreferenceUtils(context)
        if (NotificationReminderMode.fromStored(prefs.getReminderMode()) == NotificationReminderMode.OFF) {
            cancelDailyReminderAlarm(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMs = nextReminderTriggerMillis(prefs.getReminderHour(), prefs.getReminderMinute())
        val pendingIntent = dailyReminderPendingIntent(context)
        // Inexact alarm only — no SCHEDULE_EXACT_ALARM (Play policy for non-alarm-clock apps).
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMs,
            pendingIntent,
        )
        DebugLogger.debugLog(TAG, "Inexact daily reminder alarm set for $triggerAtMs")
    }

    fun cancelDailyReminderAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(dailyReminderPendingIntent(context))
    }

    private fun dailyReminderPendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_DAILY_REMINDER
            }
        return PendingIntent.getBroadcast(
            context,
            DAILY_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextReminderTriggerMillis(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now(zone)
        var target =
            LocalDate.now(zone).atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
                .atZone(zone)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }

    private const val TAG = "NotificationScheduler"
}
