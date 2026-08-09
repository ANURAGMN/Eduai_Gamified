package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

import android.os.Handler
import android.webkit.JavascriptInterface
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.InteractionTracker

/**
 * Stable JS bridge holder so [SimulationWebView] can refresh Compose callbacks via [update]
 * without recreating the [android.webkit.WebView].
 */
internal class SimulationWebViewBridge(
    private val mainHandler: Handler,
) {
    var expectedUrl: String = ""
    var onParamsChanged: (Map<String, Any>) -> Unit = {}
    var onPageFinished: () -> Unit = {}
    var onInteractionTrackingReady: () -> Unit = {}
    var onInteractionBudgetReported: (Int) -> Unit = {}
    var onSimulationIntroReported: (String) -> Unit = {}
    var onSimulationFooterReported: (String) -> Unit = {}
    var onGuideStructureReported: (String) -> Unit = {}
    var onGuideTapReported: (Int) -> Unit = {}
    var onMathProblemReported: (String) -> Unit = {}
    // V4 one-clock coach: the page-side loop pushes the CURRENT short line to speak / to display.
    var onCoachSpeak: (String) -> Unit = {}
    var onCoachStop: () -> Unit = {}
    var onCoachText: (String) -> Unit = {}
    /** Explain panel opened/closed in the page (Compose coach collapses while open). */
    var onCoachExplainVisible: (Boolean) -> Unit = {}

    private val paramsBridge = SimulationJavaScriptInterface { params ->
        mainHandler.post { onParamsChanged(params) }
    }

    fun paramsInterface(): SimulationJavaScriptInterface = paramsBridge

    @JavascriptInterface
    fun logButtonClick(buttonName: String) {
        InteractionTracker.logInteraction(buttonName)
    }

    @JavascriptInterface
    fun logVerdict(isCorrect: Boolean) {
        InteractionTracker.logVerdict(isCorrect)
    }

    @JavascriptInterface
    fun onTrackingReady() {
        mainHandler.post { onInteractionTrackingReady() }
    }

    @JavascriptInterface
    fun reportInteractionBudget(count: Int) {
        mainHandler.post { onInteractionBudgetReported(count.coerceAtLeast(1)) }
    }

    @JavascriptInterface
    fun reportSimulationIntro(text: String) {
        val trimmed = text.trim()
        DebugLogger.debugLog("SimulationWebViewBridge", "reportSimulationIntro: ${trimmed.take(80)}")
        mainHandler.post { onSimulationIntroReported(trimmed) }
    }

    /** Backward-compatible alias for older injected script versions. */
    @JavascriptInterface
    fun reportKeyConcept(text: String) {
        reportSimulationIntro(text)
    }

    @JavascriptInterface
    fun reportSimulationFooter(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        DebugLogger.debugLog("SimulationWebViewBridge", "reportSimulationFooter: ${trimmed.take(80)}")
        mainHandler.post { onSimulationFooterReported(trimmed) }
    }

    /** Guided coach: the harvested control structure (JSON) for this simulation. */
    @JavascriptInterface
    fun reportGuideStructure(json: String) {
        DebugLogger.debugLog("SimulationWebViewBridge", "reportGuideStructure: ${json.take(120)}")
        mainHandler.post { onGuideStructureReported(json) }
    }

    /** Guided coach: the learner tapped the control stamped with this data-edu-step index. */
    @JavascriptInterface
    fun onGuideTap(index: Int) {
        mainHandler.post { onGuideTapReported(index) }
    }

    /**
     * Maths coach: the current problem read off the sim's DOM, as JSON —
     * `{prompt, current, options:[{label, value, step}]}`. Re-sent each round so the coach can
     * solve it (via MathCoachSolver) and give number-specific worked feedback + highlight the answer.
     */
    @JavascriptInterface
    fun reportMathProblem(json: String) {
        DebugLogger.debugLog("reportMathProblem", json.take(160))
        mainHandler.post { onMathProblemReported(json) }
    }

    /**
     * V4 one-clock coach — the page-side loop calls this (on the SAME tick as the glow) with the one
     * short line to SPEAK for the current round. Kotlin does barge-in TTS (stop, then speak). Only
     * fires when the line changes, so it never spams.
     */
    @JavascriptInterface
    fun coachSpeak(text: String) {
        val line = text.trim()
        DebugLogger.debugLog("coachSpeak", line.take(160))
        mainHandler.post { onCoachSpeak(line) }
    }

    /**
     * V4 one-clock coach — the page-side loop calls this (same tick as the glow) with the text to
     * DISPLAY. Kotlin is a passive mirror: it just renders whatever it last received, so the on-screen
     * text can never drift from the glow.
     */
    @JavascriptInterface
    fun coachText(text: String) {
        val line = text.trim()
        DebugLogger.debugLog("coachText", line.take(160))
        mainHandler.post { onCoachText(line) }
    }

    /** Stop any ongoing coach TTS (used by the Explain panel's Stop button). */
    @JavascriptInterface
    fun coachStop() {
        DebugLogger.debugLog("coachStop", "stop")
        mainHandler.post { onCoachStop() }
    }

    /** Page Explain modal visibility — lets the native coach bar collapse while explaining. */
    @JavascriptInterface
    fun coachExplainVisible(visible: Boolean) {
        DebugLogger.debugLog("coachExplainVisible", "visible=$visible")
        mainHandler.post { onCoachExplainVisible(visible) }
    }
}
