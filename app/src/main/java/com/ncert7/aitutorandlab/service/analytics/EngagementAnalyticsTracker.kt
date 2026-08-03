package com.ncert7.aitutorandlab.service.analytics

/**
 * GA4-only events for the new first-run / engagement surfaces: onboarding picks, the home coach tour,
 * the nav walkthrough, streak celebrations, the notification primer, the in-app review request, and
 * the garden/space scene-unlock picker. Mirrors [GamificationAnalyticsTracker]'s logging style.
 */
object EngagementAnalyticsTracker {

    // ---- Onboarding (funnel steps are fired separately via FunnelAnalyticsTracker) ----

    fun onboardingSlideView(index: Int) =
        log("onboarding_slide_view", ScreenName.ONBOARDING, mapOf("index" to index))

    fun onboardingSkip(slideIndex: Int) =
        log("onboarding_skip", ScreenName.ONBOARDING, mapOf("index" to slideIndex))

    fun onboardingSubjectSelected(subject: String) =
        log("onboarding_subject_selected", ScreenName.ONBOARDING, mapOf("subject" to subject))

    fun onboardingChapterSelected(chapter: String) =
        log("onboarding_chapter_selected", ScreenName.ONBOARDING, mapOf("chapter" to chapter))

    fun onboardingWorldSelected(world: String) =
        log("onboarding_world_selected", ScreenName.ONBOARDING, mapOf("world" to world))

    fun onboardingPicks(subject: String, chapter: String, world: String) =
        log(
            "onboarding_picks",
            ScreenName.ONBOARDING,
            mapOf("subject" to subject, "chapter" to chapter, "world" to world),
        )

    // ---- Home coach tour (spotlights) ----

    fun homeTourStart() = log("home_tour_start", ScreenName.HOME)

    fun homeTourStep(step: Int) = log("home_tour_step", ScreenName.HOME, mapOf("step" to step))

    fun homeTourSkip(step: Int) = log("home_tour_skip", ScreenName.HOME, mapOf("step" to step))

    fun homeTourComplete() = log("home_tour_complete", ScreenName.HOME)

    // ---- Nav walkthrough (steps into Plan / Avatar / Leagues / Home) ----

    fun navWalkthroughStep(route: String, step: Int) =
        log("nav_walkthrough_step", ScreenName.HOME, mapOf("route" to route, "step" to step))

    fun navWalkthroughSkip(step: Int) =
        log("nav_walkthrough_skip", ScreenName.HOME, mapOf("step" to step))

    fun navWalkthroughComplete() = log("nav_walkthrough_complete", ScreenName.HOME)

    // ---- Streak celebrations ----

    fun streakGreetingShown(streak: Int) =
        log("streak_greeting_shown", ScreenName.HOME, mapOf("streak" to streak))

    fun streakGreetingContinue(streak: Int) =
        log("streak_greeting_continue", ScreenName.HOME, mapOf("streak" to streak))

    fun streakExtendedShown(streak: Int) =
        log("streak_extended_shown", ScreenName.HOME, mapOf("streak" to streak))

    fun streakExtendedDone(streak: Int) =
        log("streak_extended_done", ScreenName.HOME, mapOf("streak" to streak))

    // ---- Notification permission primer ----

    fun notificationPrimerShown(variant: String, attempt: Int) =
        log("notif_primer_shown", null, mapOf("variant" to variant, "attempt" to attempt))

    fun notificationPrimerAccepted(variant: String) =
        log("notif_primer_accepted", null, mapOf("variant" to variant))

    fun notificationPrimerDeclined(variant: String) =
        log("notif_primer_declined", null, mapOf("variant" to variant))

    fun notificationPermissionResult(granted: Boolean) =
        log("notif_permission_result", null, mapOf("granted" to granted))

    // ---- In-app review ----

    fun reviewRequested(trigger: String) =
        log("review_requested", null, mapOf("trigger" to trigger))

    fun reviewThrottled(reason: String) =
        log("review_throttled", null, mapOf("reason" to reason))

    fun planRewardBannerTap(dayCount: Int) =
        log("plan_reward_banner_tap", ScreenName.PLAN, mapOf("day_count" to dayCount))

    // ---- Garden/space scene-unlock picker ----

    fun placeCompleted(zone: Int) =
        log("place_completed", ScreenName.PLAN_TRIAL, mapOf("zone" to zone))

    fun nextPlaceOffered(candidates: Int) =
        log("next_place_offered", ScreenName.PLAN_TRIAL, mapOf("candidates" to candidates))

    fun nextPlacePicked(zone: Int) =
        log("next_place_picked", ScreenName.PLAN_TRIAL, mapOf("zone" to zone))

    fun nextPlaceSurprise(zone: Int) =
        log("next_place_surprise", ScreenName.PLAN_TRIAL, mapOf("zone" to zone))

    private fun log(
        name: String,
        screen: ScreenName?,
        params: Map<String, Any?> = emptyMap(),
    ) = FirebaseAnalyticsHelper.logEvent(eventName = name, screen = screen, params = params)
}
