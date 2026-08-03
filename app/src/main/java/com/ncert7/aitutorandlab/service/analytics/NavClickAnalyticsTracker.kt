package com.ncert7.aitutorandlab.service.analytics

import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tracks bottom-nav tab and other chrome navigation taps.
 */
object NavClickAnalyticsTracker {

    private const val TAG = "NavAnalytics"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun trackNavTab(tabRoute: String) {
        if (tabRoute.isBlank()) return
        scope.launch {
            AnalyticsEventRecorder.recordClick(
                screenName = "NAV",
                itemId = tabRoute,
                source = ClickSource.NAV.value,
                interactionType = ContentClickType.NAV_TAB.value,
            )
            FirebaseAnalyticsHelper.logNavTab(tabRoute)
            DebugLogger.debugLog(TAG, "Nav tab: $tabRoute")
        }
    }
}
