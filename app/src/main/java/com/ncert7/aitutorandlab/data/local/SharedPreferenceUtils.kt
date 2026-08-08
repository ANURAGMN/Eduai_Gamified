package com.ncert7.aitutorandlab.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ncert7.aitutorandlab.domain.migration.AppMigrationVersionStore
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import com.ncert7.aitutorandlab.utils.resolveStoredSubjectId

/** Persists app-data migration version and related legacy flags. */
class SharedPreferenceUtils(context: Context) : AppMigrationVersionStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID_TOKEN = "key_id_token"
        private const val KEY_TOKEN_EXPIRY_TIME = "key_token_expiry_time"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_HANDS_FREE_MODE = "key_hands_free_mode"
        private const val KEY_VOICE_FIRST = "key_input_voice_first"
        private const val KEY_SIMULATION_VOICE_ENABLED = "key_simulation_voice_enabled"
        private const val KEY_SIM_COACH_MODE = "key_sim_coach_mode"
        private const val KEY_SIM_COACH_MODE_MIGRATED = "key_sim_coach_mode_migrated_v3"
        private const val KEY_SIM_COACH_MODE_MIGRATED_V4 = "key_sim_coach_mode_migrated_v4"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_SELECTED_SUBJECT = "selected_subject"
        private const val KEY_SELECTED_SUBJECT_ID = "selected_subject_id"
        private const val KEY_SESSION = "key_current_session"
        private const val KEY_SIM_OPEN_COUNT = "key_sim_open_count"
        private const val KEY_SIM_OPEN_DATE = "key_sim_open_date"
        private const val KEY_LEGACY_PROGRESS_MIGRATION = "legacy_progress_migration_v1"
        private const val KEY_SIMULATION_LAST_SYNCED_DATE = "key_simulation_last_synced_date"
        private const val KEY_GAMIFIED_HOME_DEBUG = "key_gamified_home_debug"
        private const val KEY_NATIVE_TUTOR_AVATAR_DEBUG = "key_native_tutor_avatar_debug"
        private const val KEY_GARDEN_DEBUG = "key_garden_debug"
        private const val KEY_FORCE_ONBOARDING_DEBUG = "key_force_onboarding_debug"
        private const val KEY_QUEST_AD_TEST_OVERRIDE_DATE = "key_quest_ad_test_override_date"
        private const val KEY_EXAM_PLAN_USER_CONFIGURED = "key_exam_plan_user_configured"
        private const val KEY_OPEN_EXAM_PLAN_SETUP_PENDING = "key_open_exam_plan_setup_pending"
        private const val KEY_TRIAL_COMPLETIONS_SINCE_AD_PREFIX = "trial_completions_since_ad_"
        private const val KEY_LAST_COUNTED_TRIAL_ITEM_PREFIX = "last_counted_trial_item_"
        private const val KEY_GARDEN_CELEBRATION_PLANT_TOTAL_PREFIX = "garden_celebration_plant_total_"
        private const val KEY_HOME_GARDEN_PLANT_TOTAL_PREFIX = "home_garden_plant_total_"
        private const val KEY_GARDEN_STARTER_HIGHLIGHT_SEEN_PREFIX = "garden_starter_highlight_seen_"
        private const val KEY_TRIAL_MATERIALIZER_VERSION = "trial_materializer_version"
        private const val KEY_APP_DATA_MIGRATION_VERSION = "app_data_migration_version"
        private const val KEY_PROGRESS_LAST_SYNC_PREFIX = "progress_last_sync_"
        private const val KEY_CHAPTER_PROGRESS_LAST_SYNC_PREFIX = "chapter_progress_last_sync_"
        private const val KEY_CONTENT_LAST_PULL = "content_last_pull"
        private const val KEY_NOTIFICATION_PRIMER_DECLINED = "notification_primer_declined"
        private const val KEY_NOTIFICATION_PRIMER_SHOW_COUNT = "notification_primer_show_count"
        private const val KEY_NOTIFICATION_PRIMER_LAST_SHOWN_DAY = "notification_primer_last_shown_day"
        private const val KEY_RATING_FLOW_COMPLETED = "rating_flow_completed"
        private const val KEY_RATING_PROMPT_SHOW_COUNT = "rating_prompt_show_count"
        private const val KEY_RATING_PROMPT_LAST_SHOWN_DAY = "rating_prompt_last_shown_day"
        private const val KEY_HAS_COMPLETED_ANY_TASK = "has_completed_any_task"
        private const val KEY_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_REMINDER_HOUR = "notification_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "notification_reminder_minute"
        private const val KEY_REMINDER_MODE = "notification_reminder_mode"
        private const val KEY_QUIET_HOURS_START = "notification_quiet_hours_start"
        private const val KEY_QUIET_HOURS_END = "notification_quiet_hours_end"
        private const val KEY_CATEGORY_STREAKS = "notification_category_streaks"
        private const val KEY_CATEGORY_QUESTS = "notification_category_quests"
        private const val KEY_CATEGORY_REMINDERS = "notification_category_reminders"
        private const val KEY_CATEGORY_AVATAR = "notification_category_avatar"
        private const val KEY_FIRST_RUN_COMPLETED = "first_run_completed"
        private const val KEY_ONBOARDING_SUBJECT = "onboarding_subject"
        private const val KEY_ONBOARDING_CHAPTER = "onboarding_chapter"
        private const val KEY_ONBOARDING_WORLD = "onboarding_world"
        private const val KEY_HOME_TOUR_COMPLETED = "home_tour_completed"
        private const val KEY_NAV_TOUR_COMPLETED = "nav_tour_completed"
        private const val KEY_STREAK_GREETING_DAY = "streak_greeting_day"
        private const val KEY_ONBOARDING_PICKS_APPLIED = "onboarding_picks_applied"
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

    /** Hands-free voice: after the agent finishes speaking, auto-open the mic. Default on. */
    fun getHandsFreeMode(): Boolean = prefs.getBoolean(KEY_HANDS_FREE_MODE, true)

    fun setHandsFreeMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_HANDS_FREE_MODE, enabled) }
    }

    /** Which input the tutor chat opens in: false = text-first (default), true = voice-first. */
    fun getVoiceFirst(): Boolean = prefs.getBoolean(KEY_VOICE_FIRST, false)

    fun setVoiceFirst(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VOICE_FIRST, enabled) }
    }

    /** Narration in the plain simulation viewer (intro / footer TTS). Default on. */
    fun getSimulationVoiceEnabled(): Boolean = prefs.getBoolean(KEY_SIMULATION_VOICE_ENABLED, true)

    fun setSimulationVoiceEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SIMULATION_VOICE_ENABLED, enabled) }
    }

    /**
     * Selected guided-sim coaching style (v1–v4). Stored as the enum name; when null the caller's
     * DEFAULT (now ONE_CLOCK/v4) applies.
     *
     * One-time migration: an older default was ADAPTIVE (v2), so early testers may have "ADAPTIVE"
     * stored. Exactly once, clear a stored ADAPTIVE so those devices pick up the current default.
     * Anyone who deliberately re-selects v2 afterward keeps it (the migration flag prevents
     * re-clearing). Fresh installs / uninstalls already land on v4 via [SimCoachMode.DEFAULT].
     */
    fun getSimCoachMode(): String? {
        if (!prefs.getBoolean(KEY_SIM_COACH_MODE_MIGRATED, false)) {
            if (prefs.getString(KEY_SIM_COACH_MODE, null) == "ADAPTIVE") {
                prefs.edit { remove(KEY_SIM_COACH_MODE) }
            }
            prefs.edit { putBoolean(KEY_SIM_COACH_MODE_MIGRATED, true) }
        }
        // v4 rollout: the default moved to ONE_CLOCK (v4). Early testers likely have ADAPTIVE (v2) or
        // GUIDED (v3) stored — the OLD auto-defaults — which would pin them off v4. Exactly once, clear
        // those so the device lands on the current default. An explicit re-select afterward sticks
        // (setSimCoachMode marks both migrations done).
        if (!prefs.getBoolean(KEY_SIM_COACH_MODE_MIGRATED_V4, false)) {
            val stored = prefs.getString(KEY_SIM_COACH_MODE, null)
            if (stored == "ADAPTIVE" || stored == "GUIDED") {
                prefs.edit { remove(KEY_SIM_COACH_MODE) }
            }
            prefs.edit { putBoolean(KEY_SIM_COACH_MODE_MIGRATED_V4, true) }
        }
        return prefs.getString(KEY_SIM_COACH_MODE, null)
    }

    fun setSimCoachMode(modeName: String) {
        // An explicit choice satisfies BOTH migrations so it's never second-guessed later.
        prefs.edit {
            putString(KEY_SIM_COACH_MODE, modeName)
            putBoolean(KEY_SIM_COACH_MODE_MIGRATED, true)
            putBoolean(KEY_SIM_COACH_MODE_MIGRATED_V4, true)
        }
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
            remove(KEY_EXAM_PLAN_USER_CONFIGURED)
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

    override fun isLegacyProgressMigrationDone(): Boolean =
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

    fun hasGamifiedHomeDebugOverride(): Boolean =
        prefs.contains(KEY_GAMIFIED_HOME_DEBUG)

    fun isGamifiedHomeDebugEnabled(): Boolean =
        prefs.getBoolean(KEY_GAMIFIED_HOME_DEBUG, false)

    fun setGamifiedHomeDebugEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GAMIFIED_HOME_DEBUG, enabled) }
    }

    fun clearGamifiedHomeDebugOverride() {
        prefs.edit { remove(KEY_GAMIFIED_HOME_DEBUG) }
    }

    fun hasNativeTutorAvatarDebugOverride(): Boolean =
        prefs.contains(KEY_NATIVE_TUTOR_AVATAR_DEBUG)

    fun isNativeTutorAvatarDebugEnabled(): Boolean =
        prefs.getBoolean(KEY_NATIVE_TUTOR_AVATAR_DEBUG, false)

    fun setNativeTutorAvatarDebugEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NATIVE_TUTOR_AVATAR_DEBUG, enabled) }
    }

    fun clearNativeTutorAvatarDebugOverride() {
        prefs.edit { remove(KEY_NATIVE_TUTOR_AVATAR_DEBUG) }
    }

    fun hasGardenDebugOverride(): Boolean = prefs.contains(KEY_GARDEN_DEBUG)

    fun isGardenDebugEnabled(): Boolean = prefs.getBoolean(KEY_GARDEN_DEBUG, false)

    fun setGardenDebugEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GARDEN_DEBUG, enabled) }
    }

    fun clearGardenDebugOverride() {
        prefs.edit { remove(KEY_GARDEN_DEBUG) }
    }

    fun isForceOnboardingDebugEnabled(): Boolean =
        prefs.getBoolean(KEY_FORCE_ONBOARDING_DEBUG, false)

    fun setForceOnboardingDebugEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_FORCE_ONBOARDING_DEBUG, enabled) }
    }

    /** Clears first-run onboarding so the intro flow can be replayed (debug only). */
    fun resetOnboardingForDebugReplay() {
        prefs.edit {
            putBoolean(KEY_FIRST_RUN_COMPLETED, false)
            putBoolean(KEY_ONBOARDING_PICKS_APPLIED, false)
            remove(KEY_ONBOARDING_SUBJECT)
            remove(KEY_ONBOARDING_CHAPTER)
            remove(KEY_ONBOARDING_WORLD)
            putBoolean(KEY_HOME_TOUR_COMPLETED, false)
            putBoolean(KEY_NAV_TOUR_COMPLETED, false)
        }
    }

    /** Debug-only: keep quest progress at 3/3 + 1/1 until claims are tested (survives refreshTodayQuest). */
    fun setQuestAdTestOverrideDate(questDate: String) {
        prefs.edit { putString(KEY_QUEST_AD_TEST_OVERRIDE_DATE, questDate) }
    }

    fun isQuestAdTestOverrideActive(questDate: String): Boolean =
        prefs.getString(KEY_QUEST_AD_TEST_OVERRIDE_DATE, null) == questDate

    fun clearQuestAdTestOverride() {
        prefs.edit { remove(KEY_QUEST_AD_TEST_OVERRIDE_DATE) }
    }

    fun isExamPlanUserConfigured(): Boolean =
        prefs.getBoolean(KEY_EXAM_PLAN_USER_CONFIGURED, false)

    fun setExamPlanUserConfigured(configured: Boolean) {
        prefs.edit { putBoolean(KEY_EXAM_PLAN_USER_CONFIGURED, configured) }
    }

    fun setOpenExamPlanSetupPending(pending: Boolean = true) {
        prefs.edit { putBoolean(KEY_OPEN_EXAM_PLAN_SETUP_PENDING, pending) }
    }

    fun consumeOpenExamPlanSetupPending(): Boolean {
        val pending = prefs.getBoolean(KEY_OPEN_EXAM_PLAN_SETUP_PENDING, false)
        if (pending) {
            prefs.edit { remove(KEY_OPEN_EXAM_PLAN_SETUP_PENDING) }
        }
        return pending
    }

    fun getTrialCompletionsSinceMandatoryAd(studentId: String): Int {
        if (studentId.isBlank()) return 0
        return prefs.getInt("$KEY_TRIAL_COMPLETIONS_SINCE_AD_PREFIX$studentId", 0)
    }

    fun setTrialCompletionsSinceMandatoryAd(studentId: String, count: Int) {
        if (studentId.isBlank()) return
        prefs.edit { putInt("$KEY_TRIAL_COMPLETIONS_SINCE_AD_PREFIX$studentId", count.coerceAtLeast(0)) }
    }

    fun getLastCountedTrialItemId(studentId: String): Long {
        if (studentId.isBlank()) return -1L
        return prefs.getLong("$KEY_LAST_COUNTED_TRIAL_ITEM_PREFIX$studentId", -1L)
    }

    fun setLastCountedTrialItemId(studentId: String, trialItemId: Long) {
        if (studentId.isBlank()) return
        prefs.edit { putLong("$KEY_LAST_COUNTED_TRIAL_ITEM_PREFIX$studentId", trialItemId) }
    }

    /** Delta cursor for progress restore — highest `updatedAt` already applied, per user. */
    fun getProgressLastSync(userId: String): Long {
        if (userId.isBlank()) return 0L
        return prefs.getLong("$KEY_PROGRESS_LAST_SYNC_PREFIX$userId", 0L)
    }

    fun setProgressLastSync(userId: String, updatedAt: Long) {
        if (userId.isBlank()) return
        prefs.edit { putLong("$KEY_PROGRESS_LAST_SYNC_PREFIX$userId", updatedAt) }
    }

    /** Delta cursor for chapter-agent-progress restore — highest `updatedAt` applied, per user. */
    fun getChapterProgressLastSync(userId: String): Long {
        if (userId.isBlank()) return 0L
        return prefs.getLong("$KEY_CHAPTER_PROGRESS_LAST_SYNC_PREFIX$userId", 0L)
    }

    fun setChapterProgressLastSync(userId: String, updatedAt: Long) {
        if (userId.isBlank()) return
        prefs.edit { putLong("$KEY_CHAPTER_PROGRESS_LAST_SYNC_PREFIX$userId", updatedAt) }
    }

    /** Last time the Concept catalog was fully pulled (epoch ms), for the content-refresh gate. */
    fun getContentLastPull(): Long = prefs.getLong(KEY_CONTENT_LAST_PULL, 0L)

    fun setContentLastPull(ts: Long) {
        prefs.edit { putLong(KEY_CONTENT_LAST_PULL, ts) }
    }

    fun getLastGardenCelebrationPlantTotal(studentId: String): Int {
        if (studentId.isBlank()) return 0
        return prefs.getInt("$KEY_GARDEN_CELEBRATION_PLANT_TOTAL_PREFIX$studentId", 0)
    }

    fun setLastGardenCelebrationPlantTotal(studentId: String, totalPlanted: Int) {
        if (studentId.isBlank()) return
        prefs.edit {
            putInt(
                "$KEY_GARDEN_CELEBRATION_PLANT_TOTAL_PREFIX$studentId",
                totalPlanted.coerceAtLeast(0),
            )
        }
    }

    fun getLastHomeGardenPlantTotal(studentId: String): Int {
        if (studentId.isBlank()) return 0
        return prefs.getInt("$KEY_HOME_GARDEN_PLANT_TOTAL_PREFIX$studentId", 0)
    }

    fun setLastHomeGardenPlantTotal(studentId: String, totalPlanted: Int) {
        if (studentId.isBlank()) return
        prefs.edit {
            putInt(
                "$KEY_HOME_GARDEN_PLANT_TOTAL_PREFIX$studentId",
                totalPlanted.coerceAtLeast(0),
            )
        }
    }

    /**
     * First-run onboarding gate. The intro slides + subject/chapter/world picks are shown once, then
     * this is set and the app goes straight to home on every subsequent launch.
     */
    fun hasCompletedFirstRun(): Boolean =
        prefs.getBoolean(KEY_FIRST_RUN_COMPLETED, false)

    /** Persists the onboarding picks and marks first-run complete. */
    fun setFirstRunResult(subject: String, chapter: String, world: String) {
        prefs.edit {
            putString(KEY_ONBOARDING_SUBJECT, subject)
            putString(KEY_ONBOARDING_CHAPTER, chapter)
            putString(KEY_ONBOARDING_WORLD, world)
            putBoolean(KEY_FIRST_RUN_COMPLETED, true)
        }
    }

    fun getOnboardingSubject(): String? = prefs.getString(KEY_ONBOARDING_SUBJECT, null)

    fun getOnboardingChapter(): String? = prefs.getString(KEY_ONBOARDING_CHAPTER, null)

    /** "Garden" or "Space" — the reward world the student picked at first run. */
    fun getOnboardingWorld(): String? = prefs.getString(KEY_ONBOARDING_WORLD, null)

    /** Whether the onboarding picks (subject selection + garden theme) have been applied once. */
    fun hasAppliedOnboardingPicks(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_PICKS_APPLIED, false)

    fun setOnboardingPicksApplied() {
        prefs.edit { putBoolean(KEY_ONBOARDING_PICKS_APPLIED, true) }
    }

    /** First-run home spotlight tour — shown once, after onboarding, over the real home rails. */
    fun hasCompletedHomeTour(): Boolean =
        prefs.getBoolean(KEY_HOME_TOUR_COMPLETED, false)

    fun setHomeTourCompleted() {
        prefs.edit { putBoolean(KEY_HOME_TOUR_COMPLETED, true) }
    }

    /** Second tour phase: spotlighting the exam-planner and avatar/garden tabs in the bottom bar. */
    fun hasCompletedNavTour(): Boolean =
        prefs.getBoolean(KEY_NAV_TOUR_COMPLETED, false)

    fun setNavTourCompleted() {
        prefs.edit { putBoolean(KEY_NAV_TOUR_COMPLETED, true) }
    }

    /** Gates the gentle first-open-of-the-day streak celebration to once per calendar day. */
    fun wasStreakGreetingShownToday(): Boolean =
        prefs.getString(KEY_STREAK_GREETING_DAY, null) == java.time.LocalDate.now().toString()

    fun setStreakGreetingShownToday() {
        prefs.edit { putString(KEY_STREAK_GREETING_DAY, java.time.LocalDate.now().toString()) }
    }

    fun hasSeenGardenStarterPlantHighlight(studentId: String): Boolean {
        if (studentId.isBlank()) return true
        return prefs.getBoolean("$KEY_GARDEN_STARTER_HIGHLIGHT_SEEN_PREFIX$studentId", false)
    }

    fun setGardenStarterPlantHighlightSeen(studentId: String) {
        if (studentId.isBlank()) return
        prefs.edit { putBoolean("$KEY_GARDEN_STARTER_HIGHLIGHT_SEEN_PREFIX$studentId", true) }
    }

    override fun getTrialMaterializerVersion(): Int =
        prefs.getInt(KEY_TRIAL_MATERIALIZER_VERSION, 0)

    override fun setTrialMaterializerVersion(version: Int) {
        prefs.edit { putInt(KEY_TRIAL_MATERIALIZER_VERSION, version) }
    }

    override fun getAppDataMigrationVersion(): Int =
        prefs.getInt(KEY_APP_DATA_MIGRATION_VERSION, 0)

    override fun setAppDataMigrationVersion(version: Int) {
        prefs.edit { putInt(KEY_APP_DATA_MIGRATION_VERSION, version.coerceAtLeast(0)) }
    }

    fun hasDeclinedNotificationPrimer(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATION_PRIMER_DECLINED, false)

    fun setDeclinedNotificationPrimer(declined: Boolean = true) {
        prefs.edit { putBoolean(KEY_NOTIFICATION_PRIMER_DECLINED, declined) }
    }

    /** How many times the notification primer has been shown to the student (capped at 3). */
    fun getNotificationPrimerShowCount(): Int =
        prefs.getInt(KEY_NOTIFICATION_PRIMER_SHOW_COUNT, 0)

    fun incrementNotificationPrimerShowCount() {
        prefs.edit {
            putInt(KEY_NOTIFICATION_PRIMER_SHOW_COUNT, getNotificationPrimerShowCount() + 1)
        }
    }

    /** Whether the primer has already been shown today — gates it to at most once per calendar day. */
    fun wasNotificationPrimerShownToday(): Boolean =
        prefs.getString(KEY_NOTIFICATION_PRIMER_LAST_SHOWN_DAY, null) == java.time.LocalDate.now().toString()

    fun setNotificationPrimerShownToday() {
        prefs.edit { putString(KEY_NOTIFICATION_PRIMER_LAST_SHOWN_DAY, java.time.LocalDate.now().toString()) }
    }

    /** In-app rating flow: set once the student has rated on Play or sent feedback — then never ask again. */
    fun hasCompletedRatingFlow(): Boolean =
        prefs.getBoolean(KEY_RATING_FLOW_COMPLETED, false)

    fun setRatingFlowCompleted() {
        prefs.edit { putBoolean(KEY_RATING_FLOW_COMPLETED, true) }
    }

    fun getRatingPromptShowCount(): Int =
        prefs.getInt(KEY_RATING_PROMPT_SHOW_COUNT, 0)

    fun incrementRatingPromptShowCount() {
        prefs.edit { putInt(KEY_RATING_PROMPT_SHOW_COUNT, getRatingPromptShowCount() + 1) }
    }

    fun wasRatingPromptShownToday(): Boolean =
        prefs.getString(KEY_RATING_PROMPT_LAST_SHOWN_DAY, null) == java.time.LocalDate.now().toString()

    fun setRatingPromptShownToday() {
        prefs.edit { putString(KEY_RATING_PROMPT_LAST_SHOWN_DAY, java.time.LocalDate.now().toString()) }
    }

    /** True once the student has finished at least one trial task — gates the first-return review ask. */
    fun hasCompletedAnyTask(): Boolean =
        prefs.getBoolean(KEY_HAS_COMPLETED_ANY_TASK, false)

    fun setHasCompletedAnyTask() {
        prefs.edit { putBoolean(KEY_HAS_COMPLETED_ANY_TASK, true) }
    }

    fun hasAskedNotificationPermission(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, false)

    fun setAskedNotificationPermission(asked: Boolean = true) {
        prefs.edit { putBoolean(KEY_NOTIFICATION_PERMISSION_ASKED, asked) }
    }

    fun areNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun getReminderHour(): Int = prefs.getInt(KEY_REMINDER_HOUR, 17)

    fun getReminderMinute(): Int = prefs.getInt(KEY_REMINDER_MINUTE, 0)

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit {
            putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
            putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
        }
    }

    /** 0 = off, 1 = gentle (1/day), 2 = standard (up to 3/day). */
    fun getReminderMode(): Int = prefs.getInt(KEY_REMINDER_MODE, 2)

    fun setReminderMode(mode: Int) {
        prefs.edit { putInt(KEY_REMINDER_MODE, mode.coerceIn(0, 2)) }
    }

    fun getQuietHoursStart(): Int = prefs.getInt(KEY_QUIET_HOURS_START, 20)

    fun getQuietHoursEnd(): Int = prefs.getInt(KEY_QUIET_HOURS_END, 8)

    fun setQuietHours(startHour: Int, endHour: Int) {
        prefs.edit {
            putInt(KEY_QUIET_HOURS_START, startHour.coerceIn(0, 23))
            putInt(KEY_QUIET_HOURS_END, endHour.coerceIn(0, 23))
        }
    }

    fun isNotificationCategoryEnabled(category: String): Boolean =
        when (category) {
            "streaks" -> prefs.getBoolean(KEY_CATEGORY_STREAKS, true)
            "quests" -> prefs.getBoolean(KEY_CATEGORY_QUESTS, true)
            "reminders" -> prefs.getBoolean(KEY_CATEGORY_REMINDERS, true)
            "avatar" -> prefs.getBoolean(KEY_CATEGORY_AVATAR, true)
            else -> true
        }

    fun setNotificationCategoryEnabled(category: String, enabled: Boolean) {
        val key =
            when (category) {
                "streaks" -> KEY_CATEGORY_STREAKS
                "quests" -> KEY_CATEGORY_QUESTS
                "reminders" -> KEY_CATEGORY_REMINDERS
                "avatar" -> KEY_CATEGORY_AVATAR
                else -> return
            }
        prefs.edit { putBoolean(key, enabled) }
    }
}