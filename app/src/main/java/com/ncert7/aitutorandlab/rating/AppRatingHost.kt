package com.ncert7.aitutorandlab.rating

/*
 * The custom sentiment-gated rating dialog was removed in favour of the fully policy-compliant Google
 * Play In-App Review API. Reviews are now requested via [AppReviewManager] at a positive moment
 * (first return to home after completing a task), gated by [AppRatingGate]. There is no custom rating
 * UI, no "how do you feel" prompt, and no store deep-link based on sentiment.
 *
 * Unhappy users still have an always-available feedback channel via the Contact Support entry in
 * Settings — it is never used to gate or pre-filter the review flow.
 */
