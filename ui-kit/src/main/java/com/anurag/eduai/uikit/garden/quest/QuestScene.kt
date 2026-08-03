package com.anurag.eduai.uikit.garden.quest

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.anurag.eduai.uikit.garden.world.ISLAND_CELLS
import com.anurag.eduai.uikit.garden.world.ISLAND_H
import com.anurag.eduai.uikit.garden.world.ISLAND_W
import com.anurag.eduai.uikit.garden.world.IslandScene
import com.anurag.eduai.uikit.garden.world.IslandState
import com.anurag.eduai.uikit.garden.world.PLOTS
import com.anurag.eduai.uikit.garden.world.SCENE_H
import com.anurag.eduai.uikit.garden.world.SCENE_W
import com.anurag.eduai.uikit.garden.world.SpeciesTable
import com.anurag.eduai.uikit.garden.world.TOTAL_TIER_TASKS
import com.anurag.eduai.uikit.garden.world.TerrainPaths
import com.anurag.eduai.uikit.garden.world.TiersScene
import com.anurag.eduai.uikit.garden.world.TiersState
import com.anurag.eduai.uikit.garden.world.World
import com.anurag.eduai.uikit.garden.world.drawForeground
import com.anurag.eduai.uikit.garden.world.drawModule
import com.anurag.eduai.uikit.garden.world.drawSpecies
import com.anurag.eduai.uikit.garden.world.drawTerrain
import com.anurag.eduai.uikit.garden.world.hash
import com.anurag.eduai.uikit.garden.world.shade
import com.anurag.eduai.uikit.garden.world.swayDegrees

/** The band on home shows the lower part of the scene; the full view shows all of it. */
const val BAND_TOP = 150f
const val BAND_H = SCENE_H - BAND_TOP

private fun DrawScope.outpostTerrain(w: World, seed: Int) {
    drawRect(
        Brush.verticalGradient(listOf(w.skyTop, w.skyBottom), startY = 0f, endY = SCENE_H),
        size = Size(SCENE_W, SCENE_H),
    )
    // Soft atmospheric glow along the horizon — adds depth and a more vibrant sky.
    drawCircle(w.light.copy(alpha = 0.12f), SCENE_W * 0.75f, Offset(SCENE_W * 0.5f, 158f))
    for (i in 0 until 40) {
        val x = 6f + hash(seed, i) * 320f
        val y = 6f + hash(seed, i + 50) * 130f
        drawCircle(
            Color.White.copy(alpha = 0.35f + hash(seed, i + 90) * 0.5f),
            0.8f + hash(seed, i + 20),
            Offset(x, y),
        )
    }
    // A big, vibrant parent world in the sky (was a tiny pale disc). Uses the world's own accent
    // colours with a radial gradient + glow so each planet reads as bright and distinct.
    val planetCenter = Offset(272f, 60f)
    val planetR = 44f
    drawCircle(w.light.copy(alpha = 0.32f), planetR * 1.6f, planetCenter)
    drawCircle(w.light.copy(alpha = 0.18f), planetR * 2.3f, planetCenter)
    drawCircle(
        Brush.radialGradient(
            colors = listOf(w.light, w.trim, shade(w.trim, -0.38f)),
            center = Offset(planetCenter.x - planetR * 0.35f, planetCenter.y - planetR * 0.35f),
            radius = planetR * 1.5f,
        ),
        radius = planetR,
        center = planetCenter,
    )
    // A slim highlight crescent for a little extra pop.
    drawCircle(
        Color.White.copy(alpha = 0.25f),
        planetR * 0.34f,
        Offset(planetCenter.x - planetR * 0.42f, planetCenter.y - planetR * 0.42f),
    )
    drawPath(
        Path().apply { moveTo(-14f, 146f); quadraticBezierTo(64f, 100f, 148f, 146f); close() },
        shade(w.ground[2], -0.14f),
    )
    drawPath(
        Path().apply { moveTo(118f, 150f); quadraticBezierTo(210f, 94f, 306f, 150f); close() },
        shade(w.ground[2], -0.24f),
    )
    listOf(150f, 214f, 286f).forEachIndexed { k, y ->
        drawPath(
            Path().apply {
                moveTo(-6f, y)
                quadraticBezierTo(58f + k * 14f, y - 13f, 138f + k * 10f, y)
                quadraticBezierTo(218f + k * 6f, y + 13f, 338f, y - 7f)
                lineTo(338f, SCENE_H)
                lineTo(-6f, SCENE_H)
                close()
            },
            w.ground[k],
        )
    }
    for (i in 0 until 18) {
        val x = 8f + hash(seed + 3, i) * 316f
        val y = 168f + hash(seed + 3, i + 40) * 170f
        val s = 0.6f + hash(seed + 3, i + 80) * 0.7f
        drawOval(shade(w.ground[1], -0.12f), Offset(x - 9f * s, y - 3f * s), Size(18f * s, 6f * s))
    }
}

internal fun DrawScope.drawSceneSlotAt(
    zone: Zone,
    theme: Theme,
    slot: Int,
    x: Float,
    y: Float,
    growth: Float,
    scale: Float,
    seed: Int,
    time: Float,
) {
    if (theme == Theme.GARDEN) {
        drawSpecies(SpeciesTable[zone.speciesKey(slot)], x, y, growth, scale, seed)
    } else {
        // Space modules read too small — enlarge them (bigger base + ~1.6× overall) so the Mars/space
        // components are clearly visible.
        drawModule(zone.moduleKind(slot), zone.world, x, y, scale * (0.5f + 0.6f * growth) * 1.6f, time)
    }
}

private fun DrawScope.zoneScene(
    state: GardenSceneSnapshot,
    zoneIndex: Int,
    theme: Theme,
    time: Float,
    showPreview: Boolean,
    terrain: TerrainPaths?,
) {
    val zone = ZONES[zoneIndex]
    if (theme == Theme.GARDEN && terrain != null) {
        drawTerrain(
            zone.habitat,
            terrain,
            Brush.verticalGradient(
                listOf(zone.habitat.skyTop, zone.habitat.skyBottom),
                startY = 0f,
                endY = SCENE_H,
            ),
        )
    } else {
        outpostTerrain(zone.world, zoneIndex * 7 + 3)
    }

    val items = state.plantedIn(zoneIndex).associateBy { it.plot }
    val nextPlot = (0 until ZONE_CAPACITY).firstOrNull { it !in items } ?: -1

    PLOTS.forEachIndexed { index, plot ->
        val item = items[index]
        when {
            item != null -> {
                val sway = swayDegrees(time, hash(index, 3) * 6.28f, 0.72f)
                rotate(sway, Offset(plot.x, plot.y)) {
                    drawSceneSlotAt(zone, theme, item.slot, plot.x, plot.y, 1f, plot.scale, index, time)
                }
            }

            showPreview && index == nextPlot && state.steps > 0 && zoneIndex == state.currentZone -> {
                val g = state.steps / STEPS_PER_TASK.toFloat()
                val sway = swayDegrees(time, 0f, 0.9f)
                rotate(sway, Offset(plot.x, plot.y)) {
                    drawSceneSlotAt(zone, theme, state.slot, plot.x, plot.y, g, plot.scale, index, time)
                }
            }

            else ->
                drawOval(
                    if (theme == Theme.GARDEN) Color.Black.copy(alpha = 0.05f)
                    else Color.White.copy(alpha = 0.06f),
                    Offset(plot.x - 12f * plot.scale, plot.y - 4f * plot.scale),
                    Size(24f * plot.scale, 8f * plot.scale),
                )
        }
    }

    if (theme == Theme.GARDEN) drawForeground(zone.habitat)
}

@Composable
fun ZoneScene(
    state: GardenSceneSnapshot,
    zoneIndex: Int,
    time: Float,
    band: Boolean,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true,
    theme: Theme = Theme.GARDEN,
    cover: Boolean = false,
) {
    val zone = ZONES[zoneIndex]
    val terrain = remember(zone.habitat.key) { TerrainPaths(zone.habitat) }
    Canvas(modifier) {
        val unit =
            when {
                cover -> maxOf(size.width / SCENE_W, size.height / SCENE_H)
                band -> minOf(size.width / SCENE_W, size.height / BAND_H)
                else -> size.width / SCENE_W
            }
        val originX =
            when {
                cover || band -> (size.width - SCENE_W * unit) / 2f
                else -> 0f
            }
        val originY =
            when {
                cover -> size.height - SCENE_H * unit
                band -> size.height - BAND_H * unit
                else -> 0f
            }
        translate(originX, originY) {
            scale(unit, unit, pivot = Offset.Zero) {
                translate(0f, if (band && !cover) -BAND_TOP else 0f) {
                    zoneScene(state, zoneIndex, theme, time, showPreview, terrain)
                }
            }
        }
    }
}

@Composable
fun IslandProgress(
    finished: Int,
    partial: Float = 0f,
    modifier: Modifier = Modifier,
    time: Float = 0f,
    cover: Boolean = false,
    focusSpan: Float = 0f,
) {
    val island = remember { IslandState() }
    LaunchedEffect(finished) {
        island.day = finished.coerceIn(0, ISLAND_CELLS.size)
        island.view = island.day
        island.selected = -1
    }
    LaunchedEffect(partial) { island.partial = partial }
    IslandScene(island, time, modifier, cover, focusSpan)
}

@Composable
fun ColonyProgress(
    finished: Int,
    partial: Float = 0f,
    modifier: Modifier = Modifier,
    time: Float = 0f,
    cover: Boolean = false,
    focusSpan: Float = 0f,
) {
    val tiers = remember { TiersState() }
    LaunchedEffect(finished) { tiers.applyTaskCount(finished) }
    LaunchedEffect(partial) { tiers.partial = partial }
    TiersScene(tiers, time, modifier, cover, focusSpan)
}

@Composable
fun ThemeScene(
    state: GardenSceneSnapshot,
    theme: Theme,
    time: Float,
    modifier: Modifier = Modifier,
    zoneIndex: Int = state.currentZone,
    band: Boolean = false,
    showPreview: Boolean = true,
    cover: Boolean = false,
    focusSpan: Float = 0f,
) {
    val partial = if (showPreview) state.steps / STEPS_PER_TASK.toFloat() else 0f
    when (theme) {
        Theme.GARDEN, Theme.OUTPOST ->
            ZoneScene(state, zoneIndex, time, band, modifier, showPreview, theme, cover)
        Theme.ISLAND -> IslandProgress(state.totalPlanted, partial, modifier, time, cover, focusSpan)
        Theme.COLONY -> ColonyProgress(state.totalPlanted, partial, modifier, time, cover, focusSpan)
    }
}

fun sceneAspect(theme: Theme): Float =
    when (theme) {
        Theme.ISLAND -> ISLAND_W / ISLAND_H
        else -> SCENE_W / SCENE_H
    }

/** Aspect for the lower scene band (home thumbs, place picker cards). */
fun sceneBandAspect(theme: Theme): Float =
    when (theme) {
        Theme.ISLAND -> ISLAND_W / ISLAND_H
        else -> SCENE_W / BAND_H
    }

fun themeCapacity(theme: Theme): Int =
    when (theme) {
        Theme.ISLAND -> ISLAND_CELLS.size
        Theme.COLONY -> TOTAL_TIER_TASKS
        else -> ZONES.size * ZONE_CAPACITY
    }

/** Draw one finished item for collection shelves and custom compositions. */
fun DrawScope.drawFinishedItem(
    zoneIndex: Int,
    theme: Theme,
    slot: Int,
    x: Float,
    y: Float,
    scale: Float,
    seed: Int,
    time: Float,
) {
    drawSceneSlotAt(
        ZONES[zoneIndex.coerceIn(0, ZONES.lastIndex)],
        theme,
        slot,
        x,
        y,
        1f,
        scale,
        seed,
        time,
    )
}

/** Zoom for home-rail and shelf previews — items read tiny inside the full scene band. */
internal const val SCENE_ITEM_PREVIEW_MAGNIFICATION = 5f
/** Slightly less zoom when several items share one thumb (surprise picker). */
internal const val SCENE_SURPRISE_PREVIEW_MAGNIFICATION = 3.5f
internal const val SCENE_ITEM_BOTTOM_INSET = 8f

/** Centre plot on the middle row — matches the Scene tab preview slot. */
internal const val SCENE_PREVIEW_PLOT_INDEX = 6

/** Draw one plant/module magnified with its ground anchor on the bottom edge. */
internal fun DrawScope.drawSceneItemPreview(
    magnification: Float = SCENE_ITEM_PREVIEW_MAGNIFICATION,
    block: DrawScope.(plot: com.anurag.eduai.uikit.garden.world.Plot) -> Unit,
) {
    val plot = PLOTS[SCENE_PREVIEW_PLOT_INDEX]
    val bandUnit = minOf(size.width / SCENE_W, size.height / BAND_H)
    val unit = bandUnit * magnification
    val anchorX = size.width / 2f
    val anchorY = size.height - SCENE_ITEM_BOTTOM_INSET
    translate(anchorX, anchorY) {
        scale(unit, unit, pivot = Offset.Zero) {
            translate(-plot.x, -plot.y) {
                block(plot)
            }
        }
    }
}

/** Same band crop as [ZoneScene] (band mode), optionally magnified and bottom-aligned. */
internal fun DrawScope.drawSceneBandContent(
    magnification: Float = 1f,
    bottomAlign: Boolean = false,
    block: DrawScope.() -> Unit,
) {
    val bandUnit = minOf(size.width / SCENE_W, size.height / BAND_H)
    val unit = bandUnit * magnification
    val originX = (size.width - SCENE_W * unit) / 2f
    val originY =
        if (bottomAlign) size.height - BAND_H * unit - SCENE_ITEM_BOTTOM_INSET
        else (size.height - BAND_H * unit) / 2f
    translate(originX, originY) {
        scale(unit, unit, pivot = Offset.Zero) {
            translate(0f, -BAND_TOP) {
                block()
            }
        }
    }
}

internal fun DrawScope.drawPlotShadow(theme: Theme, plot: com.anurag.eduai.uikit.garden.world.Plot) {
    drawOval(
        if (theme == Theme.GARDEN) Color.Black.copy(alpha = 0.05f)
        else Color.White.copy(alpha = 0.06f),
        Offset(plot.x - 12f * plot.scale, plot.y - 4f * plot.scale),
        Size(24f * plot.scale, 8f * plot.scale),
    )
}

@Composable
fun SlotThumb(
    zoneIndex: Int,
    theme: Theme,
    slot: Int,
    modifier: Modifier = Modifier,
) {
    val zone = ZONES[zoneIndex.coerceIn(0, ZONES.lastIndex)]
    Canvas(modifier) {
        drawSceneItemPreview {
            drawPlotShadow(theme, it)
            drawSceneSlotAt(zone, theme, slot, it.x, it.y, 1f, it.scale, SCENE_PREVIEW_PLOT_INDEX, 0f)
        }
    }
}

@Composable
fun SurpriseThumb(
    zoneIndex: Int,
    theme: Theme,
    modifier: Modifier = Modifier,
) {
    val zone = ZONES[zoneIndex.coerceIn(0, ZONES.lastIndex)]
    Canvas(modifier) {
        drawSceneBandContent(
            magnification = SCENE_SURPRISE_PREVIEW_MAGNIFICATION,
            bottomAlign = true,
        ) {
            listOf(
                Triple(0, 0, 0.85f),
                Triple(2, 2, 0.90f),
                Triple(5, 5, 0.80f),
            ).forEach { (slot, plotIndex, growth) ->
                val plot = PLOTS[plotIndex]
                drawPlotShadow(theme, plot)
                drawSceneSlotAt(zone, theme, slot, plot.x, plot.y, growth, plot.scale, plotIndex, 0f)
            }
        }
    }
}
