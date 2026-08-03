package com.ncert7.aitutorandlab.ui.screens.garden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.garden.CollectionShelf
import com.anurag.eduai.uikit.garden.quest.GardenSceneSnapshot
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ThemeScene
import com.anurag.eduai.uikit.components.GardenSlotPicker
import com.anurag.eduai.uikit.garden.quest.SLOTS_PER_ZONE
import com.anurag.eduai.uikit.garden.quest.starterSlot
import com.anurag.eduai.uikit.garden.quest.ZONE_CAPACITY
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.quest.placeBased
import com.anurag.eduai.uikit.garden.quest.sceneAspect
import com.anurag.eduai.uikit.garden.quest.sceneBandAspect
import com.anurag.eduai.uikit.garden.world.rememberSceneTime
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.domain.garden.GardenPlantedListRow
import com.ncert7.aitutorandlab.domain.garden.GardenProgress
import com.ncert7.aitutorandlab.ui.screens.home.GamifiedHomeMapper
import com.ncert7.aitutorandlab.utils.GardenCopyFactory
import com.ncert7.aitutorandlab.utils.GardenWorldLabels

@Composable
fun AvatarGardenSegmentBar(
    selected: AvatarGardenSegment,
    onSelect: (AvatarGardenSegment) -> Unit,
    lookHasDraft: Boolean = false,
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val homeCopy = GardenCopyFactory.homeCopy(languageCode)

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2)
            .padding(3.dp),
    ) {
        AvatarGardenSegment.entries.forEach { segment ->
            val active = selected == segment
            val label =
                when (segment) {
                    AvatarGardenSegment.Scene -> homeCopy.segmentScene
                    AvatarGardenSegment.Journey -> homeCopy.segmentJourney
                    AvatarGardenSegment.Look -> homeCopy.segmentLook
                }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.surface1 else colors.surface2)
                    .clickable { onSelect(segment) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        color = if (active) colors.text else colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    if (segment == AvatarGardenSegment.Look && lookHasDraft) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.warning)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text("*", color = colors.surface1, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarGardenGrowHint(
    theme: Theme,
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val hint = GardenCopyFactory.themeCopy(languageCode, theme).avatarGrowHint

    Text(
        text = hint,
        color = colors.textMuted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface2)
                .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun GardenPlaceThumb(
    scene: GardenSceneSnapshot,
    theme: Theme,
    zoneIndex: Int,
    time: Float,
    locked: Boolean,
    lockedLabel: String,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    cornerRadius: Dp = 8.dp,
    lockIconSize: Dp = 22.dp,
) {
    Box(
        modifier
            .width(width)
            .aspectRatio(sceneBandAspect(theme))
            .clip(RoundedCornerShape(cornerRadius)),
    ) {
        ThemeScene(
            state = scene,
            theme = theme,
            time = time,
            zoneIndex = zoneIndex,
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(if (locked) 0.65f else 1f),
            band = true,
            showPreview = false,
        )
        if (locked) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = lockedLabel,
                    tint = Color.White,
                    modifier = Modifier.size(lockIconSize),
                )
            }
        }
    }
}

@Composable
fun GardenPlaceStrip(
    scene: GardenSceneSnapshot,
    time: Float,
    unlockedZones: List<Int>,
    selectedZone: Int,
    theme: Theme,
    filledCountByZone: Map<Int, Int>,
    zoneCapacity: Int,
    languageCode: String,
    onSelectZone: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!theme.placeBased) return
    val colors = EduAiTheme.colors
    val homeCopy = GardenCopyFactory.homeCopy(languageCode)
    val themeCopy = GardenCopyFactory.themeCopy(languageCode, theme)
    val unlockedCount = unlockedZones.size

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = homeCopy.yourPlaces,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = homeCopy.placesUnlockedOf(unlockedCount, ZONES.size),
                color = colors.textMuted,
                fontSize = 9.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZONES.indices.forEach { zoneIndex ->
                val zone = ZONES[zoneIndex]
                val name = GardenWorldLabels.zoneName(zone, theme, languageCode)
                val unlocked = zoneIndex in unlockedZones
                val filled = if (unlocked) filledCountByZone[zoneIndex] ?: 0 else 0
                val selected = unlocked && zoneIndex == selectedZone
                val full = unlocked && filled >= zoneCapacity
                val unlockHint =
                    if (!unlocked && zoneIndex > 0) {
                        themeCopy.unlockAfterPlace(GardenWorldLabels.zoneName(ZONES[zoneIndex - 1], theme, languageCode))
                    } else {
                        null
                    }
                Column(
                    Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                selected -> colors.accentBg
                                unlocked -> colors.surface1
                                else -> colors.surface2
                            },
                        )
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color =
                                when {
                                    selected -> colors.accent
                                    unlocked -> colors.border
                                    else -> colors.border.copy(alpha = 0.6f)
                                },
                            shape = RoundedCornerShape(10.dp),
                        )
                        .then(
                            if (unlocked) {
                                Modifier.clickable { onSelectZone(zoneIndex) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GardenPlaceThumb(
                        scene = scene,
                        theme = theme,
                        zoneIndex = zoneIndex,
                        time = time,
                        locked = !unlocked,
                        lockedLabel = homeCopy.lockedLabel,
                        width = 60.dp,
                        cornerRadius = 7.dp,
                        lockIconSize = 18.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = name,
                        color =
                            when {
                                selected -> colors.accent
                                unlocked -> colors.text
                                else -> colors.textMuted
                            },
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                    Text(
                        text =
                            when {
                                !unlocked && !unlockHint.isNullOrBlank() -> unlockHint
                                full -> "${homeCopy.placeFull} · $filled/$zoneCapacity"
                                unlocked -> "$filled/$zoneCapacity"
                                else -> homeCopy.lockedLabel
                            },
                        color = colors.textMuted,
                        fontSize = 8.sp,
                        maxLines = 2,
                        lineHeight = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun GardenSceneSegment(
    scene: GardenSceneSnapshot,
    progress: GardenProgress,
    theme: Theme,
    plantedRows: List<GardenPlantedListRow>,
    viewingZone: Int,
    languageCode: String,
    onViewingZoneChange: (Int) -> Unit,
    onSlotSelected: (Int) -> Unit,
    showStarterPlantHighlight: Boolean = false,
    onStarterPlantHighlightSeen: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val time by rememberSceneTime(enabled = true)
    val copy = GardenCopyFactory.themeCopy(languageCode, theme)
    val homeCopy = GardenCopyFactory.homeCopy(languageCode)
    val zoneIndex = viewingZone.coerceIn(0, ZONES.lastIndex)
    val zone = ZONES[zoneIndex]
    val placeName = GardenWorldLabels.zoneName(zone, theme, languageCode).lowercase()
    val filledInView = progress.filledCountByZone[zoneIndex] ?: 0
    val rowsInView =
        if (theme.placeBased) {
            plantedRows.filter { it.zoneIndex == zoneIndex }
        } else {
            plantedRows
        }
    val slotLabels =
        if (theme.placeBased) {
            (0 until SLOTS_PER_ZONE).map { index -> GardenWorldLabels.slotName(zone, theme, index, languageCode) }
        } else {
            emptyList()
        }
    val showPlantPicker =
        theme.placeBased &&
            zoneIndex == progress.currentZone &&
            copy.pickerTitle.isNotBlank() &&
            slotLabels.size >= SLOTS_PER_ZONE

    LaunchedEffect(showPlantPicker, showStarterPlantHighlight) {
        if (showPlantPicker && showStarterPlantHighlight) {
            kotlinx.coroutines.delay(8_000)
            onStarterPlantHighlightSeen()
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        if (theme.placeBased) {
            GardenPlaceStrip(
                scene = scene,
                time = time,
                unlockedZones = progress.unlockedZones,
                selectedZone = zoneIndex,
                theme = theme,
                filledCountByZone = progress.filledCountByZone,
                zoneCapacity = progress.zoneCapacity,
                languageCode = languageCode,
                onSelectZone = onViewingZoneChange,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        if (showPlantPicker) {
            GardenSlotPicker(
                title = copy.pickerTitle,
                theme = theme,
                zoneIndex = zoneIndex,
                labels = slotLabels,
                selectedSlot = scene.preferredSlot,
                previewSlot = scene.slot,
                surpriseLabel = homeCopy.surpriseLabel,
                surprisePreview = homeCopy.surprisePreview,
                onSlotSelected = onSlotSelected,
                highlightSlot = if (showStarterPlantHighlight) theme.starterSlot() else null,
                highlightBadge = if (showStarterPlantHighlight) homeCopy.starterPlantBadge else null,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(colors.surface2)
                .padding(10.dp),
        ) {
            ThemeScene(
                state = scene,
                theme = theme,
                time = time,
                zoneIndex = zoneIndex,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(sceneAspect(theme))
                        .clip(RoundedCornerShape(12.dp)),
                cover = true,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                val title =
                    if (theme.placeBased) placeName else copy.placeCollection
                val count =
                    if (theme.placeBased) {
                        "$filledInView / $ZONE_CAPACITY"
                    } else {
                        "${progress.totalPlanted}"
                    }
                Text(
                    text = title.replaceFirstChar { it.titlecase() },
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(text = count, color = colors.textMuted, fontSize = 11.sp)
            }
        }

        val collectionShelf =
            GamifiedHomeMapper.mapCollectionShelf(
                progress = progress,
                planted = scene.planted,
                languageCode = languageCode,
            )
        Spacer(Modifier.height(8.dp))
        CollectionShelf(state = collectionShelf)

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(colors.surface2)
                .padding(12.dp),
        ) {
            Text(
                text = copy.sceneListTitle,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (rowsInView.isEmpty()) {
                Text(
                    text = homeCopy.nothingPlantedYet,
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                rowsInView.forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(
                            text = row.conceptLabel,
                            color = colors.text,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${row.slotLabel} · ${row.kindLabel}",
                            color = colors.textMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GardenJourneySegment(
    scene: GardenSceneSnapshot,
    progress: GardenProgress?,
    theme: Theme,
    themeNote: String?,
    onChooseTheme: (Theme) -> Unit,
    onDismissNote: () -> Unit,
    onPlaceSelected: (Int) -> Unit,
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val time by rememberSceneTime(enabled = true)
    val homeCopy = GardenCopyFactory.homeCopy(languageCode)

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(colors.surface2)
                .padding(12.dp),
        ) {
            Text(
                text = homeCopy.journeyTitle,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = homeCopy.journeySubtitle,
                color = colors.textMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        AnimatedVisibility(
            visible = !themeNote.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.successBg)
                        .border(1.dp, colors.success.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onDismissNote() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✓", color = colors.success, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Text(themeNote.orEmpty(), color = colors.text, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        Theme.entries.chunked(2).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { th ->
                    val chosen = theme == th
                    val copy = GardenCopyFactory.themeCopy(languageCode, th)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface2)
                            .border(
                                width = if (chosen) 2.dp else 0.dp,
                                color = if (chosen) colors.accent else colors.surface2,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onChooseTheme(th) }
                            .padding(6.dp),
                    ) {
                        ThemeScene(
                            state = scene,
                            theme = th,
                            time = time,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        if (th.placeBased) sceneBandAspect(th) else sceneAspect(th),
                                    )
                                    .clip(RoundedCornerShape(9.dp)),
                            band = th.placeBased,
                            showPreview = false,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = copy.placeCollection,
                                color = colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            if (chosen) {
                                Text(
                                    text = homeCopy.chosen,
                                    color = colors.accent,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        Text(
                            text = copy.pitch,
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (theme.placeBased && progress != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = homeCopy.yourPlaces,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = homeCopy.placesUnlockedOf(progress.unlockedZones.size, ZONES.size),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            val themeCopy = GardenCopyFactory.themeCopy(languageCode, theme)
            ZONES.indices.forEach { zoneIndex ->
                val zone = ZONES[zoneIndex]
                val copy = themeCopy
                val unlocked = zoneIndex in progress.unlockedZones
                val filled = if (unlocked) progress.filledCountByZone[zoneIndex] ?: 0 else 0
                val full = unlocked && filled >= progress.zoneCapacity
                val isCurrent = unlocked && zoneIndex == progress.currentZone
                val unlockHint =
                    if (!unlocked && zoneIndex > 0) {
                        copy.unlockAfterPlace(ZONES[zoneIndex - 1].name(theme))
                    } else {
                        null
                    }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface2)
                        .border(
                            width = if (isCurrent) 1.5.dp else 0.dp,
                            color = if (isCurrent) colors.accent else colors.surface2,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .then(
                            if (unlocked) {
                                Modifier.clickable { onPlaceSelected(zoneIndex) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GardenPlaceThumb(
                        scene = scene,
                        theme = theme,
                        zoneIndex = zoneIndex,
                        time = time,
                        locked = !unlocked,
                        lockedLabel = homeCopy.lockedLabel,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = zone.name(theme),
                            color = if (unlocked) colors.text else colors.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = zone.teaser(theme),
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                        )
                        Text(
                            text =
                                when {
                                    !unlocked && !unlockHint.isNullOrBlank() -> unlockHint
                                    full -> "${homeCopy.placeFull} · $filled/${progress.zoneCapacity}"
                                    unlocked -> "$filled/${progress.zoneCapacity} · ${copy.item.lowercase()}s"
                                    else -> homeCopy.lockedLabel
                                },
                            color = if (isCurrent) colors.accent else colors.textMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    if (unlocked) {
                        Text(
                            text = homeCopy.viewInScene,
                            color = colors.accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = homeCopy.lockedLabel,
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
