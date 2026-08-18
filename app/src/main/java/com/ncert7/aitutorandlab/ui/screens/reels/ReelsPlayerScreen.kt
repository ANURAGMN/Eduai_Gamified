package com.ncert7.aitutorandlab.ui.screens.reels

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.ncert7.aitutorandlab.domain.reels.analytics.ReelWatchTracker
import com.ncert7.aitutorandlab.service.analytics.ReelsAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent

/**
 * Full-screen, in-app reel player using YouTube's **privacy-enhanced** embed
 * (`youtube-nocookie.com`) with related videos, branding, fullscreen and keyboard suppressed
 * (Play Families). External navigation (e.g. "watch on YouTube") is blocked so nothing leaves the
 * app. Only pass Made-for-kids [videoId]s here.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReelsPlayerScreen(
    videoId: String,
    onBack: () -> Unit,
) {
    // Per-video watch screen dwell → app_analytics (Room) → GA4 → Firestore (when mirror is on).
    TrackScreenEvent(ScreenName.REELS_PLAYER, conceptId = videoId)

    // Foreground-aware watch time: pause while backgrounded, emit a reel_watch event on close.
    val watch = remember(videoId) { ReelWatchTracker() }
    DisposableEffect(videoId) {
        watch.onPlay(System.currentTimeMillis())
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> watch.onPlay(System.currentTimeMillis())
                    Lifecycle.Event.ON_STOP -> watch.onPause(System.currentTimeMillis())
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            val now = System.currentTimeMillis()
            watch.onPause(now)
            // Duration is unknown for the nocookie embed → completion is null (watched ms only).
            ReelsAnalyticsTracker.trackWatch(
                videoId = videoId,
                watchedMs = watch.watchedMs(now),
                completion = null,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    webViewClient = ContainedYoutubeClient()
                    loadDataWithBaseURL(
                        NOCOOKIE_BASE,
                        embedHtml(videoId),
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            },
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

/** Keeps the WebView on the embed only — any attempt to navigate elsewhere (YouTube app, share,
 * channel, related links) is swallowed so the child never leaves the app. */
private class ContainedYoutubeClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString().orEmpty()
        val host = request?.url?.host.orEmpty()
        // Allow only the nocookie player + Google's video CDNs it needs; block everything else.
        val allowed = host.endsWith("youtube-nocookie.com") ||
            host.endsWith("ytimg.com") ||
            host.endsWith("googlevideo.com") ||
            host.endsWith("gstatic.com") ||
            url.startsWith("about:")
        return !allowed // true = we handled it (i.e. blocked the navigation)
    }
}

private const val NOCOOKIE_BASE = "https://www.youtube-nocookie.com"

private fun embedHtml(videoId: String): String {
    val id = videoId.trim()
    // Families-safe params: no related, modest branding, no fullscreen, inline, no annotations/keyboard.
    val src = "$NOCOOKIE_BASE/embed/$id" +
        "?autoplay=1&playsinline=1&rel=0&modestbranding=1&fs=0&iv_load_policy=3&disablekb=1"
    return """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
        <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}
        .wrap{position:fixed;inset:0}iframe{width:100%;height:100%;border:0}</style>
        </head><body><div class="wrap">
        <iframe src="$src" allow="autoplay; encrypted-media" allowfullscreen></iframe>
        </div></body></html>
    """.trimIndent()
}
