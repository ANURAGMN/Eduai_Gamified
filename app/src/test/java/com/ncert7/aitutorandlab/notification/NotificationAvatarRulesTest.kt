package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAvatarRulesTest {
    private val weekMillis = 7L * 24 * 60 * 60 * 1000
    private val hourMillis = 60L * 60 * 1000

    @Test
    fun millisUntilNextDrop_isPositiveAndWithinOneWeek() {
        val millis = NotificationAvatarRules.millisUntilNextWeeklyDrop()
        assertTrue(millis in 1..weekMillis)
    }

    @Test
    fun within24Hours_whenDropIsWithinHalfDay() {
        val now = weekMillis - (12 * hourMillis)
        assertTrue(NotificationAvatarRules.isWithin24HoursOfWeeklyDrop(now))
    }

    @Test
    fun within24Hours_whenDropIsMoreThanOneDayAway() {
        val now = weekMillis - (48 * hourMillis)
        assertFalse(NotificationAvatarRules.isWithin24HoursOfWeeklyDrop(now))
    }
}
