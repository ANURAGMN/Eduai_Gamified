package com.anurag.eduai.uikit.garden.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Geometry is authored in the fixed 332 x 352 space, so every Path here is
 * size-independent and can be built once per habitat instead of once per frame.
 */
class TerrainPaths(private val habitat: Habitat) {

    private fun band(y: Float, k: Int): Path = Path().apply {
        moveTo(-6f, y)
        quadraticBezierTo(58f + k * 14f, y - 13f, 138f + k * 10f, y)
        quadraticBezierTo(218f + k * 6f, y + 13f, 338f, y - 7f)
        lineTo(338f, SCENE_H)
        lineTo(-6f, SCENE_H)
        close()
    }

    val bands: List<Path> = listOf(band(100f, 0), band(158f, 1), band(242f, 2))

    /** Rolling hills, mesas or jagged peaks depending on where you are. */
    val far: List<Path> = when (habitat.ambience) {
        Ambience.DESERT -> listOf(
            Path().apply {
                moveTo(-14f, 106f); lineTo(18f, 66f); lineTo(74f, 66f); lineTo(96f, 106f); close()
            },
            Path().apply {
                moveTo(188f, 104f); lineTo(214f, 58f); lineTo(286f, 58f); lineTo(312f, 104f); close()
            },
            Path().apply {
                moveTo(104f, 106f); quadraticBezierTo(140f, 84f, 176f, 106f); close()
            }
        )
        Ambience.HIGHLAND, Ambience.ICE -> listOf(
            Path().apply {
                moveTo(-14f, 108f); lineTo(54f, 26f); lineTo(120f, 108f); close()
            },
            Path().apply {
                moveTo(92f, 108f); lineTo(184f, 14f); lineTo(276f, 108f); close()
            },
            Path().apply {
                moveTo(236f, 108f); lineTo(302f, 44f); lineTo(352f, 108f); close()
            }
        )
        else -> listOf(
            Path().apply { moveTo(-14f, 92f); quadraticBezierTo(64f, 48f, 148f, 92f); close() },
            Path().apply { moveTo(118f, 96f); quadraticBezierTo(210f, 40f, 306f, 96f); close() }
        )
    }

    /** Snow on the two tallest peaks. */
    val snowCaps: List<Path>? =
        if (habitat.ambience == Ambience.HIGHLAND || habitat.ambience == Ambience.ICE) listOf(
            Path().apply { moveTo(30f, 62f); lineTo(54f, 26f); lineTo(78f, 62f); close() },
            Path().apply { moveTo(152f, 58f); lineTo(184f, 14f); lineTo(216f, 58f); close() }
        ) else null

    val pond: Path? = if (habitat.pond) Path().apply {
        moveTo(-6f, 268f)
        quadraticBezierTo(86f, 248f, 178f, 270f)
        quadraticBezierTo(270f, 292f, 338f, 262f)
        lineTo(338f, SCENE_H); lineTo(-6f, SCENE_H); close()
    } else null

    /**
     * Beach: the sea sits above the sand. It has to stop well clear of y=152,
     * or the back row of plots ends up standing in open water.
     */
    val surf: Path? = if (habitat.ambience == Ambience.BEACH) Path().apply {
        moveTo(-6f, 132f)
        quadraticBezierTo(80f, 120f, 168f, 136f)
        quadraticBezierTo(256f, 150f, 338f, 128f)
    } else null

    /**
     * Island: a landmass ringed by water. Wide enough that the outer back-row plots
     * at x=46 and x=268 land on soil rather than sea.
     */
    val landmass: List<Path>? = if (habitat.ambience == Ambience.ISLAND) listOf(
        Path().apply {
            moveTo(-10f, 306f)
            quadraticBezierTo(-18f, 168f, 60f, 136f)
            quadraticBezierTo(166f, 104f, 272f, 136f)
            quadraticBezierTo(350f, 168f, 342f, 306f)
            close()
        },
        Path().apply {
            moveTo(10f, 300f)
            quadraticBezierTo(4f, 180f, 78f, 152f)
            quadraticBezierTo(168f, 122f, 258f, 152f)
            quadraticBezierTo(330f, 180f, 324f, 300f)
            close()
        }
    ) else null

    val treeLine: Path? = if (habitat.ambience == Ambience.WOODS) Path().apply {
        for (i in 0 until 13) {
            val x = 6f + i * 27f
            val h = 16f + hash(i, 5) * 12f
            val b = 98f - hash(i, 9) * 6f
            moveTo(x - 6f, b); lineTo(x, b - h); lineTo(x + 6f, b); close()
        }
    } else null

    val speckles: List<Speckle> = buildList {
        for (i in 0 until 30) {
            val x = 6f + hash(i, 11) * 320f
            val y = 122f + hash(i, 12) * 214f
            val sc = 0.6f + hash(i, 13) * 0.55f
            val flower = hash(i, 14) > 0.62f
            add(Speckle(x, y, sc, flower, habitat.speckle[i % habitat.speckle.size]))
        }
    }

    data class Speckle(val x: Float, val y: Float, val scale: Float, val flower: Boolean, val color: Color)
}

private fun DrawScope.tuft(x: Float, y: Float, s: Float, c: Color) {
    val p = Path().apply {
        moveTo(x, y); quadraticBezierTo(x - 2f * s, y - 5f * s, x - 4.2f * s, y - 6.4f * s)
        moveTo(x, y); quadraticBezierTo(x, y - 6.4f * s, x + 0.6f * s, y - 8.6f * s)
        moveTo(x, y); quadraticBezierTo(x + 2f * s, y - 5f * s, x + 4.2f * s, y - 6.4f * s)
    }
    drawPath(p, c, style = Stroke(1.25f * s, cap = StrokeCap.Round))
}

fun DrawScope.drawTerrain(habitat: Habitat, paths: TerrainPaths, skyBrush: Brush) {
    drawRect(skyBrush, size = Size(SCENE_W, SCENE_H))

    val sunR = if (habitat.ambience == Ambience.DESERT) 21f else 16f
    drawCircle(habitat.sunGlow.copy(alpha = 0.5f), sunR + 5f, Offset(278f, 40f))
    drawCircle(habitat.sunCore, sunR - 5f, Offset(278f, 40f))

    if (habitat.ambience != Ambience.DESERT && habitat.ambience != Ambience.ICE) {
        drawOval(Color.White.copy(alpha = 0.65f), Offset(28f, 20f), Size(60f, 20f))
        drawOval(Color.White.copy(alpha = 0.65f), Offset(64f, 18f), Size(40f, 16f))
        drawOval(Color.White.copy(alpha = 0.50f), Offset(133f, 13f), Size(34f, 13f))
    }

    paths.far.forEachIndexed { i, p -> drawPath(p, habitat.far[i.coerceAtMost(habitat.far.size - 1)]) }
    paths.snowCaps?.forEach { drawPath(it, Color(0xFFF4F9FD)) }
    paths.treeLine?.let { drawPath(it, habitat.far[2]) }

    if (habitat.ambience == Ambience.ISLAND) {
        drawRect(Color(0xFF2E9FC4), topLeft = Offset(-6f, 96f), size = Size(SCENE_W + 12f, SCENE_H - 90f))
        drawPath(
            Path().apply {
                moveTo(-6f, 168f); quadraticBezierTo(80f, 134f, 172f, 150f)
                quadraticBezierTo(268f, 168f, 338f, 140f)
                lineTo(338f, SCENE_H); lineTo(-6f, SCENE_H); close()
            },
            Color(0xFF6FCBE0).copy(alpha = 0.55f)
        )
        paths.landmass?.let {
            drawPath(it[0], habitat.ground[0])
            drawPath(it[1], habitat.ground[1])
        }
    } else {
        paths.bands.forEachIndexed { i, p -> drawPath(p, habitat.ground[i]) }
        drawOval(Color.White.copy(alpha = 0.12f), Offset(18f, 183f), Size(108f, 26f))
        drawOval(Color.White.copy(alpha = 0.10f), Offset(190f, 254f), Size(124f, 28f))
    }

    if (habitat.ambience == Ambience.BEACH) {
        drawRect(Color(0xFF38A9CC), topLeft = Offset(-6f, 96f), size = Size(SCENE_W + 12f, 40f))
        drawPath(
            Path().apply {
                moveTo(-6f, 114f); quadraticBezierTo(76f, 104f, 158f, 116f)
                quadraticBezierTo(240f, 128f, 338f, 110f)
                lineTo(338f, 140f); lineTo(-6f, 140f); close()
            },
            Color(0xFF7FD0E2).copy(alpha = 0.7f)
        )
        paths.surf?.let { drawPath(it, Color.White.copy(alpha = 0.85f), style = Stroke(3f)) }
    }

    paths.pond?.let {
        drawPath(it, Color(0xFF5FC0D8))
        val ripple = Path().apply {
            moveTo(30f, 300f); quadraticBezierTo(56f, 295f, 82f, 302f)
            moveTo(196f, 320f); quadraticBezierTo(222f, 315f, 248f, 322f)
        }
        drawPath(ripple, Color(0xFFC6E7EC), style = Stroke(2.4f, cap = StrokeCap.Round))
    }

    val grassColor = when (habitat.ambience) {
        Ambience.WOODS -> Color(0xFF3F8E62)
        Ambience.HIGHLAND -> Color(0xFF7FA98D)
        else -> Color(0xFF4FB07C)
    }
    paths.speckles.forEach {
        // On the island the ground is a landmass, not a full band — keep speckle off the water.
        if (habitat.ambience == Ambience.ISLAND && (it.y < 168f || it.x < 42f || it.x > 296f)) return@forEach
        when (habitat.ambience) {
            Ambience.DESERT, Ambience.BEACH -> if (it.flower) {
                drawPath(
                    Path().apply {
                        moveTo(it.x - 6f * it.scale, it.y)
                        quadraticBezierTo(it.x, it.y - 2.6f * it.scale, it.x + 6f * it.scale, it.y)
                    },
                    if (habitat.ambience == Ambience.DESERT) Color(0xFFE4C892) else Color(0xFFE6D2A6),
                    style = Stroke(1.5f * it.scale, cap = StrokeCap.Round)
                )
            } else {
                drawCircle(
                    if (habitat.ambience == Ambience.DESERT) Color(0xFFD6B981) else Color(0xFFDCC79A),
                    1.5f * it.scale, Offset(it.x, it.y)
                )
            }

            Ambience.ICE -> if (it.flower) {
                drawPath(
                    Path().apply {
                        moveTo(it.x, it.y); lineTo(it.x + 3.4f * it.scale, it.y - 4.4f * it.scale)
                        lineTo(it.x + 6.8f * it.scale, it.y); close()
                    },
                    Color.White
                )
            } else drawCircle(Color.White, 1.7f * it.scale, Offset(it.x, it.y))

            Ambience.HIGHLAND -> if (it.flower) {
                drawPath(
                    Path().apply {
                        moveTo(it.x, it.y); lineTo(it.x + 4.6f * it.scale, it.y - 5.4f * it.scale)
                        lineTo(it.x + 9.2f * it.scale, it.y); close()
                    },
                    Color(0xFFC6D3C7)
                )
            } else tuft(it.x, it.y, it.scale, grassColor)

            else -> if (it.flower) drawCircle(it.color, 1.9f * it.scale, Offset(it.x, it.y))
            else tuft(it.x, it.y, it.scale, grassColor)
        }
    }
}

/** Foreground fringe, drawn last so the scene has a near edge. */
fun DrawScope.drawForeground(habitat: Habitat) {
    when (habitat.ambience) {
        Ambience.DESERT, Ambience.BEACH, Ambience.ISLAND -> {
            val c = if (habitat.ambience == Ambience.DESERT) Color(0xFFE3C58E) else Color(0xFFE4CC96)
            drawPath(
                Path().apply { moveTo(-6f, SCENE_H); quadraticBezierTo(42f, 338f, 96f, SCENE_H); close() }, c
            )
            drawPath(
                Path().apply { moveTo(244f, SCENE_H); quadraticBezierTo(294f, 336f, 344f, SCENE_H); close() }, c
            )
        }
        Ambience.ICE -> {
            drawPath(
                Path().apply { moveTo(-6f, SCENE_H); quadraticBezierTo(40f, 336f, 92f, SCENE_H); close() }, Color.White
            )
            drawPath(
                Path().apply { moveTo(240f, SCENE_H); quadraticBezierTo(292f, 332f, 344f, SCENE_H); close() }, Color.White
            )
        }
        else -> {
            val c = when (habitat.ambience) {
                Ambience.WOODS -> Color(0xFF2F6B4E)
                Ambience.HIGHLAND -> Color(0xFF6E9179)
                else -> Color(0xFF3E9E6B)
            }
            val p = Path().apply {
                moveTo(8f, 354f); quadraticBezierTo(4f, 340f, -6f, 326f)
                moveTo(22f, 354f); quadraticBezierTo(22f, 336f, 16f, 320f)
                moveTo(36f, 354f); quadraticBezierTo(42f, 338f, 52f, 328f)
                moveTo(296f, 354f); quadraticBezierTo(290f, 340f, 280f, 330f)
                moveTo(312f, 354f); quadraticBezierTo(312f, 336f, 306f, 324f)
                moveTo(326f, 354f); quadraticBezierTo(332f, 340f, 342f, 330f)
            }
            drawPath(p, c, style = Stroke(3f, cap = StrokeCap.Round))
        }
    }
}

fun DrawScope.drawDecor(kind: DecorKind, x: Float, y: Float, s: Float) {
    when (kind) {
        DecorKind.FENCE -> {
            val posts = Path().apply {
                moveTo(x - 14f * s, y); lineTo(x - 14f * s, y - 18f * s)
                moveTo(x, y + 2f * s); lineTo(x, y - 16f * s)
                moveTo(x + 14f * s, y + 4f * s); lineTo(x + 14f * s, y - 14f * s)
                moveTo(x - 16f * s, y - 13f * s); lineTo(x + 16f * s, y - 10f * s)
                moveTo(x - 16f * s, y - 5f * s); lineTo(x + 16f * s, y - 2f * s)
            }
            drawPath(posts, Color(0xFFB58B5E), style = Stroke(3f * s, cap = StrokeCap.Round))
        }
        DecorKind.BENCH -> {
            drawRoundRectCompat(Color(0xFFB58B5E), x - 14f * s, y - 9f * s, 28f * s, 4f * s, 2f * s)
            drawRoundRectCompat(Color(0xFFC79A69), x - 14f * s, y - 17f * s, 28f * s, 3.4f * s, 1.7f * s)
            val legs = Path().apply {
                moveTo(x - 11f * s, y - 5f * s); lineTo(x - 11f * s, y)
                moveTo(x + 11f * s, y - 5f * s); lineTo(x + 11f * s, y)
                moveTo(x - 11f * s, y - 17f * s); lineTo(x - 11f * s, y - 8f * s)
                moveTo(x + 11f * s, y - 17f * s); lineTo(x + 11f * s, y - 8f * s)
            }
            drawPath(legs, Color(0xFF8A6E4F), style = Stroke(2.4f * s, cap = StrokeCap.Round))
        }
        DecorKind.LANTERN -> {
            drawRoundRectCompat(Color(0xFF6E5940), x - 1.6f * s, y - 20f * s, 3.2f * s, 20f * s, 1f * s)
            drawOval(Color(0xFF6E5940), Offset(x - 6f * s, y - 2.4f * s), Size(12f * s, 4.8f * s))
            drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.5f), 9f * s, Offset(x, y - 24f * s))
            drawRoundRectCompat(Color(0xFFFFD764), x - 4.4f * s, y - 28f * s, 8.8f * s, 9f * s, 1.6f * s)
        }
        DecorKind.STONES -> {
            for (k in 0 until 4) {
                drawOval(
                    Color(0xFFC6CFC6),
                    Offset(x - 20f * s + k * 10f * s, y - k * 3.4f * s - 2.8f * s),
                    Size(12f * s, 5.6f * s)
                )
            }
        }
    }
}
