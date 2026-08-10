package com.ncert7.aitutorandlab.service.ads

/**
 * Banner ads are paced by in-simulation engagement, not by page opens. Every tap/slider/input inside
 * a simulation counts toward [SIM_INTERACTIONS_PER_AD]; once the learner crosses that many interactions
 * since the last ad, the next open boundary shows one ad and the counter resets. This ties ad exposure
 * to real activity while never interrupting the learner mid-simulation.
 */
object ClickAdPolicy {
    const val SIM_INTERACTIONS_PER_AD = 20

    fun shouldShowAd(simInteractionsSinceLastAd: Int): Boolean {
        return simInteractionsSinceLastAd >= SIM_INTERACTIONS_PER_AD
    }
}

/** @deprecated Use [ClickAdPolicy] */
typealias SimulationAdPolicy = ClickAdPolicy
