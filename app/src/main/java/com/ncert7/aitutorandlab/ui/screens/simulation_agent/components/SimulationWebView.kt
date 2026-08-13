package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import kotlinx.coroutines.delay

private fun shouldHandlePageFinished(finishedUrl: String?, expectedUrl: String): Boolean {
    if (finishedUrl.isNullOrBlank() || finishedUrl == "about:blank") return false
    val finished = finishedUrl.substringBefore('#').trimEnd('/')
    val expected = expectedUrl.substringBefore('#').trimEnd('/')
    if (finished == expected) return true
    // Allow CDN redirects that preserve the simulation file name.
    val expectedName = expected.substringAfterLast('/')
    return expectedName.isNotEmpty() && finished.endsWith(expectedName)
}

@Composable
fun SimulationWebView(
    url: String,
    modifier: Modifier = Modifier,
    onParamsChanged: (Map<String, Any>) -> Unit = {},
    onPageFinished: () -> Unit = {},
    /** Main-frame load failed (network / HTTP / SSL) — parent can offer a skip dialog. */
    onLoadFailed: () -> Unit = {},
    onInteractionTrackingReady: () -> Unit = {},
    onInteractionBudgetReported: (Int) -> Unit = {},
    onKeyConceptReported: (String) -> Unit = {},
    onSimulationIntroReported: (String) -> Unit = onKeyConceptReported,
    onSimulationFooterReported: (String) -> Unit = {},
    onGuideStructureReported: (String) -> Unit = {},
    onGuideTap: (Int) -> Unit = {},
    onMathProblemReported: (String) -> Unit = {},
    highlightStepIndex: Int? = null,
    highlightKind: String = "answer",
    // Continuous page-side glow loop (the "pull" model). When active it OWNS the highlight for V3
    // math practice; the single-push __eduHighlight above is suppressed (index null) so the two
    // never fight. coachReteach colours the answer red while a wrong-answer "why" is on screen.
    coachLoopActive: Boolean = false,
    coachReteach: Boolean = false,
    // V4 one-clock coach: the page-side loop owns glow + text + voice. coachV4Active turns it on;
    // onCoachText mirrors its current line to Kotlin (passive display); onCoachSpeak routes its short
    // line to the app TTS (barge-in). When V4 is active, all V3 push/loop above is left inactive.
    coachV4Active: Boolean = false,
    onCoachText: (String) -> Unit = {},
    onCoachSpeak: (String) -> Unit = {},
    onCoachStop: () -> Unit = {},
    onCoachExplainVisible: (Boolean) -> Unit = {},
    explainSignal: Int = 0,
    /** Bumped to force-close the page Explain modal (e.g. slim coach strip tap). */
    explainDismissSignal: Int = 0,
    hintMode: String = "ask",
    hintSignal: Int = 0,
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bridge = remember { SimulationWebViewBridge(mainHandler) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val onLoadFailedState = rememberUpdatedState(onLoadFailed)

    // Drive the in-page highlight (guided coach) when the target step or its intent changes.
    LaunchedEffect(highlightStepIndex, highlightKind) {
        webViewRef.value?.evaluateJavascript(
            "if(window.__eduHighlight){window.__eduHighlight(${highlightStepIndex ?: -1}, '$highlightKind');}",
            null,
        )
    }

    // Coach-card "Explain" chip → open the page-side detail panel.
    LaunchedEffect(explainSignal) {
        if (explainSignal > 0) {
            webViewRef.value?.evaluateJavascript(
                "if(window.__eduExplain){window.__eduExplain();}",
                null,
            )
        }
    }

    // Slim coach strip → close the page Explain modal.
    LaunchedEffect(explainDismissSignal) {
        if (explainDismissSignal > 0) {
            webViewRef.value?.evaluateJavascript(
                "if(window.__eduCloseExplain){window.__eduCloseExplain();}",
                null,
            )
        }
    }

    // Student-chosen hint model → tell the page-side engine which behavior to use.
    LaunchedEffect(hintMode) {
        webViewRef.value?.evaluateJavascript(
            "window.__eduHintMode='$hintMode';if(window.__eduSetHintMode){window.__eduSetHintMode('$hintMode');}",
            null,
        )
    }
    // Coach-card "Hint" / "Show answer" chip → advance the page-side disclosure level.
    LaunchedEffect(hintSignal) {
        if (hintSignal > 0) {
            webViewRef.value?.evaluateJavascript("if(window.__eduHint){window.__eduHint();}", null)
        }
    }

    // Toggle the continuous glow loop on/off and its reteach colour.
    LaunchedEffect(coachLoopActive) {
        webViewRef.value?.evaluateJavascript(
            "if(window.__eduCoach){window.__eduCoach.setActive($coachLoopActive);}",
            null,
        )
    }
    LaunchedEffect(coachReteach) {
        webViewRef.value?.evaluateJavascript(
            "if(window.__eduCoach){window.__eduCoach.setReteach($coachReteach);}",
            null,
        )
    }

    // V4 one-clock coach: turn the page-side loop on/off. Persist the wanted flag so a late inject
    // (guide unlock often races ahead of onPageFinished) still picks up setActive(true).
    LaunchedEffect(coachV4Active) {
        val js =
            "window.__eduCoachV4Wanted=$coachV4Active;" +
                "if(window.__eduCoachV4){window.__eduCoachV4.setActive($coachV4Active);}"
        fun push() {
            webViewRef.value?.evaluateJavascript(js, null)
        }
        push()
        // Retries cover: WebView not bound yet, inject not finished, sim re-created options.
        listOf(200L, 600L, 1500L, 3000L).forEach { delayMs ->
            delay(delayMs)
            push()
        }
    }

    AndroidView(
        factory = { context ->
            @SuppressLint("SetJavaScriptEnabled")
            WebView(context).apply {
                webViewRef.value = this
                setBackgroundColor(Color.WHITE)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    // Always fetch sim HTML / edu-coach.js from network (GitHub Pages).
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                webViewClient =
                    object : WebViewClient() {
                        private fun runVhRescue(view: WebView?, finishedUrl: String?) {
                            if (view == null) return
                            // Seed --vh for all pages; shell restyle is gated inside the script
                            // to science_4_10 only. Extra delayed passes only for that sim.
                            view.evaluateJavascript(SimulationInteractionScript.vhRescueScript, null)
                            val needsRetries =
                                (finishedUrl ?: bridge.expectedUrl).contains("science_4_10", ignoreCase = true)
                            if (!needsRetries) return
                            listOf(100L, 400L, 1000L, 2000L).forEach { delayMs ->
                                view.postDelayed({
                                    // Allow re-bind after navigation / SPA-ish reloads of the same page.
                                    view.evaluateJavascript(
                                        "window.__eduVhRescueBound=false;" +
                                            SimulationInteractionScript.vhRescueScript,
                                        null,
                                    )
                                }, delayMs)
                            }
                        }

                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            super.onPageFinished(view, finishedUrl)
                            // Seed --vh early (and shell-rescue science_4_10) even if URL matching
                            // would skip the main inject.
                            runVhRescue(view, finishedUrl)
                            val handleInject =
                                shouldHandlePageFinished(finishedUrl, bridge.expectedUrl)
                            if (handleInject) {
                                view?.evaluateJavascript(SimulationInteractionScript.injectionScript, null)
                                // Re-apply V4 active after inject — wanted may already be true from Compose.
                                view?.evaluateJavascript(
                                    "if(window.__eduCoachV4){window.__eduCoachV4.setActive(!!window.__eduCoachV4Wanted);}",
                                    null,
                                )
                                listOf(300L, 800L, 1600L).forEach { delayMs ->
                                    view?.postDelayed({
                                        view.evaluateJavascript(
                                            "if(window.__eduCoachV4){window.__eduCoachV4.setActive(!!window.__eduCoachV4Wanted);}",
                                            null,
                                        )
                                    }, delayMs)
                                }
                            }
                            // Always clear the load-stall timer when a real page finishes — even if
                            // inject is skipped (redirect / filename mismatch). about:blank stays waiting.
                            if (!finishedUrl.isNullOrBlank() && finishedUrl != "about:blank") {
                                bridge.onPageFinished()
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                onLoadFailedState.value()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?,
                        ) {
                            @Suppress("DEPRECATION")
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            // Older API path — treat as main-frame failure when URL matches expected.
                            val expected = bridge.expectedUrl.substringBefore('#').trimEnd('/')
                            val failed = (failingUrl ?: "").substringBefore('#').trimEnd('/')
                            if (failed.isNotEmpty() && (failed == expected || failed.endsWith(expected.substringAfterLast('/')))) {
                                onLoadFailedState.value()
                            }
                        }
                    }
                addJavascriptInterface(bridge.paramsInterface(), "SimulationAndroidInterface")
                addJavascriptInterface(bridge, "AndroidBridge")
            }
        },
        update = { webView ->
            bridge.expectedUrl = url
            bridge.onParamsChanged = onParamsChanged
            bridge.onPageFinished = onPageFinished
            bridge.onInteractionTrackingReady = onInteractionTrackingReady
            bridge.onInteractionBudgetReported = onInteractionBudgetReported
            bridge.onSimulationIntroReported = onSimulationIntroReported
            bridge.onSimulationFooterReported = onSimulationFooterReported
            bridge.onGuideStructureReported = onGuideStructureReported
            bridge.onGuideTapReported = onGuideTap
            bridge.onMathProblemReported = onMathProblemReported
            bridge.onCoachText = onCoachText
            bridge.onCoachSpeak = onCoachSpeak
            bridge.onCoachStop = onCoachStop
            bridge.onCoachExplainVisible = onCoachExplainVisible
            // hintMode / hintSignal are handled by LaunchedEffects above (no bridge state needed)
            webViewRef.value = webView
            if (webView.url != url) {
                webView.clearCache(true)
                webView.loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}
