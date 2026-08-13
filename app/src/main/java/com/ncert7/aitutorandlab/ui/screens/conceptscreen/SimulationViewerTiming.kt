package com.ncert7.aitutorandlab.ui.screens.conceptscreen

/**
 * Shared timing for "Nicely explored / move on" overlays across HTML sim, SimAgent,
 * Study Agent, Math, and Revision — language-agnostic (EN/KN only change copy via [TrialCopy]).
 */
object SimulationViewerTiming {
    /** Second narration (footer / action hints) after the page loads. */
    const val FOOTER_TTS_MS = 75_000L

    /** First proceed / keep-exploring overlay (HTML sim + all agent time gates). */
    const val TRIAL_OVERLAY_MS = 300_000L // 5 min

    /** Soft reminders after the first mark (every 2 min) for agent sessions. */
    const val AGENT_REMINDER_STEP_MS = 120_000L // 2 min

    /** Agent auto-advance hard cap. */
    const val AGENT_HARD_CAP_MS = 16 * 60_000L

    /** Soft marks for [com.ncert7.aitutorandlab.ui.components.AgentSessionTimeGate]: 5, 7, 9, 11, 13, 15. */
    fun agentSoftMarkMs(): List<Long> {
        val marks = mutableListOf(TRIAL_OVERLAY_MS)
        var t = TRIAL_OVERLAY_MS + AGENT_REMINDER_STEP_MS
        while (t < AGENT_HARD_CAP_MS) {
            marks += t
            t += AGENT_REMINDER_STEP_MS
        }
        return marks
    }
}
