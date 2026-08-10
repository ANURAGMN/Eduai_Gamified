package com.ncert7.aitutorandlab.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.ads.ClickAdGate
import com.ncert7.aitutorandlab.service.analytics.AdAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.AdInteraction
import com.ncert7.aitutorandlab.service.analytics.AdPlacement
import com.ncert7.aitutorandlab.service.analytics.AdType
import com.ncert7.aitutorandlab.ui.components.AdDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GatedNavigationAction(
    private val scope: CoroutineScope,
    private val showAdDialog: (navigate: () -> Unit) -> Unit
) {
    fun run(trackClick: suspend () -> Unit, navigate: () -> Unit) {
        scope.launch {
            try {
                val needsAd = ClickAdGate.shouldShowAdBeforeNextClick()
                trackClick()
                if (needsAd) {
                    showAdDialog(navigate)
                } else {
                    navigate()
                }
            } catch (e: Exception) {
                DebugLogger.errorLog("NavigationAdGate", "Ad gate failed: ${e.message}")
                navigate()
            }
        }
    }
}

@Composable
fun NavigationAdGate(content: @Composable (GatedNavigationAction) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAd by remember { mutableStateOf(false) }
    var pendingNavigate by remember { mutableStateOf<(() -> Unit)?>(null) }

    val gated = remember(scope) {
        GatedNavigationAction(scope) { navigate ->
            pendingNavigate = navigate
            showAd = true
        }
    }

    content(gated)

    if (showAd) {
        LaunchedEffect(Unit) {
            AdAnalyticsTracker.trackAndWait(
                AdType.BANNER,
                AdInteraction.SHOWN,
                AdPlacement.AD_DIALOG
            )
        }
        AdDialog(
            context = context,
            onDismiss = {
                showAd = false
                // Ad was shown → reset the in-sim interaction counter that gates the next ad.
                ClickAdGate.consumeAd()
                pendingNavigate?.invoke()
                pendingNavigate = null
            }
        )
    }
}
