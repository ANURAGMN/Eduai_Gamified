package com.ncert7.aitutorandlab.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags

/**
 * Creates Android notification channels once at app start (API 26+).
 */
object NotificationChannels {
    fun ensureCreated(context: Context) {
        if (!GamificationFeatureFlags.isGamifiedHomeEnabled(context)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        NotificationCategory.entries.forEach { category ->
            val importance =
                if (category.highImportanceDefault) {
                    NotificationManager.IMPORTANCE_HIGH
                } else {
                    NotificationManager.IMPORTANCE_DEFAULT
                }
            val channel =
                NotificationChannel(category.channelId, category.channelLabel, importance).apply {
                    description = category.channelLabel
                }
            manager.createNotificationChannel(channel)
        }
    }
}
