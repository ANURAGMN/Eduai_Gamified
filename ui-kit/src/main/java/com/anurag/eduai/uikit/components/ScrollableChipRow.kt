package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * Horizontal chip/options row with edge fades and a scroll hint so clipped content
 * is obviously swipeable (Avatar Studio customize, mood pickers, etc.).
 */
@Composable
fun ScrollableChipRow(
    modifier: Modifier = Modifier,
    showScrollHint: Boolean = true,
    hintText: String = "Swipe for more",
    fadeWidth: Dp = 44.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = EduAiTheme.colors
    val scrollState = rememberScrollState()
    val canScrollForward by remember {
        derivedStateOf {
            scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue
        }
    }
    val canScrollBackward by remember {
        derivedStateOf { scrollState.value > 0 }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (showScrollHint && canScrollForward) {
            Text(
                text = "$hintText →",
                color = colors.accent.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, bottom = 4.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (showScrollHint && canScrollForward) 18.dp else 0.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(
                            end = if (canScrollForward) fadeWidth - 8.dp else 4.dp,
                        ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )

            if (canScrollBackward) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(28.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    0f to colors.surface1,
                                    1f to Color.Transparent,
                                ),
                            ),
                )
            }

            if (canScrollForward) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(fadeWidth)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.55f to colors.surface1.copy(alpha = 0.88f),
                                    1f to colors.surface1,
                                ),
                            ),
                )
                if (showScrollHint) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = hintText,
                        tint = colors.accent,
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 6.dp)
                                .size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Wider horizontal rail (avatar cards, bookmark cards) with the same scroll affordances.
 */
@Composable
fun ScrollableHorizontalRail(
    modifier: Modifier = Modifier,
    showScrollHint: Boolean = true,
    hintText: String = "Swipe for more",
    fadeWidth: Dp = 56.dp,
    spacing: Dp = 12.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = EduAiTheme.colors
    val scrollState = rememberScrollState()
    val canScrollForward by remember {
        derivedStateOf {
            scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue
        }
    }
    val canScrollBackward by remember {
        derivedStateOf { scrollState.value > 0 }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (showScrollHint && canScrollForward) {
            Text(
                text = "$hintText →",
                color = colors.accent.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, bottom = 4.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (showScrollHint && canScrollForward) 18.dp else 0.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(end = if (canScrollForward) fadeWidth else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                content = content,
            )

            if (canScrollBackward) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(32.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    0f to colors.surface1,
                                    1f to Color.Transparent,
                                ),
                            ),
                )
            }

            if (canScrollForward) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(fadeWidth)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.55f to colors.surface1.copy(alpha = 0.88f),
                                    1f to colors.surface1,
                                ),
                            ),
                )
                if (showScrollHint) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = hintText,
                        tint = colors.accent,
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .size(24.dp),
                    )
                }
            }
        }
    }
}
