package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.garden.CollectionShelf
import com.anurag.eduai.uikit.garden.CollectionShelfState
import com.anurag.eduai.uikit.garden.BigGrowingItem
import com.anurag.eduai.uikit.garden.quest.ColonyProgress
import com.anurag.eduai.uikit.garden.quest.IslandProgress
import com.anurag.eduai.uikit.garden.quest.SlotThumb
import com.anurag.eduai.uikit.garden.quest.SurpriseThumb
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.STEPS_PER_TASK
import com.anurag.eduai.uikit.garden.world.rememberSceneTime
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme

/** Read-only home-rail model — steps come from trial progress, not manual taps. */
data class GardenRailState(
    val sectionTitle: String,
    val openLabel: String,
    val conceptTitle: String,
    val subtitle: String,
    val steps: Int,
    val stepsPerPlant: Int,
    val artSteps: Int = steps,
    val statusLine: String,
    val hintLine: String,
    val currentZone: Int,
    val slot: Int,
    val preferredSlot: Int = -1,
    val slotLabels: List<String> = emptyList(),
    val theme: Theme = Theme.GARDEN,
    val ready: Boolean = steps >= stepsPerPlant,
    val highlightNewPlant: Boolean = false,
    val highlightStarterPlant: Boolean = false,
    val celebrationLine: String? = null,
    val growNudgeLine: String? = null,
    val slotPickerTitle: String = "",
    val surpriseLabel: String = "Surprise",
    val surprisePreview: (String) -> String = { name -> "You'll get a ${name.lowercase()}" },
    /** Tasks finished — drives Island / Space-colony scene previews on the home rail. */
    val totalPlanted: Int = 0,
    val collection: CollectionShelfState? = null,
)

enum class GrowRailVariant { Compact, Tall }

/**
 * Compact home rail: art on the left, progress on the right. Read-only in Eduapp — no step/plant
 * buttons; learning increments drive growth via [GardenRepository.recordStep].
 */
@Composable
fun GrowRail(
    state: GardenRailState,
    onOpenWorld: () -> Unit = {},
    onRailClick: (() -> Unit)? = null,
    variant: GrowRailVariant = GrowRailVariant.Compact,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val time by rememberSceneTime(enabled = true)
    val pulseActive = state.highlightNewPlant || state.highlightStarterPlant
    val highlightPulse =
        if (pulseActive) {
            val transition = rememberInfiniteTransition(label = "gardenHighlight")
            transition.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "gardenHighlightAlpha",
            ).value
        } else {
            1f
        }

    Column(modifier.fillMaxWidth()) {
        SectionHeader(
            title = state.sectionTitle,
            seeAllLabel = state.openLabel,
            onSeeAllClick = onOpenWorld,
        )

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(colors.surface2)
                .then(
                    if (onRailClick != null) {
                        Modifier.clickable { onRailClick() }
                    } else {
                        Modifier
                    },
                )
                .border(
                    width =
                        when {
                            state.highlightNewPlant -> 2.dp
                            state.highlightStarterPlant -> 2.dp
                            state.ready -> 1.5.dp
                            else -> 1.dp
                        },
                    color =
                        when {
                            state.highlightNewPlant ->
                                colors.success.copy(alpha = highlightPulse)
                            state.highlightStarterPlant ->
                                colors.accent.copy(alpha = highlightPulse)
                            state.ready -> colors.success
                            else -> colors.border
                        },
                    shape = RoundedCornerShape(EduAiDimens.cardRadius),
                )
                .padding(10.dp),
        ) {
            val art = (maxWidth * 0.30f).coerceIn(88.dp, 168.dp)
            val band = (maxWidth * 0.42f).coerceIn(120.dp, 240.dp)
            val artBg =
                when (state.theme) {
                    Theme.COLONY -> Color(0xFF2A1E18)
                    Theme.OUTPOST -> Color(0xFF20263C)
                    Theme.ISLAND -> Color(0xFFD7EFE3)
                    Theme.GARDEN -> colors.successBg
                }

            if (variant == GrowRailVariant.Compact) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(art)
                            .clip(RoundedCornerShape(10.dp))
                            .background(artBg),
                    ) {
                        GrowRailArt(state = state, time = time, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { GrowRailDetails(state, highlightPulse) }
                }
            } else {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(band)
                            .clip(RoundedCornerShape(10.dp))
                            .background(artBg),
                    ) {
                        GrowRailArt(state = state, time = time, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(10.dp))
                    GrowRailDetails(state, highlightPulse)
                }
            }
        }

        state.collection?.let { collection ->
            Spacer(Modifier.height(8.dp))
            CollectionShelf(
                state = collection,
                onOpenCollection = onOpenWorld,
            )
        }
    }
}

@Composable
private fun GrowRailArt(
    state: GardenRailState,
    time: Float,
    modifier: Modifier = Modifier,
) {
    val partial =
        (state.artSteps / state.stepsPerPlant.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
    when (state.theme) {
        Theme.ISLAND ->
            IslandProgress(
                finished = state.totalPlanted,
                partial = partial,
                modifier = modifier,
                time = time,
                cover = true,
            )
        Theme.COLONY ->
            ColonyProgress(
                finished = state.totalPlanted,
                partial = partial,
                modifier = modifier,
                time = time,
                cover = true,
            )
        Theme.GARDEN, Theme.OUTPOST ->
            BigGrowingItem(
                theme = state.theme,
                currentZone = state.currentZone,
                slot = state.slot,
                steps = state.artSteps,
                stepsPerTask = state.stepsPerPlant.coerceAtLeast(STEPS_PER_TASK),
                time = time,
                modifier = modifier,
            )
    }
}

private const val SLOTS_PER_ZONE = 6

/** Visual grid for choosing what to grow next — shown on the garden scene, not the home summary. */
@Composable
fun GardenSlotPicker(
    title: String,
    theme: Theme,
    zoneIndex: Int,
    labels: List<String>,
    selectedSlot: Int,
    previewSlot: Int,
    surpriseLabel: String,
    surprisePreview: (String) -> String,
    onSlotSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightSlot: Int? = null,
    highlightBadge: String? = null,
) {
    val colors = EduAiTheme.colors
    val options = listOf(-1) + labels.indices.toList()
    val highlightPulse =
        if (highlightSlot != null) {
            val transition = rememberInfiniteTransition(label = "starterPlantHighlight")
            transition.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "starterPlantHighlightAlpha",
            ).value
        } else {
            1f
        }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        options.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { slotIndex ->
                    val selected =
                        if (slotIndex < 0) {
                            selectedSlot < 0
                        } else {
                            selectedSlot == slotIndex
                        }
                    val label =
                        if (slotIndex < 0) {
                            surpriseLabel
                        } else {
                            labels[slotIndex]
                        }
                    val highlighted = slotIndex >= 0 && slotIndex == highlightSlot
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) colors.accentBg else colors.surface1)
                                .border(
                                    width =
                                        when {
                                            highlighted -> 2.dp
                                            selected -> 1.5.dp
                                            else -> 1.dp
                                        },
                                    color =
                                        when {
                                            highlighted ->
                                                colors.accent.copy(alpha = highlightPulse)
                                            selected -> colors.accent
                                            else -> colors.border
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onSlotSelected(slotIndex) }
                                .padding(6.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (slotIndex < 0) {
                                SurpriseThumb(
                                    zoneIndex = zoneIndex,
                                    theme = theme,
                                    modifier = Modifier.fillMaxSize(0.88f),
                                )
                            } else {
                                SlotThumb(
                                    zoneIndex = zoneIndex,
                                    theme = theme,
                                    slot = slotIndex,
                                    modifier = Modifier.fillMaxSize(0.88f),
                                )
                            }
                            if (highlighted && !highlightBadge.isNullOrBlank()) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.accent)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = highlightBadge,
                                        color = colors.onAccent,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = if (selected) colors.accent else colors.textSecondary,
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (selectedSlot < 0) {
            Text(
                text = surprisePreview(labels.getOrElse(previewSlot) { "plant" }),
                color = colors.textMuted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun GrowRailDetails(state: GardenRailState, highlightPulse: Float = 1f) {
    val colors = EduAiTheme.colors

    if (state.highlightNewPlant && !state.celebrationLine.isNullOrBlank()) {
        Text(
            text = state.celebrationLine,
            color = colors.success.copy(alpha = highlightPulse),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
    } else if (state.highlightStarterPlant && !state.growNudgeLine.isNullOrBlank()) {
        Text(
            text = state.growNudgeLine,
            color = colors.accent.copy(alpha = highlightPulse),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
    } else if (!state.growNudgeLine.isNullOrBlank()) {
        Text(
            text = state.growNudgeLine,
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
    }

    Text(
        text = state.conceptTitle,
        color = colors.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 17.sp,
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = state.subtitle,
        color = colors.textSecondary,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    Spacer(Modifier.height(8.dp))
    if (!(state.highlightNewPlant && state.steps == 0)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(state.stepsPerPlant) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(EduAiDimens.progressHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i < state.steps) colors.success else colors.border),
                )
            }
        }
        Spacer(Modifier.height(9.dp))
    }
    Text(
        text = state.hintLine,
        color = colors.textSecondary,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )

    Spacer(Modifier.height(7.dp))
    Text(
        text = state.statusLine,
        color = colors.textMuted,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
