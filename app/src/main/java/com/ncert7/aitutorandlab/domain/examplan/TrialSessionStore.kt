package com.ncert7.aitutorandlab.domain.examplan

/**
 * Holds the active exam-trial list item while the learner is in an agent/sim flow.
 * [recordPendingSessionProgress] / [consumePendingSessionProgress] bridge agent exit
 * and trial-screen resume so progress is synced before partial vs celebration logic runs.
 *
 * [markSoftProceedToNext] is for intentional "continue to next" exits (load stall / time gate)
 * so resume skips the "So close" comeback and opens the next incomplete item.
 */
object TrialSessionStore {
    var activeTrialItemId: Long? = null

    private var pendingExitItemId: Long? = null
    private var pendingExitInteractionCount: Int? = null
    private var softProceedToNext: Boolean = false

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

    fun markSoftProceedToNext() {
        softProceedToNext = true
    }

    fun consumeSoftProceedToNext(): Boolean {
        val flagged = softProceedToNext
        softProceedToNext = false
        return flagged
    }

    fun clear() {
        activeTrialItemId = null
        pendingExitItemId = null
        pendingExitInteractionCount = null
        softProceedToNext = false
    }
}
