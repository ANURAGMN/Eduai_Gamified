package com.ncert7.aitutorandlab.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.debug.DebugLogger

class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                if (!GamificationFeatureFlags.isGamifiedHomeEnabled(context)) return
                DebugLogger.debugLog(TAG, "Rescheduling notifications after ${intent.action}")
                NotificationScheduler.scheduleAll(context.applicationContext)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationBootReceiver"
    }
}
