package com.anurag.eduai.uikit.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anurag.eduai.uikit.theme.EduAiDimens

@Composable
fun RailCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    EduCard(
        modifier =
            modifier
                .defaultMinSize(minWidth = EduAiDimens.railCardMinWidth)
                .pressScaleClickable(onClick = onClick, pressedScale = 0.96f),
        content = content,
    )
}
