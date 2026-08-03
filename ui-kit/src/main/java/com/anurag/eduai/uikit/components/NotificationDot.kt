package com.anurag.eduai.uikit.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun NotificationDot(
    modifier: Modifier = Modifier,
    size: Dp = EduAiDimens.notificationDotSize,
    borderColor: Color? = null,
    pulse: Boolean = true,
) {
    val colors = EduAiTheme.colors
    // Outer Box stays exactly `size` so callers' alignment/offset math is unaffected —
    // the ping ring below only overflows visually via a scale transform, not layout.
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (pulse) {
            val ringScale = rememberPulseScale(min = 1f, max = 2.2f, durationMillis = 1300, repeatMode = RepeatMode.Restart)
            val ringAlpha = (1f - ((ringScale - 1f) / 1.2f).coerceIn(0f, 1f)) * 0.5f
            Box(
                modifier =
                    Modifier
                        .size(size)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(colors.danger.copy(alpha = ringAlpha)),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(colors.danger)
                    .then(
                        if (borderColor != null) {
                            Modifier.border(1.5.dp, borderColor, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
        )
    }
}
