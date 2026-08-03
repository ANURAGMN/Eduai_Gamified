package com.ncert7.aitutorandlab.notification

import android.content.Context
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class NotificationReminderMode(val dailyCap: Int) {
    OFF(0),
    GENTLE(1),
    STANDARD(3),
    ;

    companion object {
        fun fromStored(value: Int): NotificationReminderMode =
            entries.firstOrNull { it.ordinal == value } ?: STANDARD
    }
}

data class NotificationEvalSettings(
    val reminderMode: NotificationReminderMode,
    val reminderHour: Int,
    val reminderMinute: Int,
    val quietHoursStart: Int,
    val quietHoursEnd: Int,
    private val enabledCategories: Set<NotificationCategory>,
) {
    fun isCategoryEnabled(category: NotificationCategory): Boolean = category in enabledCategories
}

@Singleton
class NotificationSettingsStore @Inject constructor(
    private val prefs: SharedPreferenceUtils,
    @ApplicationContext private val appContext: Context,
) {
    fun isMasterEnabled(): Boolean = prefs.areNotificationsEnabled()

    fun reminderMode(): NotificationReminderMode =
        NotificationReminderMode.fromStored(prefs.getReminderMode())

    fun reminderHour(): Int = prefs.getReminderHour()

    fun reminderMinute(): Int = prefs.getReminderMinute()

    fun quietHoursStart(): Int = prefs.getQuietHoursStart()

    fun quietHoursEnd(): Int = prefs.getQuietHoursEnd()

    fun isCategoryEnabled(category: NotificationCategory): Boolean =
        prefs.isNotificationCategoryEnabled(category.channelId)

    fun effectiveDailyCap(): Int {
        val mode = reminderMode()
        if (mode == NotificationReminderMode.OFF) return 0
        return mode.dailyCap.coerceAtMost(NotificationLedger.DEFAULT_DAILY_CAP)
    }

    fun toEvalSettings(): NotificationEvalSettings =
        NotificationEvalSettings(
            reminderMode = reminderMode(),
            reminderHour = reminderHour(),
            reminderMinute = reminderMinute(),
            quietHoursStart = quietHoursStart(),
            quietHoursEnd = quietHoursEnd(),
            enabledCategories =
                NotificationCategory.entries.filter { isCategoryEnabled(it) }.toSet(),
        )

    fun setMasterEnabled(enabled: Boolean) {
        prefs.setNotificationsEnabled(enabled)
        refreshScheduling()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.setReminderTime(hour, minute)
        refreshScheduling()
    }

    fun setReminderMode(mode: NotificationReminderMode) {
        prefs.setReminderMode(mode.ordinal)
        refreshScheduling()
    }

    fun setQuietHours(startHour: Int, endHour: Int) {
        prefs.setQuietHours(startHour, endHour)
    }

    fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
        prefs.setNotificationCategoryEnabled(category.channelId, enabled)
    }

    private fun refreshScheduling() {
        if (isMasterEnabled() && reminderMode() != NotificationReminderMode.OFF) {
            NotificationScheduler.scheduleAll(appContext)
        } else {
            NotificationScheduler.cancelAll(appContext)
        }
    }
}
