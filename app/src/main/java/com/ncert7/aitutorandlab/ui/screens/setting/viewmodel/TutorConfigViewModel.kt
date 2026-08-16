package com.ncert7.aitutorandlab.ui.screens.setting.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anurag.eduai.uikit.avatar.AvatarPreset
import com.anurag.eduai.uikit.avatar.TutorConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
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
    private val sharedPrefs: SharedPreferenceUtils,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** Prefer live prefs — Hilt's singleton `userId` can be blank if captured before login. */
    private fun currentUserId(): String =
        sharedPrefs.getUserId()?.takeIf { it.isNotBlank() }.orEmpty()

    init {
        rewardedAdManager.preload()
        viewModelScope.launch {
            val userId = currentUserId()
            if (userId.isNotBlank()) {
                tutorConfigRepository.ensureLoaded(context, userId)
            }
        }
    }

    fun applyPreset(preset: AvatarPreset) {
        val userId = currentUserId()
        if (userId.isBlank()) return
        viewModelScope.launch {
            tutorConfigRepository.save(context, userId, preset.config, presetId = preset.id)
        }
    }

    fun saveConfig(config: TutorConfig, presetId: String? = null) {
        viewModelScope.launch {
            tutorConfigRepository.save(context, currentUserId(), config, presetId = presetId)
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
