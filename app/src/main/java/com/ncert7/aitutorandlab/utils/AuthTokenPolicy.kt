package com.ncert7.aitutorandlab.utils

/**
 * Pure auth/token decisions used by chat (and other agent) HTTP paths.
 * Kept free of Android so unit tests can cover the Sep-2026 JWT refresh bugs.
 */
object AuthTokenPolicy {

    const val BUFFER_SECONDS = 600L // 10 minutes
    const val BUFFER_MS = BUFFER_SECONDS * 1000L

    /**
     * Whether the app should treat the session as expired/expiring and refresh.
     *
     * If JWT `exp` is readable, trust it. If not, fall back to stored expiry —
     * never treat "unreadable JWT" alone as expired (that caused the R8 refresh storm).
     */
    fun isExpiredOrExpiring(
        hasExpClaim: Boolean,
        secondsUntilExpiry: Long?,
        storedExpiryMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        bufferSeconds: Long = BUFFER_SECONDS,
    ): Boolean {
        if (hasExpClaim) {
            val remaining = secondsUntilExpiry ?: return true
            return remaining <= bufferSeconds
        }
        if (storedExpiryMs > 0L) {
            return nowMs >= (storedExpiryMs - bufferSeconds * 1000L)
        }
        // No JWT exp and no stored expiry — force refresh
        return true
    }

    /**
     * After silent Google refresh: may we attach/retry with [newToken]?
     * Same JWT is OK when Google reuses a still-valid token.
     */
    fun isRefreshedTokenUsable(
        newToken: String?,
        hasExpClaim: Boolean,
        isExpired: Boolean,
    ): Boolean {
        if (newToken.isNullOrEmpty()) return false
        // Unreadable exp: still usable (server will 401 if truly bad)
        if (!hasExpClaim) return true
        return !isExpired
    }

    /**
     * OkHttp Authenticator: stop retrying when refresh returned the same token
     * and that token is known-expired.
     */
    fun shouldAbandonSameTokenAfter401(
        previousToken: String,
        newToken: String,
        hasExpClaim: Boolean,
        isExpired: Boolean,
    ): Boolean {
        if (newToken != previousToken) return false
        return hasExpClaim && isExpired
    }

    /** Block the request only when JWT exp is readable and already past. */
    fun shouldRefreshSynchronously(hasExpClaim: Boolean, isExpired: Boolean): Boolean {
        return hasExpClaim && isExpired
    }

    /** Background proactive refresh when exp is readable and inside buffer. */
    fun shouldRefreshInBackground(
        hasExpClaim: Boolean,
        isExpiringWithinBuffer: Boolean,
    ): Boolean {
        return hasExpClaim && isExpiringWithinBuffer
    }
}
