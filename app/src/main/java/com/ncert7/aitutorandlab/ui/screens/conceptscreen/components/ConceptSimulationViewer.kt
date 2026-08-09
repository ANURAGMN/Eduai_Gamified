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
import androidx.compose.runtime.mutableStateListOf
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
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakSimulationCoach
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakSimulationFooter
import com.ncert7.aitutorandlab.ui.screens.simulation_agent.components.speakSimulationReplay
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
    val ttsController: TextToSpeech = hiltViewModel()
    var coachAvatarCode by remember { mutableStateOf(sharedPrefs.getCoachAvatar()) }
    var showCoachSettings by remember { mutableStateOf(false) }
    var selectedVoiceLabel by remember { mutableStateOf("") }
    val (keyConceptTts, ttsState) = rememberSimulationKeyConceptTts(
        languageCode = languageCode,
        avatarCode = coachAvatarCode,
        ttsController = ttsController,
    )
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
            // Clicks only advance knowledge-bite progress — never the Explore/next overlay.
            viewModel.syncTrialSimClickCount(sessionInteractions)
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

    // Build marker — grep logcat for "CoachBuild" to confirm the running APK has the latest coach.
    // If this line is ABSENT from a sim session's log, the build is stale (incremental-compile cache).
    LaunchedEffect(Unit) {
        DebugLogger.debugLog("CoachBuild", "v5 coach: None hides tutor bubble; methodology only in settings (build 20260808v)")
    }

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
            if (viewerSession.url != decodedUrl || !guideFetchDone) {
                emptyList()
            } else if (guideDoc != null) {
                // A hosted guide always shows — even if the page never reported harvestable controls
                // (common for pure multiple-choice sims). Missing targets just become guidance steps
                // (no highlight); real targets get wired in once/if the harvest arrives.
                val doc = guideDoc!!
                SimGuideBuilder.finalizeSteps(
                    SimGuideBuilder.buildSteps(doc.steps, harvest ?: "{}"),
                    MAX_GUIDE_STEPS,
                )
            } else if (harvest != null) {
                // Heuristic sims (no hosted guide): lead with the page's own description, then controls.
                val heuristic = SimGuideBuilder.build(harvest)
                val steps = introText?.takeIf { it.isNotBlank() }
                    ?.let { listOf(SimGuideStep(it, null)) + heuristic }
                    ?: heuristic
                SimGuideBuilder.finalizeSteps(steps, MAX_GUIDE_STEPS)
            } else {
                emptyList()
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
        // A hosted guide unlocks even without a harvest (MCQ sims); heuristic guides still need it.
        val structureReady = viewerSession.harvestJson != null || guideDoc != null
        if (structureReady && guideFetchDone && introReady) {
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
                // Always notify VM: trial skips chapter progress but still records streak.
                viewModel.markSimulationCompleted(decodedConceptId)
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
            "3-min overlay timer — ${remaining / 1000}s remaining for $decodedUrl",
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
    val coachActive = viewerSession.guideUnlocked && guideSteps.isNotEmpty() && !guideDismissed

    // Authored coaching for this sim (mission + why-wrong / stuck / well-done / deviation banks +
    // thresholds). Every sim gets a usable default even when its guide ships no `coach` block.
    val coach = remember(guideDoc) { guideDoc?.coach ?: SimCoachData.default(null) }
    val coachMission = coach.mission?.takeIf { it.isNotBlank() }
        ?: decodedTitle.takeIf { it.isNotBlank() }

    // Selected coaching style (v1 scripted / v2 adaptive / v3 fully guided), persisted so the choice
    // sticks across sims for side-by-side comparison.
    var coachMode by remember { mutableStateOf(SimCoachMode.fromKey(sharedPrefs.getSimCoachMode())) }

    // ---- Coach state (shared by phases; reset per-URL and on a style switch) ----
    var coachMessage by remember(decodedUrl) { mutableStateOf<String?>(null) }
    var coachTone by remember(decodedUrl) { mutableStateOf(CoachTone.NEUTRAL) }
    var coachSeq by remember(decodedUrl) { mutableStateOf(0) }
    var wrongCursor by remember(decodedUrl) { mutableStateOf(0) }
    var correctCursor by remember(decodedUrl) { mutableStateOf(0) }
    var stuckCursor by remember(decodedUrl) { mutableStateOf(0) }
    var deviateCursor by remember(decodedUrl) { mutableStateOf(0) }
    // v3 (guided) only: which control to suggest next in practice, and the correct-round tally.
    var suggestIndex by remember(decodedUrl) { mutableStateOf(0) }
    var correctRounds by remember(decodedUrl) { mutableStateOf(0) }
    // True once this sim has produced ANY right/wrong verdict — i.e. it's a scoring/puzzle sim
    // rather than a pure demonstration. v3 uses this to decide whether to run practice rounds after
    // teaching (scoring sims) or simply conclude the lesson (demonstration sims).
    var sawVerdict by remember(decodedUrl) { mutableStateOf(false) }

    val postCoach: (String?, CoachTone) -> Unit = { text, tone ->
        if (!text.isNullOrBlank()) {
            coachMessage = text
            coachTone = tone
            coachSeq += 1
        }
    }

    // The steps shown in the scripted (Phase A) part, per style:
    //  v1 → every step, including the "explore more" closing (fully scripted end-to-end).
    //  v2 → just the first `introSteps` (a brief intro), then hands off to adaptive free-play.
    //  v3 → the full lesson (every step except the closing), taught end-to-end with per-step gating
    //       and auto-narrated explanations; afterwards it keeps going (practice rounds, or guided
    //       exploration of every remaining element) until everything is covered.
    val introStepCount = remember(guideSteps, coach) {
        if (guideSteps.isEmpty()) 0 else coach.introSteps.coerceIn(0, guideSteps.size)
    }
    val scriptedSteps = remember(guideSteps, coachMode, introStepCount) {
        when (coachMode) {
            SimCoachMode.SCRIPTED -> guideSteps
            SimCoachMode.ADAPTIVE -> guideSteps.take(introStepCount)
            SimCoachMode.GUIDED -> if (guideSteps.size > 1) guideSteps.dropLast(1) else guideSteps
            // v4 has no scripted walkthrough — the page-side loop owns everything.
            SimCoachMode.ONE_CLOCK -> emptyList()
        }
    }
    // The tappable controls v3 cycles through, round after round, during practice.
    val suggestSteps = remember(guideSteps) {
        guideSteps.filter { it.targetIndex != null && it.autoAdvance }
    }

    // EVERY interactive control the page exposed — so v3 can keep going past the scripted steps and
    // guide the learner through all the remaining elements (each solution, then the action to test
    // it), not just the ones the guide happened to script.
    val allControls = remember(viewerSession.harvestJson) {
        viewerSession.harvestJson?.let { SimGuideBuilder.harvestedControls(it) } ?: emptyList()
    }
    val exploreOptions = remember(allControls) { allControls.filter { !it.isAction && !it.needsNext } }
    val primaryAction = remember(allControls) { allControls.firstOrNull { it.isAction } }
    // v3 demonstration exploration: which option is picked and awaiting its action (null = pick one),
    // plus the options already fully covered (snapshot list so highlight/prompts stay reactive).
    var exploreSelectedIdx by remember(decodedUrl) { mutableStateOf<Int?>(null) }
    // True for a beat right after an action/verdict fires, so the result (and any feedback line)
    // stays on screen — highlight + next prompt are suppressed instead of the view snapping away.
    var coachObserving by remember(decodedUrl) { mutableStateOf(false) }
    val exploredOptions = remember(decodedUrl) { mutableStateListOf<Int>() }
    val currentExploreOption =
        if (exploreSelectedIdx != null) exploreOptions.firstOrNull { it.index == exploreSelectedIdx }
        else exploreOptions.firstOrNull { it.index !in exploredOptions }

    // v3 practice (scoring/puzzle sims — most of Math): authored, repeatable rounds that run after
    // the lesson. Each round walks these steps (highlighting + gating each control), then the
    // learner's Check/Submit yields a verdict that ends the round.
    val practiceDoc = guideDoc?.practice
    val practiceSteps = remember(practiceDoc, viewerSession.harvestJson) {
        // Works even before/without a harvest — unmatched targets become guidance (no highlight).
        if (practiceDoc == null) emptyList()
        else SimGuideBuilder.buildSteps(practiceDoc.steps, viewerSession.harvestJson ?: "{}")
    }
    val hasPractice = practiceSteps.isNotEmpty()
    var practiceStepIdx by remember(decodedUrl) { mutableStateOf(0) }
    var practiceRound by remember(decodedUrl) { mutableStateOf(0) }
    // Monotonic nonce so Replay / 10-second nudges re-speak even the same line.
    var ttsNonce by remember(decodedUrl) { mutableStateOf(0) }

    // ---- Maths solver (v3): the live on-screen problem, solved → number-specific feedback ----
    var mathSolution by remember(decodedUrl) { mutableStateOf<MathCoachSolver.Solution?>(null) }
    var mathAnswerStep by remember(decodedUrl) { mutableStateOf<Int?>(null) }  // correct option's control index
    var mathReteachStep by remember(decodedUrl) { mutableStateOf<Int?>(null) } // highlight it during a wrong-answer reteach
    var mathMoveActive by remember(decodedUrl) { mutableStateOf(false) }       // build/calc: highlight the next move
    // v4 one-clock coach: the current line the page-side loop wants displayed (passive mirror).
    var v4Line by remember(decodedUrl) { mutableStateOf("") }
    // Bumped by the coach card's "Explain" chip to trigger the page-side detail panel.
    var explainSignal by remember(decodedUrl) { mutableStateOf(0) }
    var explainDismissSignal by remember(decodedUrl) { mutableStateOf(0) }
    var explainOpen by remember(decodedUrl) { mutableStateOf(false) }
    // Student-switchable hint model (persisted); `hintSignal` is bumped by the Hint / Show-answer chip.
    var hintMode by remember { mutableStateOf(sharedPrefs.getHintMode()) }
    var hintSignal by remember(decodedUrl) { mutableStateOf(0) }
    // Identity of the round currently on screen (prompt with the build "Current/Clicks" tail stripped,
    // so tapping place-value buttons doesn't count as a new round). Used to drop a stale why-line when
    // the sim advances to the next round while a verdict's feedback is still up.
    var lastRoundKey by remember(decodedUrl) { mutableStateOf("") }
    val onMathProblem: (String) -> Unit = { json ->
        runCatching {
            // v4 owns the whole coach via its own loop. The V3 math path below calls
            // keyConceptTts.stop() on every round change — which would cut the V4 voice mid-line
            // ("voice stopping in between"). Skip it entirely in v4.
            if (coachMode == SimCoachMode.ONE_CLOCK) return@runCatching
            val o = org.json.JSONObject(json)
            val prompt = o.optString("prompt")
            val current = o.optString("current").replace(",", "").toLongOrNull()
            // New round? Refresh the coach bar so the previous round's "why" doesn't linger while the
            // sim shows the next problem (seen on Compare: round 1's line held over round 2).
            val roundKey = prompt.replace(Regex("(?i)current.*"), "").trim()
            if (roundKey.isNotBlank() && roundKey != lastRoundKey) {
                lastRoundKey = roundKey
                coachObserving = false
                mathReteachStep = null
                coachMessage = null
                // Cut any voice still narrating the PREVIOUS round — the slow TTS queue was the main
                // "voice out of sync" source (it kept talking about the last problem while the screen,
                // glow, and text had all moved on).
                keyConceptTts.stop()
            }
            val optsArr = o.optJSONArray("options")
            val opts = buildList {
                if (optsArr != null) for (i in 0 until optsArr.length()) {
                    val oo = optsArr.optJSONObject(i) ?: continue
                    val label = oo.optString("label").trim()
                    if (label.isEmpty()) continue
                    add(Triple(label, oo.optString("value").ifBlank { null }, oo.optString("step").toIntOrNull()))
                }
            }
            val sol = MathCoachSolver.solve(prompt, opts.map { it.first })
            if (sol != null) {
                mathSolution = sol
                mathMoveActive = false
                mathAnswerStep = opts.firstOrNull { it.first == sol.correctOptionLabel }?.third
                    ?: opts.firstOrNull { o2 ->
                        val c = sol.correctOptionLabel
                        c != null && (o2.first.contains(c) || o2.second == c)
                    }?.third
                DebugLogger.debugLog(
                    "MathCoach",
                    "solved prompt=${prompt.take(60)} answer=${sol.correctOptionLabel} step=$mathAnswerStep opts=${opts.size}",
                )
            } else if (current != null && (prompt.contains("build", true) || prompt.contains("target", true))) {
                // Stateful build sim — mirror the HTML sandbox's continuous glow: while building, glow
                // the biggest place-value button that still fits; the MOMENT current == target, glow
                // "Lock Build"; if the child overshoots, glow "Reset". (Lock Build / Reset are page
                // controls, not math options, so resolve them from the harvested control list.)
                // Parse the target from "Target: N" — NOT max(all numbers): the prompt also contains
                // "Current: N", which exceeds the target on an overshoot and would break Reset detection.
                val target = Regex("""target[:\s]*([\d,]+)""", RegexOption.IGNORE_CASE)
                    .find(prompt)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
                    ?: MathCoachSolver.numbersIn(prompt).maxOrNull()
                val btnVals = opts.mapNotNull { it.second?.toLongOrNull() }
                if (target != null && btnVals.isNotEmpty()) {
                    val move = MathCoachSolver.solveBuild(target, current, btnVals)
                    val submitIdx = allControls.firstOrNull {
                        val l = it.label.lowercase()
                        l.contains("lock") || l.contains("submit") || l.contains("check")
                    }?.index
                    val resetIdx = allControls.firstOrNull { it.label.contains("reset", true) }?.index
                    mathSolution = null
                    mathMoveActive = true
                    mathAnswerStep = when {
                        move.controlValue != null -> opts.firstOrNull { it.second == move.controlValue }?.third
                        current == target -> submitIdx   // build complete → glow Lock Build
                        current > target -> resetIdx     // overshot → glow Reset
                        else -> null
                    }
                    DebugLogger.debugLog(
                        "MathCoach",
                        "build target=$target current=$current move=${move.controlValue} step=$mathAnswerStep",
                    )
                } else {
                    mathSolution = null; mathAnswerStep = null; mathMoveActive = false
                }
            } else {
                mathSolution = null; mathAnswerStep = null; mathMoveActive = false
                DebugLogger.debugLog(
                    "MathCoach",
                    "no solution prompt=${prompt.take(60)} opts=${opts.map { it.first }}",
                )
            }
        }
        Unit
    }

    val coachAdaptive = viewerSession.coachAdaptive       // "in Phase B" (v2 adaptive / v3 guided)
    val coachEasedOff = viewerSession.coachEasedOff        // coach finished / concluded
    val inScripted = coachActive && !coachAdaptive && !coachEasedOff && guideStepIdx < scriptedSteps.size
    val inPhaseB = coachActive && coachAdaptive && !coachEasedOff
    val phaseBDone = coachActive && coachEasedOff
    // Hand the glow to the continuous page-side loop ONLY in Phase B (practice) AND only when the
    // live problem is actually solvable. Teach must stay on the push so the scripted step's targetIndex
    // reaches the DOM (e.g. glow "Check Sign", not the answer); method-only/pre-solve rounds also fall
    // back to the push instead of going dark. `coachObserving` pauses it during a verdict's feedback.
    val mathSolvable = mathSolution != null || mathMoveActive
    val coachLoopActive = inPhaseB && coachMode == SimCoachMode.GUIDED &&
        decodedUrl.contains("math_", ignoreCase = true) && !coachObserving && mathSolvable

    // v4 one-clock coach: the page-side loop owns everything; Kotlin only mirrors text + speaks. It's
    // active whenever the coach is unlocked and not dismissed (independent of the V3 phase machine).
    val isV4 = coachMode == SimCoachMode.ONE_CLOCK
    val coachV4Active = isV4 && viewerSession.guideUnlocked && !viewerSession.guideDismissed

    // Reteach (red answer glow) auto-expires after ~4s so it can't persist indefinitely if the learner
    // neither taps Continue nor triggers a round change.
    LaunchedEffect(mathReteachStep, decodedUrl) {
        if (mathReteachStep == null) return@LaunchedEffect
        delay(4_000)
        mathReteachStep = null
    }
    val guidedSuggestTarget = when {
        !(inPhaseB && coachMode == SimCoachMode.GUIDED) -> null
        // Pause the highlight while a result/feedback line is on screen (dip result, verdict).
        coachObserving -> null
        // Scoring sim → practice rounds: highlight the current practice step's control.
        sawVerdict -> when {
            hasPractice -> practiceSteps.getOrNull(practiceStepIdx)?.targetIndex
            suggestSteps.isNotEmpty() -> suggestSteps[suggestIndex % suggestSteps.size].targetIndex
            else -> null
        }
        // Demonstration sim → exploration: highlight the option to pick, then its action to test.
        else -> if (exploreSelectedIdx == null) currentExploreOption?.index else primaryAction?.index
    }

    // Advances the scripted/teach phase. At the end of the walkthrough:
    //  v1 → done (dismiss).
    //  v2 → hand to adaptive free-play.
    //  v3 → hand to Phase B: a scoring sim runs practice rounds; a demonstration sim runs guided
    //       exploration over every remaining element, concluding only once all are covered.
    val enterPhaseB: () -> Unit = {
        viewModel.enterAdaptiveCoach(
            decodedUrl,
            System.currentTimeMillis(),
            InteractionTracker.sessionInteractionCount.value,
        )
    }
    val concludeLesson: () -> Unit = {
        val doneMsg = practiceDoc?.done?.takeIf { hasPractice && sawVerdict } ?: coach.doneMessage
        postCoach(doneMsg, CoachTone.CORRECT)
        viewModel.easeOffCoach(decodedUrl)
        // Explore / next overlay is time-gated only (see TRIAL_OVERLAY_MS) — do not show on coach end.
    }
    val advanceScripted: () -> Unit = {
        if (guideStepIdx < scriptedSteps.size - 1) {
            viewModel.setGuideStepIdx(decodedUrl, guideStepIdx + 1)
        } else when (coachMode) {
            SimCoachMode.SCRIPTED -> viewModel.dismissGuide(decodedUrl)
            // v2 adaptive and v3 guided both continue past the scripted steps — v3 into guided
            // exploration that keeps going until every element has been covered.
            SimCoachMode.ADAPTIVE, SimCoachMode.GUIDED -> enterPhaseB()
            SimCoachMode.ONE_CLOCK -> Unit
        }
    }

    // Switching style restarts the coach from the top so the new style is seen from the beginning.
    val onCoachModeChange: (SimCoachMode) -> Unit = { mode ->
        if (mode != coachMode) {
            coachMode = mode
            sharedPrefs.setSimCoachMode(mode.name)
            coachMessage = null
            coachTone = CoachTone.NEUTRAL
            wrongCursor = 0; correctCursor = 0; stuckCursor = 0; deviateCursor = 0
            suggestIndex = 0; correctRounds = 0; sawVerdict = false
            exploreSelectedIdx = null; coachObserving = false
            practiceStepIdx = 0; practiceRound = 0
            exploredOptions.clear()
            keyConceptTts.stop()
            viewModel.restartCoach(decodedUrl)
        }
    }

    // Speaks a line on demand (Replay / nudges), bypassing dedupe via the nonce.
    val speakLine: (String) -> Unit = { text ->
        if (text.isNotBlank() && voiceEnabled && isForeground) {
            ttsNonce += 1
            keyConceptTts.speakSimulationReplay(text, decodedUrl, ttsNonce)
        }
    }
    // The line currently on screen — teach shows the step; Phase B shows the coach message.
    val currentCoachText: String? =
        if (inScripted) scriptedSteps.getOrNull(guideStepIdx)?.instruction else coachMessage
    val onReplay: () -> Unit = { currentCoachText?.let { speakLine(it) } }

    // Back: previous scripted step (teach) or previous practice step.
    val backToScripted = inScripted && guideStepIdx > 0
    val backToPractice = inPhaseB && coachMode == SimCoachMode.GUIDED && sawVerdict &&
        hasPractice && practiceStepIdx > 0 && !coachObserving
    val onBack: (() -> Unit)? = when {
        backToScripted -> ({ viewModel.setGuideStepIdx(decodedUrl, guideStepIdx - 1) })
        backToPractice -> ({ practiceStepIdx -= 1 })
        else -> null
    }

    // Continue: the learner confirms they're ready to move on. Nothing auto-advances without this.
    val continueDismissObserve = inPhaseB && coachObserving
    val continueAdvancePractice = inPhaseB && coachMode == SimCoachMode.GUIDED && sawVerdict &&
        hasPractice && !coachObserving && practiceStepIdx < practiceSteps.lastIndex &&
        practiceSteps.getOrNull(practiceStepIdx)?.targetIndex == null
    val onPhaseBContinue: (() -> Unit)? = when {
        continueDismissObserve -> ({ coachObserving = false; mathReteachStep = null })
        continueAdvancePractice -> ({ practiceStepIdx += 1 })
        else -> null
    }

    // For v2/v3: if there are no scripted steps to show, or a rotation left us past them, drop
    // straight into Phase B. (v1 never enters Phase B — it dismisses at the end instead.)
    LaunchedEffect(coachActive, coachAdaptive, coachEasedOff, coachMode, scriptedSteps.size, guideStepIdx, decodedUrl) {
        if (viewerSession.url != decodedUrl) return@LaunchedEffect
        if (coachActive && !coachAdaptive && !coachEasedOff && guideStepIdx >= scriptedSteps.size) {
            when (coachMode) {
                SimCoachMode.SCRIPTED -> Unit
                SimCoachMode.ADAPTIVE, SimCoachMode.GUIDED -> enterPhaseB()
                SimCoachMode.ONE_CLOCK -> Unit
            }
        }
    }

    // Narrate each scripted step as it becomes active. narratedStepIdx lives in the ViewModel
    // session, so rotation (which re-runs this effect) never re-speaks a step.
    LaunchedEffect(
        inScripted,
        guideStepIdx,
        voiceEnabled,
        viewerSession.narratedStepIdx,
        isForeground,
        decodedUrl,
    ) {
        if (!inScripted || !voiceEnabled || !isForeground) return@LaunchedEffect
        if (viewerSession.url != decodedUrl) return@LaunchedEffect
        if (viewerSession.narratedStepIdx == guideStepIdx) return@LaunchedEffect
        val step = scriptedSteps.getOrNull(guideStepIdx) ?: return@LaunchedEffect
        viewModel.markStepNarrated(decodedUrl, guideStepIdx)
        keyConceptTts.speakSimulationStep(
            text = step.instruction,
            simulationKey = decodedUrl,
            stepIndex = guideStepIdx,
        )
    }

    // React to right/wrong verdicts across BOTH the teach and practice phases (v1 scripted stays
    // silent). Wrong → explain why; correct → celebrate. Recording sawVerdict tells v3 whether this
    // is a scoring sim (→ practice rounds after teaching) or a demonstration (→ conclude). During
    // v3 practice a correct verdict also tallies a round and moves the suggestion forward.
    LaunchedEffect(coachActive, coachMode, decodedUrl) {
        if (!coachActive) return@LaunchedEffect
        InteractionTracker.verdicts.collect { correct ->
            sawVerdict = true
            if (coachMode == SimCoachMode.SCRIPTED) return@collect
            // v4 one-clock: the page-side loop already speaks/shows the result from the sim's own
            // feedback on its tick. Kotlin must NOT also post a verdict line (that's a second voice).
            if (coachMode == SimCoachMode.ONE_CLOCK) return@collect
            // Ignore stray repeat verdicts while a feedback line is already showing — otherwise a
            // double-tap on Check double-counts the round (bug caught in the coach sandbox).
            if (coachObserving) return@collect
            val inPractice = viewModel.viewerSession.value.let { it.coachAdaptive && !it.coachEasedOff }

            // v3 with an authored practice sequence: a verdict ends a round. Correct → say the
            // observation/inference and start the next round; wrong → say why + let them retry.
            if (coachMode == SimCoachMode.GUIDED && inPractice && hasPractice) {
                // Prefer the SOLVER's number-specific feedback when the live problem was solvable;
                // otherwise fall back to the authored method hints (never guesses).
                val sol = mathSolution
                if (correct) {
                    val round = practiceRound + 1
                    practiceRound = round
                    correctRounds = round
                    val line = sol?.whyCorrect
                        ?: practiceDoc?.onCorrect?.cycle(round - 1)
                        ?: coach.correctLine(correctCursor)
                    postCoach(line, CoachTone.CORRECT)
                    correctCursor += 1
                } else {
                    val line = sol?.whyWrong
                        ?: practiceDoc?.onWrong?.cycle(wrongCursor)
                        ?: coach.wrongLine(wrongCursor)
                    postCoach(line, CoachTone.WRONG)
                    wrongCursor += 1
                    // Reteach: glow the correct option while the "why" is on screen — but NOT for the
                    // stateful build sim. Its correct action is dynamic (next place-value button, or
                    // Reset/Lock as the total changes); freezing a stale button here traps the learner
                    // tapping the frozen glow and overshooting endlessly. There the live solver glow
                    // keeps tracking `current` instead.
                    mathReteachStep = if (mathMoveActive) null else mathAnswerStep
                }
                // Hold on the result, then the practice-flow effect restarts the round's first step.
                practiceStepIdx = 0
                coachObserving = true
                return@collect
            }

            if (correct) {
                postCoach(coach.correctLine(correctCursor), CoachTone.CORRECT)
                correctCursor += 1
                if (coachMode == SimCoachMode.GUIDED) {
                    correctRounds += 1
                    if (inPractice) suggestIndex += 1
                }
            } else {
                postCoach(coach.wrongLine(wrongCursor), CoachTone.WRONG)
                wrongCursor += 1
            }
        }
    }

    // v3 practice flow — walk the authored round steps like a tutor: narrate each step and gate on
    // the learner's input. Guidance steps advance on Continue, actionable ones on the tap; nothing
    // advances on its own. Paused while `coachObserving` (a verdict's feedback is on screen).
    // NOTE: while the continuous loop owns the glow (`coachLoopActive`), we do NOT run this generic
    // step narration. Its authored lines ("tap Reset", "tap the biggest button") post on their own
    // timer and drift out of step with the live glow/sim — that was the "text says something else"
    // desync. During the live loop the glow is the guide and only the (current) verdict feedback speaks.
    val practiceActive = inPhaseB && coachMode == SimCoachMode.GUIDED && sawVerdict && hasPractice &&
        !coachLoopActive
    LaunchedEffect(practiceActive, practiceStepIdx, practiceRound, coachObserving, decodedUrl) {
        if (!practiceActive || coachObserving) return@LaunchedEffect
        val step = practiceSteps.getOrNull(practiceStepIdx) ?: return@LaunchedEffect
        val idxAtStart = practiceStepIdx
        postCoach(step.instruction, CoachTone.NEUTRAL)
        // Pure-guidance step with a next step: wait for Continue; nudge after 10s. Never auto-advance.
        if (step.targetIndex == null && practiceStepIdx < practiceSteps.lastIndex) {
            delay(10_000)
            if (practiceStepIdx == idxAtStart && !coachObserving &&
                viewModel.viewerSession.value.let { it.coachAdaptive && !it.coachEasedOff }
            ) {
                postCoach("Ready? Tap Continue when you've got it.", CoachTone.NEUTRAL)
            }
        }
    }

    // v3 teach flow — a teaching/observation step waits for the learner to tap Next (never
    // auto-advances); after 10s of no input it reminds them they can continue. An actionable step
    // waits for the learner to do it; if they stall for 10s, it offers a spoken hint.
    LaunchedEffect(inScripted, coachMode, guideStepIdx, decodedUrl) {
        if (!inScripted || coachMode != SimCoachMode.GUIDED) return@LaunchedEffect
        val step = scriptedSteps.getOrNull(guideStepIdx) ?: return@LaunchedEffect
        if (step.targetIndex == null) {
            // Teaching/observation beat — wait for the learner to tap Next; never auto-advance.
            // After 10s of no input, remind them they can continue.
            delay(10_000)
            val s = viewModel.viewerSession.value
            if (s.url == decodedUrl && s.guideStepIdx == guideStepIdx &&
                !s.coachAdaptive && !s.coachEasedOff && isForeground
            ) {
                speakLine("Ready to move on? Tap Next to continue.")
            }
        } else {
            delay(10_000)
            val s = viewModel.viewerSession.value
            if (s.url == decodedUrl && s.guideStepIdx == guideStepIdx && isForeground) {
                postCoach(coach.stuckLine(stuckCursor), CoachTone.STUCK)
                stuckCursor += 1
            }
        }
    }

    // Phase B — nudge on a lull, and finish when done. v2 eases off after enough interactions/time;
    // v3 stays fully guided and only finishes when the rounds are complete or every control has
    // been explored — then it plays the done line and goes passive.
    LaunchedEffect(inPhaseB, coachMode, decodedUrl) {
        if (!inPhaseB) return@LaunchedEffect
        var lastActivityMs = System.currentTimeMillis()
        var lastInteractionCount = InteractionTracker.sessionInteractionCount.value
        while (true) {
            delay(2_000)
            if (!isForeground) {
                lastActivityMs = System.currentTimeMillis()
                continue
            }
            val now = System.currentTimeMillis()
            val liveInteractions = InteractionTracker.sessionInteractionCount.value
            if (liveInteractions != lastInteractionCount) {
                lastInteractionCount = liveInteractions
                lastActivityMs = now
            }
            val session = viewModel.viewerSession.value
            val elapsedMs = now - session.adaptiveStartMs
            val interactionsDone = liveInteractions - session.adaptiveStartInteractions

            // Live "next option to explore" (exploredOptions mutates as the learner taps).
            val selIdx = exploreSelectedIdx
            val liveOption = if (selIdx != null) exploreOptions.firstOrNull { it.index == selIdx }
                else exploreOptions.firstOrNull { it.index !in exploredOptions }
            val safetyCap = elapsedMs >= coach.easeOffAfterSeconds * 2 * 1000L
            val practiceTarget = practiceDoc?.rounds ?: coach.roundsToComplete
            val finished = when {
                coachMode != SimCoachMode.GUIDED ->
                    elapsedMs >= coach.easeOffAfterSeconds * 1000L ||
                        interactionsDone >= coach.easeOffAfterInteractions
                // Scoring sim → done after enough rounds. Wait out the last feedback line first.
                sawVerdict ->
                    (practiceRound >= practiceTarget && !coachObserving) ||
                        (!hasPractice && correctRounds >= coach.roundsToComplete) ||
                        safetyCap
                // A solvable MCQ math problem is on screen but not yet attempted. Its answer options
                // arrive via reportMathProblem, NOT the harvested control list, so exploreOptions is
                // empty here — do NOT conclude on that. Wait for the child to answer (the glow +
                // worked-feedback path). Only the safety cap ends it. This was the bug: Compare/other
                // no-practice MCQ sims eased off ~2s after teaching, before any answer.
                mathAnswerStep != null -> safetyCap
                // Startup-race guard: on a math sim the injected JS can take a beat (~1-2s) to send the
                // first reportMathProblem. Until then mathAnswerStep is null and exploreOptions may be
                // empty — hold off concluding for a short grace so a slow first report can't be raced.
                decodedUrl.contains("math_", true) && elapsedMs < 6_000L -> safetyCap
                // Exploration finishes only when every element has been covered (or safety net).
                // Wait out the last observation so its inference isn't cut off by the wrap-up.
                else -> exploreOptions.isEmpty() ||
                    (exploreOptions.all { it.index in exploredOptions } && selIdx == null && !coachObserving) ||
                    safetyCap
            }
            if (finished) {
                concludeLesson()
                break
            }

            // Skip lull nudges entirely while the live loop owns the round — the glow is already
            // pointing at the right control, so a timer-driven "you're stuck, tap X" line only adds a
            // fourth, out-of-step voice. (Finish detection above still runs.)
            if (now - lastActivityMs >= coach.stuckAfterSeconds * 1000L && !coachObserving && !coachLoopActive) {
                when {
                    // Practice: re-point at the current round step.
                    coachMode == SimCoachMode.GUIDED && sawVerdict && hasPractice ->
                        practiceSteps.getOrNull(practiceStepIdx)?.let {
                            postCoach(it.instruction, CoachTone.STUCK)
                        }
                    coachMode == SimCoachMode.GUIDED && !sawVerdict && liveOption != null ->
                        if (exploreSelectedIdx == null) {
                            postCoach("Try tapping “${liveOption.label}”.", CoachTone.STUCK)
                        } else if (primaryAction != null) {
                            postCoach("Tap “${primaryAction.label}” to test “${liveOption.label}”.", CoachTone.STUCK)
                        } else Unit
                    coachMode == SimCoachMode.GUIDED && suggestSteps.isNotEmpty() ->
                        postCoach(suggestSteps[suggestIndex % suggestSteps.size].instruction, CoachTone.STUCK)
                    else -> {
                        postCoach(coach.stuckLine(stuckCursor), CoachTone.STUCK)
                        stuckCursor += 1
                    }
                }
                lastActivityMs = now
            }
        }
    }

    // v3 exploration — whenever we return to "pick an option" and a new (un-explored) option comes
    // up, prompt for it by name. The small delay lets the previous observation land before the next
    // instruction (which would otherwise flush it), and the effect keys cancel it if state moves on.
    LaunchedEffect(
        inPhaseB,
        coachMode,
        sawVerdict,
        exploreSelectedIdx,
        coachObserving,
        currentExploreOption?.index,
        decodedUrl,
    ) {
        if (!(inPhaseB && coachMode == SimCoachMode.GUIDED && !sawVerdict)) return@LaunchedEffect
        if (exploreSelectedIdx != null || coachObserving) return@LaunchedEffect
        val opt = currentExploreOption ?: return@LaunchedEffect
        delay(700)
        val s = viewModel.viewerSession.value
        if (s.url != decodedUrl || !s.coachAdaptive || s.coachEasedOff) return@LaunchedEffect
        val tail =
            if (primaryAction != null) ", then tap “${primaryAction.label}” to test it."
            else " and watch what changes."
        postCoach("Now try “${opt.label}” — tap it$tail", CoachTone.NEUTRAL)
    }

    // After an action/verdict the result stays on screen with a Continue button — we do NOT
    // auto-advance. If the learner idles for 10s, gently remind them (spoken, so it doesn't flush
    // the result line they're reading).
    LaunchedEffect(coachObserving, decodedUrl) {
        if (!coachObserving) return@LaunchedEffect
        delay(10_000)
        if (coachObserving && isForeground) {
            speakLine("Ready to continue? Tap the Continue button.")
        }
    }

    // Speak each new coach line (keyed on seq so repeated hints still play). Never speak once the
    // guide has been cancelled/closed.
    LaunchedEffect(coachSeq) {
        if (coachSeq == 0) return@LaunchedEffect
        if (viewerSession.guideDismissed) return@LaunchedEffect
        val msg = coachMessage ?: return@LaunchedEffect
        if (voiceEnabled && isForeground) {
            keyConceptTts.speakSimulationCoach(msg, decodedUrl, coachSeq)
        }
    }

    // The moment the guide is cancelled, silence any in-flight narration.
    LaunchedEffect(guideDismissed) {
        if (guideDismissed) keyConceptTts.stop()
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
                coachMode = coachMode,
                onCoachModeChange = onCoachModeChange,
                onSettingsClick = { showCoachSettings = true },
            )

            if (showCoachSettings) {
                CoachSettingsSheet(
                    hintMode = hintMode,
                    onHintMode = { id -> hintMode = id; sharedPrefs.setHintMode(id) },
                    voiceEnabled = voiceEnabled,
                    onVoiceEnabled = { voiceEnabled = it },
                    speed = ttsState.speechRate,
                    onSpeed = { ttsController.setSpeechRate(it) },
                    voiceOptions = ttsState.availableVoices.take(8).mapIndexed { i, _ -> "Voice ${i + 1}" },
                    selectedVoice = selectedVoiceLabel,
                    onVoiceSelect = { label ->
                        val idx = (label.removePrefix("Voice ").trim().toIntOrNull() ?: 1) - 1
                        ttsState.availableVoices.getOrNull(idx)?.let { ttsController.setVoice(it) }
                        selectedVoiceLabel = label
                    },
                    avatarOptions = listOf("Boy", "Girl", "None"),
                    selectedAvatar = when (coachAvatarCode) {
                        "girl" -> "Girl"
                        "disable" -> "None"
                        else -> "Boy"
                    },
                    onAvatarSelect = { name ->
                        val code = when (name) {
                            "Girl" -> "girl"
                            "None" -> "disable"
                            else -> "boy"
                        }
                        coachAvatarCode = code
                        sharedPrefs.setCoachAvatar(code)
                        ttsController.switchCharacter(code) // switch the actual tutor character
                    },
                    onDismiss = { showCoachSettings = false },
                )
            }

            // Tutor avatar (used by the floating coach bubble and the below-sim coach bars).
            // "None" (disable) hides the tutor so the bubble falls back to the bulb glyph.
            val coachAvatar: (@Composable () -> Unit)? = if (useNativeAvatar && coachAvatarCode != "disable") {
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
            }

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
                        when {
                            // Scripted phase: the suggested control advances the walkthrough. In v3
                            // an off-path tap gets a gentle redirect instead of being ignored.
                            inScripted -> {
                                val current = scriptedSteps.getOrNull(guideStepIdx)
                                val onTarget = current?.autoAdvance == true &&
                                    current.targetIndex != null &&
                                    tapped == current.targetIndex
                                if (onTarget) {
                                    // Any option covered during the lesson is skipped in exploration.
                                    if (exploreOptions.any { it.index == tapped } && tapped !in exploredOptions) {
                                        exploredOptions.add(tapped)
                                    }
                                    advanceScripted()
                                } else if (coachMode == SimCoachMode.GUIDED &&
                                    current?.targetIndex != null
                                ) {
                                    postCoach(coach.deviateLine(deviateCursor), CoachTone.NEUTRAL)
                                    deviateCursor += 1
                                }
                            }
                            // v3 authored practice (scoring sim): walk the round's steps — tapping the
                            // highlighted control advances; the Check/Submit at the end yields a
                            // verdict (handled by the verdict collector) that ends the round.
                            inPhaseB && coachMode == SimCoachMode.GUIDED && sawVerdict && hasPractice -> {
                                if (!coachObserving) {
                                    val step = practiceSteps.getOrNull(practiceStepIdx)
                                    val target = step?.targetIndex
                                    if (target != null && target == tapped) {
                                        if (practiceStepIdx < practiceSteps.lastIndex) practiceStepIdx += 1
                                        // last step (Check/Submit): stay; the verdict ends the round.
                                    } else if (target != null) {
                                        postCoach(coach.deviateLine(deviateCursor), CoachTone.NEUTRAL)
                                        deviateCursor += 1
                                    }
                                }
                            }
                            // Fallback practice with no authored sequence: cycle the guide's controls.
                            inPhaseB && coachMode == SimCoachMode.GUIDED && sawVerdict &&
                                suggestSteps.isNotEmpty() -> {
                                val suggested = suggestSteps[suggestIndex % suggestSteps.size].targetIndex
                                if (suggested != null && tapped == suggested) {
                                    suggestIndex += 1
                                    postCoach(coach.correctLine(correctCursor), CoachTone.CORRECT)
                                    correctCursor += 1
                                } else {
                                    postCoach(coach.deviateLine(deviateCursor), CoachTone.NEUTRAL)
                                    deviateCursor += 1
                                }
                            }
                            // v3 exploration (demonstration sim): cover every element — pick an
                            // option, then trigger the action to test it, one element at a time.
                            inPhaseB && coachMode == SimCoachMode.GUIDED -> {
                                val opt = currentExploreOption
                                if (opt != null) {
                                    if (exploreSelectedIdx == null) {
                                        // Any un-covered option the learner picks is fine — not just
                                        // the highlighted one.
                                        val picked = exploreOptions.firstOrNull {
                                            it.index == tapped && it.index !in exploredOptions
                                        }
                                        when {
                                            picked != null ->
                                                if (primaryAction != null) {
                                                    exploreSelectedIdx = picked.index
                                                    postCoach(
                                                        "Good — now tap “${primaryAction.label}” to test it.",
                                                        CoachTone.NEUTRAL,
                                                    )
                                                } else {
                                                    exploredOptions.add(picked.index) // no action → done with this one
                                                }
                                            tapped == primaryAction?.index -> Unit // fine to pre-tap the action
                                            else -> {
                                                postCoach(coach.deviateLine(deviateCursor), CoachTone.NEUTRAL)
                                                deviateCursor += 1
                                            }
                                        }
                                    } else {
                                        when {
                                            primaryAction != null && tapped == primaryAction.index -> {
                                                if (opt.index !in exploredOptions) exploredOptions.add(opt.index)
                                                exploreSelectedIdx = null
                                                coachObserving = true
                                                postCoach(
                                                    coach.inferenceFor(opt.label)
                                                        ?: "Watch what changes with “${opt.label}”.",
                                                    CoachTone.CORRECT,
                                                )
                                            }
                                            tapped == opt.index -> Unit // re-selecting the same option
                                            else -> {
                                                postCoach(coach.deviateLine(deviateCursor), CoachTone.NEUTRAL)
                                                deviateCursor += 1
                                            }
                                        }
                                    }
                                }
                            }
                            // v2 adaptive free-play: taps are free; the coach reacts to verdicts only.
                            else -> Unit
                        }
                    },
                    onMathProblemReported = onMathProblem,
                    // The continuous page-side loop owns the glow for V3 math practice (self-correcting,
                    // no push staleness). While it's active we suppress the single-push highlight so the
                    // two never fight. The old push still drives the scripted teach walkthrough, V1/V2,
                    // and non-math sims.
                    highlightStepIndex = when {
                        coachLoopActive -> null
                        // Reteach: glow the correct answer while the "why" is on screen.
                        mathReteachStep != null -> mathReteachStep
                        // v3 maths practice: glow the correct answer / next move as a live hint
                        // (build → the biggest button that fits; MCQ → the correct option). Show it
                        // as soon as the learner reaches Phase B with a solvable problem — NOT gated
                        // on a prior verdict, else MCQ sims with no practice block never glow.
                        inPhaseB && coachMode == SimCoachMode.GUIDED &&
                            mathAnswerStep != null && !coachObserving -> mathAnswerStep
                        inScripted -> scriptedSteps.getOrNull(guideStepIdx)?.targetIndex
                        else -> guidedSuggestTarget
                    },
                    // Red only for the answer the learner just missed (reteach); amber for every
                    // proactive hint (next build move / suggested option) so it doesn't read as "wrong".
                    highlightKind = if (mathReteachStep != null) "answer" else "hint",
                    coachLoopActive = coachLoopActive,
                    coachReteach = mathReteachStep != null,
                    // v4 one-clock coach: turn the page-side loop on, mirror its text, speak its line.
                    coachV4Active = coachV4Active,
                    onCoachText = { line -> v4Line = line },
                    onCoachSpeak = { line ->
                        if (voiceEnabled && line.isNotBlank() && isForeground) {
                            keyConceptTts.stop()                 // barge-in: drop the previous line
                            keyConceptTts.speak(line, "v4-$decodedUrl-${System.nanoTime()}")
                        }
                    },
                    onCoachStop = { keyConceptTts.stop() }, // Explain panel Stop button
                    onCoachExplainVisible = { visible -> explainOpen = visible },
                    explainSignal = explainSignal,          // coach-card Explain chip → open the page panel
                    explainDismissSignal = explainDismissSignal,
                    hintMode = hintMode,                    // student-chosen hint model
                    hintSignal = hintSignal,                // Hint / Show-answer chip → advance disclosure
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

                // V5 floating coach — overlays the sim (Layout C): small bubble + one-line peek +
                // floating Hint. Frees the vertical space the bottom card used to take.
                if (trialPrompt == null && isV4 && coachV4Active && v4Line.isNotBlank()) {
                    SimFloatingCoach(
                        message = v4Line.ifBlank { coachMission ?: "Follow the glowing hint." },
                        hintMode = hintMode,
                        voiceEnabled = voiceEnabled,
                        onVoiceChange = { voiceEnabled = it },
                        onHint = { hintSignal++ },
                        onExplain = { explainOpen = true; explainSignal++ },
                        explainOpen = explainOpen,
                        onReplay = onReplay,
                        avatar = coachAvatar,
                        // Anchor to the bottom region only (not fillMaxSize) so the rest of the sim
                        // stays fully touchable — a full transparent overlay can eat WebView taps.
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    )
                }
            }

            // Coach bar lives BELOW the simulation (its own row in the Column) rather than
            // floating over it, so it never covers controls like "Start race" when there's little
            // room under the sim.
            if (trialPrompt == null && inScripted) {
                // Phase A — scripted walkthrough (all of v1; the intro of v2/v3).
                val step = scriptedSteps[guideStepIdx]
                SimCoachOverlay(
                    instruction = step.instruction,
                    stepNumber = guideStepIdx + 1,
                    totalSteps = scriptedSteps.size,
                    isLast = guideStepIdx >= scriptedSteps.size - 1,
                    // Sliders / text inputs can't be completed with a single tap, so they show a
                    // Next button (requireAction = false) instead of gating on a tap.
                    requireAction = step.targetIndex != null && step.autoAdvance,
                    onNext = advanceScripted,
                    onClose = {
                        keyConceptTts.stop()
                        viewModel.dismissGuide(decodedUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    avatar = coachAvatar,
                    onBack = onBack,
                    onReplay = onReplay,
                )
            } else if (trialPrompt == null && (inPhaseB || phaseBDone)) {
                // Phase B — free-play coach (v2 adaptive / v3 fully guided). Never gates; carries
                // the current line, and in v3 keeps a control highlighted as the suggested next move.
                SimAdaptiveCoachBar(
                    message = coachMessage
                        ?: coachMission
                        ?: "Give it a try — tap the controls and see what happens.",
                    mission = coachMission,
                    tone = coachTone,
                    onClose = {
                        keyConceptTts.stop()
                        viewModel.dismissGuide(decodedUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    avatar = coachAvatar,
                    onReplay = onReplay,
                    onBack = onBack,
                    onContinue = onPhaseBContinue,
                )
            }
            // (V5 one-clock coach now renders as SimFloatingCoach overlaying the sim above,
            // instead of a bottom card here — see the Box content.)
        }
    }
}

/** Safe cyclic index into a list of coach lines (wraps, tolerates negatives, empty → null). */
private fun List<String>.cycle(i: Int): String? =
    if (isEmpty()) null else this[((i % size) + size) % size]

/** Unwraps the hosting [Activity] from a Compose [Context] (may be a ContextWrapper). */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
