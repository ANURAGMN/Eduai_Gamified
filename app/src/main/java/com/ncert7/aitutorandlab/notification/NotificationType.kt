package com.ncert7.aitutorandlab.notification

/**
 * Local notification types (Phase 1). Lower [evalPriority] = chosen first when multiple are eligible.
 */
enum class NotificationType(
    val id: String,
    val category: NotificationCategory,
    val evalPriority: Int,
    val highPriority: Boolean,
    val deepLinkRoute: String,
) {
    EXAM_COUNTDOWN(
        id = "exam_countdown",
        category = NotificationCategory.REMINDERS,
        evalPriority = 10,
        highPriority = true,
        deepLinkRoute = "plan",
    ),
    STREAK_AT_RISK(
        id = "streak_at_risk",
        category = NotificationCategory.STREAKS,
        evalPriority = 20,
        highPriority = true,
        deepLinkRoute = "trial",
    ),
    TASKS_PENDING(
        id = "tasks_pending",
        category = NotificationCategory.QUESTS,
        evalPriority = 30,
        highPriority = false,
        deepLinkRoute = "trial",
    ),
    CHAPTER_PROGRESS(
        id = "chapter_progress",
        category = NotificationCategory.REMINDERS,
        evalPriority = 35,
        highPriority = false,
        deepLinkRoute = "chapter",
    ),
    DAILY_REMINDER(
        id = "daily_reminder",
        category = NotificationCategory.REMINDERS,
        evalPriority = 40,
        highPriority = false,
        deepLinkRoute = "trial",
    ),
    STREAK_SAVED(
        id = "streak_saved",
        category = NotificationCategory.STREAKS,
        evalPriority = 50,
        highPriority = false,
        deepLinkRoute = "home",
    ),
    STREAK_COMEBACK(
        id = "streak_comeback",
        category = NotificationCategory.STREAKS,
        evalPriority = 55,
        highPriority = false,
        deepLinkRoute = "trial",
    ),
    INACTIVITY_3(
        id = "inactivity_3",
        category = NotificationCategory.REMINDERS,
        evalPriority = 60,
        highPriority = false,
        deepLinkRoute = "home",
    ),
    INACTIVITY_7(
        id = "inactivity_7",
        category = NotificationCategory.REMINDERS,
        evalPriority = 65,
        highPriority = false,
        deepLinkRoute = "home",
    ),
    INACTIVITY_14(
        id = "inactivity_14",
        category = NotificationCategory.REMINDERS,
        evalPriority = 70,
        highPriority = false,
        deepLinkRoute = "home",
    ),
    WEEKLY_XP_CLOSE(
        id = "weekly_xp_close",
        category = NotificationCategory.QUESTS,
        evalPriority = 80,
        highPriority = false,
        deepLinkRoute = "progress",
    ),
    AVATAR_UNLOCK_EXPIRING(
        id = "avatar_unlock_expiring",
        category = NotificationCategory.AVATAR,
        evalPriority = 90,
        highPriority = false,
        deepLinkRoute = "avatar_studio",
    ),
    ;

    companion object {
        fun fromId(id: String): NotificationType? = entries.firstOrNull { it.id == id }
    }
}
