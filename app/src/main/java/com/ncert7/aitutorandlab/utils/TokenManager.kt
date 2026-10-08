package com.ncert7.aitutorandlab.utils

import android.content.Context
import android.widget.Toast
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.UnknownHostException

object TokenManager {

    private val refreshMutex = Mutex()

    // ──────────────────────────── Save / Get / Clear ────────────────────────────

    /**
     * @param isRefresh  Pass true when called from refreshTokenSilently()
     *                   so the new token gets logged for verification.
     */
    fun saveIdToken(context: Context, idToken: String, isRefresh: Boolean = false) {
        val prefs = SharedPreferenceUtils(context)
        prefs.setIdToken(idToken)

        // Extract actual expiry time from JWT token instead of assuming 60 minutes
        val expiryTimeMs = JwtDecoder.getExpiryTimeInMillis(idToken)
        if (expiryTimeMs != null) {
            prefs.setTokenExpiryTime(expiryTimeMs)
            val currentTimeMs = System.currentTimeMillis()
            val expiresInSeconds = (expiryTimeMs - currentTimeMs) / 1000
            DebugLogger.debugLog("TokenManager", "✓ Token expiry extracted from JWT: expires in ${expiresInSeconds}s")
        } else {
            // Fallback to 60 minutes if JWT decoding fails (should be rare with Gson decoder)
            prefs.setTokenExpiryTime(System.currentTimeMillis() + 60 * 60 * 1000L)
            DebugLogger.debugLog("TokenManager", "JWT exp unreadable — using 60min stored fallback")
        }

        if (isRefresh) {
            val last4 = idToken.takeLast(4)
            DebugLogger.debugLog("TokenManager", "✓ REFRESHED token (last 4): ****$last4")
        } else {
            DebugLogger.debugLog("TokenManager", "✓ Token saved (login) with JWT expiry time")
        }
    }

    fun getIdToken(context: Context): String? {
        return SharedPreferenceUtils(context).getIdToken()
    }

    fun isTokenExpiredOrExpiring(context: Context): Boolean {
        return SharedPreferenceUtils(context).isTokenExpiredOrExpiring()
    }

    fun clearAllTokens(context: Context) {
        val prefs = SharedPreferenceUtils(context)
        val oldToken = prefs.getIdToken()
        val oldExpiry = prefs.getTokenExpiryTime()

        prefs.clearIdToken()
        prefs.setTokenExpiryTime(0L)

        DebugLogger.debugLog(
            "TokenManager",
            "✗ All tokens cleared (had expiry: ${if (oldExpiry > 0) "yes" else "no"}, token: ${if (oldToken != null) "yes" else "no"})"
        )
    }

    // ──────────────────────────── Silent Refresh ─────────────────────────────────

    suspend fun refreshTokenSilently(context: Context): Boolean = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            // Double-check: if token is still valid after acquiring lock, skip refresh
            if (!isTokenExpiredOrExpiring(context)) {
                DebugLogger.debugLog("TokenManager", "✓ Token still valid after acquiring lock, skip refresh")
                return@withLock true
            }

            DebugLogger.debugLog("TokenManager", "⟳ Starting silent token refresh...")

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.AUTH_KEY)
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)

            try {
                // Attempt silent sign-in with timeout
                val account = withTimeoutOrNull(15_000L) {
                    googleSignInClient.silentSignIn().await()
                }

                if (account == null) {
                    DebugLogger.errorLog("TokenManager", "✗ Silent sign-in timed out (15 seconds)")
                    showToastOnMain(context, "Could not refresh session. Please check your connection.")
                    return@withLock false
                }

                // Extract new token
                val newToken = account.idToken
                if (newToken.isNullOrEmpty()) {
                    DebugLogger.errorLog("TokenManager", "✗ Silent sign-in succeeded but idToken is null/empty")
                    return@withLock false
                }

                // Google often returns the same JWT while it is still valid — that is OK.
                val oldToken = getIdToken(context)
                if (oldToken != null && oldToken == newToken) {
                    if (JwtDecoder.hasExpClaim(newToken) && !JwtDecoder.isTokenExpired(newToken)) {
                        DebugLogger.debugLog("TokenManager", "Same token returned but still valid — OK")
                    } else {
                        DebugLogger.debugLog(
                            "TokenManager",
                            "Same token returned and exp missing/expired — saving anyway"
                        )
                    }
                }

                // Save the new token with isRefresh=true for logging
                saveIdToken(context, newToken, isRefresh = true)
                DebugLogger.debugLog("TokenManager", "✓ Token refreshed successfully via silent sign-in")
                return@withLock true

            } catch (e: UnknownHostException) {
                DebugLogger.debugLog("TokenManager", "✗ Offline: cannot refresh token - ${e.message}")
                showToastOnMain(context, "Connect to the internet to refresh your session.")
                return@withLock false

            } catch (e: Exception) {
                val msg = e.message ?: e.javaClass.simpleName
                DebugLogger.errorLog("TokenManager", "✗ Silent sign-in failed: $msg")
                if (isNetworkException(e)) {
                    showToastOnMain(context, "Connect to the internet to refresh your session.")
                }
                return@withLock false
            }
        }
    }

    // ──────────────────────────── Helpers ────────────────────────────────────────

    private fun isNetworkException(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return e is java.io.IOException ||
                msg.contains("network") ||
                msg.contains("timeout") ||
                msg.contains("unable to resolve") ||
                msg.contains("failed to connect")
    }

    private suspend fun showToastOnMain(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}