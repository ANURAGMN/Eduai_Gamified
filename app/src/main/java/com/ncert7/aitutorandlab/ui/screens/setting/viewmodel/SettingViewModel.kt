package com.ncert7.aitutorandlab.ui.screens.setting.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.ConceptSessionRepository
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.entities.StudentEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.repository.NetworkException
import com.ncert7.aitutorandlab.repository.QuestRepository
import com.ncert7.aitutorandlab.notification.NotificationOrchestrator
import com.ncert7.aitutorandlab.notification.NotificationType
import com.ncert7.aitutorandlab.utils.LanguageHelper
import com.ncert7.aitutorandlab.utils.TokenManager
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed class UpdateProfileState {
    object Idle : UpdateProfileState()
    object Loading : UpdateProfileState()
    object Success : UpdateProfileState()
    data class Error(val message: String) : UpdateProfileState()
}

sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}
@HiltViewModel
class SettingViewModel @Inject constructor(
    private val sharedPref: SharedPreferenceUtils,
    private val repository: FirebaseRepository,
    private val studentDao: StudentDao,
    private val questRepository: QuestRepository,
    private val notificationOrchestrator: NotificationOrchestrator,
    @ApplicationContext private val context: Context,
    val userId: String
) : ViewModel() {
    
    private val _student = MutableStateFlow<StudentEntity?>(null)
    val student: StateFlow<StudentEntity?> = _student.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateState: StateFlow<UpdateProfileState> = _updateState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        normalizeLanguageCode(getCurrentLanguageCode()),
    )
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(
        LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    init {
        // Load student profile
        loadStudent()
    }

    private fun loadStudent() {
        viewModelScope.launch {
            val result = studentDao.getStudentSync(userId)
            _student.value = result
            _selectedLanguage.value = normalizeLanguageCode(getCurrentLanguageCode())
        }
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            val normalized = normalizeLanguageCode(langCode)
            _selectedLanguage.value = normalized
            sharedPref.setLanguagePreference(normalized)
            LanguageHelper.setLanguage(normalized)

            studentDao.getStudentSync(userId)?.let { existing ->
                val updated = existing.copy(language = normalized, isSynced = false)
                studentDao.updateStudent(updated)
                _student.value = updated
            }
        }
    }

    fun debugPrepareQuestAdTest(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val language = normalizeLanguageCode(sharedPref.getLanguagePreference() ?: "en")
            questRepository.debugPrepareAdClaimTest(userId, language)
            onDone()
        }
    }

    fun debugFireTestNotification(
        type: NotificationType,
        onResult: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            val message =
                when (val result = notificationOrchestrator.fireDebugTest(type)) {
                    is NotificationOrchestrator.DebugFireResult.Fired ->
                        "Test notification fired: ${result.typeId}"
                    NotificationOrchestrator.DebugFireResult.NoPermission ->
                        "Enable notifications in system settings first."
                    NotificationOrchestrator.DebugFireResult.NotLoggedIn ->
                        "Log in to test notifications."
                    NotificationOrchestrator.DebugFireResult.NotDebugBuild ->
                        "Debug builds only."
                }
            onResult(message)
        }
    }

    fun updateProfile(
        updatedName: String,
        updatedPhone: String,
        updatedSchool: String,
        updatedClass: Int
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateProfileState.Loading

            try {
                val existing = studentDao.getStudentSync(userId)
                if (existing == null) {
                    _updateState.value = UpdateProfileState.Error("User not found")
                    return@launch
                }

                val updatedStudent = existing.copy(
                    studentName = updatedName,
                    phoneNumber = updatedPhone,
                    studentSchool = updatedSchool,
                    classLevel = updatedClass,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )

                val firebaseSuccess = withTimeout(15_000) {
                    repository.updateUserProfile(
                        userId = existing.studentId,
                        name = updatedName,
                        phone = updatedPhone,
                        school = updatedSchool,
                        studentClass = updatedClass,
                        updatedAt = updatedStudent.updatedAt
                    )
                }

                if (firebaseSuccess) {
                    studentDao.updateStudent(updatedStudent.copy(isSynced = true))
                    _student.value = updatedStudent.copy(isSynced = true)
                    _updateState.value = UpdateProfileState.Success
                } else {
                    studentDao.updateStudent(updatedStudent)
                    _student.value = updatedStudent
                    _updateState.value = UpdateProfileState.Error("Failed to sync with server")
                }
            } catch (e: NetworkException) {
                _updateState.value = UpdateProfileState.Error(
                    e.message ?: "Network error. Please check your connection and try again."
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _updateState.value = UpdateProfileState.Error(
                    "Connection timed out. Please check your internet and try again."
                )
            } catch (e: Exception) {
                DebugLogger.errorLog("SettingViewModel", "Profile update failed: ${e.message}")
                _updateState.value = UpdateProfileState.Error(
                    e.message ?: "Failed to save profile. Please try again."
                )
            }
        }
    }

    fun updateProfilePhoto(photoUri: String) {
        viewModelScope.launch {
            try {
                val existing = studentDao.getStudentSync(userId) ?: return@launch
                val updated = existing.copy(
                    profilePhotoUrl = photoUri,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                studentDao.updateStudent(updated)
                _student.value = updated
            } catch (e: Exception) {
                DebugLogger.errorLog("SettingViewModel", "Profile photo update failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _updateState.value = UpdateProfileState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _logoutState.value = LogoutState.Loading
                DebugLogger.debugLog("SettingViewModel", "Starting logout process")

                // Clear student data
                studentDao.deleteAllStudents()
                DebugLogger.debugLog("SettingViewModel", "Cleared student data")

                // Clear shared preferences
                sharedPref.clearAllUserData()
                DebugLogger.debugLog("SettingViewModel", "Cleared user preferences")

                // Clear Google authentication tokens
                TokenManager.clearAllTokens(context)
                DebugLogger.debugLog("SettingViewModel", "Cleared authentication tokens")

                // Clear all session mappings for chatbot
                ConceptSessionRepository(context).clearAllMappings()
                DebugLogger.debugLog("SettingViewModel", "Cleared concept session mappings")

                // Clear all sessions from database
                val db = EduAiDatabase.getInstance(context)
                db.sessionDao().deleteAllSessions()
                DebugLogger.debugLog("SettingViewModel", "Cleared database sessions")

                _logoutState.value = LogoutState.Success
                DebugLogger.debugLog("SettingViewModel", "Logout completed successfully")

            } catch (e: Exception) {
                DebugLogger.errorLog("SettingViewModel", "Error during logout: ${e.message}")
                // Still set logout state even if there's an error
                _logoutState.value = LogoutState.Error(e.message ?: "Logout failed")
            }
        }
    }
}