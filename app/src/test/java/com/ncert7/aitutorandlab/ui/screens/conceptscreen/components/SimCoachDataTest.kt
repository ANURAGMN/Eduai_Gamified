package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-Kotlin checks for the adaptive coach data model. No org.json here so these run under a
 * plain JVM unit test — the JSON [SimGuideBuilder.parseDoc] path is exercised on-device instead.
 */
class SimCoachDataTest {

    private fun coach(
        stuck: List<String> = listOf("s0", "s1"),
        wrong: List<String> = listOf("w0", "w1", "w2"),
        correct: List<String> = listOf("c0"),
    ) = SimCoachData(
        mission = "m",
        whenStuck = stuck,
        whenWrong = wrong,
        whenCorrect = correct,
        doneMessage = "done",
    )

    @Test
    fun `banks cycle in order and wrap around`() {
        val c = coach()
        assertEquals("w0", c.wrongLine(0))
        assertEquals("w1", c.wrongLine(1))
        assertEquals("w2", c.wrongLine(2))
        assertEquals("w0", c.wrongLine(3)) // wraps
        assertEquals("s0", c.stuckLine(0))
        assertEquals("s1", c.stuckLine(1))
        assertEquals("s0", c.stuckLine(2)) // wraps
    }

    @Test
    fun `single-line bank always returns that line`() {
        val c = coach()
        assertEquals("c0", c.correctLine(0))
        assertEquals("c0", c.correctLine(5))
    }

    @Test
    fun `negative cursor is handled safely`() {
        val c = coach()
        assertEquals("w2", c.wrongLine(-1))
    }

    @Test
    fun `empty bank returns null instead of crashing`() {
        val c = coach(stuck = emptyList())
        assertNull(c.stuckLine(0))
    }

    @Test
    fun `default coach fills every bank so any sim is usable`() {
        val d = SimCoachData.default("My mission")
        assertEquals("My mission", d.mission)
        assertNotNull(d.wrongLine(0))
        assertNotNull(d.stuckLine(0))
        assertNotNull(d.correctLine(0))
        assertNotNull(d.deviateLine(0)) // v3 redirect bank
        // Sensible, non-zero thresholds.
        assert(d.easeOffAfterInteractions > 0)
        assert(d.easeOffAfterSeconds >= 30)
        assert(d.stuckAfterSeconds >= 6)
        assert(d.roundsToComplete >= 1)
    }

    @Test
    fun `deviate bank is empty by default on the raw constructor`() {
        assertNull(coach().deviateLine(0))
    }

    @Test
    fun `inferenceFor matches by substring and prefers the longest key`() {
        val c = coach().copy(
            elements = mapOf(
                "lemon" to "Lemon is an acid.",
                "soda" to "Soda is a base.",
                "baking soda" to "Baking soda is a base.",
            ),
        )
        // Exact + substring (label longer than key).
        assertEquals("Lemon is an acid.", c.inferenceFor("Lemon"))
        assertEquals("Lemon is an acid.", c.inferenceFor("Lemon Juice"))
        // Longest-key wins: "baking soda" over "soda".
        assertEquals("Baking soda is a base.", c.inferenceFor("Baking Soda"))
        // No match → null (caller falls back to a generic line).
        assertNull(c.inferenceFor("Mercury"))
        // Empty map → null.
        assertNull(coach().inferenceFor("lemon"))
    }

    @Test
    fun `coach mode fromKey round-trips names and falls back to v4`() {
        assertEquals(SimCoachMode.SCRIPTED, SimCoachMode.fromKey("SCRIPTED"))
        assertEquals(SimCoachMode.ADAPTIVE, SimCoachMode.fromKey("ADAPTIVE"))
        assertEquals(SimCoachMode.GUIDED, SimCoachMode.fromKey("GUIDED"))
        assertEquals(SimCoachMode.ONE_CLOCK, SimCoachMode.fromKey("ONE_CLOCK"))
        assertEquals(SimCoachMode.DEFAULT, SimCoachMode.fromKey(null))
        assertEquals(SimCoachMode.DEFAULT, SimCoachMode.fromKey("nonsense"))
        assertEquals(SimCoachMode.ONE_CLOCK, SimCoachMode.DEFAULT)
    }

    @Test
    fun `every mode has a distinct short label for the selector`() {
        val shorts = SimCoachMode.values().map { it.short }
        assertEquals(listOf("V1", "V2", "V3", "V4"), shorts)
        assertEquals(shorts.size, shorts.toSet().size)
    }
}
