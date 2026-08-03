package com.ncert7.aitutorandlab.notification

import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Emits a primer request after a meaningful win (streak / quest / trial reward).
 *
 * Cadence: at most once per calendar day, up to [MAX_PRIMER_SHOWS] times total, each with a different
 * persuasion angle in order — study more, don't lose your streak, don't miss friends' updates. So a
 * student who keeps dismissing sees all three framings across three days, then it stops. Once the OS
 * dialog has actually been shown (accepted primer) or the permission is granted, it stops entirely.
 *
 * The [variant] passed in by callers is ignored — the angle is chosen by attempt number so the three
 * framings are guaranteed to rotate regardless of where the win came from.
 */
object NotificationPermissionGate {
    private const val MAX_PRIMER_SHOWS = 3

    /** One persuasion per attempt, in order. */
    private val persuasions =
        listOf(
            NotificationPrimerVariant.STUDY,
            NotificationPrimerVariant.STREAK,
            NotificationPrimerVariant.FRIENDS,
        )

    private val _primerRequests = MutableSharedFlow<NotificationPrimerVariant>(extraBufferCapacity = 1)
    val primerRequests: SharedFlow<NotificationPrimerVariant> = _primerRequests.asSharedFlow()

    fun onMeaningfulWin(
        context: android.content.Context,
        prefs: SharedPreferenceUtils,
        variant: NotificationPrimerVariant,
    ) {
        if (!GamificationFeatureFlags.isGamifiedHomeEnabled(context)) return
        if (!prefs.areNotificationsEnabled()) return
        if (NotificationPermissionHelper.hasPostNotificationsPermission(context)) return
        if (prefs.hasAskedNotificationPermission()) return

        val shown = prefs.getNotificationPrimerShowCount()
        if (shown >= MAX_PRIMER_SHOWS) return
        if (prefs.wasNotificationPrimerShownToday()) return

        val variant = persuasions[shown.coerceIn(0, persuasions.lastIndex)]
        prefs.setNotificationPrimerShownToday()
        prefs.incrementNotificationPrimerShowCount()
        EngagementAnalyticsTracker.notificationPrimerShown(variant.name, shown + 1)
        _primerRequests.tryEmit(variant)
    }
}
