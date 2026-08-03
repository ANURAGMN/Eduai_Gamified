package com.anurag.eduai.uikit.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun EduProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val animated by
        animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(600),
            label = "eduProgress",
        )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(EduAiDimens.progressHeight)
                .clip(RoundedCornerShape(EduAiDimens.progressHeight / 2))
                .background(colors.border),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(animated)
                    .height(EduAiDimens.progressHeight)
                    .background(colors.accent),
        )
    }
}
