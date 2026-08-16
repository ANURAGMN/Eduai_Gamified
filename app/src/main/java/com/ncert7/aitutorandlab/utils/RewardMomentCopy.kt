package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.components.RewardOverlayCopy
import com.anurag.eduai.uikit.components.defaultRewardOverlayCopy
import com.ncert7.aitutorandlab.notification.NotificationCategory

/** §5 P3 reward/moment chrome + notification category labels (EN / KN). */
object RewardMomentCopy {
    fun xpChipLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "XP" else "XP"

    fun gemsChipLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ರತ್ನಗಳು" else "gems"

    fun okLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ಸರಿ" else "OK"

    fun rewardOverlay(languageCode: String): RewardOverlayCopy =
        if (isKannadaLanguage(languageCode)) {
            RewardOverlayCopy(
                title = "ದಿನ ಪೂರ್ಣ!",
                subtitle = "ಇಂದಿನ ಗಮನ ಮುಗಿಸಿದ್ದೀರಿ",
                weeklyXpLabel = "ವಾರದ XP",
                xpEarnedLabel = "ಗಳಿಸಿದ XP",
                gemsLabel = "ರತ್ನಗಳು",
                collectCta = "ಬಹುಮಾನ ಪಡೆಯಿರಿ",
            )
        } else {
            defaultRewardOverlayCopy()
        }

    fun categoryLabel(category: NotificationCategory, languageCode: String): String {
        if (!isKannadaLanguage(languageCode)) return category.channelLabel
        return when (category) {
            NotificationCategory.STREAKS -> "ಸರಣಿಗಳು"
            NotificationCategory.QUESTS -> "ಕ್ವೆಸ್ಟ್‌ಗಳು"
            NotificationCategory.REMINDERS -> "ಜ್ಞಾಪನೆಗಳು"
            NotificationCategory.AVATAR -> "ಅವತಾರ್"
            NotificationCategory.LEAGUES_SOCIAL -> "ಲೀಗ್‌ಗಳು ಮತ್ತು ಸಾಮಾಜಿಕ"
        }
    }
}
