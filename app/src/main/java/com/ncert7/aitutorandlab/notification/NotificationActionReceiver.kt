package com.ncert7.aitutorandlab.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/** Dismisses a single notification when the user taps Cancel. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_CANCEL) return
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIF_ID, -1)
        if (notifId != -1) {
            NotificationManagerCompat.from(context).cancel(notifId)
        }
    }
}
