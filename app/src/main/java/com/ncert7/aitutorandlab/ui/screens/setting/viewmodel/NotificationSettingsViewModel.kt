package com.ncert7.aitutorandlab.ui.screens.setting.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.notification.NotificationCategory
import com.ncert7.aitutorandlab.notification.NotificationPermissionHelper
import com.ncert7.aitutorandlab.notification.NotificationReminderMode
import com.ncert7.aitutorandlab.notification.NotificationSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NotificationSettingsUiState(
    val masterEnabled: Boolean = true,
    val osPermissionGranted: Boolean = true,
    val showSystemSettingsFallback: Boolean = false,
    val reminderHour: Int = 17,
    val reminderMinute: Int = 0,
    val reminderMode: NotificationReminderMode = NotificationReminderMode.STANDARD,
    val quietHoursStart: Int = 20,
    val quietHoursEnd: Int = 8,
    val categoryEnabled: Map<NotificationCategory, Boolean> = emptyMap(),
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsStore: NotificationSettingsStore,
    private val sharedPref: SharedPreferenceUtils,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        refreshFromStorage()
    }

    fun refreshFromStorage() {
        val osGranted = NotificationPermissionHelper.hasPostNotificationsPermission(appContext)
        val askedBefore = sharedPref.hasAskedNotificationPermission()
        _uiState.value =
            NotificationSettingsUiState(
                masterEnabled = settingsStore.isMasterEnabled(),
                osPermissionGranted = osGranted,
                showSystemSettingsFallback =
                    NotificationPermissionHelper.shouldShowSettingsFallback(appContext, askedBefore),
                reminderHour = settingsStore.reminderHour(),
                reminderMinute = settingsStore.reminderMinute(),
                reminderMode = settingsStore.reminderMode(),
                quietHoursStart = settingsStore.quietHoursStart(),
                quietHoursEnd = settingsStore.quietHoursEnd(),
                categoryEnabled =
                    NotificationCategory.entries
                        .filter { it != NotificationCategory.LEAGUES_SOCIAL }
                        .associateWith { settingsStore.isCategoryEnabled(it) },
            )
    }

    fun onMasterToggle(enabled: Boolean) {
        settingsStore.setMasterEnabled(enabled)
        _uiState.update { it.copy(masterEnabled = enabled) }
    }

    fun onReminderTimeChanged(hour: Int, minute: Int) {
        settingsStore.setReminderTime(hour, minute)
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    }

    fun onReminderModeChanged(mode: NotificationReminderMode) {
        settingsStore.setReminderMode(mode)
        _uiState.update { it.copy(reminderMode = mode) }
    }

    fun onQuietHoursChanged(startHour: Int, endHour: Int) {
        settingsStore.setQuietHours(startHour, endHour)
        _uiState.update { it.copy(quietHoursStart = startHour, quietHoursEnd = endHour) }
    }

    fun onCategoryToggle(category: NotificationCategory, enabled: Boolean) {
        settingsStore.setCategoryEnabled(category, enabled)
        _uiState.update { state ->
            state.copy(categoryEnabled = state.categoryEnabled + (category to enabled))
        }
    }

    fun onPermissionGranted() {
        sharedPref.setAskedNotificationPermission(true)
        settingsStore.setMasterEnabled(true)
        refreshFromStorage()
    }
}
