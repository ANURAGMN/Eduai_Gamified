package com.ncert7.aitutorandlab.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decision-table coverage for the JWT refresh / expiry policies introduced
 * after the R8 + java-jwt chat auth failure.
 */
class AuthTokenPolicyTest {

    // ── isExpiredOrExpiring ─────────────────────────────────────────────────

    @Test
    fun `readable jwt with plenty of life is not expiring`() {
        assertFalse(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = true,
                secondsUntilExpiry = 3600,
                storedExpiryMs = 0L,
            )
        )
    }

    @Test
    fun `readable jwt inside buffer is expiring`() {
        assertTrue(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = true,
                secondsUntilExpiry = 120,
                storedExpiryMs = 0L,
            )
        )
    }

    @Test
    fun `readable jwt with null remaining is treated expired`() {
        assertTrue(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = true,
                secondsUntilExpiry = null,
                storedExpiryMs = Long.MAX_VALUE,
            )
        )
    }

    @Test
    fun `unreadable jwt falls back to valid stored expiry`() {
        val now = 1_000_000L
        assertFalse(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = false,
                secondsUntilExpiry = null,
                storedExpiryMs = now + 3_600_000L,
                nowMs = now,
            )
        )
    }

    @Test
    fun `unreadable jwt falls back to expired stored expiry`() {
        val now = 1_000_000L
        assertTrue(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = false,
                secondsUntilExpiry = null,
                storedExpiryMs = now - 1L,
                nowMs = now,
            )
        )
    }

    @Test
    fun `unreadable jwt with stored expiry inside buffer is expiring`() {
        val now = 1_000_000L
        // stored expiry 5 minutes from now, buffer 10 minutes → expiring
        assertTrue(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = false,
                secondsUntilExpiry = null,
                storedExpiryMs = now + 5 * 60 * 1000L,
                nowMs = now,
            )
        )
    }

    @Test
    fun `unreadable jwt with no stored expiry forces refresh`() {
        assertTrue(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = false,
                secondsUntilExpiry = null,
                storedExpiryMs = 0L,
            )
        )
    }

    @Test
    fun `readable jwt is preferred over stale stored expiry`() {
        // JWT says 1h left; stored expiry is in the past — trust JWT
        assertFalse(
            AuthTokenPolicy.isExpiredOrExpiring(
                hasExpClaim = true,
                secondsUntilExpiry = 3600,
                storedExpiryMs = 1L,
                nowMs = 100_000L,
            )
        )
    }

    // ── isRefreshedTokenUsable ──────────────────────────────────────────────

    @Test
    fun `null or blank refreshed token is not usable`() {
        assertFalse(AuthTokenPolicy.isRefreshedTokenUsable(null, hasExpClaim = true, isExpired = false))
        assertFalse(AuthTokenPolicy.isRefreshedTokenUsable("", hasExpClaim = true, isExpired = false))
    }

    @Test
    fun `valid refreshed token is usable`() {
        assertTrue(AuthTokenPolicy.isRefreshedTokenUsable("tok", hasExpClaim = true, isExpired = false))
    }

    @Test
    fun `expired refreshed token is not usable`() {
        assertFalse(AuthTokenPolicy.isRefreshedTokenUsable("tok", hasExpClaim = true, isExpired = true))
    }

    @Test
    fun `refreshed token without exp claim is still usable`() {
        // Avoid blocking retry when decode fails; server will 401 if truly bad
        assertTrue(AuthTokenPolicy.isRefreshedTokenUsable("tok", hasExpClaim = false, isExpired = true))
    }

    // ── shouldAbandonSameTokenAfter401 ──────────────────────────────────────

    @Test
    fun `different token after 401 is not abandoned`() {
        assertFalse(
            AuthTokenPolicy.shouldAbandonSameTokenAfter401(
                previousToken = "old",
                newToken = "new",
                hasExpClaim = true,
                isExpired = true,
            )
        )
    }

    @Test
    fun `same still-valid token after 401 is not abandoned`() {
        assertFalse(
            AuthTokenPolicy.shouldAbandonSameTokenAfter401(
                previousToken = "same",
                newToken = "same",
                hasExpClaim = true,
                isExpired = false,
            )
        )
    }

    @Test
    fun `same expired token after 401 is abandoned`() {
        assertTrue(
            AuthTokenPolicy.shouldAbandonSameTokenAfter401(
                previousToken = "same",
                newToken = "same",
                hasExpClaim = true,
                isExpired = true,
            )
        )
    }

    @Test
    fun `same token without exp claim is not abandoned`() {
        assertFalse(
            AuthTokenPolicy.shouldAbandonSameTokenAfter401(
                previousToken = "same",
                newToken = "same",
                hasExpClaim = false,
                isExpired = true,
            )
        )
    }

    // ── sync / background refresh gates ─────────────────────────────────────

    @Test
    fun `sync refresh only when exp readable and expired`() {
        assertTrue(AuthTokenPolicy.shouldRefreshSynchronously(hasExpClaim = true, isExpired = true))
        assertFalse(AuthTokenPolicy.shouldRefreshSynchronously(hasExpClaim = true, isExpired = false))
        assertFalse(AuthTokenPolicy.shouldRefreshSynchronously(hasExpClaim = false, isExpired = true))
    }

    @Test
    fun `background refresh only when exp readable and inside buffer`() {
        assertTrue(AuthTokenPolicy.shouldRefreshInBackground(hasExpClaim = true, isExpiringWithinBuffer = true))
        assertFalse(AuthTokenPolicy.shouldRefreshInBackground(hasExpClaim = true, isExpiringWithinBuffer = false))
        assertFalse(AuthTokenPolicy.shouldRefreshInBackground(hasExpClaim = false, isExpiringWithinBuffer = true))
    }
}
