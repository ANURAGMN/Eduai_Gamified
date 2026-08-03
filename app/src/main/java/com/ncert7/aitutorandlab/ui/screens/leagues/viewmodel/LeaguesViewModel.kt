package com.ncert7.aitutorandlab.ui.screens.leagues.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anurag.eduai.uikit.screens.LeagueUiState
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.repository.LeagueRepository
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.ui.screens.leagues.LeagueUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaguesViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository,
    private val streakRepository: StreakRepository,
    private val studentDao: StudentDao,
    private val sharedPrefs: SharedPreferenceUtils,
) : ViewModel() {

    private val userId: String
        get() = sharedPrefs.getUserId().orEmpty()

    private val _uiState = MutableStateFlow(LeagueUiState(participants = emptyList()))
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            if (userId.isBlank()) {
                _isLoading.value = false
                return@launch
            }
            refreshBoard()
            val displayName = studentDao.getStudentSync(userId)?.studentName.orEmpty()
            val streak = streakRepository.getStreakFlow(userId).first()?.streakCount ?: 1
            leagueRepository.observeBoardState(userId, displayName, streak).collectLatest { board ->
                if (board != null) {
                    _uiState.value = LeagueUiMapper.toUiState(board)
                }
                _isLoading.value = false
            }
        }
    }

    fun refreshBoard() {
        viewModelScope.launch {
            if (userId.isBlank()) return@launch
            val student = studentDao.getStudentSync(userId)
            val streak = streakRepository.getStreakFlow(userId).first()?.streakCount ?: 1
            leagueRepository.refreshLeagueBoard(
                studentId = userId,
                displayName = student?.studentName.orEmpty(),
                streak = streak,
            )
        }
    }
}
