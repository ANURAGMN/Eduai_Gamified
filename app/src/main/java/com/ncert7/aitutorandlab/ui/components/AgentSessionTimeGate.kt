package com.ncert7.aitutorandlab.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.SimulationTrialProceedOverlay
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.SimulationTrialPromptKind
import com.ncert7.aitutorandlab.utils.TrialCopy
import kotlinx.coroutines.delay

/**
 * Recurring "still there / ready to move on?" gate for agentic interactions (study, revision,
 * simulation-agent chats). While the screen is in the foreground it counts elapsed time and, at
 * each two-minute mark (2, 4, 6, 8, 10, 12, 14 min), shows a dismissible proceed overlay. Tapping
 * "Keep going" hides it until the next mark; tapping proceed calls [onProceed]. At the 16-minute
 * hard cap it auto-advances by calling [onProceed] itself.
 *
 * Time only accrues while the app is in the foreground, so an ad / background pause doesn't age the
 * session. Drop it inside the screen's root Box and align [modifier] to the bottom.
 */
@Composable
fun AgentSessionTimeGate(
    languageCode: String,
    inTrialMode: Boolean,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val softMarksMs = remember { (1..7).map { it * 2 * 60_000L } } // 2,4,6,8,10,12,14 min
    val hardCapMs = 16 * 60_000L

    var elapsedMs by rememberSaveable { mutableStateOf(0L) }
    var nextMarkIndex by rememberSaveable { mutableStateOf(0) }
    var showOverlay by remember { mutableStateOf(false) }
    var proceeded by remember { mutableStateOf(false) }
    val currentOnProceed by rememberUpdatedState(onProceed)

    // Timer runs only while foreground; pauses on background (ad / home / Play Store).
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isForeground) {
        if (!isForeground || proceeded) return@LaunchedEffect
        while (true) {
            delay(1_000)
            elapsedMs += 1_000
            if (elapsedMs >= hardCapMs) {
                proceeded = true
                currentOnProceed() // 16-min hard cap → auto-advance
                return@LaunchedEffect
            }
            if (!showOverlay &&
                nextMarkIndex < softMarksMs.size &&
                elapsedMs >= softMarksMs[nextMarkIndex]
            ) {
                showOverlay = true
            }
        }
    }

    if (proceeded) return

    val (title, message) = TrialCopy.agentTimeCheckPrompt(languageCode, inTrialMode)
    SimulationTrialProceedOverlay(
        visible = showOverlay,
        kind = SimulationTrialPromptKind.TIME_EXPLORATION,
        title = title,
        message = message,
        proceedLabel = TrialCopy.agentProceedLabel(languageCode, inTrialMode),
        exploreLabel = TrialCopy.agentKeepGoingLabel(languageCode),
        onProceed = {
            proceeded = true
            showOverlay = false
            currentOnProceed()
        },
        onKeepExploring = {
            showOverlay = false
            // Advance to the next mark strictly ahead of where we are now.
            nextMarkIndex = softMarksMs
                .indexOfFirst { it > elapsedMs }
                .let { if (it == -1) softMarksMs.size else it }
        },
        modifier = modifier,
    )
}
