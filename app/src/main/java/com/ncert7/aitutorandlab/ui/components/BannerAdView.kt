package com.ncert7.aitutorandlab.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.ads.AdManager
import com.ncert7.aitutorandlab.service.analytics.AdAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.AdInteraction
import com.ncert7.aitutorandlab.service.analytics.AdPlacement
import com.ncert7.aitutorandlab.service.analytics.AdType

/**
 * Displays a Google Mobile Ads banner and records impressions/clicks to analytics.
 */
@Composable
fun BannerAdView(
    context: Context,
    modifier: Modifier = Modifier,
    placement: AdPlacement = AdPlacement.AD_DIALOG,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((String) -> Unit)? = null
) {
    val adManager = remember { AdManager(context) }
    val adView = remember { adManager.createBannerAd() }

    remember {
        adManager.loadBannerAd(adView)
        adView
    }

    remember {
        val adListener = object : AdListener() {
            override fun onAdLoaded() {
                DebugLogger.debugLog("BannerAdView", " Ad loaded successfully")
                AdAnalyticsTracker.track(AdType.BANNER, AdInteraction.LOADED, placement)
                onAdLoaded?.invoke()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                DebugLogger.errorLog(
                    "BannerAdView",
                    " Ad failed to load: ${adError.message} (Code: ${adError.code})"
                )
                AdAnalyticsTracker.track(
                    AdType.BANNER,
                    AdInteraction.FAILED,
                    placement,
                    detail = "code=${adError.code}"
                )
                onAdFailedToLoad?.invoke(adError.message)
            }

            override fun onAdClicked() {
                DebugLogger.debugLog("BannerAdView", " Ad clicked")
                AdAnalyticsTracker.track(AdType.BANNER, AdInteraction.CLICK, placement)
            }

            override fun onAdOpened() {
                DebugLogger.debugLog("BannerAdView", " Ad opened (overlay shown)")
                AdAnalyticsTracker.track(AdType.BANNER, AdInteraction.OPENED, placement)
            }

            override fun onAdClosed() {
                DebugLogger.debugLog("BannerAdView", " Ad closed (returned to app)")
                AdAnalyticsTracker.track(AdType.BANNER, AdInteraction.CLOSED, placement)
            }

            override fun onAdImpression() {
                DebugLogger.debugLog("BannerAdView", " Ad impression recorded")
                AdAnalyticsTracker.track(AdType.BANNER, AdInteraction.IMPRESSION, placement)
            }
        }
        adManager.setAdListener(adView, adListener)
        adListener
    }

    DisposableEffect(Unit) {
        onDispose {
            DebugLogger.debugLog("BannerAdView", "Disposing banner ad resources")
            adManager.destroyBannerAd(adView)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}
