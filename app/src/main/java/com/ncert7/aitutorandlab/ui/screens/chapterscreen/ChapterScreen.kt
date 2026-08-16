package com.ncert7.aitutorandlab.ui.screens.chapterscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.components.EduChapterPicker
import com.anurag.eduai.uikit.components.EduChapterPickerItem
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.mathagent.usecase.MathIntent
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.chapterscreen.viewmodel.ChapterViewModel
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.viewmodel.MathViewModel

private const val MATH_SUBJECT_ID = "5c0a6b6d-7c6b-4f35-9d5b-9fd0fd8e8a01"

/**
 * Chapter picker — onboarding-style: pick a chapter, then Continue opens it as a trial.
 * Math chapters keep their existing problem flow.
 */
@Composable
fun ChapterScreen(
    subjectId: String,
    onBackClick: () -> Unit = {},
    onOpenChapterTrial: (chapterId: String) -> Unit = {},
    onStudyClick: (String, String) -> Unit = { _, _ -> },
    viewModel: ChapterViewModel = hiltViewModel(),
    mathViewModel: MathViewModel = hiltViewModel(),
) {
    TrackScreenEvent(screenName = ScreenName.CHAPTER)

    val state by viewModel.state.collectAsState()
    val mathState by mathViewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val currentLanguage = configuration.locales[0]?.language ?: "en"
    val isKannada = currentLanguage.startsWith("kn", ignoreCase = true)
    val isMathSubject = subjectId == MATH_SUBJECT_ID

    var selectedChapterId by remember { mutableStateOf("") }

    LaunchedEffect(subjectId, currentLanguage) {
        viewModel.loadChapters(subjectId, currentLanguage)
    }

    LaunchedEffect(state.chapters) {
        if (selectedChapterId.isEmpty() && state.chapters.isNotEmpty()) {
            selectedChapterId = state.chapters.first().id
        }
    }

    val context = LocalContext.current
    LaunchedEffect(subjectId, mathState.problems.isEmpty()) {
        if (isMathSubject && mathState.problems.isEmpty()) {
            val userId = SharedPreferenceUtils(context).getUserId() ?: ""
            if (userId.isNotEmpty()) {
                mathViewModel.onIntent(MathIntent.Initialize(userId))
            }
        }
    }

    EduAiTheme {
        when {
            state.isLoading && state.chapters.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.unable_to_load_chapters))
                }
            }
            else -> {
                // "Recommended" wording is dynamic: a fresh account sees "Start here"; once any
                // chapter has progress, the hint on the next incomplete chapter reads "Continue".
                val anyProgress = state.chapters.any { (it.progressUiModel?.completed ?: 0) > 0 }
                EduChapterPicker(
                    title = "${state.subjectName} · ${if (isKannada) "ಅಧ್ಯಾಯ ಆರಿಸಿ" else "pick a chapter"}",
                    subtitle = if (anyProgress) {
                        if (isKannada) "ಎಲ್ಲಿ ನಿಲ್ಲಿಸಿದ್ದೀರೋ ಅಲ್ಲಿಂದ ಮುಂದುವರಿಸಿ" else "Pick up where you left off"
                    } else {
                        if (isKannada) "ಎಲ್ಲಿಂದ ಪ್ರಾರಂಭಿಸಬೇಕು?" else "Where do you want to start?"
                    },
                    chapters = state.chapters.map {
                        EduChapterPickerItem(
                            id = it.id,
                            label = it.name,
                            done = it.progressUiModel?.completed ?: 0,
                            total = it.progressUiModel?.total ?: it.totalConcepts,
                        )
                    },
                    selectedId = selectedChapterId,
                    recommendedLabel = if (anyProgress) {
                        if (isKannada) "ಮುಂದುವರಿಸಿ" else "Continue"
                    } else {
                        if (isKannada) "ಇಲ್ಲಿ ಪ್ರಾರಂಭಿಸಿ" else "Start here"
                    },
                    backLabel = if (isKannada) "ವಿಷಯ" else "Subject",
                    continueLabel = if (isKannada) "ಮುಂದುವರಿಸಿ" else "Continue",
                    loadingLabel = if (isKannada) "ಅಧ್ಯಾಯಗಳನ್ನು ಲೋಡ್ ಮಾಡಲಾಗುತ್ತಿದೆ…" else "Loading chapters…",
                    emptyLabel =
                        if (isKannada) {
                            "ಈ ವಿಷಯಕ್ಕೆ ಯಾವುದೇ ಅಧ್ಯಾಯಗಳು ಲಭ್ಯವಿಲ್ಲ."
                        } else {
                            "No chapters available for this subject yet."
                        },
                    isLoading = state.isLoading,
                    onSelect = { id ->
                        selectedChapterId = id
                        // Tap opens immediately; Continue still works for the highlighted row.
                        DebugLogger.debugLog("ChapterScreen", "Chapter tap → trial: $id")
                        onOpenChapterTrial(id)
                    },
                    onBack = onBackClick,
                    onContinue = {
                        if (selectedChapterId.isNotEmpty()) {
                            // All subjects (incl. Math) open the chapter trial; the materializer
                            // builds Math problems + study + revision for Math chapters.
                            DebugLogger.debugLog("ChapterScreen", "Chapter continue → trial: $selectedChapterId")
                            onOpenChapterTrial(selectedChapterId)
                        }
                    },
                )
            }
        }
    }
}
