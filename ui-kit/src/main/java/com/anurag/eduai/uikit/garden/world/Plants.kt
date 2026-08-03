package com.anurag.eduai.uikit.garden.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val DEG = (PI / 180.0).toFloat()

private fun DrawScope.contactShadow(x: Float, y: Float, w: Float) {
    drawOval(
        color = Color.Black.copy(alpha = 0.11f),
        topLeft = Offset(x - w, y + 1f - w * 0.3f),
        size = Size(w * 2f, w * 0.6f)
    )
}

private fun DrawScope.leafShape(x: Float, y: Float, dir: Float, len: Float, c: Color) {
    val p = Path().apply {
        moveTo(x, y)
        quadraticBezierTo(x + 0.62f * len * dir, y - 0.52f * len, x + len * dir, y + 0.05f * len)
        quadraticBezierTo(x + 0.48f * len * dir, y + 0.49f * len, x, y)
        close()
    }
    drawPath(p, c)
    val rib = Path().apply {
        moveTo(x, y)
        quadraticBezierTo(x + 0.55f * len * dir, y - 0.20f * len, x + 0.90f * len * dir, y + 0.02f * len)
    }
    drawPath(rib, shade(c, -0.24f).copy(alpha = 0.7f), style = Stroke(maxOf(0.5f, len * 0.05f)))
}

private fun DrawScope.petalRing(
    cx: Float, cy: Float, radius: Float, count: Int, rx: Float, ry: Float, color: Color, offset: Float
) {
    for (k in 0 until count) {
        val deg = k * (360f / count) + offset
        val a = deg * DEG
        val px = cx + cos(a) * radius
        val py = cy + sin(a) * radius
        rotate(deg, Offset(px, py)) {
            drawOval(color, Offset(px - rx, py - ry), Size(rx * 2f, ry * 2f))
        }
    }
}

private fun DrawScope.drawDisc(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    val h = (9f + sp.height * g) * s
    val lean = (hash(seed, 1) - 0.5f) * 12f
    val tx = x + lean * s
    val stemColor = sp.stemColor ?: Color(0xFF3E9E6B)
    contactShadow(x, y, 11f * s)

    val stem = Path().apply {
        moveTo(x, y)
        cubicTo(x - 4f * s, y - h * 0.5f, tx + 4f * s, y - h * 0.72f, tx, y - h)
    }
    drawPath(stem, shade(stemColor, -0.10f), style = Stroke(3f * s, cap = StrokeCap.Round))

    val leaves = min(3, (g * 4f).toInt())
    for (j in 0 until leaves) {
        val dir = if (j % 2 == 1) 1f else -1f
        val c = sp.leafColor ?: if (j % 2 == 1) Color(0xFF3FA878) else Color(0xFF2F8A60)
        leafShape(x + lean * s * (0.26f + j * 0.2f), y - h * (0.26f + j * 0.2f), dir, (13f + 3f * j) * s, c)
    }

    if (g > 0.42f) {
        val t = (g - 0.42f) / 0.58f
        val r = (sp.radius * 0.52f + sp.radius * 0.48f * t) * s
        val cy = y - h
        val step = 360f / sp.petals
        petalRing(tx, cy, r, sp.petals, sp.radius * 0.80f * s, sp.radius * 0.40f * s, shade(sp.c0, -0.22f), 0f)
        petalRing(tx, cy, r * 0.9f, sp.petals, sp.radius * 0.72f * s, sp.radius * 0.35f * s, sp.c0, step / 2f)
        petalRing(tx, cy, r * 0.6f, sp.petals, sp.radius * 0.46f * s, sp.radius * 0.24f * s, sp.c1, 0f)

        val cr = (sp.radius * 0.40f + sp.radius * 0.20f * t) * s
        drawCircle(sp.c2, cr, Offset(tx, cy))
        drawCircle(shade(sp.c2, 0.30f), cr * 0.5f, Offset(tx - cr * 0.28f, cy - cr * 0.28f))
        for (d in 0 until 6) {
            val a = d * 60f * DEG
            drawCircle(shade(sp.c2, -0.30f), cr * 0.15f, Offset(tx + cos(a) * cr * 0.52f, cy + sin(a) * cr * 0.52f))
        }
    } else {
        drawCircle(Color(0xFF4FB483), 3.8f * s, Offset(tx, y - h))
    }
}

private fun DrawScope.drawCup(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    contactShadow(x, y, 12f * s)
    val offsets = listOf(-12f to 0.86f, 1f to 1.0f, 13f to 0.78f).take(sp.stems)
    offsets.forEachIndexed { j, (dx, mul) ->
        val h = (11f + sp.height * g) * s * mul
        val xx = x + dx * s + (hash(seed, j) - 0.5f) * 3f * s
        val stem = Path().apply {
            moveTo(xx, y)
            quadraticBezierTo(xx + 2f * s, y - h * 0.5f, xx, y - h)
        }
        drawPath(stem, Color(0xFF37946A), style = Stroke(2.4f * s, cap = StrokeCap.Round))

        val side = if (j % 2 == 1) -1f else 1f
        val blade = Path().apply {
            moveTo(xx, y - 2f * s)
            quadraticBezierTo(xx + 11f * s * side, y - h * 0.40f, xx + 4f * s * side, y - h * 0.74f)
            quadraticBezierTo(xx + 1f * s * side, y - h * 0.38f, xx, y - 2f * s)
            close()
        }
        drawPath(blade, sp.leafColor ?: Color(0xFF2F8A60))

        if (g > 0.40f) {
            val t = min(1f, (g - 0.40f) / 0.42f)
            val w = sp.cupWidth * s * (0.62f + 0.38f * t)
            val hh = sp.cupHeight * s * (0.62f + 0.38f * t)
            val top = y - h
            val dark = shade(sp.c0, -0.20f)
            if (sp.bell) {
                val outer = Path().apply {
                    moveTo(xx - w, top)
                    quadraticBezierTo(xx - w, top + hh, xx, top + hh)
                    quadraticBezierTo(xx + w, top + hh, xx + w, top)
                    close()
                }
                drawPath(outer, dark)
                val inner = Path().apply {
                    moveTo(xx - w * 0.72f, top)
                    quadraticBezierTo(xx - w * 0.72f, top + hh * 0.86f, xx, top + hh * 0.86f)
                    quadraticBezierTo(xx + w * 0.72f, top + hh * 0.86f, xx + w * 0.72f, top)
                    close()
                }
                drawPath(inner, sp.c0)
                drawCircle(shade(sp.c1, 0.28f), w * 0.3f, Offset(xx, top + hh * 0.55f))
            } else {
                val outer = Path().apply {
                    moveTo(xx - w, top)
                    quadraticBezierTo(xx - w, top - hh, xx, top - hh * 1.06f)
                    quadraticBezierTo(xx + w, top - hh, xx + w, top)
                    quadraticBezierTo(xx, top + hh * 0.30f, xx - w, top)
                    close()
                }
                drawPath(outer, dark)
                val mid = Path().apply {
                    moveTo(xx - w * 0.78f, top)
                    quadraticBezierTo(xx - w * 0.78f, top - hh * 0.92f, xx, top - hh * 0.98f)
                    quadraticBezierTo(xx + w * 0.78f, top - hh * 0.92f, xx + w * 0.78f, top)
                    quadraticBezierTo(xx, top + hh * 0.24f, xx - w * 0.78f, top)
                    close()
                }
                drawPath(mid, sp.c0)
                val inner = Path().apply {
                    moveTo(xx - w * 0.36f, top - hh * 0.10f)
                    quadraticBezierTo(xx - w * 0.36f, top - hh * 0.82f, xx, top - hh * 0.88f)
                    quadraticBezierTo(xx + w * 0.36f, top - hh * 0.82f, xx + w * 0.36f, top - hh * 0.10f)
                    close()
                }
                drawPath(inner, sp.c1)
            }
        } else {
            drawOval(
                Color(0xFF4FB483),
                Offset(xx - 2.8f * s, y - h - 2f * s - 4.8f * s),
                Size(5.6f * s, 9.6f * s)
            )
        }
    }
}

private fun DrawScope.drawSpike(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    contactShadow(x, y, 11f * s)
    val xs = listOf(-10f, -3f, 4f, 11f, -16f, 17f).take(sp.stalks)
    val stemColor = sp.stemColor ?: Color(0xFF5F9E82)
    xs.forEachIndexed { j, dx ->
        val h = (9f + sp.height * g) * s * (0.84f + hash(seed, j) * 0.18f)
        val xx = x + dx * s
        val stem = Path().apply {
            moveTo(xx, y)
            quadraticBezierTo(xx + (if (j % 2 == 1) 2f else -2f) * s, y - h * 0.5f, xx, y - h)
        }
        drawPath(stem, stemColor, style = Stroke(1.9f * s, cap = StrokeCap.Round))

        if (sp.cylinder) {
            if (g > 0.4f) {
                val ch = 16f * s * g
                drawRoundRectCompat(sp.c0, xx - 2.9f * s, y - h, 5.8f * s, ch, 2.9f * s)
                drawRoundRectCompat(shade(sp.c0, 0.24f).copy(alpha = 0.65f), xx - 2.9f * s, y - h, 2.1f * s, ch, 1.05f * s)
            }
        } else {
            val n = (g * 8f).toInt()
            for (f in 0 until n) {
                val fy = y - h + f * 4.1f * s
                val fr = (2.6f - f * 0.10f) * s
                val fx = xx + (if (f % 2 == 1) 2.1f else -2.1f) * s
                drawCircle(if (f % 2 == 1) sp.c1 else sp.c0, fr, Offset(fx, fy))
                if (f % 3 == 0) {
                    drawCircle(shade(sp.c1, 0.34f), fr * 0.38f, Offset(xx + (if (f % 2 == 1) 1.1f else -3.1f) * s, fy - 0.7f * s))
                }
            }
        }
    }
    val lc = sp.leafColor ?: Color(0xFF7FB39C)
    leafShape(x - 6f * s, y - 2f * s, -1f, 12f * s, lc)
    leafShape(x + 6f * s, y - 2f * s, 1f, 12f * s, lc)
}

private fun DrawScope.drawBush(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    val r = (8f + sp.radius * g) * s
    contactShadow(x, y, r)
    val f = sp.foliage
    drawPath(Path().apply { moveTo(x, y); lineTo(x, y - r * 0.5f) }, shade(f[0], -0.25f), style = Stroke(2.6f * s))
    drawCircle(f[0], r * 0.68f, Offset(x - r * 0.44f, y - r * 0.68f))
    drawCircle(f[0], r * 0.64f, Offset(x + r * 0.46f, y - r * 0.62f))
    drawCircle(f[1], r * 0.80f, Offset(x, y - r * 1.02f))
    drawCircle(f[2], r * 0.48f, Offset(x - r * 0.28f, y - r * 1.20f))
    drawCircle(f[2].copy(alpha = 0.78f), r * 0.34f, Offset(x + r * 0.34f, y - r * 1.06f))

    val blooms = maxOf(1, (g * 6f).toInt())
    for (j in 0 until blooms) {
        val a = (j * 67f + hash(seed, j) * 40f) * DEG
        val bx = x + cos(a) * r * 0.62f
        val by = y - r * 0.98f + sin(a) * r * 0.52f
        val br = 4.4f * s
        drawCircle(shade(sp.c0, -0.18f), br, Offset(bx, by))
        drawCircle(sp.c0, br * 0.68f, Offset(bx, by))
        drawCircle(sp.c1, br * 0.36f, Offset(bx, by))
        drawCircle(shade(sp.c1, 0.45f), br * 0.14f, Offset(bx - br * 0.22f, by - br * 0.22f))
    }
}

private fun DrawScope.drawTree(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    val t = (11f + sp.trunk * g) * s
    val r = (7f + sp.canopy * g) * s
    contactShadow(x, y, r * 0.95f)
    val lean = (hash(seed, 2) - 0.5f) * 10f * s
    val tx = x + lean

    val trunk = Path().apply {
        moveTo(x - 3.6f * s, y)
        quadraticBezierTo(x - 3.6f * s + 1.3f * s + lean * 0.4f, y - t * 0.6f, x - 3.6f * s + 0.7f * s + lean, y - t)
        lineTo(x - 3.6f * s + 0.7f * s + lean + 5.8f * s, y - t)
        quadraticBezierTo(x + 2.2f * s + lean * 0.6f, y - t * 0.6f, x + 2.9f * s, y)
        close()
    }
    drawPath(trunk, sp.bark)

    if (sp.treeForm == TreeForm.TRI) {
        drawPath(Path().apply {
            moveTo(tx - r, y - t * 0.86f); lineTo(tx, y - t - r * 1.45f); lineTo(tx + r, y - t * 0.86f); close()
        }, shade(sp.c0, -0.14f))
        drawPath(Path().apply {
            moveTo(tx - r * 0.82f, y - t - r * 0.42f); lineTo(tx, y - t - r * 2f); lineTo(tx + r * 0.82f, y - t - r * 0.42f); close()
        }, sp.c0)
        drawPath(Path().apply {
            moveTo(tx - r * 0.58f, y - t - r * 1.1f); lineTo(tx, y - t - r * 2.45f); lineTo(tx + r * 0.58f, y - t - r * 1.1f); close()
        }, sp.c1)
        if (sp.snowCap) {
            drawPath(Path().apply {
                moveTo(tx - r * 0.34f, y - t - r * 1.72f); lineTo(tx, y - t - r * 2.43f); lineTo(tx + r * 0.34f, y - t - r * 1.72f); close()
            }, Color.White.copy(alpha = 0.92f))
        }
    } else if (sp.treeForm == TreeForm.FAN) {
        for (f in 0 until 7) {
            val a = (-168f + f * 24f) * DEG
            val c = if (f % 2 == 1) sp.c0 else sp.c1
            val ex = tx + cos(a) * r * 1.85f
            val ey = y - t + sin(a) * r * 1.5f + r * 0.36f
            val frondPath = Path().apply {
                moveTo(tx, y - t)
                quadraticBezierTo(tx + cos(a) * r * 0.9f, y - t + sin(a) * r * 0.9f, ex, ey)
            }
            drawPath(frondPath, c, style = Stroke(4.2f * s, cap = StrokeCap.Round))
            drawPath(frondPath, shade(c, -0.28f).copy(alpha = 0.55f), style = Stroke(1.1f * s))
        }
        if (sp.fruit && g > 0.65f) {
            for (fr in 0 until 3) {
                drawCircle(sp.c2, 3.1f * s, Offset(tx - 4.6f * s + fr * 4.6f * s, y - t + 4f * s))
            }
        }
    } else {
        drawCircle(shade(sp.c0, -0.12f), r * 0.68f, Offset(tx - r * 0.62f, y - t - r * 0.06f))
        drawCircle(shade(sp.c0, -0.04f), r * 0.70f, Offset(tx + r * 0.60f, y - t - r * 0.16f))
        drawCircle(sp.c1, r * 0.88f, Offset(tx, y - t - r * 0.62f))
        drawCircle(sp.c2, r * 0.50f, Offset(tx - r * 0.30f, y - t - r * 0.84f))
        drawCircle(sp.c2.copy(alpha = 0.82f), r * 0.38f, Offset(tx + r * 0.40f, y - t - r * 0.50f))
        if (sp.dots) {
            val n = (g * 9f).toInt()
            for (d in 0 until n) {
                val a = d * 40f * DEG
                drawCircle(
                    Color.White.copy(alpha = 0.92f), 2.6f * s,
                    Offset(tx + cos(a) * r * 0.72f, y - t - r * 0.34f + sin(a) * r * 0.56f)
                )
            }
        }
    }
}

private fun DrawScope.drawRosette(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    val n = sp.fronds
    val len = (9f + sp.frondLen * g) * s
    contactShadow(x, y, len * 0.75f)
    for (j in 0 until n) {
        val a = (-172f + j * (164f / (n - 1))) * DEG
        val ex = x + cos(a) * len
        val ey = y - kotlin.math.abs(sin(a)) * len * 0.95f
        val c = if (j % 2 == 1) sp.c0 else sp.c1
        if (sp.frond) {
            val p = Path().apply {
                moveTo(x, y)
                quadraticBezierTo((x + ex) / 2f, ey - len * 0.36f, ex, ey)
            }
            drawPath(p, c, style = Stroke(3f * s, cap = StrokeCap.Round))
        } else {
            val p = Path().apply {
                moveTo(x, y)
                quadraticBezierTo((x + ex) / 2f, ey - len * 0.22f, ex, ey)
                quadraticBezierTo((x + ex) / 2f, ey + len * 0.17f, x, y)
                close()
            }
            drawPath(p, c)
        }
    }
    if (sp.bloom && g > 0.55f) {
        drawCircle(shade(sp.c2, -0.18f), 5f * s, Offset(x, y - len * 0.58f))
        drawCircle(sp.c2, 2.9f * s, Offset(x, y - len * 0.58f))
        drawCircle(shade(sp.c2, 0.40f), 1.1f * s, Offset(x - 1.2f * s, y - len * 0.58f - 1.2f * s))
    }
}

private fun DrawScope.drawShroom(sp: Species, x: Float, y: Float, g: Float, s: Float) {
    contactShadow(x, y, 10f * s)
    val caps = listOf(0f to 1f, -11f to 0.72f, 9f to 0.6f)
    caps.forEach { (dx, mul) ->
        val h = (6f + 15f * g) * s * mul
        val xx = x + dx * s
        val w = (6.5f + 5f * g) * s * mul
        drawRoundRectCompat(Color(0xFFF4F1E6), xx - 2.2f * s * mul, y - h, 4.4f * s * mul, h, 2.2f * s)
        drawRoundRectCompat(Color(0xFFDFD9C8), xx - 2.2f * s * mul, y - h, 1.6f * s * mul, h, 0.9f * s)
        val outer = Path().apply {
            moveTo(xx - w, y - h)
            quadraticBezierTo(xx - w, y - h - w * 1.16f, xx, y - h - w * 1.16f)
            quadraticBezierTo(xx + w, y - h - w * 1.16f, xx + w, y - h)
            close()
        }
        drawPath(outer, shade(sp.c0, -0.16f))
        val inner = Path().apply {
            moveTo(xx - w * 0.82f, y - h - w * 0.12f)
            quadraticBezierTo(xx - w * 0.82f, y - h - w * 1.08f, xx, y - h - w * 1.08f)
            quadraticBezierTo(xx + w * 0.82f, y - h - w * 1.08f, xx + w * 0.82f, y - h - w * 0.12f)
            close()
        }
        drawPath(inner, sp.c0)
        for (d in 0 until 3) {
            drawCircle(
                Color.White, 1.5f * s,
                Offset(xx - w * 0.45f + d * w * 0.45f, y - h - w * 0.54f + (d % 2) * w * 0.30f)
            )
        }
    }
}

internal fun DrawScope.drawRoundRectCompat(
    color: Color, left: Float, top: Float, w: Float, h: Float, radius: Float
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
    )
}

private fun DrawScope.drawGrass(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    contactShadow(x, y, 12f * s)
    val n = sp.stalks
    for (j in 0 until n) {
        val dir = if (j % 2 == 1) 1f else -1f
        val h = (13f + sp.height * g) * s * (0.58f + hash(seed, j) * 0.52f)
        val sp2 = (j - n / 2f) * 2.8f * s
        val c = if (j % 2 == 1) sp.c0 else sp.c1
        val blade = Path().apply {
            moveTo(x + sp2, y)
            quadraticBezierTo(x + sp2 + dir * 8f * s, y - h * 0.6f, x + sp2 + dir * 18f * s, y - h)
        }
        drawPath(blade, c, style = Stroke(2f * s, cap = StrokeCap.Round))
        if (g > 0.55f && j % 3 == 0) {
            val hx = x + sp2 + dir * 18f * s
            rotate(dir * 28f, Offset(hx, y - h)) {
                drawOval(sp.c2, Offset(hx - 2.2f * s, y - h - 5.4f * s), Size(4.4f * s, 10.8f * s))
            }
        }
    }
}

private fun DrawScope.drawBroad(sp: Species, x: Float, y: Float, g: Float, s: Float) {
    contactShadow(x, y, 14f * s)
    val n = sp.fronds
    val len = (11f + sp.frondLen * g) * s
    for (j in 0 until n) {
        val a = (-160f + j * (140f / (n - 1))) * DEG
        val ex = x + cos(a) * len * 1.1f
        val ey = y - kotlin.math.abs(sin(a)) * len
        val c = if (j % 2 == 1) sp.c0 else sp.c1
        val leaf = Path().apply {
            moveTo(x, y)
            quadraticBezierTo((x + ex) / 2f - 8f * s, ey - len * 0.3f, ex, ey)
            quadraticBezierTo((x + ex) / 2f + 6f * s, ey + len * 0.28f, x, y)
            close()
        }
        drawPath(leaf, c)
        val lit = Path().apply {
            moveTo(x, y)
            quadraticBezierTo((x + ex) / 2f - 3f * s, ey - len * 0.14f, ex, ey)
            quadraticBezierTo((x + ex) / 2f + 2f * s, ey + len * 0.1f, x, y)
            close()
        }
        drawPath(lit, shade(c, 0.18f).copy(alpha = 0.5f))
        drawPath(
            Path().apply { moveTo(x, y); lineTo(ex, ey) },
            shade(c, -0.32f).copy(alpha = 0.7f), style = Stroke(1f * s)
        )
        if (sp.notch) {
            drawPath(
                Path().apply {
                    moveTo((x + ex) / 2f, (y + ey) / 2f - 3f * s)
                    lineTo((x + ex) / 2f + 5f * s, (y + ey) / 2f - 6.4f * s)
                },
                Color(0xFF8FD1A9), style = Stroke(2.4f * s)
            )
        }
    }
    if (sp.fruit && g > 0.65f) {
        drawPath(
            Path().apply {
                moveTo(x + 4f * s, y - len * 0.5f)
                quadraticBezierTo(x + 11f * s, y - len * 0.5f + 3f * s, x + 13f * s, y - len * 0.5f - 3f * s)
            },
            sp.c2, style = Stroke(4.4f * s, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawCushion(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    val r = (8f + sp.radius * g) * s
    contactShadow(x, y, r)
    drawOval(sp.c0, Offset(x - r, y - r * 0.92f), Size(r * 2f, r * 1.28f))
    drawOval(sp.c1, Offset(x - r * 0.96f, y - r * 0.92f), Size(r * 1.12f, r * 0.84f))
    drawOval(sp.c1, Offset(x + r * 0.01f, y - r * 0.77f), Size(r * 0.92f, r * 0.7f))
    drawOval(shade(sp.c1, 0.2f), Offset(x - r * 0.44f, y - r * 0.96f), Size(r * 0.68f, r * 0.48f))
    val n = (g * 8f).toInt()
    for (j in 0 until n) {
        drawCircle(
            sp.c2, 1.9f * s,
            Offset(x - r * 0.74f + hash(seed, j) * r * 1.48f, y - r * 0.5f - hash(seed, j + 9) * r * 0.45f)
        )
    }
}

private fun DrawScope.drawCactus(sp: Species, x: Float, y: Float, g: Float, s: Float) {
    contactShadow(x, y, 11f * s)
    if (sp.pear) {
        listOf(Triple(0f, 0f, 1f), Triple(-11f, -8f, 0.8f), Triple(10f, -10f, 0.75f))
            .forEachIndexed { j, (dx, dy, mul) ->
                val w = (9f + 5f * g) * s * mul
                val h = (12f + 9f * g) * s * mul
                val cx = x + dx * s
                val cy = y + dy * s - h * 0.7f
                val c = if (j % 2 == 1) sp.c1 else sp.c0
                drawOval(c, Offset(cx - w, cy - h), Size(w * 2f, h * 2f))
                drawOval(shade(c, 0.24f).copy(alpha = 0.55f), Offset(cx - w * 0.64f, cy - h * 0.72f), Size(w * 0.68f, h * 1.16f))
                for (k in 0 until 4) {
                    drawCircle(
                        Color.White.copy(alpha = 0.7f), 0.9f * s,
                        Offset(cx - w * 0.4f + k * w * 0.28f, cy - h * 0.3f + (k % 2) * h * 0.42f)
                    )
                }
                if (g > 0.65f) drawCircle(sp.c2, 3.1f * s, Offset(cx, cy - h * 0.98f))
            }
        return
    }
    val h = (13f + 40f * g) * s
    drawRoundRectCompat(sp.c0, x - 6f * s, y - h, 12f * s, h, 6f * s)
    drawRoundRectCompat(shade(sp.c0, 0.24f).copy(alpha = 0.6f), x - 4.4f * s, y - h + 2f * s, 2.6f * s, h - 4f * s, 1.3f * s)
    drawPath(
        Path().apply { moveTo(x + 2.2f * s, y - h + 3f * s); lineTo(x + 2.2f * s, y - 3f * s) },
        shade(sp.c0, -0.22f), style = Stroke(1f * s)
    )
    if (g > 0.45f) {
        drawPath(
            Path().apply {
                moveTo(x - 6f * s, y - h * 0.58f); lineTo(x - 13f * s, y - h * 0.58f)
                lineTo(x - 13f * s, y - h * 0.84f)
            },
            sp.c1, style = Stroke(7f * s, cap = StrokeCap.Round)
        )
        drawPath(
            Path().apply {
                moveTo(x + 6f * s, y - h * 0.7f); lineTo(x + 12.5f * s, y - h * 0.7f)
                lineTo(x + 12.5f * s, y - h * 0.9f)
            },
            sp.c1, style = Stroke(6f * s, cap = StrokeCap.Round)
        )
    }
    if (g > 0.8f) {
        drawCircle(shade(sp.c2, -0.15f), 3.8f * s, Offset(x, y - h - 2.4f * s))
        drawCircle(shade(sp.c2, 0.3f), 1.9f * s, Offset(x, y - h - 2.4f * s))
    }
}

/** Single entry point. Growth g runs 0..1; s is the plot scale. */
fun DrawScope.drawSpecies(sp: Species, x: Float, y: Float, g: Float, s: Float, seed: Int) {
    if (g < 0.10f) {
        contactShadow(x, y, 7f * s)
        drawOval(Color(0xFFB79B7C), Offset(x - 5f * s, y - 5.5f * s), Size(10f * s, 6f * s))
        return
    }
    when (sp.arch) {
        Arch.DISC -> drawDisc(sp, x, y, g, s, seed)
        Arch.CUP -> drawCup(sp, x, y, g, s, seed)
        Arch.SPIKE -> drawSpike(sp, x, y, g, s, seed)
        Arch.BUSH -> drawBush(sp, x, y, g, s, seed)
        Arch.TREE -> drawTree(sp, x, y, g, s, seed)
        Arch.ROSETTE -> drawRosette(sp, x, y, g, s, seed)
        Arch.SHROOM -> drawShroom(sp, x, y, g, s)
        Arch.GRASS -> drawGrass(sp, x, y, g, s, seed)
        Arch.BROAD -> drawBroad(sp, x, y, g, s)
        Arch.CUSHION -> drawCushion(sp, x, y, g, s, seed)
        Arch.CACTUS -> drawCactus(sp, x, y, g, s)
    }
}
