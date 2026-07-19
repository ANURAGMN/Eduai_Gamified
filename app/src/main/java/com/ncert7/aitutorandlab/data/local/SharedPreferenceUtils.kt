package com.ncert7.aitutorandlab.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import com.ncert7.aitutorandlab.utils.resolveStoredSubjectId

class SharedPreferenceUtils(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID_TOKEN = "key_id_token"
        private const val KEY_TOKEN_EXPIRY_TIME = "key_token_expiry_time"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_SELECTED_SUBJECT = "selected_subject"
        private const val KEY_SELECTED_SUBJECT_ID = "selected_subject_id"
        private const val KEY_SESSION = "key_current_session"
        private const val KEY_SIM_OPEN_COUNT = "key_sim_open_count"
        private const val KEY_SIM_OPEN_DATE = "key_sim_open_date"
        private const val KEY_LEGACY_PROGRESS_MIGRATION = "legacy_progress_migration_v1"
        private const val KEY_SIMULATION_LAST_SYNCED_DATE = "key_simulation_last_synced_date"
    }

    fun setIdToken(idToken: String) {
        prefs.edit { putString(KEY_ID_TOKEN, idToken) }
    }

    fun getIdToken(): String? {
        return prefs.getString(KEY_ID_TOKEN, null)
    }

    fun clearIdToken() {
        prefs.edit { remove(KEY_ID_TOKEN) }
    }

    fun setTokenExpiryTime(expiryTimeMs: Long) {
        prefs.edit { putLong(KEY_TOKEN_EXPIRY_TIME, expiryTimeMs) }
    }

    fun getTokenExpiryTime(): Long {
        return prefs.getLong(KEY_TOKEN_EXPIRY_TIME, 0L)
    }

    fun isTokenExpiredOrExpiring(): Boolean {
        val token = getIdToken()

        // No token = definitely expired
        if (token.isNullOrEmpty()) {
            com.ncert7.aitutorandlab.debug.DebugLogger.debugLog("SharedPreferenceUtils", "✗ No token found in storage")
            return true
        }

        // Check JWT expiry directly from token (most accurate)
        // This validates against the actual exp claim from Google
        val isTokenExpiringFromJwt = com.ncert7.aitutorandlab.utils.JwtDecoder.isTokenExpiringWithinBuffer(token, 600L)
        if (isTokenExpiringFromJwt) {
            com.ncert7.aitutorandlab.debug.DebugLogger.debugLog("SharedPreferenceUtils", "✗ Token expiring (from JWT exp claim)")
            return true
        }

        // Fallback: also check stored expiry time as secondary validation
        val storedExpiryTime = getTokenExpiryTime()
        if (storedExpiryTime > 0L) {
            val currentTime = System.currentTimeMillis()
            val bufferTime = 10 * 60 * 1000 // 10 minutes
            val isStoredExpired = currentTime >= (storedExpiryTime - bufferTime)
            if (isStoredExpired) {
                com.ncert7.aitutorandlab.debug.DebugLogger.debugLog("SharedPreferenceUtils", "✗ Token expiring (from stored expiry time)")
                return true
            }
        }

        // Token is still valid
        val secondsRemaining = com.ncert7.aitutorandlab.utils.JwtDecoder.getSecondsUntilExpiry(token) ?: 0
        com.ncert7.aitutorandlab.debug.DebugLogger.debugLog("SharedPreferenceUtils", "✓ Token valid: ${secondsRemaining}s remaining")
        return false
    }

    fun clearAllAuthData() {
        prefs.edit {
            remove(KEY_ID_TOKEN)
            remove(KEY_TOKEN_EXPIRY_TIME)
        }
    }

    // Other preferences (unchanged)
    fun setUserId(id: String) {
        prefs.edit { putString(KEY_USER_ID, id) }
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun setLanguagePreference(lang: String) {
        prefs.edit { putString(KEY_LANGUAGE, normalizeLanguageCode(lang)) }
    }

    fun getLanguagePreference(): String? {
        return normalizeLanguageCode(prefs.getString(KEY_LANGUAGE, "en"))
    }

    fun setSubjectSelectionId(subjectId: String) {
        prefs.edit {
            putString(KEY_SELECTED_SUBJECT_ID, subjectId)
            remove(KEY_SELECTED_SUBJECT)
        }
    }

    /** @deprecated Legacy name storage — use [getSubjectSelectionId] */
    fun setSubjectSelection(subject: String) {
        setSubjectSelectionId(resolveStoredSubjectId(subject))
    }

    fun getSubjectSelectionId(): String {
        val storedId = prefs.getString(KEY_SELECTED_SUBJECT_ID, null)
        if (!storedId.isNullOrBlank()) return resolveStoredSubjectId(storedId)
        val legacyName = prefs.getString(KEY_SELECTED_SUBJECT, null)
        val resolved = resolveStoredSubjectId(legacyName)
        prefs.edit {
            putString(KEY_SELECTED_SUBJECT_ID, resolved)
            remove(KEY_SELECTED_SUBJECT)
        }
        return resolved
    }

    /** @deprecated Use [getSubjectSelectionId] — kept for callers not yet migrated */
    fun getSubjectSelection(): String? = getSubjectSelectionId()

    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit { putBoolean(KEY_IS_LOGGED_IN, isLoggedIn) }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setCurrentSession(sessionId: String) {
        prefs.edit { putString(KEY_SESSION, sessionId) }
    }

    fun getCurrentSession(): String? {
        return prefs.getString(KEY_SESSION, null)
    }

    fun clearCurrentSession() {
        prefs.edit { remove(KEY_SESSION) }
    }

    /**
     * Clear all user data on logout
     * Removes user ID, selected subject, and current session
     * Keeps language preference for future login convenience
     */
    fun clearAllUserData() {
        prefs.edit {
            remove(KEY_USER_ID)
            remove(KEY_SELECTED_SUBJECT)
            remove(KEY_SELECTED_SUBJECT_ID)
            remove(KEY_SESSION)
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_SIM_OPEN_COUNT)
            remove(KEY_SIM_OPEN_DATE)
        }
    }

    /**
     * Increments the count of simulations opened today.
     * Returns true if the user has reached the daily ad-free limit (5).
     */
    fun incrementSimulationOpenCount(): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = prefs.getString(KEY_SIM_OPEN_DATE, "")
        
        var currentCount = if (today == lastDate) {
            prefs.getInt(KEY_SIM_OPEN_COUNT, 0)
        } else {
            0
        }
        
        currentCount++
        
        prefs.edit {
            putInt(KEY_SIM_OPEN_COUNT, currentCount)
            putString(KEY_SIM_OPEN_DATE, today)
        }
        
        return currentCount
    }

    fun getSimulationOpenCount(): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = prefs.getString(KEY_SIM_OPEN_DATE, "")
        return if (today == lastDate) {
            prefs.getInt(KEY_SIM_OPEN_COUNT, 0)
        } else {
            0
        }
    }

    fun isLegacyProgressMigrationDone(): Boolean =
        prefs.getBoolean(KEY_LEGACY_PROGRESS_MIGRATION, false)

    fun setLegacyProgressMigrationDone() {
        prefs.edit { putBoolean(KEY_LEGACY_PROGRESS_MIGRATION, true) }
    }

    fun setSimulationLastSyncedDate(day: String) {
        prefs.edit { putString(KEY_SIMULATION_LAST_SYNCED_DATE, day) }
    }

    fun getSimulationLastSyncedDate(): String? {
        return prefs.getString(KEY_SIMULATION_LAST_SYNCED_DATE, null)
    }
}