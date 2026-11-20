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

        // The strict whitelist: Only these specific IDs are allowed to be visible in this turn.
        val whitelistIds = parts.map { it.partId!! }.toSet()

        // Regex to find EVERY tag in the SVG that has an id="..."
        val tagPattern = Regex("""(?is)<([a-zA-Z_][\w:-]*)\b([^>]*?\bid\s*=\s*["']([^"']+)["'][^>]*?)(/?)>""")

        // We must never hide core SVG structural definitions, otherwise the image breaks.
        val structuralTags = setOf("clippath", "mask", "lineargradient", "radialgradient", "pattern", "filter", "marker", "defs", "symbol", "style", "svg")

        var matchedWhitelist = false

        // Do a single global sweep of the entire SVG file
        val out = tagPattern.replace(svgString) { m ->
            val tagName = m.groupValues[1]
            var attrs = m.groupValues[2]
            val id = m.groupValues[3]
            val selfClose = m.groupValues[4]

            // Leave technical SVG drawing definitions alone
            if (structuralTags.contains(tagName.lowercase())) {
                return@replace m.value
            }

            // Strip any existing visibility attributes so we can cleanly inject our own
            attrs = attrs.replace(Regex("""(?i)\b(opacity|display|visibility|font-size|fill-opacity|stroke-opacity)\s*=\s*["'][^"']*["']"""), "")
            attrs = attrs.replace(Regex("""(?i)(opacity|display|visibility|font-size|fill-opacity|stroke-opacity)\s*:\s*[^;"]*;?"""), "")

            if (whitelistIds.contains(id)) {
                // This ID is on the timeline list for this turn.
                matchedWhitelist = true
                val unit = parts.find { it.partId == id }
                if (unit != null && charIndex >= unit.triggerCharIndex) {
                    // Trigger reached: SHOW IT
                    attrs = attrs.trimEnd() + """ opacity="1" display="inline" visibility="visible""""
                } else {
                    // Trigger not reached yet: HIDE IT (waiting for AI to speak)
                    attrs = attrs.trimEnd() + """ opacity="0" display="none" visibility="hidden" font-size="0" fill-opacity="0" stroke-opacity="0""""
                }
            } else {
                // This ID is NOT on the timeline list. It belongs to a future turn.
                // Aggressively HIDE IT so it doesn't bleed onto the screen.
                attrs = attrs.trimEnd() + """ opacity="0" display="none" visibility="hidden" font-size="0" fill-opacity="0" stroke-opacity="0""""
            }

            "<$tagName$attrs$selfClose>"
        }

        // If the timeline had parts, but we found absolutely none of them in the SVG, fallback to visible
        return if (matchedWhitelist) out else forceFullyVisible(svgString)
    }

    fun forceFullyVisible(svgString: String): String =
        svgString
            .replace(Regex("""(?i)\bopacity\s*=\s*["']0(?:\.0+)?["']"""), """opacity="1"""")
            .replace(Regex("""(?i)opacity\s*:\s*0(?:\.0+)?"""), "opacity:1")
            .replace(Regex("""(?i)\bvisibility\s*=\s*["']hidden["']"""), """visibility="visible"""")
            .replace(Regex("""(?i)visibility\s*:\s*hidden;?"""), "visibility:visible;")
            .replace(Regex("""(?i)\bdisplay\s*=\s*["']none["']"""), """display="inline"""")
            .replace(Regex("""(?i)display\s*:\s*none;?"""), "display:inline;")
            .replace(Regex("""(?i)\bfont-size\s*=\s*["']0(?:\.0+)?["']"""), "")
            .replace(Regex("""(?i)\bfill-opacity\s*=\s*["']0(?:\.0+)?["']"""), "")
            .replace(Regex("""(?i)\bstroke-opacity\s*=\s*["']0(?:\.0+)?["']"""), "")

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