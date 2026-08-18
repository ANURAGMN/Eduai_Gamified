package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.screens.AvatarStudioCopy
import com.anurag.eduai.uikit.screens.defaultAvatarStudioCopy

object AvatarStudioCopyFactory {
    fun forLanguage(languageCode: String): AvatarStudioCopy =
        if (isKannadaLanguage(languageCode)) kannada() else defaultAvatarStudioCopy()

    private fun kannada() =
        AvatarStudioCopy(
            screenTitle = "ಅವತಾರ ಸ್ಟುಡಿಯೋ",
            sectionTitle = "ಅವತಾರ ಸ್ಟುಡಿಯೋ",
            mood = "ಮೂಡ್",
            customize = "ಕಸ್ಟಮೈಸ್",
            expression = "ಭಾವನೆ",
            saved = "ಉಳಿಸಲಾಗಿದೆ ✓",
            saveWithAds = "ಉಳಿಸಿ · 1 ಜಾಹೀರಾತು",
            shareWithFriends = "ಸ್ನೇಹಿತರೊಂದಿಗೆ ಹಂಚಿಕೊಳ್ಳಿ",
            saveHint = "ಉಳಿಸಲು 1 ಜಾಹೀರಾತು ನೋಡಿ. ನಿಮ್ಮ ಶಿಕ್ಷಕ ಮುಖಪುಟ ಮತ್ತು ಆಚರಣೆಗಳಲ್ಲಿ ಕಾಣಿಸುತ್ತಾರೆ.",
            saveAdFailed = "ಜಾಹೀರಾತು ಪೂರ್ಣಗೊಳ್ಳಲಿಲ್ಲ. ನಿಮ್ಮ ಶಿಕ್ಷಕರ ರೂಪ ಉಳಿಸಲಾಗಿಲ್ಲ — ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.",
            weeklyAvatars = "ಈ ವಾರದ ಅವತಾರಗಳು",
            newInDays = { days -> if (days == 1) "1 ದಿನದಲ್ಲಿ ಹೊಸದು" else "$days ದಿನಗಳಲ್ಲಿ ಹೊಸದು" },
            unlockHint = "ಅನ್ಲಾಕ್ ಮಾಡಲು 1 ಜಾಹೀರಾತು ನೋಡಿ. ನಿಮ್ಮದಾಗಿ ಇರಿಸಿ ಮತ್ತು ಹಂಚಿಕೊಳ್ಳಿ.",
            use = "ಬಳಸಿ",
            share = "ಹಂಚಿಕೊಳ್ಳಿ",
            unlockWithAds = "ಅನ್ಲಾಕ್ · 1 ಜಾಹೀರಾತು",
            locked = "ಲಾಕ್",
            savingTutor = "ನಿಮ್ಮ ಶಿಕ್ಷಕರನ್ನು ಉಳಿಸಲಾಗುತ್ತಿದೆ",
            unlockingPreset = { name -> "\"$name\" ಅನ್ಲಾಕ್ ಮಾಡಲಾಗುತ್ತಿದೆ" },
            outfit = { v ->
                when (v) {
                    1 -> "ದುಪ್ಪಟ್ಟು: ಶರ್ಟ್"
                    2 -> "ದುಪ್ಪಟ್ಟು: ಹುಡಿ"
                    3 -> "ದುಪ್ಪಟ್ಟು: V-ನೆಕ್"
                    else -> "ದುಪ್ಪಟ್ಟು: ಟೀ"
                }
            },
            neck = { v ->
                when (v) {
                    1 -> "ಕುತ್ತಿಗೆ: ಸಣ್ಣ"
                    2 -> "ಕುತ್ತಿಗೆ: ವಿಶಾಲ"
                    else -> "ಕುತ್ತಿಗೆ: ಸಾಮಾನ್ಯ"
                }
            },
            hair = { v ->
                when (v) {
                    1 -> "ಕೇಶ: ಸೈಡ್-ಪಾರ್ಟ್"
                    2 -> "ಕೇಶ: ಕರ್ಲಿ"
                    else -> "ಕೇಶ: ಗಜಗಜ"
                }
            },
            hairColor = { v ->
                when (v) {
                    1 -> "ಬಣ್ಣ: ಕಪ್ಪು"
                    2 -> "ಬಣ್ಣ: ಆಬರ್ನ್"
                    else -> "ಬಣ್ಣ: ಕಂದು"
                }
            },
            glasses = { v ->
                when (v) {
                    1 -> "ಕನ್ನಡಕ: ಗೋಲಾಕಾರ"
                    2 -> "ಕನ್ನಡಕ: ಇಲ್ಲ"
                    else -> "ಕನ್ನಡಕ: ಕ್ಲಾಸಿಕ್"
                }
            },
            frame = { v ->
                when (v) {
                    1 -> "ಫ್ರೇಮ್: ಕಂದು"
                    2 -> "ಫ್ರೇಮ್: ನೇವಿ"
                    else -> "ಫ್ರೇಮ್: ಕಪ್ಪು"
                }
            },
            eyeLine = { on -> if (on) "ಕಣ್ಣಿನ ರೇಖೆ: ಆನ್" else "ಕಣ್ಣಿನ ರೇಖೆ: ಆಫ್" },
            cheeks = { on -> if (on) "ಗಾಲ: ಆನ್" else "ಗಾಲ: ಆಫ್" },
            moodLabel = { v ->
                when (v) {
                    1 -> "ಮೂಡ್: ಸಂತೋಷ"
                    2 -> "ಮೂಡ್: ಕೋಪ"
                    3 -> "ಮೂಡ್: ಯೋಚನೆ"
                    4 -> "ಮೂಡ್: ಆಶ್ಚರ್ಯ"
                    5 -> "ಮೂಡ್: ದುಃಖ"
                    else -> "ಮೂಡ್: ಆಟೋ"
                }
            },
            gestureLabel = { v ->
                when (v) {
                    1 -> "ಸಂಕೇತ: ನಮಸ್ಕಾರ"
                    2 -> "ಸಂಕೇತ: ಚಪ್ಪಾಳೆ"
                    3 -> "ಸಂಕೇತ: ಸೂಚನೆ"
                    4 -> "ಸಂಕೇತ: ತಲೆ"
                    5 -> "ಸಂಕೇತ: ಯೋಚನೆ"
                    else -> "ಸಂಕೇತ: ಆಟೋ"
                }
            },
            spin360 = "360° ತಿರುಗಿಸಿ",
            swipeMoods = "ಮೂಡ್‌ಗಳನ್ನು ಸ್ವೈಪ್ ಮಾಡಿ",
            swipeOptions = "ಆಯ್ಕೆಗಳನ್ನು ಸ್ವೈಪ್ ಮಾಡಿ",
            swipeExpressions = "ಭಾವನೆಗಳನ್ನು ಸ್ವೈಪ್ ಮಾಡಿ",
            swipeAvatars = "ಅವತಾರಗಳನ್ನು ಸ್ವೈಪ್ ಮಾಡಿ",
        )
}
