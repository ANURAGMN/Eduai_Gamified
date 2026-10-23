package com.ncert7.aitutorandlab.ui.screens.doubts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.remote.AgenticAIClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoubtsUiState(
    val isLoading: Boolean = false,
    val answer: String? = null,
    val error: String? = null,
)

@HiltViewModel
class DoubtsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiClient: AgenticAIClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoubtsUiState())
    val uiState: StateFlow<DoubtsUiState> = _uiState.asStateFlow()

    fun askDoubt(question: String, subjectName: String, chapterName: String) {
        if (question.isBlank()) return

        viewModelScope.launch {
            _uiState.value = DoubtsUiState(isLoading = true)

            val contextStr =
                "Subject: $subjectName, Chapter: $chapterName. The user is a 7th-grade student asking about this topic."

            val result = aiClient.askGeneralDoubt(question, contextStr)

            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null && response.success && response.answer != null) {
                    _uiState.value = DoubtsUiState(answer = response.answer)
                } else {
                    _uiState.value = DoubtsUiState(
                        error = context.getString(R.string.ask_ai_doubt_failed),
                    )
                }
            } else {
                _uiState.value = DoubtsUiState(
                    error = result.exceptionOrNull()?.message
                        ?: context.getString(R.string.ask_ai_doubt_offline),
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = DoubtsUiState()
    }
}
