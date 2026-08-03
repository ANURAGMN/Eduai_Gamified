package com.ncert7.aitutorandlab.ui.gamification.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.domain.gamification.RewardEventBus
import com.ncert7.aitutorandlab.domain.gamification.RewardUiEvent
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.notification.NotificationPermissionGate
import com.ncert7.aitutorandlab.notification.NotificationPrimerVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardOverlayViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val rewardEventBus: RewardEventBus,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val streakDao: StreakDao,
) : ViewModel() {

    private val _pendingReward = MutableStateFlow<RewardUiEvent?>(null)
    val pendingReward: StateFlow<RewardUiEvent?> = _pendingReward.asStateFlow()

    init {
        viewModelScope.launch {
            rewardEventBus.events.collect { event ->
                _pendingReward.value = event
            }
        }
    }

    fun dismissReward() {
        _pendingReward.value = null
        viewModelScope.launch {
            val userId = sharedPreferenceUtils.getUserId().orEmpty()
            val streakCount =
                if (userId.isNotBlank()) {
                    streakDao.getStreakByUserId(userId)?.streakCount ?: 0
                } else {
                    0
                }
            val variant =
                if (streakCount <= 1) {
                    NotificationPrimerVariant.STREAK
                } else {
                    NotificationPrimerVariant.QUEST
                }
            NotificationPermissionGate.onMeaningfulWin(
                context = appContext,
                prefs = sharedPreferenceUtils,
                variant = variant,
            )
        }
    }
}
