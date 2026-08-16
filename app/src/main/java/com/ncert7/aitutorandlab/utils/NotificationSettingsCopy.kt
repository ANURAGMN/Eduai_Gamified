package com.ncert7.aitutorandlab.utils

import com.ncert7.aitutorandlab.notification.NotificationReminderMode

/** Notification settings screen chrome (EN / KN). */
object NotificationSettingsCopy {
    private fun kn(languageCode: String): Boolean = isKannadaLanguage(languageCode)

    fun screenTitle(languageCode: String): String =
        if (kn(languageCode)) "ಅಧಿಸೂಚನೆಗಳು" else "Notifications"

    fun backContentDescription(languageCode: String): String =
        if (kn(languageCode)) "ಹಿಂದೆ" else "Back"

    fun generalSection(languageCode: String): String =
        if (kn(languageCode)) "ಸಾಮಾನ್ಯ" else "General"

    fun blockedAtSystem(languageCode: String): String =
        if (kn(languageCode)) {
            "ಅಧಿಸೂಚನೆಗಳು ಸಿಸ್ಟಮ್ ಮಟ್ಟದಲ್ಲಿ ನಿರ್ಬಂಧಿಸಲಾಗಿದೆ."
        } else {
            "Notifications are blocked at the system level."
        }

    fun enableInSystemSettings(languageCode: String): String =
        if (kn(languageCode)) "ಸಿಸ್ಟಮ್ ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಸಕ್ರಿಯಗೊಳಿಸಿ" else "Enable in system settings"

    fun notificationsToggleTitle(languageCode: String): String = screenTitle(languageCode)

    fun notificationsToggleSubtitle(languageCode: String): String =
        if (kn(languageCode)) {
            "ದೈನಂದಿನ ಜ್ಞಾಪನೆಗಳು ಮತ್ತು ಸರಣಿ ಎಚ್ಚರಿಕೆಗಳು"
        } else {
            "Daily reminders and streak alerts"
        }

    fun dailyReminderSection(languageCode: String): String =
        if (kn(languageCode)) "ದೈನಂದಿನ ಜ್ಞಾಪನೆ" else "Daily reminder"

    fun reminderTime(languageCode: String): String =
        if (kn(languageCode)) "ಜ್ಞಾಪನೆ ಸಮಯ" else "Reminder time"

    fun reminderMode(languageCode: String): String =
        if (kn(languageCode)) "ಜ್ಞಾಪನೆ ಮೋಡ್" else "Reminder mode"

    fun modeOff(languageCode: String): String =
        if (kn(languageCode)) "ಆಫ್" else "Off"

    fun modeGentle(languageCode: String): String =
        if (kn(languageCode)) "ಮೃದು" else "Gentle"

    fun modeStandard(languageCode: String): String =
        if (kn(languageCode)) "ಸಾಮಾನ್ಯ" else "Standard"

    fun modeHint(languageCode: String, mode: NotificationReminderMode): String =
        when (mode) {
            NotificationReminderMode.OFF ->
                if (kn(languageCode)) "ಯಾವುದೇ ನಿಗದಿತ ಜ್ಞಾಪನೆಗಳಿಲ್ಲ." else "No scheduled reminders."
            NotificationReminderMode.GENTLE ->
                if (kn(languageCode)) {
                    "ದಿನಕ್ಕೆ ಗರಿಷ್ಠ ೧ ಅಧಿಸೂಚನೆ."
                } else {
                    "Up to 1 notification per day."
                }
            NotificationReminderMode.STANDARD ->
                if (kn(languageCode)) {
                    "ದಿನಕ್ಕೆ ಗರಿಷ್ಠ ೩ ಅಧಿಸೂಚನೆಗಳು."
                } else {
                    "Up to 3 notifications per day."
                }
        }

    fun categoriesSection(languageCode: String): String =
        if (kn(languageCode)) "ವರ್ಗಗಳು" else "Categories"

    fun openChannelSettingsCd(languageCode: String): String =
        if (kn(languageCode)) {
            "Android ಚಾನಲ್ ಸೆಟ್ಟಿಂಗ್‌ಗಳನ್ನು ತೆರೆಯಿರಿ"
        } else {
            "Open Android channel settings"
        }

    fun quietHoursSection(languageCode: String): String =
        if (kn(languageCode)) "ಶಾಂತ ಗಂಟೆಗಳು" else "Quiet hours"

    fun startLabel(languageCode: String): String =
        if (kn(languageCode)) "ಪ್ರಾರಂಭ" else "Start"

    fun endLabel(languageCode: String): String =
        if (kn(languageCode)) "ಅಂತ್ಯ" else "End"

    fun quietHoursHint(languageCode: String): String =
        if (kn(languageCode)) {
            "ಶಾಂತ ಗಂಟೆಗಳಲ್ಲಿ ಅಧಿಸೂಚನೆಗಳು ಕಳುಹಿಸಲಾಗುವುದಿಲ್ಲ."
        } else {
            "No notifications are sent during quiet hours."
        }

    fun reminderTimePickerTitle(languageCode: String): String =
        if (kn(languageCode)) "ದೈನಂದಿನ ಜ್ಞಾಪನೆ ಸಮಯ" else "Daily reminder time"

    fun quietStartPickerTitle(languageCode: String): String =
        if (kn(languageCode)) "ಶಾಂತ ಗಂಟೆಗಳ ಪ್ರಾರಂಭ" else "Quiet hours start"

    fun quietEndPickerTitle(languageCode: String): String =
        if (kn(languageCode)) "ಶಾಂತ ಗಂಟೆಗಳ ಅಂತ್ಯ" else "Quiet hours end"

    fun okLabel(languageCode: String): String =
        if (kn(languageCode)) "ಸರಿ" else "OK"

    fun cancelLabel(languageCode: String): String =
        if (kn(languageCode)) "ರದ್ದುಮಾಡಿ" else "Cancel"
}
