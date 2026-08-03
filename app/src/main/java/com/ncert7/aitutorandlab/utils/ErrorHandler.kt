package com.ncert7.aitutorandlab.utils

import android.content.Context
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handling for all HTTP and network errors.
 * Single source of truth for error messages and status codes.
 */
object ErrorHandler {

    /**
     * Returns a localized error message based on the HTTP status code.
     */
    fun getErrorMessage(context: Context, statusCode: Int): String {
        return when (statusCode) {
            401 -> context.getString(R.string.authentication_failed_please_log_in_again)
            403 -> context.getString(R.string.access_denied_you_don_t_have_permission_to_access_this_resource)
            404 -> context.getString(R.string.resource_not_found)
            501 -> context.getString(R.string.please_try_after_some_time)
            502 -> context.getString(R.string.please_try_tomorrow)
            else -> context.getString(R.string.an_unexpected_error_occurred_please_try_again)
        }
    }

    /** User-facing copy for study-agent session API failures. */
    fun getStudyAgentErrorMessage(context: Context, statusCode: Int?): String {
        return when (statusCode) {
            401 -> context.getString(R.string.error_please_sign_in_again)
            429, 503 -> context.getString(R.string.error_service_busy_try_again_shortly)
            500 -> context.getString(R.string.error_server_error)
            in 502..599 -> context.getString(R.string.error_server_error)
            null, 0 -> context.getString(R.string.sorry_i_couldn_t_process_that_please_try_again)
            else -> getErrorMessage(context, statusCode)
        }
    }

    fun httpStatusFrom(error: Throwable?): Int? {
        val message = error?.message.orEmpty()
        if (message.isBlank()) return null
        return extractStatusCode(message).takeIf { it in 100..599 }
    }

    fun extractStatusCode(errorMessage: String): Int {
        return try {
            val patterns = listOf(
                "HTTP (\\d{3})".toRegex(),
                "SERVER_ERROR_(\\d{3})".toRegex(),
                "(\\d{3})".toRegex()
            )
            for (pattern in patterns) {
                val match = pattern.find(errorMessage)
                if (match != null) return match.groupValues[1].toInt()
            }
            0
        } catch (e: Exception) {
            DebugLogger.errorLog("ErrorHandler", "Failed to extract status code: ${e.message}")
            0
        }
    }

    fun isRetryable(statusCode: Int): Boolean {
        return when (statusCode) {
            in 500..599 -> true
            in 400..499 -> false
            -1, -2 -> true
            else -> true
        }
    }

    fun shouldRetryException(exception: Exception?): Boolean {
        return when {
            exception == null -> false
            exception is SocketTimeoutException -> true
            exception is java.net.ConnectException -> true
            exception is UnknownHostException -> true
            exception is IOException -> {
                val statusCode = extractStatusCode(exception.message ?: "")
                when {
                    statusCode == 500 -> false // Firestore/quota 500 won't self-heal on immediate retry
                    statusCode in 500..599 -> true
                    statusCode in 400..499 -> false
                    statusCode == 0 -> true
                    else -> true
                }
            }
            else -> false
        }
    }

    fun logError(tag: String, statusCode: Int, message: String) {
        DebugLogger.errorLog(tag, "HTTP $statusCode - $message")
    }

    /**
     * Centralized exception handling that returns localized error strings.
     */
    fun handleException(
        context: Context,
        exception: Exception,
        operation: String = "operation",
        tag: String = "ErrorHandler"
    ): String {
        return when (exception) {
            is SocketTimeoutException -> {
                DebugLogger.errorLog(tag, "$operation - Connection timed out")
                context.getString(R.string.error_connection_timeout)
            }
            is UnknownHostException -> {
                DebugLogger.errorLog(tag, "$operation - Unable to reach server")
                context.getString(R.string.error_unable_to_reach_server)
            }
            is retrofit2.HttpException -> {
                val statusCode = exception.code()
                DebugLogger.errorLog(tag, "$operation - HTTP $statusCode")
                when (statusCode) {
                    501, 502 -> getErrorMessage(context, statusCode)
                    404 -> getErrorMessage(context, 404)
                    500 -> getErrorMessage(context, 500)
                    in 500..599 -> context.getString(R.string.error_server_try_later)
                    in 400..499 -> context.getString(R.string.error_request_error)
                    else -> context.getString(R.string.error_network_error_with_code, statusCode)
                }
            }
            is IOException -> {
                val statusCode = extractStatusCode(exception.message ?: "")
                DebugLogger.errorLog(
                    tag,
                    "$operation - IOException with status $statusCode: ${exception.message}"
                )
                when {
                    statusCode == 501 || statusCode == 502 -> getErrorMessage(context, statusCode)
                    statusCode == 404 -> getErrorMessage(context, 404)
                    statusCode == 500 -> getErrorMessage(context, 500)
                    statusCode in 500..599 -> context.getString(R.string.error_server_try_later)
                    statusCode in 400..499 -> context.getString(R.string.error_request_error)
                    else -> context.getString(R.string.error_connection_error)
                }
            }
            else -> {
                DebugLogger.errorLog(
                    tag,
                    "$operation - ${exception.javaClass.simpleName}: ${exception.message}"
                )
                context.getString(R.string.error_unknown)
            }
        }
    }

    // ============ RESPONSE CODE HANDLING ============

    /**
     * Handles different HTTP response codes and determines retry strategy
     * @param resp The response object
     * @param context The application context (for token operations)
     * @param attempt Current attempt number
     * @param tokenExpiredRetries Number of token refresh retries already attempted
     * @return Triple<shouldContinue, shouldBreak, newTokenExpiredRetries>
     */
    suspend fun handleResponseCode(
        resp: Response<*>,
        context: Context,
        attempt: Int,
        tokenExpiredRetries: Int,
        maxTokenRefreshRetries: Int = 3,
        tag: String = "ErrorHandler"
    ): ResponseHandlerResult {
        val code = resp.code()
        val errBody = safeGetErrorBody(resp)
        val lastEx = IOException("HTTP $code: ${errBody ?: resp.message()}")

        return when {
            resp.isSuccessful -> ResponseHandlerResult.Success

            code in 500..599 -> {
                logError(tag, code, "Server error - will retry if transient")
                ResponseHandlerResult.ServerError(lastEx)
            }

            code == 401 -> {
                handle401Error(context, lastEx, tokenExpiredRetries, maxTokenRefreshRetries, tag)
            }

            code in listOf(403, 404) -> {
                logError(tag, code, "Client error - not retrying")
                ResponseHandlerResult.ClientError(lastEx)
            }

            code in 400..499 -> {
                ResponseHandlerResult.OtherClientError(lastEx)
            }

            else -> {
                ResponseHandlerResult.UnknownError(lastEx)
            }
        }
    }

    /**
     * Handles 401 Unauthorized errors with token refresh logic
     * Uses JWT decoder to check actual token expiry, not just stored value
     */
    private suspend fun handle401Error(
        context: Context,
        lastEx: Exception,
        tokenExpiredRetries: Int,
        maxTokenRefreshRetries: Int,
        tag: String
    ): ResponseHandlerResult {
        // Get current token for validation
        val currentToken = TokenManager.getIdToken(context)

        // Check if token is expired using JWT decoder (most accurate)
        val isTokenExpiredFromJwt = if (currentToken != null) {
            JwtDecoder.isTokenExpired(currentToken)
        } else {
            true // No token = definitely expired
        }

        // Also check if expiring soon (within buffer)
        val isTokenExpiringWithinBuffer = if (currentToken != null) {
            JwtDecoder.isTokenExpiringWithinBuffer(currentToken, 600L)
        } else {
            true
        }

        val isTokenExpired = isTokenExpiredFromJwt || isTokenExpiringWithinBuffer

        val secondsRemaining = if (currentToken != null) {
            JwtDecoder.getSecondsUntilExpiry(currentToken) ?: -1
        } else {
            -1
        }

        DebugLogger.debugLog(
            tag,
            "Got 401 - Token expired/expiring: $isTokenExpired (JWT: $isTokenExpiredFromJwt, Buffer: $isTokenExpiringWithinBuffer), " +
                    "Seconds remaining: ${secondsRemaining}, Retry attempt: $tokenExpiredRetries/$maxTokenRefreshRetries"
        )

        if (isTokenExpired && tokenExpiredRetries < maxTokenRefreshRetries) {
            val newRetryCount = tokenExpiredRetries + 1
            DebugLogger.debugLog(
                tag,
                "⟳ Token is expired/expiring, attempting refresh ($newRetryCount/$maxTokenRefreshRetries)"
            )

            val refreshSuccess = TokenManager.refreshTokenSilently(context)
            if (refreshSuccess) {
                // Verify new token was actually obtained
                val newToken = TokenManager.getIdToken(context)
                if (newToken != null && newToken != currentToken) {
                    val newSecondsRemaining = JwtDecoder.getSecondsUntilExpiry(newToken) ?: 0
                    DebugLogger.debugLog(tag, "✓ Token refreshed successfully (${newSecondsRemaining}s now available)")
                    delay(300L) // Small delay before retry to ensure token is propagated
                    return ResponseHandlerResult.Token401RetryAfterRefresh(newRetryCount)
                } else {
                    DebugLogger.errorLog(tag, " Token refresh returned same token or null")
                    return ResponseHandlerResult.Token401RetryAfterFailedRefresh(newRetryCount)
                }
            } else {
                DebugLogger.errorLog(tag, " Token refresh failed")
                return ResponseHandlerResult.Token401RetryAfterFailedRefresh(newRetryCount)
            }
        } else if (isTokenExpired && tokenExpiredRetries >= maxTokenRefreshRetries) {
            DebugLogger.errorLog(
                tag,
                " Token refresh retries exhausted ($maxTokenRefreshRetries attempts), giving up"
            )
            return ResponseHandlerResult.Token401Exhausted(lastEx)
        } else {
            // Got 401 but token appears valid
            DebugLogger.errorLog(
                tag,
                " Got 401 but token appears valid (${secondsRemaining}s remaining) - authentication issue"
            )
            return ResponseHandlerResult.Token401NotExpired(lastEx)
        }
    }

    private fun safeGetErrorBody(resp: Response<*>): String? {
        return try {
            resp.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Sealed class representing different response handling outcomes
     */
    sealed class ResponseHandlerResult {
        object Success : ResponseHandlerResult()
        data class ServerError(val exception: Exception) : ResponseHandlerResult()
        data class ClientError(val exception: Exception) : ResponseHandlerResult()
        data class OtherClientError(val exception: Exception) : ResponseHandlerResult()
        data class UnknownError(val exception: Exception) : ResponseHandlerResult()
        data class Token401RetryAfterRefresh(val newRetryCount: Int) : ResponseHandlerResult()
        data class Token401RetryAfterFailedRefresh(val newRetryCount: Int) : ResponseHandlerResult()
        data class Token401Exhausted(val exception: Exception) : ResponseHandlerResult()
        data class Token401NotExpired(val exception: Exception) : ResponseHandlerResult()
    }

}