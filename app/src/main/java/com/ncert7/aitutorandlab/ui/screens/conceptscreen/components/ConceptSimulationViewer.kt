package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ncert7.aitutorandlab.config.LocalNativeTutorAvatarEnabled
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.service.analytics.InteractionTracker
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AgentTutorAvatarBubble
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.viewmodel.ConceptSimulationViewModel
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.SimulationWebView
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.rememberSimulationKeyConceptTts
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakSimulationFooter
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakSimulationStep
import com.ncert7.aitutorandlab.utils.TrialCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import java.net.URLDecoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val MAX_GUIDE_STEPS = 9

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
    val sessionInteractions by InteractionTracker.sessionInteractionCount.collectAsState()
    val trialPrompt by viewModel.trialPrompt.collectAsState()
    val viewerSession by viewModel.viewerSession.collectAsState()
    val languageCode = remember { getCurrentLanguageCode() }
    val inTrialMode = TrialSessionStore.activeTrialItemId != null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val sharedPrefs = remember { SharedPreferenceUtils(context) }
    var voiceEnabled by remember { mutableStateOf(sharedPrefs.getSimulationVoiceEnabled()) }
    val (keyConceptTts, ttsState) = rememberSimulationKeyConceptTts(languageCode = languageCode)
    val useNativeAvatar = LocalNativeTutorAvatarEnabled.current
    val wordBoundaryIndex by keyConceptTts.wordBoundaryIndex.collectAsState()

    // Stop narration the moment the screen leaves the foreground (home button, an ad/reward video,
    // tapping through to the Play Store, multi-window focus loss). Backgrounding does NOT dispose
    // this composable, so without this the TTS would keep talking behind the ad / from the
    // background. isForeground also gates the timers so they don't speak while we're away.
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner, keyConceptTts) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE -> {
                    isForeground = false
                    keyConceptTts.stop()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(voiceEnabled, keyConceptTts) {
        keyConceptTts.speechEnabled = voiceEnabled
        sharedPrefs.setSimulationVoiceEnabled(voiceEnabled)
    }

    LaunchedEffect(sessionInteractions) {
        if (inTrialMode) {
            viewModel.syncTrialSimClickCount(sessionInteractions)
            viewModel.onTrialSimClickCountChanged(sessionInteractions)
        }
    }

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

    var progressMarked by remember(decodedUrl) { mutableStateOf(false) }

    var guideDoc by remember(decodedUrl) { mutableStateOf<SimGuideDoc?>(null) }
    var guideFetchDone by remember(decodedUrl) { mutableStateOf(false) }

    LaunchedEffect(decodedUrl) {
        guideDoc = SimGuideRepository.fetchGuide(context, decodedUrl)
        guideFetchDone = true
    }

    LaunchedEffect(decodedUrl) {
        if (viewModel.beginViewerSession(decodedUrl)) {
            keyConceptTts.resetDedupe()
        }
    }

    val introTts by rememberUpdatedState(keyConceptTts)
    val introSimulationKey by rememberUpdatedState(decodedUrl)
    val introTitle by rememberUpdatedState(decodedTitle)

    // The top-of-page description (everything above the interactive canvas). It is now delivered
    // as the FIRST guided step — read aloud through the same step-narration path — instead of a
    // separate pre-guide narration. This removes the intro-vs-guide sequencing entirely.
    var introText by remember(decodedUrl) { mutableStateOf<String?>(null) }
    val onSimulationIntroReported: (String) -> Unit = { htmlText ->
        val text =
            htmlText.trim().takeIf { it.isNotEmpty() }
                ?: introTitle.takeIf { it.isNotBlank() }
        if (text != null && introText == null) {
            introText = text
            viewModel.markIntroHandled(decodedUrl)
        }
    }

    val guideSteps =
        remember(viewerSession.harvestJson, guideDoc, guideFetchDone, introText, decodedUrl) {
            val harvest = viewerSession.harvestJson
            if (harvest == null || viewerSession.url != decodedUrl || !guideFetchDone) {
                emptyList()
            } else {
                val hosted =
                    guideDoc?.let { SimGuideBuilder.buildFromDoc(it, harvest) }?.takeIf { it.isNotEmpty() }
                val steps =
                    if (hosted != null) {
                        // A hosted guide already opens with its own intro step.
                        hosted
                    } else {
                        // Heuristic sims: lead with the page's own description, then the controls.
                        val heuristic = SimGuideBuilder.build(harvest)
                        introText?.takeIf { it.isNotBlank() }
                            ?.let { listOf(SimGuideStep(it, null)) + heuristic }
                            ?: heuristic
                    }
                SimGuideBuilder.finalizeSteps(steps, MAX_GUIDE_STEPS)
            }
        }

    // The description below the canvas — captured early, spoken later by the 1.25-min timer.
    var footerHtmlText by remember(decodedUrl) { mutableStateOf<String?>(null) }
    val onSimulationFooterReported: (String) -> Unit = { htmlText ->
        htmlText.trim().takeIf { it.isNotEmpty() }?.let { text ->
            if (footerHtmlText == null) footerHtmlText = text
        }
    }

    // Second narration ("description of the simulation") at 1.25 min. Driven from here (not the
    // WebView) so it survives rotation and always fires — falling back to the concept description
    // if the page had no readable footer text.
    LaunchedEffect(decodedUrl, viewerSession.startedAtMs, voiceEnabled) {
        if (!voiceEnabled || viewerSession.url != decodedUrl || viewerSession.startedAtMs <= 0L) {
            return@LaunchedEffect
        }
        delay(viewModel.remainingFooterDelayMs())
        if (!isForeground) return@LaunchedEffect
        if (!viewModel.shouldHandleFooter(decodedUrl) || viewModel.viewerSession.value.url != decodedUrl) {
            return@LaunchedEffect
        }
        val text =
            footerHtmlText
                ?: viewModel.keyConceptFallback(decodedConceptId)
                ?: decodedTitle.takeIf { it.isNotBlank() }
        if (!text.isNullOrBlank()) {
            viewModel.markFooterHandled(decodedUrl)
            introTts.speakSimulationFooter(text = text, simulationKey = introSimulationKey)
        }
    }

    // Unlock the guided coach once its walkthrough is ready — the controls are harvested, the
    // hosted guide (if any) has resolved, and the intro text (its first step) is in hand. Gating
    // on introText keeps step 0 stable, so the very first narration is the intro itself.
    LaunchedEffect(
        viewerSession.harvestJson,
        guideFetchDone,
        introText,
        viewerSession.guideUnlocked,
        decodedUrl,
    ) {
        if (viewerSession.url != decodedUrl || viewerSession.guideUnlocked) return@LaunchedEffect
        val introReady = introText != null || guideDoc != null
        if (viewerSession.harvestJson != null && guideFetchDone && introReady) {
            viewModel.unlockGuide(decodedUrl)
        }
    }

    val onPageFinishedHandler by rememberUpdatedState {
        {
            if (viewModel.shouldHandlePageReady(decodedUrl)) {
                viewModel.markPageReadyHandled(decodedUrl)
                viewModel.captureTrialItemId()
                DebugLogger.debugLog(
                    "ConceptSimulationViewer",
                    "Simulation page ready — overlay in ${viewModel.remainingOverlayDelayMs() / 1000}s",
                )
            }
            if (decodedConceptId.isNotEmpty() && decodedConceptId != "empty" && !progressMarked) {
                progressMarked = true
                if (TrialSessionStore.activeTrialItemId == null) {
                    viewModel.markSimulationCompleted(decodedConceptId)
                } else {
                    DebugLogger.debugLog(
                        "ConceptSimulationViewer",
                        "Trial mode — URL completion tracked via click count for concept: $decodedConceptId",
                    )
                }
                DebugLogger.debugLog(
                    "ConceptSimulationViewer",
                    "Simulation page loaded for concept: $decodedConceptId",
                )
            }
            // If the page never reported readable intro text, fall back to the title so the
            // guide's first step still has something to say (it's narrated as step 0, not here).
            if (viewModel.shouldHandleIntro(decodedUrl)) {
                scope.launch {
                    delay(3_000)
                    if (!viewModel.shouldHandleIntro(decodedUrl)) return@launch
                    viewModel.markIntroHandled(decodedUrl)
                    if (introText == null) {
                        introText = introTitle.takeIf { it.isNotBlank() } ?: decodedTitle
                    }
                }
            }
        }
    }

    LaunchedEffect(isInitPending, decodedUrl, viewerSession.startedAtMs) {
        if (isInitPending || decodedUrl.isBlank() || viewerSession.url != decodedUrl) return@LaunchedEffect
        viewModel.captureTrialItemId()
        val remaining = viewModel.remainingOverlayDelayMs()
        DebugLogger.debugLog(
            "ConceptSimulationViewer",
            "2-min overlay timer — ${remaining / 1000}s remaining for $decodedUrl",
        )
        delay(remaining)
        viewModel.showTimeBasedExplorePromptIfNeeded()
    }

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
            if (TrialSessionStore.activeTrialItemId != null) {
                InteractionTracker.setSessionCountingEnabled(false)
            }
        }
    }

    LaunchedEffect(progressMarked, decodedConceptId) {
        if (
            progressMarked &&
            decodedConceptId.isNotEmpty() &&
            TrialSessionStore.activeTrialItemId != null
        ) {
            InteractionTracker.setSessionCountingEnabled(true)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // A configuration change (e.g. rotating the phone) tears down and rebuilds this
            // composable. Skipping cleanup then keeps the same viewer session alive, so the
            // intro TTS, guided steps, and timers are NOT replayed on tilt.
            if (activity?.isChangingConfigurations == true) return@onDispose

            keyConceptTts.stop()
            viewModel.resetViewerSession()
            val trialItemId = TrialSessionStore.activeTrialItemId
            if (trialItemId != null) {
                val count = InteractionTracker.sessionInteractionCount.value
                TrialSessionStore.recordPendingSessionProgress(trialItemId, count)
                runBlocking {
                    viewModel.flushTrialSimClickCount(count)
                }
            }
            InteractionTracker.endSession()
        }
    }

    val handleBack = {
        val trialItemId = TrialSessionStore.activeTrialItemId
        if (trialItemId != null) {
            val count = InteractionTracker.sessionInteractionCount.value
            TrialSessionStore.recordPendingSessionProgress(trialItemId, count)
            runBlocking {
                viewModel.flushTrialSimClickCount(count)
            }
        }
        onBackClick()
    }

    val guideStepIdx = viewerSession.guideStepIdx
    val guideDismissed = viewerSession.guideDismissed
    val guideActive = viewerSession.guideUnlocked && guideSteps.isNotEmpty() && !guideDismissed

    // Read the current guided step aloud as it becomes active — step 0 is the simulation's intro
    // description, so the walkthrough now opens by narrating the intro itself. narratedStepIdx
    // lives in the ViewModel session, so rotation (which re-runs this effect) never re-speaks a step.
    LaunchedEffect(
        guideActive,
        guideStepIdx,
        voiceEnabled,
        viewerSession.narratedStepIdx,
        isForeground,
        decodedUrl,
    ) {
        if (!guideActive || !voiceEnabled || !isForeground) return@LaunchedEffect
        if (viewerSession.url != decodedUrl) return@LaunchedEffect
        if (viewerSession.narratedStepIdx == guideStepIdx) return@LaunchedEffect
        val step = guideSteps.getOrNull(guideStepIdx) ?: return@LaunchedEffect
        viewModel.markStepNarrated(decodedUrl, guideStepIdx)
        keyConceptTts.speakSimulationStep(
            text = step.instruction,
            simulationKey = decodedUrl,
            stepIndex = guideStepIdx,
        )
    }

    when {
        isInitPending -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SimulationHeader(
                title = decodedTitle,
                onBackClick = handleBack,
                voiceEnabled = voiceEnabled,
                onVoiceEnabledChange = { voiceEnabled = it },
                languageCode = languageCode,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                SimulationWebView(
                    url = decodedUrl,
                    onPageFinished = { onPageFinishedHandler() },
                    onInteractionTrackingReady = {
                        if (TrialSessionStore.activeTrialItemId != null) {
                            InteractionTracker.setSessionCountingEnabled(true)
                        }
                    },
                    onInteractionBudgetReported = { budget ->
                        InteractionTracker.reportInteractionBudget(budget)
                        if (TrialSessionStore.activeTrialItemId != null) {
                            viewModel.onHtmlInteractionBudget(budget)
                        }
                    },
                    onSimulationIntroReported = onSimulationIntroReported,
                    onSimulationFooterReported = onSimulationFooterReported,
                    onGuideStructureReported = { json ->
                        viewModel.storeHarvestJson(decodedUrl, json)
                    },
                    onGuideTap = { tapped ->
                        if (!guideActive) return@SimulationWebView
                        val current = guideSteps.getOrNull(guideStepIdx)
                        // Only tap-advance steps that are meant to be tapped. Sliders / text inputs
                        // advance via the Next button instead, so a stray tap doesn't skip them.
                        if (current?.autoAdvance == true &&
                            current.targetIndex != null &&
                            tapped == current.targetIndex &&
                            guideStepIdx < guideSteps.lastIndex
                        ) {
                            viewModel.setGuideStepIdx(decodedUrl, guideStepIdx + 1)
                        }
                    },
                    highlightStepIndex =
                        if (guideActive) guideSteps.getOrNull(guideStepIdx)?.targetIndex else null,
                )

                trialPrompt?.let { promptKind ->
                    val (title, message) = viewModel.trialPromptCopy(promptKind, languageCode)
                    SimulationTrialProceedOverlay(
                        visible = true,
                        kind = promptKind,
                        title = title,
                        message = message,
                        proceedLabel = TrialCopy.simProceedLabel(languageCode),
                        exploreLabel = TrialCopy.simKeepExploringLabel(languageCode),
                        onProceed = {
                            scope.launch {
                                viewModel.completeTrialSimProceed(sessionInteractions)
                                viewModel.clearTrialPrompt()
                                handleBack()
                            }
                        },
                        onKeepExploring = {
                            viewModel.dismissTrialPromptContinueExploring()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(2f),
                    )
                }

            }

            // Coach bar lives BELOW the simulation (its own row in the Column) rather than
            // floating over it, so it never covers controls like "Start race" when there's little
            // room under the sim.
            if (guideActive && trialPrompt == null) {
                val step = guideSteps[guideStepIdx]
                SimCoachOverlay(
                    instruction = step.instruction,
                    stepNumber = guideStepIdx + 1,
                    totalSteps = guideSteps.size,
                    isLast = guideStepIdx >= guideSteps.lastIndex,
                    // Sliders / text inputs can't be completed with a single tap, so they show a
                    // Next button (requireAction = false) instead of gating on a tap.
                    requireAction = step.targetIndex != null && step.autoAdvance,
                    onNext = {
                        if (guideStepIdx < guideSteps.lastIndex) {
                            viewModel.setGuideStepIdx(decodedUrl, guideStepIdx + 1)
                        } else {
                            viewModel.dismissGuide(decodedUrl)
                        }
                    },
                    onClose = { viewModel.dismissGuide(decodedUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    avatar = if (useNativeAvatar) {
                        {
                            AgentTutorAvatarBubble(
                                avatarSize = 44.dp,
                                ttsState = ttsState,
                                wordBoundaryIndex = wordBoundaryIndex,
                                isListening = false,
                                isThinking = false,
                                fullBody = false,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** Unwraps the hosting [Activity] from a Compose [Context] (may be a ContextWrapper). */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
