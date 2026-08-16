package com.ncert7.aitutorandlab.utils

/** Tutor avatar settings section chrome (EN / KN). */
object TutorAvatarSettingsCopy {
    fun title(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ನಿಮ್ಮ ಶಿಕ್ಷಕ" else "Your tutor"

    fun body(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಅಧ್ಯಯನ, ಗಣಿತ, ಪುನರಾವಲೋಕನ ಮತ್ತು ಸಿಮ್ಯುಲೇಶನ್ ಏಜೆಂಟ್‌ಗಳಿಗೆ ನೋಟ ಆಯ್ಕೆಮಾಡಿ."
        } else {
            "Pick a look for Study, Math, Revision, and Simulation agents."
        }

    fun customizeInStudio(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಅವತಾರ್ ಸ್ಟುಡಿಯೋದಲ್ಲಿ ಕಸ್ಟಮೈಸ್ ಮಾಡಿ"
        } else {
            "Customize in Avatar Studio"
        }
}
