package com.anurag.eduai.uikit.avatar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.components.EduGhostButton
import com.anurag.eduai.uikit.components.EduProgressBar
import com.anurag.eduai.uikit.theme.EduAiTheme

/** Active rewarded-ad session — unlock a preset or save a custom tutor look. */
data class AdRewardRequest(
    val sessionId: String,
    val actionLabel: String,
)

/**
 * Rewarded-video flow for avatar unlock/save. When [showRewardedAds] is provided it
 * plays real AdMob rewarded ads; otherwise falls back to a simulated countdown (ui-kit preview).
 */
@Composable
fun AdRewardOverlay(
    request: AdRewardRequest?,
    onComplete: (sessionId: String) -> Unit,
    onCancel: (sessionId: String) -> Unit,
    totalAds: Int = 1,
    showRewardedAds: (suspend (sessionId: String, totalAds: Int, actionLabel: String) -> Boolean)? = null,
) {
    if (request == null) return
    val colors = EduAiTheme.colors

    var adIndex by remember(request.sessionId) { mutableIntStateOf(1) }
    val progress = remember(request.sessionId) { Animatable(0f) }

    LaunchedEffect(request.sessionId) {
        val sessionId = request.sessionId
        if (showRewardedAds != null) {
            val earned = showRewardedAds(sessionId, totalAds, request.actionLabel)
            if (earned) {
                onComplete(sessionId)
            } else {
                onCancel(sessionId)
            }
            return@LaunchedEffect
        }
        for (i in 1..totalAds) {
            adIndex = i
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(3000, easing = LinearEasing))
        }
        onComplete(sessionId)
    }

    if (showRewardedAds != null) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = colors.accent)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = request.actionLabel,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
        return
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface1)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Rewarded video",
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Ad $adIndex of $totalAds",
                color = colors.textMuted,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            EduProgressBar(progress = progress.value, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = request.actionLabel,
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            EduGhostButton(text = "Cancel", onClick = { onCancel(request.sessionId) })
        }
    }
}
