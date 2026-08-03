package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.forRole

@Composable
fun EduChip(
    label: String,
    role: EduChipRole = EduChipRole.Neutral,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    showNotificationDot: Boolean = false,
    onClick: (() -> Unit)? = null,
    labelContent: (@Composable (Color) -> Unit)? = null,
) {
    val colors = EduAiTheme.colors
    val (foreground, background) = colors.forRole(role)
    val clickableModifier =
        if (onClick != null) {
            Modifier.pressScaleClickable(onClick = onClick, pressedScale = 0.93f)
        } else {
            Modifier
        }

    BoxWithDot(showDot = showNotificationDot) {
        Row(
            modifier =
                modifier
                    .clip(RoundedCornerShape(EduAiDimens.chipRadius))
                    .background(background)
                    .then(clickableModifier)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            if (labelContent != null) {
                labelContent(foreground)
            } else {
                Text(
                    text = label,
                    color = foreground,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun BoxWithDot(
    showDot: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = EduAiTheme.colors
    Box {
        content()
        if (showDot) {
            NotificationDot(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 2.dp),
                borderColor = colors.surface1,
            )
        }
    }
}
