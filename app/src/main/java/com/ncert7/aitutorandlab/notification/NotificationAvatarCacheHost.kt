package com.ncert7.aitutorandlab.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.anurag.eduai.uikit.avatar.TutorConfigStore
import kotlinx.coroutines.delay

/** Keeps the notification large-icon disk cache in sync with [TutorConfigStore]. */
@Composable
fun NotificationAvatarCacheHost() {
    val context = LocalContext.current
    TutorConfigStore.load(context)
    val config by TutorConfigStore.state

    LaunchedEffect(config) {
        // Brief delay so the window is ready for off-screen avatar capture.
        delay(400)
        NotificationAvatarCache.refresh(context, config)
    }
}
