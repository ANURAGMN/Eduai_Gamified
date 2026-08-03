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
import kotlin.math.sin

/**
 * Six archetypes, recoloured per world, give 48 buildable things.
 * Exactly the trick the plants use — a new module is a palette, not a drawing.
 */

private fun DrawScope.groundShadow(x: Float, y: Float, w: Float) {
    drawOval(Color.Black.copy(alpha = 0.28f), Offset(x - w, y - w * 0.3f + 1f), Size(w * 2f, w * 0.6f))
}

private fun DrawScope.blinker(x: Float, y: Float, r: Float, c: Color, time: Float, phase: Float) {
    val a = 0.2f + 0.8f * abs(sin(time * 2.7f + phase))
    drawCircle(c.copy(alpha = a), r, Offset(x, y))
}

private fun DrawScope.dome(w: World, x: Float, y: Float, s: Float, time: Float) {
    val r = 16f * s
    groundShadow(x, y, r * 1.1f)
    val glass = Path().apply {
        moveTo(x - r, y)
        cubicTo(x - r, y - r * 1.34f, x + r, y - r * 1.34f, x + r, y)
        close()
    }
    drawPath(glass, w.glass.copy(alpha = 0.85f))
    val outline = Path().apply {
        moveTo(x - r, y); cubicTo(x - r, y - r * 1.34f, x + r, y - r * 1.34f, x + r, y)
    }
    drawPath(outline, w.body, style = Stroke(2f * s))
    drawPath(Path().apply { moveTo(x, y); lineTo(x, y - r) }, w.body, style = Stroke(1.3f * s))
    val rib = Path().apply {
        moveTo(x - r * 0.55f, y); cubicTo(x - r * 0.55f, y - r * 0.9f, x + r * 0.55f, y - r * 0.9f, x + r * 0.55f, y)
    }
    drawPath(rib, w.body, style = Stroke(1.3f * s))
    val gleam = Path().apply {
        moveTo(x - r * 0.72f, y - r * 0.4f)
        quadraticBezierTo(x - r * 0.6f, y - r * 0.95f, x - r * 0.2f, y - r * 0.86f)
    }
    drawPath(gleam, Color.White.copy(alpha = 0.45f), style = Stroke(2f * s, cap = StrokeCap.Round))
    drawRoundRectCompat(w.body, x - r * 1.12f, y - 2f * s, r * 2.24f, 5f * s, 2f * s)
    drawRoundRectCompat(w.bodyDark, x - r * 1.12f, y + 0.6f * s, r * 2.24f, 2.4f * s, 1.2f * s)
    blinker(x + r * 0.86f, y - 4.4f * s, 1.8f * s, w.light, time, 0f)
}

private fun DrawScope.array(w: World, x: Float, y: Float, s: Float, time: Float) {
    val h = 26f * s
    groundShadow(x, y, 13f * s)
    drawRoundRectCompat(w.bodyDark, x - 2f * s, y - h, 4f * s, h, 1.6f * s)
    // panels track the light slowly
    val tilt = sin(time * 0.25f) * 6f
    listOf(-1f, 1f).forEach { side ->
        rotate(tilt * side, Offset(x, y - h)) {
            val left = x + if (side < 0) -24f * s else 3f * s
            drawRoundRectCompat(w.trim, left, y - h - 6f * s, 21f * s, 13f * s, 1.6f * s)
            val grid = Path().apply {
                moveTo(left, y - h - 1.6f * s); lineTo(left + 21f * s, y - h - 1.6f * s)
                moveTo(left + 7f * s, y - h - 6f * s); lineTo(left + 7f * s, y - h + 7f * s)
                moveTo(left + 14f * s, y - h - 6f * s); lineTo(left + 14f * s, y - h + 7f * s)
            }
            drawPath(grid, w.bodyDark.copy(alpha = 0.6f), style = Stroke(0.9f * s))
        }
    }
    drawRoundRectCompat(w.body, x - 8f * s, y - 4f * s, 16f * s, 5f * s, 2f * s)
}

private fun DrawScope.mast(w: World, x: Float, y: Float, s: Float, time: Float) {
    val h = 34f * s
    groundShadow(x, y, 11f * s)
    val legs = Path().apply {
        moveTo(x - 7f * s, y); lineTo(x - 2.4f * s, y - h)
        lineTo(x + 2.4f * s, y - h); lineTo(x + 7f * s, y)
    }
    drawPath(legs, w.body, style = Stroke(2.2f * s))
    val braces = Path().apply {
        moveTo(x - 5.6f * s, y - 8f * s); lineTo(x + 5.6f * s, y - 8f * s)
        moveTo(x - 4.2f * s, y - 17f * s); lineTo(x + 4.2f * s, y - 17f * s)
        moveTo(x - 3f * s, y - 25f * s); lineTo(x + 3f * s, y - 25f * s)
        moveTo(x - 7f * s, y); lineTo(x + 5.6f * s, y - 8f * s)
        moveTo(x + 7f * s, y); lineTo(x - 5.6f * s, y - 8f * s)
    }
    drawPath(braces, w.bodyDark.copy(alpha = 0.8f), style = Stroke(1.2f * s))
    rotate(-24f + sin(time * 0.35f) * 10f, Offset(x, y - h)) {
        val dish = Path().apply {
            moveTo(x - 9f * s, y - h - 2f * s)
            cubicTo(x - 9f * s, y - h - 13f * s, x + 9f * s, y - h - 13f * s, x + 9f * s, y - h - 2f * s)
            close()
        }
        drawPath(dish, w.trim)
        drawCircle(w.bodyDark, 2.2f * s, Offset(x, y - h - 2f * s))
    }
    blinker(x, y - h - 6f * s, 1.8f * s, w.light, time, 1.4f)
}

private fun DrawScope.lab(w: World, x: Float, y: Float, s: Float, time: Float) {
    val width = 34f * s
    val height = 15f * s
    groundShadow(x, y, width * 0.6f)
    drawRoundRectCompat(w.body, x - width / 2f, y - height - 3f * s, width, height, height / 2f)
    drawRoundRectCompat(w.bodyDark, x - width / 2f, y - height - 3f * s, width * 0.22f, height, height / 2f)
    drawRoundRectCompat(w.bodyDark, x + width * 0.28f, y - height - 3f * s, width * 0.22f, height, height / 2f)
    for (k in 0 until 3) {
        val cx = x - 8f * s + k * 8f * s
        drawCircle(w.glass, 3f * s, Offset(cx, y - height * 0.5f - 3f * s))
        drawCircle(Color.White.copy(alpha = 0.6f), 1f * s, Offset(cx - 1f * s, y - height * 0.6f - 3f * s))
    }
    val legs = Path().apply {
        moveTo(x - width * 0.3f, y - 3f * s); lineTo(x - width * 0.3f, y)
        moveTo(x + width * 0.3f, y - 3f * s); lineTo(x + width * 0.3f, y)
    }
    drawPath(legs, w.bodyDark, style = Stroke(2.4f * s))
    blinker(x + width * 0.42f, y - height * 0.8f - 3f * s, 1.6f * s, w.light, time, 2.2f)
}

private fun DrawScope.pad(w: World, x: Float, y: Float, s: Float, time: Float) {
    drawOval(w.bodyDark, Offset(x - 20f * s, y - 7f * s), Size(40f * s, 14f * s))
    drawOval(w.body, Offset(x - 16f * s, y - 6.4f * s), Size(32f * s, 10.8f * s))
    val marks = Path().apply {
        moveTo(x - 8f * s, y - 1f * s); lineTo(x + 8f * s, y - 1f * s)
        moveTo(x, y - 4f * s); lineTo(x, y + 2f * s)
    }
    drawPath(marks, w.trim, style = Stroke(1.4f * s))
    val legs = Path().apply {
        moveTo(x - 8f * s, y - 4f * s); lineTo(x - 4f * s, y - 15f * s)
        moveTo(x + 8f * s, y - 4f * s); lineTo(x + 4f * s, y - 15f * s)
    }
    drawPath(legs, w.bodyDark, style = Stroke(2f * s))
    drawPath(Path().apply {
        moveTo(x - 7f * s, y - 15f * s); lineTo(x, y - 24f * s); lineTo(x + 7f * s, y - 15f * s); close()
    }, w.body)
    drawRoundRectCompat(w.bodyDark, x - 7f * s, y - 15f * s, 14f * s, 5f * s, 1.4f * s)
    drawCircle(w.glass, 2.2f * s, Offset(x, y - 17.6f * s))
    blinker(x - 14f * s, y - 2f * s, 1.6f * s, w.light, time, 3.1f)
}

private fun DrawScope.tank(w: World, x: Float, y: Float, s: Float, time: Float) {
    val h = 28f * s
    val wd = 15f * s
    groundShadow(x, y, wd * 0.8f)
    drawRoundRectCompat(w.body, x - wd / 2f, y - h, wd, h, wd / 2f)
    drawRoundRectCompat(w.bodyDark.copy(alpha = 0.7f), x - wd / 2f, y - h, wd * 0.3f, h, wd * 0.15f)
    drawRoundRectCompat(w.trim, x - wd / 2f, y - h * 0.66f, wd, 3f * s, 0.5f)
    drawRoundRectCompat(w.trim, x - wd / 2f, y - h * 0.34f, wd, 3f * s, 0.5f)
    drawRoundRectCompat(w.bodyDark, x - wd * 0.28f, y - h - 4f * s, wd * 0.56f, 4.4f * s, 1.6f * s)
    val pipe = Path().apply {
        moveTo(x + wd / 2f, y - h * 0.2f); lineTo(x + wd / 2f + 6f * s, y - h * 0.2f)
        lineTo(x + wd / 2f + 6f * s, y)
    }
    drawPath(pipe, w.bodyDark, style = Stroke(2f * s))
    blinker(x, y - h - 6f * s, 1.6f * s, w.light, time, 0.8f)
}

fun DrawScope.drawModule(kind: ModuleKind, w: World, x: Float, y: Float, s: Float, time: Float) {
    when (kind) {
        ModuleKind.DOME -> dome(w, x, y, s, time)
        ModuleKind.ARRAY -> array(w, x, y, s, time)
        ModuleKind.MAST -> mast(w, x, y, s, time)
        ModuleKind.LAB -> lab(w, x, y, s, time)
        ModuleKind.PAD -> pad(w, x, y, s, time)
        ModuleKind.TANK -> tank(w, x, y, s, time)
    }
}

fun DrawScope.drawSiteDecor(kind: SiteDecor, x: Float, y: Float, s: Float, time: Float) {
    when (kind) {
        SiteDecor.FLAG -> {
            drawRoundRectCompat(Color(0xFFB4BECC), x - 1.4f * s, y - 26f * s, 2.8f * s, 26f * s, 1f * s)
            drawPath(Path().apply {
                moveTo(x + 1.4f * s, y - 26f * s); lineTo(x + 17f * s, y - 22f * s); lineTo(x + 1.4f * s, y - 17f * s); close()
            }, Color(0xFFE2603F))
        }
        SiteDecor.FLOODLIGHT -> {
            drawRoundRectCompat(Color(0xFF8B94A4), x - 1.6f * s, y - 22f * s, 3.2f * s, 22f * s, 1f * s)
            drawPath(Path().apply {
                moveTo(x - 6f * s, y - 28f * s); lineTo(x + 6f * s, y - 28f * s)
                lineTo(x + 4f * s, y - 22f * s); lineTo(x - 4f * s, y - 22f * s); close()
            }, Color(0xFFC6CEDA))
            val glow = 0.16f + 0.14f * abs(sin(time * 1.3f))
            drawPath(Path().apply {
                moveTo(x - 5f * s, y - 22f * s); lineTo(x - 16f * s, y); lineTo(x + 16f * s, y); lineTo(x + 5f * s, y - 22f * s); close()
            }, Color(0xFFFFE9A8).copy(alpha = glow))
        }
        SiteDecor.CRATES -> {
            drawRoundRectCompat(Color(0xFFC7A25E), x - 13f * s, y - 10f * s, 12f * s, 10f * s, 1.4f * s)
            drawRoundRectCompat(Color(0xFFB08C4E), x, y - 9f * s, 11f * s, 9f * s, 1.4f * s)
            drawRoundRectCompat(Color(0xFFD8B270), x - 8f * s, y - 19f * s, 10f * s, 9f * s, 1.4f * s)
        }
        SiteDecor.BEACON -> {
            drawPath(Path().apply {
                moveTo(x - 7f * s, y); lineTo(x, y - 20f * s); lineTo(x + 7f * s, y); close()
            }, Color.Transparent)
            val frame = Path().apply {
                moveTo(x - 7f * s, y); lineTo(x, y - 20f * s); lineTo(x + 7f * s, y)
            }
            drawPath(frame, Color(0xFF9CA6B6), style = Stroke(2f * s))
            val a = 0.25f + 0.75f * abs(sin(time * 2.2f))
            drawCircle(Color(0xFFFF6B4A).copy(alpha = a * 0.35f), 7f * s, Offset(x, y - 23f * s))
            drawCircle(Color(0xFFFF6B4A).copy(alpha = a), 3.4f * s, Offset(x, y - 23f * s))
        }
    }
}

/** The heritage object: one hull section per world finished, and it flies with you. */
fun DrawScope.drawShip(x: Float, y: Float, sections: Int) {
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(x - 26f, y - 5f), Size(52f, 12f))
    val hull = Path().apply {
        moveTo(x - 26f, y - 14f)
        quadraticBezierTo(x - 16f, y - 27f, x, y - 27f)
        quadraticBezierTo(x + 16f, y - 27f, x + 26f, y - 14f)
        quadraticBezierTo(x + 16f, y - 6f, x, y - 6f)
        quadraticBezierTo(x - 16f, y - 6f, x - 26f, y - 14f)
        close()
    }
    drawPath(hull, Color(0xFFDCE3ED))
    val shade = Path().apply {
        moveTo(x - 26f, y - 14f)
        quadraticBezierTo(x - 16f, y - 27f, x, y - 27f)
        quadraticBezierTo(x - 4f, y - 17f, x, y - 6f)
        quadraticBezierTo(x - 16f, y - 6f, x - 26f, y - 14f)
        close()
    }
    drawPath(shade, Color(0xFFB4BECC))
    drawCircle(Color(0xFF8FDCF2), 4.4f, Offset(x + 7f, y - 18f))
    drawCircle(Color.White.copy(alpha = 0.7f), 1.4f, Offset(x + 5.6f, y - 19.4f))
    val struts = Path().apply {
        moveTo(x - 9f, y - 6f); lineTo(x - 9f, y)
        moveTo(x + 9f, y - 6f); lineTo(x + 9f, y)
    }
    drawPath(struts, Color(0xFF8B94A4), style = Stroke(2.6f))
    for (k in 0 until sections) {
        val row = k % 4
        val up = if (k < 4) 0f else 9f
        drawRoundRectCompat(
            if (k % 2 == 0) Color(0xFFC6CEDA) else Color(0xFF5FC7E8),
            x - 22f + row * 11f, y - 30f - up, 9f, 7f, 2f
        )
    }
}

/** Pip. Reacts to the place, never to the student's absence. */
fun DrawScope.drawPip(x: Float, y: Float, time: Float) {
    val roll = sin(time * 0.55f) * 26f
    translate(roll, 0f) {
        drawOval(Color.Black.copy(alpha = 0.3f), Offset(x - 15f, y - 4.4f), Size(30f, 8.8f))
        drawRoundRectCompat(Color(0xFFDCE3ED), x - 13f, y - 16f, 26f, 12f, 4f)
        drawRoundRectCompat(Color(0xFFB4BECC), x - 13f, y - 16f, 26f, 4.4f, 2.2f)
        listOf(-8f, 1f, 10f).forEach { drawCircle(Color(0xFF5A6270), 4.4f, Offset(x + it, y - 3f)) }
        drawRoundRectCompat(Color(0xFFB4BECC), x - 1.6f, y - 28f, 3.2f, 12f, 1f)
        drawRoundRectCompat(Color(0xFFEAF0F8), x - 9f, y - 36f, 18f, 10f, 4f)
        drawCircle(Color(0xFF2C3B4E), 2.8f, Offset(x - 3.4f, y - 31f))
        drawCircle(Color(0xFF2C3B4E), 2.8f, Offset(x + 3.4f, y - 31f))
        drawCircle(Color(0xFF8FDCF2), 1f, Offset(x - 2.6f, y - 32f))
        drawCircle(Color(0xFF8FDCF2), 1f, Offset(x + 4.2f, y - 32f))
        drawPath(
            Path().apply { moveTo(x + 9f, y - 34f); lineTo(x + 16f, y - 41f) },
            Color(0xFFB4BECC), style = Stroke(1.8f)
        )
        val a = 0.2f + 0.8f * abs(sin(time * 3f))
        drawCircle(Color(0xFFFF6B4A).copy(alpha = a), 2.2f, Offset(x + 16f, y - 41f))
    }
}

/** Two crew working, one walking the perimeter — "is someone living here". */
fun DrawScope.drawCrew(time: Float) {
    crewFigure(146f, 300f, 0.95f)
    crewFigure(156f, 303f, 0.85f)
    val walk = sin(time * 0.4f) * 40f
    translate(walk, 0f) { crewFigure(196f, 332f, 1f) }
}

private fun DrawScope.crewFigure(x: Float, y: Float, s: Float) {
    drawCircle(Color(0xFFEAF0F8), 2.8f * s, Offset(x, y - 9f * s))
    drawRoundRectCompat(Color(0xFFDCE3ED), x - 2f * s, y - 6.4f * s, 4f * s, 5f * s, 1.4f * s)
    val legs = Path().apply {
        moveTo(x - 1.4f * s, y - 1.4f * s); lineTo(x - 1.4f * s, y)
        moveTo(x + 1.4f * s, y - 1.4f * s); lineTo(x + 1.4f * s, y)
    }
    drawPath(legs, Color(0xFFB4BECC), style = Stroke(1.2f * s))
}
