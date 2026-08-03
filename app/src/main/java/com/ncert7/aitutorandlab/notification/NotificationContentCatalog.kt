package com.ncert7.aitutorandlab.notification

/**
 * Runtime values injected into notification copy via `{tokens}`.
 */
data class NotificationTokens(
    val name: String = "",
    val bite: String = "",
    val days: Int = 0,
    val streak: Int = 0,
    val gems: Int = 0,
    val league: String = "",
    val avatar: String = "",
) {
    fun fill(template: String): String =
        template
            .replace("{name}", name)
            .replace("{bite}", bite)
            .replace("{days}", days.toString())
            .replace("{streak}", streak.toString())
            .replace("{gems}", gems.toString())
            .replace("{league}", league)
            .replace("{avatar}", avatar)
}

data class NotificationContentTemplate(
    val type: NotificationType,
    val titleTemplate: String,
    val bodyTemplate: String,
    val primaryLabel: String,
    val deepLinkParams: Map<String, String> = emptyMap(),
)

data class ResolvedNotificationContent(
    val type: NotificationType,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val primaryLabel: String,
    val deepLinkRoute: String,
    val deepLinkParams: Map<String, String>,
    val highPriority: Boolean,
)

private data class KnNotificationCopy(
    val title: String,
    val body: String,
    val primaryLabel: String,
)

object NotificationContentCatalog {
    private val templates: Map<NotificationType, NotificationContentTemplate> =
        mapOf(
            NotificationType.DAILY_REMINDER to
                NotificationContentTemplate(
                    type = NotificationType.DAILY_REMINDER,
                    titleTemplate = "Time for today's plan",
                    bodyTemplate = "Hi {name} — {bite} is ready. About {days} min today.",
                    primaryLabel = "Start now",
                ),
            NotificationType.STREAK_AT_RISK to
                NotificationContentTemplate(
                    type = NotificationType.STREAK_AT_RISK,
                    titleTemplate = "Keep your {streak}-day streak",
                    bodyTemplate = "One quick task keeps your flame going. You've got this.",
                    primaryLabel = "Continue streak",
                ),
            NotificationType.STREAK_SAVED to
                NotificationContentTemplate(
                    type = NotificationType.STREAK_SAVED,
                    titleTemplate = "Your streak is safe",
                    bodyTemplate = "A streak freeze covered yesterday — still on {streak} days.",
                    primaryLabel = "View streak",
                ),
            NotificationType.STREAK_COMEBACK to
                NotificationContentTemplate(
                    type = NotificationType.STREAK_COMEBACK,
                    titleTemplate = "Fresh start!",
                    bodyTemplate = "Begin a new streak today — {bite} is ready when you are.",
                    primaryLabel = "Start again",
                ),
            NotificationType.TASKS_PENDING to
                NotificationContentTemplate(
                    type = NotificationType.TASKS_PENDING,
                    titleTemplate = "{days} tasks left today",
                    bodyTemplate = "Finish today's trial to complete the day. Two minutes each.",
                    primaryLabel = "Finish today",
                ),
            NotificationType.CHAPTER_PROGRESS to
                NotificationContentTemplate(
                    type = NotificationType.CHAPTER_PROGRESS,
                    titleTemplate = "Continue {bite}",
                    bodyTemplate = "You're {days}% through — pick up where you left off.",
                    primaryLabel = "Continue",
                ),
            NotificationType.EXAM_COUNTDOWN to
                NotificationContentTemplate(
                    type = NotificationType.EXAM_COUNTDOWN,
                    titleTemplate = "{days} days to your exam",
                    bodyTemplate = "Today's plan: {bite}. A little each day adds up.",
                    primaryLabel = "View plan",
                ),
            NotificationType.INACTIVITY_3 to
                NotificationContentTemplate(
                    type = NotificationType.INACTIVITY_3,
                    titleTemplate = "{days} days away",
                    bodyTemplate = "Pick up {bite} whenever you're ready.",
                    primaryLabel = "Open",
                ),
            NotificationType.INACTIVITY_7 to
                NotificationContentTemplate(
                    type = NotificationType.INACTIVITY_7,
                    titleTemplate = "{days} days away",
                    bodyTemplate = "Your garden's waiting — a two-minute session brings it back.",
                    primaryLabel = "Open",
                ),
            NotificationType.INACTIVITY_14 to
                NotificationContentTemplate(
                    type = NotificationType.INACTIVITY_14,
                    titleTemplate = "{days} days away",
                    bodyTemplate = "Still here when you are — your progress is safe.",
                    primaryLabel = "Open",
                ),
            NotificationType.WEEKLY_XP_CLOSE to
                NotificationContentTemplate(
                    type = NotificationType.WEEKLY_XP_CLOSE,
                    titleTemplate = "Almost at your weekly goal",
                    bodyTemplate = "A little more XP this week gets you there.",
                    primaryLabel = "View progress",
                ),
            NotificationType.AVATAR_UNLOCK_EXPIRING to
                NotificationContentTemplate(
                    type = NotificationType.AVATAR_UNLOCK_EXPIRING,
                    titleTemplate = "New tutor leaves soon",
                    bodyTemplate = "Try {avatar} before this week's drop rotates out.",
                    primaryLabel = "View avatar",
                ),
        )

    private val knText: Map<NotificationType, KnNotificationCopy> =
        mapOf(
            NotificationType.DAILY_REMINDER to
                KnNotificationCopy(
                    title = "ಇಂದಿನ ಯೋಜನೆಗೆ ಸಮಯ",
                    body = "ನಮಸ್ಕಾರ {name} — {bite} ಸಿದ್ಧ. ಇಂದು ಸುಮಾರು {days} ನಿಮಿಷ.",
                    primaryLabel = "ಈಗ ಪ್ರಾರಂಭಿಸಿ",
                ),
            NotificationType.STREAK_AT_RISK to
                KnNotificationCopy(
                    title = "ನಿಮ್ಮ {streak} ದಿನದ ಸ್ಟ್ರೀಕ್",
                    body = "ಒಂದು ಚಿಕ್ಕ ಕೆಲಸವೇ ಜ್ವಾಲೆಯನ್ನು ಉಳಿಸುತ್ತದೆ. ನಿಮಗೆ ಸಾಧ್ಯ!",
                    primaryLabel = "ಸ್ಟ್ರೀಕ್ ಮುಂದುವರಿಸಿ",
                ),
            NotificationType.STREAK_SAVED to
                KnNotificationCopy(
                    title = "ನಿಮ್ಮ ಸ್ಟ್ರೀಕ್ ಸುರಕ್ಷಿತ",
                    body = "ಸ್ಟ್ರೀಕ್ ಫ್ರೀಜ್ ನಿನ್ನೆ ಕವರ್ ಮಾಡಿತು — ಇನ್ನೂ {streak} ದಿನ.",
                    primaryLabel = "ಸ್ಟ್ರೀಕ್ ನೋಡಿ",
                ),
            NotificationType.STREAK_COMEBACK to
                KnNotificationCopy(
                    title = "ಹೊಸ ಆರಂಭ!",
                    body = "ಇಂದು ಹೊಸ ಸ್ಟ್ರೀಕ್ — {bite} ನಿಮಗಾಗಿ ಸಿದ್ಧ.",
                    primaryLabel = "ಮತ್ತೆ ಪ್ರಾರಂಭಿಸಿ",
                ),
            NotificationType.TASKS_PENDING to
                KnNotificationCopy(
                    title = "ಇಂದು {days} ಕೆಲಸ ಬಾಕಿ",
                    body = "ಇಂದಿನ ಟ್ರಯಲ್ ಮುಗಿಸಿ ದಿನ ಪೂರ್ಣಗೊಳಿಸಿ.",
                    primaryLabel = "ಇಂದು ಮುಗಿಸಿ",
                ),
            NotificationType.CHAPTER_PROGRESS to
                KnNotificationCopy(
                    title = "{bite} ಮುಂದುವರಿಸಿ",
                    body = "ನೀವು {days}% ಪೂರ್ಣಗೊಳಿಸಿದ್ದೀರಿ — ಎಲ್ಲಿ ನಿಲ್ಲಿಸಿದ್ದೀರಿ ಅಲ್ಲಿಂದ.",
                    primaryLabel = "ಮುಂದುವರಿಸಿ",
                ),
            NotificationType.EXAM_COUNTDOWN to
                KnNotificationCopy(
                    title = "ಪರೀಕ್ಷೆಗೆ {days} ದಿನ",
                    body = "ಇಂದಿನ ಯೋಜನೆ: {bite}. ಪ್ರತಿದಿನ ಸ್ವಲ್ಪ ಸಾಕು.",
                    primaryLabel = "ಯೋಜನೆ ನೋಡಿ",
                ),
            NotificationType.INACTIVITY_3 to
                KnNotificationCopy(
                    title = "{days} ದಿನಗಳಿಂದ",
                    body = "{bite} ಸಿದ್ಧವಾದಾಗ ಮುಂದುವರಿಯಿರಿ.",
                    primaryLabel = "ತೆರೆಯಿರಿ",
                ),
            NotificationType.INACTIVITY_7 to
                KnNotificationCopy(
                    title = "{days} ದಿನಗಳಿಂದ",
                    body = "ನಿಮ್ಮ ತೋಟ ಕಾಯುತ್ತಿದೆ — ಎರಡು ನಿಮಿಷ ಸಾಕು.",
                    primaryLabel = "ತೆರೆಯಿರಿ",
                ),
            NotificationType.INACTIVITY_14 to
                KnNotificationCopy(
                    title = "{days} ದಿನಗಳಿಂದ",
                    body = "ಯಾವಾಗ ಬೇಕಾದರೂ ಬನ್ನಿ — ನಿಮ್ಮ ಪ್ರಗತಿ ಸುರಕ್ಷಿತ.",
                    primaryLabel = "ತೆರೆಯಿರಿ",
                ),
            NotificationType.WEEKLY_XP_CLOSE to
                KnNotificationCopy(
                    title = "ವಾರದ ಗುರಿ ಹತ್ತಿರ",
                    body = "ಈ ವಾರ ಸ್ವಲ್ಪ XP ಇನ್ನೂ ಸಾಕು.",
                    primaryLabel = "ಪ್ರಗತಿ ನೋಡಿ",
                ),
            NotificationType.AVATAR_UNLOCK_EXPIRING to
                KnNotificationCopy(
                    title = "ಹೊಸ ಟ್ಯೂಟರ್ ಶೀಘ್ರ",
                    body = "ಈ ವಾರದ {avatar} ರೋಟೇಟ್ ಆಗುವ ಮೊದಲು ಪ್ರಯತ್ನಿಸಿ.",
                    primaryLabel = "ಅವತಾರ ನೋಡಿ",
                ),
        )

    fun resolve(
        type: NotificationType,
        tokens: NotificationTokens,
        languageCode: String,
    ): ResolvedNotificationContent {
        val base =
            templates[type]
                ?: error("Missing notification template for $type")
        val kn =
            if (isKannada(languageCode)) {
                knText[type]
            } else {
                null
            }
        val titleTemplate = kn?.title ?: base.titleTemplate
        val bodyTemplate = kn?.body ?: base.bodyTemplate
        val primaryLabel = kn?.primaryLabel ?: base.primaryLabel
        return ResolvedNotificationContent(
            type = type,
            category = type.category,
            title = tokens.fill(titleTemplate),
            body = tokens.fill(bodyTemplate),
            primaryLabel = primaryLabel,
            deepLinkRoute = type.deepLinkRoute,
            deepLinkParams = base.deepLinkParams,
            highPriority = type.highPriority,
        )
    }

    fun templateFor(type: NotificationType): NotificationContentTemplate =
        templates[type] ?: error("Missing notification template for $type")

    private fun isKannada(languageCode: String): Boolean =
        languageCode.equals("kn", ignoreCase = true) ||
            languageCode.startsWith("kn-", ignoreCase = true)
}
