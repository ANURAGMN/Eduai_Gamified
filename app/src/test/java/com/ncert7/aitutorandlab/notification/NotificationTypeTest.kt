package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTypeTest {
    @Test
    fun fromId_resolvesKnownTypes() {
        assertEquals(NotificationType.DAILY_REMINDER, NotificationType.fromId("daily_reminder"))
        assertEquals(NotificationType.AVATAR_UNLOCK_EXPIRING, NotificationType.fromId("avatar_unlock_expiring"))
    }

    @Test
    fun fromId_returnsNullForUnknown() {
        assertNull(NotificationType.fromId("league_overtaken"))
    }

    @Test
    fun allEntries_haveUniqueIds() {
        val ids = NotificationType.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun priorityOrder_matchesSpec() {
        assertTrue(NotificationType.EXAM_COUNTDOWN.evalPriority < NotificationType.STREAK_AT_RISK.evalPriority)
        assertTrue(NotificationType.STREAK_AT_RISK.evalPriority < NotificationType.TASKS_PENDING.evalPriority)
        assertTrue(NotificationType.TASKS_PENDING.evalPriority < NotificationType.DAILY_REMINDER.evalPriority)
        assertTrue(NotificationType.INACTIVITY_3.evalPriority < NotificationType.WEEKLY_XP_CLOSE.evalPriority)
        assertTrue(NotificationType.WEEKLY_XP_CLOSE.evalPriority < NotificationType.AVATAR_UNLOCK_EXPIRING.evalPriority)
    }
}
