package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class NotificationTimeRulesTest {
    @Test
    fun quietHours_overnightWindow_blocksLateEvening() {
        assertTrue(NotificationTimeRules.isQuietHours(21, startHour = 20, endHour = 8))
    }

    @Test
    fun quietHours_overnightWindow_blocksEarlyMorning() {
        assertTrue(NotificationTimeRules.isQuietHours(7, startHour = 20, endHour = 8))
    }

    @Test
    fun quietHours_overnightWindow_allowsMidday() {
        assertFalse(NotificationTimeRules.isQuietHours(12, startHour = 20, endHour = 8))
    }

    @Test
    fun quietHours_overnightWindow_allowsJustBeforeStart() {
        assertFalse(NotificationTimeRules.isQuietHours(19, startHour = 20, endHour = 8))
    }

    @Test
    fun quietHours_sameStartAndEnd_disablesQuietHours() {
        assertFalse(NotificationTimeRules.isQuietHours(23, startHour = 20, endHour = 20))
    }

    @Test
    fun isReminderWindow_matchesWithinThirtyMinutes() {
        assertTrue(
            NotificationTimeRules.isReminderWindow(
                now = LocalTime.of(17, 15),
                reminderHour = 17,
                reminderMinute = 0,
            ),
        )
        assertFalse(
            NotificationTimeRules.isReminderWindow(
                now = LocalTime.of(18, 0),
                reminderHour = 17,
                reminderMinute = 0,
            ),
        )
    }

    @Test
    fun isEvening_startsAtSixPm() {
        assertFalse(NotificationTimeRules.isEvening(17))
        assertTrue(NotificationTimeRules.isEvening(18))
    }
}
