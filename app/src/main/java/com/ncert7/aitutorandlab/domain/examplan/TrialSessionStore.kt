package com.ncert7.aitutorandlab.domain.examplan

/**
 * Holds the active exam-trial list item while the learner is in an agent/sim flow.
 * [recordPendingSessionProgress] / [consumePendingSessionProgress] bridge agent exit
 * and trial-screen resume so progress is synced before partial vs celebration logic runs.
 */
object TrialSessionStore {
    var activeTrialItemId: Long? = null

    private var pendingExitItemId: Long? = null
    private var pendingExitInteractionCount: Int? = null

    fun recordPendingSessionProgress(itemId: Long, interactionCount: Int) {
        pendingExitItemId = itemId
        pendingExitInteractionCount = interactionCount.coerceAtLeast(0)
    }

    fun consumePendingSessionProgress(): Pair<Long, Int>? {
        val itemId = pendingExitItemId ?: return null
        val count = pendingExitInteractionCount ?: return null
        pendingExitItemId = null
        pendingExitInteractionCount = null
        return itemId to count
    }

    fun clear() {
        activeTrialItemId = null
        pendingExitItemId = null
        pendingExitInteractionCount = null
    }
}
