package com.ncert7.aitutorandlab.service.analytics

/**
 * High-frequency analytics (clicks, screens, ads, gamification) go to GA4 only.
 * Firestore is reserved for app state (progress, gems balance), not event logging.
 */
object AnalyticsFirestoreMirror {
    const val ENABLED: Boolean = false
}
