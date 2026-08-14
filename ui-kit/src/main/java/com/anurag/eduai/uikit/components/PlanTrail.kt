package com.anurag.eduai.uikit.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.theme.EduAiTheme
import kotlin.math.hypot
import kotlinx.coroutines.launch
import android.graphics.Paint as NativePaint
import android.graphics.Typeface

enum class PlanDayStatus { Done, Partial, Today, Upcoming }

enum class PlanDayType { Lesson, Revise, Mock, Exam }

data class PlanDayNode(
    val day: Int,
    val status: PlanDayStatus,
    val type: PlanDayType = PlanDayType.Lesson,
    val label: String = "",
    /** Comma-separated concept IDs for lesson/revise days. */
    val conceptIds: String = "",
)

/** Prototype SVG viewBox — 640×128 */
private const val DESIGN_W = 640f
private const val DESIGN_H = 128f

private val NODE_COORDS =
    listOf(
        40f to 55f,
        110f to 20f,
        180f to 55f,
        250f to 90f,
        320f to 55f,
        390f to 20f,
        460f to 55f,
        530f to 90f,
        600f to 55f,
    )

@Composable
fun PlanTrail(
    days: List<PlanDayNode>,
    onSeeAll: () -> Unit = {},
    onAddPlan: () -> Unit = onSeeAll,
    onDayClick: (PlanDayNode) -> Unit = {},
    modifier: Modifier = Modifier,
    sectionTitle: String = "Your exam prep plan",
    seeAllLabel: String = "See all",
    addPlanLabel: String = "Add plan",
    emptyTitle: String = "No exam plan yet",
    emptyBody: String = "Set exam type, chapters, and daily study time to build your prep trail.",
    todayLabel: String = "Today",
    dayLabelFor: (Int) -> String = { "Day $it" },
    growHint: String = "Finish a day to grow your world — every task plants something new.",
) {
    val colors = EduAiTheme.colors
    val shown = days.take(9)
    val hasPlan = days.isNotEmpty()
    val todayPulse = rememberPulseScale(min = 1f, max = 1.35f, durationMillis = 1400)
    var tappedIndex by remember { mutableIntStateOf(-1) }
    val tapScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val starPainter = rememberVectorPainter(Icons.Filled.Star)
    val playPainter = rememberVectorPainter(Icons.Filled.PlayArrow)
    val refreshPainter = rememberVectorPainter(Icons.Outlined.Refresh)
    val checkPainter = rememberVectorPainter(Icons.Outlined.CheckBox)
    val flagPainter = rememberVectorPainter(Icons.Outlined.Flag)
    val bookPainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.MenuBook)

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = sectionTitle,
            seeAllLabel = if (hasPlan) seeAllLabel else addPlanLabel,
            onSeeAllClick = if (hasPlan) onSeeAll else onAddPlan,
        )

        if (!hasPlan) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .pressScaleClickable(onClick = onAddPlan, pressedScale = 0.98f)
                        .background(colors.surface2, RoundedCornerShape(12.dp))
                        .padding(16.dp),
            ) {
                Text(
                    text = emptyTitle,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = emptyBody,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                )
                Button(onClick = onAddPlan) {
                    Text(addPlanLabel)
                }
            }
            return@Column
        }

        Canvas(
            modifier =
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp)
                    .width(640.dp)
                    .height(128.dp)
                    .pointerInput(shown) {
                        detectTapGestures { tap ->
                            val sx = size.width / DESIGN_W
                            val sy = size.height / DESIGN_H
                            shown.forEachIndexed { i, day ->
                                val (cx, cy) = NODE_COORDS.getOrElse(i) { 40f to 55f }
                                val rPx = if (day.status == PlanDayStatus.Today) 27f * sx else 21f * sx
                                val px = cx * sx
                                val py = cy * sy
                                if (hypot(tap.x - px, tap.y - py) <= rPx + 12f) {
                                    scope.launch {
                                        tappedIndex = i
                                        tapScale.snapTo(0.86f)
                                        tapScale.animateTo(
                                            1f,
                                            animationSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium,
                                                ),
                                        )
                                        tappedIndex = -1
                                    }
                                    onDayClick(day)
                                    return@detectTapGestures
                                }
                            }
                        }
                    },
        ) {
            val sx = size.width / DESIGN_W
            val sy = size.height / DESIGN_H
            fun tx(v: Float) = v * sx
            fun ty(v: Float) = v * sy

            // Wave path — same control points as prototype
            val path =
                Path().apply {
                    moveTo(tx(40f), ty(55f))
                    quadraticTo(tx(75f), ty(20f), tx(110f), ty(20f))
                    quadraticTo(tx(145f), ty(20f), tx(180f), ty(55f))
                    quadraticTo(tx(215f), ty(90f), tx(250f), ty(90f))
                    quadraticTo(tx(285f), ty(90f), tx(320f), ty(55f))
                    quadraticTo(tx(355f), ty(20f), tx(390f), ty(20f))
                    quadraticTo(tx(425f), ty(20f), tx(460f), ty(55f))
                    quadraticTo(tx(495f), ty(90f), tx(530f), ty(90f))
                    quadraticTo(tx(565f), ty(90f), tx(600f), ty(55f))
                }
            // Vibrant gradient trail connecting the days, instead of a flat grey line.
            drawPath(
                path = path,
                brush =
                    Brush.horizontalGradient(
                        listOf(colors.accent, colors.pro, colors.success),
                    ),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )

            shown.forEachIndexed { i, day ->
                val (cx, cy) = NODE_COORDS.getOrElse(i) { 40f to 55f }
                val center = Offset(tx(cx), ty(cy))
                val baseRadiusPx = if (day.status == PlanDayStatus.Today) 27.dp.toPx() else 21.dp.toPx()
                val pressScale = if (i == tappedIndex) tapScale.value else 1f
                val radiusPx = baseRadiusPx * pressScale
                val style = nodeStyle(day, colors)
                val painter =
                    when {
                        day.status == PlanDayStatus.Done -> starPainter
                        day.status == PlanDayStatus.Today -> playPainter
                        day.type == PlanDayType.Revise -> refreshPainter
                        day.type == PlanDayType.Mock -> checkPainter
                        day.type == PlanDayType.Exam -> flagPainter
                        else -> bookPainter
                    }
                val iconPx = if (day.status == PlanDayStatus.Today) 19.dp.toPx() else 16.dp.toPx()

                if (day.status == PlanDayStatus.Today) {
                    // Soft breathing halo behind today's node so it reads as the live/active step.
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.22f),
                        radius = radiusPx * todayPulse,
                        center = center,
                    )
                }

                // Outer fill
                drawCircle(color = style.bg, radius = radiusPx, center = center)
                // Border
                drawCircle(
                    color = style.border,
                    radius = radiusPx,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Icon centered in node
                withTransform({
                    translate(left = center.x - iconPx / 2f, top = center.y - iconPx / 2f)
                }) {
                    with(painter) {
                        draw(size = Size(iconPx, iconPx), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(style.tint))
                    }
                }

                // Label centered under node (native Paint Align.CENTER = prototype translateX(-50%))
                val label =
                    if (day.status == PlanDayStatus.Today) todayLabel else dayLabelFor(day.day)
                val labelColor =
                    if (day.status == PlanDayStatus.Today) colors.accent else colors.textMuted
                val paint =
                    NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
                        color = labelColor.toArgb()
                        textSize = 8.dp.toPx()
                        textAlign = NativePaint.Align.CENTER
                        typeface =
                            if (day.status == PlanDayStatus.Today) {
                                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            } else {
                                Typeface.DEFAULT
                            }
                    }
                val labelBaseline = center.y + radiusPx + 8.dp.toPx() - paint.ascent()
                drawContext.canvas.nativeCanvas.drawText(label, center.x, labelBaseline, paint)
            }
        }

        // Connects the plan to the reward world: finishing plan days grows the garden/space.
        Text(
            text = growHint,
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .background(colors.accentBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

private data class NodeStyle(
    val bg: Color,
    val border: Color,
    val tint: Color,
)

private fun nodeStyle(
    day: PlanDayNode,
    colors: com.anurag.eduai.uikit.theme.EduAiColors,
): NodeStyle =
    when {
        day.status == PlanDayStatus.Done ->
            NodeStyle(colors.success, colors.success, Color.White)
        day.status == PlanDayStatus.Partial ->
            NodeStyle(colors.surface2, colors.warning, colors.warning)
        day.status == PlanDayStatus.Today ->
            NodeStyle(colors.accent, colors.accent, Color.White)
        day.type == PlanDayType.Revise ->
            NodeStyle(colors.surface2, colors.borderStrong, colors.warning)
        day.type == PlanDayType.Mock ->
            NodeStyle(colors.surface2, colors.borderStrong, colors.pro)
        day.type == PlanDayType.Exam ->
            NodeStyle(colors.surface2, colors.borderStrong, colors.textMuted)
        else ->
            NodeStyle(colors.surface2, colors.borderStrong, colors.textSecondary)
    }
