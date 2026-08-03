package com.ncert7.aitutorandlab.service.sync

/**
 * Pure sync-policy decisions, extracted so they can be unit-tested without Firestore/Room/WorkManager.
 * These encode the Phase-2 read-side rules (catalog gate + delta restore + last-write-wins).
 */
object SyncPolicy {

    /**
     * Content-refresh gate: skip the full Concept catalog read when we already have content locally
     * and pulled it within [ttlMs]. Empty DB (first install/login) always pulls.
     */
    fun shouldSkipCatalogPull(
        localConceptCount: Int,
        lastPullMs: Long,
        nowMs: Long,
        ttlMs: Long,
    ): Boolean = localConceptCount > 0 && (nowMs - lastPullMs) < ttlMs

    /**
     * Delta cursor with self-heal: normally the stored cursor, but if we hold a cursor yet have no
     * local rows (progress was wiped) we fall back to 0 so the next restore re-pulls in full and the
     * device can't be stranded empty.
     */
    fun effectiveDeltaCursor(storedLastSync: Long, localRowCount: Int): Long =
        if (storedLastSync > 0L && localRowCount == 0) 0L else storedLastSync

    /**
     * Last-write-wins: apply an incoming server record only if it is strictly newer than the local
     * one (or there is no local row). Prevents a restore from clobbering a newer, possibly unsynced,
     * local change.
     */
    fun shouldApplyIncoming(serverUpdatedAt: Long, localUpdatedAt: Long?): Boolean =
        localUpdatedAt == null || serverUpdatedAt > localUpdatedAt
}
