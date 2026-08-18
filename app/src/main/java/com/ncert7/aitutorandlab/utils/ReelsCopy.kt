package com.ncert7.aitutorandlab.utils

/** EN / KN labels for the reels (video lessons) UI. */
object ReelsCopy {
    fun title(lang: String) = if (isKannadaLanguage(lang)) "ರೀಲ್ಸ್" else "Reels"
    fun videoLessons(lang: String) = if (isKannadaLanguage(lang)) "ವೀಡಿಯೊ ಪಾಠಗಳು" else "Video lessons"
    fun searchHint(lang: String) = if (isKannadaLanguage(lang)) "ಪಾಠಗಳನ್ನು ಹುಡುಕಿ" else "Search lessons"
    fun newest(lang: String) = if (isKannadaLanguage(lang)) "ಹೊಸವು" else "Newest"
    fun mostWatched(lang: String) = if (isKannadaLanguage(lang)) "ಹೆಚ್ಚು ವೀಕ್ಷಿಸಿದವು" else "Most watched"
    fun seeAll(lang: String) = if (isKannadaLanguage(lang)) "ಎಲ್ಲವೂ" else "See all"
    fun emptyState(lang: String) = if (isKannadaLanguage(lang)) "ಇನ್ನೂ ವೀಡಿಯೊಗಳಿಲ್ಲ" else "No videos yet"
    fun noMatches(lang: String) = if (isKannadaLanguage(lang)) "ಹೊಂದಾಣಿಕೆ ಇಲ್ಲ" else "No matches"
    fun viewsSuffix(lang: String) = if (isKannadaLanguage(lang)) "ವೀಕ್ಷಣೆ" else "views"
}
