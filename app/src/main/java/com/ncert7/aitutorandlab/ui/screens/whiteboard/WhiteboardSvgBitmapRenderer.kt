package com.ncert7.aitutorandlab.ui.screens.whiteboard

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import com.ncert7.aitutorandlab.data.remote.WbRevealTimelineUnit

/**
 * Host-side SVG → Bitmap for the whiteboard Visual Board.
 *
 * Geometry scenes from the tutor API often ship `viewBox` only (no width/height).
 * [SVG.renderToPicture] with no size then yields a 0×0 picture and a blank board.
 */
object WhiteboardSvgBitmapRenderer {

    data class RenderSize(val width: Int, val height: Int)

    fun resolveOutputSize(svg: SVG, fallbackW: Int = 900, fallbackH: Int = 660): RenderSize {
        val vb = svg.documentViewBox
        val vbW = vb?.width() ?: 0f
        val vbH = vb?.height() ?: 0f
        val outW = when {
            vbW > 0f -> vbW.toInt().coerceAtLeast(1)
            svg.documentWidth > 0f -> svg.documentWidth.toInt().coerceAtLeast(1)
            else -> fallbackW
        }
        val outH = when {
            vbH > 0f -> vbH.toInt().coerceAtLeast(1)
            svg.documentHeight > 0f -> svg.documentHeight.toInt().coerceAtLeast(1)
            else -> fallbackH
        }
        return RenderSize(outW, outH)
    }

    fun applyReveal(
        svgString: String,
        timeline: List<WbRevealTimelineUnit>,
        charIndex: Int,
    ): String {
        val parts = timeline.filter { it.unitType == "scene_part" && !it.partId.isNullOrBlank() }
        if (parts.isEmpty()) {
            return forceFullyVisible(svgString)
        }
        var out = svgString
        var matched = 0
        for (unit in parts) {
            val op = if (charIndex >= unit.triggerCharIndex) "1" else "0"
            val (next, ok) = setElementOpacityById(out, unit.partId!!, op)
            if (ok) {
                matched++
                out = next
            }
        }
        // Timeline part ids didn't hit any element — don't leave authored opacity-0 scenes blank.
        return if (matched == 0) forceFullyVisible(svgString) else out
    }

    /**
     * Set or inject `opacity` on the opening tag that owns [id].
     * Tutor scene SVGs often ship fully visible groups (no opacity attr); timed reveal
     * must inject opacity="0" until [charIndex] reaches the trigger.
     */
    fun setElementOpacityById(svgString: String, id: String, opacity: String): Pair<String, Boolean> {
        val tagPattern = Regex(
            """(?is)<([a-zA-Z_][\w:-]*)\b([^>]*?\bid\s*=\s*["']${Regex.escape(id)}["'][^>]*?)(/?)>""",
        )
        val m = tagPattern.find(svgString) ?: return svgString to false
        val tagName = m.groupValues[1]
        var attrs = m.groupValues[2]
        val selfClose = m.groupValues[3]
        val opacityAttr = Regex("""(?i)\bopacity\s*=\s*["'][^"']*["']""")
        attrs = if (opacityAttr.containsMatchIn(attrs)) {
            opacityAttr.replace(attrs, """opacity="$opacity"""")
        } else {
            attrs.trimEnd() + """ opacity="$opacity""""
        }
        val replacement = "<$tagName$attrs$selfClose>"
        return svgString.replaceRange(m.range, replacement) to true
    }

    fun forceFullyVisible(svgString: String): String =
        svgString
            .replace(Regex("""(?i)opacity\s*=\s*["']0(?:\.0+)?["']"""), """opacity="1"""")
            .replace(Regex("""(?i)opacity\s*:\s*0(?:\.0+)?"""), "opacity:1")
            .replace(Regex("""(?i)visibility\s*=\s*["']hidden["']"""), """visibility="visible"""")

    /**
     * AndroidSVG 1.4 rejects some SVG2 marker values used by tutor geometry scenes
     * (notably `orient="auto-start-reverse"`). Soften those so parse succeeds.
     */
    fun sanitizeForAndroidSvg(svgString: String): String =
        svgString
            .replace(
                Regex("""orient\s*=\s*["']auto-start-reverse["']""", RegexOption.IGNORE_CASE),
                """orient="auto"""",
            )
            .replace(
                Regex("""orient\s*=\s*["']auto-start["']""", RegexOption.IGNORE_CASE),
                """orient="auto"""",
            )

    /**
     * @param sized when false, calls [SVG.renderToPicture] with no args (the broken path).
     */
    fun renderToBitmap(svgString: String, sized: Boolean = true): Bitmap {
        val svg = SVG.getFromString(sanitizeForAndroidSvg(svgString))
        if (!sized) {
            val picture = svg.renderToPicture()
            val w = picture.width.coerceAtLeast(0)
            val h = picture.height.coerceAtLeast(0)
            if (w == 0 || h == 0) {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawPicture(picture)
            return bmp
        }
        val size = resolveOutputSize(svg)
        val picture = svg.renderToPicture(size.width, size.height)
        val bmp = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawPicture(picture)
        return bmp
    }

    /** Count pixels that are not near-white (board content signal). */
    fun countNonNearWhite(bitmap: Bitmap, threshold: Int = 240): Int {
        var count = 0
        val w = bitmap.width
        val h = bitmap.height
        val row = IntArray(w)
        for (y in 0 until h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            for (c in row) {
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                if (r < threshold || g < threshold || b < threshold) count++
            }
        }
        return count
    }
}
