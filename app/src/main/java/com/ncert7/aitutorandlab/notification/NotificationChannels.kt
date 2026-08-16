package com.ncert7.aitutorandlab.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.utils.RewardMomentCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode

/**
 * Creates Android notification channels once at app start (API 26+).
 * Channel **ids** stay stable; display names follow the current app language.
 */
object NotificationChannels {
    fun ensureCreated(context: Context) {
        if (!GamificationFeatureFlags.isGamifiedHomeEnabled(context)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val language = getCurrentLanguageCode()
        NotificationCategory.entries.forEach { category ->
            val importance =
                if (category.highImportanceDefault) {
                    NotificationManager.IMPORTANCE_HIGH
                } else {
                    NotificationManager.IMPORTANCE_DEFAULT
                }
            val label = RewardMomentCopy.categoryLabel(category, language)
            val channel =
                NotificationChannel(category.channelId, label, importance).apply {
                    description = label
                }
            manager.createNotificationChannel(channel)
        }
    }
}
