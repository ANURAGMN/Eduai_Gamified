package com.ncert7.aitutorandlab.service.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RV.3: unit coverage for the money-path LWW decision used by [GamificationSyncManager.restoreProfile].
 * Balances must never be recomputed from ledgers (R.2); restore is a pure last-write-wins on `updatedAt`
 * with a zero-balance placeholder that always yields to remote.
 */
class GamificationLwwTest {

    private fun wins(
        missing: Boolean = false,
        xp: Int = 0,
        weekly: Int = 0,
        gems: Int = 0,
        localAt: Long = 0L,
        remoteAt: Long = 0L,
    ) = GamificationSyncManager.localProfileWins(missing, xp, weekly, gems, localAt, remoteAt)

    @Test
    fun remoteWins_whenLocalMissing() {
        assertFalse(wins(missing = true, remoteAt = 100))
    }

    @Test
    fun remoteWins_whenLocalIsZeroPlaceholder() {
        // Fresh device: empty local (0/0/0) must yield to remote even if its timestamp is newer.
        assertFalse(wins(xp = 0, weekly = 0, gems = 0, localAt = 9_999, remoteAt = 100))
    }

    @Test
    fun localWins_whenNonPlaceholderAndNewer() {
        assertTrue(wins(xp = 50, localAt = 200, remoteAt = 100))
    }

    @Test
    fun localWins_onTimestampTie_soIdenticalRemoteIsNotReapplied() {
        assertTrue(wins(gems = 5, localAt = 100, remoteAt = 100))
    }

    @Test
    fun remoteWins_whenLocalOlder() {
        assertFalse(wins(xp = 50, localAt = 100, remoteAt = 200))
    }

    @Test
    fun nonPlaceholder_ifAnyBalanceNonZero() {
        // gems-only progress still counts as real local data.
        assertTrue(wins(xp = 0, weekly = 0, gems = 3, localAt = 300, remoteAt = 100))
    }
}
