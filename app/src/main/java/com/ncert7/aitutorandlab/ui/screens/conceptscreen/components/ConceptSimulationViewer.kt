package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.InteractionTracker
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.viewmodel.ConceptSimulationViewModel
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.SimulationWebView
import java.net.URLDecoder

@Composable
fun ConceptSimulationViewer(
    simulationUrl: String,
    simulationTitle: String,
    conceptId: String = "",
    subjectName: String = "",
    chapterName: String = "",
    onBackClick: () -> Unit = {},
    viewModel: ConceptSimulationViewModel = hiltViewModel()
) {
    val decodedConceptId = remember(conceptId) {
        try {
            URLDecoder.decode(conceptId, "UTF-8")
        } catch (e: Exception) {
            conceptId
        }
    }

    TrackScreenEvent(
        screenName = ScreenName.SIMULATIONVIEWER,
        conceptId = decodedConceptId
    )

    val isInitPending by viewModel.isAdCheckPending.collectAsState()

    val decodedTitle = try {
        URLDecoder.decode(simulationTitle, "UTF-8")
    } catch (e: Exception) {
        simulationTitle
    }

    val decodedUrl = try {
        URLDecoder.decode(simulationUrl, "UTF-8")
    } catch (e: Exception) {
        simulationUrl
    }

    val decodedSubject = try {
        URLDecoder.decode(subjectName, "UTF-8")
    } catch (e: Exception) {
        subjectName
    }

    val decodedChapter = try {
        URLDecoder.decode(chapterName, "UTF-8")
    } catch (e: Exception) {
        chapterName
    }

    var progressMarked by remember { mutableStateOf(false) }

    LaunchedEffect(decodedConceptId, decodedUrl, decodedTitle) {
        if (decodedConceptId.isNotEmpty() && decodedUrl.isNotEmpty() && decodedTitle.isNotEmpty()) {
            DebugLogger.debugLog(
                "ConceptSimulationViewer",
                "LaunchedEffect: Initializing viewer for conceptId=$decodedConceptId"
            )
            viewModel.initializeSimulationWithAdCheck(
                conceptId = decodedConceptId,
                simulationUrl = decodedUrl,
                simulationTitle = decodedTitle
            )
            InteractionTracker.startSession(
                simulationTitle = decodedTitle,
                subjectName = decodedSubject,
                chapterName = decodedChapter
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            InteractionTracker.endSession()
        }
    }

    val handlePageLoaded = {
        if (decodedConceptId.isNotEmpty() && decodedConceptId != "empty" && !progressMarked) {
            progressMarked = true
            viewModel.markSimulationCompleted(decodedConceptId)
            DebugLogger.debugLog(
                "ConceptSimulationViewer",
                "Simulation page loaded for concept: $decodedConceptId"
            )
        }
    }

    when {
        isInitPending -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SimulationHeader(
                title = decodedTitle,
                onBackClick = onBackClick
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                SimulationWebView(
                    url = decodedUrl,
                    onPageFinished = handlePageLoaded
                )
            }
        }
    }
}
