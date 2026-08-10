package com.anurag.eduai.uikit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme
import kotlin.math.sin

/**
 * Item categories on the trial path, each with its own icon + accent.
 * [Simulation] = URL lab · [SimAgent] = simulation chat agent · [Study] = lesson chat ·
 * [Math] = math problem agent · [Revision] = revise.
 */
enum class TrialNodeType { Simulation, SimAgent, Study, Math, Revision }

/** DONE = cleared · ACTIVE = suggested next (START) · UPCOMING = not attended yet (still tappable). */
enum class TrialNodeState { Done, Active, Upcoming }

data class TrialPathNode(
    val id: Long,
    val type: TrialNodeType,
    val state: TrialNodeState,
    val title: String,
    /** 0f..1f — fills the ring around the active node (e.g. 3/7 knowledge bites). */
    val progress: Float = 0f,
)

/** A chapter "club": a header band followed by its ordered nodes (sims first, then study). */
data class TrialPathChapter(
    val title: String,
    val nodes: List<TrialPathNode>,
)

/**
 * A Duolingo-style vertical trail for one trial day: chapters are clubbed under header bands and
 * items zig-zag down the path, each tagged by type (Simulation / Study / Revision). The suggested
 * next item is raised, ringed by its progress and labelled START. Nothing is locked — not-yet-done
 * items are simply shown in a lighter tint and remain tappable. [dayIndex] rotates the palette so
 * each day's trail looks a little different.
 */
@Composable
fun PlanTrialPath(
    chapters: List<TrialPathChapter>,
    onNodeClick: (TrialPathNode) -> Unit,
    modifier: Modifier = Modifier,
    dayIndex: Int = 0,
    onChestClick: (Int) -> Unit = {},
    onDayCompleteClick: () -> Unit = {},
) {
    val colors = EduAiTheme.colors
    val chapterAccents = listOf(colors.accent, colors.pro, colors.warning, colors.success)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var nodeIndex = 0
        chapters.forEachIndexed { chapterIndex, chapter ->
            val accentIndex = modPositive(chapterIndex + dayIndex, chapterAccents.size)
            ChapterHeader(
                title = chapter.title,
                accent = chapterAccents.getOrElse(accentIndex) { chapterAccents.first() },
            )
            Spacer(modifier = Modifier.height(10.dp))
            chapter.nodes.forEach { node ->
                val sway = (sin(nodeIndex * 0.9) * 54).dp
                TrialNode(node = node, swayX = sway, onClick = { onNodeClick(node) })
                Spacer(modifier = Modifier.height(16.dp))
                nodeIndex++
            }
            val chapterDone =
                chapter.nodes.isNotEmpty() && chapter.nodes.all { it.state == TrialNodeState.Done }
            SpecialNode(
                kind = SpecialKind.Chest,
                ready = chapterDone,
                label = if (chapterDone) "Reward ready!" else "Chapter reward",
                onClick = { onChestClick(chapterIndex) },
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
        val allDone =
            chapters.isNotEmpty() &&
                chapters.all { ch -> ch.nodes.isNotEmpty() && ch.nodes.all { it.state == TrialNodeState.Done } }
        SpecialNode(
            kind = SpecialKind.Milestone,
            ready = allDone,
            label = if (allDone) "Day complete!" else "Finish the day",
            onClick = onDayCompleteClick,
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

private fun modPositive(value: Int, size: Int): Int {
    if (size <= 0) return 0
    val mod = value % size
    return if (mod < 0) mod + size else mod
}

@Composable
private fun ChapterHeader(title: String, accent: Color) {
    val colors = EduAiTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(accent)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "CHAPTER",
                color = colors.onAccent.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                color = colors.onAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.onAccent)
    }
}

@Composable
private fun TrialNode(
    node: TrialPathNode,
    swayX: Dp,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val typeColor =
        when (node.type) {
            TrialNodeType.Simulation -> colors.pro
            TrialNodeType.SimAgent -> colors.success
            TrialNodeType.Study -> colors.accent
            TrialNodeType.Math -> colors.warning
            TrialNodeType.Revision -> colors.warning.copy(alpha = 0.85f)
        }
    val icon =
        when (node.type) {
            TrialNodeType.Simulation -> Icons.Outlined.Science
            TrialNodeType.SimAgent -> Icons.Outlined.SmartToy
            TrialNodeType.Study -> Icons.Outlined.MenuBook
            TrialNodeType.Math -> Icons.Outlined.Calculate
            TrialNodeType.Revision -> Icons.Outlined.Autorenew
        }
    val typeLabel =
        when (node.type) {
            TrialNodeType.Simulation -> "Simulation"
            TrialNodeType.SimAgent -> "Sim agent"
            TrialNodeType.Study -> "Study"
            TrialNodeType.Math -> "Math"
            TrialNodeType.Revision -> "Revision"
        }

    val filled = node.state != TrialNodeState.Upcoming // Done or Active are solid; upcoming is tinted.
    val circleColor = if (filled) typeColor else typeColor.copy(alpha = 0.16f)
    val iconTint = if (filled) colors.onAccent else typeColor

    Column(
        modifier = Modifier.fillMaxWidth().offset(x = swayX),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (node.state == TrialNodeState.Active) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface1)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "START",
                    color = typeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Box(contentAlignment = Alignment.Center) {
            // Completion ring around the active node.
            if (node.state == TrialNodeState.Active) {
                val track = colors.border
                val fill = colors.accent
                val sweep = 360f * node.progress.coerceIn(0f, 1f)
                Canvas(modifier = Modifier.size(76.dp)) {
                    val stroke = 6.dp.toPx()
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    if (sweep > 0f) {
                        drawArc(
                            color = fill,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
            }

            // Raised 3D base for a tactile look.
            Box(
                modifier =
                    Modifier
                        .size(58.dp)
                        .offset(y = 5.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.30f)),
            )
            // Main node — always tappable.
            Box(
                modifier =
                    Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .then(
                            if (!filled) {
                                Modifier.border(2.dp, typeColor.copy(alpha = 0.55f), CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .pressScaleClickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = typeLabel,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp),
                )
            }
            // Done check badge.
            if (node.state == TrialNodeState.Done) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.success),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = typeLabel,
            color = typeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = node.title,
            color = colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 200.dp),
        )
    }
}

private enum class SpecialKind { Chest, Milestone }

/**
 * A reward marker on the path — a treasure chest at each chapter's end and a trophy at the day's end.
 * Greyed and inert until [ready] (all preceding items done), then it glows, pulses, and becomes tappable.
 */
@Composable
private fun SpecialNode(
    kind: SpecialKind,
    ready: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val gold = colors.warning
    val icon = if (kind == SpecialKind.Chest) Icons.Outlined.Redeem else Icons.Outlined.EmojiEvents
    val pulseRaw = rememberPulseScale(min = 0.96f, max = 1.08f, durationMillis = 1100)
    val pulse = if (ready) pulseRaw else 1f
    val circleColor = if (ready) gold else colors.surface2
    val iconTint = if (ready) colors.onAccent else colors.textMuted

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (ready) {
                Box(
                    modifier =
                        Modifier
                            .size(92.dp)
                            .scale(pulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(gold.copy(alpha = 0.5f), Color.Transparent),
                                ),
                            ),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .offset(y = 5.dp)
                        .clip(CircleShape)
                        .background(circleColor.copy(alpha = 0.30f)),
            )
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(circleColor)
                        .then(
                            if (ready) Modifier.pressScaleClickable(onClick = onClick) else Modifier,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (ready) gold else colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
