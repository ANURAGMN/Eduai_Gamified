package com.ncert7.aitutorandlab.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.config.LocalNativeTutorAvatarEnabled
import com.ncert7.aitutorandlab.config.rememberNativeTutorAvatarEnabled
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.ContentClickNavigation
import com.ncert7.aitutorandlab.service.analytics.NavClickAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SimulationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SimulationInteraction
import com.ncert7.aitutorandlab.service.analytics.SimulationSource
import com.ncert7.aitutorandlab.ui.screens.chapterscreen.ChapterScreen
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.ChatbotScreen
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.ConceptScreen
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.ConceptSimulationViewer
import com.ncert7.aitutorandlab.config.ReelsFeatureFlags
import com.ncert7.aitutorandlab.ui.screens.friends.FriendsScreen
import com.ncert7.aitutorandlab.ui.screens.garden.AvatarTabNavigation
import com.ncert7.aitutorandlab.ui.screens.reels.ReelsExploreScreen
import com.ncert7.aitutorandlab.ui.screens.reels.ReelsPlayerScreen
import com.ncert7.aitutorandlab.ui.screens.home.GamifiedHomeRoute
import com.ncert7.aitutorandlab.ui.screens.home.HomeScreen
import com.ncert7.aitutorandlab.ui.screens.textbook.TextbookWebScreen
import com.ncert7.aitutorandlab.ui.screens.textbook.TextbooksScreen
import com.ncert7.aitutorandlab.ui.screens.leagues.LeaguesScreen
import com.ncert7.aitutorandlab.ui.screens.plan.PlanDayActions
import com.ncert7.aitutorandlab.ui.screens.plan.PlanOverviewScreen
import com.ncert7.aitutorandlab.ui.screens.plan.PlanTrialNavigation
import com.ncert7.aitutorandlab.ui.screens.plan.PlanTrialScreen
import com.ncert7.aitutorandlab.ui.screens.home.viewmodel.HomeViewModel
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.ncert7.aitutorandlab.ui.screens.progess.ProgressScreen
import com.ncert7.aitutorandlab.ui.screens.setting.AvatarStudioRoute
import com.ncert7.aitutorandlab.ui.screens.setting.SettingScreen
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.SimulationAgentScreen
import com.ncert7.aitutorandlab.ui.screens.subjectscreen.GamifiedSubjectScreen
import com.ncert7.aitutorandlab.ui.screens.subjectscreen.SubjectScreen
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.MathAgentScreen
import com.ncert7.aitutorandlab.ui.screens.revisionscreen.RevisionScreen
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anurag.eduai.uikit.components.EduIntroTourOverlay
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.anurag.eduai.uikit.navigation.EduBottomNavBadges
import com.anurag.eduai.uikit.navigation.EduBottomNavBar
import com.anurag.eduai.uikit.navigation.EduBottomNavItem
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.ui.screens.quests.QuestsRoute
import androidx.navigation.NavHostController
import com.ncert7.aitutorandlab.notification.NotificationDeepLink
import com.ncert7.aitutorandlab.notification.NotificationDeepLinkStore
import com.ncert7.aitutorandlab.utils.NavTourCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.ui.theme.TextSecondary

private fun NavHostController.navigateToTab(route: String) {
    NavClickAnalyticsTracker.trackNavTab(route)
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun BottomNavBar(onLogout: () -> Unit = {}) {
    NavigationAdGate { gated ->
        BottomNavBarContent(onLogout = onLogout, gated = gated)
    }
}

@Composable
private fun BottomNavBarContent(
    onLogout: () -> Unit,
    gated: GatedNavigationAction
) {
    val context = LocalContext.current
    val isGamified = GamificationFeatureFlags.isGamifiedHomeEnabled(context)
    // Reels replaces the Quests tab only when the feature flag is on; otherwise the bar is unchanged
    // (Quests stays). When on, Quests is reachable from Settings instead.
    val reelsEnabled = ReelsFeatureFlags.isReelsEnabled()
    val reelsNavItems =
        if (reelsEnabled) {
            listOf(
                EduBottomNavItem.Home,
                EduBottomNavItem.Plan,
                EduBottomNavItem.Reels,
                EduBottomNavItem.Leagues,
                EduBottomNavItem.Avatar,
                EduBottomNavItem.Profile,
            )
        } else {
            EduBottomNavItem.defaultBarItems
        }
    val legacyItems = listOf(BottomNavItem.Home, BottomNavItem.Progress, BottomNavItem.Setting)
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { SharedPreferenceUtils(context) }

    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route

    // Two-phase first-run coach tour, owned here because both phases live at this level: phase 1 is
    // the home rails (inside GamifiedHomeRoute), phase 2 spotlights the Plan and Avatar tabs in the
    // bottom bar (rendered here, over the Scaffold). Streak celebrations wait until both are done.
    var homeRailTourDone by rememberSaveable { mutableStateOf(sharedPrefs.hasCompletedHomeTour()) }
    var navTourDone by rememberSaveable { mutableStateOf(sharedPrefs.hasCompletedNavTour()) }
    var navTourStep by rememberSaveable { mutableIntStateOf(0) }
    var tourViewport by remember { mutableStateOf<Rect?>(null) }
    // On-screen rect of each bottom-nav tab, for spotlighting a tab (e.g. Reels) in the nav tour.
    var navTabBounds by remember { mutableStateOf<Map<EduBottomNavItem, Rect>>(emptyMap()) }

    val navigateToTrial: (Int) -> Unit = { dayIndex ->
        navController.navigate(PlanTrialNavigation.routeForDay(dayIndex))
    }

    LaunchedEffect(Unit) {
        when (val link = NotificationDeepLinkStore.consume()) {
            is NotificationDeepLink.Tab -> navController.navigateToTab(link.route)
            is NotificationDeepLink.Screen -> navController.navigate(link.route)
            null -> Unit
        }
    }

    val gamifiedTabRoutes = EduBottomNavItem.entries.map { it.route }.toSet()
    val showBottomBar =
        if (isGamified) {
            currentRoute in gamifiedTabRoutes
        } else {
            currentRoute == BottomNavItem.Home.route ||
                currentRoute == BottomNavItem.Progress.route ||
                currentRoute == BottomNavItem.Setting.route
        }

    val badgeViewModel: HomeViewModel = hiltViewModel()
    val todayQuest by badgeViewModel.todayQuest.collectAsState()
    val unseenFriendFeed by badgeViewModel.unseenFriendFeed.collectAsState()
    val questBadge =
        if (isGamified) {
            todayQuest?.let { quest ->
                (quest.simsTotal > 0 && quest.simsDone >= quest.simsTotal && !quest.simsClaimed) ||
                    (quest.studyTotal > 0 && quest.studyDone >= quest.studyTotal && !quest.studyClaimed) ||
                    (
                        quest.simsTotal > 0 &&
                            quest.studyTotal > 0 &&
                            quest.simsDone >= quest.simsTotal &&
                            quest.studyDone >= quest.studyTotal &&
                            !quest.bonusClaimed
                    )
            } ?: false
        } else {
            false
        }

    val nativeTutorAvatarEnabled = rememberNativeTutorAvatarEnabled()

    CompositionLocalProvider(LocalNativeTutorAvatarEnabled provides nativeTutorAvatarEnabled) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { tourViewport = it.boundsInRoot() },
    ) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                if (isGamified) {
                    EduAiTheme {
                        EduBottomNavBar(
                            currentRoute = currentRoute.orEmpty(),
                            badges =
                                EduBottomNavBadges(
                                    quests = questBadge,
                                    leagues = unseenFriendFeed > 0,
                                    profile = false,
                                ),
                            items = reelsNavItems,
                            onItemSelected = { item ->
                                if (currentRoute == item.route) return@EduBottomNavBar
                                navController.navigateToTab(item.route)
                            },
                            onItemBounds = { item, rect -> navTabBounds = navTabBounds + (item to rect) },
                        )
                    }
                } else {
                    NavigationBar(containerColor = BackgroundPrimary, tonalElevation = 8.dp) {
                        legacyItems.forEach { item ->
                            val selected = currentRoute == item.route

                            NavigationBarItem(
                                selected = selected,
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) TextPrimary else TextSecondary
                                    )
                                },
                                label = {
                                    if (selected) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary
                                        )
                                    }
                                },
                                onClick = {
                                    if (currentRoute == item.route) return@NavigationBarItem
                                    navController.navigateToTab(item.route)
                                },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent
                                    )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(BottomNavItem.Home.route) {
                val context = LocalContext.current
                if (GamificationFeatureFlags.isGamifiedHomeEnabled(context)) {
                    GamifiedHomeRoute(
                        gated = gated,
                        onNavigateToLearning = { navController.navigate("gamified_subjects") },
                        onNavigateToChapters = { subjectId ->
                            navController.navigate("chapters/$subjectId")
                        },
                        onNavigateToSettings = {
                            if (isGamified) {
                                navController.navigateToTab(EduBottomNavItem.Profile.route)
                            } else {
                                navController.navigateToTab(BottomNavItem.Setting.route)
                            }
                        },
                        onNavigateToProgress = {
                            navController.navigate(BottomNavItem.Progress.route)
                        },
                        onNavigateToPlan = {
                            navController.navigateToTab(EduBottomNavItem.Plan.route)
                        },
                        onNavigateToQuests = {
                            navController.navigateToTab(EduBottomNavItem.Quests.route)
                        },
                        onNavigateToLeagues = {
                            navController.navigateToTab(EduBottomNavItem.Leagues.route)
                        },
                        onNavigateToAvatar = {
                            navController.navigateToTab(EduBottomNavItem.Avatar.route)
                        },
                        onNavigateToFriends = { navController.navigate("friends") },
                        onNavigateToReels = {
                            navController.navigateToTab(EduBottomNavItem.Reels.route)
                        },
                        onNavigateToRevision = { chapterId ->
                            navController.navigate("revision/$chapterId")
                        },
                        onLessonClick = { conceptId ->
                            navController.navigate("chatbot?conceptId=$conceptId")
                        },
                        onNavigateToTrial = navigateToTrial,
                        onNavigateToRoute = { route -> navController.navigate(route) },
                        onOpenTextbooks = { navController.navigate("textbooks") },
                        onSimulationClick = { simulationId, conceptId ->
                            navController.navigate("simulation_agent/$simulationId?conceptId=$conceptId")
                        },
                        onSessionInvalid = onLogout,
                        showIntroTour = !homeRailTourDone,
                        onIntroTourFinished = { skipped, step ->
                            if (skipped) {
                                EngagementAnalyticsTracker.homeTourSkip(step)
                            } else {
                                EngagementAnalyticsTracker.homeTourComplete()
                            }
                            sharedPrefs.setHomeTourCompleted()
                            homeRailTourDone = true
                        },
                        streaksEnabled = navTourDone,
                    )
                } else {
                    HomeScreen(
                        onNavigateToLearning = { navController.navigate("learning") },
                        onOpenTextbooks = { navController.navigate("textbooks") },
                        onNavigateToChapters = { subjectId ->
                            navController.navigate("chapters/$subjectId")
                        },
                        onLessonClick = { conceptId ->
                            gated.run(
                                trackClick = { ContentClickNavigation.trackHomeLessonClick(conceptId) },
                                navigate = { navController.navigate("chatbot?conceptId=$conceptId") }
                            )
                        },
                        onSimulationClick = { simulationId, conceptId ->
                            gated.run(
                                trackClick = {
                                    SimulationAnalyticsTracker.trackSimulationClickAndWait(
                                        conceptId = conceptId,
                                        interaction = SimulationInteraction.AGENT,
                                        source = SimulationSource.HOME
                                    )
                                },
                                navigate = {
                                    navController.navigate("simulation_agent/$simulationId?conceptId=$conceptId")
                                }
                            )
                        },
                        onSimulationUrlClick = { title, url, conceptId ->
                            gated.run(
                                trackClick = {
                                    SimulationAnalyticsTracker.trackSimulationClickAndWait(
                                        conceptId = conceptId,
                                        interaction = SimulationInteraction.URL,
                                        source = SimulationSource.HOME
                                    )
                                },
                                navigate = {
                                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                    // Titles like "Are They Equal?" contain `?` which breaks the
                                    // path route if left raw — encode like url/conceptId.
                                    val encodedTitle = java.net.URLEncoder.encode(
                                        title.replace("/", "-"),
                                        "UTF-8",
                                    )
                                    val encodedConceptId = if (conceptId.isNotBlank()) {
                                        java.net.URLEncoder.encode(conceptId, "UTF-8")
                                    } else {
                                        "empty"
                                    }
                                    navController.navigate(
                                        "concept_sim_view/$encodedUrl/$encodedTitle/$encodedConceptId//"
                                    )
                                }
                            )
                        },
                        onSessionInvalid = onLogout
                    )
                }
            }
            composable(EduBottomNavItem.Plan.route) {
                val context = LocalContext.current
                val sharedPrefs = SharedPreferenceUtils(context)
                val selectedSubjectId = sharedPrefs.getSubjectSelectionId()
                val homeViewModel: HomeViewModel = hiltViewModel()
                PlanOverviewScreen(
                    showBackNavigation = false,
                    onDayClick = { day ->
                        PlanDayActions.navigateForPlanDay(
                            day = day,
                            gated = gated,
                            selectedSubjectId = selectedSubjectId,
                            scope = scope,
                            resolveChapterId = { conceptId ->
                                homeViewModel.chapterIdForConcept(conceptId)
                            },
                            onNavigateToPlan = { },
                            onNavigateToChapters = { subjectId ->
                                navController.navigate("chapters/$subjectId")
                            },
                            onNavigateToRevision = { chapterId ->
                                navController.navigate("revision/$chapterId")
                            },
                            onLessonClick = { conceptId ->
                                gated.run(
                                    trackClick = { ContentClickNavigation.trackHomeLessonClick(conceptId) },
                                    navigate = { navController.navigate("chatbot?conceptId=$conceptId") },
                                )
                            },
                            onNavigateToTrial = navigateToTrial,
                        )
                    },
                )
            }
            composable(
                route = "plan_trial/{dayIndex}",
                arguments = listOf(navArgument("dayIndex") { type = NavType.IntType }),
            ) {
                PlanTrialScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onItemClick = { item ->
                        scope.launch {
                            ContentClickNavigation.trackPlanTrialItemClick(
                                itemId = item.conceptId.ifBlank { item.sourceId },
                                kind = item.kind,
                            )
                        }
                        PlanTrialNavigation.buildDestination(item)?.let { route ->
                            navController.navigate(route)
                        }
                    },
                )
            }
            composable(EduBottomNavItem.Quests.route) {
                QuestsRoute(
                    gated = gated,
                    onNavigateToPlan = { navController.navigateToTab(EduBottomNavItem.Plan.route) },
                    onNavigateToChapters = { subjectId ->
                        navController.navigate("chapters/$subjectId")
                    },
                    onNavigateToRevision = { chapterId ->
                        navController.navigate("revision/$chapterId")
                    },
                    onNavigateToTrial = navigateToTrial,
                    onNavigateToRoute = { route -> navController.navigate(route) },
                    onLessonClick = { conceptId ->
                        navController.navigate("chatbot?conceptId=$conceptId")
                    },
                    onSimulationClick = { simulationId, conceptId ->
                        navController.navigate("simulation_agent/$simulationId?conceptId=$conceptId")
                    },
                )
            }
            composable(EduBottomNavItem.Leagues.route) {
                LeaguesScreen(showBackNavigation = false)
            }
            composable(EduBottomNavItem.Reels.route) {
                ReelsExploreScreen(
                    onPlay = { video -> navController.navigate("reels_player/${video.videoId}") },
                    onBack = null,
                )
            }
            composable(
                route = "reels_player/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
                ReelsPlayerScreen(videoId = videoId, onBack = { navController.popBackStack() })
            }
            composable(EduBottomNavItem.Avatar.route) {
                AvatarStudioRoute(showBackNavigation = false)
            }
            composable(EduBottomNavItem.Profile.route) {
                SettingScreen(
                    showBackNavigation = false,
                    onNavigateBack = {
                        navController.navigateToTab(EduBottomNavItem.Home.route)
                    },
                    onLogout = onLogout,
                    onNavigateToFriends = { navController.navigate("friends") },
                    onNavigateToAvatarStudio = {
                        navController.navigateToTab(EduBottomNavItem.Avatar.route)
                    },
                    onNavigateToProgress = {
                        navController.navigate(BottomNavItem.Progress.route)
                    },
                    onNavigateToQuests = {
                        navController.navigate(EduBottomNavItem.Quests.route)
                    },
                )
            }
            composable("plan_overview") {
                val context = LocalContext.current
                val sharedPrefs = SharedPreferenceUtils(context)
                val selectedSubjectId = sharedPrefs.getSubjectSelectionId()
                val scope = rememberCoroutineScope()
                val homeViewModel: HomeViewModel = hiltViewModel()
                PlanOverviewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDayClick = { day ->
                        PlanDayActions.navigateForPlanDay(
                            day = day,
                            gated = gated,
                            selectedSubjectId = selectedSubjectId,
                            scope = scope,
                            resolveChapterId = { conceptId ->
                                homeViewModel.chapterIdForConcept(conceptId)
                            },
                            onNavigateToPlan = { /* already on plan */ },
                            onNavigateToChapters = { subjectId ->
                                navController.navigate("chapters/$subjectId")
                            },
                            onNavigateToRevision = { chapterId ->
                                navController.navigate("revision/$chapterId")
                            },
                            onLessonClick = { conceptId ->
                                gated.run(
                                    trackClick = { ContentClickNavigation.trackHomeLessonClick(conceptId) },
                                    navigate = { navController.navigate("chatbot?conceptId=$conceptId") },
                                )
                            },
                            onNavigateToTrial = navigateToTrial,
                        )
                    },
                )
            }
            composable("leagues") {
                LeaguesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable("friends") {
                FriendsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(BottomNavItem.Progress.route) {
                ProgressScreen(
                    onGoHome = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    },
                    onGoSetting = {
                        if (isGamified) {
                            navController.navigateToTab(EduBottomNavItem.Profile.route)
                        } else {
                            navController.navigate(BottomNavItem.Setting.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(BottomNavItem.Setting.route) {
                SettingScreen(
                    onNavigateBack = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToFriends = { navController.navigate("friends") },
                    onNavigateToAvatarStudio = { navController.navigate("avatar_studio") },
                    onLogout = onLogout
                )
            }
            composable("avatar_studio") {
                AvatarStudioRoute(onNavigateBack = { navController.popBackStack() })
            }
            composable("gamified_subjects") {
                GamifiedSubjectScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSubjectClick = { subjectId ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackSubjectClick(subjectId) },
                            navigate = { navController.navigate("chapters/$subjectId") },
                        )
                    },
                )
            }
            composable("learning") {
                LearningNavigator(
                    onBackToHome = { navController.popBackStack() },
                    onGoHome = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    },
                    onGoSetting = {
                        if (isGamified) {
                            navController.navigateToTab(EduBottomNavItem.Profile.route)
                        } else {
                            navController.navigate(BottomNavItem.Setting.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onGoProgress = {
                        navController.navigate("progress") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    }
                )
            }
            composable("chapters/{subjectId}") { backStackEntry ->
                val subjectId =
                    backStackEntry.arguments?.getString("subjectId") ?: return@composable
                ChapterScreen(
                    subjectId = subjectId,
                    onBackClick = { navController.popBackStack() },
                    onOpenChapterTrial = { chapterId ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackChapterListClick(chapterId, "TRIAL") },
                            navigate = { navController.navigate("chapter_trial/$chapterId") },
                        )
                    },
                    onStudyClick = { chapterId, type ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackChapterListClick(chapterId, type) },
                            navigate = { navController.navigate("concepts/$chapterId/$type") },
                        )
                    },
                )
            }
            composable(
                route = "chapter_trial/{chapterId}",
                arguments = listOf(navArgument("chapterId") { type = NavType.StringType }),
            ) {
                val scope = rememberCoroutineScope()
                PlanTrialScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onItemClick = { item ->
                        scope.launch {
                            ContentClickNavigation.trackPlanTrialItemClick(
                                itemId = item.conceptId.ifBlank { item.sourceId },
                                kind = item.kind,
                            )
                        }
                        PlanTrialNavigation.buildDestination(item)?.let { route ->
                            navController.navigate(route)
                        }
                    },
                )
            }
            composable("concepts/{chapterId}/{type}") { backStackEntry ->
                val chapterId =
                    backStackEntry.arguments?.getString("chapterId") ?: return@composable
                val type =
                    backStackEntry.arguments?.getString("type") ?: return@composable
                ConceptScreen(
                    chapterId = chapterId,
                    type = type,
                    onBackClick = { navController.popBackStack() },
                    onConceptClick = { conceptId, problemId, conceptType ->
                        gated.run(
                            trackClick = {
                                ContentClickNavigation.trackConceptClick(conceptId, problemId, conceptType)
                            },
                            navigate = {
                                when (conceptType) {
                                    "MATH PROBLEM" -> navController.navigate("math_agent?chapterId=$chapterId&problemId=$problemId")
                                    else -> navController.navigate("chatbot?conceptId=$conceptId")
                                }
                            }
                        )
                    },
                    onGoHome = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    },
                    onSimulationAgentClick = { simulationId, conceptId ->
                        navController.navigate("simulation_agent/$simulationId?conceptId=$conceptId")
                    },
                    onSimulationClick = { title, url, conceptId, subjectName, chapterName ->
                        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                        val encodedTitle = java.net.URLEncoder.encode(
                            title.replace("/", "-"),
                            "UTF-8",
                        )
                        val encodedConceptId = java.net.URLEncoder.encode(conceptId, "UTF-8")
                        val encodedSubject = java.net.URLEncoder.encode(subjectName, "UTF-8")
                        val encodedChapter = java.net.URLEncoder.encode(chapterName, "UTF-8")
                        navController.navigate(
                            "concept_sim_view/$encodedUrl/$encodedTitle/$encodedConceptId/$encodedSubject/$encodedChapter"
                        )
                    },
                    onGoSetting = {
                        if (isGamified) {
                            navController.navigateToTab(EduBottomNavItem.Profile.route)
                        } else {
                            navController.navigate(BottomNavItem.Setting.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable("subjects") {
                SubjectScreen(
                    onBackClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onSubjectClick = { subject ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackSubjectClick(subject) },
                            navigate = { navController.navigate("chapters/${subject}") }
                        )
                    },
                    onGoHome = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoSetting = {
                        if (isGamified) {
                            navController.navigateToTab(EduBottomNavItem.Profile.route)
                        } else {
                            navController.navigate(BottomNavItem.Setting.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(
                route = "chatbot?conceptId={conceptId}",
                arguments = listOf(navArgument("conceptId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val conceptId = backStackEntry.arguments?.getString("conceptId")
                ChatbotScreen(conceptId = conceptId)
            }

            composable("revision/{chapterId}") { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                DebugLogger.debugLog("BottomNavBar", "Revision route - chapterId: $chapterId")
                RevisionScreen(
                    chapterId = chapterId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = LearningRoutes.MATH_AGENT,
                arguments = listOf(
                    navArgument("chapterId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("problemId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val problemId = backStackEntry.arguments?.getString("problemId")
                MathAgentScreen(
                    problemId = problemId
                )
            }

            composable(
                route = "simulation_agent/{simulationId}?conceptId={conceptId}",
                arguments = listOf(
                    navArgument("simulationId") { type = NavType.StringType },
                    navArgument("conceptId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val simulationId = backStackEntry.arguments?.getString("simulationId")!!
                val conceptId = backStackEntry.arguments?.getString("conceptId") ?: ""
                SimulationAgentScreen(
                    simulationId = simulationId,
                    conceptId = conceptId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = LearningRoutes.CONCEPT_SIM_VIEW,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("conceptId") { type = NavType.StringType },
                    navArgument("subjectName") { type = NavType.StringType; defaultValue = "" },
                    navArgument("chapterName") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: "Simulation"
                val conceptId = backStackEntry.arguments?.getString("conceptId") ?: ""
                val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
                val chapterName = backStackEntry.arguments?.getString("chapterName") ?: ""

                ConceptSimulationViewer(
                    simulationUrl = url,
                    simulationTitle = title,
                    conceptId = conceptId,
                    subjectName = subjectName,
                    chapterName = chapterName,
                    onBackClick = { navController.popBackStack() },
                    onNext = { route -> navController.navigate(route) },
                )
            }

            composable("textbooks") {
                TextbooksScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBook = { book ->
                        val encUrl = java.net.URLEncoder.encode(book.url, "UTF-8")
                        val encTitle = java.net.URLEncoder.encode(book.title, "UTF-8")
                        navController.navigate("textbook/$encUrl/$encTitle")
                    },
                )
            }

            composable(
                route = "textbook/{url}/{title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val rawUrl = backStackEntry.arguments?.getString("url") ?: ""
                val rawTitle = backStackEntry.arguments?.getString("title") ?: "Textbook"
                val url = try {
                    java.net.URLDecoder.decode(rawUrl, "UTF-8")
                } catch (_: Exception) {
                    rawUrl
                }
                val title = try {
                    java.net.URLDecoder.decode(rawTitle, "UTF-8")
                } catch (_: Exception) {
                    rawTitle
                }
                TextbookWebScreen(
                    url = url,
                    title = title,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    // Coach tour phase 2 — a guided walkthrough that actually steps INTO each screen: exam planner →
    // avatar/garden → leaderboard → home, one card per screen (lighter scrim so the screen shows
    // through). Rendered over the Scaffold so it survives the tab switches. Skip or the final card
    // returns home and enables the streak celebrations.
    val navTourActive = isGamified && homeRailTourDone && !navTourDone
    val navLanguage = getCurrentLanguageCode()
    val navWalkthroughSteps =
        remember(navLanguage, reelsEnabled) {
            NavTourCopy.steps(navLanguage).let { steps ->
                if (reelsEnabled) steps
                else steps.filterNot { it.route == EduBottomNavItem.Reels.route }
            }
        }
    val finishNavTour: (skipped: Boolean) -> Unit = { skipped ->
        if (skipped) {
            EngagementAnalyticsTracker.navWalkthroughSkip(navTourStep)
        } else {
            EngagementAnalyticsTracker.navWalkthroughComplete()
        }
        // Release the Avatar tab's segment back to the user before leaving the tour.
        AvatarTabNavigation.setForcedSegment(null)
        navController.navigateToTab(EduBottomNavItem.Home.route)
        sharedPrefs.setNavTourCompleted()
        navTourDone = true
    }
    LaunchedEffect(navTourActive, navTourStep, navWalkthroughSteps) {
        if (!navTourActive) {
            AvatarTabNavigation.setForcedSegment(null)
            return@LaunchedEffect
        }
        val step = navWalkthroughSteps.getOrNull(navTourStep)
        val route = step?.route ?: EduBottomNavItem.Home.route
        // Drive the Avatar tab through Scene → Journey → Look as the cards advance; null on
        // non-Avatar steps hands control back so normal tab switches behave.
        AvatarTabNavigation.setForcedSegment(step?.segment)
        EngagementAnalyticsTracker.navWalkthroughStep(route, navTourStep)
        if (currentRoute != route) navController.navigateToTab(route)
    }
    // Keep system-back inside the walkthrough (step back, or exit to home from the first card) so it
    // can't desync the underlying tab navigation from the card being shown.
    BackHandler(enabled = navTourActive) {
        if (navTourStep > 0) navTourStep-- else finishNavTour(true)
    }
    if (navTourActive) {
        val stepData = navWalkthroughSteps.getOrNull(navTourStep)
        if (stepData != null) {
            // Spotlight the Reels tab icon on its step; other steps stay screen-level (no spotlight).
            val spotlightTarget =
                if (stepData.route == EduBottomNavItem.Reels.route) navTabBounds[EduBottomNavItem.Reels] else null
            EduAiTheme {
                EduIntroTourOverlay(
                    step = navTourStep,
                    total = navWalkthroughSteps.size,
                    target = spotlightTarget,
                    viewport = tourViewport,
                    title = stepData.title,
                    body = stepData.body,
                    onBack = { if (navTourStep > 0) navTourStep-- },
                    onNext = {
                        if (navTourStep >= navWalkthroughSteps.lastIndex) {
                            finishNavTour(false)
                        } else {
                            navTourStep++
                        }
                    },
                    onSkip = { finishNavTour(true) },
                    skipLabel = NavTourCopy.skipLabel(navLanguage),
                    backLabel = NavTourCopy.backLabel(navLanguage),
                    nextLabel = NavTourCopy.nextLabel(navLanguage),
                    doneLabel = NavTourCopy.doneLabel(navLanguage),
                    stepOfTotal = NavTourCopy.stepOfTotal(navLanguage),
                )
            }
        }
    }
    }
    }
}