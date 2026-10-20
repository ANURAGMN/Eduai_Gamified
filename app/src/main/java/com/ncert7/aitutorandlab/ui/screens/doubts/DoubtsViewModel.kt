package com.ncert7.aitutorandlab.ui.screens.doubts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.remote.AgenticAIClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoubtsUiState(
    val isLoading: Boolean = false,
    val answer: String? = null,
    val error: String? = null
)

@HiltViewModel
class DoubtsViewModel @Inject constructor(
    private val aiClient: AgenticAIClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoubtsUiState())
    val uiState: StateFlow<DoubtsUiState> = _uiState.asStateFlow()

    fun askDoubt(question: String, subjectName: String, chapterName: String) {
        if (question.isBlank()) return

        viewModelScope.launch {
            _uiState.value = DoubtsUiState(isLoading = true)

            // The context is generated programmatically based on where the user currently is
            val contextStr = "Subject: $subjectName, Chapter: $chapterName. The user is a 7th-grade student asking about this topic."

            val result = aiClient.askGeneralDoubt(question, contextStr)

            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null && response.success && response.answer != null) {
                    _uiState.value = DoubtsUiState(answer = response.answer)
                } else {
                    _uiState.value = DoubtsUiState(error = "Failed to get a clear answer. Please try again.")
                }
            } else {
                // Extracts the safely caught 500/503/IOException from callWithRetry
                _uiState.value = DoubtsUiState(error = result.exceptionOrNull()?.message ?: "Unable to connect to the server.")
            }
        }
    }

    fun resetState() {
        _uiState.value = DoubtsUiState()
    }
}