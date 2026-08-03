package com.ncert7.aitutorandlab.data.remote

import android.content.Context
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.utils.JwtDecoder
import com.ncert7.aitutorandlab.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * OkHttp interceptor that:
 * 1. Validates token exists and is NOT expired/expiring
 * 2. If token is expiring in <10 min, kicks off background silent refresh (non-blocking)
 * 3. Attaches the current valid token to every request (Authorization + X-API-Key headers)
 *
 * The interceptor NEVER blocks the current request. Token refresh is fire-and-forget
 * background operation so the next request gets a fresh token.
 *
 * Key improvements:
 * - Direct JWT validation (checks exp claim, not just stored expiry)
 * - Smart refresh logic (prevents duplicate refreshes via cooldown + flag)
 * - Comprehensive logging for debugging token issues
 * - Fallback to stored token even if refresh fails
 */
class ProactiveTokenInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "ProactiveTokenInterceptor"
        private const val REFRESH_COOLDOWN_MS = 60_000L // 1 minute between refresh attempts
        private const val TOKEN_BUFFER_SECONDS = 600L // 10 minutes

        // Shared atomic flags to prevent duplicate refreshes
        private val isRefreshing = AtomicBoolean(false)
        private val lastRefreshAttemptMs = AtomicLong(0L)

        // Background scope for fire-and-forget refresh coroutines
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        DebugLogger.debugLog(TAG, "───────────────────────────────────────")
        DebugLogger.debugLog(TAG, "⟳ Intercepting request: ${chain.request().url}")

        // Step 1: Proactive refresh for tokens expiring soon (non-blocking)
        triggerBackgroundRefreshIfNeeded()

        // Step 2: If already expired, refresh synchronously so we don't send a doomed request
        var token = TokenManager.getIdToken(context)
        if (token.isNullOrEmpty()) {
            DebugLogger.errorLog(TAG, "✗ CRITICAL: No token found in storage!")
            return chain.proceed(chain.request())
        }

        if (JwtDecoder.isTokenExpired(token)) {
            DebugLogger.warnLog(TAG, "Token expired — refreshing synchronously before request")
            refreshTokenBlocking()
            token = TokenManager.getIdToken(context)
            if (token.isNullOrEmpty()) {
                DebugLogger.errorLog(TAG, "✗ No token after refresh")
                return chain.proceed(chain.request())
            }
        }

        val isTokenExpiringWithinBuffer = JwtDecoder.isTokenExpiringWithinBuffer(token, TOKEN_BUFFER_SECONDS)
        val secondsRemaining = JwtDecoder.getSecondsUntilExpiry(token) ?: 0

        when {
            JwtDecoder.isTokenExpired(token) -> {
                DebugLogger.errorLog(TAG, "✗ Token still expired after refresh attempt")
            }
            isTokenExpiringWithinBuffer -> {
                DebugLogger.warnLog(TAG, "⚠ Token expiring in ${secondsRemaining}s (buffer: ${TOKEN_BUFFER_SECONDS}s)")
            }
            else -> {
                DebugLogger.debugLog(TAG, "✓ Token valid: ${secondsRemaining}s remaining")
            }
        }

        // Attach token to request (both headers for compatibility)
        val requestBuilder = chain.request().newBuilder()
        requestBuilder.header("Authorization", "Bearer $token")
        requestBuilder.header("X-API-Key", token)

        // Log token attachment (mask for security)
        val tokenLast4 = token.takeLast(4)
        DebugLogger.debugLog(TAG, "✓ Attached token to request (ends with: $tokenLast4)")

        val request = requestBuilder.build()
        DebugLogger.debugLog(TAG, "─────────────────────────────────────── ►")

        return chain.proceed(request)
    }

    /**
     * Triggers a background token refresh if token is expiring soon.
     * Non-blocking: uses fire-and-forget coroutine.
     *
     * Guards against duplicate refreshes:
     * - Only one refresh at a time via [isRefreshing] flag
     * - Minimum [REFRESH_COOLDOWN_MS] between refresh attempts
     * - Only runs if token is actually expiring (within buffer)
     */
    private fun triggerBackgroundRefreshIfNeeded() {
        val token = TokenManager.getIdToken(context)

        if (token.isNullOrEmpty()) {
            DebugLogger.debugLog(TAG, "No token to refresh")
            return
        }

        // Check if token is actually expiring soon
        if (!JwtDecoder.isTokenExpiringWithinBuffer(token, TOKEN_BUFFER_SECONDS)) {
            DebugLogger.debugLog(TAG, "Token valid, no refresh needed")
            return
        }

        // Check if we're already refreshing
        if (!isRefreshing.compareAndSet(false, true)) {
            DebugLogger.debugLog(TAG, "⟳ Refresh already in progress, skipping")
            return
        }

        // Check cooldown to prevent refresh spam
        val now = System.currentTimeMillis()
        val timeSinceLastAttempt = now - lastRefreshAttemptMs.get()
        if (timeSinceLastAttempt < REFRESH_COOLDOWN_MS) {
            DebugLogger.debugLog(TAG, "⟳ Refresh cooldown active (${timeSinceLastAttempt}ms/${REFRESH_COOLDOWN_MS}ms), skipping")
            isRefreshing.set(false)
            return
        }

        lastRefreshAttemptMs.set(now)

        // Fire-and-forget background refresh
        scope.launch {
            try {
                val secondsRemaining = JwtDecoder.getSecondsUntilExpiry(token) ?: 0
                DebugLogger.debugLog(TAG, "⟳ Background: Starting proactive refresh (${secondsRemaining}s remaining)")

                val success = TokenManager.refreshTokenSilently(context)

                if (success) {
                    val newToken = TokenManager.getIdToken(context)
                    if (newToken != null) {
                        val newSecondsRemaining = JwtDecoder.getSecondsUntilExpiry(newToken) ?: 0
                        DebugLogger.debugLog(TAG, "✓ Background: Token refreshed successfully (${newSecondsRemaining}s now)")
                    }
                } else {
                    DebugLogger.errorLog(TAG, "✗ Background: Token refresh failed (will retry on next request)")
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "✗ Background: Refresh exception: ${e.message}")
            } finally {
                isRefreshing.set(false)
            }
        }
    }

    /** Blocks the OkHttp thread briefly to obtain a fresh token before a doomed 401. */
    private fun refreshTokenBlocking() {
        if (!isRefreshing.compareAndSet(false, true)) {
            // Another refresh in flight — wait briefly for it to finish
            Thread.sleep(400)
            return
        }
        try {
            runBlocking(Dispatchers.IO) {
                TokenManager.refreshTokenSilently(context)
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Synchronous refresh failed: ${e.message}")
        } finally {
            isRefreshing.set(false)
        }
    }

    // Helper to add warn level logging to DebugLogger if not already present
}