package com.ncert7.aitutorandlab.domain.reels.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelWatchTrackerTest {

    @Test
    fun `single play-pause accumulates the span`() {
        val t = ReelWatchTracker()
        t.onPlay(1_000)
        t.onPause(4_000)
        assertEquals(3_000, t.watchedMs(10_000))
        assertFalse(t.isPlaying)
    }

    @Test
    fun `multiple play-pause cycles sum`() {
        val t = ReelWatchTracker()
        t.onPlay(0); t.onPause(2_000)   // 2s
        t.onPlay(5_000); t.onPause(8_500) // 3.5s
        assertEquals(5_500, t.watchedMs(9_000))
    }

    @Test
    fun `watchedMs includes the live span while still playing`() {
        val t = ReelWatchTracker()
        t.onPlay(1_000)
        assertEquals(2_000, t.watchedMs(3_000))
        assertTrue(t.isPlaying)
    }

    @Test
    fun `second play without pause is ignored (idempotent)`() {
        val t = ReelWatchTracker()
        t.onPlay(1_000)
        t.onPlay(9_000) // must not reset the start
        assertEquals(2_000, t.watchedMs(3_000))
    }

    @Test
    fun `pause without an active play is a no-op`() {
        val t = ReelWatchTracker()
        t.onPause(5_000)
        assertEquals(0, t.watchedMs(5_000))
    }

    @Test
    fun `backward time within a span is clamped to zero`() {
        val t = ReelWatchTracker()
        t.onPlay(10_000)
        t.onPause(4_000) // end before start → clamp to 0, not negative
        assertEquals(0, t.watchedMs(10_000))
    }

    @Test
    fun `an absurd span is capped at maxSpanMs`() {
        val t = ReelWatchTracker(maxSpanMs = 1_000)
        t.onPlay(0)
        t.onPause(60_000)
        assertEquals(1_000, t.watchedMs(60_000))
    }

    @Test
    fun `live span is also capped while playing`() {
        val t = ReelWatchTracker(maxSpanMs = 1_000)
        t.onPlay(0)
        assertEquals(1_000, t.watchedMs(60_000))
    }

    @Test
    fun `reset clears accumulated and live time`() {
        val t = ReelWatchTracker()
        t.onPlay(0); t.onPause(3_000)
        t.reset()
        assertEquals(0, t.watchedMs(9_000))
        assertFalse(t.isPlaying)
    }

    @Test
    fun `completion is null when duration is unknown`() {
        assertNull(ReelWatchTracker.completionFraction(5_000, null))
        assertNull(ReelWatchTracker.completionFraction(5_000, 0))
    }

    @Test
    fun `completion is the watched over duration ratio`() {
        assertEquals(0.5f, ReelWatchTracker.completionFraction(5_000, 10_000)!!, 0.0001f)
        assertEquals(0f, ReelWatchTracker.completionFraction(0, 10_000)!!, 0.0001f)
    }

    @Test
    fun `completion clamps replays to 100 percent`() {
        assertEquals(1f, ReelWatchTracker.completionFraction(25_000, 10_000)!!, 0.0001f)
    }
}
