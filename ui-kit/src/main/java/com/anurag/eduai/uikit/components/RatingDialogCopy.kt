package com.anurag.eduai.uikit.components

/**
 * Localized chrome for [EduRatingDialog]. Defaults stay English.
 * Note: the app currently uses Play In-App Review (no custom dialog host), but this
 * component stays localizable for previews / future reuse.
 */
data class RatingDialogCopy(
    val titleEnjoying: (String) -> String = { appName -> "Enjoying $appName?" },
    val titleHelpUs: String = "Help us do better",
    val bodyRate: String = "How would you rate your experience so far?",
    val bodyFeedback: String =
        "Sorry it isn't a 5-star experience yet. Tell us what would make it better — this goes straight to the team.",
    val feedbackPlaceholder: String = "What can we improve?",
    val rateLabel: String = "Rate",
    val rateOnPlayLabel: String = "Rate on Play Store",
    val sendFeedbackLabel: String = "Send feedback",
    val notNowLabel: String = "Not now",
    val starContentDescription: (Int) -> String = { i -> "$i star" },
)

fun defaultRatingDialogCopy(): RatingDialogCopy = RatingDialogCopy()
