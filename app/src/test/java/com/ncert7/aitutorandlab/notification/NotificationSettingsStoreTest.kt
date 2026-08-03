package com.ncert7.aitutorandlab.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSettingsStoreTest {
    @Test
    fun reminderMode_dailyCapMatchesSpec() {
        assertEquals(0, NotificationReminderMode.OFF.dailyCap)
        assertEquals(1, NotificationReminderMode.GENTLE.dailyCap)
        assertEquals(3, NotificationReminderMode.STANDARD.dailyCap)
    }

    @Test
    fun evalSettings_categoryGateWorks() {
        val settings =
            NotificationEvalSettings(
                reminderMode = NotificationReminderMode.STANDARD,
                reminderHour = 17,
                reminderMinute = 0,
                quietHoursStart = 20,
                quietHoursEnd = 8,
                enabledCategories = setOf(NotificationCategory.STREAKS),
            )

        assertEquals(true, settings.isCategoryEnabled(NotificationCategory.STREAKS))
        assertEquals(false, settings.isCategoryEnabled(NotificationCategory.QUESTS))
    }
}
