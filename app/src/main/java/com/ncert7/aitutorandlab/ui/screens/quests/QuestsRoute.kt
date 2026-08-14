package com.ncert7.aitutorandlab.ui.screens.quests

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.SideEffect
import com.ncert7.aitutorandlab.utils.HomeCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.components.QuestTrail
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.domain.gamification.QuestClaimType
import com.ncert7.aitutorandlab.service.analytics.ContentClickNavigation
import com.ncert7.aitutorandlab.service.analytics.SimulationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SimulationInteraction
import com.ncert7.aitutorandlab.service.analytics.SimulationSource
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.components.QuestClaimDialog
import com.ncert7.aitutorandlab.ui.navigation.GatedNavigationAction
import com.ncert7.aitutorandlab.ui.screens.home.GamifiedHomeMapper
import com.ncert7.aitutorandlab.ui.screens.home.viewmodel.HomeViewModel
import com.ncert7.aitutorandlab.ui.screens.plan.PlanDayActions
import com.ncert7.aitutorandlab.ui.screens.plan.TrialQuestClickActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsRoute(
    gated: GatedNavigationAction,
    onNavigateToPlan: () -> Unit,
    onNavigateToChapters: (String) -> Unit,
    onNavigateToRevision: (String) -> Unit,
    onNavigateToTrial: (Int) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onLessonClick: (String) -> Unit,
    onSimulationClick: (String, String) -> Unit,
) {
    TrackScreenEvent(screenName = ScreenName.QUESTS)

    val context = LocalContext.current
    val activity = context as? Activity
    val sharedPreferenceUtils = SharedPreferenceUtils(context)
    val selectedSubjectId = sharedPreferenceUtils.getSubjectSelectionId()
    val viewModel: HomeViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val student by viewModel.student.collectAsState()
    val studentLoaded by viewModel.studentLoaded.collectAsState()
    val progressConcepts by viewModel.progressConcepts.collectAsState()
    val progressSimulations by viewModel.progressSimulations.collectAsState()
    val planDays by viewModel.planDays.collectAsState()
    val todayPlanDay by viewModel.todayPlanDay.collectAsState()
    val todayQuest by viewModel.todayQuest.collectAsState()
    val todayTrialItems by viewModel.todayTrialItems.collectAsState()
    val rewardedAdReady by viewModel.rewardedAdReady.collectAsState()
    val gamificationProfile by viewModel.gamificationProfile.collectAsState()

    val localeLanguage = getCurrentLanguageCode()
    SideEffect {
        viewModel.syncLanguage(localeLanguage)
    }
    val currentLanguage = localeLanguage

    var pendingQuestClaim by remember { mutableStateOf<QuestClaimType?>(null) }

    LaunchedEffect(Unit) {
        viewModel.preloadRewardedAd()
    }

    LaunchedEffect(selectedSubjectId) {
        viewModel.refreshExamPlanStatuses()
    }

    val (_, focus) =
        remember(
            progressConcepts,
            progressSimulations,
            currentLanguage,
            planDays,
            todayPlanDay?.dayIndex,
            todayQuest?.simsDone,
            todayQuest?.studyDone,
            todayTrialItems,
        ) {
            GamifiedHomeMapper.map(
                greeting = "",
                userName = student?.studentName.orEmpty(),
                streak = 0,
                todayConceptCount = 0,
                todaySimulationCount = 0,
                selectedSubjectName = "",
                selectedSubjectId = selectedSubjectId,
                progressConcepts = progressConcepts,
                progressSimulations = progressSimulations,
                languageCode = currentLanguage,
                gems = gamificationProfile?.gems ?: 0,
                planDays = planDays,
                todayPlanDay = todayPlanDay,
                todayQuest = todayQuest,
                todayTrialItems = todayTrialItems,
            )
        }

    val resolveChapterId: suspend (String) -> String? = { conceptId ->
        progressConcepts.firstOrNull { it.second?.conceptId == conceptId }?.second?.chapterId
            ?: progressSimulations.firstOrNull { it.second?.conceptId == conceptId }?.second?.chapterId
            ?: viewModel.chapterIdForConcept(conceptId)
    }

    EduAiTheme {
        pendingQuestClaim?.let { claimType ->
            val (title, message) = questClaimDialogCopy(claimType)
            QuestClaimDialog(
                title = title,
                message = message,
                gemsReward = claimType.gemAmount(),
                adReady = rewardedAdReady,
                onWatchAd = {
                    val hostActivity = activity
                    if (hostActivity == null) {
                        pendingQuestClaim = null
                        scope.launch { snackbarHostState.showSnackbar("Unable to show ad.") }
                        return@QuestClaimDialog
                    }
                    pendingQuestClaim = null
                    viewModel.claimQuestWithAd(hostActivity, claimType) { result ->
                        questClaimResultMessage(result)?.let { text ->
                            scope.launch { snackbarHostState.showSnackbar(text) }
                        }
                    }
                },
                onDismiss = { pendingQuestClaim = null },
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            HomeCopy.questsSectionTitle(getCurrentLanguageCode()),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(EduAiTheme.colors.surface1)
                        .padding(padding),
            ) {
                when {
                    student == null && !studentLoaded -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = HomeCopy.questsScreenSubtitle(getCurrentLanguageCode()),
                                color = EduAiTheme.colors.textSecondary,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                            if (focus.conceptId == null && todayQuest == null) {
                                Text(
                                    text = HomeCopy.questsEmptyHint(getCurrentLanguageCode()),
                                    color = EduAiTheme.colors.textMuted,
                                )
                            } else {
                                val questState =
                                    GamifiedHomeMapper.map(
                                        greeting = "",
                                        userName = "",
                                        streak = 0,
                                        todayConceptCount = 0,
                                        todaySimulationCount = 0,
                                        selectedSubjectName = "",
                                        progressConcepts = progressConcepts,
                                        progressSimulations = progressSimulations,
                                        languageCode = currentLanguage,
                                        todayPlanDay = todayPlanDay,
                                        todayQuest = todayQuest,
                                    ).first.quests
                                QuestTrail(
                                    state = questState,
                                    onSeeAll = onNavigateToPlan,
                                    sectionTitle = HomeCopy.questsSectionTitle(getCurrentLanguageCode()),
                                    seeAllLabel = HomeCopy.seeAllLabel(getCurrentLanguageCode()),
                                    onSimsClick = {
                                        TrialQuestClickActions.handleSimsClick(
                                            quest = todayQuest,
                                            focus = focus,
                                            todayPlanDay = todayPlanDay,
                                            onClaim = { pendingQuestClaim = QuestClaimType.SIMS },
                                            onNavigateToRoute = onNavigateToRoute,
                                            onNavigateToTrial = onNavigateToTrial,
                                            onLegacyClick = {
                                                val simulationId = focus.simulationId
                                                val conceptId = focus.simulationConceptId
                                                if (simulationId != null && conceptId != null) {
                                                    gated.run(
                                                        trackClick = {
                                                            SimulationAnalyticsTracker.trackSimulationClickAndWait(
                                                                conceptId = conceptId,
                                                                interaction = SimulationInteraction.AGENT,
                                                                source = SimulationSource.HOME,
                                                            )
                                                        },
                                                        navigate = { onSimulationClick(simulationId, conceptId) },
                                                    )
                                                } else {
                                                    onNavigateToChapters(selectedSubjectId)
                                                }
                                            },
                                        )
                                    },
                                    onStudyClick = {
                                        TrialQuestClickActions.handleStudyClick(
                                            quest = todayQuest,
                                            focus = focus,
                                            todayPlanDay = todayPlanDay,
                                            onClaim = { pendingQuestClaim = QuestClaimType.STUDY },
                                            onNavigateToRoute = onNavigateToRoute,
                                            onNavigateToTrial = onNavigateToTrial,
                                            onLegacyClick = {
                                                PlanDayActions.navigateForTodayPlanDay(
                                                    todayPlanDay = todayPlanDay,
                                                    planDays = planDays,
                                                    gated = gated,
                                                    selectedSubjectId = selectedSubjectId,
                                                    scope = scope,
                                                    resolveChapterId = resolveChapterId,
                                                    onNavigateToPlan = onNavigateToPlan,
                                                    onNavigateToChapters = onNavigateToChapters,
                                                    onNavigateToRevision = onNavigateToRevision,
                                                    onLessonClick = onLessonClick,
                                                    fallbackConceptId = focus.conceptId,
                                                    onNavigateToTrial = onNavigateToTrial,
                                                )
                                            },
                                        )
                                    },
                                    onBonusClick = {
                                        val quest = todayQuest
                                        when {
                                            quest != null &&
                                                quest.simsTotal > 0 &&
                                                quest.studyTotal > 0 &&
                                                quest.simsDone >= quest.simsTotal &&
                                                quest.studyDone >= quest.studyTotal &&
                                                !quest.bonusClaimed -> {
                                                pendingQuestClaim = QuestClaimType.BONUS
                                            }
                                            quest?.bonusClaimed == true -> {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Bonus already claimed today.")
                                                }
                                            }
                                            else -> {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Complete both daily quests to unlock the bonus.",
                                                    )
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Gems balance: ${gamificationProfile?.gems ?: 0}",
                                color = EduAiTheme.colors.text,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
