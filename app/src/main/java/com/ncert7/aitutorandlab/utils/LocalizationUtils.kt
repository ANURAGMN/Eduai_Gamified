package com.ncert7.aitutorandlab.utils

import androidx.appcompat.app.AppCompatDelegate
import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.SubjectEntity

object SubjectIds {
    const val MATH = "5c0a6b6d-7c6b-4f35-9d5b-9fd0fd8e8a01"
    const val SCIENCE = "9a7d0d20-7b8d-4b8c-8c12-5a1a8a55f002"
}

/** Remote subject icons (same URLs stored on Firestore Concept docs in the live app). */
object SubjectIconUrls {
    const val MATH = "https://anuragmn.github.io/EduAI_app/Simulations/maths.png"
    const val SCIENCE = "https://anuragmn.github.io/EduAI_app/Simulations/science.png"
}

/** Prefer synced iconUrl; fall back to known Math/Science artwork from the live app. */
fun resolveSubjectIconUrl(
    subjectId: String?,
    subjectName: String? = null,
    storedIconUrl: String? = null,
): String? {
    if (!storedIconUrl.isNullOrBlank()) return storedIconUrl
    return when (subjectId) {
        SubjectIds.MATH -> SubjectIconUrls.MATH
        SubjectIds.SCIENCE -> SubjectIconUrls.SCIENCE
        else -> {
            val name = subjectName.orEmpty()
            when {
                name.contains("math", ignoreCase = true) ||
                    name.contains("ಗಣಿತ", ignoreCase = false) -> SubjectIconUrls.MATH
                name.contains("science", ignoreCase = true) ||
                    name.contains("ವಿಜ್ಞಾನ", ignoreCase = false) -> SubjectIconUrls.SCIENCE
                else -> null
            }
        }
    }
}

/**
 * Resolve legacy subject name prefs to stable subject IDs.
 */
fun resolveStoredSubjectId(stored: String?): String {
    if (stored.isNullOrBlank()) return SubjectIds.SCIENCE
    if (stored.contains("-") && stored.length >= 32) return stored
    return when {
        stored.equals("science", ignoreCase = true) -> SubjectIds.SCIENCE
        stored.contains("math", ignoreCase = true) ||
            stored.contains("ಗಣಿತ", ignoreCase = false) -> SubjectIds.MATH
        stored.contains("science", ignoreCase = true) ||
            stored.contains("ವಿಜ್ಞಾನ", ignoreCase = false) -> SubjectIds.SCIENCE
        else -> SubjectIds.SCIENCE
    }
}

fun isKannadaLanguage(languageCode: String): Boolean =
    normalizeLanguageCode(languageCode) == "kn"

/**
 * Extension functions to get localized names based on app language
 */

fun SubjectEntity.getLocalizedName(languageCode: String = getCurrentLanguageCode()): String {
    return if (isKannadaLanguage(languageCode)) subjectNameKannada else subjectName
}

fun ChapterEntity.getLocalizedName(languageCode: String = getCurrentLanguageCode()): String {
    return if (isKannadaLanguage(languageCode)) {
        chapterNameKannada.ifBlank { chapterName }
    } else {
        chapterName
    }
}

fun ConceptEntity.getLocalizedName(languageCode: String = getCurrentLanguageCode()): String {
    return if (isKannadaLanguage(languageCode)) {
        conceptNameKannada.ifBlank { conceptName }
    } else {
        conceptName
    }
}

/** Optional prefs fallback when AppCompat locales are not applied yet (e.g. right after login). */
private var storedLanguagePreference: (() -> String?)? = null

fun bindStoredLanguagePreference(provider: () -> String?) {
    storedLanguagePreference = provider
}

/**
 * Check if the app is currently in Kannada language
 */
fun isKannada(): Boolean = isKannadaLanguage(getCurrentLanguageCode())

/**
 * Get current app language code (`en` or `kn`).
 */
fun getCurrentLanguageCode(): String {
    val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language
    if (!currentLocale.isNullOrBlank()) {
        return normalizeLanguageCode(currentLocale)
    }
    return normalizeLanguageCode(storedLanguagePreference?.invoke())
}

/**
 * Normalize profile/UI language values to progress language codes used in Room DB.
 * Accepts: en, kn, English, Kannada, en-IN, kn-IN, etc.
 */
fun normalizeLanguageCode(raw: String?): String {
    if (raw.isNullOrBlank()) return "en"
    return when (raw.trim().lowercase()) {
        "kn", "kannada" -> "kn"
        "en", "english" -> "en"
        else -> if (raw.startsWith("kn", ignoreCase = true)) "kn" else "en"
    }
}

/**
 * Resolve language for progress writes/queries: explicit value first, else current app locale.
 */
fun resolveProgressLanguage(language: String? = null): String {
    return if (language.isNullOrBlank()) getCurrentLanguageCode() else normalizeLanguageCode(language)
}

/** Legacy Firestore/local rows without explicit language — excluded from today's counts. */
const val LEGACY_PROGRESS_LANGUAGE = "legacy"

/** Language codes used for new progress writes and today's progress queries. */
fun isExplicitProgressLanguage(language: String): Boolean =
    language == "en" || language == "kn"

/**
 * Resolve language when restoring progress from Firestore.
 * Legacy docs (no language field, no _en/_kn doc suffix) map to [LEGACY_PROGRESS_LANGUAGE].
 */
fun resolveProgressLanguageFromFirestore(documentId: String, languageField: String?): String {
    if (!languageField.isNullOrBlank()) return normalizeLanguageCode(languageField)
    return when {
        documentId.endsWith("_kn") -> "kn"
        documentId.endsWith("_en") -> "en"
        else -> LEGACY_PROGRESS_LANGUAGE
    }
}

/** Legacy values stored before normalization (Firebase profile / early builds). */
fun legacyProgressLanguageAlias(normalized: String): String? = when (normalized) {
    "en" -> "English"
    "kn" -> "Kannada"
    else -> null
}

