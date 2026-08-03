package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun EduCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(colors.surface2)
                .padding(EduAiDimens.cardPadding),
        content = content,
    )
}
