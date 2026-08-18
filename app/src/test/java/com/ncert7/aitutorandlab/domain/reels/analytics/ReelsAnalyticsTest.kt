package com.ncert7.aitutorandlab.domain.reels.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReelsAnalyticsTest {

    // ---- openInteraction ----

    @Test
    fun `open interaction without query`() {
        assertEquals("open|pos=2", ReelsAnalytics.openInteraction(2, null))
        assertEquals("open|pos=0", ReelsAnalytics.openInteraction(0, "   "))
    }

    @Test
    fun `open interaction with query is trimmed and appended`() {
        assertEquals("open|pos=1|q=fractions", ReelsAnalytics.openInteraction(1, "  fractions  "))
    }

    @Test
    fun `unknown position floors at -1`() {
        assertEquals("open|pos=-1", ReelsAnalytics.openInteraction(-9, null))
    }

    // ---- watchInteraction ----

    @Test
    fun `watch interaction without completion`() {
        assertEquals("watch|ms=4200", ReelsAnalytics.watchInteraction(4_200, null))
    }

    @Test
    fun `watch interaction with completion percent`() {
        assertEquals("watch|ms=5000|pct=50", ReelsAnalytics.watchInteraction(5_000, 0.5f))
        assertEquals("watch|ms=5000|pct=100", ReelsAnalytics.watchInteraction(5_000, 1.4f))
    }

    @Test
    fun `negative watched ms floors at zero`() {
        assertEquals("watch|ms=0", ReelsAnalytics.watchInteraction(-10, null))
    }

    // ---- searchInteraction ----

    @Test
    fun `search interaction encodes result count`() {
        assertEquals("search|results=7", ReelsAnalytics.searchInteraction(7))
        assertEquals("search|results=0", ReelsAnalytics.searchInteraction(-3))
    }

    // ---- helpers ----

    @Test
    fun `sanitizedQuery trims, nulls blanks, and bounds length`() {
        assertNull(ReelsAnalytics.sanitizedQuery(null))
        assertNull(ReelsAnalytics.sanitizedQuery("   "))
        assertEquals("photosynthesis", ReelsAnalytics.sanitizedQuery("  photosynthesis "))
        val long = "x".repeat(200)
        assertEquals(ReelsAnalytics.MAX_QUERY_LEN, ReelsAnalytics.sanitizedQuery(long)!!.length)
    }

    @Test
    fun `percent rounds and clamps`() {
        assertEquals(0, ReelsAnalytics.percent(-0.2f))
        assertEquals(33, ReelsAnalytics.percent(0.333f))
        assertEquals(100, ReelsAnalytics.percent(1.9f))
    }

    // ---- GA4 param maps ----

    @Test
    fun `open params carry section, position and null query`() {
        val p = ReelsAnalytics.openParams("vid1", ReelSection.NEWEST, 3, null)
        assertEquals("vid1", p["video_id"])
        assertEquals("newest", p["section"])
        assertEquals(3, p["position"])
        assertNull(p["query"])
    }

    @Test
    fun `watch params expose ms and completion percent`() {
        val p = ReelsAnalytics.watchParams("vid1", 8_000, 0.25f)
        assertEquals(8_000L, p["watched_ms"])
        assertEquals(25, p["completion_pct"])
        assertNull(ReelsAnalytics.watchParams("vid1", 8_000, null)["completion_pct"])
    }

    @Test
    fun `search params carry query and result count`() {
        val p = ReelsAnalytics.searchParams(" sky ", 4)
        assertEquals("sky", p["query"])
        assertEquals(4, p["result_count"])
    }

    @Test
    fun `section values are stable analytics keys`() {
        assertEquals("newest", ReelSection.NEWEST.value)
        assertEquals("most_watched", ReelSection.MOST_WATCHED.value)
        assertEquals("search", ReelSection.SEARCH.value)
    }
}
