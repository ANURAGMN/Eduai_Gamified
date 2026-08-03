package com.ncert7.aitutorandlab.service.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsDurationUtilsTest {

    @Test
    fun capDuration_returnsRawWhenUnderMax() {
        val start = 1_000L
        val end = 61_000L
        val capped = AnalyticsDurationUtils.capDuration(start, end)

        assertEquals(60_000L, capped.durationMillis)
        assertEquals(end, capped.endTime)
        assertFalse(capped.wasCapped)
    }

    @Test
    fun capDuration_capsOrphanSessionsAtTwoHours() {
        val start = 0L
        val end = AnalyticsDurationUtils.MAX_ORPHAN_MS + 60_000L
        val capped = AnalyticsDurationUtils.capDuration(start, end)

        assertEquals(AnalyticsDurationUtils.MAX_ORPHAN_MS, capped.durationMillis)
        assertEquals(AnalyticsDurationUtils.MAX_ORPHAN_MS, capped.endTime)
        assertTrue(capped.wasCapped)
    }

    @Test
    fun capDuration_neverReturnsNegativeDuration() {
        val capped = AnalyticsDurationUtils.capDuration(endTime = 500L, startTime = 1_000L)

        assertEquals(0L, capped.durationMillis)
        assertEquals(1_000L, capped.endTime)
        assertFalse(capped.wasCapped)
    }
}
