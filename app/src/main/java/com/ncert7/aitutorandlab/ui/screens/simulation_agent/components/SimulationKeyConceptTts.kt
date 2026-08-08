package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode

/**
 * Speaks the simulation intro (first heading + description from HTML, or API insight)
 * once per dedupe key when the page loads.
 */
class SimulationIntroTtsController(
    private val ttsController: TextToSpeech,
) {
    var speechEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                pendingText = null
                pendingDedupeKey = null
                ttsController.stop()
            }
        }

    private var spokenDedupeKey: String? = null
    private var pendingText: String? = null
    private var pendingDedupeKey: String? = null

    /** Live speaking state straight from the engine — safe to poll inside a coroutine loop. */
    val isSpeaking: Boolean
        get() = ttsController.state.value.isSpeaking

    /** Word-boundary index from the engine, used to drive avatar lip-sync. */
    val wordBoundaryIndex: kotlinx.coroutines.flow.StateFlow<Int>
        get() = ttsController.currentWordIndex

    private val isEngineReady: Boolean
        get() {
            val state = ttsController.state.value
            return state.isInitialized &&
                state.voicesFullyLoaded &&
                state.selectedVoice != null
        }

    fun hasSpokenForSimulation(simulationKey: String): Boolean =
        spokenDedupeKey?.startsWith("$simulationKey|") == true

    fun hasPendingForSimulation(simulationKey: String): Boolean =
        pendingDedupeKey?.startsWith("$simulationKey|") == true

    fun speak(text: String, dedupeKey: String) {
        if (!speechEnabled) return
        val cleaned = SimulationIntroTtsSanitizer.forSpeech(text)
        if (cleaned.isBlank() || dedupeKey == spokenDedupeKey) return

        if (!isEngineReady) {
            pendingText = cleaned
            pendingDedupeKey = dedupeKey
            DebugLogger.debugLog(TAG, "Intro TTS queued (engine not ready): ${cleaned.take(60)}…")
            return
        }
        spokenDedupeKey = dedupeKey
        pendingText = null
        pendingDedupeKey = null
        val voiceName = ttsController.state.value.selectedVoice?.name ?: "engine-default"
        DebugLogger.debugLog(TAG, "Intro TTS speaking [$dedupeKey] voice=$voiceName: ${cleaned.take(80)}…")
        ttsController.speak(cleaned)
    }

    fun flushPending() {
        if (!isEngineReady) return
        val text = pendingText ?: return
        val key = pendingDedupeKey ?: return
        speak(text, key)
    }

    fun stop() {
        ttsController.stop()
    }

    fun resetDedupe() {
        spokenDedupeKey = null
        pendingText = null
        pendingDedupeKey = null
    }

    companion object {
        private const val TAG = "SimulationIntroTts"
    }
}

@Composable
fun rememberSimulationKeyConceptTts(
    languageCode: String,
    avatarCode: String = "boy",
    ttsController: TextToSpeech = hiltViewModel(),
): Pair<SimulationIntroTtsController, TextToSpeech.TTSState> {
    val context = LocalContext.current
    val ttsState by ttsController.state.collectAsState()
    val controller =
        remember(ttsController) {
            SimulationIntroTtsController(ttsController)
        }

    LaunchedEffect(Unit) {
        ttsController.initialize(context)
    }

    LaunchedEffect(languageCode, avatarCode, ttsState.isInitialized, ttsState.voicesFullyLoaded) {
        if (!ttsState.isInitialized || !ttsState.voicesFullyLoaded || languageCode.isEmpty()) return@LaunchedEffect
        ttsController.setAppLanguage(avatarCode, normalizeLanguageCode(languageCode))
        controller.flushPending()
    }

    return controller to ttsState
}

fun SimulationIntroTtsController.speakSimulationIntro(
    text: String,
    simulationKey: String,
) {
    speak(text, dedupeKey = "$simulationKey|intro")
}

fun SimulationIntroTtsController.speakSimulationFooter(
    text: String,
    simulationKey: String,
) {
    speak(text, dedupeKey = "$simulationKey|footer")
}

/** Reads a single guided-coach step aloud. Deduped per step so it plays exactly once. */
fun SimulationIntroTtsController.speakSimulationStep(
    text: String,
    simulationKey: String,
    stepIndex: Int,
) {
    speak(text, dedupeKey = "$simulationKey|step|$stepIndex")
}

/**
 * Reads an adaptive-coach line aloud during free-play. Keyed on a monotonically increasing
 * [seq] so each new line (even a repeated hint) plays instead of being deduped away.
 */
fun SimulationIntroTtsController.speakSimulationCoach(
    text: String,
    simulationKey: String,
    seq: Int,
) {
    speak(text, dedupeKey = "$simulationKey|coach|$seq")
}

/** Replays the current line on demand (Replay button); nonce keeps each replay un-deduped. */
fun SimulationIntroTtsController.speakSimulationReplay(
    text: String,
    simulationKey: String,
    nonce: Int,
) {
    stop()
    speak(text, dedupeKey = "$simulationKey|replay|$nonce")
}

/** @deprecated Use [speakSimulationIntro] */
fun SimulationIntroTtsController.speakFromHtml(
    htmlText: String,
    simulationKey: String,
) = speakSimulationIntro(htmlText, simulationKey)

fun SimulationIntroTtsController.speakTitleFallback(
    title: String,
    simulationKey: String,
) {
    if (hasSpokenForSimulation(simulationKey) || hasPendingForSimulation(simulationKey)) return
    speak(title, dedupeKey = "$simulationKey|title")
}

fun SimulationIntroTtsController.speakFromApiInsight(
    keyInsight: String,
    simulationKey: String,
    conceptIndex: Int,
) {
    speak(
        keyInsight,
        dedupeKey = "$simulationKey|api|$conceptIndex",
    )
}

/** Alias for older call sites. */
typealias SimulationKeyConceptTtsController = SimulationIntroTtsController
