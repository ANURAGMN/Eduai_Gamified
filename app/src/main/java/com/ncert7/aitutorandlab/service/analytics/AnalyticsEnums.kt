package com.ncert7.aitutorandlab.service.analytics


enum class ScreenName(val displayName: String) {
    LOGIN("LOGIN"),
    USER_DETAIL_ENTRY("USER_DETAIL_ENTRY"),
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
    MATH_AGENT("MATH_AGENT")

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
    HOME_VIEW("home_view")
}

enum class ClickSource(val value: String) {
    HOME("HOME"),
    CONCEPT_LIST("CONCEPT_LIST"),
    CHAPTER_LIST("CHAPTER_LIST"),
    SUBJECT_LIST("SUBJECT_LIST")
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
    REVISION("REVISION")
}

enum class SimulationInteraction(val value: String) {
    URL("URL"),
    AGENT("AGENT")
}

/** Ad format shown in the app (extend when interstitial/rewarded are added). */
enum class AdType(val value: String) {
    BANNER("BANNER")
}

/** Where the ad was shown. */
enum class AdPlacement(val value: String) {
    AD_DIALOG("AD_DIALOG")
}

/** Ad lifecycle events synced to Firestore (screenName = AD). */
enum class AdInteraction(val value: String) {
    SHOWN("SHOWN"),
    LOADED("LOADED"),
    IMPRESSION("IMPRESSION"),
    CLICK("CLICK"),
    OPENED("OPENED"),
    CLOSED("CLOSED"),
    FAILED("FAILED")
}