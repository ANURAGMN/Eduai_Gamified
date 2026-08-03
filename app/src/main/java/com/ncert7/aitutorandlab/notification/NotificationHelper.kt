package com.ncert7.aitutorandlab.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.ncert7.aitutorandlab.MainActivity
import com.ncert7.aitutorandlab.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun fire(content: ResolvedNotificationContent, largeIcon: Bitmap? = null) {
        NotificationChannels.ensureCreated(context)
        val notifId = stableNotificationId(content.type)
        val paramsString =
            content.deepLinkParams.entries.joinToString(",") { "${it.key}=${it.value}" }

        val openIntent =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ROUTE, content.deepLinkRoute)
                putExtra(EXTRA_PARAMS, paramsString)
                putExtra(EXTRA_NOTIF_ID, notifId)
            }
        val primaryPendingIntent =
            PendingIntent.getActivity(
                context,
                notifId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val cancelIntent =
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_NOTIF_ID, notifId)
            }
        val cancelPendingIntent =
            PendingIntent.getBroadcast(
                context,
                notifId + CANCEL_REQUEST_CODE_OFFSET,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val accentColor = ContextCompat.getColor(context, content.category.accentColorRes)
        val iconBitmap =
            largeIcon
                ?: NotificationAvatarCache.load(context)
                ?: buildFallbackLargeIcon(content.category)

        val builder =
            NotificationCompat.Builder(context, content.category.channelId)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setLargeIcon(iconBitmap)
                .setColor(accentColor)
                .setContentTitle(content.title)
                .setContentText(content.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
                .setPriority(
                    if (content.highPriority) {
                        NotificationCompat.PRIORITY_HIGH
                    } else {
                        NotificationCompat.PRIORITY_DEFAULT
                    },
                )
                .setAutoCancel(true)
                .setContentIntent(primaryPendingIntent)
                .addAction(0, content.primaryLabel, primaryPendingIntent)
                .addAction(0, context.getString(R.string.notification_action_cancel), cancelPendingIntent)

        notify(notifId, builder)
    }

    @SuppressLint("MissingPermission")
    private fun notify(notifId: Int, builder: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    private fun buildFallbackLargeIcon(category: NotificationCategory): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
        val cx = size / 2f
        val cy = size / 2f
        val radius = size / 2f - 8f
        canvas.drawCircle(cx, cy, radius, fillPaint)

        val tint = ContextCompat.getColor(context, category.accentColorRes)
        val drawable =
            ContextCompat.getDrawable(context, category.fallbackLargeIconRes)?.mutate()
                ?: return bitmap
        DrawableCompat.setTint(drawable, tint)
        val inset = (size * 0.22f).toInt()
        drawable.setBounds(inset, inset, size - inset, size - inset)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        const val EXTRA_ROUTE = "eduai_notification_route"
        const val EXTRA_PARAMS = "eduai_notification_params"
        const val EXTRA_NOTIF_ID = "eduai_notification_id"
        const val ACTION_CANCEL = "com.ncert7.aitutorandlab.notification.ACTION_CANCEL"
        private const val CANCEL_REQUEST_CODE_OFFSET = 10_000

        fun stableNotificationId(type: NotificationType): Int = 3_000 + type.ordinal
    }
}
