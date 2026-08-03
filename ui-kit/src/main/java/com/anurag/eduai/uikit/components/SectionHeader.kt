package com.anurag.eduai.uikit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    seeAllLabel: String? = null,
    onSeeAllClick: (() -> Unit)? = null,
) {
    val colors = EduAiTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (seeAllLabel != null && onSeeAllClick != null) {
            Text(
                text = seeAllLabel,
                color = colors.accent,
                modifier = Modifier.pressScaleClickable(onClick = onSeeAllClick, pressedScale = 0.94f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
