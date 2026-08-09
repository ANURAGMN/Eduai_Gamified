package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (avatar != null) {
                avatar()
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step $stepNumber of $totalSteps",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier.height(2.dp))
                Text(
                    text = instruction,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CoachIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close guide",
                onClick = onClose,
            )
        }

        Spacer(modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                CoachChip(text = "‹ Back", onClick = onBack)
                Spacer(modifier.size(6.dp))
            }
            if (onReplay != null) {
                CoachIconButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Replay",
                    onClick = onReplay,
                    tint = BrandPrimary,
                    filled = true,
                )
                Spacer(modifier.size(6.dp))
            }
            if (requireAction) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier.size(6.dp))
                Text(
                    text = "Tap the glowing control above",
                    color = BrandPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier.size(8.dp))
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
                Spacer(modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BrandPrimary)
                        .clickable { onNext() }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
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

private val HINT_MODES = listOf(
    "ask" to "Try first",
    "guided" to "Step-by-step",
    "self" to "Self-explain",
    "ondemand" to "Answer on tap",
)

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
private fun CoachIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = TextSecondary,
    filled: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (filled) BrandPrimary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
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
    /** Open the detailed explanation panel (page-side). Null hides the chip. */
    onExplain: (() -> Unit)? = null,
    /** Current hint model id ("ask" | "guided" | "self" | "ondemand"). */
    hintMode: String = "ask",
    /** Advance the hint (nudge → reveal). Null hides the chip. */
    onHint: (() -> Unit)? = null,
    /** Change the hint model. Null hides the mode selector. */
    onHintModeChange: ((String) -> Unit)? = null,
    /**
     * When true, collapse to a slim strip so the in-page Explain panel isn't stacked under a
     * full coach card (Compose sits above the WebView).
     */
    explainOpen: Boolean = false,
    /** Called when the learner taps the slim "Explaining…" strip to dismiss / return focus. */
    onDismissExplain: (() -> Unit)? = null,
) {
    val accent = when (tone) {
        CoachTone.WRONG -> androidx.compose.ui.graphics.Color(0xFFE2574C)
        CoachTone.CORRECT -> androidx.compose.ui.graphics.Color(0xFF2E9E6B)
        else -> BrandPrimary
    }
    var modePickerOpen by remember { mutableStateOf(false) }
    val modeLabel = HINT_MODES.firstOrNull { it.first == hintMode }?.second ?: "Coach"
    val hintLabel = if (hintMode == "ondemand") "Show answer" else "Hint"

    if (explainOpen) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .clickable { onDismissExplain?.invoke() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier.size(8.dp))
            Text(
                text = "Explaining — tap here when done",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            CoachIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close explanation",
                onClick = { onDismissExplain?.invoke() },
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (avatar != null) {
                Box(modifier = Modifier.size(32.dp)) { avatar() }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!mission.isNullOrBlank()) {
                    Text(
                        text = mission,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier.height(2.dp))
                }
                Text(
                    text = message,
                    color = if (tone == CoachTone.NEUTRAL) TextPrimary else accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CoachIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close guide",
                onClick = onClose,
            )
        }

        val showActions =
            onBack != null || onReplay != null || onContinue != null || onExplain != null || onHint != null || onHintModeChange != null
        if (showActions) {
            Spacer(modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (onHint != null && hintMode != "guided") {
                    CoachChip(text = hintLabel, onClick = onHint, filled = true)
                    Spacer(modifier.size(6.dp))
                }
                if (onReplay != null) {
                    CoachIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Replay",
                        onClick = onReplay,
                        tint = BrandPrimary,
                        filled = true,
                    )
                    Spacer(modifier.size(4.dp))
                }
                if (onExplain != null) {
                    CoachIconButton(
                        icon = Icons.Default.Info,
                        contentDescription = "Explain",
                        onClick = onExplain,
                        tint = BrandPrimary,
                        filled = true,
                    )
                    Spacer(modifier.size(4.dp))
                }
                if (onBack != null) {
                    CoachChip(text = "‹", onClick = onBack)
                    Spacer(modifier.size(4.dp))
                }

                Spacer(modifier.weight(1f))

                if (onHintModeChange != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandPrimary.copy(alpha = 0.10f))
                            .border(
                                width = 1.dp,
                                color = BrandPrimary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { modePickerOpen = !modePickerOpen }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = modeLabel,
                            color = BrandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 110.dp),
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = if (modePickerOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (modePickerOpen) "Hide coach modes" else "Change coach mode",
                            tint = BrandPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (onContinue != null) {
                    Spacer(modifier.size(6.dp))
                    CoachChip(text = "Continue ›", onClick = onContinue, filled = true)
                }
            }

            AnimatedVisibility(
                visible = modePickerOpen && onHintModeChange != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    HINT_MODES.forEach { (id, label) ->
                        CoachChip(
                            text = label,
                            onClick = {
                                onHintModeChange?.invoke(id)
                                modePickerOpen = false
                            },
                            filled = id == hintMode,
                        )
                        Spacer(modifier.size(6.dp))
                    }
                }
            }
        }
    }
}

/**
 * Floating coach (Layout C): the sim fills the screen; the coach is a small round bubble + a slim
 * one-line peek at the bottom, plus a floating Hint / Show-answer button. Tapping the bubble/peek
 * expands a popover with the full line, Explain, Replay, voice toggle and the coaching-style chips.
 * Hidden while the page Explain sheet is open (so it doesn't cover it).
 */
@Composable
fun SimFloatingCoach(
    message: String,
    hintMode: String,
    voiceEnabled: Boolean,
    onVoiceChange: (Boolean) -> Unit,
    onHint: () -> Unit,
    onHintModeChange: (String) -> Unit,
    onExplain: () -> Unit,
    explainOpen: Boolean,
    onReplay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (explainOpen) return
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 10.dp, bottom = 74.dp)
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(0.5.dp, BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text(text = message, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoachChip(text = "ⓘ Explain", onClick = onExplain)
                    if (onReplay != null) {
                        Spacer(Modifier.size(6.dp))
                        CoachIconButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Replay",
                            onClick = onReplay,
                            tint = BrandPrimary,
                            filled = true,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (voiceEnabled) "Voice on" else "Voice off",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onVoiceChange(!voiceEnabled) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(text = "Coaching style", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(
                        "ask" to "Try first",
                        "guided" to "Step-by-step",
                        "self" to "Self-explain",
                        "ondemand" to "Answer on tap",
                    ).forEach { (id, label) ->
                        CoachChip(text = label, onClick = { onHintModeChange(id) }, filled = id == hintMode)
                        Spacer(Modifier.size(6.dp))
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.Lightbulb,
                    contentDescription = "Coach",
                    tint = White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = message,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBackground)
                    .border(0.5.dp, BrandPrimary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            if (hintMode != "guided") {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (hintMode == "ondemand") "Show answer" else "Hint",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(BrandPrimary)
                        .clickable { onHint() }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}
