package com.ncert7.aitutorandlab.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.ContentClickNavigation
import com.ncert7.aitutorandlab.service.analytics.SimulationAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.SimulationInteraction
import com.ncert7.aitutorandlab.service.analytics.SimulationSource
import com.ncert7.aitutorandlab.ui.screens.chapterscreen.ChapterScreen
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.ChatbotScreen
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.ConceptScreen
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.ConceptSimulationViewer
import com.ncert7.aitutorandlab.ui.screens.home.HomeScreen
import com.ncert7.aitutorandlab.ui.screens.progess.ProgressScreen
import com.ncert7.aitutorandlab.ui.screens.setting.SettingScreen
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.SimulationAgentScreen
import com.ncert7.aitutorandlab.ui.screens.subjectscreen.SubjectScreen
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.MathAgentScreen
import com.ncert7.aitutorandlab.ui.screens.revisionscreen.RevisionScreen
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary

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
    val items = listOf(BottomNavItem.Home, BottomNavItem.Progress, BottomNavItem.Setting)
    val navController = rememberNavController()

    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route

    val showBottomBar =
        currentRoute == BottomNavItem.Home.route || currentRoute == BottomNavItem.Progress.route || currentRoute == BottomNavItem.Setting.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = BackgroundPrimary, tonalElevation = 8.dp) {
                    items.forEach { item ->
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
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                HomeScreen(
                    onNavigateToLearning = { navController.navigate("learning") },
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
                                val encodedConceptId = if (conceptId.isNotBlank()) {
                                    java.net.URLEncoder.encode(conceptId, "UTF-8")
                                } else {
                                    "empty"
                                }
                                navController.navigate(
                                    "concept_sim_view/$encodedUrl/$title/$encodedConceptId//"
                                )
                            }
                        )
                    },
                    onSessionInvalid = onLogout
                )
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
                        navController.navigate("setting") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Setting.route) {
                SettingScreen(
                    onNavigateBack = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    },
                    onLogout = onLogout
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
                        navController.navigate("setting") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
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
                    onStudyClick = { chapterId, type ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackChapterListClick(chapterId, type) },
                            navigate = { navController.navigate("concepts/$chapterId/$type") }
                        )
                    },
                    onSimulationClick = { chapterId, type ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackChapterListClick(chapterId, type) },
                            navigate = { navController.navigate("concepts/$chapterId/$type") }
                        )
                    },
                    onRevisionClick = { chapterId ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackRevisionClick(chapterId) },
                            navigate = {
                                DebugLogger.debugLog("BottomNavBar", "Navigating to revision with chapterId: $chapterId")
                                navController.navigate("revision/$chapterId")
                            }
                        )
                    },
                    onGoHome = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    },
                    onGoSetting = {
                        navController.navigate("setting") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    },
                    onProgressClick = {
                        navController.navigate("progress") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
                        }
                    }
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
                        val encodedConceptId = java.net.URLEncoder.encode(conceptId, "UTF-8")
                        val encodedSubject = java.net.URLEncoder.encode(subjectName, "UTF-8")
                        val encodedChapter = java.net.URLEncoder.encode(chapterName, "UTF-8")
                        navController.navigate(
                            "concept_sim_view/$encodedUrl/$title/$encodedConceptId/$encodedSubject/$encodedChapter"
                        )
                    },
                    onGoSetting = {
                        navController.navigate("setting") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            restoreState = true
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
                        navController.navigate("setting") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
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
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}