package com.ncert7.aitutorandlab.ui.screens.login.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.firebase.model.User
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.entities.StudentEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.repository.StudentLocalRepository
import com.ncert7.aitutorandlab.repository.TutorConfigRepository
import com.ncert7.aitutorandlab.repository.UserCheckResult
import com.ncert7.aitutorandlab.service.auth.FirebaseAuthBridge
import com.ncert7.aitutorandlab.service.auth.PadaamsEmailAuth
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import com.ncert7.aitutorandlab.service.sync.FirebaseSyncManager
import com.ncert7.aitutorandlab.utils.LanguageHelper
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    private val streakRepository: StreakRepository,
    private val studentLocalRepository: StudentLocalRepository,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val tutorConfigRepository: TutorConfigRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _user = MutableStateFlow(User())
    val user = _user.asStateFlow()

    private val _userSaveState = MutableStateFlow<UserSaveState>(UserSaveState.Idle)
    val userSaveState = _userSaveState.asStateFlow()

    private val _existingUserSyncState =
        MutableStateFlow<ExistingUserSyncState>(ExistingUserSyncState.Idle)
    val existingUserSyncState = _existingUserSyncState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        normalizeLanguageCode(getCurrentLanguageCode()),
    )
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        DebugLogger.debugLog("UserViewModel", "UserViewModel initialized")
    }

    /**
     * Returns true only when login prefs match a student row in the local database.
     * Clears stale prefs (e.g. cloud backup restored isLoggedIn without Room data).
     */
    suspend fun hasValidLocalSession(): Boolean {
        if (!sharedPreferenceUtils.isLoggedIn()) return false
        val userId = sharedPreferenceUtils.getUserId()
        if (userId.isNullOrBlank()) {
            sharedPreferenceUtils.clearAllUserData()
            return false
        }
        val student = studentLocalRepository.getStudentSync(userId)
        if (student == null) {
            DebugLogger.warnLog(
                "UserViewModel",
                "Stale session: isLoggedIn=true but no local student for $userId — clearing prefs"
            )
            sharedPreferenceUtils.clearAllUserData()
            sharedPreferenceUtils.clearAllAuthData()
            return false
        }
        return true
    }

    fun updateId(id: String) {
        _user.value = _user.value.copy(id = id)
    }

    fun updateName(name: String?) {
        _user.value = _user.value.copy(displayName = name)
    }

    fun updateEmail(email: String) {
        _user.value = _user.value.copy(email = email)
    }

    fun updateProfilePictureUri(uri: String?) {
        _user.value = _user.value.copy(profilePictureUri = uri)
    }

    fun updateSchool(school: String) {
        _user.value = _user.value.copy(schoolName = school)
    }

    fun updatePhoneNumber(phone: String) {
        _user.value = _user.value.copy(phoneNumber = phone)
    }

    fun updateClass(stdClass: Int) {
        _user.value = _user.value.copy(studentClass = stdClass)
    }

    fun updateLanguage(language: String) {
        _user.value = _user.value.copy(language = language)
    }

    fun updateCreatedAt(createdAt: Long) {
        _user.value = _user.value.copy(createdAt = createdAt)
    }

    fun updateUpdatedAt(updatedAt: Long) {
        _user.value = _user.value.copy(lastLogin = updatedAt)
    }

    /**
     * Convenience method to update the entire user object
     * Useful when receiving user data from Google Sign-In
     */
    fun updateUser(user: User) {
        _user.value = user
    }

    /**
     * Set language with language code (en, kn, etc.)
     * Updates UI state, saves to SharedPreferences, and applies to app
     */
    fun setLanguage(langCode: String) {
        val normalized = normalizeLanguageCode(langCode)
        _selectedLanguage.value = normalized
        sharedPreferenceUtils.setLanguagePreference(normalized)
        LanguageHelper.setLanguage(normalized)
    }

    /**
     * In-app sign-in for @padaams.in emails (no Google OAuth). Goes straight to home after sync.
     */
    fun signInWithPadaamsEmail(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            if (!PadaamsEmailAuth.validateCredentials(email, password)) {
                _loginState.value = LoginState.Error(IllegalArgumentException("Invalid credentials"))
                return@launch
            }

            try {
                FirebaseAuthBridge.signInWithEmailPassword(email.trim().lowercase(), password)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e)
                return@launch
            }

            val normalizedEmail = email.trim().lowercase()
            val language = _selectedLanguage.value
            _user.value = User(
                id = normalizedEmail,
                email = normalizedEmail,
                displayName = PadaamsEmailAuth.displayNameFor(normalizedEmail),
                language = language,
                studentClass = 7,
                appName = AppConfig.APP_NAME
            )
            _loginState.value = LoginState.Idle
            DebugLogger.debugLog("UserViewModel", "Padaams email sign-in: $normalizedEmail")
            saveExistingUserLocally(context)
        }
    }

    /**
     * Handle Google login flow
     * Checks if user exists in Firebase by email and appName
     * Updates login state accordingly
     */
    fun handleGoogleLogin(firebaseUser: User) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                withTimeout(LOGIN_FIRESTORE_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        // Best-effort; rules need auth_index but login must not hang on quota writes.
                        try {
                            repo.ensureAuthIndex(firebaseUser.id)
                        } catch (e: Exception) {
                            DebugLogger.warnLog("UserViewModel", "auth_index skipped: ${e.message}")
                        }

                        val currentLanguage = _selectedLanguage.value
                        updateLanguage(currentLanguage)

                        when (val result = repo.checkUserExists(firebaseUser.email, firebaseUser.id)) {
                            is UserCheckResult.Found -> {
                                _user.value = result.user
                                _loginState.value = LoginState.ExistingUser(result.user)
                                DebugLogger.debugLog("UserViewModel", "Existing user found - Email: ${firebaseUser.email}")
                            }

                            is UserCheckResult.NotFound -> {
                                _user.value = firebaseUser.copy(language = currentLanguage)
                                DebugLogger.debugLog("UserViewModel", "New user detected - Email: ${firebaseUser.email}")
                                _loginState.value = LoginState.NewUser
                            }

                            is UserCheckResult.Error -> {
                                _loginState.value = LoginState.Error(result.exception)
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _loginState.value = LoginState.Error(
                    Exception("Service busy, try again shortly.")
                )
                DebugLogger.errorLog("UserViewModel", "Login timed out waiting for Firestore")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e)
                DebugLogger.debugLog("UserViewModel", "Error during login: ${e.message}")
            }
        }
    }

    /**
     * Check if user exists - delegates to repository
     */
    suspend fun checkUserExists(email: String, studentId: String? = null): UserCheckResult {
        return repo.checkUserExists(email, studentId)
    }

    /**
     * Save existing user data locally and sync content
     * This is called when an existing user logs in
     */
    fun saveExistingUserLocally(context: Context) {
        viewModelScope.launch {
            _existingUserSyncState.value = ExistingUserSyncState.Syncing
            try {
                val currentUser = _user.value
                val db = EduAiDatabase.getInstance(context)
                val localRepo = StudentLocalRepository(db.studentDao())
                val sharedPreference = SharedPreferenceUtils(context)

                val preferredLanguage =
                    normalizeLanguageCode(
                        _selectedLanguage.value.ifBlank { currentUser.language },
                    )

                val studentEntity = StudentEntity(
                    studentId = currentUser.id,
                    studentName = currentUser.displayName.orEmpty(),
                    email = currentUser.email,
                    phoneNumber = currentUser.phoneNumber,
                    studentSchool = currentUser.schoolName,
                    language = preferredLanguage,
                    classLevel = currentUser.studentClass,
                    profilePhotoUrl = currentUser.profilePictureUri,
                    createdAt = currentUser.createdAt,
                    updatedAt = currentUser.lastLogin,
                    isSynced = true
                )
                localRepo.saveStudentLocally(studentEntity)

                sharedPreference.setLoggedIn(true)
                sharedPreference.setLanguagePreference(preferredLanguage)
                LanguageHelper.setLanguage(preferredLanguage)
                sharedPreference.setUserId(currentUser.id)

                tutorConfigRepository.ensureLoaded(appContext, currentUser.id)

                // Cloud restore is best-effort — never block home entry on Firestore quota.
                try {
                    withTimeout(LOGIN_SYNC_TIMEOUT_MS) {
                        withContext(Dispatchers.IO) {
                            try {
                                repo.ensureAuthIndex(currentUser.id)
                            } catch (e: Exception) {
                                DebugLogger.warnLog("UserViewModel", "auth_index on sync skipped: ${e.message}")
                            }

                            val syncManager = FirebaseSyncManager(
                                subjectDao = db.subjectDao(),
                                chapterDao = db.chapterDao(),
                                conceptDao = db.conceptDao(),
                                progressDao = db.progressDao(),
                                streakDao = db.streakDao(),
                                chapterProgressDao = db.chapterAgentProgressDao(),
                                context = context
                            )

                            val existingSubjects =
                                db.subjectDao().getSubjectsForClassSync(currentUser.studentClass)
                            if (existingSubjects.isEmpty()) {
                                val contentResult = syncManager.syncAllContent()
                                DebugLogger.debugLog("UserViewModel", "Content sync: ${contentResult.message}")
                            }

                            val progressResult = syncManager.syncUserProgress(currentUser.id)
                            DebugLogger.debugLog("UserViewModel", "Progress sync: ${progressResult.message}")

                            val chapterProgressResult =
                                syncManager.syncChapterAgentProgress(currentUser.id)
                            DebugLogger.debugLog(
                                "UserViewModel",
                                "Chapter progress sync: ${chapterProgressResult.message}"
                            )

                            streakRepository.syncStreakOnLogin(currentUser.id)
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    DebugLogger.warnLog(
                        "UserViewModel",
                        "Cloud sync timed out (${LOGIN_SYNC_TIMEOUT_MS}ms); continuing with local login"
                    )
                } catch (e: Exception) {
                    DebugLogger.warnLog("UserViewModel", "Cloud sync failed: ${e.message}")
                }

                DataSyncService.onUserAuthenticated(currentUser.id)
                DebugLogger.debugLog(
                    "UserViewModel",
                    "DataSyncService initialized with studentId: ${currentUser.id}"
                )

                _existingUserSyncState.value = ExistingUserSyncState.Success
            } catch (e: Exception) {
                DebugLogger.debugLog("UserViewModel", "Error saving user locally: ${e.message}")
                _existingUserSyncState.value = ExistingUserSyncState.Error(e)
            }
        }
    }

    /**
     * Submit new user data to Firebase and save locally
     * This is called when a new user completes registration
     */
    fun submitNewUser(context: Context) {
        viewModelScope.launch {
            _userSaveState.value = UserSaveState.Saving
            try {
                val currentUser = _user.value

                repo.ensureAuthIndex(currentUser.id)

                // Debug logging to verify user ID
                DebugLogger.debugLog("UserViewModel", "Submitting new user with ID: ${currentUser.id}")
                DebugLogger.debugLog("UserViewModel", "User email: ${currentUser.email}")
                DebugLogger.debugLog("UserViewModel", "User name: ${currentUser.displayName}")

                // Create user in Firebase
                val success = repo.createNewUser(currentUser)

                if (success) {
                    // Save to local database
                    val db = EduAiDatabase.getInstance(context)
                    val localRepo = StudentLocalRepository(db.studentDao())
                    val sharedPreference = SharedPreferenceUtils(context)

                    val studentEntity = StudentEntity(
                        studentId = currentUser.id,
                        studentName = currentUser.displayName.orEmpty(),
                        email = currentUser.email,
                        phoneNumber = currentUser.phoneNumber,
                        studentSchool = currentUser.schoolName,
                        language = currentUser.language,
                        classLevel = currentUser.studentClass,
                        profilePhotoUrl = currentUser.profilePictureUri,
                        createdAt = currentUser.createdAt,
                        updatedAt = currentUser.lastLogin,
                        isSynced = true
                    )
                    localRepo.saveStudentLocally(studentEntity)

                    // Sync content and restore any cloud progress (same as existing-user login)
                    val syncManager = FirebaseSyncManager(
                        subjectDao = db.subjectDao(),
                        chapterDao = db.chapterDao(),
                        conceptDao = db.conceptDao(),
                        progressDao = db.progressDao(),
                        streakDao = db.streakDao(),
                        chapterProgressDao = db.chapterAgentProgressDao(),
                        context = context
                    )
                    val contentResult = syncManager.syncAllContent()
                    DebugLogger.debugLog("UserViewModel", "Content sync: ${contentResult.message}")

                    val progressResult = syncManager.syncUserProgress(currentUser.id)
                    DebugLogger.debugLog("UserViewModel", "Progress sync: ${progressResult.message}")

                    val chapterProgressResult = syncManager.syncChapterAgentProgress(currentUser.id)
                    DebugLogger.debugLog("UserViewModel", "Chapter progress sync: ${chapterProgressResult.message}")

                    // Create initial streak for new user
                    DebugLogger.debugLog("UserViewModel", "Creating initial streak for new user")
                    streakRepository.syncStreakOnLogin(currentUser.id)
                    DebugLogger.debugLog("UserViewModel", "Initial streak created")

                    // Save preferences
                    sharedPreference.setLoggedIn(true)
                    sharedPreference.setLanguagePreference(currentUser.language)
                    sharedPreference.setUserId(currentUser.id)

                    tutorConfigRepository.ensureLoaded(appContext, currentUser.id)

                    // Initialize DataSyncService and sync pre-login funnel events
                    DataSyncService.onUserAuthenticated(currentUser.id)
                    DebugLogger.debugLog("UserViewModel", "DataSyncService initialized with studentId: ${currentUser.id}")

                    _userSaveState.value = UserSaveState.Success
                } else {
                    _userSaveState.value = UserSaveState.Error(Exception("Failed to create user"))
                }
            } catch (e: Exception) {
                _userSaveState.value = UserSaveState.Error(e)
                DebugLogger.debugLog("UserViewModel", "Error submitting user: ${e.message}")
            }
        }
    }

    /**
     * Reset login state to Idle
     * Useful when navigating away from login screens
     */
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * Reset user save state to Idle
     */
    fun resetUserSaveState() {
        _userSaveState.value = UserSaveState.Idle
    }

    /**
     * Reset existing user sync state to Idle
     */
    fun resetExistingUserSyncState() {
        _existingUserSyncState.value = ExistingUserSyncState.Idle
    }

    /**
     * Reset all user state after logout
     */
    fun resetUserState() {
        DebugLogger.debugLog("UserViewModel", "Resetting user state after logout")
        _loginState.value = LoginState.Idle
        _user.value = User()
        _userSaveState.value = UserSaveState.Idle
        _existingUserSyncState.value = ExistingUserSyncState.Idle
        _selectedLanguage.value = sharedPreferenceUtils.getLanguagePreference() ?: "en"
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class ExistingUser(val currentUser: User) : LoginState()
    object NewUser : LoginState()
    data class Error(val exception: Throwable) : LoginState()
}

sealed class UserSaveState {
    object Idle : UserSaveState()
    object Saving : UserSaveState()
    object Success : UserSaveState()
    data class Error(val exception: Throwable) : UserSaveState()
}

sealed class ExistingUserSyncState {
    object Idle : ExistingUserSyncState()
    object Syncing : ExistingUserSyncState()
    object Success : ExistingUserSyncState()
    data class Error(val exception: Throwable) : ExistingUserSyncState()
}

private const val LOGIN_FIRESTORE_TIMEOUT_MS = 25_000L
private const val LOGIN_SYNC_TIMEOUT_MS = 30_000L