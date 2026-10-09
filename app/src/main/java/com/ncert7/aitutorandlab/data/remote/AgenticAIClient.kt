package com.ncert7.aitutorandlab.data.remote

import android.content.Context
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.utils.ErrorHandler
import com.ncert7.aitutorandlab.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.gson.JsonPrimitive
import retrofit2.Response
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request

class AgenticAIClient(
    agenticAIBaseUrl: String,
    private val context: Context
) {
    val service: AgenticAIService
    private val okHttpClient = OkHttpClient()

    private val _currentThreadId = MutableStateFlow<String?>(null)
    private val _currentSessionId = MutableStateFlow<String?>(null)

    init {
        val retrofit = RetrofitProvider.buildRetrofit(agenticAIBaseUrl, context)
        service = retrofit.create(AgenticAIService::class.java)
    }

    private suspend fun <T : Any> callWithRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 300L,
        factor: Double = 2.0,
        call: suspend () -> Response<T>
    ): Result<T> {
        var attempt = 0
        var lastEx: Exception? = null
        var delayMs = initialDelayMs
        var tokenExpiredRetries = 0
        val maxTokenRefreshRetries = 3

        while (attempt < maxAttempts) {
            attempt++
            try {
                DebugLogger.debugLog("AgenticAIClient", "Call attempt=$attempt for call (starting)")

                val currentToken = TokenManager.getIdToken(context)
                if (currentToken == null) {
                    DebugLogger.errorLog("AgenticAIClient", "No token found in storage")
                }

                val resp = call()
                try {
                    val urlStr = resp.raw().request.url.toString()
                    val code = resp.code()
                    DebugLogger.debugLog("AgenticAIClient", "Response received: attempt=$attempt url=$urlStr code=$code")
                    val reqHeaders: Headers = resp.raw().request.headers
                    val headerName = BuildConfig.API_KEY_HEADER_NAME.trim().ifEmpty { "X-API-Key" }
                    val hv = reqHeaders[headerName]
                    if (hv != null) {
                        val masked = if (hv.length <= 6) "****" else "****" + hv.takeLast(4)
                        DebugLogger.debugLog("AgenticAIClient", "Request contained header $headerName with value=$masked")
                    } else {
                        DebugLogger.debugLog("AgenticAIClient", "Request did not contain header $headerName")
                    }
                } catch (inner: Exception) {
                    DebugLogger.errorLog("AgenticAIClient", "Error logging response metadata: ${inner.message}")
                }

                when {
                    resp.isSuccessful && resp.body() != null -> {
                        val body = resp.body()!!
                        val isAppSuccess = isApplicationSuccess(body)

                        try {
                            val jsonString = com.google.gson.Gson().toJson(body)
                            DebugLogger.debugLog("AgenticAIClient", "Response body JSON: $jsonString")
                        } catch (e: Exception) {
                            DebugLogger.debugLog("AgenticAIClient", "Could not serialize response to JSON: ${e.message}")
                        }

                        if (isAppSuccess) {
                            return Result.success(body)
                        } else {
                            val message = getServerMessage(body)
                            lastEx = IOException("Server error: ${message ?: "Unknown error"}")
                            break
                        }
                    }

                    resp.isSuccessful -> {
                        lastEx = IOException("Empty response body (HTTP ${resp.code()})")
                        break
                    }

                    else -> {
                        val result = ErrorHandler.handleResponseCode(
                            resp, context, attempt, tokenExpiredRetries, maxTokenRefreshRetries, "AgenticAIClient"
                        )

                        when (result) {
                            ErrorHandler.ResponseHandlerResult.Success -> {
                                return Result.success(resp.body() as T)
                            }
                            is ErrorHandler.ResponseHandlerResult.ServerError -> {
                                lastEx = result.exception
                                val errMsg = result.exception.message.orEmpty()
                                val code = ErrorHandler.extractStatusCode(errMsg)
                                val transientSsl500 = code == 500 && (
                                        errMsg.contains("start_new_session", ignoreCase = true) ||
                                                errMsg.contains("SSL error", ignoreCase = true) ||
                                                errMsg.contains("unexpected eof", ignoreCase = true) ||
                                                errMsg.contains("TutorExecutionFailed", ignoreCase = true)
                                        )
                                if (code == 500 && !transientSsl500) {
                                    break
                                }
                                if (transientSsl500) {
                                    DebugLogger.debugLog("AgenticAIClient", "Transient SSL/upstream 500 — will retry (attempt=$attempt)")
                                }
                            }
                            is ErrorHandler.ResponseHandlerResult.ClientError -> {
                                lastEx = result.exception
                                break
                            }
                            is ErrorHandler.ResponseHandlerResult.OtherClientError -> {
                                lastEx = result.exception
                            }
                            is ErrorHandler.ResponseHandlerResult.UnknownError -> {
                                lastEx = result.exception
                            }
                            is ErrorHandler.ResponseHandlerResult.Token401RetryAfterRefresh -> {
                                tokenExpiredRetries = result.newRetryCount
                                attempt--
                                continue
                            }
                            is ErrorHandler.ResponseHandlerResult.Token401RetryAfterFailedRefresh -> {
                                tokenExpiredRetries = result.newRetryCount
                                lastEx = IOException("Token refresh failed")
                                continue
                            }
                            is ErrorHandler.ResponseHandlerResult.Token401Exhausted -> {
                                lastEx = result.exception
                                break
                            }
                            is ErrorHandler.ResponseHandlerResult.Token401NotExpired -> {
                                lastEx = result.exception
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                lastEx = e
                DebugLogger.errorLog("AgenticAIClient", "Attempt $attempt/$maxAttempts failed: ${e.message}")
            }

            if (attempt < maxAttempts && ErrorHandler.shouldRetryException(lastEx)) {
                delay(delayMs)
                delayMs = (delayMs * factor).toLong()
            } else if (attempt < maxAttempts && !ErrorHandler.shouldRetryException(lastEx)) {
                break
            }
        }

        return Result.failure(lastEx ?: IOException("Unknown error after $maxAttempts attempts"))
    }

    private fun isApplicationSuccess(body: Any): Boolean {
        return when (body) {
            is StartSessionResponse -> body.success
            is ContinueSessionResponse -> body.success
            is SessionStatusResponse -> body.success
            is SessionHistoryResponse -> body.success
            is SessionSummaryResponse -> body.success
            is ConceptsListResponse -> body.success
            is TestImageResponse -> body.success
            is TranslationResponse -> body.success
            is RevisionChaptersResponse -> body.success
            is RevStartSessionResponse -> body.success
            is RevContinueSessionResponse -> body.success
            is RevSessionStatusResponse -> body.success
            is RevSessionHistoryResponse -> body.success
            is SimSessionResponse -> true
            is SimSimulationsListResponse -> true
            is SimHealthResponse -> true
            is HealthResponse -> true
            is MathStartSessionResponse -> body.success
            is MathContinueSessionResponse -> body.success
            is MathSessionStatusResponse -> body.success
            is MathSessionHistoryResponse -> body.success
            is ProblemsListResponse -> body.success
            is WbConceptsResponse -> body.success
            is WbSessionTurnResponse -> body.success
            else -> true
        }
    }

    private fun getServerMessage(body: Any): String? {
        return when (body) {
            is StartSessionResponse -> body.message
            is ContinueSessionResponse -> body.message
            is SessionStatusResponse -> body.message
            is SessionHistoryResponse -> body.message
            is SessionSummaryResponse -> body.message
            is ConceptsListResponse -> body.message
            is TestImageResponse -> body.message
            is TranslationResponse -> body.error
            is RevisionChaptersResponse -> body.message
            is RevStartSessionResponse -> body.message
            is RevContinueSessionResponse -> body.message
            is RevSessionStatusResponse -> body.message
            is RevSessionHistoryResponse -> body.message
            is SimSessionResponse -> null
            is SimSimulationsListResponse -> null
            is SimHealthResponse -> null
            is MathStartSessionResponse -> body.message
            is MathContinueSessionResponse -> body.message
            is MathSessionHistoryResponse -> body.message
            is ProblemsListResponse -> body.message
            is WbConceptsResponse -> body.message
            is WbSessionTurnResponse -> body.message
            else -> null
        }
    }

    private fun safeGetErrorBody(resp: Response<*>): String? {
        return try {
            resp.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
    }

    // ==================== EDUCATION AGENT ====================

    suspend fun startSession(
        conceptTitle: String,
        studentId: String,
        personaName: String? = null,
        sessionLabel: String? = null,
        isKannada: Boolean = false,
        studentLevel: String = "medium"
    ): Result<StartSessionResponse> = withContext(Dispatchers.IO) {
        val req = StartSessionRequest(
            conceptTitle = conceptTitle,
            studentId = studentId,
            personaName = personaName,
            sessionLabel = sessionLabel,
            isKannada = isKannada,
            studentLevel = studentLevel
        )

        val res = callWithRetry { service.startSession(req) }

        if (res.isSuccess) {
            val body = res.getOrNull()
            body?.threadId?.let { _currentThreadId.value = it }
            body?.sessionId?.let { _currentSessionId.value = it }
            DebugLogger.debugLog(
                "AgenticAIClient",
                "Session started: threadId=${body?.threadId}, sessionId=${body?.sessionId}"
            )
        }
        res
    }

    suspend fun continueSession(
        userMessage: String,
        clickedAutosuggestion: Boolean,
        studentLevel: String,
        isKannada: Boolean
    ): Result<ContinueSessionResponse> =
        withContext(Dispatchers.IO) {
            val thread = _currentThreadId.value
                ?: return@withContext Result.failure(IOException("No active thread"))

            val req = ContinueSessionRequest(
                threadId = thread,
                userMessage = userMessage,
                clickedAutosuggestion = clickedAutosuggestion,
                studentLevel = studentLevel,
                isKannada = isKannada
            )

            val res = callWithRetry { service.continueSession(req) }

            if (res.isSuccess) {
                val body = res.getOrNull()
                body?.threadId?.let {
                    if (it != _currentThreadId.value) {
                        DebugLogger.debugLog(
                            "AgenticAIClient",
                            "ThreadId updated: ${_currentThreadId.value} -> $it"
                        )
                        _currentThreadId.value = it
                    }
                }
            }

            res
        }

    fun setCurrentThreadAndSession(threadId: String?, sessionId: String?) {
        _currentThreadId.value = threadId
        _currentSessionId.value = sessionId
    }

    suspend fun getSessionStatus(threadId: String): Result<SessionStatusResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getSessionStatus(threadId) }
        }

    suspend fun getSessionHistory(threadId: String): Result<SessionHistoryResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getSessionHistory(threadId) }
        }

    suspend fun getConceptsList(): Result<ConceptsListResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getAvailableConcepts() }
        }

    // ==================== TRANSLATION METHODS ====================

    suspend fun translateToKannada(text: String): Result<TranslationResponse> =
        withContext(Dispatchers.IO) {
            val req = TranslationRequest(text)
            callWithRetry { service.translateToKannada(req) }
        }

    suspend fun translateToEnglish(text: String): Result<TranslationResponse> =
        withContext(Dispatchers.IO) {
            val req = TranslationRequest(text)
            callWithRetry { service.translateToEnglish(req) }
        }

    // ==================== REVISION METHODS ====================

    suspend fun getRevisionChapters(): Result<RevisionChaptersResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getRevisionChapters() }
        }

    suspend fun startRevisionSession(
        chapter: String,
        studentId: String? = null,
        isKannada: Boolean = false,
        sessionLabel: String? = null
    ): Result<RevStartSessionResponse> = withContext(Dispatchers.IO) {
        val req = RevStartSessionRequest(
            chapter = chapter,
            studentId = studentId,
            isKannada = isKannada,
            sessionLabel = sessionLabel
        )

        val res = callWithRetry { service.startRevisionSession(req) }

        if (res.isSuccess) {
            val body = res.getOrNull()
            body?.threadId?.let { _currentThreadId.value = it }
            body?.sessionId?.let { _currentSessionId.value = it }
        }
        res
    }

    suspend fun continueRevisionSession(
        threadId: String,
        userMessage: String,
        isKannada: Boolean? = null
    ): Result<RevContinueSessionResponse> = withContext(Dispatchers.IO) {
        val req = RevContinueSessionRequest(
            threadId = threadId,
            userMessage = userMessage,
            isKannada = isKannada
        )

        val res = callWithRetry { service.continueRevisionSession(req) }

        if (res.isSuccess) {
            val body = res.getOrNull()
            body?.threadId?.let {
                if (it != _currentThreadId.value) {
                    _currentThreadId.value = it
                }
            }
        }

        res
    }

    suspend fun getRevisionSessionStatus(threadId: String): Result<RevSessionStatusResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getRevisionSessionStatus(threadId) }
        }

    suspend fun getRevisionSessionHistory(threadId: String): Result<RevSessionHistoryResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getRevisionSessionHistory(threadId) }
        }

    suspend fun deleteRevisionSession(threadId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = service.deleteRevisionSession(threadId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errBody = safeGetErrorBody(response)
                    Result.failure(IOException("HTTP ${response.code()}: ${errBody ?: response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== SIMULATION METHODS ====================

    suspend fun simulationHealthCheck(): Result<SimHealthResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.simulationHealthCheck() }
        }

    suspend fun getAvailableSimulations(): Result<SimSimulationsListResponse>  =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getAvailableSimulations() }
        }

    suspend fun startSimulationSession(
        simulationId: String,
        studentId: String? = null,
        language: String? = "english"
    ): Result<SimSessionResponse> = withContext(Dispatchers.IO) {
        val req = SimStartSessionRequest(
            simulationId = simulationId,
            studentId = studentId,
            language = language
        )

        val res = callWithRetry { service.startSimulationSession(req) }

        if (res.isSuccess) {
            val body = res.getOrNull()
            body?.sessionId?.let { _currentSessionId.value = it }
        }
        res
    }

    suspend fun sendSimulationResponse(
        sessionId: String,
        studentResponse: String,
        studentChangedParams: Map<String, Any>? = null
    ): Result<SimSessionResponse> = withContext(Dispatchers.IO) {
        val convertedParams = studentChangedParams?.mapValues { (_, v) ->
            when (v) {
                is String -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                is Boolean -> JsonPrimitive(v)
                else -> JsonPrimitive(v.toString())
            }
        }
        val req = SimStudentResponseRequest(
            studentResponse = studentResponse,
            studentChangedParams = convertedParams

        )
        callWithRetry { service.sendSimulationResponse(sessionId, req) }
    }

    suspend fun submitSimulationQuiz(
        sessionId: String,
        answer: String
    ): Result<SimSessionResponse> = withContext(Dispatchers.IO) {
        val req = SimQuizAnswerRequest(answer = answer)
        callWithRetry { service.submitSimulationQuiz(sessionId, req) }
    }

    suspend fun getSimulationSession(sessionId: String): Result<SimSessionResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getSimulationSession(sessionId) }
        }

    // ==================== MATH AGENT METHODS ====================

    suspend fun getAvailableMathProblems(): Result<ProblemsListResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getAvailableMathProblems() }
        }

    suspend fun startMathSession(
        problemId: String,
        studentId: String? = null,
        sessionLabel: String? = null,
        isKannada: Boolean = false
    ): Result<MathStartSessionResponse> = withContext(Dispatchers.IO) {
        val cleanProblemId = problemId.trim()
        if (cleanProblemId.isEmpty() || cleanProblemId == "null") {
            DebugLogger.errorLog(
                "AgenticAIClient",
                "Blocked startMathSession — problem_id is missing or invalid: '$problemId'"
            )
            return@withContext Result.failure(
                IllegalArgumentException("problem_id is required")
            )
        }

        val req = MathStartSessionRequest(
            problemId = cleanProblemId,
            studentId = studentId,
            sessionLabel = sessionLabel,
            isKannada = isKannada
        )

        val res = callWithRetry { service.startMathSession(req) }

        if (res.isSuccess) {
            val body = res.getOrNull()
            body?.threadId?.let { _currentThreadId.value = it }
        }
        res
    }

    suspend fun continueMathSession(
        threadId: String,
        userMessage: String,
        isKannada: Boolean = false,
        imageUri: String? = null,
    ): Result<MathContinueSessionResponse> = withContext(Dispatchers.IO) {
        if (threadId.isNullOrEmpty() || threadId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Thread ID cannot be null or blank"))
        }
        val hasImage = imageUri != null

        val threadIdPart = RequestBody.create("text/plain".toMediaType(), threadId)
        val userMessagePart = RequestBody.create("text/plain".toMediaType(), userMessage)
        val isKannadaPart = RequestBody.create("text/plain".toMediaType(), isKannada.toString())

        val imagePart: okhttp3.MultipartBody.Part? = imageUri?.let { uriStr ->
            try {
                val uri = android.net.Uri.parse(uriStr)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    val requestBody = okhttp3.RequestBody.create("application/octet-stream".toMediaType(), bytes)
                    okhttp3.MultipartBody.Part.createFormData("image", "image.jpg", requestBody)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        try {
            val maxImageRetryAttempts = if (hasImage) 5 else 1
            val imageRetryDelayMs = 3000L

            var lastResult: Result<MathContinueSessionResponse>? = null
            for (imageAttempt in 1..maxImageRetryAttempts) {
                val response = callWithRetry {
                    service.continueMathSession(
                        threadId = threadIdPart,
                        userMessage = userMessagePart,
                        isKannada = isKannadaPart,
                        image = imagePart
                    )
                }

                lastResult = response

                if (response.isSuccess) {
                    return@withContext response
                }

                if (!hasImage) {
                    break
                }

                if (imageAttempt < maxImageRetryAttempts) {
                    delay(imageRetryDelayMs)
                }
            }

            lastResult ?: Result.failure(IOException("No response received"))
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getMathSessionStatus(threadId: String): Result<MathSessionStatusResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getMathSessionStatus(threadId) }
        }

    suspend fun getMathSessionHistory(threadId: String): Result<MathSessionHistoryResponse> =
        withContext(Dispatchers.IO) {
            callWithRetry { service.getMathSessionHistory(threadId) }
        }

    // ==================== NEW WHITEBOARD METHODS ====================

    suspend fun getWhiteboardConcepts(): Result<WbConceptsResponse> = withContext(Dispatchers.IO) {
        callWithRetry { service.getWhiteboardConcepts() }
    }

    // FIX: Pass studentId dynamically to avoid 500 error on fresh sessions
    suspend fun startWhiteboardSession(conceptId: String, studentId: String? = null): Result<WbSessionTurnResponse> = withContext(Dispatchers.IO) {
        val req = WbStartSessionRequest(conceptId = conceptId, studentId = studentId)
        val res = callWithRetry { service.startWhiteboardSession(req) }
        if (res.isSuccess) {
            res.getOrNull()?.threadId?.let { _currentThreadId.value = it }
        }
        res
    }

    suspend fun continueWhiteboardSession(threadId: String, userMessage: String): Result<WbSessionTurnResponse> = withContext(Dispatchers.IO) {
        val req = WbContinueSessionRequest(threadId = threadId, userMessage = userMessage)
        val res = callWithRetry { service.continueWhiteboardSession(req) }
        if (res.isSuccess) {
            res.getOrNull()?.threadId?.let { if (it != _currentThreadId.value) _currentThreadId.value = it }
        }
        res
    }

    suspend fun fetchWhiteboardSvg(sceneId: String): String? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = BuildConfig.AGENTIC_AI_BASE_URL.trimEnd('/')
            val svgUrl = "$baseUrl/whiteboard-tutor/scenes/$sceneId.svg"

            val token = TokenManager.getIdToken(context)
            val requestBuilder = Request.Builder().url(svgUrl)
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                var svgString = response.body?.string() ?: return@withContext null
                if (svgString.startsWith("\"") && svgString.endsWith("\"")) {
                    svgString = svgString.substring(1, svgString.length - 1)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                }
                svgString
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}