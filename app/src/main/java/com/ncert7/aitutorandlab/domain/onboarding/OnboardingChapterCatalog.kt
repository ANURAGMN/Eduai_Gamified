package com.ncert7.aitutorandlab.domain.onboarding

import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.utils.SubjectIds
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.isKannadaLanguage

/** Class-7 NCERT chapter titles — fallback when Room has not synced yet. */
object OnboardingChapterCatalog {
    val mathChapters =
        listOf(
            "Integers",
            "Fractions and Decimals",
            "Data Handling",
            "Simple Equations",
            "Lines and Angles",
            "The Triangle and its Properties",
            "Comparing Quantities",
            "Rational Numbers",
            "Perimeter and Area",
            "Algebraic Expressions",
            "Exponents and Powers",
            "Symmetry",
            "Visualising Solid Shapes",
            "Practical Geometry",
        )

    val scienceChapters =
        listOf(
            "Nutrition in Plants",
            "Nutrition in Animals",
            "Fibre to Fabric",
            "Heat",
            "Acids, Bases and Salts",
            "Physical and Chemical Changes",
            "Weather, Climate and Adaptations of Animals to Climate",
            "Winds, Storms and Cyclones",
            "Soil",
            "Respiration in Organisms",
            "Transportation in Animals and Plants",
            "Reproduction in Plants",
            "Motion and Time",
            "Electric Current and its Effects",
            "Light",
            "Water: A Precious Resource",
            "Forests: Our Lifeline",
            "Wastewater Story",
        )

    val mathChaptersKn =
        listOf(
            "ಪೂರ್ಣಾಂಕಗಳು",
            "ಭಿನ್ನರಾಶಿಗಳು ಮತ್ತು ದಶಾಂಶಗಳು",
            "ದತ್ತಾಂಶದ ನಿರ್ವಹಣೆ",
            "ಸರಳ ಸಮೀಕರಣಗಳು",
            "ರೇಖೆಗಳು ಮತ್ತು ಕೋಣಗಳು",
            "ತ್ರಿಭುಜ ಮತ್ತು ಅದರ ಗುಣಲಕ್ಷಣಗಳು",
            "ಪ್ರಮಾಣಗಳ ಪರಿಗಣನೆ",
            "ಭಾಗಲಬ್ಧ ಸಂಖ್ಯೆಗಳು",
            "ಪರಿಧಿ ಮತ್ತು ವಿಸ್ತೀರ್ಣ",
            "ಬೀಜಗಣಿತೀಯ ಪದಗಳು",
            "ಘಾತಗಳು ಮತ್ತು ಘಾತಾಂಕಗಳು",
            "ಸಮಮಿತಿ",
            "ಘನಾಕಾರಗಳ ದೃಶ್ಯೀಕರಣ",
            "ಪ್ರಾಯೋಗಿಕ ರೇಖಾಗಣಿತ",
        )

    val scienceChaptersKn =
        listOf(
            "ಸಸ್ಯಗಳಲ್ಲಿ ಪೋಷಣೆ",
            "ಪ್ರಾಣಿಗಳಲ್ಲಿ ಪೋಷಣೆ",
            "ನಾರಿನಿಂದ ಬಟ್ಟೆ",
            "ಉಷ್ಣತೆ",
            "ಅಮ್ಲಗಳು, ಕ್ಷಾರಗಳು ಮತ್ತು ಲವಣಗಳು",
            "ಭೌತಿಕ ಮತ್ತು ರಾಸಾಯನಿಕ ಬದಲಾವಣೆಗಳು",
            "ಹವಾಮಾನ, ಜಲವಾಯು ಮತ್ತು ಪ್ರಾಣಿಗಳ ಅನುಕೂಲನ",
            "ಗಾಳಿಗಳು, ಬಿರುಗಾಳಿಗಳು ಮತ್ತು ಚಂಡಮಾರುತಗಳು",
            "ಮಣ್ಣು",
            "ಜೀವಿಗಳಲ್ಲಿ ಶ್ವಸನ",
            "ಪ್ರಾಣಿಗಳು ಮತ್ತು ಸಸ್ಯಗಳಲ್ಲಿ ಪರಿವಹನ",
            "ಸಸ್ಯಗಳಲ್ಲಿ ಪ್ರಜನನ",
            "ಚಲನೆ ಮತ್ತು ಸಮಯ",
            "ವಿದ್ಯುತ್ ಪ್ರವಾಹ ಮತ್ತು ಅದರ ಪರಿಣಾಮಗಳು",
            "ಬೆಳಕು",
            "ನೀರು: ಅಮೂಲ್ಯ ಸಂಪನ್ಮೂಲ",
            "ಅರಣ್ಯಗಳು: ನಮ್ಮ ಜೀವರೇಖೆ",
            "ಕೆಡು ನೀರಿನ ಕಥೆ",
        )

    fun fallbackForSubject(subjectKey: String, languageCode: String = "en"): List<String> =
        when (subjectKey) {
            "Math" ->
                if (isKannadaLanguage(languageCode)) mathChaptersKn else mathChapters
            "Science" ->
                if (isKannadaLanguage(languageCode)) scienceChaptersKn else scienceChapters
            else -> emptyList()
        }

    fun subjectIdForKey(subjectKey: String): String? =
        when (subjectKey) {
            "Math" -> SubjectIds.MATH
            "Science" -> SubjectIds.SCIENCE
            else -> null
        }

    fun chapterLabels(entities: List<ChapterEntity>, languageCode: String): List<String> =
        entities.sortedBy { it.orderIndex }.map { it.getLocalizedName(languageCode) }

    fun resolveChapter(
        entities: List<ChapterEntity>,
        pickedLabel: String,
        languageCode: String,
    ): ChapterEntity? {
        if (pickedLabel.isBlank()) return null
        val normalizedPick = normalizeLabel(pickedLabel)
        return entities.firstOrNull { entity ->
            listOf(entity.chapterName, entity.chapterNameKannada, entity.getLocalizedName(languageCode))
                .any { normalizeLabel(it) == normalizedPick }
        }
    }

    private fun normalizeLabel(raw: String): String =
        raw.trim()
            .lowercase()
            .replace("&", "and")
            .replace(Regex("\\s+"), " ")
}
