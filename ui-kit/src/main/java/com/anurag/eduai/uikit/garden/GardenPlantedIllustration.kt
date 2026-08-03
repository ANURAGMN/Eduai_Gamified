package com.anurag.eduai.uikit.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anurag.eduai.uikit.garden.quest.STEPS_PER_TASK
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.world.rememberSceneTime
import com.anurag.eduai.uikit.theme.EduAiTheme

/** Fully-grown plant or module for moment overlays. */
@Composable
fun GardenPlantedIllustration(
    zone: Int,
    slot: Int,
    theme: Theme = Theme.GARDEN,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val time by rememberSceneTime(enabled = true)
    Box(
        modifier =
            modifier
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.successBg),
    ) {
        BigGrowingItem(
            theme = theme,
            currentZone = zone,
            slot = slot,
            steps = STEPS_PER_TASK,
            stepsPerTask = STEPS_PER_TASK,
            time = time,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
