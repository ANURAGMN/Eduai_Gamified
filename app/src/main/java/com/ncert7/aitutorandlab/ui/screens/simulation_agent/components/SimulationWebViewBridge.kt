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
}
