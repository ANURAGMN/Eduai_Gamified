package com.ncert7.aitutorandlab.data.remote

import android.content.Context
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.utils.AuthTokenPolicy
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
 * 1. If token expires within 10 minutes, kicks off a background silent refresh
 * 2. If token is already expired, refreshes synchronously before the request
 * 3. Attaches Authorization + X-API-Key on every request
 *
 * Logging: routine refresh/valid paths use [DebugLogger.debugLog] only (not Firestore).
 * Use errorLog only for missing-token / hard failures.
 */
class ProactiveTokenInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "ProactiveTokenInterceptor"
        private const val REFRESH_COOLDOWN_MS = 60_000L
        private const val TOKEN_BUFFER_SECONDS = 600L

        private val isRefreshing = AtomicBoolean(false)
        private val lastRefreshAttemptMs = AtomicLong(0L)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        triggerBackgroundRefreshIfNeeded()

        var token = TokenManager.getIdToken(context)
        if (token.isNullOrEmpty()) {
            DebugLogger.errorLog(TAG, "✗ CRITICAL: No token found in storage!")
            return chain.proceed(chain.request())
        }

        // Only block when JWT exp is readable AND past. If exp unreadable, send stored token
        // and let [TokenAuthenticator] / 401 handler refresh — avoids refresh storms.
        if (AuthTokenPolicy.shouldRefreshSynchronously(
                hasExpClaim = JwtDecoder.hasExpClaim(token),
                isExpired = JwtDecoder.isTokenExpired(token),
            )
        ) {
            DebugLogger.debugLog(TAG, "Token expired — refreshing synchronously before request")
            refreshTokenBlocking()
            token = TokenManager.getIdToken(context)
            if (token.isNullOrEmpty()) {
                DebugLogger.errorLog(TAG, "✗ No token after refresh")
                return chain.proceed(chain.request())
            }
        }

        val secondsRemaining = JwtDecoder.getSecondsUntilExpiry(token)
        when {
            secondsRemaining == null -> {
                DebugLogger.debugLog(TAG, "Token exp unreadable — attaching stored token")
            }
            secondsRemaining <= 0 -> {
                // Still expired after refresh — attach anyway; Authenticator may recover on 401
                DebugLogger.debugLog(TAG, "Token still expired after refresh — attaching anyway")
            }
            secondsRemaining <= TOKEN_BUFFER_SECONDS -> {
                DebugLogger.debugLog(TAG, "Token expiring in ${secondsRemaining}s (buffer: ${TOKEN_BUFFER_SECONDS}s)")
            }
            else -> {
                DebugLogger.debugLog(TAG, "✓ Token valid: ${secondsRemaining}s remaining")
            }
        }

        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .header("X-API-Key", token)
            .build()

        return chain.proceed(request)
    }

    private fun triggerBackgroundRefreshIfNeeded() {
        val token = TokenManager.getIdToken(context) ?: return
        if (!AuthTokenPolicy.shouldRefreshInBackground(
                hasExpClaim = JwtDecoder.hasExpClaim(token),
                isExpiringWithinBuffer = JwtDecoder.isTokenExpiringWithinBuffer(token, TOKEN_BUFFER_SECONDS),
            )
        ) {
            return
        }

        if (!isRefreshing.compareAndSet(false, true)) return

        val now = System.currentTimeMillis()
        if (now - lastRefreshAttemptMs.get() < REFRESH_COOLDOWN_MS) {
            isRefreshing.set(false)
            return
        }
        lastRefreshAttemptMs.set(now)

        scope.launch {
            try {
                TokenManager.refreshTokenSilently(context)
            } catch (e: Exception) {
                DebugLogger.debugLog(TAG, "Background refresh exception: ${e.message}")
            } finally {
                isRefreshing.set(false)
            }
        }
    }

    private fun refreshTokenBlocking() {
        if (!isRefreshing.compareAndSet(false, true)) {
            Thread.sleep(400)
            return
        }
        try {
            runBlocking(Dispatchers.IO) {
                TokenManager.refreshTokenSilently(context)
            }
        } catch (e: Exception) {
            DebugLogger.debugLog(TAG, "Synchronous refresh failed: ${e.message}")
        } finally {
            isRefreshing.set(false)
        }
    }
}
