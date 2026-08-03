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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import com.anurag.eduai.uikit.garden.quest.SCENE_PREVIEW_PLOT_INDEX
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.drawPlotShadow
import com.anurag.eduai.uikit.garden.quest.drawSceneItemPreview
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

private fun DrawScope.parentBody(kind: ParentBody, time: Float) {
    when (kind) {
        ParentBody.EARTH -> {
            drawCircle(Color(0xFF2E6FB8), 34f, Offset(266f, 62f))
            val spin = (time * 6f) % 68f
            translate(spin - 34f, 0f) {
                drawPath(Path().apply {
                    moveTo(250f, 50f); quadraticBezierTo(264f, 42f, 276f, 52f)
                    quadraticBezierTo(268f, 62f, 254f, 58f); close()
                }, Color(0xFF4FA57C).copy(alpha = 0.9f))
                drawPath(Path().apply {
                    moveTo(258f, 78f); quadraticBezierTo(274f, 72f, 280f, 82f)
                    quadraticBezierTo(268f, 90f, 256f, 84f); close()
                }, Color(0xFF4FA57C).copy(alpha = 0.9f))
            }
            drawOval(Color.White.copy(alpha = 0.35f), Offset(240f, 42f), Size(36f, 10f))
        }
        ParentBody.JUPITER -> {
            drawCircle(Color(0xFFD8B98C), 52f, Offset(252f, 52f))
            val bands = Path().apply {
                moveTo(204f, 36f); lineTo(300f, 36f)
                moveTo(206f, 64f); lineTo(298f, 64f)
                moveTo(212f, 80f); lineTo(292f, 80f)
            }
            drawPath(bands, Color(0xFFB08C5E).copy(alpha = 0.8f), style = Stroke(7f))
            drawOval(Color(0xFFC2624A), Offset(259f, 59f), Size(26f, 14f))
        }
        ParentBody.SATURN -> {
            drawCircle(Color(0xFFE2C88C), 30f, Offset(256f, 56f))
            drawPath(Path().apply { moveTo(214f, 60f); lineTo(298f, 60f) }, Color(0xFFC6A96C), style = Stroke(5f))
            drawOval(Color.Transparent, Offset(204f, 45f), Size(104f, 26f))
            drawOval(
                Color(0xFFE8D6A8).copy(alpha = 0.85f), Offset(204f, 45f), Size(104f, 26f),
                style = Stroke(5f)
            )
        }
        ParentBody.PHOBOS -> {
            drawOval(Color(0xFF8A7F72), Offset(257f, 34f), Size(30f, 24f))
            drawCircle(Color(0xFF75695D), 3.4f, Offset(266f, 42f))
            drawCircle(Color(0xFF75695D), 3f, Offset(278f, 50f))
        }
        ParentBody.SUN -> {
            drawCircle(Color(0xFFFFF3D2).copy(alpha = 0.18f), 20f, Offset(276f, 40f))
            drawCircle(Color(0xFFFFF3D2), 9f, Offset(276f, 40f))
        }
        ParentBody.NONE -> {
            drawOval(Color(0xFFF7D48C).copy(alpha = 0.3f), Offset(170f, 36f), Size(140f, 32f))
            drawOval(Color(0xFFF7D48C).copy(alpha = 0.22f), Offset(90f, 58f), Size(180f, 28f))
        }
    }
}

/** Distant silhouettes, so each world has a horizon rather than a flat band. */
private fun DrawScope.horizon(w: World) {
    val a = shade(w.ground[2], -0.18f)
    val b = shade(w.ground[2], -0.30f)
    when (w.surface) {
        Surface.CRATER -> {
            drawPath(Path().apply { moveTo(-14f, 114f); quadraticBezierTo(60f, 88f, 140f, 114f); close() }, a)
            drawPath(Path().apply { moveTo(112f, 116f); quadraticBezierTo(214f, 84f, 312f, 116f); close() }, b)
        }
        Surface.DUNE -> {
            drawPath(Path().apply {
                moveTo(-14f, 114f); lineTo(16f, 74f); lineTo(72f, 74f); lineTo(94f, 114f); close()
            }, a)
            drawPath(Path().apply {
                moveTo(186f, 112f); lineTo(212f, 66f); lineTo(284f, 66f); lineTo(310f, 112f); close()
            }, b)
            drawPath(Path().apply { moveTo(100f, 114f); quadraticBezierTo(140f, 94f, 180f, 114f); close() }, a)
        }
        Surface.ICE, Surface.GEYSER -> {
            drawPath(Path().apply { moveTo(-14f, 116f); lineTo(48f, 40f); lineTo(110f, 116f); close() }, a)
            drawPath(Path().apply { moveTo(30f, 74f); lineTo(48f, 40f); lineTo(66f, 74f); close() }, Color.White.copy(alpha = 0.9f))
            drawPath(Path().apply { moveTo(84f, 116f); lineTo(176f, 26f); lineTo(268f, 116f); close() }, b)
            drawPath(Path().apply { moveTo(150f, 74f); lineTo(176f, 26f); lineTo(202f, 74f); close() }, Color.White.copy(alpha = 0.9f))
            drawPath(Path().apply { moveTo(232f, 116f); lineTo(296f, 56f); lineTo(352f, 116f); close() }, a)
        }
        Surface.LAVA -> {
            drawPath(Path().apply { moveTo(-14f, 116f); lineTo(46f, 58f); lineTo(106f, 116f); close() }, b)
            drawPath(Path().apply { moveTo(34f, 74f); lineTo(46f, 58f); lineTo(58f, 74f); close() }, Color(0xFFFF6B4A).copy(alpha = 0.55f))
            drawPath(Path().apply { moveTo(140f, 116f); lineTo(214f, 46f); lineTo(288f, 116f); close() }, a)
            drawPath(Path().apply { moveTo(200f, 66f); lineTo(214f, 46f); lineTo(228f, 66f); close() }, Color(0xFFFF9E7A).copy(alpha = 0.6f))
        }
        Surface.ROCK -> {
            drawPath(Path().apply {
                moveTo(-14f, 116f); lineTo(24f, 76f); lineTo(52f, 98f); lineTo(84f, 62f); lineTo(126f, 116f); close()
            }, a)
            drawPath(Path().apply {
                moveTo(170f, 116f); lineTo(212f, 70f); lineTo(246f, 96f); lineTo(282f, 66f); lineTo(322f, 116f); close()
            }, b)
        }
        Surface.CLOUD -> {
            drawOval(Color(0xFFF7ECD2).copy(alpha = 0.4f), Offset(-20f, 84f), Size(200f, 40f))
            drawOval(Color(0xFFF7ECD2).copy(alpha = 0.32f), Offset(150f, 92f), Size(210f, 34f))
        }
        Surface.LAKE -> {
            drawPath(Path().apply { moveTo(-14f, 114f); quadraticBezierTo(66f, 82f, 150f, 114f); close() }, a)
            drawPath(Path().apply { moveTo(124f, 116f); quadraticBezierTo(220f, 78f, 314f, 116f); close() }, b)
            drawOval(Color(0xFFF7D48C).copy(alpha = 0.22f), Offset(30f, 96f), Size(180f, 22f))
        }
    }
}

private class SkyField(seed: Int) {
    val stars = List(46) {
        Triple(
            6f + hash(seed, it) * 320f,
            6f + hash(seed, it + 50) * 104f,
            0.7f + hash(seed, it + 90) * 1.3f
        )
    }
}

private fun DrawScope.surfaceDetail(w: World, seed: Int, time: Float) {
    for (i in 0 until 20) {
        val x = 8f + hash(seed, i) * 316f
        val y = 126f + hash(seed, i + 40) * 212f
        val sc = 0.6f + hash(seed, i + 80) * 0.7f
        when (w.surface) {
            Surface.CRATER -> {
                drawOval(shade(w.ground[1], -0.12f), Offset(x - 9f * sc, y - 3.4f * sc), Size(18f * sc, 6.8f * sc))
                drawOval(
                    shade(w.ground[0], 0.2f), Offset(x - 9f * sc, y - 4.4f * sc), Size(18f * sc, 6.8f * sc),
                    style = Stroke(1f)
                )
            }
            Surface.DUNE -> drawPath(
                Path().apply { moveTo(x - 11f * sc, y); quadraticBezierTo(x, y - 5f * sc, x + 11f * sc, y) },
                shade(w.ground[1], 0.14f), style = Stroke(1.6f * sc)
            )
            Surface.ICE -> drawPath(
                Path().apply { moveTo(x, y); lineTo(x + 12f * sc, y - 4f * sc); lineTo(x + 21f * sc, y + 1f * sc) },
                Color(0xFF5FA8D8).copy(alpha = 0.65f), style = Stroke(1.3f * sc)
            )
            Surface.LAVA -> {
                val glow = 0.35f + 0.5f * abs(sin(time * 1.2f + i))
                drawPath(
                    Path().apply { moveTo(x, y); lineTo(x + 13f * sc, y - 4f * sc); lineTo(x + 23f * sc, y + 1f * sc) },
                    Color(0xFFFF6B4A).copy(alpha = glow), style = Stroke(2f * sc)
                )
            }
            Surface.ROCK -> drawPath(
                Path().apply { moveTo(x, y); lineTo(x + 6f * sc, y - 7f * sc); lineTo(x + 12f * sc, y); close() },
                shade(w.ground[1], -0.16f)
            )
            Surface.GEYSER -> drawOval(Color.White.copy(alpha = 0.7f), Offset(x - 8f * sc, y - 2.8f * sc), Size(16f * sc, 5.6f * sc))
            Surface.LAKE -> if (hash(seed, i + 200) > 0.6f)
                drawOval(Color(0xFF3E2E12).copy(alpha = 0.7f), Offset(x - 16f * sc, y - 5f * sc), Size(32f * sc, 10f * sc))
            Surface.CLOUD -> drawOval(Color(0xFFF7D48C).copy(alpha = 0.3f), Offset(x - 20f * sc, y - 5f * sc), Size(40f * sc, 10f * sc))
        }
    }
}

private fun DrawScope.worldAmbient(w: World, time: Float) {
    // orbiting satellite, every world
    val orbit = ((time * 11f) % 430f) - 46f
    translate(orbit, -orbit * 0.04f) {
        drawRoundRectCompat(Color(0xFFC6CEDA), 0f, 84f, 10f, 3.2f, 1.4f)
        drawRoundRectCompat(Color(0xFF5FC7E8), -6f, 82f, 5f, 6.8f, 1f)
        drawRoundRectCompat(Color(0xFF5FC7E8), 11f, 82f, 5f, 6.8f, 1f)
    }

    // rover on patrol, with a dust trail
    val patrol = sin(time * 0.32f) * 39f + 39f
    translate(patrol, 0f) {
        translate(64f, 332f) { roverBody(Color(0xFFDCE3ED), 1f, time) }
        for (k in 0 until 4) {
            drawRoundRectCompat(Color.Black.copy(alpha = 0.16f), 30f - k * 9f, 332f, 6f, 2.6f, 1.3f)
        }
    }

    when (w.surface) {
        Surface.CRATER -> {
            val t = (time % 12f) / 12f
            if (t < 0.14f) {
                val p = t / 0.14f
                translate(40f + p * 130f, 48f + p * 64f) {
                    drawPath(Path().apply { moveTo(0f, 0f); lineTo(-24f, -12f) }, Color.White.copy(alpha = 1f - p), style = Stroke(2f, cap = StrokeCap.Round))
                }
            }
        }
        Surface.DUNE -> {
            listOf(196f to 22f, 244f to 29f).forEachIndexed { i, (y, period) ->
                val t = ((time + i * 7f) % period) / period
                translate(-40f + t * 420f, 0f) {
                    drawPath(Path().apply {
                        moveTo(0f, y); quadraticBezierTo(3f, y - 16f, -4f, y - 24f)
                        quadraticBezierTo(10f, y - 18f, 10f, y); close()
                    }, Color(0xFFE8B48C).copy(alpha = 0.4f))
                }
            }
            drawOval(Color(0xFF7FE3C8).copy(alpha = 0.35f), Offset(150f, 283f), Size(36f, 18f))
        }
        Surface.ICE -> {
            val a = 0.22f + 0.38f * abs(sin(time * 0.42f))
            drawPath(Path().apply {
                moveTo(-10f, 40f); quadraticBezierTo(90f, 16f, 190f, 40f)
                quadraticBezierTo(290f, 62f, 348f, 32f)
                lineTo(348f, 64f); quadraticBezierTo(190f, 80f, -10f, 66f); close()
            }, Color(0xFF7FE3C8).copy(alpha = a))
            val drill = sin(time * 0.3f) * 32f
            translate(drill, 0f) { translate(232f, 266f) { drillBot(time) } }
        }
        Surface.LAKE -> {
            for (i in 0 until 10) {
                val t = ((time * 0.42f + i * 0.31f) % 1f)
                val x = 20f + i * 32f
                drawPath(
                    Path().apply { moveTo(x, 112f + t * 214f); lineTo(x - 1.6f, 121f + t * 214f) },
                    Color(0xFFF7D48C).copy(alpha = 0.6f * (1f - t)), style = Stroke(1.4f, cap = StrokeCap.Round)
                )
            }
            val bob = sin(time * 0.9f) * 4f
            translate(0f, bob) {
                drawCircle(Color(0xFFF7D48C).copy(alpha = 0.85f), 11f, Offset(250f, 148f))
                drawRoundRectCompat(Color(0xFFC9AF80), 246f, 158f, 8f, 6f, 2f)
            }
            val flash = if ((time % 11f) > 10.6f) 0.5f else 0f
            if (flash > 0f) drawRect(Color(0xFFFFE9A8).copy(alpha = flash), size = Size(SCENE_W, SCENE_H))
        }
        Surface.CLOUD -> {
            val flash = if ((time % 8f) > 7.7f) 0.6f else 0f
            if (flash > 0f) drawRect(Color(0xFFFFF3D2).copy(alpha = flash), size = Size(SCENE_W, SCENE_H))
            listOf(Triple(60f, 150f, 0f), Triple(262f, 186f, 2.1f)).forEach { (x, y, ph) ->
                val bob = sin(time * 1.1f + ph) * 3.5f
                translate(0f, bob) {
                    drawOval(Color(0xFFF7ECD2), Offset(x - 26f, y - 11f), Size(52f, 22f))
                    drawRoundRectCompat(Color(0xFFD2BC92), x - 9f, y + 8f, 18f, 8f, 3f)
                    drawCircle(Color(0xFFC97B1E), 4f, Offset(x, y))
                }
            }
            for (i in 0 until 3) {
                val a = 0.14f + 0.26f * abs(sin(time * 1.2f + i))
                drawOval(Color.White.copy(alpha = a), Offset(16f + i * 100f, 226f + i * 30f), Size(108f, 12f))
            }
        }
        Surface.LAVA -> {
            listOf(Triple(69f, 178f, 0f), Triple(286f, 206f, 2.6f)).forEach { (x, y, ph) ->
                val scaleY = 0.45f + 0.75f * abs(sin(time * 1.16f + ph))
                drawPath(Path().apply {
                    moveTo(x - 9f, y); quadraticBezierTo(x - 4f, y - 58f * scaleY, x, y - 58f * scaleY)
                    quadraticBezierTo(x + 6f, y - 40f * scaleY, x + 9f, y); close()
                }, Color(0xFFFF9E7A).copy(alpha = 0.5f))
            }
            for (i in 0 until 8) {
                val t = ((time * 0.11f + i * 0.13f) % 1f)
                drawCircle(
                    Color(0xFF6E5B48).copy(alpha = 0.7f * (1f - t)), 1.5f,
                    Offset(30f + i * 40f - t * 20f, 120f + t * 216f)
                )
            }
            val rover = sin(time * 0.36f) * 30f
            translate(rover, 0f) { translate(228f, 262f) { roverBody(Color(0xFFE2622F), 0.8f, time) } }
        }
        Surface.ROCK -> {
            val rover = sin(time * 0.34f) * 34f
            translate(rover, 0f) { translate(214f, 268f) { roverBody(Color(0xFF9CA2AE), 0.75f, time) } }
            listOf(Triple(44f, 286f, 0f), Triple(304f, 258f, 1.6f)).forEach { (x, y, ph) ->
                val a = 0.2f + 0.8f * abs(sin(time * 1.9f + ph))
                drawPath(Path().apply {
                    moveTo(x - 5f, y); lineTo(x, y - 13f); lineTo(x + 6f, y); close()
                }, Color(0xFFAECBE8).copy(alpha = a))
            }
        }
        Surface.GEYSER -> {
            listOf(Triple(83f, 236f, 0f), Triple(272f, 262f, 3.1f)).forEach { (x, y, ph) ->
                val scaleY = 0.45f + 0.75f * abs(sin(time * 1.1f + ph))
                drawPath(Path().apply {
                    moveTo(x - 10f, y); quadraticBezierTo(x - 5f, y - 70f * scaleY, x, y - 70f * scaleY)
                    quadraticBezierTo(x + 7f, y - 48f * scaleY, x + 10f, y); close()
                }, Color.White.copy(alpha = 0.55f))
            }
            for (i in 0 until 7) {
                val t = ((time * 0.09f + i * 0.14f) % 1f)
                drawCircle(Color.White.copy(alpha = 0.9f * (1f - t)), 1.6f, Offset(26f + i * 44f - t * 18f, 110f + t * 226f))
            }
            val rover = sin(time * 0.3f) * 28f
            translate(rover, 0f) { translate(240f, 270f) { roverBody(Color(0xFFAEE4F5), 0.75f, time) } }
        }
    }
}

private fun DrawScope.roverBody(color: Color, s: Float, time: Float) {
    drawRoundRectCompat(color, -10f * s, -11f * s, 20f * s, 8f * s, 3f * s)
    listOf(-6f, 1f, 8f).forEach { drawCircle(Color(0xFF5A6270), 3.2f * s, Offset(it * s, -2f * s)) }
    drawRoundRectCompat(Color(0xFFB4BECC), -1f * s, -18f * s, 2f * s, 7f * s, 0.6f)
    val a = 0.2f + 0.8f * abs(sin(time * 2.4f))
    drawCircle(Color(0xFFFF6B4A).copy(alpha = a), 1.6f * s, Offset(9f * s, -13f * s))
}

private fun DrawScope.drillBot(time: Float) {
    drawRoundRectCompat(Color(0xFFAEE4F5), -7f, -9f, 14f, 7f, 2.4f)
    drawRoundRectCompat(Color(0xFF8B94A4), -1.4f, -2f, 2.8f, 9f, 0.6f)
    val a = 0.2f + 0.8f * abs(sin(time * 3.4f))
    drawCircle(Color(0xFF5FC7E8).copy(alpha = a), 2.2f, Offset(0f, 8f))
}

private fun DrawScope.eventArt(index: Int, time: Float) {
    when (index) {
        0 -> for (k in 0 until 3) {
            val t = ((time + k * 3f) % 9f) / 9f
            if (t < 0.16f) {
                val p = t / 0.16f
                translate(30f + k * 90f + p * 130f, 34f + k * 14f + p * 64f) {
                    drawPath(
                        Path().apply { moveTo(0f, 0f); lineTo(-24f, -12f) },
                        Color.White.copy(alpha = 1f - p), style = Stroke(2f, cap = StrokeCap.Round)
                    )
                }
            }
        }
        1 -> {
            val t = (time % 18f) / 18f
            val drop = if (t < 0.5f) (0.5f - t) * 2f else 0f
            translate(206f, 130f + drop * 90f) {
                drawPath(Path().apply { moveTo(-9f, 0f); lineTo(-6f, -15f); lineTo(6f, -15f); lineTo(9f, 0f); close() }, Color(0xFFEFE3D2))
                if (drop > 0.05f) drawPath(Path().apply {
                    moveTo(-5f, 2f); quadraticBezierTo(0f, 16f, 0f, 22f); quadraticBezierTo(0f, 16f, 5f, 2f); close()
                }, Color(0xFFFFC28A).copy(alpha = 0.6f))
            }
        }
        2 -> {
            val a = 0.25f + 0.4f * abs(sin(time * 0.5f))
            drawPath(Path().apply {
                moveTo(-10f, 34f); quadraticBezierTo(90f, 10f, 190f, 34f)
                quadraticBezierTo(290f, 58f, 348f, 26f)
                lineTo(348f, 60f); quadraticBezierTo(190f, 76f, -10f, 62f); close()
            }, Color(0xFF7FE3C8).copy(alpha = a))
        }
        4 -> {
            val a = 0.3f + 0.7f * abs(sin(time * 1.6f))
            drawCircle(Color(0xFFAEE4F5).copy(alpha = a * 0.25f), 16f, Offset(46f, 296f))
            drawPath(Path().apply { moveTo(38f, 300f); lineTo(44f, 282f); lineTo(51f, 300f); close() }, Color(0xFFAEE4F5).copy(alpha = a))
        }
        else -> Unit
    }
}

@Composable
fun OutpostScene(state: OutpostState, time: Float, modifier: Modifier = Modifier) {
    val world = state.world
    val sky = remember(world.key) {
        Brush.verticalGradient(listOf(world.skyTop, world.skyBottom), startY = 0f, endY = SCENE_H)
    }
    val field = remember(world.key) { SkyField(world.key.hashCode() and 0xff) }

    Canvas(
        modifier = modifier.pointerInput(state.worldIndex, state.mode, state.pendingTask, state.movingFrom) {
            detectTapGestures { offset ->
                val unit = size.width / SCENE_W
                onOutpostTap(state, offset.x / unit, offset.y / unit)
            }
        }
    ) {
        val unit = size.width / SCENE_W
        scale(unit, unit, pivot = Offset.Zero) {
            drawRect(sky, size = Size(SCENE_W, SCENE_H))
            if (world.surface != Surface.CLOUD) {
                field.stars.forEach { (x, y, r) ->
                    drawCircle(Color.White.copy(alpha = 0.45f + 0.4f * abs(sin(time * 0.8f + x))), r, Offset(x, y))
                }
            }
            parentBody(world.parent, time)
            horizon(world)

            drawPath(bandPath(112f, 0), world.ground[0])
            drawPath(bandPath(168f, 1), world.ground[1])
            drawPath(bandPath(248f, 2), world.ground[2])
            surfaceDetail(world, world.key.hashCode() and 0x7f, time)
            worldAmbient(world, time)
            eventArt(state.sol % SOL_EVENTS.size, time)

            drawSiteModules(state, time)
            drawSiteDecorations(state, time)
            drawCrew(time)
            drawShip(48f, 306f, state.chaptersDone)
            drawPip(228f, 332f, time)
        }
    }
}

private fun bandPath(y: Float, k: Int): Path = Path().apply {
    moveTo(-6f, y)
    quadraticBezierTo(58f + k * 14f, y - 13f, 138f + k * 10f, y)
    quadraticBezierTo(218f + k * 6f, y + 13f, 338f, y - 7f)
    lineTo(338f, SCENE_H); lineTo(-6f, SCENE_H); close()
}

private fun DrawScope.drawSiteModules(state: OutpostState, time: Float) {
    val site = state.site
    val byPlot = site.modules.associateBy { it.plot }
    val free = site.firstFreePlot()
    val active = site.activeTask()
    val awaiting = state.mode == Mode.PLANT && state.pendingTask >= 0
    val moving = state.mode == Mode.MOVE

    PLOTS.forEachIndexed { index, plot ->
        val module = byPlot[index]
        val s = plot.scale * 0.86f
        when {
            module != null -> {
                drawModule(module.kind, state.world, plot.x, plot.y, s, time)
                val lit = moving && state.movingFrom == index
                if (state.selectedPlot == index || lit) {
                    drawCircle(
                        if (lit) Color(0xFFEF9F27) else Color(0xFF5FC7E8),
                        30f * s, Offset(plot.x, plot.y - 22f * s), style = Stroke(2.4f)
                    )
                }
            }

            awaiting || (moving && state.movingFrom >= 0) -> {
                val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                val pulse = 0.45f + 0.4f * abs(sin(time * 2.2f))
                drawOval(
                    Color(0xFF5FC7E8).copy(alpha = pulse),
                    Offset(plot.x - 20f * plot.scale, plot.y - 8f * plot.scale),
                    Size(40f * plot.scale, 16f * plot.scale),
                    style = Stroke(2f, pathEffect = dash)
                )
            }

            index == free && active >= 0 && site.tasks[active].steps > 0 -> {
                val g = site.tasks[active].steps / TaskState.STEPS_PER_TASK.toFloat()
                drawModule(state.moduleKind, state.world, plot.x, plot.y, s * (0.5f + g * 0.5f), time)
            }

            else -> drawOval(
                Color.White.copy(alpha = 0.07f),
                Offset(plot.x - 13f * plot.scale, plot.y - 4.5f * plot.scale),
                Size(26f * plot.scale, 9f * plot.scale)
            )
        }
    }
}

private fun DrawScope.drawSiteDecorations(state: OutpostState, time: Float) {
    val placed = state.site.decor.associateBy { it.slot }
    DECOR_SLOTS.forEachIndexed { index, slot ->
        val item = placed[index]
        if (item != null) drawSiteDecor(item.decor, slot.x, slot.y, slot.scale, time)
        else if (state.mode == Mode.DECORATE) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            drawRoundRect(
                Color(0xFFFFD764).copy(alpha = 0.75f),
                Offset(slot.x - 14f * slot.scale, slot.y - 22f * slot.scale),
                Size(28f * slot.scale, 26f * slot.scale),
                androidx.compose.ui.geometry.CornerRadius(5f, 5f),
                style = Stroke(1.8f, pathEffect = dash)
            )
        }
    }
}

private fun onOutpostTap(state: OutpostState, x: Float, y: Float) {
    if (hypot(x - 228f, y - 318f) < 30f) {
        state.pipLine = (state.pipLine + 1) % PIP_LINES.size
        return
    }
    if (state.mode == Mode.DECORATE) {
        var best = -1
        var dist = 34f
        DECOR_SLOTS.forEachIndexed { i, slot ->
            val d = hypot(slot.x - x, slot.y - y - 12f)
            if (d < dist) { dist = d; best = i }
        }
        if (best >= 0) {
            if (state.site.decor.any { it.slot == best }) state.removeDecor(best) else state.addDecor(best)
        }
        return
    }

    var plot = -1
    var dist = 34f
    PLOTS.forEachIndexed { i, p ->
        val d = hypot(p.x - x, p.y - y - 12f)
        if (d < dist) { dist = d; plot = i }
    }
    if (plot < 0) return
    val occupied = state.site.modules.any { it.plot == plot }
    when (state.mode) {
        Mode.MOVE -> {
            if (occupied) state.movingFrom = if (state.movingFrom == plot) -1 else plot
            else if (state.movingFrom >= 0) state.moveModule(plot)
        }
        Mode.PLANT -> {
            if (state.pendingTask >= 0 && !occupied) state.build(plot)
            else if (occupied) state.selectedPlot = if (state.selectedPlot == plot) -1 else plot
        }
        Mode.DECORATE -> Unit
    }
}

@Composable
fun ModuleThumb(kind: ModuleKind, world: World, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawSceneItemPreview {
            drawPlotShadow(Theme.OUTPOST, it)
            drawModule(
                kind,
                world,
                it.x,
                it.y,
                it.scale * 1.1f * 1.6f,
                0f,
            )
        }
    }
}
