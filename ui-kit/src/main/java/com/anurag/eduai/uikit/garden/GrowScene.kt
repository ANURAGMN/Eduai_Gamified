package com.anurag.eduai.uikit.garden

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import com.anurag.eduai.uikit.garden.quest.STEPS_PER_TASK
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.quest.SCENE_PREVIEW_PLOT_INDEX
import com.anurag.eduai.uikit.garden.quest.drawSceneItemPreview
import com.anurag.eduai.uikit.garden.quest.drawPlotShadow
import com.anurag.eduai.uikit.garden.quest.drawSceneSlotAt
import com.anurag.eduai.uikit.garden.world.swayDegrees

/** The plant or module growing on the home rail — drawn at Scene-tab scale, no terrain. */
@Composable
fun BigGrowingItem(
    theme: Theme,
    currentZone: Int,
    slot: Int,
    steps: Int,
    stepsPerTask: Int = STEPS_PER_TASK,
    time: Float,
    modifier: Modifier = Modifier,
) {
    val zoneIndex = currentZone.coerceIn(0, ZONES.lastIndex)
    val zone = ZONES[zoneIndex]
    Canvas(modifier) {
        drawSceneItemPreview {
            drawPlotShadow(theme, it)
            val g = (steps / stepsPerTask.toFloat()).coerceIn(0.06f, 1f)
            val sway = swayDegrees(time, 0f, if (theme == Theme.GARDEN) 0.9f else 0f)
            rotate(sway, Offset(it.x, it.y)) {
                drawSceneSlotAt(zone, theme, slot, it.x, it.y, g, it.scale, SCENE_PREVIEW_PLOT_INDEX, time)
            }
        }
    }
}
