package com.ncert7.aitutorandlab.ui.screens.friends.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.domain.gamification.FriendAddResult
import com.ncert7.aitutorandlab.repository.FriendRepository
import com.ncert7.aitutorandlab.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val gamificationRepository: GamificationRepository,
    private val sharedPrefs: SharedPreferenceUtils,
) : ViewModel() {

    private val userId: String
        get() = sharedPrefs.getUserId().orEmpty()

    private val _myFriendCode = MutableStateFlow("")
    val myFriendCode: StateFlow<String> = _myFriendCode.asStateFlow()

    private val _friendCount = MutableStateFlow(0)
    val friendCount: StateFlow<Int> = _friendCount.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            if (userId.isBlank()) return@launch
            gamificationRepository.getOrCreateProfile(userId)
            friendRepository.syncFriendCodeToRemote(userId)
            friendRepository.syncFriendSocialData(userId)
            friendRepository.markHomeFeedSeen(userId)
            _myFriendCode.value = friendRepository.getMyFriendCode(userId)
            friendRepository.observeFriendCount(userId).collectLatest { count ->
                _friendCount.value = count
            }
        }
    }

    fun addFriend(rawCode: String) {
        viewModelScope.launch {
            if (userId.isBlank()) return@launch
            val result = friendRepository.addFriendByCode(userId, rawCode)
            if (result == FriendAddResult.SUCCESS) {
                friendRepository.syncFriendSocialData(userId)
            }
            _statusMessage.value =
                when (result) {
                    FriendAddResult.SUCCESS -> "Friend added!"
                    FriendAddResult.INVALID_CODE -> "Enter a valid friend code."
                    FriendAddResult.SELF_ADD -> "You can't add your own code."
                    FriendAddResult.ALREADY_FRIENDS -> "You're already friends."
                    FriendAddResult.NOT_FOUND -> "No student found with that code."
                    FriendAddResult.FAILED -> "Could not add friend. Try again."
                }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
