package com.anurag.eduai.uikit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.theme.EduAiTheme

/**
 * A single spotlight step: a themed scrim with a rounded hole punched around the [target] rect, an
 * accent ring on it, and a tooltip card that parks on the opposite half of the screen so it never
 * covers what it points at. Coordinates are in root space; [viewport] is the root rect of the Box
 * this overlay fills, so the Canvas shifts into its own local space by [viewport]'s origin (handles
 * a Scaffold-offset host). Reused by both the home-rail tour and the bottom-nav tab tour.
 */
@Composable
fun EduIntroTourOverlay(
    step: Int,
    total: Int,
    target: Rect?,
    viewport: Rect?,
    title: String,
    body: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    skipLabel: String = "Skip",
    backLabel: String = "Back",
    nextLabel: String = "Next",
    doneLabel: String = "Got it",
    stepOfTotal: (Int, Int) -> String = { s, t -> "${s + 1} of $t" },
) {
    val colors = EduAiTheme.colors
    val targetInTopHalf = target != null && viewport != null && target.center.y < viewport.center.y
    val tooltipAtBottom = target == null || targetInTopHalf

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Lighter scrim for a screen-level walkthrough (no spotlight) so the screen shows through;
            // darker when isolating a specific element.
            val dim = if (target == null) Color(0x730B0E13) else Color(0xC00B0E13)
            if (target == null) {
                drawRect(dim)
            } else {
                val pad = 8.dp.toPx()
                val radius = 16.dp.toPx()
                // Rail/tab bounds are in root coords; this Canvas fills the (possibly offset) host Box,
                // so shift into local space by the viewport's own root origin before drawing.
                val ox = viewport?.left ?: 0f
                val oy = viewport?.top ?: 0f
                val l = (target.left - ox - pad).coerceIn(0f, size.width)
                val t = (target.top - oy - pad).coerceIn(0f, size.height)
                val r = (target.right - ox + pad).coerceIn(0f, size.width)
                val b = (target.bottom - oy + pad).coerceIn(0f, size.height)
                val scrim = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    addRoundRect(RoundRect(l, t, r, b, CornerRadius(radius, radius)))
                }
                drawPath(scrim, dim)
                drawRoundRect(
                    color = colors.accent,
                    topLeft = Offset(l, t),
                    size = Size(r - l, b - t),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 2.5.dp.toPx()),
                )

                // Arrow sitting between the tooltip and the spotlight, pointing at the highlight.
                val arrowW = 18.dp.toPx()
                val arrowH = 12.dp.toPx()
                val gap = 3.dp.toPx()
                val ax = (((target.left + target.right) / 2f) - ox).coerceIn(arrowW, size.width - arrowW)
                val arrow = Path()
                if (tooltipAtBottom) {
                    // Tooltip is below → arrow just under the spotlight, pointing up into it.
                    val tipY = b + gap
                    arrow.moveTo(ax, tipY)
                    arrow.lineTo(ax - arrowW / 2f, tipY + arrowH)
                    arrow.lineTo(ax + arrowW / 2f, tipY + arrowH)
                } else {
                    // Tooltip is above → arrow just over the spotlight, pointing down into it.
                    val tipY = t - gap
                    arrow.moveTo(ax, tipY)
                    arrow.lineTo(ax - arrowW / 2f, tipY - arrowH)
                    arrow.lineTo(ax + arrowW / 2f, tipY - arrowH)
                }
                arrow.close()
                drawPath(arrow, colors.accent)
            }
        }

        Column(
            Modifier
                .align(if (tooltipAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface2)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        stepOfTotal(step, total),
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    skipLabel,
                    color = colors.textMuted, fontSize = 12.sp,
                    modifier = Modifier.clickable { onSkip() }.padding(4.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(body, color = colors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(total) { i ->
                        Box(
                            Modifier
                                .size(if (i == step) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (i == step) colors.accent else colors.borderStrong),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (step > 0) {
                    Text(
                        backLabel,
                        color = colors.textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onBack() }.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.accent)
                        .clickable { onNext() }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                ) {
                    Text(
                        if (step >= total - 1) doneLabel else nextLabel,
                        color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
