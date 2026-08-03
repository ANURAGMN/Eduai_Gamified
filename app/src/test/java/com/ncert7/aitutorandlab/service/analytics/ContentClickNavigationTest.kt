package com.ncert7.aitutorandlab.service.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentClickNavigationTest {

    @Test
    fun chapterContentType_mapsSimulationMathAndStudy() {
        assertEquals(
            ContentClickType.CHAPTER_SIMULATION,
            ContentClickNavigation.chapterContentType("SIMULATION"),
        )
        assertEquals(
            ContentClickType.CHAPTER_MATH,
            ContentClickNavigation.chapterContentType("MATH PROBLEM"),
        )
        assertEquals(
            ContentClickType.CHAPTER_STUDY,
            ContentClickNavigation.chapterContentType("STUDY"),
        )
        assertEquals(
            ContentClickType.CHAPTER_STUDY,
            ContentClickNavigation.chapterContentType("unknown"),
        )
    }
}
