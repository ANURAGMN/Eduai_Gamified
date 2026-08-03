package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
    onInteractionTrackingReady: () -> Unit = {},
    onInteractionBudgetReported: (Int) -> Unit = {},
    onKeyConceptReported: (String) -> Unit = {},
    onSimulationIntroReported: (String) -> Unit = onKeyConceptReported,
    onSimulationFooterReported: (String) -> Unit = {},
    onGuideStructureReported: (String) -> Unit = {},
    onGuideTap: (Int) -> Unit = {},
    highlightStepIndex: Int? = null,
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bridge = remember { SimulationWebViewBridge(mainHandler) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // Drive the in-page highlight (guided coach) when the target step changes.
    LaunchedEffect(highlightStepIndex) {
        webViewRef.value?.evaluateJavascript(
            "if(window.__eduHighlight){window.__eduHighlight(${highlightStepIndex ?: -1});}",
            null,
        )
    }

    AndroidView(
        factory = { context ->
            @SuppressLint("SetJavaScriptEnabled")
            WebView(context).apply {
                webViewRef.value = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            super.onPageFinished(view, finishedUrl)
                            if (!shouldHandlePageFinished(finishedUrl, bridge.expectedUrl)) return
                            view?.evaluateJavascript(SimulationInteractionScript.injectionScript, null)
                            bridge.onPageFinished()
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
            webViewRef.value = webView
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}
