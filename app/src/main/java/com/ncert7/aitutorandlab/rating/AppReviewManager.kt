package com.ncert7.aitutorandlab.rating

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.ncert7.aitutorandlab.debug.DebugLogger

/**
 * Thin wrapper over Google Play's In-App Review API. This is the fully policy-compliant path: Google
 * shows its own rating card in-app (and decides whether/how often to show it), so we never ask the
 * user how they feel, never gate on sentiment, and never deep-link them to the store based on a rating.
 */
object AppReviewManager {

    /**
     * Requests and launches the in-app review flow. Best-effort: if Google decides not to show the
     * card (quota, already reviewed, etc.) or Play services are unavailable, this simply does nothing.
     */
    suspend fun requestInAppReview(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReview()
            manager.launchReview(activity, reviewInfo)
        } catch (e: Exception) {
            DebugLogger.errorLog("AppReviewManager", "In-app review skipped: ${e.message}")
        }
    }
}
