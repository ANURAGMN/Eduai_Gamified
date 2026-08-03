package com.ncert7.aitutorandlab.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.domain.onboarding.OnboardingChapterCatalog
import com.ncert7.aitutorandlab.utils.SubjectIds
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val chapterDao: ChapterDao,
    private val sharedPrefs: SharedPreferenceUtils,
) : ViewModel() {
    private val _chaptersBySubject = MutableStateFlow(defaultChapters())
    val chaptersBySubject: StateFlow<Map<String, List<String>>> = _chaptersBySubject.asStateFlow()

    init {
        refreshChapters(normalizeLanguageCode(sharedPrefs.getLanguagePreference()))
    }

    fun refreshChapters(languageCode: String) {
        viewModelScope.launch {
            _chaptersBySubject.value =
                mapOf(
                    "Math" to loadChapters(SubjectIds.MATH, "Math", languageCode),
                    "Science" to loadChapters(SubjectIds.SCIENCE, "Science", languageCode),
                )
        }
    }

    private suspend fun loadChapters(
        subjectId: String,
        subjectKey: String,
        languageCode: String,
    ): List<String> {
        val fromDb =
            chapterDao.getChaptersForSubjectSync(subjectId)
                .sortedBy { it.orderIndex }
                .map { it.getLocalizedName(languageCode) }
        return fromDb.ifEmpty { OnboardingChapterCatalog.fallbackForSubject(subjectKey, languageCode) }
    }

    private fun defaultChapters(): Map<String, List<String>> {
        val language = normalizeLanguageCode(sharedPrefs.getLanguagePreference())
        return mapOf(
            "Math" to OnboardingChapterCatalog.fallbackForSubject("Math", language),
            "Science" to OnboardingChapterCatalog.fallbackForSubject("Science", language),
        )
    }
}
