package com.ncert7.aitutorandlab.data.remote

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

interface AgenticAIService {
    //core session Endpoints
    @POST("/session/start")
    suspend fun startSession(@Body request: StartSessionRequest): Response<StartSessionResponse>

    @POST("/session/continue")
    suspend fun continueSession(@Body request: ContinueSessionRequest): Response<ContinueSessionResponse>

    @GET("/session/status/{thread_id}")
    suspend fun getSessionStatus(@Path("thread_id") threadId: String): Response<SessionStatusResponse>

    @GET("/session/history/{thread_id}")
    suspend fun getSessionHistory(@Path("thread_id") threadId: String): Response<SessionHistoryResponse>

    @GET("/session/summary/{thread_id}")
    suspend fun getSessionSummary(@Path("thread_id") threadId: String): Response<SessionSummaryResponse>

    @GET("/concepts")
    suspend fun getAvailableConcepts(): Response<ConceptsListResponse>

    //Utility Endpoints
    @GET("/health")
    suspend fun healthCheck(): Response<HealthResponse>

    //test endpoints
    @POST("/test/image")
    suspend fun getTestImage(@Body request: TestImageRequest): Response<TestImageResponse>

    //translation endpoints
    @POST("/translate/to-kannada")
    suspend fun translateToKannada(@Body request: TranslationRequest): Response<TranslationResponse>

    @POST("/translate/to-english")
    suspend fun translateToEnglish(@Body request: TranslationRequest): Response<TranslationResponse>

    //revision endpoints
    @GET("/revision/chapters")
    suspend fun getRevisionChapters(): Response<RevisionChaptersResponse>

    @POST("/revision/session/start")
    suspend fun startRevisionSession(@Body request: RevStartSessionRequest): Response<RevStartSessionResponse>

    @POST("/revision/session/continue")
    suspend fun continueRevisionSession(@Body request: RevContinueSessionRequest): Response<RevContinueSessionResponse>

    @GET("/revision/session/status/{thread_id}")
    suspend fun getRevisionSessionStatus(@Path("thread_id") threadId: String): Response<RevSessionStatusResponse>

    @GET("/revision/session/history/{thread_id}")
    suspend fun getRevisionSessionHistory(@Path("thread_id") threadId: String): Response<RevSessionHistoryResponse>

    @DELETE("/revision/session/{thread_id}")
    suspend fun deleteRevisionSession(@Path("thread_id") threadId: String): Response<String>

    // ==================== SIMULATION ENDPOINTS ====================
    @GET("/simulation")
    suspend fun simulationHealthCheck(): Response<SimHealthResponse>

    @GET("/simulation/simulations")
    suspend fun getAvailableSimulations(): Response<SimSimulationsListResponse>

    @POST("/simulation/session/start")
    suspend fun startSimulationSession(@Body request: SimStartSessionRequest): Response<SimSessionResponse>

    @POST("/simulation/session/{session_id}/respond")
    suspend fun sendSimulationResponse(
        @Path("session_id") sessionId: String,
        @Body request: SimStudentResponseRequest
    ): Response<SimSessionResponse>

    @POST("/simulation/session/{session_id}/submit-quiz")
    suspend fun submitSimulationQuiz(
        @Path("session_id") sessionId: String,
        @Body request: SimQuizAnswerRequest
    ): Response<SimSessionResponse>

    @GET("/simulation/session/{session_id}")
    suspend fun getSimulationSession(@Path("session_id") sessionId: String): Response<SimSessionResponse>

    // ==================== MATH AGENT ENDPOINTS ====================
    @GET("/math/problems")
    suspend fun getAvailableMathProblems(): Response<ProblemsListResponse>

    @POST("/math/session/start")
    suspend fun startMathSession(@Body request: MathStartSessionRequest): Response<MathStartSessionResponse>

    @POST("/math/session/continue")
    @Multipart
    suspend fun continueMathSession(
        @Part("thread_id") threadId: okhttp3.RequestBody,
        @Part("user_message") userMessage: okhttp3.RequestBody,
        @Part("is_kannada") isKannada: okhttp3.RequestBody,
        @Part image: MultipartBody.Part? = null
    ): Response<MathContinueSessionResponse>

    @GET("/math/session/status/{thread_id}")
    suspend fun getMathSessionStatus(@Path("thread_id") threadId: String): Response<MathSessionStatusResponse>

    @GET("/math/session/history/{thread_id}")
    suspend fun getMathSessionHistory(@Path("thread_id") threadId: String): Response<MathSessionHistoryResponse>

    // ==================== WHITEBOARD TUTOR ENDPOINTS ====================
    @GET("/whiteboard-tutor/concepts")
    suspend fun getWhiteboardConcepts(): Response<WbConceptsResponse>

    @POST("/whiteboard-tutor/session/start")
    suspend fun startWhiteboardSession(@Body request: WbStartSessionRequest): Response<WbSessionTurnResponse>

    @POST("/whiteboard-tutor/session/continue")
    suspend fun continueWhiteboardSession(@Body request: WbContinueSessionRequest): Response<WbSessionTurnResponse>

    @GET("/whiteboard-tutor/session/status/{thread_id}")
    suspend fun getWhiteboardSessionStatus(@Path("thread_id") threadId: String): Response<WbSessionStatusResponse>

    @GET("/whiteboard-tutor/session/history/{thread_id}")
    suspend fun getWhiteboardSessionHistory(@Path("thread_id") threadId: String): Response<WbSessionHistoryResponse>

    @POST("/whiteboard-tutor/session/{thread_id}/whiteboard/reset")
    suspend fun resetWhiteboardSession(@Path("thread_id") threadId: String): Response<WbSessionTurnResponse>

    @DELETE("/whiteboard-tutor/session/{thread_id}")
    suspend fun deleteWhiteboardSession(@Path("thread_id") threadId: String): Response<WbDeleteSessionResponse>

    @GET("/whiteboard-tutor/scenes/manifest")
    suspend fun getWhiteboardSceneManifest(): Response<WbSceneManifestResponse>
}

//All Data Classes
data class StartSessionRequest(
    @SerializedName("concept_title") val conceptTitle: String,
    @SerializedName("student_id") val studentId: String,
    @SerializedName("persona_name") val personaName: String? = null,
    @SerializedName("session_label") val sessionLabel: String? = null,
    @SerializedName("is_kannada") val isKannada: Boolean = false,
    @SerializedName("student_level") val studentLevel: String = "medium"
)

data class ContinueSessionRequest(
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("user_message") val userMessage: String,
    @SerializedName("clicked_autosuggestion") val clickedAutosuggestion: Boolean? = false,
    @SerializedName("is_kannada") val isKannada: Boolean = false,
    @SerializedName("student_level") val studentLevel: String? = null
)


data class TestImageRequest(
    @SerializedName("concept_title") val conceptTitle: String,
    @SerializedName("definition_context") val definitionContext: String = ""
)

data class TranslationRequest(
    @SerializedName("text") val text: String,
)
data class SessionMetadata(
    @SerializedName("show_simulation") val showSimulation: Boolean? = false,
    @SerializedName("simulation_config") val simulationConfig: Map<String,Any>? = emptyMap(),
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_description") val imageDescription : String? = null,
    @SerializedName("image_node") val imageNode: String? = null,
    @SerializedName("video_url") val videoUrl: String? = null,
    @SerializedName("video_node") val videoNode: String? = null,
    @SerializedName("quiz_score") val quizScore: Float? = null,
    @SerializedName("retrieval_score") val retrievalScore: Float? = null,
    @SerializedName("sim_concepts") val simConcepts: List<String>? = null,
    @SerializedName("sim_current_idx") val simCurrentIdx: Int? = null,
    @SerializedName("sim_total_concepts") val simTotalConcepts: Int? = null,
    @SerializedName("misconception_detected") val misconceptionDetected: Boolean? = false,
    @SerializedName("last_correction") val lastCorrection: String? = "",
    @SerializedName("node_transitions") val nodeTransitions: List<Map<String,Any>> = emptyList()
)

data class StartSessionResponse(
    val success: Boolean = false,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("agent_response") val agentResponse: String,
    @SerializedName("current_state") val currentState: String? = null,
    @SerializedName("concept_title") val conceptTitle: String? = null,
    val message: String? = "Session started successfully",
    val metadata: SessionMetadata = SessionMetadata(),
    val autosuggestions: List<String> = emptyList()
)

data class ContinueSessionResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String? = null,
    @SerializedName("agent_response") val agentResponse: String? = null,
    @SerializedName("current_state") val currentState: String? = null,
    val metadata: SessionMetadata = SessionMetadata(),
    val message: String? = "Response generated successfully",
    val autosuggestions: List<String> = emptyList()
)

data class TestImageResponse(
    val success: Boolean = false,
    val concept: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_description") val imageDescription: String? = null,
    val message: String? = null
)

data class ConceptsListResponse(
    val success: Boolean = true,
    val concepts: List<String> = emptyList(),
    val total: Int = 0,
    val message: String? = "Available concepts retrieved successfully"
)

data class TranslationResponse(
    @SerializedName("original") val original: String,
    @SerializedName("translated") val translated: String,
    @SerializedName("success") val success: Boolean,
    @SerializedName("error") val error: String? = null
)

data class HealthResponse(
    val status: String,
    val version: String,
    val persistence: String,
    @SerializedName("agent_type") val agentType: String,
    @SerializedName("available_endpoints") val availableEndpoints: List<String>
)

data class SessionStatusResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String? = null,
    val exists: Boolean = false,
    @SerializedName("current_state") val currentState: String? = null,
    val progress: Map<String, Any>? = null,
    @SerializedName("concept_title") val conceptTitle: String? = null,
    val message: String? = "Status retrieved successfully"
)
data class SessionHistoryResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String? = null,
    val exists: Boolean = false,
    val messages: List<Map<String, Any>> = emptyList(),
    @SerializedName("node_transitions") val nodeTransitions: List<Map<String, Any>> = emptyList(),
    @SerializedName("concept_title") val conceptTitle: String? = null,
    val message: String? = "History retrieved successfully")

data class SessionSummaryResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String? = null,
    val exists: Boolean = false,
    val summary: Map<String, Any>? = null,
    @SerializedName("quiz_score") val quizScore: Float? = null,
    @SerializedName("transfer_success") val transferSuccess: Boolean? = null,
    @SerializedName("misconception_detected") val misconceptionDetected: Boolean? = null,
    @SerializedName("definition_echoed") val definitionEchoed: Boolean? = null,
    val message: String? = "Summary retrieved successfully")

// Revision endpoints data classes
data class RevisionChaptersResponse(
    val success: Boolean = true,
    val chapters: List<String> = emptyList(),
    val total: Int = 0,
    val message: String? = "Available chapters retrieved successfully."
)

data class RevStartSessionRequest(
    @SerializedName("chapter") val chapter: String,
    @SerializedName("student_id") val studentId: String? = null,
    @SerializedName("is_kannada") val isKannada: Boolean = false,
    @SerializedName("session_label") val sessionLabel: String? = null
)

data class RevStartSessionResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("student_id") val studentId: String,
    @SerializedName("chapter") val chapter: String,
    @SerializedName("agent_response") val agentResponse: String,
    @SerializedName("current_state") val currentState: String,
    val message: String? = "Revision session started successfully"
)

data class RevContinueSessionRequest(
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("user_message") val userMessage: String,
    @SerializedName("is_kannada") val isKannada: Boolean? = null
)

data class RevContinueSessionResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("agent_response") val agentResponse: String,
    @SerializedName("current_state") val currentState: String,
    val message: String? = "Revision response generated successfully"
)

data class RevSessionStatusResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String,
    val exists: Boolean = false,
    @SerializedName("current_state") val currentState: String? = null,
    @SerializedName("chapter") val chapter: String? = null,
    val progress: Map<String, Any>? = null,
    val message: String? = "Revision status retrieved successfully"
)

data class RevSessionHistoryResponse(
    val success: Boolean = false,
    @SerializedName("thread_id") val threadId: String,
    val exists: Boolean = false,
    val messages: List<Map<String, Any>>? = null,
    @SerializedName("node_transitions") val nodeTransitions: List<Map<String, Any>>? = null,
    @SerializedName("chapter") val chapter: String? = null,
    val message: String? = "Revision history retrieved successfully"
)

//Simulation data classes

data class SimStartSessionRequest(
    @SerializedName("simulation_id") val simulationId: String,
    @SerializedName("student_id") val studentId: String? = null,
    @SerializedName("language") val language:String?="english"
)

data class SimStudentResponseRequest(
    @SerializedName("student_response") val studentResponse: String,
    @SerializedName("student_changed_params") val studentChangedParams: Map<String, JsonElement>? = null
)

data class SimQuizAnswerRequest(val answer: String)

// ==================== RESPONSE MODELS ====================

data class SimHealthResponse(
    val status: String,
    val service: String,
    val version: String,
    @SerializedName("available_simulations") val availableSimulations: List<String>
)

data class SimSimulationsListResponse(
    val simulations: List<SimSimulationMetadata>
)

data class SimSimulationMetadata(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("concept_count") val conceptCount: Int? = null,
    val tags: List<String>? = null
)

data class SimSessionResponse(
    @SerializedName("session_id") val sessionId: String,
    val simulation: SimSimulationState,
    val concepts: SimConceptsInfo,
    @SerializedName("teacher_message") val teacherMessage: SimTeacherMessage,
    @SerializedName("learning_state") val learningState: SimLearningState,
    @SerializedName("language")val language: String?="english",
    val summary: Map<String, String>? = null
)

data class SimSimulationState(
    val id: String,
    val title: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("current_params") val currentParams: Map<String, JsonElement>,
    @SerializedName("param_change") val paramChange: SimParameterChange? = null
)

data class SimParameterChange(
    val parameter: String,
    val before: JsonElement? = null,
    val after: JsonElement? = null,
    val reason: String? = null,
    @SerializedName("before_url") val beforeUrl: String? = null,
    @SerializedName("after_url") val afterUrl: String? = null
)

data class SimConceptsInfo(
    val total: Int,
    @SerializedName("current_index") val currentIndex: Int,
    @SerializedName("current_concept") val currentConcept: SimConcept? = null,
    @SerializedName("all_concepts") val allConcepts: List<SimConcept> = emptyList(),
    @SerializedName("all_completed") val allCompleted: Boolean? = false,
    @SerializedName("previous_concept") val previousConcept: SimPreviousConcept? = null
)

data class SimConcept(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("key_insight") val keyInsight: String,
    @SerializedName("related_params") val relatedParams: List<String>
)

data class SimPreviousConcept(
    val id: String,
    val title: String,
    val completed: Boolean
)

data class SimTeacherMessage(
    val text: String,
    val timestamp: String,
    @SerializedName("requires_response") val requiresResponse: Boolean,
    @SerializedName("correction_made") val correctionMade: Boolean? = false,
    @SerializedName("asks_for_reasoning") val asksForReasoning: Boolean? = false,
    @SerializedName("concept_transition") val conceptTransition: Boolean? = false,
    @SerializedName("session_ending") val sessionEnding: Boolean? = false
)

data class SimLearningState(
    @SerializedName("understanding_level") val understandingLevel: String,
    @SerializedName("understanding_reasoning") val understandingReasoning: String? = null,
    @SerializedName("exchange_count") val exchangeCount: Int,
    @SerializedName("concept_complete") val conceptComplete: Boolean,
    @SerializedName("session_complete") val sessionComplete: Boolean,
    val strategy: String,
    @SerializedName("teacher_mode") val teacherMode: String,
    @SerializedName("trajectory_status") val trajectoryStatus: String? = null,
    @SerializedName("needs_deeper") val needsDeeper: Boolean? = false
)

// ==================== MATH AGENT DATA CLASSES ====================

data class ProblemsListResponse(
    val success: Boolean,
    val problems: List<MathProblem> = emptyList(),
    val total: Int = 0,
    val message: String = "Available problems retrieved successfully."
)

data class MathProblem(
    @SerializedName("problem_id") val problemId: String,
    @SerializedName("topic") val topic: String,
    @SerializedName("difficulty") val difficulty: String? = null,
)

data class MathStartSessionRequest(
    @SerializedName("problem_id") val problemId: String,
    @SerializedName("student_id") val studentId: String? = null,
    @SerializedName("session_label") val sessionLabel: String? = null,
    @SerializedName("is_kannada") val isKannada: Boolean = false
)

data class MathStartSessionResponse(
    val success: Boolean,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("problem_id") val problemId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("agent_response") val agentResponse: String,
    @SerializedName("current_state") val currentState: String,
    val message: String = "Session started successfully. Agent is ready for student input.",
    val metadata: SessionMetadata = SessionMetadata()
)

data class MathContinueSessionRequest(
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("user_message") val userMessage: String,
    @SerializedName("is_kannada") val isKannada: Boolean = false,
    val image: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("student_id") val studentId: String? = null
)

data class MathContinueSessionResponse(
    val success: Boolean,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("agent_response") val agentResponse: String,
    @SerializedName("current_state") val currentState: String,
    val metadata: SessionMetadata = SessionMetadata(),
    val message: String
)

data class MathSessionStatusResponse(
    val success: Boolean,
    @SerializedName("thread_id") val threadId: String,
    val exists: Boolean,
    @SerializedName("current_state") val currentState: String? = null,
    @SerializedName("problem_id") val problemId: String? = null,
    val progress: JsonElement? = null,
    val message: String = "Status retrieved successfully."
)

data class MathSessionHistoryResponse(
    val success: Boolean,
    @SerializedName("thread_id") val threadId: String,
    val exists: Boolean,
    val messages: List<SessionMessage> = emptyList(),
    @SerializedName("node_transitions") val nodeTransitions: List<NodeTransition> = emptyList(),
    @SerializedName("problem_id") val problemId: String? = null,
    val message: String = "History retrieved successfully."
)

data class SessionMessage(
    val role: String,
    val content: String,
    val node: String? = null,
    val timestamp: String? = null
)

data class NodeTransition(
    val from: String? = null,
    val to: String? = null,
    val timestamp: String? = null,
    val reason: String? = null
)
// ==================== WHITEBOARD DATA CLASSES ====================
data class WbConceptInfo(
    @SerializedName("concept_id") val conceptId: String,
    val title: String,
    val chapter: Int,
    @SerializedName("has_scene") val hasScene: Boolean,
    @SerializedName("scene_id") val sceneId: String? = null
)

data class WbConceptsResponse(
    val success: Boolean,
    val concepts: List<WbConceptInfo>,
    val total: Int,
    val message: String? = null
)

data class WbStartSessionRequest(
    @SerializedName("concept_id") val conceptId: String,
    @SerializedName("student_id") val studentId: String? = null
)

data class WbContinueSessionRequest(
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("user_message") val userMessage: String
)

// FIX: Made all dynamic whiteboard fields deeply nullable to prevent Gson parsing crashes
data class WbApiTutorTurn(
    val message: String,
    @SerializedName("current_node") val currentNode: String? = null,
    @SerializedName("current_state") val currentState: String? = null,
    @SerializedName("whiteboard_summary") val whiteboardSummary: String? = null,
    @SerializedName("whiteboard_state") val whiteboardState: WbStateResponse? = null,
    @SerializedName("reveal_timeline") val revealTimeline: List<WbRevealTimelineUnit>? = null,
    val metadata: Map<String, Any>? = null
)

data class WbStateResponse(
    val objects: Map<String, WbBackendWhiteboardObject>? = null
)

data class WbBackendWhiteboardObject(
    val instance_id: String? = null,
    val object_type_id: String? = null,
    val props: WbWhiteboardProps? = null
)

data class WbWhiteboardProps(
    val title: String? = null,
    val text: String? = null,
    val steps: List<String>? = null,
    val scene_id: String? = null,
    val base_layer_id: String? = null
)

data class WbRevealTimelineUnit(
    @SerializedName("unit_id") val unitId: String,
    @SerializedName("object_id") val objectId: String,
    @SerializedName("unit_type") val unitType: String,
    @SerializedName("unit_index") val unitIndex: Int,
    @SerializedName("part_id") val partId: String?,
    val cue: String,
    @SerializedName("trigger_char_index") val triggerCharIndex: Int
)

data class WbSessionTurnResponse(
    val success: Boolean,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("thread_id") val threadId: String,
    val concept: WbConceptInfo,
    val turn: WbApiTutorTurn,
    val message: String? = null
)

data class WbSessionStatusResponse(
    val success: Boolean,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("thread_id") val threadId: String,
    val concept: WbConceptInfo,
    @SerializedName("current_node") val currentNode: String? = null,
    @SerializedName("current_state") val currentState: String? = null,
    @SerializedName("board_revision") val boardRevision: Int? = null,
    @SerializedName("whiteboard_state") val whiteboardState: WbStateResponse? = null,
    val message: String? = null
)

data class WbSessionHistoryResponse(
    val success: Boolean,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("thread_id") val threadId: String,
    val concept: WbConceptInfo,
    val messages: List<SessionMessage>,
    val message: String? = null
)

data class WbDeleteSessionResponse(
    val success: Boolean,
    @SerializedName("thread_id") val threadId: String,
    val message: String? = null
)

data class WbSceneManifestResponse(
    val success: Boolean,
    @SerializedName("schema_version") val schemaVersion: String,
    val scenes: List<Map<String, Any>>,
    val total: Int,
    val message: String? = null
)