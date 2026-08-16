package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.screens.LeaguesCopy
import com.anurag.eduai.uikit.screens.defaultLeaguesCopy
import com.ncert7.aitutorandlab.domain.gamification.LeagueTier

/** App-module factory for leagues chrome + tier titles (EN / KN). */
object LeaguesCopyFactory {
    fun forLanguage(languageCode: String): LeaguesCopy =
        if (isKannadaLanguage(languageCode)) kannada() else defaultLeaguesCopy()

    fun screenTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಲೀಗ್‌ಗಳು" else "Leagues"

    fun backContentDescription(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಹಿಂದೆ" else "Back"

    fun tierDisplayName(tier: LeagueTier, languageCode: String): String =
        HomeCopy.leagueCaption(tier.storageKey, languageCode)

    fun leagueTitle(tier: LeagueTier, languageCode: String): String {
        val name = tierDisplayName(tier, languageCode)
        return if (isKannadaLanguage(languageCode)) "$name ಲೀಗ್" else "$name League"
    }

    private fun kannada(): LeaguesCopy =
        LeaguesCopy(
            sectionTitle = "ಲೀಗ್‌ಗಳು",
            daysLeft = { days ->
                if (days == 1) "೧ ದಿನ ಉಳಿದಿದೆ" else "$days ದಿನ ಉಳಿದಿವೆ"
            },
            standingLine = { rank, total, promoCount, target ->
                "ನಿಮ್ಮ ಶ್ರೇಣಿ $rank / $total — ಮೇಲಿನ $promoCount ಜನರು $target ಗೆ ಬಡ್ತಿ"
            },
            promotionZone = { target -> "ಬಡ್ತಿ ವಲಯ · $target ಗೆ ಮುಂದುವರಿಯುತ್ತದೆ" },
            safeZone = "ಸುರಕ್ಷಿತ ವಲಯ",
            demotionZone = "ಕೆಳಗಿಳಿಯುವ ವಲಯ",
        )
}
