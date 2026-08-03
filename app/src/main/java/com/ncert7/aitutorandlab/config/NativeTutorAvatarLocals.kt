package com.ncert7.aitutorandlab.config

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** App-wide native tutor avatar flag — provided at the nav root, read by agent screens. */
val LocalNativeTutorAvatarEnabled = staticCompositionLocalOf { false }

/** Re-reads the debug pref when the host activity resumes (e.g. after Settings toggle). */
@Composable
fun rememberNativeTutorAvatarEnabled(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember {
        mutableStateOf(GamificationFeatureFlags.isNativeTutorAvatarEnabled(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = GamificationFeatureFlags.isNativeTutorAvatarEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

fun isNativeTutorAvatarEnabledForContext(context: Context): Boolean =
    GamificationFeatureFlags.isNativeTutorAvatarEnabled(context)
