package com.ncert7.aitutorandlab.domain.examplan

/**
 * Trial simulation URL thresholds for knowledge-bite progress (not UI overlays).
 *
 * [completionAt] — bites required before the trial item is DONE and rewards fire.
 * Always the plan goal ([DEFAULT_GOAL] = 15), **not** capped by the HTML click budget —
 * short sims used to finish at 2–6 taps via `min(goal, budget)`, which skipped the full
 * reward bar. Explore / next overlays stay time-gated separately via
 * [com.ncert7.aitutorandlab.ui.screens.conceptscreen.SimulationViewerTiming.TRIAL_OVERLAY_MS].
 */
data class SimulationTrialThresholds(
    val htmlClickBudget: Int,
    val completionAt: Int,
) {
    companion object {
        /** Fixed knowledge-bite goal for Plan URL sims — every sim needs this many taps
         *  before DONE / XP / plant. Single source of truth for the materializer too. */
        const val DEFAULT_GOAL = 15

        fun compute(htmlClickBudget: Int, planRequiredCount: Int = DEFAULT_GOAL): SimulationTrialThresholds {
            val budget = htmlClickBudget.coerceAtLeast(1)
            // Never lower the bar for a small HTML budget — always require the plan goal.
            val completionAt = planRequiredCount.coerceAtLeast(DEFAULT_GOAL)
            return SimulationTrialThresholds(
                htmlClickBudget = budget,
                completionAt = completionAt,
            )
        }
    }
}
