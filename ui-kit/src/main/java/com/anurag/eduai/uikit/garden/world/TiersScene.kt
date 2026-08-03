package com.anurag.eduai.uikit.garden.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.sin

private fun skyFor(stage: Int): Pair<Color, Color> {
    val t = ((stage - 6f) / 6f).coerceIn(0f, 1f)
    return lerp(Color(0xFF2A1410), Color(0xFF1E4A7A), t) to
        lerp(Color(0xFF8A4A2C), Color(0xFFA8DCF2), t)
}

private fun groundFor(stage: Int): List<Color> {
    val t = ((stage - 7f) / 5f).coerceIn(0f, 1f)
    return listOf(
        lerp(Color(0xFFC4643A), Color(0xFF5FA86B), t),
        lerp(Color(0xFFAE5730), Color(0xFF4E9660), t),
        lerp(Color(0xFF984A28), Color(0xFF3F8452), t)
    )
}

private fun band(y: Float, k: Int): Path = Path().apply {
    moveTo(-6f, y)
    quadraticBezierTo(58f + k * 14f, y - 13f, 138f + k * 10f, y)
    quadraticBezierTo(218f + k * 6f, y + 13f, 338f, y - 7f)
    lineTo(338f, SCENE_H); lineTo(-6f, SCENE_H); close()
}

private fun DrawScope.figure(x: Float, y: Float, s: Float) {
    drawCircle(Color(0xFFEAF0F8), 2.8f * s, Offset(x, y - 9f * s))
    drawRoundRectCompat(Color(0xFFDCE3ED), x - 2f * s, y - 6.4f * s, 4f * s, 5f * s, 1.4f * s)
    val legs = Path().apply {
        moveTo(x - 1.4f * s, y - 1.4f * s); lineTo(x - 1.4f * s, y)
        moveTo(x + 1.4f * s, y - 1.4f * s); lineTo(x + 1.4f * s, y)
    }
    drawPath(legs, Color(0xFFB4BECC), style = Stroke(1.2f * s))
}

private fun DrawScope.rover(x: Float, y: Float, s: Float, time: Float) {
    drawRoundRectCompat(Color(0xFFC6CEDA), x - 10f * s, y - 11f * s, 20f * s, 8f * s, 3f * s)
    listOf(-6f, 1f, 8f).forEach { drawCircle(Color(0xFF5A6270), 3.2f * s, Offset(x + it * s, y - 2f * s)) }
    drawRoundRectCompat(Color(0xFFB4BECC), x - 1f * s, y - 18f * s, 2f * s, 7f * s, 0.6f)
    val a = 0.2f + 0.8f * abs(sin(time * 2.4f))
    drawCircle(Color(0xFFFF6B4A).copy(alpha = a), 1.6f * s, Offset(x + 9f * s, y - 13f * s))
}

private fun DrawScope.tower(x: Float, y: Float, w: Float, h: Float, time: Float) {
    drawRoundRectCompat(Color(0xFFC6CEDA), x - w / 2f, y - h, w, h, 2f)
    drawRoundRectCompat(Color(0xFFA2ACBC), x - w / 2f, y - h, w * 0.34f, h, 2f)
    val rows = (h / 7f).toInt()
    for (r in 0 until rows) for (c in 0 until 2) {
        val lit = (r + c) % 3 == 0
        val a = if (lit) 0.35f + 0.65f * abs(sin(time * 1.4f + r + c)) else 1f
        drawRoundRectCompat(
            (if (lit) Color(0xFFFFD764) else Color(0xFF7E8899)).copy(alpha = a),
            x - w * 0.3f + c * w * 0.34f, y - h + 5f + r * 7f, w * 0.22f, 3.4f, 0.5f
        )
    }
}

private fun DrawScope.reactor(x: Float, y: Float, s: Float, time: Float) {
    drawOval(Color(0xFF8B94A4), Offset(x - 24f * s, y - 7f * s), Size(48f * s, 14f * s))
    val pulse = 0.35f + 0.55f * abs(sin(time * 1.5f))
    drawOval(
        Color(0xFF5FC7E8).copy(alpha = pulse), Offset(x - 20f * s, y - 23f * s),
        Size(40f * s, 18f * s), style = Stroke(4f * s)
    )
    drawOval(
        Color(0xFFAEE4F5).copy(alpha = pulse * 0.8f), Offset(x - 12f * s, y - 19f * s),
        Size(24f * s, 10f * s), style = Stroke(2.4f * s)
    )
    drawRoundRectCompat(Color(0xFFC6CEDA), x - 5f * s, y - 14f * s, 10f * s, 14f * s, 2f * s)
    drawCircle(Color(0xFF7FE3C8).copy(alpha = pulse), 3f * s, Offset(x, y - 20f * s))
}

private fun DrawScope.ringStation(x: Float, y: Float, time: Float) {
    drawOval(Color(0xFFC6CEDA), Offset(x - 34f, y - 11f), Size(68f, 22f), style = Stroke(4f))
    drawOval(
        Color(0xFF5FC7E8).copy(alpha = 0.4f + 0.4f * abs(sin(time))),
        Offset(x - 34f, y - 11f), Size(68f, 22f), style = Stroke(1.4f)
    )
    drawRoundRectCompat(Color(0xFFEAF0F8), x - 7f, y - 5f, 14f, 10f, 3f)
    drawRoundRectCompat(Color(0xFF4FB9E0), x - 20f, y - 2f, 9f, 4f, 1.4f)
    drawRoundRectCompat(Color(0xFF4FB9E0), x + 11f, y - 2f, 9f, 4f, 1.4f)
}

private fun DrawScope.rocket(x: Float, y: Float, s: Float, lift: Float) {
    translate(0f, -lift) {
        val body = Path().apply {
            moveTo(x - 5f * s, y); lineTo(x - 5f * s, y - 22f * s)
            quadraticBezierTo(x, y - 32f * s, x + 5f * s, y - 22f * s)
            lineTo(x + 5f * s, y); close()
        }
        drawPath(body, Color(0xFFEAF0F8))
        drawPath(Path().apply {
            moveTo(x - 5f * s, y - 6f * s); lineTo(x - 9f * s, y + 2f * s); lineTo(x - 5f * s, y + 2f * s); close()
            moveTo(x + 5f * s, y - 6f * s); lineTo(x + 9f * s, y + 2f * s); lineTo(x + 5f * s, y + 2f * s); close()
        }, Color(0xFFC6CEDA))
        drawCircle(Color(0xFF5FC7E8), 2.4f * s, Offset(x, y - 16f * s))
        if (lift > 1f) {
            drawPath(Path().apply {
                moveTo(x - 5f * s, y + 2f * s)
                quadraticBezierTo(x, y + 20f * s, x, y + 30f * s)
                quadraticBezierTo(x, y + 20f * s, x + 5f * s, y + 2f * s)
                close()
            }, Color(0xFFFF9E7A).copy(alpha = 0.6f))
        }
    }
}

/**
 * The eight sites a tier is built from, laid across the front of the scene.
 *
 * Spread the full width rather than stacked in one corner: a task has to put something *somewhere
 * new*, or it does not read as an addition. The slight zigzag in y and the alternating scale keep it
 * from looking like a ruler.
 */
private val SITES: List<Triple<Float, Float, Float>> = listOf(
    Triple(26f, 337f, 0.64f),
    Triple(65f, 347f, 0.70f),
    Triple(104f, 335f, 0.62f),
    Triple(143f, 346f, 0.69f),
    Triple(182f, 336f, 0.64f),
    Triple(221f, 347f, 0.70f),
    Triple(260f, 335f, 0.62f),
    Triple(298f, 345f, 0.68f),
)

private fun DrawScope.buildSite(w: World, done: Int, partial: Float, time: Float) {
    // One real module per finished task, not a bar on a chart. Cycling the six archetypes means two
    // consecutive tasks never place the same shape, which is most of what makes an addition read as
    // an addition. All eight pads are drawn whether filled or not, so the site always shows how far
    // through the tier you are — and when a tier completes and the row clears, the empty pads make
    // it obvious that a new tier has started rather than that something was lost.
    val placed = done.coerceIn(0, TASKS_PER_TIER)

    SITES.forEachIndexed { i, (x, y, s) ->
        if (i < placed) return@forEachIndexed
        // pad waiting for a module
        drawOval(
            Color(0xFFB4BECC).copy(alpha = if (i == placed) 0.34f else 0.16f),
            Offset(x - 13f * s, y - 3.4f * s), Size(26f * s, 7f * s)
        )
        if (i == placed) {
            val pulse = 0.30f + 0.30f * abs(sin(time * 2f))
            drawOval(
                Color(0xFFFFD764).copy(alpha = pulse),
                Offset(x - 15f * s, y - 4f * s), Size(30f * s, 8f * s),
                style = Stroke(1.4f)
            )
        }
    }

    for (i in 0 until placed) {
        val (x, y, s) = SITES[i]
        drawModule(ModuleKind.entries[i % ModuleKind.entries.size], w, x, y, s, time)
    }

    // The one being built right now: it rises onto its pad across the seven steps.
    if (placed < TASKS_PER_TIER && partial > 0.01f) {
        val (x, y, s) = SITES[placed]
        val p = partial.coerceIn(0f, 1f)
        val lift = (1f - p) * 30f
        drawOval(
            Color(0xFFFFD764).copy(alpha = 0.30f),
            Offset(x - 15f * s, y - 4f * s), Size(30f * s, 8f * s)
        )
        // a landing beam, so it is obvious where it is coming down
        drawPath(
            Path().apply {
                moveTo(x - 7f * s, y - lift - 26f * s); lineTo(x - 11f * s, y)
                lineTo(x + 11f * s, y); lineTo(x + 7f * s, y - lift - 26f * s)
            },
            Color(0xFF8FDCF2).copy(alpha = 0.16f + 0.10f * abs(sin(time * 1.6f)))
        )
        translate(0f, -lift) {
            scale(0.35f + 0.65f * p, 0.35f + 0.65f * p, pivot = Offset(x, y)) {
                drawModule(ModuleKind.entries[placed % ModuleKind.entries.size], w, x, y, s, time)
            }
        }
    }
}

/**
 * @param cover when true, scale to *cover* the canvas rather than fit its width: fill both axes,
 * centre horizontally and anchor to the bottom, letting the excess fall outside the bounds. This is
 * how the scene is cropped into a short band without any layout trickery — the canvas is exactly the
 * size it was given and the crop happens in the draw, which is the only place it is fully predictable.
 */
@Composable
fun TiersScene(
    state: TiersState,
    time: Float,
    modifier: Modifier = Modifier,
    cover: Boolean = false,
    focusSpan: Float = 0f,
) {
    val stage = state.stage
    val sky = remember(stage) {
        val (a, b) = skyFor(stage)
        Brush.verticalGradient(listOf(a, b), startY = 0f, endY = SCENE_H)
    }
    val ground = remember(stage) { groundFor(stage) }
    val bands = remember { listOf(band(150f, 0), band(206f, 1), band(276f, 2)) }
    val w = COLONY_PALETTE

    Canvas(modifier) {
        // Three framings, cheapest first.
        //  - default: fit the width, height falls where it may (scrollable callers).
        //  - cover:   fill the box both ways and crop, anchored bottom.
        //  - focus:   frame a `focusSpan`-wide window centred on the site being built. In a 98 dp
        //             square rail the whole 332-unit scene shrinks a module to 6.6 dp; framing 110
        //             units of it puts the same module at 20 dp, with the colony still around it.
        val active = SITES.getOrNull(state.intoTier.coerceIn(0, TASKS_PER_TIER - 1))
        val unit = when {
            focusSpan > 0f -> maxOf(size.width, size.height) / focusSpan
            cover -> maxOf(size.width / SCENE_W, size.height / SCENE_H)
            else -> size.width / SCENE_W
        }
        val originX = when {
            focusSpan > 0f && active != null ->
                (size.width / 2f - active.first * unit)
                    .coerceIn(minOf(0f, size.width - SCENE_W * unit), 0f)
            cover -> (size.width - SCENE_W * unit) / 2f
            else -> 0f
        }
        val originY = if (cover || focusSpan > 0f) size.height - SCENE_H * unit else 0f
        val tf = ((stage - 7f) / 5f).coerceIn(0f, 1f)
        translate(originX, originY) {
        scale(unit, unit, pivot = Offset.Zero) {
            drawRect(sky, size = Size(SCENE_W, SCENE_H))

            for (i in 0 until 40) {
                val sx = 6f + hash(9, i) * 320f
                val sy = 6f + hash(9, i + 50) * 110f
                val r = 0.7f + hash(9, i + 90) * 1.2f
                drawCircle(Color.White.copy(alpha = (1f - tf * 0.85f) * (0.4f + hash(9, i + 130) * 0.6f)), r, Offset(sx, sy))
            }

            if (tf > 0.2f) {
                val drift = sin(time * 0.12f) * 9f
                translate(drift, 0f) {
                    drawOval(Color.White.copy(alpha = tf * 0.7f), Offset(40f, 51f), Size(60f, 18f))
                    drawOval(Color.White.copy(alpha = tf * 0.7f), Offset(76f, 49f), Size(40f, 14f))
                }
            }

            drawOval(Color(0xFF8A7F72).copy(alpha = 1f - tf * 0.5f), Offset(250f, 33f), Size(28f, 22f))

            if (stage >= 12) {
                drawCircle(Color(0xFF5FA86B), 21f, Offset(58f, 52f))
                drawPath(Path().apply {
                    moveTo(40f, 46f); quadraticBezierTo(52f, 39f, 62f, 48f)
                    quadraticBezierTo(54f, 56f, 42f, 52f); close()
                }, Color(0xFF3F8452))
                drawOval(Color(0xFFC6CEDA).copy(alpha = 0.7f), Offset(28f, 45f), Size(60f, 14f), style = Stroke(2f))
                drawCircle(Color(0xFFC98A5E), 9f, Offset(110f, 30f))
                drawOval(Color(0xFFE2C88C).copy(alpha = 0.8f), Offset(95f, 26f), Size(30f, 8f), style = Stroke(1.6f))

                val gate = 0.4f + 0.5f * abs(sin(time * 1.1f))
                drawCircle(Color(0xFFAFA9EC).copy(alpha = gate), 21f, Offset(286f, 112f), style = Stroke(4f))
                drawCircle(Color(0xFF7FE3C8).copy(alpha = gate), 13f, Offset(286f, 112f), style = Stroke(3f))
                drawCircle(Color(0xFF2A1E4A), 7f, Offset(286f, 112f))

                listOf(96f to 22f, 120f to 30f, 138f to 18f).forEachIndexed { i, (y, period) ->
                    val t = ((time + i * 5f) % period) / period
                    translate(-40f + t * 420f, 0f) {
                        drawPath(Path().apply {
                            moveTo(-10f, y); quadraticBezierTo(0f, y - 5f, 8f, y)
                            quadraticBezierTo(0f, y + 5f, -10f, y); close()
                        }, Color(0xFFEAF0F8))
                        drawPath(Path().apply { moveTo(-10f, y); lineTo(-18f, y - 3f); lineTo(-18f, y + 3f); close() }, Color(0xFF5FC7E8))
                    }
                }
            }

            if (stage >= 7) {
                val t = ((time * 11f) % 430f) - 46f
                translate(t, 0f) {
                    drawRoundRectCompat(Color(0xFFDCE3ED), 0f, 80f, 8f, 2.8f, 1.2f)
                    drawRoundRectCompat(Color(0xFF4FB9E0), -5f, 78f, 4f, 6f, 0.8f)
                    drawRoundRectCompat(Color(0xFF4FB9E0), 9f, 78f, 4f, 6f, 0.8f)
                }
            }

            if (stage >= 11) {
                drawPath(Path().apply { moveTo(168f, 150f); lineTo(206f, 74f) }, Color(0xFFB4BECC).copy(alpha = 0.8f), style = Stroke(1.6f))
                val climb = ((time * 0.09f) % 1f)
                drawRoundRectCompat(Color(0xFFFFD764), 168f + climb * 38f - 3f, 150f - climb * 76f - 2.5f, 6f, 5f, 1.6f)
                ringStation(206f, 70f, time)
            }

            bands.forEachIndexed { i, p -> drawPath(p, ground[i]) }

            for (i in 0 until 16) {
                val cx = 8f + hash(5, i) * 316f
                val cy = 160f + hash(5, i + 30) * 172f
                val cs = 0.6f + hash(5, i + 60) * 0.7f
                drawOval(
                    shade(ground[1], -0.12f).copy(alpha = 1f - tf * 0.7f),
                    Offset(cx - 8f * cs, cy - 3f * cs), Size(16f * cs, 6f * cs)
                )
            }

            if (stage >= 8) {
                drawOval(Color(0xFF4FA3C4), Offset(88f, 315f), Size(124f, 30f))
                drawOval(Color(0xFF6FC0DC), Offset(100f, 318f), Size(100f, 20f))
                for (g in 0 until 14) {
                    val gx = 10f + hash(7, g) * 312f
                    val gy = 196f + hash(7, g + 20) * 146f
                    val gs = 0.7f + hash(7, g + 40) * 0.5f
                    val blades = Path().apply {
                        moveTo(gx, gy); quadraticBezierTo(gx - 2f * gs, gy - 5f * gs, gx - 4.2f * gs, gy - 6.4f * gs)
                        moveTo(gx, gy); quadraticBezierTo(gx, gy - 6.4f * gs, gx + 0.6f * gs, gy - 8.6f * gs)
                        moveTo(gx, gy); quadraticBezierTo(gx + 2f * gs, gy - 5f * gs, gx + 4.2f * gs, gy - 6.4f * gs)
                    }
                    drawPath(blades, Color(0xFF4FA57C), style = Stroke(1.25f * gs, cap = StrokeCap.Round))
                }
                drawRoundRectCompat(Color(0xFFC6CEDA), 24f, 196f, 6f, 34f, 3f)
                drawOval(Color(0xFF7FE3C8).copy(alpha = 0.5f), Offset(18f, 187f), Size(18f, 10f))
                drawRoundRectCompat(Color(0xFFC6CEDA), 306f, 204f, 5f, 28f, 2.5f)
                drawOval(Color(0xFF7FE3C8).copy(alpha = 0.5f), Offset(301f, 196f), Size(14f, 8f))
                drawPath(Path().apply {
                    moveTo(196f, SCENE_H); quadraticBezierTo(206f, 322f, 190f, 300f)
                    quadraticBezierTo(178f, 282f, 192f, 266f)
                }, Color(0xFF6FC0DC).copy(alpha = 0.8f), style = Stroke(5f, cap = StrokeCap.Round))
            }

            // Stage 1 — never removed
            drawOval(shade(ground[2], -0.14f), Offset(40f, 293f), Size(44f, 14f))
            drawPath(Path().apply {
                moveTo(52f, 296f); lineTo(56f, 283f)
                moveTo(72f, 296f); lineTo(68f, 283f)
            }, Color(0xFFB4BECC), style = Stroke(2.4f))
            drawPath(Path().apply { moveTo(55f, 283f); lineTo(62f, 274f); lineTo(69f, 283f); close() }, Color(0xFFDCE3ED))
            drawRoundRectCompat(Color(0xFFA2ACBC), 55f, 283f, 14f, 5f, 1.6f)
            drawCircle(Color(0xFF8FDCF2), 2.2f, Offset(62f, 280f))
            val steam = 0.2f + 0.25f * abs(sin(time * 0.9f))
            drawOval(Color.White.copy(alpha = steam), Offset(49f, 293f), Size(26f, 10f))
            drawRoundRectCompat(Color(0xFFB4BECC), 90f, 278f, 1.8f, 22f, 0.6f)
            drawPath(Path().apply {
                moveTo(91.8f, 278f); lineTo(102.8f, 281f); lineTo(91.8f, 284.4f); close()
            }, Color(0xFFE2603F))
            drawRoundRectCompat(Color(0xFF8B94A4), 72f, 304f, 13f, 5f, 1.6f)

            drawModule(ModuleKind.DOME, w, 112f, 298f, 0.7f, time)

            if (stage >= 2) {
                rotate(sin(time * 0.25f) * 5f, Offset(24f, 232f)) {
                    drawModule(ModuleKind.ARRAY, w, 24f, 258f, 0.62f, time)
                }
                rotate(sin(time * 0.25f + 1f) * 5f, Offset(300f, 228f)) {
                    drawModule(ModuleKind.ARRAY, w, 300f, 254f, 0.6f, time)
                }
                drawPath(Path().apply {
                    moveTo(40f, 262f); lineTo(96f, 296f)
                    moveTo(288f, 258f); lineTo(232f, 292f)
                }, Color(0xFF8B94A4).copy(alpha = 0.6f), style = Stroke(1.4f))
                val lit = 0.3f + 0.7f * abs(sin(time * 1.8f))
                listOf(96f, 132f, 232f).forEach {
                    drawCircle(Color(0xFFFFD764).copy(alpha = lit), 1.8f, Offset(it, 292f))
                }
            }

            if (stage >= 3) {
                drawModule(ModuleKind.DOME, w, 168f, 300f, 0.86f, time)
                drawPath(Path().apply {
                    moveTo(158f, 300f); lineTo(158f, 291f)
                    moveTo(166f, 300f); lineTo(166f, 287f)
                    moveTo(174f, 300f); lineTo(174f, 290f)
                }, Color(0xFF3FA878), style = Stroke(2.2f, cap = StrokeCap.Round))
                drawCircle(Color(0xFF5FC788), 3.2f, Offset(166f, 285f))
                drawCircle(Color(0xFF5FC788), 2.4f, Offset(158f, 289f))
                drawCircle(Color(0xFF7FD8A6), 2.4f, Offset(174f, 288f))
                rotate(sin(time * 0.9f) * 16f, Offset(168f, 288f)) {
                    drawPath(Path().apply {
                        moveTo(168f, 288f); quadraticBezierTo(160f, 292f, 156f, 297f)
                        moveTo(168f, 288f); quadraticBezierTo(176f, 292f, 180f, 297f)
                    }, Color(0xFF8FDCF2).copy(alpha = 0.7f), style = Stroke(1.2f))
                }
            }

            if (stage >= 4) {
                drawModule(ModuleKind.PAD, w, 252f, 296f, 0.72f, time)
                drawRoundRectCompat(Color(0xFF8B94A4).copy(alpha = 0.85f), 236f, 286f, 30f, 3f, 1.5f)
                val p1 = ((time * 0.05f) % 1f)
                rover(-40f + p1 * 420f, 318f, 0.85f, time)
                val p2 = ((time * 0.037f + 0.5f) % 1f)
                rover(-40f + p2 * 420f, 262f, 0.6f, time)
                rover(196f + sin(time * 0.33f) * 34f, 284f, 0.55f, time)
            }

            if (stage >= 5) {
                drawModule(ModuleKind.LAB, w, 206f, 258f, 0.6f, time)
                drawModule(ModuleKind.LAB, w, 126f, 252f, 0.52f, time)
                figure(190f, 296f, 1f)
                figure(198f, 299f, 0.9f)
                val d = time * 0.8f
                drawOval(Color(0xFFC6CEDA), Offset(216f + sin(d) * 12f, 236f + sin(d * 1.4f) * 6f), Size(10f, 5f))
            }

            if (stage >= 6) {
                drawModule(ModuleKind.PAD, w, 296f, 306f, 0.8f, time)
                val cycle = (time % 17f) / 17f
                val lift = if (cycle > 0.62f) ((cycle - 0.62f) / 0.38f) * 120f else 0f
                rocket(296f, 300f, 0.9f, lift)
                drawRoundRectCompat(Color(0xFFC7A25E), 266f, 300f, 11f, 8f, 2f)
                drawRoundRectCompat(Color(0xFFB08C4E), 277f, 302f, 9f, 6f, 2f)
                figure(262f, 306f, 0.8f)
            }

            if (stage >= 7) {
                rotate(sin(time * 0.3f) * 8f, Offset(36f, 200f)) {
                    drawModule(ModuleKind.MAST, w, 36f, 232f, 0.6f, time)
                }
                rotate(sin(time * 0.35f) * 12f, Offset(311f, 232f)) {
                    drawPath(Path().apply {
                        moveTo(300f, 232f)
                        cubicTo(300f, 218f, 322f, 218f, 322f, 232f)
                        close()
                    }, Color(0xFFC6CEDA))
                }
                drawRoundRectCompat(Color(0xFF8B94A4), 309f, 232f, 4f, 14f, 1f)
                val beam = 0.12f + 0.34f * abs(sin(time * 0.8f))
                drawPath(Path().apply {
                    moveTo(36f, 198f); lineTo(120f, 96f); lineTo(152f, 104f); close()
                }, Color(0xFF5FC7E8).copy(alpha = beam))
                drawPath(Path().apply {
                    moveTo(311f, 228f); lineTo(246f, 110f); lineTo(278f, 104f); close()
                }, Color(0xFF5FC7E8).copy(alpha = beam * 0.8f))
            }

            if (stage >= 9) {
                tower(88f, 254f, 15f, 44f, time)
                tower(108f, 250f, 12f, 36f, time)
                tower(68f, 252f, 11f, 30f, time)
                tower(126f, 256f, 10f, 26f, time)
                drawPath(Path().apply { moveTo(56f, 266f); lineTo(146f, 272f) }, shade(ground[1], -0.2f), style = Stroke(5f))
                figure(100f, 268f, 0.8f)
                figure(114f, 270f, 0.75f)
                figure(107f, 271f, 0.5f)
                val h1 = ((time * 0.06f) % 1f)
                translate(-34f + h1 * 430f, 0f) {
                    drawRoundRectCompat(Color(0xFFEAF0F8), 0f, 254f, 18f, 6f, 3f)
                    drawRoundRectCompat(Color(0xFF8FDCF2), 4f, 251f, 10f, 4f, 2f)
                    drawOval(Color(0xFF5FC7E8).copy(alpha = 0.4f), Offset(0f, 260f), Size(18f, 5f))
                }
                drawCircle(Color(0xFF4FA57C), 6f, Offset(152f, 282f))
                drawRoundRectCompat(Color(0xFF6B4F35), 151f, 282f, 2f, 5f, 0.5f)
                drawCircle(Color(0xFF5FC788), 4.4f, Offset(162f, 285f))
            }

            if (stage >= 10) {
                reactor(232f, 236f, 0.8f, time)
                val flow = 0.2f + 0.4f * abs(sin(time * 1.9f))
                drawPath(Path().apply {
                    moveTo(212f, 240f); lineTo(150f, 262f)
                    moveTo(252f, 240f); lineTo(292f, 266f)
                }, Color(0xFF5FC7E8).copy(alpha = flow), style = Stroke(2f))
            }

            // Last, so it sits in front of everything — it is the thing you are actually doing.
            buildSite(w, state.intoTier, state.partial, time)
        }
        }
    }
}
