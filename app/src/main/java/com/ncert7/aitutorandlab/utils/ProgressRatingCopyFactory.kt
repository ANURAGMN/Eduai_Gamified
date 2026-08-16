package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.components.HomeProgressRailCopy
import com.anurag.eduai.uikit.components.RatingDialogCopy
import com.anurag.eduai.uikit.components.defaultHomeProgressRailCopy
import com.anurag.eduai.uikit.components.defaultRatingDialogCopy

/** Factories for §5 P2 ui-kit surfaces (EN / KN). */
object ProgressRatingCopyFactory {
    fun homeProgressRail(languageCode: String): HomeProgressRailCopy =
        if (isKannadaLanguage(languageCode)) homeProgressKannada() else defaultHomeProgressRailCopy()

    fun ratingDialog(languageCode: String): RatingDialogCopy =
        if (isKannadaLanguage(languageCode)) ratingKannada() else defaultRatingDialogCopy()

    private fun homeProgressKannada(): HomeProgressRailCopy =
        HomeProgressRailCopy(
            sectionTitle = "ನಿಮ್ಮ ವಾರ",
            seeAllLabel = HomeCopy.seeAllLabel("kn"),
            streakTitle = { streak ->
                if (streak == 1) "೧-ದಿನದ ಸರಣಿ" else "$streak-ದಿನದ ಸರಣಿ"
            },
            streakSubtitle = "ಇಂದು ಸರಣಿ ಉಳಿಸಿ",
            leagueTitle = { leagueName, rank ->
                if (rank > 0) "$leagueName · ಶ್ರೇಣಿ $rank" else leagueName
            },
            leagueSubtitle = { promoteCount ->
                "ಮೇಲಿನ $promoteCount ಬಡ್ತಿ · ತೆರೆಯಲು ಟ್ಯಾಪ್ ಮಾಡಿ"
            },
            inviteTitle = HomeCopy.inviteFriendsTitle("kn"),
            inviteSubtitle = "ಒಟ್ಟಿಗೆ ಕಲಿಯಿರಿ",
            shareLabel = HomeCopy.shareLabel("kn"),
        )

    private fun ratingKannada(): RatingDialogCopy =
        RatingDialogCopy(
            titleEnjoying = { appName -> "$appName ಇಷ್ಟವಾಗುತ್ತಿದೆಯೇ?" },
            titleHelpUs = "ನಾವು ಉತ್ತಮವಾಗಲು ಸಹಾಯ ಮಾಡಿ",
            bodyRate = "ಇಲ್ಲಿಯವರೆಗಿನ ಅನುಭವವನ್ನು ನೀವು ಹೇಗೆ ರೇಟ್ ಮಾಡುತ್ತೀರಿ?",
            bodyFeedback =
                "ಇನ್ನೂ ೫-ನಕ್ಷತ್ರ ಅನುಭವವಾಗಿಲ್ಲ. ಏನು ಉತ್ತಮವಾಗಬೇಕು ಎಂದು ಹೇಳಿ — ಇದು ನೇರವಾಗಿ ತಂಡಕ್ಕೆ ಹೋಗುತ್ತದೆ.",
            feedbackPlaceholder = "ನಾವು ಏನನ್ನು ಸುಧಾರಿಸಬಹುದು?",
            rateLabel = "ರೇಟ್",
            rateOnPlayLabel = "Play Store ನಲ್ಲಿ ರೇಟ್ ಮಾಡಿ",
            sendFeedbackLabel = "ಪ್ರತಿಕ್ರಿಯೆ ಕಳುಹಿಸಿ",
            notNowLabel = "ಈಗ ಬೇಡ",
            starContentDescription = { i -> "$i ನಕ್ಷತ್ರ" },
        )
}
