package com.anurag.eduai.uikit.garden.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Everything here reads the single scene clock. No element owns an animation —
 * each derives its phase from `time` plus a stable offset.
 */

private fun wrap(t: Float, period: Float) = (t % period) / period

private fun DrawScope.butterfly(x: Float, y: Float, s: Float, flap: Float, c0: Color, c1: Color) {
    val squeeze = 0.45f + 0.55f * abs(sin(flap))
    val left = Path().apply {
        moveTo(x, y); quadraticBezierTo(x - 7f * s * squeeze, y - 8f * s, x - 11f * s * squeeze, y - 2f * s)
        quadraticBezierTo(x - 8f * s * squeeze, y + 4f * s, x, y); close()
    }
    val right = Path().apply {
        moveTo(x, y); quadraticBezierTo(x + 7f * s * squeeze, y - 9f * s, x + 11f * s * squeeze, y - 3f * s)
        quadraticBezierTo(x + 8f * s * squeeze, y + 4f * s, x, y); close()
    }
    drawPath(left, c0); drawPath(right, c1)
}

private fun DrawScope.bird(x: Float, y: Float, s: Float, flap: Float) {
    val squeeze = 0.5f + 0.5f * abs(sin(flap))
    val p = Path().apply {
        moveTo(x, y); quadraticBezierTo(x - 6f * s, y - 5f * s * squeeze, x - 11f * s, y - 1.6f * s * squeeze)
        moveTo(x, y); quadraticBezierTo(x + 6f * s, y - 5.6f * s * squeeze, x + 11f * s, y - 2f * s * squeeze)
    }
    drawPath(p, Color(0xFF5F7A8C), style = Stroke(2f * s, cap = StrokeCap.Round))
}

private fun DrawScope.rabbit(x: Float, y: Float, s: Float) {
    drawOval(Color(0xFFE8DCC8), Offset(x - 8f * s, y - 5.4f * s), Size(16f * s, 10.8f * s))
    drawCircle(Color(0xFFE8DCC8), 4f * s, Offset(x + 7f * s, y - 4.4f * s))
    val ears = Path().apply {
        moveTo(x + 6f * s, y - 8f * s); quadraticBezierTo(x + 5f * s, y - 15f * s, x + 7.6f * s, y - 16f * s)
        quadraticBezierTo(x + 9f * s, y - 15f * s, x + 8f * s, y - 8f * s); close()
        moveTo(x + 9f * s, y - 8f * s); quadraticBezierTo(x + 9f * s, y - 15f * s, x + 11.6f * s, y - 15.6f * s)
        quadraticBezierTo(x + 13f * s, y - 14f * s, x + 11f * s, y - 8f * s); close()
    }
    drawPath(ears, Color(0xFFE8DCC8))
    drawCircle(Color.White, 2.6f * s, Offset(x - 8f * s, y - 1f * s))
    drawCircle(Color(0xFF4A3A2A), 0.9f * s, Offset(x + 9f * s, y - 5f * s))
}

private fun DrawScope.dragonfly(x: Float, y: Float, s: Float, flap: Float) {
    val squeeze = 0.4f + 0.6f * abs(sin(flap))
    drawRoundRectCompat(Color(0xFF5DCAA5), x - 1f * s, y - 1.2f * s, 16f * s, 2.4f * s, 1.2f * s)
    drawOval(Color(0xFFB7E8F2).copy(alpha = 0.9f), Offset(x - 3f * s, y - 6.6f * s), Size(14f * s, 5.2f * s * squeeze))
    drawOval(Color(0xFFB7E8F2).copy(alpha = 0.9f), Offset(x + 3f * s, y - 6.6f * s), Size(14f * s, 5.2f * s * squeeze))
}

fun DrawScope.drawAmbientBack(habitat: Habitat, time: Float) {
    when (habitat.ambience) {
        Ambience.MEADOW -> {
            val t = wrap(time, 22f)
            translate(-46f + t * 430f, -t * 16f) { bird(0f, 52f, 1f, time * 7f) }
            val t2 = wrap(time + 9f, 30f)
            translate(-46f + t2 * 430f, -t2 * 12f) { bird(0f, 38f, 0.8f, time * 7.4f) }
        }
        Ambience.WOODS -> {
            for (i in 0 until 5) {
                val t = wrap(time + i * 2.2f, 10f)
                val x = 24f + i * 66f - t * 30f
                val y = 104f + t * 232f
                val colors = listOf(
                    Color(0xFFEF9F27), Color(0xFFD8752E), Color(0xFFC9A227),
                    Color(0xFFE8B44A), Color(0xFFB85C2A)
                )
                val leaf = Path().apply {
                    moveTo(x, y); quadraticBezierTo(x + 5f, y - 6f, x + 10f, y - 1f)
                    quadraticBezierTo(x + 5f, y + 5f, x, y); close()
                }
                drawPath(leaf, colors[i].copy(alpha = if (t < 0.12f || t > 0.9f) 0.2f else 0.9f))
            }
            val t = wrap(time, 24f)
            translate(-46f + t * 430f, 0f) { bird(0f, 46f, 0.9f, time * 6.5f) }
        }
        Ambience.WETLAND -> {
            listOf(Triple(62f, 300f, 0f), Triple(204f, 322f, 1.5f), Triple(276f, 296f, 2.9f))
                .forEach { (x, y, off) ->
                    val t = wrap(time + off, 4.6f)
                    val r = 4f + t * 22f
                    drawOval(
                        Color(0xFFDFF3F7).copy(alpha = (1f - t) * 0.85f),
                        Offset(x - r, y - r / 3f), Size(r * 2f, r * 0.66f),
                        style = Stroke(1.6f)
                    )
                }
            val t = wrap(time, 26f)
            translate(-30f + t * 400f, 0f) {
                drawOval(Color(0xFF4E8FA0), Offset(0f, 310f), Size(12f, 5.6f))
                val tail = Path().apply { moveTo(0f, 313f); lineTo(-4.4f, 310f); lineTo(-4.4f, 316f); close() }
                drawPath(tail, Color(0xFF4E8FA0))
            }
        }

        Ambience.BEACH -> {
            listOf(0f, 9f, 17f).forEachIndexed { i, off ->
                val t = wrap(time + off, 19f + i * 6f)
                translate(-46f + t * 430f, -t * 14f) { bird(0f, 44f + i * 12f, 1f - i * 0.12f, time * 7f) }
            }
            val swell = sin(time * 1.05f) * 4f
            val foam = Path().apply {
                moveTo(-6f, 142f + swell); quadraticBezierTo(80f, 130f, 168f, 146f + swell)
                quadraticBezierTo(256f, 160f, 338f, 138f + swell)
            }
            drawPath(foam, Color.White.copy(alpha = 0.55f), style = Stroke(2.4f))
        }

        Ambience.ISLAND -> {
            listOf(0f, 11f).forEachIndexed { i, off ->
                val t = wrap(time + off, 21f + i * 7f)
                translate(-46f + t * 430f, -t * 10f) { bird(0f, 50f + i * 16f, 0.95f - i * 0.2f, time * 8f) }
            }
            // dolphins arc out of the water, offshore of the landmass
            listOf(Triple(300f, 246f, 0f), Triple(22f, 268f, 8f)).forEach { (x, y, off) ->
                val t = wrap(time + off, 16f)
                if (t in 0.76f..0.92f) {
                    val p = (t - 0.76f) / 0.16f
                    val dy = -kotlin.math.sin(p * 3.14159f) * 30f
                    val body = Path().apply {
                        moveTo(x - 11f, y + 4f + dy)
                        quadraticBezierTo(x - 3f, y - 7f + dy, x + 9f, y - 6f + dy)
                        quadraticBezierTo(x + 2f, y + 1f + dy, x - 11f, y + 4f + dy)
                        close()
                    }
                    drawPath(body, Color(0xFF7C93A8))
                }
            }
        }

        Ambience.DESERT -> {
            // sand blowing across, and a camel silhouette crossing the distance
            for (i in 0 until 2) {
                val t = wrap(time + i * 7f, 16f + i * 5f)
                translate(-40f + t * 420f, 0f) {
                    val gust = Path().apply {
                        moveTo(0f, 150f + i * 56f); quadraticBezierTo(22f, 146f + i * 56f, 44f, 151f + i * 56f)
                    }
                    drawPath(gust, Color(0xFFE8D3A0).copy(alpha = 0.6f), style = Stroke(1.6f, cap = StrokeCap.Round))
                }
            }
            val t = wrap(time, 34f)
            if (t < 0.5f) {
                translate(-40f + (t / 0.5f) * 420f, 0f) {
                    val camel = Path().apply {
                        moveTo(-10f, 108f); quadraticBezierTo(-11f, 101f, -7f, 100f)
                        quadraticBezierTo(-5f, 95f, -1f, 96f)
                        quadraticBezierTo(2f, 92f, 5f, 96f)
                        quadraticBezierTo(9f, 97f, 9f, 102f)
                        lineTo(9f, 108f); lineTo(7f, 108f); lineTo(7f, 103f)
                        lineTo(4f, 103f); lineTo(4f, 108f); lineTo(2f, 108f); lineTo(2f, 103f)
                        lineTo(-4f, 103f); lineTo(-4f, 108f); close()
                    }
                    drawPath(camel, Color(0xFFB78F5F).copy(alpha = 0.5f))
                }
            }
        }

        Ambience.HIGHLAND -> {
            listOf(0f, 13f).forEachIndexed { i, off ->
                val t = wrap(time + off, 30f + i * 8f)
                translate(-46f + t * 430f, 0f) { bird(0f, 46f + i * 14f, 1.4f - i * 0.3f, time * 3.2f) }
            }
            // waterfall down the tall peak
            for (k in 0 until 4) {
                val t = wrap(time + k * 0.42f, 1.7f)
                drawRoundRectCompat(
                    Color(0xFFEAF3F8).copy(alpha = if (t < 0.22f) t / 0.22f * 0.9f else (1f - t) * 0.9f),
                    186f + k * 3.4f, 58f + t * 24f, 2.8f, 14f, 1.4f
                )
            }
            val cloud = wrap(time, 44f)
            translate(-46f + cloud * 430f, 0f) {
                drawOval(Color.White.copy(alpha = 0.45f), Offset(0f, 75f), Size(68f, 18f))
            }
        }

        Ambience.ICE -> {
            // aurora ribbons breathing over the peaks
            val a1 = 0.22f + 0.38f * abs(sin(time * 0.42f))
            drawPath(
                Path().apply {
                    moveTo(-10f, 26f); quadraticBezierTo(80f, 4f, 170f, 26f)
                    quadraticBezierTo(260f, 48f, 344f, 18f)
                    lineTo(344f, 50f); quadraticBezierTo(170f, 64f, -10f, 52f); close()
                },
                Color(0xFF7FE3C8).copy(alpha = a1)
            )
            val a2 = 0.18f + 0.32f * abs(sin(time * 0.33f + 2f))
            drawPath(
                Path().apply {
                    moveTo(-10f, 40f); quadraticBezierTo(90f, 20f, 180f, 42f)
                    quadraticBezierTo(270f, 64f, 344f, 34f)
                    lineTo(344f, 60f); quadraticBezierTo(180f, 74f, -10f, 62f); close()
                },
                Color(0xFFAFA9EC).copy(alpha = a2)
            )
            for (i in 0 until 9) {
                val t = wrap(time + i * 1.5f, 10f + i * 0.9f)
                drawCircle(
                    Color.White.copy(alpha = if (t < 0.08f) t / 0.08f else 0.9f),
                    1.6f + (i % 3) * 0.6f,
                    Offset(18f + i * 36f - t * 20f, 96f + t * 256f)
                )
            }
        }
    }
}

fun DrawScope.drawAmbientFront(habitat: Habitat, time: Float, totalPlants: Int) {
    when (habitat.ambience) {
        Ambience.MEADOW -> {
            val a = time * 0.8f
            butterfly(286f + sin(a) * 14f, 150f + cos(a * 1.3f) * 12f, 1f, time * 12f, Color(0xFFFF9EC1), Color(0xFFFFC2D8))
            if (totalPlants >= VISITORS[1].threshold) {
                val b = time * 0.6f + 2f
                butterfly(58f + sin(b) * 16f, 176f + cos(b * 1.1f) * 10f, 0.85f, time * 11f, Color(0xFFFFD764), Color(0xFFFFE9A8))
            }
        }
        Ambience.WOODS -> {
            drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.25f + 0.35f * abs(sin(time * 1.5f))), 7f, Offset(266f, 330f))
            drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.20f + 0.30f * abs(sin(time * 1.5f + 1f))), 5.4f, Offset(282f, 335f))
        }
        Ambience.WETLAND -> {
            val a = time * 0.7f
            dragonfly(232f + sin(a) * 18f, 178f + cos(a * 1.4f) * 9f, 1f, time * 16f)
        }

        Ambience.BEACH -> {
            // a crab pacing the sand, and footprints fading in sequence
            val walk = sin(time * 0.48f) * 31f + 31f
            translate(walk, 0f) {
                drawOval(Color(0xFFE24B4A), Offset(113f, 331f), Size(14f, 9f))
                val claws = Path().apply {
                    moveTo(113f, 334f); quadraticBezierTo(109f, 330f, 107f, 333f)
                    moveTo(127f, 334f); quadraticBezierTo(131f, 330f, 133f, 333f)
                }
                drawPath(claws, Color(0xFFE24B4A), style = Stroke(2f, cap = StrokeCap.Round))
                drawCircle(Color.White, 1.2f, Offset(117.6f, 332f))
                drawCircle(Color.White, 1.2f, Offset(122.4f, 332f))
            }
            for (k in 0 until 5) {
                val t = wrap(time + (5 - k) * 1.6f, 12f)
                drawOval(
                    Color(0xFFDCC79A).copy(alpha = (1f - t) * 0.8f),
                    Offset(202f + k * 22f, 328f - k * 4f), Size(9f, 5.2f)
                )
            }
        }

        Ambience.ISLAND -> {
            drawCircle(Color(0xFFF2E1BB), 3.4f, Offset(86f, 300f))
            drawCircle(Color(0xFFF2E1BB), 2.6f, Offset(96f, 304f))
            val foam = Path().apply {
                moveTo(40f, 322f + sin(time) * 2f); quadraticBezierTo(74f, 313f + sin(time) * 2f, 110f, 323f + sin(time) * 2f)
                moveTo(196f, 336f + sin(time + 1f) * 2f); quadraticBezierTo(230f, 327f + sin(time + 1f) * 2f, 266f, 337f + sin(time + 1f) * 2f)
            }
            drawPath(foam, Color(0xFFBFE9F5), style = Stroke(2.6f, cap = StrokeCap.Round))
        }

        Ambience.DESERT -> {
            // tumbleweed rolling through the foreground
            val t = wrap(time, 15f)
            val x = -30f + t * 402f
            rotate(t * 940f, Offset(x, 334f)) {
                val ball = Path().apply {
                    moveTo(x - 8f, 334f); lineTo(x + 8f, 334f)
                    moveTo(x, 326f); lineTo(x, 342f)
                    moveTo(x - 6f, 328f); lineTo(x + 6f, 340f)
                    moveTo(x + 6f, 328f); lineTo(x - 6f, 340f)
                }
                drawPath(ball, Color(0xFFB99760), style = Stroke(1.5f))
                drawCircle(Color(0xFFB99760), 8f, Offset(x, 334f), style = Stroke(1.5f))
            }
        }

        Ambience.HIGHLAND -> {
            for (k in 0 until 3) {
                drawOval(
                    Color(0xFFB6C4B8),
                    Offset(14f + k * 8f, 318f - k * 4f), Size(20f - k * 4f, 11f - k * 2f)
                )
            }
        }

        Ambience.ICE -> {
            // a snow fox trots past every twenty seconds or so
            val t = wrap(time, 21f)
            if (t < 0.42f) {
                val p = t / 0.42f
                translate(-40f + p * 420f, 0f) {
                    val fox = Path().apply {
                        moveTo(-9f, 335f); quadraticBezierTo(-9f, 329f, -4f, 328f)
                        quadraticBezierTo(-1f, 324f, 3f, 326f)
                        quadraticBezierTo(7f, 326f, 8f, 330f)
                        lineTo(8f, 335f); lineTo(6.4f, 335f); lineTo(6.4f, 332f)
                        lineTo(3.4f, 332f); lineTo(3.4f, 335f); lineTo(1.8f, 335f); lineTo(1.8f, 332f)
                        lineTo(-3.2f, 332f); lineTo(-3.2f, 335f); close()
                    }
                    drawPath(fox, Color(0xFFF2F6FA))
                    drawPath(
                        Path().apply { moveTo(8f, 330f); lineTo(11f, 325f); lineTo(13f, 330f); close() },
                        Color(0xFFF2F6FA)
                    )
                    drawPath(
                        Path().apply {
                            moveTo(-9f, 332f); quadraticBezierTo(-15f, 330f, -16f, 326f)
                            quadraticBezierTo(-11f, 326f, -8f, 330f); close()
                        },
                        Color(0xFFF2F6FA)
                    )
                    drawCircle(Color(0xFF8FA5B8), 0.9f, Offset(11f, 328f))
                }
            }
        }
    }

    // Cumulative visitor: arrives once and never leaves.
    if (totalPlants >= VISITORS[3].threshold) {
        val t = wrap(time, 17f)
        if (t < 0.36f) {
            val p = t / 0.36f
            val hop = abs(sin(p * 18f)) * 9f
            translate(-40f + p * 420f, -hop) { rabbit(0f, 338f, 1f) }
        }
    }
    if (totalPlants >= VISITORS[0].threshold) {
        val a = time * 1.1f
        val bx = 196f + sin(a) * 22f
        val by = 212f + cos(a * 1.7f) * 8f
        drawOval(Color(0xFFF5C542), Offset(bx - 3.4f, by - 2.4f), Size(6.8f, 4.8f))
        drawOval(Color.White.copy(alpha = 0.8f), Offset(bx - 3f, by - 4f), Size(6f, 2.8f))
    }
}

/** The character. Reacts to the place, never to the student's absence. */
fun DrawScope.drawSprout(x: Float, y: Float, time: Float) {
    val bob = sin(time * 1.6f) * 1.6f
    translate(0f, bob) {
        drawOval(Color.Black.copy(alpha = 0.13f), Offset(x - 11f, y - 3f), Size(22f, 8f))
        drawCircle(Color(0xFF5FC788), 13f, Offset(x, y - 13f))
        drawCircle(Color(0xFF7FD8A6), 5.4f, Offset(x - 4f, y - 17f))
        val leaves = Path().apply {
            moveTo(x, y - 25f); quadraticBezierTo(x - 1f, y - 33f, x - 7f, y - 35f)
            quadraticBezierTo(x - 6f, y - 27f, x, y - 25f); close()
            moveTo(x + 1f, y - 25f); quadraticBezierTo(x + 3f, y - 32f, x + 9f, y - 33f)
            quadraticBezierTo(x + 7f, y - 26f, x + 1f, y - 25f); close()
        }
        drawPath(leaves, Color(0xFF3FA878))
        drawCircle(Color(0xFF16211C), 2.2f, Offset(x - 4.6f, y - 13f))
        drawCircle(Color(0xFF16211C), 2.2f, Offset(x + 4.6f, y - 13f))
        drawCircle(Color.White, 0.9f, Offset(x - 3.8f, y - 14f))
        drawCircle(Color.White, 0.9f, Offset(x + 5.4f, y - 14f))
        val smile = Path().apply {
            moveTo(x - 3.4f, y - 7.6f); quadraticBezierTo(x, y - 4.4f, x + 3.4f, y - 7.6f)
        }
        drawPath(smile, Color(0xFF16211C), style = Stroke(1.5f, cap = StrokeCap.Round))
        drawCircle(Color(0xFFFF9EC1).copy(alpha = 0.55f), 2.4f, Offset(x - 9.6f, y - 8.6f))
        drawCircle(Color(0xFFFF9EC1).copy(alpha = 0.55f), 2.4f, Offset(x + 9.6f, y - 8.6f))
    }
}

/** The compounding object: grows one stage per finished chapter, follows you everywhere. */
fun DrawScope.drawHeritageTree(x: Float, y: Float, stage: Float, time: Float) {
    val sway = sin(time * 0.7f) * 0.6f
    rotate(sway, Offset(x, y)) {
        drawSpecies(
            SpeciesTable["oak"].copy(
                c0 = Color(0xFF2A7F58), c1 = Color(0xFF3FA878), c2 = Color(0xFF5FC788),
                trunk = 44f, canopy = 24f
            ),
            x, y, stage, 0.8f, 4
        )
    }
}
