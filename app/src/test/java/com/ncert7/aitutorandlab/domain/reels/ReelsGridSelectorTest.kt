package com.ncert7.aitutorandlab.domain.reels

import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelsGridSelectorTest {

    private fun v(
        id: String,
        published: Long,
        views: Long,
        kids: Boolean = true,
    ) = YoutubeVideo(
        videoId = id,
        title = id,
        publishedAtMillis = published,
        viewCount = views,
        madeForKids = kids,
    )

    @Test
    fun `twelve videos split into six newest and six most watched, no overlap`() {
        // ids n1..n12; publishedAt increasing so n12 is newest; views set so the OLDEST have most views.
        val videos = (1..12).map { i -> v("n$i", published = i.toLong(), views = (13 - i).toLong()) }
        val grid = ReelsGridSelector.select(videos)

        assertEquals(6, grid.newest.size)
        assertEquals(6, grid.mostWatched.size)
        assertEquals(listOf("n12", "n11", "n10", "n9", "n8", "n7"), grid.newest.map { it.videoId })
        // remaining n1..n6 by views desc: n1 has the most views (12)
        assertEquals(listOf("n1", "n2", "n3", "n4", "n5", "n6"), grid.mostWatched.map { it.videoId })
        assertTrue((grid.newest.map { it.videoId } intersect grid.mostWatched.map { it.videoId }.toSet()).isEmpty())
    }

    @Test
    fun `a newest video that is also highest viewed is not repeated in most watched`() {
        // n12 is the newest (published 12) AND has the most views — it must land in newest only.
        val videos = (1..12).map { i -> v("n$i", published = i.toLong(), views = if (i == 12) 9999 else i.toLong()) }
        val grid = ReelsGridSelector.select(videos)
        assertTrue(grid.newest.any { it.videoId == "n12" })
        assertFalse(grid.mostWatched.any { it.videoId == "n12" })
    }

    @Test
    fun `fewer than twelve videos yields fewer tiles with no duplicates`() {
        val videos = (1..8).map { i -> v("n$i", published = i.toLong(), views = i.toLong()) }
        val grid = ReelsGridSelector.select(videos)
        assertEquals(6, grid.newest.size)
        assertEquals(2, grid.mostWatched.size)
        assertEquals(8, grid.all.size)
        assertEquals(8, grid.all.map { it.videoId }.toSet().size)
    }

    @Test
    fun `non kids and blank id videos are filtered out when requireMadeForKids`() {
        val videos = listOf(
            v("ok1", 5, 5),
            v("notkids", 6, 6, kids = false),
            v("", 7, 7),
        )
        val grid = ReelsGridSelector.select(videos, requireMadeForKids = true)
        assertEquals(listOf("ok1"), grid.all.map { it.videoId })
    }

    @Test
    fun `non kids videos are kept when requireMadeForKids is false`() {
        val videos = listOf(
            v("ok1", 5, 5),
            v("notkids", 6, 6, kids = false),
            v("", 7, 7),
        )
        val grid = ReelsGridSelector.select(videos, requireMadeForKids = false)
        assertEquals(listOf("notkids", "ok1"), grid.all.map { it.videoId })
    }

    @Test
    fun `duplicate ids are collapsed`() {
        val videos = listOf(v("dup", 1, 1), v("dup", 2, 2), v("other", 3, 3))
        val grid = ReelsGridSelector.select(videos)
        assertEquals(2, grid.all.size)
        assertEquals(setOf("dup", "other"), grid.all.map { it.videoId }.toSet())
    }

    @Test
    fun `empty input gives empty grid`() {
        val grid = ReelsGridSelector.select(emptyList())
        assertTrue(grid.isEmpty)
    }
}
