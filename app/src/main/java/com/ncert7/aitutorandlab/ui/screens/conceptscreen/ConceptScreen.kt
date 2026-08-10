package com.ncert7.aitutorandlab.ui.screens.conceptscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.ncert7.aitutorandlab.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.usecase.ChatIntent
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.components.AdDialog
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.viewmodel.ChatViewModel
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.ConceptCard
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.ConceptScreenHeader
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.viewmodel.ConceptViewModel
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.viewmodel.SimulationAgentViewModel
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus

@Composable
fun ConceptScreen(
    chapterId: String,
    type: String,
    onBackClick: () -> Unit = {},
    onConceptClick: (conceptId: String, problemId: String, conceptType: String) -> Unit = { _, _, _ -> },
    onSimulationAgentClick: (String, String) -> Unit = { _, _ -> },
    onSimulationClick: (title: String, url: String, conceptId: String, subjectName: String, chapterName: String) -> Unit = { _, _, _, _, _ -> },
    onGoHome:() -> Unit = {},
    onGoSetting:() -> Unit = {},
    viewModel: ConceptViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
) {
    TrackScreenEvent(screenName = ScreenName.CONCEPT)

    val dimes = LocalDimensions.current
    val configuration = LocalConfiguration.current
    val state by viewModel.state.collectAsState()
    val pendingNavigation by viewModel.pendingNavigation.collectAsState()
    val showAdDialog by viewModel.showAdDialog.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLanguage = configuration.locales[0]?.language ?: "en"

    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { nav ->
            if (nav.isDirect) {
                DebugLogger.debugLog("ConceptScreen", "Performing Direct Navigation: ${nav.route}")
                when (nav.route) {
                    "simulation_agent" -> {
                        val simId = nav.simulationId
                        val conceptId = nav.conceptId
                        if (simId != null && conceptId != null) {
                            onSimulationAgentClick(simId, conceptId)
                        }
                    }
                    "concept_sim_view" -> {
                        if (nav.simulationTitle != null && nav.simulationUrl != null && nav.conceptId != null) {
                            onSimulationClick(
                                nav.simulationTitle,
                                nav.simulationUrl,
                                nav.conceptId,
                                state.subjectName,
                                state.chapterName
                            )
                        }
                    }
                }
                viewModel.clearPendingNavigation()
            }
        }
    }

    val simulationViewModel: SimulationAgentViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        simulationViewModel.loadAvailableSimulations()
    }

    LaunchedEffect(chapterId, type, currentLanguage) {
        viewModel.loadConcepts(chapterId, type, currentLanguage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundPrimary)
        ) {
            ConceptScreenHeader(
                classLevel = state.classLevel,
                subjectName = state.subjectName,
                chapterName = state.chapterName,
                progress = state.progressUiModel,
                onBackClick = onBackClick,
                onGoHome = onGoHome,
                onGoSetting = onGoSetting
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.unable_to_load_concepts),
                        color = TextPrimary
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(dimes.spaceMedium),
                ) {
                    Text(
                        text = when {
                            state.type.equals("TRIAL", ignoreCase = true) ->
                                if (currentLanguage.startsWith("kn", ignoreCase = true)) "ಪಾಠಗಳು ಮತ್ತು ಸಿಮ್ಯುಲೇಶನ್‌ಗಳು" else "Lessons & simulations"
                            state.type.equals("SIMULATION", ignoreCase = true) -> stringResource(R.string.simulations_to_explore)
                            state.type.equals("MATH PROBLEM", ignoreCase = true) -> stringResource(R.string.problem_to_solve)
                            else -> stringResource(R.string.lessons_to_master)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = dimes.spaceSmall)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(dimes.spaceMedium)
                    ) {
                        itemsIndexed(state.concepts, key = { _, it -> it.id }) { index, conceptUiModel ->
                            ConceptCard(
                                concept = conceptUiModel,
                                serialNumber = index + 1,
                                isTrial = state.type.equals("TRIAL", ignoreCase = true),
                                onClick = { conceptId, problemId, conceptType ->
                                    // Always start a fresh session — the "Continue vs Start new"
                                    // resume dialog has been removed to cut popups.
                                    chatViewModel.onIntent(ChatIntent.StartFreshSession(conceptUiModel.sessionKey))
                                    onConceptClick(conceptId, problemId, conceptType)
                                },
                                onSimulationAgentClick = { simId, conceptId ->
                                    if (conceptUiModel.status == ProgressStatus.NOT_STARTED) {
                                        simulationViewModel.clearSessionMapping(simId)
                                    }
                                    viewModel.onSimulationOpened(simId, conceptId)
                                },
                                onSimulationClick = { title, url, conceptId ->
                                    viewModel.onSimulationUrlOpened(title, url, conceptId)
                                }
                            )
                        }
                    }
                }
            }

            // Ad shown at this open boundary when in-sim interactions have crossed the policy
            // threshold (see ClickAdPolicy.SIM_INTERACTIONS_PER_AD); counter resets on dismiss.
            if (showAdDialog) {
                AdDialog(
                    context = context,
                    onDismiss = { viewModel.dismissAdAndNavigate() }
                )
            }

            // Session-resume dialog removed — concepts always start fresh (see onClick above).
        }
    }
}