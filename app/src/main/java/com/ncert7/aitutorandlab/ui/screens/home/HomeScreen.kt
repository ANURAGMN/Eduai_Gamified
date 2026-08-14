package com.ncert7.aitutorandlab.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.service.analytics.FunnelAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelStep
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.home.components.HomeScreenTopBar
import com.ncert7.aitutorandlab.ui.screens.home.components.LoadingHomeHeader
import com.ncert7.aitutorandlab.ui.screens.home.components.PracticeSimulationCard
import com.ncert7.aitutorandlab.ui.screens.home.components.TodayProgressCard
import com.ncert7.aitutorandlab.ui.theme.BackgroundSecondary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.screens.home.viewmodel.HomeViewModel
import com.ncert7.aitutorandlab.ui.screens.textbook.TextbookEntryCard
import java.time.LocalTime

@Composable
private fun rememberTimeBasedGreeting(): String {
    return stringResource(
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.good_morning
            in 12..16 -> R.string.good_afternoon
            in 17..21 -> R.string.good_evening
            else -> R.string.good_night
        }
    )
}

@Composable
fun HomeScreen(
    onNavigateToLearning: () -> Unit = {},
    onOpenTextbooks: () -> Unit = {},
    onNavigateToChapters: (String) -> Unit = {},
    onLessonClick: (String) -> Unit = {},
    onSimulationClick: (String, String) -> Unit = { _, _ -> },
    onSimulationUrlClick: (String, String, String) -> Unit = { _, _, _ -> },
    onSessionInvalid: () -> Unit = {}
) {
    TrackScreenEvent(screenName = ScreenName.HOME)

    val dimens = LocalDimensions.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val sharedPreferenceUtils = SharedPreferenceUtils(context)
    val selectedSubjectId = sharedPreferenceUtils.getSubjectSelectionId()

    val viewModel: HomeViewModel = hiltViewModel()

    val progressConcepts by viewModel.progressConcepts.collectAsState()
    val progressSimulations by viewModel.progressSimulations.collectAsState()

    val streakCount by viewModel.streakCount.collectAsState()
    val todayCompletedConceptCount by viewModel.todayConceptCount.collectAsState()
    val todayCompletedSimulationCount by viewModel.todaySimulationCount.collectAsState()
    val student by viewModel.student.collectAsState()
    val studentLoaded by viewModel.studentLoaded.collectAsState()
    val selectedSubjectName by viewModel.selectedSubjectName.collectAsState()
    val greeting = rememberTimeBasedGreeting()
    var homeViewTracked by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val currentLanguage = configuration.locales[0]?.language ?: "en"

    LaunchedEffect(currentLanguage) {
        viewModel.setLanguage(currentLanguage)
    }

    LaunchedEffect(selectedSubjectId) {
        viewModel.refreshSelectedSubjectName()
    }

    LaunchedEffect(studentLoaded, student) {
        if (studentLoaded && student != null && !homeViewTracked) {
            homeViewTracked = true
            FunnelAnalyticsTracker.track(FunnelStep.HOME_VIEW)
        }
        if (studentLoaded && student == null && sharedPreferenceUtils.isLoggedIn()) {
            sharedPreferenceUtils.clearAllUserData()
            sharedPreferenceUtils.clearAllAuthData()
            onSessionInvalid()
        }
    }

    val subjectLabel = selectedSubjectName.ifBlank {
        stringResource(R.string.select_subject)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BackgroundSecondary)
                    .verticalScroll(scrollState)
        ) {
            if (student == null) {
                LoadingHomeHeader(
                    subject = subjectLabel,
                    onChangeSubject = { onNavigateToLearning() }
                )
            } else {
                HomeScreenTopBar(
                    userName = student?.studentName ?: "",
                    subject = subjectLabel,
                    streakDays = streakCount,
                    greeting = greeting,
                    onChangeSubject = { onNavigateToLearning() }
                )
            }

            Column(modifier = Modifier.padding(dimens.screenPadding)) {
                TodayProgressCard(
                    progressConcepts = progressConcepts,
                    languageCode = currentLanguage,
                    onLessonClick = onLessonClick,
                    todayCompletedConcept = todayCompletedConceptCount,
                    todayCompletedSimulation = todayCompletedSimulationCount,
                    onShowAllChapters = {
                        onNavigateToChapters(selectedSubjectId)
                    }
                )
                Spacer(modifier = Modifier.height(dimens.spaceSmall))
                PracticeSimulationCard(
                    progressSimulations = progressSimulations,
                    languageCode = currentLanguage,
                    onSimulationClick = { simulationId, conceptId ->
                        onSimulationClick(simulationId, conceptId)
                    },
                    onSimulationUrlClick = { title, url, conceptId ->
                        onSimulationUrlClick(title, url, conceptId)
                    }
                )
                Spacer(modifier = Modifier.height(dimens.spaceSmall))
                TextbookEntryCard(onClick = onOpenTextbooks)
            }
        }
    }
}
