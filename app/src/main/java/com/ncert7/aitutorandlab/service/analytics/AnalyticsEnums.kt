package com.ncert7.aitutorandlab.service.analytics


enum class ScreenName(val displayName: String) {
    LOGIN("LOGIN"),
    USER_DETAIL_ENTRY("USER_DETAIL_ENTRY"),
    ONBOARDING("ONBOARDING"),
    HOME("HOME"),
    SUBJECT("SUBJECT"),
    CHAPTER("CHAPTER"),
    CONCEPT("CONCEPT"),
    CONCEPT_DETAIL("CONCEPT_DETAIL"),
    PROGRESS("PROGRESS"),
    SETTINGS("SETTINGS"),
    CHATBOT("CHATBOT"),
    SIMULATIONLIST("SIMULATION_LIST"),
    SIMULATIONVIEWER("SIMULATION_VIEWER"),
    SIMULATIONAGENT("SIMULATION_AGENT"),
    REVISION("REVISION"),
    MATH_AGENT("MATH_AGENT"),
    PLAN("PLAN"),
    PLAN_TRIAL("PLAN_TRIAL"),
    QUESTS("QUESTS"),
    FRIENDS("FRIENDS"),
    LEAGUES("LEAGUES"),
    AVATAR("AVATAR")

}

enum class EventType(val type: String) {
    ENTRY("ENTRY"),
    EXIT("EXIT"),
    CLICK("CLICK"),
    COMPLETE("COMPLETE"),
    FUNNEL("FUNNEL"),
    AD("AD")
}

/** Onboarding / sign-in funnel steps (stored with screenName = FUNNEL). */
enum class FunnelStep(val value: String) {
    LOGIN_VIEW("login_view"),
    GMAIL_TAP("gmail_tap"),
    INSTITUTIONAL_EXPAND("institutional_expand"),
    INSTITUTIONAL_SIGN_IN("institutional_sign_in"),
    PROFILE_SUBMIT("profile_submit"),
    // First-run onboarding (intro slides → subject → chapter → reward world).
    ONBOARDING_START("onboarding_start"),
    ONBOARDING_SUBJECT_SELECTED("onboarding_subject_selected"),
    ONBOARDING_CHAPTER_SELECTED("onboarding_chapter_selected"),
    ONBOARDING_WORLD_SELECTED("onboarding_world_selected"),
    ONBOARDING_COMPLETE("onboarding_complete"),
    HOME_VIEW("home_view")
}

enum class ClickSource(val value: String) {
    HOME("HOME"),
    CONCEPT_LIST("CONCEPT_LIST"),
    CHAPTER_LIST("CHAPTER_LIST"),
    SUBJECT_LIST("SUBJECT_LIST"),
    PLAN_TRIAL("PLAN_TRIAL"),
    NAV("NAV"),
    GAMIFIED_HOME("GAMIFIED_HOME"),
}

/** @deprecated Use [ClickSource] — kept for simulation call sites. */
typealias SimulationSource = ClickSource

enum class ContentClickType(val value: String) {
    LESSON("LESSON"),
    STUDY("STUDY"),
    MATH_PROBLEM("MATH_PROBLEM"),
    CHAPTER_STUDY("CHAPTER_STUDY"),
    CHAPTER_MATH("CHAPTER_MATH"),
    CHAPTER_SIMULATION("CHAPTER_SIMULATION"),
    SUBJECT("SUBJECT"),
    REVISION("REVISION"),
    NAV_TAB("NAV_TAB"),
}

enum class SimulationInteraction(val value: String) {
    URL("URL"),
    AGENT("AGENT")
}

/** Ad format shown in the app (extend when interstitial/rewarded are added). */
enum class AdType(val value: String) {
    BANNER("BANNER"),
    REWARDED("REWARDED"),
}

/** Where the ad was shown. */
enum class AdPlacement(val value: String) {
    AD_DIALOG("AD_DIALOG"),
    QUEST_CLAIM("QUEST_CLAIM"),
    QUEST_BONUS("QUEST_BONUS"),
    AVATAR_SAVE("AVATAR_SAVE"),
    AVATAR_UNLOCK("AVATAR_UNLOCK"),
    TRIAL_CLAIM("TRIAL_CLAIM"),
    TRIAL_DOUBLE_XP("TRIAL_DOUBLE_XP"),
}

/** Ad lifecycle events synced to Firestore (screenName = AD). */
enum class AdInteraction(val value: String) {
    SHOWN("SHOWN"),
    LOADED("LOADED"),
    IMPRESSION("IMPRESSION"),
    CLICK("CLICK"),
    OPENED("OPENED"),
    CLOSED("CLOSED"),
    FAILED("FAILED"),
    REWARD_EARNED("REWARD_EARNED"),
    REWARD_SKIPPED("REWARD_SKIPPED"),
    NOT_READY("NOT_READY"),
}

/** Trial/plan lifecycle param values. */
enum class TrialViewMode(val value: String) {
    PATH("path"),
    STACKED("stacked"),
    GARDEN("garden"),
}

enum class PlanFeasibilityIssueType(val value: String) {
    OVER_CAPACITY("over_capacity"),
    PAST_DATE("past_date"),
    EMPTY_CHAPTER("empty_chapter"),
    TOO_MANY_CHAPTERS("too_many_chapters"),
    OTHER("other"),
}

/** XP / gem economy sources for GA4. */
enum class EconomySource(val value: String) {
    ITEM_COMPLETE("item_complete"),
    STREAK("streak"),
    STREAK_MILESTONE("streak_milestone"),
    QUEST("quest"),
    AD_CLAIM("ad_claim"),
    CHEST("chest"),
    INVITE("invite"),
    SESSION_BONUS("session_bonus"),
    TRIAL_DOUBLE("trial_double"),
}

enum class GemSink(val value: String) {
    AVATAR_UNLOCK("avatar_unlock"),
    OTHER("other"),
}

enum class QuestKind(val value: String) {
    SIMS("sims"),
    STUDY("study"),
    BONUS("bonus"),
}

enum class StreakMilestone(val value: Int) {
    DAY_7(7),
    DAY_30(30),
    DAY_50(50),
}

enum class InviteChannel(val value: String) {
    SHARE_SHEET("share_sheet"),
    COPY_CODE("copy_code"),
}

enum class AvatarUnlockVia(val value: String) {
    AD("ad"),
    GEMS("gems"),
}