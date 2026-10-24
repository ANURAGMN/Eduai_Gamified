package com.ncert7.aitutorandlab.ui.screens.whiteboard

import android.app.Application
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.ncert7.aitutorandlab.data.remote.WbRevealTimelineUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Device-free coverage for the Angle Validity blank-board root causes:
 * 1) AndroidSVG 1.4 rejects `orient="auto-start-reverse"` → parse failure → empty board
 * 2) viewBox-only SVGs need explicit render size (900×660)
 * 3) opacity-0 / mismatched scene_part ids must force-show
 *
 * Note: Robolectric's Canvas does not replay AndroidSVG Picture draws (black bitmaps),
 * so pixel painting is validated via host rasterization of the same fixture, not here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class WhiteboardSvgBitmapRendererTest {

    private fun fixtureSvg(): String =
        checkNotNull(javaClass.classLoader!!.getResourceAsStream("whiteboard/angle_validity.svg"))
            .bufferedReader()
            .use { it.readText() }

    @Test
    fun angleValidity_rawSvg_failsAndroidSvgParse() {
        assertThrows(SVGParseException::class.java) {
            SVG.getFromString(fixtureSvg())
        }
    }

    @Test
    fun angleValidity_sanitize_rewritesAutoStartReverse() {
        val raw = fixtureSvg()
        assertTrue(raw.contains("auto-start-reverse"))
        val cleaned = WhiteboardSvgBitmapRenderer.sanitizeForAndroidSvg(raw)
        assertFalse(cleaned.contains("auto-start-reverse"))
        assertTrue(cleaned.contains("""orient="auto""""))
        // Must parse after sanitize — this is what the phone needs.
        val svg = SVG.getFromString(cleaned)
        assertTrue(svg.documentViewBox != null)
    }

    @Test
    fun angleValidity_viewBoxOnly_resolvesTo900x660() {
        val svg = SVG.getFromString(WhiteboardSvgBitmapRenderer.sanitizeForAndroidSvg(fixtureSvg()))
        val size = WhiteboardSvgBitmapRenderer.resolveOutputSize(svg)
        assertEquals(900, size.width)
        assertEquals(660, size.height)
    }

    @Test
    fun forceFullyVisible_unhidesOpacityZero() {
        val hidden =
            """<svg viewBox="0 0 10 10"><g id="part" opacity="0"><rect width="10" height="10"/></g></svg>"""
        val shown = WhiteboardSvgBitmapRenderer.forceFullyVisible(hidden)
        assertTrue(shown.contains("""opacity="1""""))
        assertFalse(shown.contains("""opacity="0""""))
    }

    @Test
    fun applyReveal_mismatchedPartIds_forceShows() {
        val hidden =
            """<svg viewBox="0 0 10 10"><g id="real" opacity="0"><rect width="10" height="10"/></g></svg>"""
        val timeline = listOf(
            WbRevealTimelineUnit(
                unitId = "u1",
                objectId = "o1",
                unitType = "scene_part",
                unitIndex = 0,
                partId = "missing_id",
                cue = "",
                triggerCharIndex = 0,
            ),
        )
        val out = WhiteboardSvgBitmapRenderer.applyReveal(hidden, timeline, charIndex = 0)
        assertTrue(out.contains("""opacity="1""""))
    }

    @Test
    fun applyReveal_injectsOpacityOnGroupsWithoutAttr() {
        // Photosynthesis-style: fully visible groups, timed by scene_part ids.
        val svg =
            """<svg viewBox='0 0 10 10'><g id='plant_base'><rect/></g><g id='sunlight_input'><rect/></g></svg>"""
        val timeline = listOf(
            part("plant_base", trigger = 0),
            part("sunlight_input", trigger = 40),
        )
        val atStart = WhiteboardSvgBitmapRenderer.applyReveal(svg, timeline, charIndex = 0)
        assertTrue(atStart.contains("""id='plant_base' opacity="1""""))
        assertTrue(atStart.contains("""id='sunlight_input' opacity="0""""))

        val later = WhiteboardSvgBitmapRenderer.applyReveal(svg, timeline, charIndex = 40)
        assertTrue(later.contains("""id='sunlight_input' opacity="1""""))
    }

    @Test
    fun applyReveal_updatesExistingOpacityAttr() {
        val svg =
            """<svg viewBox="0 0 10 10"><g id="pair_one" opacity="0"><rect/></g></svg>"""
        val timeline = listOf(part("pair_one", trigger = 10))
        val hidden = WhiteboardSvgBitmapRenderer.applyReveal(svg, timeline, charIndex = 0)
        assertTrue(hidden.contains("""opacity="0""""))
        val shown = WhiteboardSvgBitmapRenderer.applyReveal(svg, timeline, charIndex = 10)
        assertTrue(shown.contains("""opacity="1""""))
    }

    private fun part(partId: String, trigger: Int) = WbRevealTimelineUnit(
        unitId = "u_$partId",
        objectId = "o1",
        unitType = "scene_part",
        unitIndex = 0,
        partId = partId,
        cue = "",
        triggerCharIndex = trigger,
    )
}
