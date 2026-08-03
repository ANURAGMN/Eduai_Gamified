package com.ncert7.aitutorandlab.domain.chatbot.usecase

import android.content.Context
import com.ncert7.aitutorandlab.data.local.ConceptSessionRepository
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.remote.AgenticAIClient
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.model.SessionResult
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.dataclass.ChatMessageModel
import com.ncert7.aitutorandlab.utils.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


class SessionUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val agenticAIClient: AgenticAIClient,
    private val sharedPrefs: SharedPreferenceUtils
) {
    private val conceptThreadMap = mutableMapOf<String, String>()
    private val conceptSessionMap = mutableMapOf<String, String>()

    suspend fun startSession(
        concept: String,
        userId: String,
        isKannada: Boolean,
        studentLevel: String
    ): SessionResult {
        return try {
            val result = agenticAIClient.startSession(
                conceptTitle = concept,
                studentId = userId,
                isKannada = isKannada,
                studentLevel = studentLevel
            )

            if (result.isSuccess) {
                val response = result.getOrNull() ?: return sessionFailure(null)
                if (!response.success) return sessionFailure(null, fallbackStatus = 500)

                saveThreadMapping( concept, response.threadId, response.sessionId)
                agenticAIClient.setCurrentThreadAndSession(response.threadId, response.sessionId)

                SessionResult(
                    success = true,
                    autosuggestions = response.autosuggestions,
                    agentResponse = response.agentResponse,
                    metadata = response.metadata,
                    currentState = response.currentState
                )
            } else {
                sessionFailure(result.exceptionOrNull())
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("SessionUseCase", "startSession error: ${e.message}")
            sessionFailure(e)
        }
    }

    suspend fun resumeSession(threadId: String, sessionId: String?): SessionResult {
        return try {
            DebugLogger.debugLog("SessionUseCase", "Resuming session - thread=$threadId")
            agenticAIClient.setCurrentThreadAndSession(threadId, sessionId)

            val histResult = agenticAIClient.getSessionHistory(threadId)
            if (histResult.isSuccess) {
                val messages = histResult.getOrNull()?.messages ?: emptyList()
                val chatMessages = messages.mapNotNull { msg ->
                    val role = (msg["role"] as? String)?.lowercase() ?: return@mapNotNull null
                    val content = msg["content"] as? String ?: return@mapNotNull null
                    val sender = when (role) {
                        "assistant", "ai" -> "ai"
                        "user" -> "user"
                        else -> return@mapNotNull null
                    }
                    ChatMessageModel(
                        sender = sender,
                        content = content,
                        timestamp = (msg["timestamp"] as? Long) ?: System.currentTimeMillis(),
                    )
                }
                SessionResult(success = true, messages = chatMessages)
            } else {
                sessionFailure(histResult.exceptionOrNull())
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("SessionUseCase", "resumeSession error: ${e.message}")
            sessionFailure(e)
        }
    }

    suspend fun continueSession(
        userMessage: String,
        clickedAutosuggestion: Boolean,
        studentLevel: String,
        isKannada: Boolean
    ): SessionResult {
        return try {
            val response = agenticAIClient.continueSession(
                userMessage = userMessage,
                clickedAutosuggestion = clickedAutosuggestion,
                studentLevel = studentLevel,
                isKannada = isKannada
            )

            if (response.isSuccess) {
                val resp = response.getOrNull() ?: return sessionFailure(null)
                if (!resp.success) return sessionFailure(null, fallbackStatus = 500)
                SessionResult(
                    success = true,
                    autosuggestions = resp.autosuggestions,
                    agentResponse = resp.agentResponse,
                    metadata = resp.metadata,
                    currentState = resp.currentState
                )
            } else {
                sessionFailure(response.exceptionOrNull())
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("SessionUseCase", "continueSession error: ${e.message}")
            sessionFailure(e)
        }
    }

    suspend fun clearAllSessions() {
        try {
            conceptThreadMap.clear()
            conceptSessionMap.clear()
            withContext(Dispatchers.IO) {
                ConceptSessionRepository(context).clearAllMappings()
            }
            agenticAIClient.setCurrentThreadAndSession(null, null)
            DebugLogger.debugLog("SessionUseCase", "All sessions cleared")
        } catch (e: Exception) {
            DebugLogger.errorLog("SessionUseCase", "clearAllSessions failed: ${e.message}")
        }
    }

    suspend fun deleteSessionMapping(concept: String) {
        conceptThreadMap.remove(concept)
        conceptSessionMap.remove(concept)
        withContext(Dispatchers.IO) {
            ConceptSessionRepository(context).deleteMapping(concept)
        }
        agenticAIClient.setCurrentThreadAndSession(null, null)
    }

    suspend fun loadThreadMapping(concept: String): Pair<String, String?>? {
        conceptThreadMap[concept]?.let { threadId ->
            return Pair(threadId, conceptSessionMap[concept])
        }

        return withContext(Dispatchers.IO) {
            try {
                ConceptSessionRepository(context)
                    .loadMapping(concept)?.also { (thread, session) ->
                        conceptThreadMap[concept] = thread
                        session?.let { conceptSessionMap[concept] = it }
                    }
            } catch (e: Exception) {
                DebugLogger.errorLog("SessionUseCase", "loadThreadMapping: ${e.message}")
                null
            }
        }
    }

    fun hasExistingSession(concept: String): Boolean {
        if (conceptThreadMap[concept] != null) return true
        val repository = ConceptSessionRepository(context)
        return repository.loadMapping(concept) != null
    }


    private fun sessionFailure(error: Throwable?, fallbackStatus: Int? = null): SessionResult {
        val status = ErrorHandler.httpStatusFrom(error) ?: fallbackStatus
        if (status != null) {
            DebugLogger.errorLog("SessionUseCase", "Session API failed with HTTP $status: ${error?.message}")
        }
        return SessionResult(success = false, httpStatusCode = status)
    }

    private fun saveThreadMapping(concept: String, threadId: String, sessionId: String) {
        conceptThreadMap[concept] = threadId
        sessionId.let { conceptSessionMap[concept] = it }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ConceptSessionRepository(context).saveMapping(concept, threadId, sessionId)
                DebugLogger.debugLog("SessionUseCase", "Saved mapping for concept: $concept")
            } catch (e: Exception) {
                DebugLogger.errorLog("SessionUseCase", "saveThreadMapping: ${e.message}")
            }
        }
    }
}

