package com.anurag.eduai.uikit.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.theme.EduAiDimens

@Composable
fun HorizontalRail(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(EduAiDimens.railSpacing),
        content = content,
    )
}
