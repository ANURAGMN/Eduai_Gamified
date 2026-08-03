package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun ThemeToggle(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    Box(
        modifier =
            modifier
                .width(42.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) colors.accent else colors.borderStrong)
                .clickable(onClick = onToggle),
        contentAlignment = if (isDark) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(2.dp)
                    .width(20.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(androidx.compose.ui.graphics.Color.White),
        )
    }
}
