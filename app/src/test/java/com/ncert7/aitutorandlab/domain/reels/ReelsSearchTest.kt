package com.ncert7.aitutorandlab.domain.reels

import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReelsSearchTest {

    private fun v(id: String, title: String, caption: String = "", kn: String = "", published: Long = 0) =
        YoutubeVideo(
            videoId = id,
            title = title,
            titleKannada = kn,
            caption = caption,
            publishedAtMillis = published,
            madeForKids = true,
        )

    private val photo = v("v1", "Photosynthesis in 60 seconds", published = 4)
    private val sky = v("v2", "Why the sky is blue", published = 3)
    private val fractions = v("v3", "Fractions with a pizza", caption = "learn fractions", published = 2)
    private val mult = v("v4", "Multiplication trick", published = 1)
    private val all = listOf(photo, sky, fractions, mult)

    @Test
    fun `blank query returns the list unchanged`() {
        assertEquals(all, ReelsSearch.filter(all, "   "))
    }

    @Test
    fun `substring match is case insensitive`() {
        assertEquals(listOf("v1"), ReelsSearch.filter(all, "PHOTO").map { it.videoId })
    }

    @Test
    fun `typo is tolerated`() {
        assertEquals(listOf("v1"), ReelsSearch.filter(all, "fotosynthesis").map { it.videoId })
        assertTrue(ReelsSearch.filter(all, "fracton").any { it.videoId == "v3" })
    }

    @Test
    fun `multi word query ands the terms`() {
        assertEquals(listOf("v2"), ReelsSearch.filter(all, "sky blue").map { it.videoId })
        // "sky" matches v2 but "pizza" does not → no result
        assertTrue(ReelsSearch.filter(all, "sky pizza").isEmpty())
    }

    @Test
    fun `caption is searched`() {
        assertTrue(ReelsSearch.filter(all, "learn").any { it.videoId == "v3" })
    }

    @Test
    fun `no match returns empty`() {
        assertTrue(ReelsSearch.filter(all, "zzzqqq").isEmpty())
    }

    @Test
    fun `exact substring ranks above a fuzzy match`() {
        val a = v("a", "cat facts", published = 1)
        val b = v("b", "kat clips", published = 2)
        val result = ReelsSearch.filter(listOf(b, a), "cat").map { it.videoId }
        assertEquals(listOf("a", "b"), result) // a (exact) before b (typo)
    }

    @Test
    fun `levenshtein basics`() {
        assertEquals(0, ReelsSearch.levenshtein("abc", "abc"))
        assertEquals(1, ReelsSearch.levenshtein("cat", "kat"))
        assertEquals(2, ReelsSearch.levenshtein("fracton", "fractions"))
    }
}
