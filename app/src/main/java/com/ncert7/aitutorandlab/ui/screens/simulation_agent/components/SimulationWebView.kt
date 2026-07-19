package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ncert7.aitutorandlab.service.analytics.InteractionTracker

class SimulationWebViewClient(
    private val onPageFinished: () -> Unit
) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.evaluateJavascript(SimulationInteractionScript.injectionScript, null)
        onPageFinished()
    }
}

@Composable
fun SimulationWebView(
    url: String,
    modifier: Modifier = Modifier,
    onParamsChanged: (Map<String, Any>) -> Unit = {},
    onPageFinished: () -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            @SuppressLint("SetJavaScriptEnabled")
            val webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                webViewClient = SimulationWebViewClient(onPageFinished)

                addJavascriptInterface(
                    SimulationJavaScriptInterface(onParamsChanged),
                    "SimulationAndroidInterface"
                )
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun logButtonClick(buttonName: String) {
                        InteractionTracker.logInteraction(buttonName)
                    }

                    @JavascriptInterface
                    fun logVerdict(isCorrect: Boolean) {
                        InteractionTracker.logVerdict(isCorrect)
                    }
                }, "AndroidBridge")

                loadUrl(url)
            }
            webView
        },
        modifier = modifier.fillMaxSize()
    )
}
