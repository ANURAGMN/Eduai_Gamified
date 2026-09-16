package com.ncert7.aitutorandlab.ui.screens.whiteboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.remote.AgenticAIClient
import com.ncert7.aitutorandlab.data.remote.WbApiTutorTurn
import com.ncert7.aitutorandlab.data.remote.WbConceptInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val role: String, val content: String, val isLatestAi: Boolean = false)

@HiltViewModel
class WhiteboardViewModel @Inject constructor(
    private val apiClient: AgenticAIClient,
    private val sharedPrefs: SharedPreferenceUtils // Added to fetch student ID
) : ViewModel() {

    private val _concepts = MutableStateFlow<List<WbConceptInfo>>(emptyList())
    val concepts = _concepts.asStateFlow()

    private val _conceptTitle = MutableStateFlow("Science Tutor")
    val conceptTitle = _conceptTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _currentThreadId = MutableStateFlow<String?>(null)

    private val _whiteboardData = MutableStateFlow<WhiteboardData?>(null)
    val whiteboardData = _whiteboardData.asStateFlow()

    private val _latestTurn = MutableStateFlow<WbApiTutorTurn?>(null)
    val latestTurn = _latestTurn.asStateFlow()

    fun fetchConcepts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = apiClient.getWhiteboardConcepts()
            if (result.isSuccess) {
                _concepts.value = result.getOrNull()!!.concepts
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load concepts."
            }
            _isLoading.value = false
        }
    }

    fun startSession(conceptId: String) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _chatHistory.value = emptyList()

            val studentId = sharedPrefs.getUserId() ?: "" // Safely grab student ID

            val result = apiClient.startWhiteboardSession(conceptId, studentId) // Pass it to the client
            if (result.isSuccess) {
                val body = result.getOrNull()!!
                _currentThreadId.value = body.threadId
                _conceptTitle.value = body.concept.title
                _latestTurn.value = body.turn
                _chatHistory.value = listOf(ChatMessage("ai", body.turn.message, isLatestAi = true))

                parseWhiteboardState(body.turn.whiteboardState?.objects)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to start session."
            }
            _isLoading.value = false
        }
    }

    fun sendMessage(userText: String) {
        val threadId = _currentThreadId.value ?: return
        if (userText.isBlank()) return

        if (_isLoading.value) return

        _errorMessage.value = null

        val updatedHistory = _chatHistory.value.map { it.copy(isLatestAi = false) }.toMutableList()
        updatedHistory.add(ChatMessage("user", userText))
        _chatHistory.value = updatedHistory

        viewModelScope.launch {
            _isLoading.value = true

            val result = apiClient.continueWhiteboardSession(threadId, userText)
            if (result.isSuccess) {
                val body = result.getOrNull()!!
                _latestTurn.value = body.turn

                val newHistory = _chatHistory.value.toMutableList()
                newHistory.add(ChatMessage("ai", body.turn.message, isLatestAi = true))
                _chatHistory.value = newHistory

                parseWhiteboardState(body.turn.whiteboardState?.objects)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to get response."
            }
            _isLoading.value = false
        }
    }

    private suspend fun parseWhiteboardState(objects: Map<String, com.ncert7.aitutorandlab.data.remote.WbBackendWhiteboardObject>?) {
        var flowchartSteps = emptyList<FlowchartStep>()
        var explanationCard: ExplanationCard? = null
        var rawSvgString: String? = null

        objects?.values?.forEach { obj ->
            val instanceId = obj.instance_id ?: ""
            when (obj.object_type_id) {
                "process_flow" -> {
                    flowchartSteps = obj.props?.steps?.mapIndexed { index, stepText ->
                        FlowchartStep(id = "$instanceId:step:$index", text = stepText)
                    } ?: emptyList()
                }
                "text_card" -> {
                    explanationCard = ExplanationCard(
                        id = "$instanceId:card",
                        title = obj.props?.title ?: "EXPLANATION",
                        text = obj.props?.text ?: ""
                    )
                }
                "scene_asset" -> {
                    val sceneId = obj.props?.scene_id
                    if (!sceneId.isNullOrEmpty()) {
                        rawSvgString = apiClient.fetchWhiteboardSvg(sceneId)
                    }
                }
            }
        }

        _whiteboardData.value = WhiteboardData(
            flowchartSteps = flowchartSteps,
            explanationCard = explanationCard,
            svgString = rawSvgString
        )
    }
}

data class WhiteboardData(
    val flowchartSteps: List<FlowchartStep>,
    val explanationCard: ExplanationCard?,
    val svgString: String?
)

data class FlowchartStep(val id: String, val text: String)
data class ExplanationCard(val id: String, val title: String, val text: String)