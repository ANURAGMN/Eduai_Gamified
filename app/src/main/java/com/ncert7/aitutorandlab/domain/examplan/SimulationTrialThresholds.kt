package com.ncert7.aitutorandlab.domain.examplan

/**
 * Trial simulation URL thresholds for knowledge-bite prompts.
 *
 * [completionAt] — when the trial item counts as done (min of plan default and HTML budget).
 * [firstPromptAt] — in-WebView nudge to proceed or keep exploring (same as completion).
 * [secondPromptAt] — optional second nudge for learners who chose to explore further.
 */
data class SimulationTrialThresholds(
    val htmlClickBudget: Int,
    val completionAt: Int,
    val firstPromptAt: Int,
    val secondPromptAt: Int?,
) {
    companion object {
        const val DEFAULT_GOAL = 7
        const val EXPLORATION_NUDGE = 10

        fun compute(htmlClickBudget: Int, planRequiredCount: Int = DEFAULT_GOAL): SimulationTrialThresholds {
            val budget = htmlClickBudget.coerceAtLeast(1)
            val goal = planRequiredCount.coerceAtLeast(1)
            val completionAt = minOf(goal, budget)
            val secondPromptAt =
                if (budget > completionAt) {
                    minOf(EXPLORATION_NUDGE, budget).takeIf { it > completionAt }
                } else {
                    null
                }
            return SimulationTrialThresholds(
                htmlClickBudget = budget,
                completionAt = completionAt,
                firstPromptAt = completionAt,
                secondPromptAt = secondPromptAt,
            )
        }
    }
}
