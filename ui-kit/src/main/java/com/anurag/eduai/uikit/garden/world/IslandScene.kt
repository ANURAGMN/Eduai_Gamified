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
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

private fun diamond(x: Float, y: Float): Path = Path().apply {
    moveTo(x, y - TILE_H / 2f)
    lineTo(x + TILE_W / 2f, y)
    lineTo(x, y + TILE_H / 2f)
    lineTo(x - TILE_W / 2f, y)
    close()
}

private fun DrawScope.tile(cell: Cell) {
    val x = cell.x
    val y = cell.y
    val region = cell.region
    val depth = cell.elevation * TILE_LIFT + 9f

    val left = Path().apply {
        moveTo(x - TILE_W / 2f, y); lineTo(x, y + TILE_H / 2f)
        lineTo(x, y + TILE_H / 2f + depth); lineTo(x - TILE_W / 2f, y + depth); close()
    }
    drawPath(left, region.wallDark)

    val right = Path().apply {
        moveTo(x + TILE_W / 2f, y); lineTo(x, y + TILE_H / 2f)
        lineTo(x, y + TILE_H / 2f + depth); lineTo(x + TILE_W / 2f, y + depth); close()
    }
    drawPath(right, region.wall)

    if (cell.elevation > 1f) {
        for (k in 1 until 3) {
            val o = depth * k / 3f
            val striation = Path().apply {
                moveTo(x - TILE_W / 2f, y + o); lineTo(x, y + TILE_H / 2f + o); lineTo(x + TILE_W / 2f, y + o)
            }
            drawPath(striation, Color.Black.copy(alpha = 0.07f), style = Stroke(1.2f))
        }
    }

    drawPath(diamond(x, y), region.top)

    val edge = Path().apply {
        moveTo(x - TILE_W / 2f, y); lineTo(x, y - TILE_H / 2f); lineTo(x + TILE_W / 2f, y)
    }
    drawPath(edge, shade(region.top, 0.30f).copy(alpha = 0.85f), style = Stroke(1.1f))

    if (cell.ring == 5) {
        val surf = Path().apply { moveTo(x, y + TILE_H / 2f); lineTo(x + TILE_W / 2f, y) }
        drawPath(surf, Color.White.copy(alpha = 0.5f), style = Stroke(1.4f))
    }
}

private fun DrawScope.landmark(kind: LandmarkKind, x: Float, y: Float) {
    when (kind) {
        LandmarkKind.CAIRN -> {
            drawOval(Color.Black.copy(alpha = 0.12f), Offset(x - 7f, y - 4f), Size(14f, 6f))
            drawPath(Path().apply { moveTo(x - 5f, y); lineTo(x, y - 7f); lineTo(x + 5f, y); close() }, Color(0xFF9FB3C4))
            drawPath(Path().apply { moveTo(x - 3f, y - 4f); lineTo(x, y - 9f); lineTo(x + 3f, y - 4f); close() }, Color(0xFFC4D7E6))
            drawPath(Path().apply { moveTo(x, y - 9f); lineTo(x, y - 15f); lineTo(x + 7f, y - 12.6f); close() }, Color(0xFFE24B4A))
        }
        LandmarkKind.CABIN -> {
            drawRoundRectCompat(Color(0xFFA5825E), x - 6.5f, y - 8f, 13f, 8f, 1f)
            drawRoundRectCompat(Color(0xFF8E6E4C), x - 6.5f, y - 8f, 4f, 8f, 1f)
            drawPath(Path().apply { moveTo(x - 8f, y - 8f); lineTo(x, y - 16f); lineTo(x + 8f, y - 8f); close() }, Color(0xFF8A5A3B))
            drawRoundRectCompat(Color(0xFF5F4630), x - 1.5f, y - 5f, 3f, 5f, 0.6f)
            drawRoundRectCompat(Color(0xFF6E5940), x + 2.5f, y - 18f, 2.6f, 5f, 0.6f)
        }
        LandmarkKind.WINDMILL -> {
            drawPath(Path().apply {
                moveTo(x - 5.5f, y); lineTo(x - 3.2f, y - 13f); lineTo(x + 3.2f, y - 13f); lineTo(x + 5.5f, y); close()
            }, Color(0xFFF4F1E6))
            drawPath(Path().apply { moveTo(x - 6.5f, y - 13f); lineTo(x + 6.5f, y - 13f) }, Color(0xFFB9A886), style = Stroke(2.2f))
            val blades = Path().apply {
                moveTo(x, y - 15f); lineTo(x - 9f, y - 20f)
                moveTo(x, y - 15f); lineTo(x + 9f, y - 10f)
                moveTo(x, y - 15f); lineTo(x - 5f, y - 6f)
                moveTo(x, y - 15f); lineTo(x + 5f, y - 24f)
            }
            drawPath(blades, Color(0xFF8A6E4F), style = Stroke(1.6f))
            drawCircle(Color(0xFF6E5940), 1.6f, Offset(x, y - 15f))
        }
        LandmarkKind.WELL -> {
            drawOval(Color(0xFF9BA79C), Offset(x - 7f, y - 4.2f), Size(14f, 6.4f))
            drawPath(Path().apply {
                moveTo(x - 6f, y - 1f); lineTo(x - 4.6f, y - 6f); lineTo(x + 4.6f, y - 6f); lineTo(x + 6f, y - 1f); close()
            }, Color(0xFFB7C2B6))
            val frame = Path().apply {
                moveTo(x - 5f, y - 6f); lineTo(x - 5f, y - 12f)
                moveTo(x + 5f, y - 6f); lineTo(x + 5f, y - 12f)
            }
            drawPath(frame, Color(0xFF8A6E4F), style = Stroke(1.6f))
            drawPath(Path().apply { moveTo(x - 6.5f, y - 12f); lineTo(x, y - 17f); lineTo(x + 6.5f, y - 12f); close() }, Color(0xFF8A5A3B))
        }
        LandmarkKind.LIGHTHOUSE -> {
            drawOval(Color(0xFFD8CBAE), Offset(x - 7f, y - 4f), Size(14f, 6f))
            drawRoundRectCompat(Color(0xFFF7F5EE), x - 4f, y - 18f, 8f, 17f, 1f)
            drawRoundRectCompat(Color(0xFFE24B4A), x - 4f, y - 14f, 8f, 3.6f, 0.5f)
            drawRoundRectCompat(Color(0xFFE24B4A), x - 4f, y - 7.5f, 8f, 3.6f, 0.5f)
            drawRoundRectCompat(Color(0xFFFFD764), x - 5.2f, y - 23f, 10.4f, 5.2f, 1.2f)
            drawOval(Color(0xFFFFD764).copy(alpha = 0.35f), Offset(x - 9f, y - 23.4f), Size(18f, 6f))
        }
        LandmarkKind.JETTY -> {
            drawRoundRectCompat(Color(0xFFB08A5F), x - 9f, y - 2f, 18f, 3f, 0.6f)
            val posts = Path().apply {
                moveTo(x - 7f, y + 1f); lineTo(x - 7f, y + 5f)
                moveTo(x - 1f, y + 1f); lineTo(x - 1f, y + 5f)
                moveTo(x + 5f, y + 1f); lineTo(x + 5f, y + 5f)
                moveTo(x + 6f, y - 2f); lineTo(x + 6f, y - 11f)
            }
            drawPath(posts, Color(0xFF8A6E4F), style = Stroke(1.5f))
            drawPath(Path().apply { moveTo(x + 6f, y - 11f); lineTo(x + 12f, y - 8.6f); lineTo(x + 6f, y - 6.2f); close() }, Color(0xFF12B5A6))
        }
    }
}

private fun DrawScope.feature(cell: Cell) {
    val x = cell.x
    val y = cell.y - 2f
    LANDMARK_BY_CELL[cell.index]?.let { landmark(it.kind, x, y); return }
    if (!cell.hasFeature) return

    when (cell.ring) {
        0 -> {
            drawPath(Path().apply { moveTo(x - 5f, y); lineTo(x, y - 8f); lineTo(x + 5f, y); close() }, Color.White)
            drawPath(Path().apply { moveTo(x - 2.6f, y - 1.4f); lineTo(x, y - 5.6f); lineTo(x + 2.6f, y - 1.4f); close() }, Color(0xFFD2E2EE))
        }
        1 -> if (cell.variant > 0.55f) {
            drawOval(Color.Black.copy(alpha = 0.10f), Offset(x - 6f, y - 3.6f), Size(12f, 5.2f))
            drawPath(Path().apply { moveTo(x - 5f, y); lineTo(x, y - 8f); lineTo(x + 5f, y); close() }, Color(0xFF9AAE9C))
            drawPath(Path().apply { moveTo(x - 2f, y - 4.8f); lineTo(x, y - 8f); lineTo(x + 2f, y - 4.8f); close() }, Color(0xFFD6E2E8))
        } else {
            drawRoundRectCompat(Color(0xFF6B5B44), x - 0.8f, y - 3f, 1.6f, 3f, 0.4f)
            drawPath(Path().apply { moveTo(x - 4f, y - 2f); lineTo(x, y - 11f); lineTo(x + 4f, y - 2f); close() }, Color(0xFF2E7C58))
            drawPath(Path().apply { moveTo(x - 3f, y - 6f); lineTo(x, y - 14f); lineTo(x + 3f, y - 6f); close() }, Color(0xFF3B9169))
        }
        2 -> {
            drawOval(Color.Black.copy(alpha = 0.10f), Offset(x - 7f, y - 3.4f), Size(14f, 5.2f))
            drawRoundRectCompat(Color(0xFF6B4F35), x - 1.1f, y - 5f, 2.2f, 5f, 0.5f)
            drawPath(Path().apply { moveTo(x - 5.4f, y - 4f); lineTo(x, y - 14f); lineTo(x + 5.4f, y - 4f); close() }, Color(0xFF1B6F4C))
            drawPath(Path().apply { moveTo(x - 4.4f, y - 9f); lineTo(x, y - 18f); lineTo(x + 4.4f, y - 9f); close() }, Color(0xFF26855C))
            drawPath(Path().apply { moveTo(x - 3.2f, y - 14f); lineTo(x, y - 21f); lineTo(x + 3.2f, y - 14f); close() }, Color(0xFF33996C))
            if (cell.variant > 0.66f) {
                drawRoundRectCompat(Color(0xFF6B4F35), x + 6f, y - 4f, 1.6f, 4f, 0.4f)
                drawPath(Path().apply { moveTo(x + 3.4f, y - 3.4f); lineTo(x + 6.8f, y - 10f); lineTo(x + 10f, y - 3.4f); close() }, Color(0xFF26855C))
            }
        }
        3 -> {
            val colors = listOf(Color(0xFFFFD764), Color(0xFFFF9EC1), Color.White, Color(0xFFC9C2FF))
            for (j in 0 until 4) {
                val xx = x - 6.5f + j * 4.4f
                val h = 5f + hash(cell.index, j) * 5f
                drawPath(Path().apply { moveTo(xx, y); lineTo(xx, y - h) }, Color(0xFF3E9E6B), style = Stroke(1.5f, cap = StrokeCap.Round))
                drawCircle(colors[j % 4], 2.2f, Offset(xx, y - h - 1.8f))
            }
        }
        4 -> {
            val blades = Path().apply {
                moveTo(x - 5f, y); quadraticBezierTo(x - 3.6f, y - 7f, x - 6f, y - 9.5f)
                moveTo(x - 1f, y); quadraticBezierTo(x - 1f, y - 8f, x + 0.4f, y - 10.5f)
                moveTo(x + 3.4f, y); quadraticBezierTo(x + 2f, y - 7f, x + 4.8f, y - 9.5f)
            }
            drawPath(blades, Color(0xFF4FA57C), style = Stroke(1.5f, cap = StrokeCap.Round))
            if (cell.variant > 0.5f) {
                drawCircle(Color.White, 2f, Offset(x + 1f, y - 11f))
                drawCircle(Color(0xFFFFD764), 1.7f, Offset(x - 4f, y - 9f))
            }
        }
        else -> when {
            cell.variant > 0.62f -> {
                drawOval(Color.Black.copy(alpha = 0.10f), Offset(x - 6f, y - 3.4f), Size(12f, 4.8f))
                drawPath(
                    Path().apply { moveTo(x - 0.4f, y); quadraticBezierTo(x - 2f, y - 6f, x - 1f, y - 11f) },
                    Color(0xFFA5825E), style = Stroke(2.2f)
                )
                val fronds = Path().apply {
                    moveTo(x - 1f, y - 11f); quadraticBezierTo(x - 9f, y - 12f, x - 10.5f, y - 7f)
                    moveTo(x - 1f, y - 11f); quadraticBezierTo(x + 7f, y - 13f, x + 8.5f, y - 8f)
                    moveTo(x - 1f, y - 11f); quadraticBezierTo(x - 5f, y - 18f, x - 9.5f, y - 18.5f)
                    moveTo(x - 1f, y - 11f); quadraticBezierTo(x + 3f, y - 18f, x + 7.5f, y - 17.5f)
                }
                drawPath(fronds, Color(0xFF2E8C63), style = Stroke(1.8f, cap = StrokeCap.Round))
                drawCircle(Color(0xFF8A6E4F), 1.6f, Offset(x - 1f, y - 11f))
            }
            cell.variant > 0.35f -> {
                drawOval(Color(0xFFDFCFA8), Offset(x - 6.4f, y - 3.2f), Size(8.8f, 4.4f))
                drawOval(Color(0xFFCDB88E), Offset(x + 0.2f, y - 3.6f), Size(5.6f, 3.2f))
            }
            else -> {
                drawPath(Path().apply {
                    moveTo(x - 3f, y - 1f); quadraticBezierTo(x, y - 5f, x + 3f, y - 1f); close()
                }, Color(0xFFF7DFC8))
                drawCircle(Color(0xFFF4C0D1), 1.4f, Offset(x + 4f, y - 1f))
            }
        }
    }
}

/**
 * @param cover when true, scale to *cover* the canvas rather than fit its width: fill both axes,
 * centre horizontally and anchor to the bottom. Cropping in the draw rather than by oversizing the
 * composable and relying on clipping — the canvas is exactly the size it was given.
 */
@Composable
fun IslandScene(
    state: IslandState,
    time: Float,
    modifier: Modifier = Modifier,
    cover: Boolean = false,
    focusSpan: Float = 0f,
) {
    val skyBrush = remember {
        Brush.verticalGradient(
            listOf(Color(0xFF8FD3F0), Color(0xFFD8F0F7)),
            startY = 0f, endY = ISLAND_H
        )
    }
    val ordered = remember { ISLAND_CELLS.sortedWith(compareBy({ it.depth }, { it.ring })) }

    // Tap-to-select only when the scene is drawn whole. In `cover` mode it is a thumbnail inside a
    // card that owns the tap, and the hit-test would be wrong anyway: it un-projects with the fit
    // transform (`width / ISLAND_W`), which is not the transform the cover path draws with. A child
    // `pointerInput` also swallows the gesture before the card's own `clickable` ever sees it.
    val tappable =
        if (cover) {
            modifier
        } else {
            modifier.pointerInput(state.view) {
                detectTapGestures { offset ->
                    val unit = size.width / ISLAND_W
                    val px = offset.x / unit
                    val py = offset.y / unit
                    var best = -1
                    var bestDist = 20f
                    ISLAND_CELLS.filter { it.index < state.view }.forEach {
                        val d = hypot(it.x - px, it.y - py)
                        if (d < bestDist) { bestDist = d; best = it.index }
                    }
                    if (best >= 0) state.selected = if (state.selected == best) -1 else best
                }
            }
        }

    Canvas(tappable) {
        // See the note in TiersScene: fit / cover / focus. Focus frames a window centred on the tile
        // being earned, which in a small rail is the difference between a 9 dp diamond and a 27 dp one.
        val active = ISLAND_CELLS.getOrNull(state.view)
        val unit = when {
            focusSpan > 0f -> maxOf(size.width, size.height) / focusSpan
            cover -> maxOf(size.width / ISLAND_W, size.height / ISLAND_H)
            else -> size.width / ISLAND_W
        }
        val originX = when {
            focusSpan > 0f && active != null ->
                (size.width / 2f - active.x * unit)
                    .coerceIn(minOf(0f, size.width - ISLAND_W * unit), 0f)
            cover -> (size.width - ISLAND_W * unit) / 2f
            else -> 0f
        }
        val originY = when {
            // The island grows outward from the centre, not up from the ground, so centre the
            // window on the active tile vertically too rather than pinning it to the bottom.
            focusSpan > 0f && active != null ->
                (size.height / 2f - active.y * unit)
                    .coerceIn(minOf(0f, size.height - ISLAND_H * unit), 0f)
            cover -> size.height - ISLAND_H * unit
            else -> 0f
        }
        translate(originX, originY) {
        scale(unit, unit, pivot = Offset.Zero) {
            drawRect(skyBrush, size = Size(ISLAND_W, ISLAND_H))
            drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.5f), 20f, Offset(278f, 30f))
            drawCircle(Color(0xFFFFD764), 13f, Offset(278f, 30f))
            drawOval(Color.White.copy(alpha = 0.7f), Offset(30f, 16f), Size(60f, 20f))
            drawOval(Color.White.copy(alpha = 0.7f), Offset(66f, 14f), Size(40f, 16f))

            // gulls
            val gullShift = ((time * 12f) % 400f) - 40f
            translate(gullShift, 0f) {
                val g = Path().apply {
                    moveTo(126f, 44f); quadraticBezierTo(131f, 40f, 136f, 44f)
                    moveTo(138f, 44f); quadraticBezierTo(143f, 40f, 148f, 44f)
                }
                drawPath(g, Color.White.copy(alpha = 0.85f), style = Stroke(1.6f, cap = StrokeCap.Round))
            }

            drawRect(Color(0xFF2E9FC4), topLeft = Offset(0f, 62f), size = Size(ISLAND_W, ISLAND_H - 62f))
            drawOval(Color(0xFF49B2CF), Offset(-12f, 64f), Size(356f, 172f))
            drawOval(Color(0xFF6FCADD), Offset(16f, 80f), Size(300f, 140f))
            drawOval(Color(0xFF8FD9E8), Offset(34f, 92f), Size(264f, 116f))

            val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 9f), (time * 6f) % 16f)
            drawOval(
                Color.White.copy(alpha = 0.35f + 0.2f * abs(sin(time * 0.9f))),
                Offset(26f, 87f), Size(280f, 126f),
                style = Stroke(2f, pathEffect = dash)
            )

            // sailboat on the horizon
            val bob = sin(time * 1.2f) * 2f
            translate(0f, bob) {
                drawPath(Path().apply {
                    moveTo(32f, 96f); lineTo(49f, 96f); lineTo(46.4f, 100f); lineTo(34f, 100f); close()
                }, Color(0xFF7C6248))
                drawPath(Path().apply { moveTo(40f, 95f); lineTo(40f, 80f); lineTo(51f, 91f); close() }, Color.White)
                drawPath(Path().apply { moveTo(38.6f, 95f); lineTo(38.6f, 82f); lineTo(31.6f, 92f); close() }, Color(0xFFEAF6FB))
            }

            // unrevealed land reads as a promise, not a hole
            ISLAND_CELLS.filter { it.index >= state.view }.forEach {
                drawPath(diamond(it.x, it.y), Color.White.copy(alpha = 0.12f))
            }

            ordered.filter { it.index < state.view }.forEach { cell ->
                tile(cell)
                feature(cell)
                if (state.selected == cell.index) {
                    drawPath(diamond(cell.x, cell.y), Color(0xFF0B5E4A), style = Stroke(2.4f))
                }
            }

            // The tile being earned right now. It lowers into its slot across the seven steps rather
            // than popping in at the end, so every step has something to show. Scaling from the
            // cell's own centre avoids needing a layer for alpha.
            ISLAND_CELLS.getOrNull(state.view)?.let { cell ->
                val p = state.partial.coerceIn(0f, 1f)
                // The marker is drawn even at zero, so between tasks you can still see where the
                // next tile is going. Only the tile itself waits for the first step.
                val ring = 0.35f + 0.3f * abs(sin(time * 2.2f))
                drawPath(
                    diamond(cell.x, cell.y),
                    Color.White.copy(alpha = ring),
                    style = Stroke(1.8f)
                )
                if (p > 0.01f) {
                    val lift = (1f - p) * 26f
                    val s = 0.5f + 0.5f * p
                    translate(0f, -lift) {
                        scale(s, s, pivot = Offset(cell.x, cell.y)) {
                            tile(cell)
                            feature(cell)
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * Single island tile (+ landmark/feature) for the home collection shelf.
 * [cellIndex] is the order the tile was earned (0 = peak).
 */
@Composable
fun IslandTileThumb(
    cellIndex: Int,
    modifier: Modifier = Modifier,
) {
    val cell = ISLAND_CELLS.getOrNull(cellIndex.coerceAtLeast(0)) ?: return
    Canvas(modifier) {
        val target = Offset(size.width / 2f, size.height * 0.68f)
        val unit = (minOf(size.width, size.height) / 42f).coerceAtLeast(1.6f)
        scale(unit, unit, pivot = target) {
            translate(target.x - cell.x, target.y - cell.y) {
                tile(cell)
                feature(cell)
            }
        }
    }
}
