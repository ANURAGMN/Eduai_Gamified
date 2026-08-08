package com.ncert7.aitutorandlab.domain.mathagent.usecase

import android.content.Context
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.ProblemSessionRepository
import com.ncert7.aitutorandlab.data.remote.AgenticAIClient
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.mathagent.model.MathSessionResult
import com.ncert7.aitutorandlab.ui.screens.mathagentscreen.dataclass.MathMessageModel
import com.ncert7.aitutorandlab.utils.ErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for managing math agent sessions
 * Handles session creation, continuation, and state management
 *
 * Uses ProblemSessionRepository for persistent storage of problem ID to thread/session mappings
 * This ensures session continuity across app restarts
 */
class MathSessionUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val agenticAIClient: AgenticAIClient,
    private val sendMessageUseCase: MathSendMessageUseCase,
    private val problemSessionRepository: ProblemSessionRepository
) {
    /**
     * Starts a new math tutoring session
     */
    suspend fun startSession(
        problemId: String,
        studentId: String,
        isKannada: Boolean
    ): MathSessionResult {
        val cleanProblemId = problemId.trim()
        if (cleanProblemId.isEmpty() || cleanProblemId == "null") {
            DebugLogger.errorLog(
                "MathSessionUseCase",
                "Blocked session start — invalid problemId: '$problemId'"
            )
            return MathSessionResult(
                success = false,
                agentResponse = context.getString(R.string.math_session_missing_problem)
            )
        }

        return try {
            val result = agenticAIClient.startMathSession(
                problemId = cleanProblemId,
                studentId = studentId,
                isKannada = isKannada
            )

            result.onSuccess { response ->
                DebugLogger.debugLog("MathSessionUseCase", "Session started: ${response.threadId}")
            }.onFailure { exception ->
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "Failed to start session: ${exception.message}"
                )
            }

            // Convert result to MathSessionResult
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                // Save thread mapping for session persistence
                saveThreadMapping(cleanProblemId, response.threadId, response.sessionId)
                agenticAIClient.setCurrentThreadAndSession(response.threadId, response.sessionId)

                MathSessionResult(
                    success = true,
                    agentResponse = response.agentResponse,
                    currentState = response.currentState,
                    metadata = response.metadata,
                    threadId = response.threadId,
                    sessionId = response.sessionId,
                    messages = listOf(
                        sendMessageUseCase.createAssistantMessage(
                            content = response.agentResponse,
                            node = response.currentState
                        )
                    )
                )
            } else {
                MathSessionResult(
                    success = false,
                    agentResponse = mapApiFailure(
                        result.exceptionOrNull(),
                        context.getString(R.string.math_session_failed_start)
                    )
                )
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(
                "MathSessionUseCase",
                "Exception starting session: ${e.message}"
            )
            MathSessionResult(
                success = false,
                agentResponse = mapApiFailure(e, context.getString(R.string.math_session_failed_start))
            )
        }
    }

    suspend fun continueSession(
        problemId: String,  // Change: use problemId instead of threadId
        userMessage: String,
        isKannada: Boolean,
        imageUri: String? = null
    ): MathSessionResult {
        return try {
            DebugLogger.debugLog("MathSessionUseCase", "=== CONTINUE SESSION DEBUG ===")
            DebugLogger.debugLog("MathSessionUseCase", "problemId: '$problemId'")
            DebugLogger.debugLog("MathSessionUseCase", "userMessage: '$userMessage'")
            DebugLogger.debugLog("MathSessionUseCase", "isKannada: $isKannada")
            DebugLogger.debugLog("MathSessionUseCase", "imageUri is null: ${imageUri == null}")

            // Load the saved thread mapping using problem ID
            val mapping = loadThreadMapping(problemId)
            if (mapping == null) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "✗ No thread mapping found for problemId: '$problemId'"
                )
                return MathSessionResult(
                    success = false,
                    agentResponse = "No previous session found for this problem"
                )
            }

            val (threadId, sessionId) = mapping
            DebugLogger.debugLog("MathSessionUseCase", "Loaded mapping - threadId: '$threadId', sessionId: '$sessionId'")

            // CRITICAL VALIDATION: Ensure threadId is valid before making request
            if (threadId.isNullOrEmpty() || threadId.isBlank()) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    " Cannot continue session: threadId is null, empty, or blank. threadId='$threadId'"
                )
                return MathSessionResult(
                    success = false,
                    agentResponse = "Invalid thread ID: thread_id cannot be empty"
                )
            }

            val cleanThreadId = threadId.trim()
            if (cleanThreadId.isEmpty()) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "Cannot continue session: threadId contains only whitespace. Original: '$threadId'"
                )
                return MathSessionResult(
                    success = false,
                    agentResponse = "Thread ID is invalid"
                )
            }

            // Double-check threadId is valid before passing to client
            if (cleanThreadId.isNullOrBlank()) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    " CRITICAL: cleanThreadId is null or blank after trim! This should not happen."
                )
                return MathSessionResult(
                    success = false,
                    agentResponse = "Critical: Thread ID validation failed"
                )
            }

            if (userMessage.isNullOrEmpty()) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "✗ Cannot continue session: userMessage is empty"
                )
                return MathSessionResult(
                    success = false,
                    agentResponse = "User message cannot be empty"
                )
            }

            DebugLogger.debugLog(
                "MathSessionUseCase",
                " Pre-call validation passed. Calling client with threadId: '$cleanThreadId', userMessage length: ${userMessage.length}, isKannada: $isKannada"
            )

            val result = agenticAIClient.continueMathSession(
                threadId = cleanThreadId,
                userMessage = userMessage,
                isKannada = isKannada,
                imageUri = imageUri
            )

            result.onSuccess { response ->
                DebugLogger.debugLog(
                    "MathSessionUseCase",
                    " Session continued successfully with threadId: '$cleanThreadId'"
                )
            }.onFailure { exception ->
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    " Failed to continue session with threadId: '$cleanThreadId'. Error: ${exception.message}"
                )
            }

            if (result.isSuccess) {
                val response = result.getOrNull()!!
                MathSessionResult(
                    success = true,
                    agentResponse = response.agentResponse,
                    currentState = response.currentState,
                    metadata = response.metadata,
                    threadId = response.threadId,
                    messages = listOf(
                        sendMessageUseCase.createAssistantMessage(
                            content = response.agentResponse,
                            node = response.currentState
                        )
                    )
                )
            } else {
                MathSessionResult(
                    success = false,
                    agentResponse = mapApiFailure(
                        result.exceptionOrNull(),
                        context.getString(R.string.math_session_failed_continue)
                    )
                )
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(
                "MathSessionUseCase",
                " Exception continuing session: ${e.message}\nStackTrace: ${e.stackTraceToString()}"
            )
            MathSessionResult(
                success = false,
                agentResponse = mapApiFailure(e, context.getString(R.string.math_session_failed_continue))
            )
        }
    }

    /**
     * Save problem ID to thread ID and session ID mapping
     * Called after successfully starting a session
     * This is a suspend function to ensure the mapping is persisted before returning
     */
    private suspend fun saveThreadMapping(problemId: String, threadId: String, sessionId: String?) {
        // keep in-memory map immediately
        DebugLogger.debugLog(
            "MathSessionUseCase",
            "saveThreadMapping called - problemId: '$problemId', threadId: '$threadId', sessionId: '$sessionId'"
        )

        // persist synchronously on IO dispatcher so callers can rely on stored mapping immediately
        withContext(Dispatchers.IO) {
            try {
                problemSessionRepository.saveMapping(problemId, threadId, sessionId)
                DebugLogger.debugLog(
                    "MathSessionUseCase",
                    "✓ Successfully saved problem mapping: problemId='$problemId' -> threadId='$threadId'"
                )
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "✗ saveThreadMapping error: ${e.message}\nStack: ${e.stackTraceToString()}"
                )
            }
        }
    }

    /**
     * Load problem ID to thread ID and session ID mapping from persistent storage
     * Used to check if a previous session exists and to resume sessions
     */
    suspend fun loadThreadMapping(problemId: String): Pair<String, String?>? {
        if (problemId.isBlank()) {
            DebugLogger.errorLog(
                "MathSessionUseCase",
                "✗ Cannot load thread mapping with blank problemId"
            )
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val mapping = problemSessionRepository.loadMapping(problemId)
                if (mapping != null) {
                    DebugLogger.debugLog(
                        "MathSessionUseCase",
                        "✓ Loaded thread mapping for problemId='$problemId': threadId='${mapping.first}', sessionId='${mapping.second}'"
                    )
                } else {
                    DebugLogger.debugLog(
                        "MathSessionUseCase",
                        "No thread mapping found for problemId='$problemId'"
                    )
                }
                mapping
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    " loadThreadMapping error for problemId='$problemId': ${e.message}\nStack: ${e.stackTraceToString()}"
                )
                null
            }
        }
    }

    /**
     * Check if there's an existing session for a problem
     */
    suspend fun hasExistingSession(problemId: String): Boolean {
        if (problemId.isBlank()) {
            DebugLogger.debugLog("MathSessionUseCase", "hasExistingSession: problemId is blank")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val exists = problemSessionRepository.hasMapping(problemId)
                DebugLogger.debugLog(
                    "MathSessionUseCase",
                    if (exists) "✓ Existing session found for problemId='$problemId'"
                    else "No existing session for problemId='$problemId'"
                )
                exists
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "✗ hasExistingSession error for problemId='$problemId': ${e.message}"
                )
                false
            }
        }
    }

    /**
     * Delete session mapping for a problem
     * Called when user chooses to start fresh instead of resuming
     */
    suspend fun deleteSessionMapping(problemId: String) {
        if (problemId.isBlank()) {
            DebugLogger.errorLog(
                "MathSessionUseCase",
                "Cannot delete mapping with blank problemId"
            )
            return
        }

        withContext(Dispatchers.IO) {
            try {
                problemSessionRepository.deleteMapping(problemId)
                DebugLogger.debugLog(
                    "MathSessionUseCase",
                    "✓ Deleted session mapping for problemId: '$problemId'"
                )
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "✗ deleteSessionMapping error: ${e.message}"
                )
            }
        }
    }

    /**
     * Resume an existing session using stored thread ID
     */
    suspend fun resumeSession(threadId: String, sessionId: String?): MathSessionResult {
        return try {
            // Validate threadId before attempting to resume
            if (threadId.isNullOrEmpty() || threadId.isBlank()) {
                DebugLogger.errorLog(
                    "MathSessionUseCase",
                    "Cannot resume session: Invalid threadId: '$threadId'"
                )
                return MathSessionResult(
                    success = false,
                    agentResponse = context.getString(R.string.math_session_failed_resume) + " (Invalid thread ID)"
                )
            }

            val cleanThreadId = threadId.trim()
            DebugLogger.debugLog("MathSessionUseCase", "Resuming session - thread=$cleanThreadId")
            agenticAIClient.setCurrentThreadAndSession(cleanThreadId, sessionId)

            // Get session history to show previous messages
            val histResult = agenticAIClient.getMathSessionHistory(cleanThreadId)
            if (histResult.isSuccess) {
                val messages = histResult.getOrNull()?.messages ?: emptyList()
                val mathMessages = messages.map { msg ->
                    MathMessageModel(
                        role = msg.role,
                        content = msg.content,
                        node = msg.node,
                        timestamp = System.currentTimeMillis()
                    )
                }

                MathSessionResult(
                    success = true,
                    agentResponse = context.getString(R.string.math_session_resumed),
                    threadId = cleanThreadId,
                    sessionId = sessionId,
                    messages = mathMessages
                )
            } else {
                MathSessionResult(
                    success = false,
                    agentResponse = context.getString(R.string.math_session_failed_resume)
                )
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(
                "MathSessionUseCase",
                "Exception resuming session: ${e.message}"
            )
            MathSessionResult(
                success = false,
                agentResponse = context.getString(R.string.math_error_prefix, e.message ?: "")
            )
        }
    }

    private fun mapApiFailure(error: Throwable?, fallback: String): String {
        if (error == null) return fallback
        val status = ErrorHandler.httpStatusFrom(error)
        return when (status) {
            401 -> context.getString(R.string.error_please_sign_in_again)
            422 -> context.getString(R.string.math_session_missing_problem)
            429, 503 -> context.getString(R.string.error_service_busy_try_again_shortly)
            in 500..599 -> context.getString(R.string.error_server_try_later)
            else -> when (error) {
                is Exception -> ErrorHandler.handleException(
                    context,
                    error,
                    operation = "math session",
                    tag = "MathSessionUseCase"
                )
                else -> fallback
            }
        }
    }
}