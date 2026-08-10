package com.ncert7.aitutorandlab.ui.gamification.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
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

    /**
     * Drain a pending reward. The blocking XP/gems overlay is no longer shown, but this still runs so
     * XP/gems (awarded upstream) are acknowledged and a meaningful-win notification primer can fire.
     */
    fun dismissReward() {
        _pendingReward.value = null
        // The gate chooses the persuasion angle by show-count and ignores the variant passed here,
        // so we pass a placeholder. It is throttled to once/day, max 3 times, and stops after the
        // permission has been asked or granted.
        NotificationPermissionGate.onMeaningfulWin(
            context = appContext,
            prefs = sharedPreferenceUtils,
            variant = NotificationPrimerVariant.STREAK,
        )
    }
}
