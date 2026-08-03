package com.ncert7.aitutorandlab.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.anurag.eduai.uikit.avatar.TutorConfig
import com.anurag.eduai.uikit.avatar.TutorConfigStore
import com.anurag.eduai.uikit.avatar.captureTutorNotificationIcon
import com.ncert7.aitutorandlab.debug.DebugLogger
import java.io.File
import java.io.FileOutputStream

/**
 * Disk cache for the tutor avatar used as the notification large icon (works from background workers).
 */
object NotificationAvatarCache {
    private const val FILE_NAME = "notification_tutor_large_icon.png"
    private const val TAG = "NotificationAvatarCache"

    fun load(context: Context): Bitmap? {
        val file = cacheFile(context)
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }

    fun refreshFromSavedConfig(context: Context) {
        TutorConfigStore.load(context)
        refresh(context, TutorConfigStore.state.value)
    }

    fun refresh(context: Context, config: TutorConfig) {
        captureTutorNotificationIcon(
            context = context,
            config = config,
            onReady = { bitmap ->
                save(context, bitmap)
                bitmap.recycle()
                DebugLogger.debugLog(TAG, "Notification avatar cache updated")
            },
            onError = {
                DebugLogger.debugLog(TAG, "Avatar capture skipped — no activity context")
            },
        )
    }

    fun clear(context: Context) {
        cacheFile(context).delete()
    }

    fun save(context: Context, bitmap: Bitmap) {
        runCatching {
            FileOutputStream(cacheFile(context)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }.onFailure { e ->
            DebugLogger.errorLog(TAG, "Failed to cache avatar icon: ${e.message}")
        }
    }

    private fun cacheFile(context: Context): File = File(context.cacheDir, FILE_NAME)
}
