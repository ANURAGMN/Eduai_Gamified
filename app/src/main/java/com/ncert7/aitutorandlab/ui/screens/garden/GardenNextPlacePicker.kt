package com.ncert7.aitutorandlab.ui.screens.garden

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.components.EduPrimaryButton
import com.anurag.eduai.uikit.components.EduSecondaryButton
import com.anurag.eduai.uikit.garden.quest.GardenSceneSnapshot
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ThemeScene
import com.anurag.eduai.uikit.garden.quest.ZONE_CAPACITY
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.quest.sceneBandAspect
import com.anurag.eduai.uikit.garden.world.rememberSceneTime
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.utils.GardenCopyFactory

/**
 * The next-place picker shown after a scene is completed (12 plants). The student can pick which
 * scene to grow next from a few unlocked candidates, or tap "Surprise me" to take the recommended one
 * — so choosing is always optional and never blocks progress.
 */
data class GardenNextPlacePickerUi(
    val completedZoneIndex: Int,
    /** The scenes offered to grow next (2–3 unlocked, not-yet-visited zones). */
    val candidateZoneIndexes: List<Int>,
    /** The default / "Surprise me" pick. */
    val recommendedZoneIndex: Int,
    val theme: Theme,
)

@Composable
fun GardenNextPlacePickerOverlay(
    picker: GardenNextPlacePickerUi?,
    languageCode: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (picker == null) return
    val colors = EduAiTheme.colors
    val homeCopy = GardenCopyFactory.homeCopy(languageCode)
    val time by rememberSceneTime(enabled = true)
    val candidates = picker.candidateZoneIndexes.ifEmpty { listOf(picker.recommendedZoneIndex) }
    var selected by remember(picker) { mutableIntStateOf(picker.recommendedZoneIndex) }
    val selectedZone = ZONES[selected.coerceIn(0, ZONES.lastIndex)]
    val selectedName = selectedZone.name(picker.theme)

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface1)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = homeCopy.nextPlaceTitle,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = homeCopy.nextPlaceSubtitle,
                color = colors.textMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )

            // Candidate scenes — tap to choose. A single candidate simply shows one card.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                candidates.forEach { zoneIndex ->
                    val zone = ZONES[zoneIndex.coerceIn(0, ZONES.lastIndex)]
                    val isSelected = zoneIndex == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface2)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.accent else colors.border,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { selected = zoneIndex }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ThemeScene(
                            state =
                                GardenSceneSnapshot(
                                    currentZone = zoneIndex,
                                    steps = 0,
                                    preferredSlot = -1,
                                    planted = emptyList(),
                                    previewSeed = 0,
                                ),
                            theme = picker.theme,
                            time = time,
                            zoneIndex = zoneIndex,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(sceneBandAspect(picker.theme))
                                    .clip(RoundedCornerShape(9.dp)),
                            band = true,
                            showPreview = false,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = zone.name(picker.theme),
                            color = if (isSelected) colors.accent else colors.text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = selectedZone.teaser(picker.theme),
                color = colors.textMuted,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "0 / $ZONE_CAPACITY",
                color = colors.accent,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(16.dp))
            EduPrimaryButton(
                text = homeCopy.growInPlace(selectedName),
                onClick = { onConfirm(selected) },
                fillMaxWidth = true,
            )
            Spacer(Modifier.height(8.dp))
            EduSecondaryButton(
                text = homeCopy.continueLabel,
                onClick = onDismiss,
                fillMaxWidth = true,
            )
        }
    }
}
