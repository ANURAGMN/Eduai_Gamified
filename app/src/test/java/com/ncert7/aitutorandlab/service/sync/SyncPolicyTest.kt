package com.ncert7.aitutorandlab.service.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the pure sync-policy decisions (Phase-2 read-side rules). */
class SyncPolicyTest {

    private val ttl = 3L * 24 * 60 * 60 * 1000 // 3 days
    private val now = 1_000_000_000_000L

    // ---- shouldSkipCatalogPull ----

    @Test
    fun catalog_emptyDb_alwaysPulls() {
        // No local content → never skip, even if "pulled" just now.
        assertFalse(SyncPolicy.shouldSkipCatalogPull(localConceptCount = 0, lastPullMs = now, nowMs = now, ttlMs = ttl))
    }

    @Test
    fun catalog_freshWithinTtl_skips() {
        val lastPull = now - (ttl / 2) // pulled 1.5 days ago
        assertTrue(SyncPolicy.shouldSkipCatalogPull(localConceptCount = 300, lastPullMs = lastPull, nowMs = now, ttlMs = ttl))
    }

    @Test
    fun catalog_pastTtl_pulls() {
        val lastPull = now - (ttl + 1) // just past the window
        assertFalse(SyncPolicy.shouldSkipCatalogPull(localConceptCount = 300, lastPullMs = lastPull, nowMs = now, ttlMs = ttl))
    }

    @Test
    fun catalog_exactlyAtTtl_pulls() {
        val lastPull = now - ttl // boundary: elapsed == ttl is NOT < ttl
        assertFalse(SyncPolicy.shouldSkipCatalogPull(localConceptCount = 300, lastPullMs = lastPull, nowMs = now, ttlMs = ttl))
    }

    // ---- effectiveDeltaCursor (self-heal) ----

    @Test
    fun cursor_normalDelta_keepsStored() {
        assertEquals(500L, SyncPolicy.effectiveDeltaCursor(storedLastSync = 500L, localRowCount = 42))
    }

    @Test
    fun cursor_wipedLocal_selfHealsToZero() {
        // Cursor set but no local rows → full re-pull so the device isn't stranded empty.
        assertEquals(0L, SyncPolicy.effectiveDeltaCursor(storedLastSync = 500L, localRowCount = 0))
    }

    @Test
    fun cursor_firstLogin_isZero() {
        assertEquals(0L, SyncPolicy.effectiveDeltaCursor(storedLastSync = 0L, localRowCount = 0))
    }

    @Test
    fun cursor_zeroStoredWithLocal_isZero() {
        assertEquals(0L, SyncPolicy.effectiveDeltaCursor(storedLastSync = 0L, localRowCount = 10))
    }

    // ---- shouldApplyIncoming (last-write-wins) ----

    @Test
    fun lww_noLocal_applies() {
        assertTrue(SyncPolicy.shouldApplyIncoming(serverUpdatedAt = 100L, localUpdatedAt = null))
    }

    @Test
    fun lww_serverNewer_applies() {
        assertTrue(SyncPolicy.shouldApplyIncoming(serverUpdatedAt = 200L, localUpdatedAt = 100L))
    }

    @Test
    fun lww_serverEqual_doesNotClobber() {
        assertFalse(SyncPolicy.shouldApplyIncoming(serverUpdatedAt = 100L, localUpdatedAt = 100L))
    }

    @Test
    fun lww_serverOlder_doesNotClobber() {
        // A delayed/deferred local change must survive an older server doc.
        assertFalse(SyncPolicy.shouldApplyIncoming(serverUpdatedAt = 50L, localUpdatedAt = 100L))
    }
}
