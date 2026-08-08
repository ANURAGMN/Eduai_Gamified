package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.CardBackground
import com.ncert7.aitutorandlab.ui.theme.ColorHint
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.ui.theme.White

/**
 * A slim coach bar over a simulation WebView: the current instruction, a progress
 * indicator, and a Next control. It never blocks the sim — it sits at the bottom and
 * points the learner (with the in-page highlight) at what to do next.
 */
@Composable
fun SimCoachOverlay(
    instruction: String,
    stepNumber: Int,
    totalSteps: Int,
    isLast: Boolean,
    requireAction: Boolean,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Optional lip-syncing tutor avatar; falls back to the School glyph when null. */
    avatar: (@Composable () -> Unit)? = null,
    /** Go to the previous step (null hides the control, e.g. on the first step). */
    onBack: (() -> Unit)? = null,
    /** Re-read the current step aloud. */
    onReplay: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (avatar != null) {
                avatar()
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step $stepNumber of $totalSteps",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = instruction,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close guide",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                CoachChip(text = "‹ Back", onClick = onBack)
                Spacer(Modifier.size(6.dp))
            }
            if (onReplay != null) {
                CoachChip(text = "↻ Replay", onClick = onReplay)
                Spacer(Modifier.size(8.dp))
            }
            if (requireAction) {
                // The action IS the way forward — do it in the experiment above. "Skip" is
                // a tiny escape hatch so a stuck learner is never fully trapped.
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Tap the glowing control above to continue",
                    color = BrandPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Skip",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNext() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .size(width = 16.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i < stepNumber) BrandPrimary else ColorHint),
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BrandPrimary)
                        .clickable { onNext() }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (isLast) "Done" else "Next ›",
                        color = White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * The free-play adaptive coach bar. Shown after the brief intro walkthrough, it never gates the
 * learner — it just carries the coach's current line: the mission by default, a "why that's wrong"
 * explanation after a wrong answer, an unstuck nudge after a lull, or a well-done on a correct
 * answer. [tone] tints the accent so wrong/stuck/correct read differently at a glance.
 */
enum class CoachTone { NEUTRAL, WRONG, STUCK, CORRECT }

/** Small pill button used for Back / Replay / Continue controls in the coach bars. */
@Composable
private fun CoachChip(
    text: String,
    onClick: () -> Unit,
    filled: Boolean = false,
) {
    Text(
        text = text,
        color = if (filled) White else BrandPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) BrandPrimary else BrandPrimary.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun SimAdaptiveCoachBar(
    message: String,
    mission: String?,
    tone: CoachTone,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: (@Composable () -> Unit)? = null,
    /** Re-read the current line aloud. */
    onReplay: (() -> Unit)? = null,
    /** Step back (e.g. previous practice step). Null hides the control. */
    onBack: (() -> Unit)? = null,
    /** Move forward when the coach is waiting for the learner (null = nothing to confirm now). */
    onContinue: (() -> Unit)? = null,
) {
    val accent = when (tone) {
        CoachTone.WRONG -> androidx.compose.ui.graphics.Color(0xFFE2574C)
        CoachTone.CORRECT -> androidx.compose.ui.graphics.Color(0xFF2E9E6B)
        else -> BrandPrimary
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (avatar != null) {
                avatar()
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!mission.isNullOrBlank()) {
                    Text(
                        text = mission,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = message,
                    color = if (tone == CoachTone.NEUTRAL) TextPrimary else accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close guide",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (onBack != null || onReplay != null || onContinue != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    CoachChip(text = "‹ Back", onClick = onBack)
                    Spacer(Modifier.size(6.dp))
                }
                if (onReplay != null) {
                    CoachChip(text = "↻ Replay", onClick = onReplay)
                }
                Spacer(Modifier.weight(1f))
                if (onContinue != null) {
                    CoachChip(text = "Continue ›", onClick = onContinue, filled = true)
                }
            }
        }
    }
}
