package com.ncert7.aitutorandlab.notification

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.ncert7.aitutorandlab.R

/**
 * Android notification channel boundary. Users can mute categories independently.
 * [LEAGUES_SOCIAL] is reserved for Phase 2 (FCM).
 */
enum class NotificationCategory(
    val channelId: String,
    val channelLabel: String,
    @ColorRes val accentColorRes: Int,
    @DrawableRes val fallbackLargeIconRes: Int,
    val highImportanceDefault: Boolean,
) {
    STREAKS(
        channelId = "streaks",
        channelLabel = "Streaks",
        accentColorRes = R.color.notification_streaks,
        fallbackLargeIconRes = R.drawable.ic_fire,
        highImportanceDefault = true,
    ),
    QUESTS(
        channelId = "quests",
        channelLabel = "Quests",
        accentColorRes = R.color.notification_quests,
        fallbackLargeIconRes = R.drawable.ic_trophy,
        highImportanceDefault = false,
    ),
    REMINDERS(
        channelId = "reminders",
        channelLabel = "Reminders",
        accentColorRes = R.color.notification_reminders,
        fallbackLargeIconRes = R.drawable.ic_book,
        highImportanceDefault = false,
    ),
    AVATAR(
        channelId = "avatar",
        channelLabel = "Avatar",
        accentColorRes = R.color.notification_avatar,
        fallbackLargeIconRes = R.drawable.ic_simulation,
        highImportanceDefault = false,
    ),
    LEAGUES_SOCIAL(
        channelId = "leagues_social",
        channelLabel = "Leagues & social",
        accentColorRes = R.color.notification_reminders,
        fallbackLargeIconRes = R.drawable.ic_trophy,
        highImportanceDefault = false,
    ),
}
