package com.ncert7.aitutorandlab.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ncert7.aitutorandlab.debug.DebugLogger

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DAILY_REMINDER) return

        val appContext = context.applicationContext
        DebugLogger.debugLog(TAG, "Daily alarm received — enqueueing eval worker")

        val work =
            OneTimeWorkRequestBuilder<NotificationEvalWorker>()
                .setInputData(
                    workDataOf(
                        NotificationEvalWorker.KEY_TRIGGER to
                            NotificationEvalTrigger.DAILY_ALARM.name,
                    ),
                )
                .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            work,
        )
        NotificationScheduler.scheduleDailyReminderAlarm(appContext)
    }

    companion object {
        const val TAG = "DailyReminderReceiver"
        const val ACTION_DAILY_REMINDER = "com.ncert7.aitutorandlab.notification.DAILY_REMINDER"
        private const val WORK_NAME = "notification_daily_alarm"
    }
}
