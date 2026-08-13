package com.ncert7.aitutorandlab.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.MathAgentScreen
import com.ncert7.aitutorandlab.ui.screens.plan.PlanTrialNavigation
import com.ncert7.aitutorandlab.ui.screens.plan.PlanTrialScreen
import com.ncert7.aitutorandlab.ui.screens.revisionscreen.RevisionScreen
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.SimulationAgentScreen
import com.ncert7.aitutorandlab.ui.screens.subjectscreen.SubjectScreen

object LearningRoutes {
    const val HOME = "home"
    const val SUBJECTS = "subjects"
    const val CHAPTERS = "chapters/{subjectId}"
    const val CONCEPTS = "concepts/{chapterId}/{type}"
    const val CHATBOT = "chatbot?conceptId={conceptId}"
    const val MATH_AGENT = "math_agent?chapterId={chapterId}&problemId={problemId}"
    const val SIMULATION_AGENT = "simulation_agent/{simulationId}?conceptId={conceptId}"
    const val CONCEPT_SIM_VIEW = "concept_sim_view/{url}/{title}/{conceptId}/{subjectName}/{chapterName}"
    const val REVISION = "revision/{chapterId}"
}

@Composable
fun LearningNavigator(
    navController: NavHostController = rememberNavController(),
    onBackToHome: () -> Unit,
    onGoHome: () -> Unit = {},
    onGoSetting: () -> Unit = {},
    onGoProgress: () -> Unit = {}
) {
    NavigationAdGate { gated ->
        NavHost(
            navController = navController,
            startDestination = LearningRoutes.SUBJECTS,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(LearningRoutes.HOME) {
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
                                // Titles with `?` (e.g. "Are They Equal?") break unencoded path routes.
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
                    }
                )
            }

            composable(LearningRoutes.SUBJECTS) {
                SubjectScreen(
                    onBackClick = onBackToHome,
                    onSubjectClick = { subjectId ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackSubjectClick(subjectId) },
                            navigate = { navController.navigate("chapters/${subjectId}") }
                        )
                    },
                    onGoHome = onGoHome,
                    onGoSetting = onGoSetting
                )
            }

            composable(LearningRoutes.CHAPTERS) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId") ?: return@composable
                ChapterScreen(
                    subjectId = subjectId,
                    onBackClick = { navController.popBackStack() },
                    onOpenChapterTrial = { chapterId ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackChapterListClick(chapterId, "TRIAL") },
                            navigate = { navController.navigate("chapter_trial/$chapterId") }
                        )
                    },
                    onStudyClick = { chapterId, type ->
                        gated.run(
                            trackClick = { ContentClickNavigation.trackChapterListClick(chapterId, type) },
                            navigate = { navController.navigate("concepts/$chapterId/$type") }
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

            composable(LearningRoutes.CONCEPTS) { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                val type = backStackEntry.arguments?.getString("type") ?: "STUDY"
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
                    onSimulationAgentClick = { simulationId, conceptId ->
                        navController.navigate("simulation_agent/$simulationId?conceptId=$conceptId")
                    },
                    onSimulationClick = { title, url, conceptId, subjectName, chapterName ->
                        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                        // Titles with `?` (e.g. "Are They Equal?") break unencoded path routes.
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
                    onGoHome = onGoHome,
                    onGoSetting = onGoSetting
                )
            }

            composable(
                route = LearningRoutes.CHATBOT,
                arguments = listOf(navArgument("conceptId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val conceptId = backStackEntry.arguments?.getString("conceptId")
                ChatbotScreen(conceptId = conceptId)
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
                MathAgentScreen(problemId = problemId)
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

            composable(
                route = LearningRoutes.SIMULATION_AGENT,
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

            composable(LearningRoutes.REVISION) { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                DebugLogger.debugLog("LearningNavigator", "Revision route - chapterId: $chapterId")
                RevisionScreen(
                    chapterId = chapterId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
