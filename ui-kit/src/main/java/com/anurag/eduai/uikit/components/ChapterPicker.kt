package com.anurag.eduai.uikit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * A chapter option for [EduChapterPicker].
 *
 * @param done concepts completed in this chapter (0 when unknown / not started).
 * @param total concepts in this chapter (0 when unknown — progress is then hidden).
 */
data class EduChapterPickerItem(
    val id: String,
    val label: String,
    val done: Int = 0,
    val total: Int = 0,
)

private val DoneColor = Color(0xFF16A34A)
private val InProgressColor = Color(0xFFF59E0B)

/**
 * Onboarding-style chapter picker: back link, bold title, soft subtitle, a scrollable list
 * of dense selectable rows (status dot · name · progress ring), and a Continue button. The
 * "recommended" hint is dynamic — it lands on the first incomplete chapter and its wording is
 * supplied by the caller ("Start here" for a fresh account, "Continue" once there's progress).
 * Purely presentational — the caller owns selection + navigation.
 */
@Composable
fun EduChapterPicker(
    title: String,
    subtitle: String,
    chapters: List<EduChapterPickerItem>,
    selectedId: String,
    recommendedLabel: String,
    backLabel: String,
    continueLabel: String,
    loadingLabel: String,
    emptyLabel: String = "No chapters available yet.",
    isLoading: Boolean = false,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors

    // Dynamic recommendation: the first chapter that isn't complete. If everything is complete →
    // none. If there's no progress data at all (e.g. onboarding) → the first chapter.
    val firstIncomplete = chapters.indexOfFirst { it.done < it.total }
    val recIdx = when {
        firstIncomplete >= 0 -> firstIncomplete
        chapters.any { it.total > 0 } -> -1
        chapters.isNotEmpty() -> 0
        else -> -1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface1)
            .padding(horizontal = 22.dp)
            .padding(top = 16.dp, bottom = 22.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBack() }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
            Text(backLabel, color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(10.dp))
        Text(title, color = colors.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = colors.textSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))

        // One grouped card with hairline dividers keeps the list dense.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (isLoading) {
                Text(loadingLabel, color = colors.textMuted, fontSize = 13.sp)
            } else if (chapters.isEmpty()) {
                Text(emptyLabel, color = colors.textMuted, fontSize = 13.sp)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
                ) {
                    chapters.forEachIndexed { i, c ->
                        ChapterPickRow(
                            item = c,
                            recommended = i == recIdx,
                            recommendedLabel = recommendedLabel,
                            selected = c.id == selectedId,
                            showDivider = i < chapters.lastIndex,
                            onClick = { onSelect(c.id) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        EduPrimaryButton(
            text = continueLabel,
            onClick = onContinue,
            fillMaxWidth = true,
            enabled = selectedId.isNotEmpty(),
        )
    }
}

@Composable
private fun ChapterPickRow(
    item: EduChapterPickerItem,
    recommended: Boolean,
    recommendedLabel: String,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val isDone = item.total > 0 && item.done >= item.total
    val inProgress = item.done > 0 && !isDone
    val pct = if (item.total > 0) (item.done * 100 / item.total) else 0
    val statusColor = when {
        isDone -> DoneColor
        inProgress -> InProgressColor
        else -> colors.textMuted
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) colors.accentBg else colors.surface2)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Spacer(Modifier.size(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.label,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (recommended) {
                    Text(recommendedLabel, color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.size(8.dp))
            ProgressRing(
                pct = pct,
                color = statusColor,
                done = isDone,
                track = colors.border,
                textColor = colors.textSecondary,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
        }
    }
}

/** Small circular progress indicator with the percent in the middle (a check when complete). */
@Composable
private fun ProgressRing(
    pct: Int,
    color: Color,
    done: Boolean,
    track: Color,
    textColor: Color,
) {
    Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (pct > 0) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * (pct.coerceIn(0, 100) / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        if (done) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp),
            )
        } else {
            Text(
                text = pct.toString(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
