package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationContentCatalogTest {
    @Test
    fun resolve_fillsEnglishTokens() {
        val resolved =
            NotificationContentCatalog.resolve(
                type = NotificationType.DAILY_REMINDER,
                tokens =
                    NotificationTokens(
                        name = "Aanya",
                        bite = "Electric Cell",
                        days = 18,
                    ),
                languageCode = "en",
            )

        assertEquals("Time for today's plan", resolved.title)
        assertTrue(resolved.body.contains("Aanya"))
        assertTrue(resolved.body.contains("Electric Cell"))
        assertTrue(resolved.body.contains("18"))
        assertEquals("Start now", resolved.primaryLabel)
    }

    @Test
    fun resolve_usesKannadaWhenRequested() {
        val resolved =
            NotificationContentCatalog.resolve(
                type = NotificationType.STREAK_AT_RISK,
                tokens = NotificationTokens(streak = 5),
                languageCode = "kn",
            )

        assertEquals("ನಿಮ್ಮ 5 ದಿನದ ಸ್ಟ್ರೀಕ್", resolved.title)
        assertEquals("ಸ್ಟ್ರೀಕ್ ಮುಂದುವರಿಸಿ", resolved.primaryLabel)
    }

    @Test
    fun notificationType_priorityOrder_examBeforeInactivity() {
        assertTrue(NotificationType.EXAM_COUNTDOWN.evalPriority < NotificationType.INACTIVITY_14.evalPriority)
    }

    @Test
    fun resolve_allShippedTypesHaveCopy() {
        val tokens =
            NotificationTokens(
                name = "Sam",
                bite = "Math",
                days = 3,
                streak = 4,
                avatar = "Nova",
            )
        NotificationType.entries.forEach { type ->
            val resolved = NotificationContentCatalog.resolve(type, tokens, languageCode = "en")
            assertTrue("${type.id} title blank", resolved.title.isNotBlank())
            assertTrue("${type.id} body blank", resolved.body.isNotBlank())
            assertTrue("${type.id} primary blank", resolved.primaryLabel.isNotBlank())
            assertTrue("${type.id} title too long", resolved.title.length <= 40)
            assertEquals(type.category, resolved.category)
            assertEquals(type.deepLinkRoute, resolved.deepLinkRoute)
        }
    }

    @Test
    fun resolve_allShippedTypesHaveKannadaCopy() {
        val tokens = NotificationTokens(name = "Sam", bite = "Math", days = 2, streak = 3, avatar = "Nova")
        NotificationType.entries.forEach { type ->
            val resolved = NotificationContentCatalog.resolve(type, tokens, languageCode = "kn")
            assertTrue("${type.id} kn title blank", resolved.title.isNotBlank())
            assertTrue("${type.id} kn body blank", resolved.body.isNotBlank())
        }
    }

    @Test
    fun resolve_chapterProgressQuotesTopicAndPercent() {
        val resolved =
            NotificationContentCatalog.resolve(
                type = NotificationType.CHAPTER_PROGRESS,
                tokens = NotificationTokens(bite = "Photosynthesis", days = 42),
                languageCode = "en",
            )
        assertEquals("Continue Photosynthesis", resolved.title)
        assertTrue(resolved.body.contains("42%"))
    }
}
