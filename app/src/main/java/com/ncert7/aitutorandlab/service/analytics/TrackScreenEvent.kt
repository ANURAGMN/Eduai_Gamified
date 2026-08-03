package com.ncert7.aitutorandlab.service.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Tracks screen entry and exit events for analytics purposes.
 * Re-opens screen timing when the app returns from background (new session).
 */
@Composable
fun TrackScreenEvent(
    screenName: ScreenName,
    conceptId: String? = null,
) {
    LaunchedEffect(screenName, conceptId) {
        SessionManager.trackScreenEntry(screenName, conceptId)
    }

    DisposableEffect(screenName, conceptId) {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    SessionManager.trackScreenEntryImmediate(screenName, conceptId)
                }
            }
        processLifecycle.addObserver(observer)
        onDispose {
            processLifecycle.removeObserver(observer)
            SessionManager.trackScreenExitImmediate(screenName)
        }
    }
}