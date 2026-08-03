package com.ncert7.aitutorandlab.notification

/**
 * Pending navigation consumed by [com.ncert7.aitutorandlab.ui.navigation.BottomNavBar] after launch.
 */
sealed class NotificationDeepLink {
    abstract val route: String

    /** Bottom-tab destinations — use popUpTo tab root when navigating. */
    data class Tab(override val route: String) : NotificationDeepLink()

    /** Stack destinations such as plan trial or avatar studio. */
    data class Screen(override val route: String) : NotificationDeepLink()
}

object NotificationDeepLinkMapper {
    private val tabRoutes =
        setOf("home", "plan", "quests", "leagues", "avatar", "profile")

    fun fromRoute(route: String, params: Map<String, String> = emptyMap()): NotificationDeepLink {
        val navRoute =
            when (route) {
                "trial" -> {
                    val dayIndex = params["dayIndex"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    com.ncert7.aitutorandlab.ui.screens.plan.PlanTrialNavigation.routeForDay(dayIndex)
                }
                "chapter" -> {
                    val chapterId = params["chapterId"].orEmpty()
                    if (chapterId.isBlank()) "home" else "concepts/$chapterId/study"
                }
                "plan" -> "plan"
                "home" -> "home"
                "streak" -> "home"
                "quest" -> "quests"
                "progress" -> "progress"
                "avatar_studio", "avatarStudio" -> "avatar_studio"
                "league" -> "leagues"
                "friends" -> "friends"
                else -> route
            }
        return if (navRoute in tabRoutes) {
            NotificationDeepLink.Tab(navRoute)
        } else {
            NotificationDeepLink.Screen(navRoute)
        }
    }

    fun parseParams(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",")
            .mapNotNull { segment ->
                val parts = segment.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
    }
}

object NotificationDeepLinkStore {
    @Volatile
    private var pending: NotificationDeepLink? = null

    fun setFromIntent(route: String?, paramsRaw: String?) {
        if (route.isNullOrBlank()) return
        pending = NotificationDeepLinkMapper.fromRoute(route, NotificationDeepLinkMapper.parseParams(paramsRaw))
    }

    fun consume(): NotificationDeepLink? {
        val link = pending
        pending = null
        return link
    }
}
