package com.ncert7.aitutorandlab.ui.gamification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.components.RewardOverlay
import com.anurag.eduai.uikit.theme.EduAiTheme
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

    Box(modifier = modifier.fillMaxSize()) {
        content()
        NotificationPermissionHost()
        NotificationAvatarCacheHost()

        pendingReward?.let { reward ->
            EduAiTheme {
                RewardOverlay(
                    visible = true,
                    xpEarned = reward.xpEarned,
                    gemsEarned = reward.gemsEarned,
                    xpFrom = reward.xpBarFrom,
                    xpTo = reward.xpBarTo,
                    weeklyXpTotal = reward.weeklyXpTotal,
                    onCollect = viewModel::dismissReward,
                )
            }
        }
    }
}
