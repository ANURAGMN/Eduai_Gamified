package com.ncert7.aitutorandlab.ui.gamification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncert7.aitutorandlab.notification.NotificationAvatarCacheHost
import com.ncert7.aitutorandlab.notification.NotificationPermissionHost
import com.ncert7.aitutorandlab.ui.gamification.viewmodel.RewardOverlayViewModel

@Composable
fun GamificationRewardHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val viewModel: RewardOverlayViewModel = hiltViewModel()
    val pendingReward by viewModel.pendingReward.collectAsState()

    // The blocking XP/gems "Collect" overlay is suppressed to cut popups. XP/gems are still awarded
    // upstream and shown on Home/Progress. We still drain each reward event via dismissReward(), which
    // keeps the (throttled) notification-permission primer firing on a meaningful win — just without
    // the interrupting reward overlay. The primer self-limits to once/day, 3 times total.
    LaunchedEffect(pendingReward) {
        if (pendingReward != null) viewModel.dismissReward()
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        NotificationPermissionHost()
        NotificationAvatarCacheHost()
    }
}
