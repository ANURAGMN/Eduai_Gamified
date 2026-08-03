package com.ncert7.aitutorandlab.utils

/** Home screen tutor bubble copy. */
object HomeCopy {
    fun tutorTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ನಿಮ್ಮ ಶಿಕ್ಷಕ" else "Your tutor"

    fun tutorMessage(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಇಂದಿನ ಕ್ವೆಸ್ಟ್‌ಗಳಿಗೆ ಸಿದ್ಧವೇ? ಹೋಗೋಣ!"
        } else {
            "Ready for today's quests? Let's go!"
        }

    fun youtubeSectionTitle(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "ವೀಡಿಯೋ ಪಾಠಗಳು" else "Video lessons"

    fun youtubePlaybackFailedMessage(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) {
            "ಈ ವೀಡಿಯೋವನ್ನು ಆ್ಯಪ್‌ನಲ್ಲಿ ಪ್ಲೇ ಮಾಡಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ."
        } else {
            "This video couldn't be played in the app."
        }

    fun openInYoutubeLabel(languageCode: String): String =
        if (isKannadaLanguage(languageCode)) "YouTube ನಲ್ಲಿ ತೆರೆಯಿರಿ" else "Open in YouTube"
}
