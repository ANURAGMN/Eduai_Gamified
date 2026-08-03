package com.anurag.eduai.uikit.garden.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import com.anurag.eduai.uikit.garden.quest.SCENE_PREVIEW_PLOT_INDEX
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.drawPlotShadow
import com.anurag.eduai.uikit.garden.quest.drawSceneItemPreview
import kotlin.math.hypot
import kotlin.math.sin

private const val SPROUT_X = 288f
private const val SPROUT_Y = 312f

@Composable
fun GardenScene(state: GardenState, time: Float, modifier: Modifier = Modifier) {
    val habitat = state.place.habitat
    val paths = remember(habitat.key) { TerrainPaths(habitat) }
    val skyBrush = remember(habitat.key) {
        Brush.verticalGradient(
            listOf(habitat.skyTop, habitat.skyBottom),
            startY = 0f,
            endY = SCENE_H
        )
    }

    Canvas(
        modifier = modifier.pointerInput(state.placeIndex, state.mode, state.pendingTask, state.movingFrom) {
            detectTapGestures { offset ->
                val unit = size.width / SCENE_W
                onSceneTap(state, offset.x / unit, offset.y / unit)
            }
        }
    ) {
        val unit = size.width / SCENE_W
        scale(unit, unit, pivot = Offset.Zero) {
            drawTerrain(habitat, paths, skyBrush)
            drawAmbientBack(habitat, time)
            drawPlots(state, time)
            drawDecorations(state)
            drawHeritageTree(46f, 300f, state.heritageStage, time)
            drawSprout(SPROUT_X, SPROUT_Y, time)
            drawAmbientFront(habitat, time, state.totalPlants)
            drawForeground(habitat)
        }
    }
}

private fun DrawScope.drawPlots(state: GardenState, time: Float) {
    val place = state.place
    val occupied = place.plants.associateBy { it.plot }
    val free = place.firstFreePlot()
    val active = place.activeTask()
    val awaitingPlot = state.mode == Mode.PLANT && state.pendingTask >= 0
    val moving = state.mode == Mode.MOVE

    PLOTS.forEachIndexed { index, plot ->
        val plant = occupied[index]
        when {
            plant != null -> {
                val phase = hash(index, 3) * 6.283f
                val sway = sin(time * 1.15f + phase) * 0.85f
                rotate(sway, Offset(plot.x, plot.y)) {
                    drawSpecies(SpeciesTable[plant.species], plot.x, plot.y, 1f, plot.scale, index)
                }
                val highlighted = state.selectedPlot == index || (moving && state.movingFrom == index)
                if (highlighted) {
                    drawCircle(
                        color = if (moving && state.movingFrom == index) Color(0xFFEF9F27) else Color(0xFF0F6E56),
                        radius = 30f * plot.scale,
                        center = Offset(plot.x, plot.y - 26f * plot.scale),
                        style = Stroke(2.4f)
                    )
                }
            }

            awaitingPlot || (moving && state.movingFrom >= 0) -> {
                val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                val pulse = 0.45f + 0.4f * kotlin.math.abs(sin(time * 2.2f))
                drawOval(
                    color = Color(0xFF0F6E56).copy(alpha = pulse),
                    topLeft = Offset(plot.x - 20f * plot.scale, plot.y - 8f * plot.scale),
                    size = Size(40f * plot.scale, 16f * plot.scale),
                    style = Stroke(2f, pathEffect = dash)
                )
                drawCircle(Color(0xFF0F6E56), 3f * plot.scale, Offset(plot.x, plot.y - 10f * plot.scale))
            }

            index == free && active >= 0 && place.tasks[active].steps > 0 -> {
                val g = place.tasks[active].steps / TaskState.STEPS_PER_TASK.toFloat()
                val sway = sin(time * 1.4f) * 0.85f
                rotate(sway, Offset(plot.x, plot.y)) {
                    drawSpecies(SpeciesTable[state.species], plot.x, plot.y, g, plot.scale, index)
                }
            }

            else -> drawOval(
                Color.Black.copy(alpha = 0.05f),
                Offset(plot.x - 13f * plot.scale, plot.y - 4.5f * plot.scale),
                Size(26f * plot.scale, 9f * plot.scale)
            )
        }
    }
}

private fun DrawScope.drawDecorations(state: GardenState) {
    val placed = state.place.decor.associateBy { it.slot }
    DECOR_SLOTS.forEachIndexed { index, slot ->
        val decor = placed[index]
        if (decor != null) {
            drawDecor(decor.kind, slot.x, slot.y, slot.scale)
        } else if (state.mode == Mode.DECORATE) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            drawRoundRect(
                color = Color(0xFF854F0B).copy(alpha = 0.7f),
                topLeft = Offset(slot.x - 14f * slot.scale, slot.y - 20f * slot.scale),
                size = Size(28f * slot.scale, 24f * slot.scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f),
                style = Stroke(1.8f, pathEffect = dash)
            )
        }
    }
}

private fun nearest(points: List<Plot>, x: Float, y: Float, radius: Float): Int {
    var best = -1
    var bestDist = radius
    points.forEachIndexed { i, p ->
        val d = hypot(p.x - x, p.y - y - 12f)
        if (d < bestDist) {
            bestDist = d
            best = i
        }
    }
    return best
}

private fun onSceneTap(state: GardenState, x: Float, y: Float) {
    if (hypot(x - SPROUT_X, y - (SPROUT_Y - 14f)) < 26f) {
        state.sproutLine = (state.sproutLine + 1) % SPROUT_LINES.size
        return
    }

    if (state.mode == Mode.DECORATE) {
        val slot = nearest(DECOR_SLOTS, x, y, 34f)
        if (slot >= 0) {
            val existing = state.place.decor.any { it.slot == slot }
            if (existing) state.removeDecor(slot) else state.addDecor(slot)
        }
        return
    }

    val plot = nearest(PLOTS, x, y, 34f)
    if (plot < 0) return
    val occupied = state.place.plants.any { it.plot == plot }

    when (state.mode) {
        Mode.MOVE -> {
            if (occupied) state.movingFrom = if (state.movingFrom == plot) -1 else plot
            else if (state.movingFrom >= 0) state.movePlant(plot)
        }

        Mode.PLANT -> {
            if (state.pendingTask >= 0 && !occupied) state.placeAt(plot)
            else if (occupied) state.selectedPlot = if (state.selectedPlot == plot) -1 else plot
        }

        Mode.DECORATE -> Unit
    }
}

/** Small thumbnail used by the species picker — Scene-tab scale. */
@Composable
fun SpeciesThumb(speciesKey: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawSceneItemPreview {
            drawPlotShadow(Theme.GARDEN, it)
            drawSpecies(SpeciesTable[speciesKey], it.x, it.y, 1f, it.scale, SCENE_PREVIEW_PLOT_INDEX)
        }
    }
}
