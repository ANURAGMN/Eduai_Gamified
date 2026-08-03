package com.ncert7.aitutorandlab.ui.screens.setting.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anurag.eduai.uikit.avatar.AvatarPreset
import com.anurag.eduai.uikit.avatar.TutorConfig
import com.ncert7.aitutorandlab.repository.TutorConfigRepository
import com.ncert7.aitutorandlab.service.ads.RewardedAdManager
import com.ncert7.aitutorandlab.service.analytics.AdPlacement
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TutorConfigViewModel @Inject constructor(
    private val tutorConfigRepository: TutorConfigRepository,
    private val rewardedAdManager: RewardedAdManager,
    @ApplicationContext private val context: Context,
    val userId: String,
) : ViewModel() {

    init {
        rewardedAdManager.preload()
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                tutorConfigRepository.ensureLoaded(context, userId)
            }
        }
    }

    fun applyPreset(preset: AvatarPreset) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            tutorConfigRepository.save(context, userId, preset.config, presetId = preset.id)
        }
    }

    fun saveConfig(config: TutorConfig, presetId: String? = null) {
        viewModelScope.launch {
            tutorConfigRepository.save(context, userId, config, presetId = presetId)
        }
    }

    suspend fun showAvatarRewardedAds(
        activity: Activity,
        sessionId: String,
        totalAds: Int,
    ): Boolean {
        val placement =
            when {
                sessionId == "save_custom" -> AdPlacement.AVATAR_SAVE
                sessionId.startsWith("unlock_") -> AdPlacement.AVATAR_UNLOCK
                else -> AdPlacement.AVATAR_SAVE
            }
        if (!rewardedAdManager.isReady()) {
            rewardedAdManager.preload()
        }
        return rewardedAdManager.showRewardedSequence(activity, totalAds, placement)
    }
}
