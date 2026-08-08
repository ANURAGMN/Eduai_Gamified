package com.ncert7.aitutorandlab.domain.examplan

/**
 * Trial simulation URL thresholds for knowledge-bite progress (not UI overlays).
 *
 * [completionAt] — when the trial item counts as done (min of plan default and HTML budget).
 * Explore / next overlays are time-gated separately via [com.ncert7.aitutorandlab.ui.screens.conceptscreen.SimulationViewerTiming.TRIAL_OVERLAY_MS].
 */
data class SimulationTrialThresholds(
    val htmlClickBudget: Int,
    val completionAt: Int,
) {
    companion object {
        const val DEFAULT_GOAL = 7

        fun compute(htmlClickBudget: Int, planRequiredCount: Int = DEFAULT_GOAL): SimulationTrialThresholds {
            val budget = htmlClickBudget.coerceAtLeast(1)
            val goal = planRequiredCount.coerceAtLeast(1)
            val completionAt = minOf(goal, budget)
            return SimulationTrialThresholds(
                htmlClickBudget = budget,
                completionAt = completionAt,
            )
        }
    }
}
