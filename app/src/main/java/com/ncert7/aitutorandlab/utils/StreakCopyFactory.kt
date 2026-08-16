package com.ncert7.aitutorandlab.utils

import com.anurag.eduai.uikit.components.StreakCopy
import com.anurag.eduai.uikit.components.defaultStreakCopy

/** App-module factory for streak overlay chrome (EN / KN). */
object StreakCopyFactory {
    fun forLanguage(languageCode: String): StreakCopy =
        if (isKannadaLanguage(languageCode)) kannada() else defaultStreakCopy()

    private fun kannada(): StreakCopy =
        StreakCopy(
            dayStreakLabel = "ದಿನಗಳ ಸರಣಿ",
            greetingLine = { name -> "ನಿಮ್ಮನ್ನು ನೋಡಿ ಸಂತೋಷವಾಯಿತು, $name — ಇಂದು ಸರಣಿ ಉಳಿಸಿ." },
            continueLabel = "ಹೋಗೋಣ",
            extendedTitle = "ಸರಣಿ ವಿಸ್ತರಿಸಿದೆ!",
            extendedLine = { name -> "+೧ ದಿನ — ಚೆನ್ನಾಗಿ ಮಾಡಿದ್ದೀರಿ, $name." },
            awesomeLabel = "ಅದ್ಭುತ",
            fallbackName = "ಸ್ನೇಹಿತ",
            // Mon…Sun short forms (matches English week-row order)
            weekdayLetters = listOf("ಮಂ", "ಮ", "ಬು", "ಗು", "ಶು", "ಶ", "ಭಾ"),
        )
}
