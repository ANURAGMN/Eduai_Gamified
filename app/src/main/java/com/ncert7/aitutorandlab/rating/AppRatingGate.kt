package com.ncert7.aitutorandlab.rating

import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils

/**
 * Decides *whether* to request an in-app review at a positive moment. It does not show any UI — the
 * caller pairs a `true` result with [AppReviewManager.requestInAppReview] so Google's own card (or
 * nothing) is what the user sees. Fully compliant: no sentiment question, no store deep-link.
 *
 * Throttle: at most once per calendar day and up to [MAX_REQUESTS] times total. (Google additionally
 * quota-limits how often the card actually appears.)
 */
object AppRatingGate {
    private const val MAX_REQUESTS = 3

    fun shouldRequestReview(
        context: android.content.Context,
        prefs: SharedPreferenceUtils,
    ): Boolean = decideReviewRequest(context, prefs) is ReviewRequestDecision.Proceed

    fun decideReviewRequest(
        context: android.content.Context,
        prefs: SharedPreferenceUtils,
    ): ReviewRequestDecision {
        if (!GamificationFeatureFlags.isGamifiedHomeEnabled(context)) {
            return ReviewRequestDecision.NotEligible
        }
        if (prefs.getRatingPromptShowCount() >= MAX_REQUESTS) {
            return ReviewRequestDecision.Throttled("max_requests")
        }
        if (prefs.wasRatingPromptShownToday()) {
            return ReviewRequestDecision.Throttled("already_shown_today")
        }

        prefs.setRatingPromptShownToday()
        prefs.incrementRatingPromptShowCount()
        return ReviewRequestDecision.Proceed
    }
}
