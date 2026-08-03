package com.anurag.eduai.uikit.garden

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.garden.quest.SlotThumb
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.placeBased
import com.anurag.eduai.uikit.theme.EduAiTheme

@Composable
fun CollectionShelf(
    state: CollectionShelfState,
    onOpenCollection: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val collectedCount = state.items.size
    val lockedCount = state.lockedSlotCount.coerceAtLeast(0)
    val totalSlots = collectedCount + lockedCount
    val listState = rememberLazyListState()

    LaunchedEffect(collectedCount, state.totalCount) {
        if (totalSlots > 0) {
            listState.animateScrollToItem(totalSlots - 1)
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .background(colors.surface2)
            .clickable { onOpenCollection() },
    ) {
        if (collectedCount > 0) {
            Canvas(Modifier.fillMaxSize()) {
                drawCollectionShelfBackdrop(state.theme)
            }
        }
        LazyRow(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            items(count = totalSlots) { index ->
                if (index < collectedCount) {
                    val item = state.items[index]
                    if (state.theme.placeBased) {
                        CollectionShelfThumb(
                            zone = item.zone,
                            slot = item.slot,
                            theme = state.theme,
                            isNewest = index == collectedCount - 1,
                            modifier = Modifier.width(54.dp),
                        )
                    } else {
                        CollectionShelfMarker(
                            theme = state.theme,
                            index = index + 1,
                            label = item.label,
                            isNewest = index == collectedCount - 1,
                        )
                    }
                } else {
                    CollectionShelfEmptySlot(
                        theme = state.theme,
                        showHint = index == collectedCount,
                        hint = state.lockedSlotHint.ifBlank { state.emptyMessage },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionShelfEmptySlot(
    theme: Theme,
    showHint: Boolean,
    hint: String,
) {
    val colors = EduAiTheme.colors
    val width = if (theme.placeBased) 54.dp else 52.dp
    val dashColor = colors.textMuted.copy(alpha = 0.45f)

    Box(
        Modifier
            .width(width)
            .height(if (theme.placeBased) 72.dp else 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface1.copy(alpha = if (theme.placeBased) 0.35f else 0.85f))
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(1.5f, 1.5f),
                    size = Size(size.width - 3f, size.height - 3f),
                    cornerRadius = CornerRadius(16f, 16f),
                    style =
                        Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        ),
                )
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showHint && hint.isNotBlank()) {
            Text(
                text = hint,
                color = colors.textMuted,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "+",
                color = dashColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

@Composable
private fun CollectionShelfThumb(
    zone: Int,
    slot: Int,
    theme: Theme,
    isNewest: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val pulse =
        if (isNewest) {
            val transition = rememberInfiniteTransition(label = "collectionNewest")
            transition.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "collectionNewestAlpha",
            ).value
        } else {
            1f
        }

    Box(
        modifier
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isNewest) {
                    Modifier.border(
                        1.5.dp,
                        colors.success.copy(alpha = pulse),
                        RoundedCornerShape(8.dp),
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        SlotThumb(
            zoneIndex = zone,
            theme = theme,
            slot = slot,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CollectionShelfMarker(
    theme: Theme,
    index: Int,
    label: String,
    isNewest: Boolean,
) {
    val colors = EduAiTheme.colors
    val bg =
        when (theme) {
            Theme.ISLAND -> colors.successBg
            Theme.COLONY -> colors.accentBg
            else -> colors.surface1
        }
    val accent =
        when (theme) {
            Theme.ISLAND -> colors.success
            Theme.COLONY -> colors.accent
            else -> colors.textMuted
        }

    Box(
        Modifier
            .width(52.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (isNewest) {
                    Modifier.border(1.5.dp, accent, RoundedCornerShape(10.dp))
                } else {
                    Modifier.border(1.dp, colors.border, RoundedCornerShape(10.dp))
                },
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.ifBlank { index.toString() },
            color = accent,
            fontSize = if (label.length > 2) 9.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun DrawScope.drawCollectionShelfBackdrop(theme: Theme) {
    val garden = theme == Theme.GARDEN
    val island = theme == Theme.ISLAND
    val groundTop = size.height * 0.72f

    drawRect(
        Brush.verticalGradient(
            when {
                garden -> listOf(Color(0xFFF2FAF4), Color(0xFFDDF0E4))
                island -> listOf(Color(0xFFE8F4E8), Color(0xFFC8E6C9))
                theme == Theme.COLONY -> listOf(Color(0xFF2A2A40), Color(0xFF3D3D58))
                else -> listOf(Color(0xFF20263C), Color(0xFF39405C))
            },
            startY = 0f,
            endY = groundTop,
        ),
    )

    drawRect(
        when {
            garden -> Color(0xFFBFE3CB)
            island -> Color(0xFF9FD4A8)
            theme == Theme.COLONY -> Color(0xFF4A4160)
            else -> Color(0xFF4A4160)
        },
        topLeft = Offset(0f, groundTop),
        size = Size(size.width, size.height - groundTop),
    )
}
