package com.ncert7.aitutorandlab.ui.screens.friends.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.entities.FriendConnectionEntity
import com.ncert7.aitutorandlab.domain.gamification.FriendAddResult
import com.ncert7.aitutorandlab.repository.FriendRepository
import com.ncert7.aitutorandlab.repository.GamificationRepository
import com.ncert7.aitutorandlab.utils.FriendsCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
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
    private val friendFeedService: com.ncert7.aitutorandlab.domain.gamification.FriendFeedService,
    private val gamificationRepository: GamificationRepository,
    private val sharedPrefs: SharedPreferenceUtils,
) : ViewModel() {

    private val userId: String
        get() = sharedPrefs.getUserId().orEmpty()

    private val _myFriendCode = MutableStateFlow("")
    val myFriendCode: StateFlow<String> = _myFriendCode.asStateFlow()

    private val _friendCount = MutableStateFlow(0)
    val friendCount: StateFlow<Int> = _friendCount.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<FriendConnectionEntity>>(emptyList())
    val pendingRequests: StateFlow<List<FriendConnectionEntity>> = _pendingRequests.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            if (userId.isBlank()) return@launch
            gamificationRepository.getOrCreateProfile(userId)
            friendRepository.seedDemoFriendRequestsIfNeeded(userId)
            friendFeedService.simulateBotFriendFeedIfNeeded(userId)
            friendRepository.syncFriendCodeToRemote(userId)
            friendRepository.syncFriendSocialData(userId)
            friendRepository.markHomeFeedSeen(userId)
            _myFriendCode.value = friendRepository.getMyFriendCode(userId)
            friendRepository.observeFriendCount(userId).collectLatest { count ->
                _friendCount.value = count
            }
        }
        viewModelScope.launch {
            if (userId.isBlank()) return@launch
            friendRepository.observePendingRequests(userId).collectLatest { pending ->
                _pendingRequests.value = pending
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
            _statusMessage.value = FriendsCopy.addResultMessage(getCurrentLanguageCode(), result)
        }
    }

    fun acceptRequest(friendStudentId: String) {
        viewModelScope.launch {
            if (userId.isBlank()) return@launch
            val ok = friendRepository.acceptFriendRequest(userId, friendStudentId)
            if (ok) {
                friendRepository.getAcceptedDemoBots(userId)
                    .firstOrNull { it.friendStudentId == friendStudentId }
                    ?.let { bot ->
                        friendFeedService.seedFeedFromAcceptedBot(
                            ownerStudentId = userId,
                            botId = bot.friendStudentId,
                            botName = bot.displayName,
                        )
                    }
            }
            _statusMessage.value = FriendsCopy.acceptResultMessage(getCurrentLanguageCode(), ok)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
