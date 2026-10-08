package com.ncert7.aitutorandlab.data.remote

import android.content.Context
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.utils.AuthTokenPolicy
import com.ncert7.aitutorandlab.utils.JwtDecoder
import com.ncert7.aitutorandlab.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Retries a request once after a 401 by silently refreshing the Google ID token.
 * Works with [ProactiveTokenInterceptor] for chat/math/revision/simulation APIs.
 */
class TokenAuthenticator(private val context: Context) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val MAX_PRIOR_RESPONSES = 1
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) > MAX_PRIOR_RESPONSES) {
            DebugLogger.debugLog(TAG, "401 retry budget exhausted")
            return null
        }

        val previousToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
            .orEmpty()

        val refreshed = try {
            runBlocking(Dispatchers.IO) {
                TokenManager.refreshTokenSilently(context)
            }
        } catch (e: Exception) {
            DebugLogger.debugLog(TAG, "Refresh on 401 failed: ${e.message}")
            false
        }

        if (!refreshed) return null

        val newToken = TokenManager.getIdToken(context) ?: return null

        // Google often returns the same JWT if it is still valid — that is OK.
        if (AuthTokenPolicy.shouldAbandonSameTokenAfter401(
                previousToken = previousToken,
                newToken = newToken,
                hasExpClaim = JwtDecoder.hasExpClaim(newToken),
                isExpired = JwtDecoder.isTokenExpired(newToken),
            )
        ) {
            DebugLogger.debugLog(TAG, "Refresh returned same expired token — giving up")
            return null
        }

        DebugLogger.debugLog(TAG, "Retrying request after 401 with refreshed token")
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header("X-API-Key", newToken)
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
