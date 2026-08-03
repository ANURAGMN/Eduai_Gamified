package com.ncert7.aitutorandlab.rating

/** Result of [AppRatingGate.decideReviewRequest] — caller logs analytics then acts. */
sealed class ReviewRequestDecision {
    data object Proceed : ReviewRequestDecision()

    data class Throttled(val reason: String) : ReviewRequestDecision()

    data object NotEligible : ReviewRequestDecision()
}
