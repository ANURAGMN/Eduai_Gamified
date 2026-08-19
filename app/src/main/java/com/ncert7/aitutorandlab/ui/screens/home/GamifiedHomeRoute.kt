package com.ncert7.aitutorandlab.ui.screens.home

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.components.SubjectTile
import com.anurag.eduai.uikit.components.subjectMaterialIcon
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.runtime.SideEffect
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.HomeCopy
import com.ncert7.aitutorandlab.utils.StreakCopyFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.screens.EduHomeScreen
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.config.ReelsFeatureFlags
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.domain.gamification.QuestClaimType
import com.ncert7.aitutorandlab.utils.ReelsCopy
import com.ncert7.aitutorandlab.service.analytics.ContentClickNavigation
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelStep
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.SimulationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SimulationInteraction
import com.ncert7.aitutorandlab.service.analytics.SimulationSource
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.InviteChannel
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.components.QuestClaimDialog
import com.ncert7.aitutorandlab.ui.navigation.GatedNavigationAction
import com.anurag.eduai.uikit.screens.YoutubeVideoItem
import com.ncert7.aitutorandlab.ui.screens.home.components.YoutubeVideosSection
import com.ncert7.aitutorandlab.ui.screens.textbook.TextbookEntryCard
import com.ncert7.aitutorandlab.ui.screens.home.viewmodel.HomeViewModel
import com.ncert7.aitutorandlab.ui.screens.youtube.YoutubePlayerDialog
import com.ncert7.aitutorandlab.ui.screens.garden.AvatarTabNavigation
import com.ncert7.aitutorandlab.ui.screens.plan.PlanDayActions
import com.ncert7.aitutorandlab.ui.screens.plan.TrialQuestClickActions
import com.anurag.eduai.uikit.components.BookmarkItem
import com.anurag.eduai.uikit.components.PlanDayNode
import com.anurag.eduai.uikit.components.RevisionItem
import com.anurag.eduai.uikit.components.StreakCelebrationOverlay
import com.anurag.eduai.uikit.components.StreakExtendedOverlay
import com.ncert7.aitutorandlab.rating.AppRatingGate
import com.ncert7.aitutorandlab.rating.AppReviewManager
import com.ncert7.aitutorandlab.rating.ReviewRequestDecision
import java.time.LocalTime
import kotlinx.coroutines.launch

import com.ncert7.aitutorandlab.ui.screens.quests.questClaimDialogCopy
import com.ncert7.aitutorandlab.ui.screens.quests.questClaimResultMessage
import com.ncert7.aitutorandlab.ui.screens.quests.questClaimUnableToShowAd

private fun formatLeagueName(tier: String?, languageCode: String): String =
    HomeCopy.leagueCaption(tier, languageCode)

@Composable
private fun rememberGamifiedTimeBasedGreeting(): String {
    return stringResource(
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.good_morning
            in 12..16 -> R.string.good_afternoon
            in 17..21 -> R.string.good_evening
            else -> R.string.good_night
        },
    )
}

@Composable
fun GamifiedHomeRoute(
    gated: GatedNavigationAction,
    onNavigateToLearning: () -> Unit,
    onNavigateToChapters: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToQuests: () -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToAvatar: () -> Unit = {},
    onNavigateToReels: () -> Unit = {},
    onNavigateToRevision: (String) -> Unit,
    onLessonClick: (String) -> Unit,
    onNavigateToTrial: (Int) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onOpenTextbooks: () -> Unit = {},
    onSimulationClick: (String, String) -> Unit,
    onSessionInvalid: () -> Unit,
    // First-run home-rail tour, driven by the parent (BottomNavBar) which owns both tour phases.
    showIntroTour: Boolean = false,
    onIntroTourFinished: (skipped: Boolean, step: Int) -> Unit = { _, _ -> },
    // Streak celebrations wait until both tour phases (home rails + nav tabs) are done.
    streaksEnabled: Boolean = true,
) {
    TrackScreenEvent(screenName = ScreenName.HOME)

    val context = LocalContext.current
    val activity = context as? Activity
    val sharedPreferenceUtils = SharedPreferenceUtils(context)
    val selectedSubjectId = sharedPreferenceUtils.getSubjectSelectionId()
    val viewModel: HomeViewModel = hiltViewModel()
    val greeting = rememberGamifiedTimeBasedGreeting()

    val progressConcepts by viewModel.progressConcepts.collectAsState()
    val progressSimulations by viewModel.progressSimulations.collectAsState()
    val streakCount by viewModel.streakCount.collectAsState()
    val dailyStreakGreeting by viewModel.dailyStreakGreeting.collectAsState()
    val streakExtended by viewModel.streakExtended.collectAsState()
    val todayCompletedConceptCount by viewModel.todayConceptCount.collectAsState()
    val todayCompletedSimulationCount by viewModel.todaySimulationCount.collectAsState()
    val student by viewModel.student.collectAsState()
    val studentLoaded by viewModel.studentLoaded.collectAsState()
    val selectedSubjectName by viewModel.selectedSubjectName.collectAsState()
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val chapterCounts by viewModel.chapterCounts.collectAsState()
    val chapterProgressSums by viewModel.chapterProgressSums.collectAsState()
    val gamificationProfile by viewModel.gamificationProfile.collectAsState()
    val leagueRank by viewModel.leagueRank.collectAsState()
    val planDays by viewModel.planDays.collectAsState()
    val todayPlanDay by viewModel.todayPlanDay.collectAsState()
    val todayQuest by viewModel.todayQuest.collectAsState()
    val todayTrialItems by viewModel.todayTrialItems.collectAsState()
    val localizedTrialTitles by viewModel.localizedTrialTitles.collectAsState()
    val rewardedAdReady by viewModel.rewardedAdReady.collectAsState()
    val friendFeed by viewModel.friendFeed.collectAsState()
    val friendCount by viewModel.friendCount.collectAsState()
    val unseenFriendFeed by viewModel.unseenFriendFeed.collectAsState()
    val youtubeVideos by viewModel.youtubeVideos.collectAsState()
    val gardenProgress by viewModel.gardenProgress.collectAsState()
    val gardenPlantedItems by viewModel.gardenPlantedItems.collectAsState()
    val gardenHighlightNewPlant by viewModel.gardenHighlightNewPlant.collectAsState()
    val gardenHighlightStarterPlant by viewModel.gardenHighlightStarterPlant.collectAsState()
    val gardenEnabled = GamificationFeatureFlags.isGardenEnabled(context)

    var pendingQuestClaim by remember { mutableStateOf<QuestClaimType?>(null) }
    var selectedYoutubeVideo by remember { mutableStateOf<YoutubeVideoItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.preloadRewardedAd()
        viewModel.refreshYoutubeVideos()
        viewModel.refreshGardenOnHomeResume()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshGardenOnHomeResume()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(gardenHighlightNewPlant) {
        if (gardenHighlightNewPlant) {
            kotlinx.coroutines.delay(8_000)
            viewModel.acknowledgeGardenHighlight()
        }
    }

    LaunchedEffect(gardenHighlightStarterPlant) {
        if (gardenHighlightStarterPlant) {
            kotlinx.coroutines.delay(8_000)
            viewModel.acknowledgeGardenStarterHighlight()
        }
    }

    LaunchedEffect(rewardedAdReady) {
        if (!rewardedAdReady) {
            kotlinx.coroutines.delay(2500)
            viewModel.refreshRewardedAdState()
        }
    }

    val localeLanguage = getCurrentLanguageCode()
    SideEffect {
        viewModel.syncLanguage(localeLanguage)
    }
    val currentLanguage = localeLanguage
    var homeViewTracked by remember { mutableStateOf(false) }

    val youtubeItems =
        remember(currentLanguage, youtubeVideos) {
            viewModel.mapYoutubeItems(currentLanguage)
        }

    LaunchedEffect(selectedSubjectId) {
        viewModel.refreshSelectedSubjectName()
        viewModel.ensureExamPlanForCurrentSubject()
        viewModel.refreshExamPlanStatuses()
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

    // Fully policy-compliant rating ask: after finishing a task, flag pending review; on the next
    // home visit, request Google Play In-App Review. Google shows its own card (or nothing) — no
    // custom prompt. The gate throttles to once/day, up to 3 times.
    LaunchedEffect(Unit) {
        val hostActivity = activity ?: return@LaunchedEffect
        if (!sharedPreferenceUtils.isPendingReviewOnHome()) return@LaunchedEffect
        when (val decision = AppRatingGate.decideReviewRequest(context, sharedPreferenceUtils)) {
            ReviewRequestDecision.Proceed -> {
                sharedPreferenceUtils.setPendingReviewOnHome(false)
                EngagementAnalyticsTracker.reviewRequested("first_task_return")
                AppReviewManager.requestInAppReview(hostActivity)
            }
            is ReviewRequestDecision.Throttled -> {
                if (decision.reason == "max_requests") {
                    sharedPreferenceUtils.setPendingReviewOnHome(false)
                }
                EngagementAnalyticsTracker.reviewThrottled(decision.reason)
            }
            ReviewRequestDecision.NotEligible -> Unit
        }
    }

    val (homeState, focus) =
        remember(
            greeting,
            student?.studentName,
            streakCount,
            todayCompletedConceptCount,
            todayCompletedSimulationCount,
            selectedSubjectName,
            progressConcepts,
            progressSimulations,
            currentLanguage,
            gamificationProfile?.gems,
            gamificationProfile?.leagueTier,
            gamificationProfile?.weeklyXp,
            leagueRank,
            planDays,
            todayPlanDay?.dayIndex,
            todayPlanDay?.status,
            todayQuest?.simsDone,
            todayQuest?.simsTotal,
            todayQuest?.simsClaimed,
            todayQuest?.studyDone,
            todayQuest?.studyTotal,
            todayQuest?.studyClaimed,
            todayQuest?.bonusClaimed,
            todayTrialItems,
            localizedTrialTitles,
            friendFeed,
            friendCount,
            availableSubjects,
            chapterCounts,
            chapterProgressSums,
            gardenEnabled,
            gardenProgress,
            gardenPlantedItems,
            gardenHighlightNewPlant,
            gardenHighlightStarterPlant,
        ) {
            GamifiedHomeMapper.map(
                greeting = greeting,
                userName = student?.studentName.orEmpty(),
                streak = streakCount,
                todayConceptCount = todayCompletedConceptCount,
                todaySimulationCount = todayCompletedSimulationCount,
                selectedSubjectName = selectedSubjectName,
                selectedSubjectId = selectedSubjectId,
                progressConcepts = progressConcepts,
                progressSimulations = progressSimulations,
                languageCode = currentLanguage,
                gems = gamificationProfile?.gems ?: 0,
                leagueName = formatLeagueName(gamificationProfile?.leagueTier, currentLanguage),
                leagueRank = leagueRank,
                weeklyXp = gamificationProfile?.weeklyXp ?: 0,
                planDays = planDays,
                todayPlanDay = todayPlanDay,
                todayQuest = todayQuest,
                todayTrialItems = todayTrialItems,
                friends = friendFeed,
                friendCount = friendCount,
                availableSubjects = availableSubjects,
                chapterCountsBySubject = chapterCounts,
                chapterProgressSumsBySubject = chapterProgressSums,
                gardenEnabled = gardenEnabled,
                gardenProgress = gardenProgress,
                gardenHighlightNewPlant = gardenHighlightNewPlant,
                gardenHighlightStarterPlant = gardenHighlightStarterPlant,
                gardenPlantedItems = gardenPlantedItems,
                localizedTrialTitles = localizedTrialTitles,
            )
        }

    val shareFriendCodeFromHome: (String) -> Unit = { code ->
        if (code.isNotBlank()) {
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Add me on EduAI! My friend code is $code",
                    )
                }
            context.startActivity(Intent.createChooser(intent, "Share friend code"))
        }
    }

    val resolveChapterId: suspend (String) -> String? = { conceptId ->
        progressConcepts.firstOrNull { it.second?.conceptId == conceptId }?.second?.chapterId
            ?: progressSimulations.firstOrNull { it.second?.conceptId == conceptId }?.second?.chapterId
            ?: viewModel.chapterIdForConcept(conceptId)
    }

    val navigatePlanDay: (PlanDayNode) -> Unit = { day ->
        PlanDayActions.navigateForPlanDay(
            day = day,
            gated = gated,
            selectedSubjectId = selectedSubjectId,
            scope = scope,
            resolveChapterId = resolveChapterId,
            onNavigateToPlan = onNavigateToPlan,
            onNavigateToChapters = onNavigateToChapters,
            onNavigateToRevision = onNavigateToRevision,
            onLessonClick = onLessonClick,
            onNavigateToTrial = onNavigateToTrial,
        )
    }

    val navigateTodayTrial: () -> Unit = {
        val dayIndex =
            todayPlanDay?.dayIndex
                ?: planDays.firstOrNull { it.status == com.anurag.eduai.uikit.components.PlanDayStatus.Today }?.day
        if (dayIndex != null) {
            onNavigateToTrial(dayIndex)
        } else {
            onNavigateToPlan()
        }
    }

    EduAiTheme {
        pendingQuestClaim?.let { claimType ->
            val (title, message) = questClaimDialogCopy(claimType, currentLanguage)
            QuestClaimDialog(
                title = title,
                message = message,
                gemsReward = claimType.gemAmount(),
                adReady = rewardedAdReady,
                languageCode = currentLanguage,
                onWatchAd = {
                    val hostActivity = activity
                    if (hostActivity == null) {
                        pendingQuestClaim = null
                        scope.launch {
                            snackbarHostState.showSnackbar(questClaimUnableToShowAd(currentLanguage))
                        }
                        return@QuestClaimDialog
                    }
                    pendingQuestClaim = null
                    viewModel.claimQuestWithAd(hostActivity, claimType) { result ->
                        questClaimResultMessage(result, currentLanguage)?.let { text ->
                            scope.launch { snackbarHostState.showSnackbar(text) }
                        }
                    }
                },
                onDismiss = { pendingQuestClaim = null },
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
        // When Reels is on, Home taps go to the nocookie Reels player route (passed by parent).
        // Legacy dialog stays for the flag-off path.
        if (!ReelsFeatureFlags.isReelsEnabled()) {
            selectedYoutubeVideo?.let { video ->
                val startIndex =
                    youtubeItems.indexOfFirst { it.videoId == video.videoId }.coerceAtLeast(0)
                YoutubePlayerDialog(
                    videos = youtubeItems.ifEmpty { listOf(video) },
                    startIndex = startIndex,
                    languageCode = currentLanguage,
                    onDismiss = { selectedYoutubeVideo = null },
                )
            }
        }
        if (student == null && !studentLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            EduHomeScreen(
                state = homeState,
                showIntroTour = showIntroTour,
                onIntroTourFinished = onIntroTourFinished,
                onTourStep = { step ->
                    if (step == 0) EngagementAnalyticsTracker.homeTourStart()
                    EngagementAnalyticsTracker.homeTourStep(step)
                },
                onProfileClick = onNavigateToSettings,
                onStreakClick = onNavigateToProgress,
                onGemsClick = { /* Sprint 2+ */ },
                onLeagueClick = {
                    viewModel.refreshHomeLeagueRank()
                    onNavigateToLeagues()
                },
                showFriendDot = unseenFriendFeed > 0,
                onFriendsSeeAll = {
                    viewModel.markFriendFeedSeen()
                    onNavigateToFriends()
                },
                onAddFriend = {
                    viewModel.markFriendFeedSeen()
                    onNavigateToFriends()
                },
                onInviteShare = {
                    scope.launch {
                        val code = viewModel.getMyFriendCode().ifBlank {
                            gamificationProfile?.friendCode.orEmpty()
                        }
                        if (code.isBlank()) {
                            snackbarHostState.showSnackbar("Friend code not ready yet.")
                        } else {
                            GamificationAnalyticsTracker.inviteSent(InviteChannel.SHARE_SHEET)
                            shareFriendCodeFromHome(code)
                        }
                    }
                },
                onCheerFriend = { index -> viewModel.cheerFriendAtIndex(index) },
                onStartToday = navigateTodayTrial,
                onQuestsSeeAll = onNavigateToQuests,
                onOpenGarden = {
                    viewModel.acknowledgeGardenStarterHighlight()
                    AvatarTabNavigation.openScene()
                    onNavigateToAvatar()
                },
                onGardenRailClick = {
                    viewModel.acknowledgeGardenStarterHighlight()
                    AvatarTabNavigation.openScene()
                    onNavigateToAvatar()
                },
                onSimsQuestClick = {
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
                onStudyQuestClick = {
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
                                focus = focus,
                                onNavigateToRoute = onNavigateToRoute,
                            )
                        },
                    )
                },
                onBonusQuestClick = {
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
                                    "Complete both daily quests (sims + plan task) to unlock the bonus.",
                                )
                            }
                        }
                    }
                },
                onPlanSeeAll = onNavigateToPlan,
                onPlanAdd = {
                    viewModel.requestOpenPlanSetup()
                    onNavigateToPlan()
                },
                onPlanDayClick = navigatePlanDay,
                onTutorClick = navigateTodayTrial,
                onBookmarksSeeAll = onNavigateToLearning,
                onBookmarkOpen = { item ->
                    when {
                        item.isPlaceholder -> onNavigateToLearning()
                        item.simulationId.isNotBlank() && item.conceptId.isNotBlank() -> {
                            gated.run(
                                trackClick = {
                                    SimulationAnalyticsTracker.trackSimulationClickAndWait(
                                        conceptId = item.conceptId,
                                        interaction = SimulationInteraction.AGENT,
                                        source = SimulationSource.HOME,
                                    )
                                },
                                navigate = { onSimulationClick(item.simulationId, item.conceptId) },
                            )
                        }
                        item.conceptId.isNotBlank() -> {
                            gated.run(
                                trackClick = { ContentClickNavigation.trackHomeLessonClick(item.conceptId) },
                                navigate = { onLessonClick(item.conceptId) },
                            )
                        }
                        else -> onNavigateToLearning()
                    }
                },
                onRevisionOpen = { item ->
                    if (item.chapterId.isNotBlank()) {
                        gated.run(
                            trackClick = { ContentClickNavigation.trackRevisionClick(item.chapterId) },
                            navigate = { onNavigateToRevision(item.chapterId) },
                        )
                    } else if (item.conceptId.isNotBlank()) {
                        scope.launch {
                            val chapterId = resolveChapterId(item.conceptId)
                            if (chapterId != null) {
                                gated.run(
                                    trackClick = { ContentClickNavigation.trackRevisionClick(chapterId) },
                                    navigate = { onNavigateToRevision(chapterId) },
                                )
                            } else {
                                onNavigateToChapters(selectedSubjectId)
                            }
                        }
                    } else {
                        onNavigateToChapters(selectedSubjectId)
                    }
                },
                onSubjectOpen = { subject ->
                    val subjectId = subject.subjectId
                    if (subjectId.isBlank()) {
                        onNavigateToLearning()
                        return@EduHomeScreen
                    }
                    gated.run(
                        trackClick = { ContentClickNavigation.trackSubjectClick(subjectId) },
                        navigate = {
                            sharedPreferenceUtils.setSubjectSelectionId(subjectId)
                            viewModel.refreshSelectedSubjectName()
                            viewModel.refreshExamPlanStatuses()
                            onNavigateToChapters(subjectId)
                        },
                    )
                },
                subjectIconContent = { subject, tint ->
                    SubjectRailIcon(subject = subject, tint = tint)
                },
                belowSubjectsContent = {
                    Column {
                        YoutubeVideosSection(
                            title = HomeCopy.youtubeSectionTitle(currentLanguage),
                            videos = youtubeItems,
                            onVideoClick = { video ->
                                if (ReelsFeatureFlags.isReelsEnabled()) {
                                    onNavigateToRoute("reels_player/${video.videoId}")
                                } else {
                                    selectedYoutubeVideo = video
                                }
                            },
                            seeAllLabel =
                                if (ReelsFeatureFlags.isReelsEnabled()) {
                                    ReelsCopy.seeAll(currentLanguage)
                                } else {
                                    null
                                },
                            onSeeAll =
                                if (ReelsFeatureFlags.isReelsEnabled()) {
                                    onNavigateToReels
                                } else {
                                    null
                                },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextbookEntryCard(onClick = onOpenTextbooks)
                    }
                },
            )
        }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // Full-screen streak celebrations, on top of home. The triumphant "extended" beat wins if
            // both are queued (e.g. the day's first task just extended the streak).
            val todayIdx = (java.time.LocalDate.now().dayOfWeek.value - 1).coerceIn(0, 6)
            val streakCopy = remember(currentLanguage) { StreakCopyFactory.forLanguage(currentLanguage) }
            val celebrationName = homeState.userName.takeIf { it.isNotBlank() } ?: streakCopy.fallbackName
            val extendedShownValue = streakExtended
            LaunchedEffect(streaksEnabled, extendedShownValue) {
                if (streaksEnabled && extendedShownValue != null) {
                    EngagementAnalyticsTracker.streakExtendedShown(extendedShownValue)
                }
            }
            val greetingShownValue = dailyStreakGreeting
            LaunchedEffect(streaksEnabled, greetingShownValue, extendedShownValue) {
                if (streaksEnabled && extendedShownValue == null && greetingShownValue != null) {
                    EngagementAnalyticsTracker.streakGreetingShown(greetingShownValue)
                }
            }
            StreakExtendedOverlay(
                visible = streaksEnabled && streakExtended != null,
                streak = streakExtended ?: streakCount,
                name = celebrationName,
                doneDays = minOf(todayIdx + 1, streakExtended ?: streakCount, 7),
                todayIndex = todayIdx,
                copy = streakCopy,
                onDone = {
                    EngagementAnalyticsTracker.streakExtendedDone(streakExtended ?: streakCount)
                    viewModel.acknowledgeStreakExtended()
                },
            )
            StreakCelebrationOverlay(
                visible = streaksEnabled && streakExtended == null && dailyStreakGreeting != null,
                streak = dailyStreakGreeting ?: streakCount,
                name = celebrationName,
                doneDays = minOf(todayIdx, dailyStreakGreeting ?: streakCount),
                todayIndex = todayIdx,
                copy = streakCopy,
                onContinue = {
                    EngagementAnalyticsTracker.streakGreetingContinue(dailyStreakGreeting ?: streakCount)
                    viewModel.acknowledgeDailyStreakGreeting()
                },
            )
        }
    }
}

/** Renders the live backend subject icon (Glide) with a subject-glyph fallback. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun SubjectRailIcon(subject: SubjectTile, tint: Color) {
    if (!subject.iconUrl.isNullOrEmpty()) {
        GlideImage(
            model = subject.iconUrl,
            contentDescription = subject.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp)),
        )
    } else {
        Icon(
            imageVector = subjectMaterialIcon(subject.name),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(44.dp),
        )
    }
}
