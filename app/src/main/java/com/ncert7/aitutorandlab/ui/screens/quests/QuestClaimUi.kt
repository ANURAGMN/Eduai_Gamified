package com.ncert7.aitutorandlab.ui.screens.quests

import com.ncert7.aitutorandlab.domain.gamification.QuestClaimResult
import com.ncert7.aitutorandlab.domain.gamification.QuestClaimType
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.isKannadaLanguage

fun questClaimDialogCopy(
    type: QuestClaimType,
    languageCode: String = getCurrentLanguageCode(),
): Pair<String, String> {
    val kn = isKannadaLanguage(languageCode)
    return when (type) {
        QuestClaimType.SIMS ->
            if (kn) {
                "ಸಿಮ್ ಕ್ವೆಸ್ಟ್ ಕ್ಲೈಮ್" to
                    "ಇಂದು ೩ ಸಿಮ್ಯುಲೇಶನ್ ಮುಗಿಸಿದ್ದೀರಿ. ರತ್ನಗಳಿಗಾಗಿ ಚಿಕ್ಕ ವೀಡಿಯೋ ನೋಡಿ."
            } else {
                "Claim sims quest" to
                    "You finished 3 simulations today. Watch a short video to collect your gems."
            }
        QuestClaimType.STUDY ->
            if (kn) {
                "ಅಧ್ಯಯನ ಕ್ವೆಸ್ಟ್ ಕ್ಲೈಮ್" to
                    "ಇಂದಿನ ಯೋಜನೆ ಕಾರ್ಯ ಮುಗಿಸಿದ್ದೀರಿ. ರತ್ನಗಳಿಗಾಗಿ ಚಿಕ್ಕ ವೀಡಿಯೋ ನೋಡಿ."
            } else {
                "Claim study quest" to
                    "You finished today's plan task. Watch a short video to collect your gems."
            }
        QuestClaimType.BONUS ->
            if (kn) {
                "ಬೋನಸ್ ಕ್ವೆಸ್ಟ್ ಕ್ಲೈಮ್" to
                    "ಎರಡೂ ದೈನಂದಿನ ಕ್ವೆಸ್ಟ್‌ಗಳು ಮುಗಿದಿವೆ. ಬೋನಸ್ ರತ್ನಗಳಿಗಾಗಿ ಚಿಕ್ಕ ವೀಡಿಯೋ ನೋಡಿ."
            } else {
                "Claim bonus quest" to
                    "Both daily quests are done. Watch a short video for the bonus gems."
            }
    }
}

fun questClaimWatchLabel(languageCode: String, gemsReward: Int): String =
    if (isKannadaLanguage(languageCode)) {
        "ಚಿಕ್ಕ ವೀಡಿಯೋ ನೋಡಿ · +$gemsReward ರತ್ನ"
    } else {
        "Watch short video · +$gemsReward gems"
    }

fun questClaimNotNowLabel(languageCode: String): String =
    if (isKannadaLanguage(languageCode)) "ಈಗ ಬೇಡ" else "Not now"

fun questClaimAdLoadingLabel(languageCode: String): String =
    if (isKannadaLanguage(languageCode)) {
        "ಜಾಹೀರಾತು ಲೋಡ್ ಆಗುತ್ತಿದೆ… ಸ್ವಲ್ಪ ನಂತರ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ."
    } else {
        "Loading ad… try again in a moment."
    }

fun questClaimUnableToShowAd(languageCode: String): String =
    if (isKannadaLanguage(languageCode)) {
        "ಜಾಹೀರಾತು ತೋರಿಸಲಾಗಲಿಲ್ಲ."
    } else {
        "Unable to show ad."
    }

fun questClaimResultMessage(
    result: QuestClaimResult,
    languageCode: String = getCurrentLanguageCode(),
): String? {
    val kn = isKannadaLanguage(languageCode)
    return when (result) {
        QuestClaimResult.SUCCESS -> null
        QuestClaimResult.NOT_ELIGIBLE ->
            if (kn) "ಈ ಕ್ವೆಸ್ಟ್ ಈಗ ಕ್ಲೈಮ್ ಮಾಡಲಾಗುವುದಿಲ್ಲ." else "This quest can't be claimed right now."
        QuestClaimResult.NOT_READY ->
            if (kn) {
                "ಜಾಹೀರಾತು ಇನ್ನೂ ಲೋಡ್ ಆಗುತ್ತಿದೆ. ಸ್ವಲ್ಪ ನಂತರ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ."
            } else {
                "Ad is still loading. Try again in a moment."
            }
        QuestClaimResult.AD_SKIPPED ->
            if (kn) {
                "ರತ್ನಗಳಿಗಾಗಿ ಪೂರ್ಣ ವೀಡಿಯೋ ನೋಡಿ."
            } else {
                "Watch the full video to earn gems."
            }
        QuestClaimResult.DAILY_CAP_REACHED ->
            if (kn) {
                "ಇಂದಿನ ಕ್ವೆಸ್ಟ್ ಜಾಹೀರಾತು ಮಿತಿ ತಲುಪಿದೆ."
            } else {
                "You've reached today's quest ad limit."
            }
        QuestClaimResult.GRANT_FAILED ->
            if (kn) {
                "ರತ್ನ ನೀಡಲಾಗಲಿಲ್ಲ. ನಂತರ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ."
            } else {
                "Could not grant gems. Try again later."
            }
    }
}
