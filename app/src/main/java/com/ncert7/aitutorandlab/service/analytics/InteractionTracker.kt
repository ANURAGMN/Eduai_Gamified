package com.ncert7.aitutorandlab.service.analytics

import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.SimulationInteractionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InteractionEvent(
    val simulationTitle: String,
    val subjectName: String,
    val chapterName: String,
    val elementClicked: String,
    val elementType: String,
    val givenAnswer: String,
    val isCorrect: String,
    val timeTaken: String,
    val timestamp: String
)

object InteractionTracker {
    private const val TAG = "InteractionTracker"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repository: SimulationInteractionRepository? = null

    private var sessionTitle: String = ""
    private var sessionSubject: String = ""
    private var sessionChapter: String = ""
    private var sessionStartMs: Long = 0L
    private var sessionTimestamp: String = ""
    private var isFirstEventOfSession: Boolean = false

    private val _events = MutableStateFlow<List<InteractionEvent>>(emptyList())
    val events: StateFlow<List<InteractionEvent>> = _events.asStateFlow()

    private val _totalInteractions = MutableStateFlow(0)
    val totalInteractions: StateFlow<Int> = _totalInteractions.asStateFlow()

    /** Resets when [startSession] is called — use for exam-trial 7-click threshold. */
    private val _sessionInteractionCount = MutableStateFlow(0)
    val sessionInteractionCount: StateFlow<Int> = _sessionInteractionCount.asStateFlow()

    /** Trackable click targets reported by injected simulation JS (0 until page reports). */
    private val _sessionInteractionBudget = MutableStateFlow(0)
    val sessionInteractionBudget: StateFlow<Int> = _sessionInteractionBudget.asStateFlow()

    /** Emits every time a verdict lands (true = correct, false = wrong). Drives the adaptive coach. */
    private val _verdicts = MutableSharedFlow<Boolean>(extraBufferCapacity = 16)
    val verdicts: SharedFlow<Boolean> = _verdicts.asSharedFlow()

    /** When false, interactions are logged but do not increment [sessionInteractionCount]. */
    private var sessionCountingEnabled = true

    fun initialize(simulationInteractionRepository: SimulationInteractionRepository) {
        repository = simulationInteractionRepository
        DebugLogger.debugLog(TAG, "Initialized")
    }

    fun startSession(
        simulationTitle: String,
        subjectName: String,
        chapterName: String
    ) {
        if (sessionStartMs > 0L) {
            endSession()
        }
        sessionTitle = simulationTitle
        sessionSubject = subjectName
        sessionChapter = chapterName
        sessionStartMs = System.currentTimeMillis()
        sessionTimestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        isFirstEventOfSession = true
        _sessionInteractionCount.value = 0
        _sessionInteractionBudget.value = 0
        sessionCountingEnabled = true
    }

    fun reportInteractionBudget(count: Int) {
        if (count <= 0) return
        _sessionInteractionBudget.value = count
        DebugLogger.debugLog(TAG, "HTML interaction budget: $count")
    }

    fun setSessionCountingEnabled(enabled: Boolean) {
        sessionCountingEnabled = enabled
    }

    fun endSession() {
        if (sessionStartMs <= 0L) return

        val current = _events.value
        if (current.isEmpty()) {
            sessionStartMs = 0L
            return
        }

        val lastIndex = current.indexOfLast {
            it.simulationTitle == sessionTitle &&
                it.subjectName == sessionSubject &&
                it.chapterName == sessionChapter
        }
        if (lastIndex < 0) {
            sessionStartMs = 0L
            return
        }

        val elapsedSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000).coerceAtLeast(0)
        val elapsedTime = "${elapsedSeconds}s"
        val updated = current.toMutableList()
        updated[lastIndex] = updated[lastIndex].copy(timeTaken = elapsedTime)
        _events.value = updated
        repository?.let { repo ->
            scope.launch {
                repo.updateLatestSessionTime(
                    simulationTitle = sessionTitle,
                    subjectName = sessionSubject,
                    chapterName = sessionChapter,
                    timeTaken = elapsedTime
                )
            }
        }
        sessionStartMs = 0L
    }

    fun logInteraction(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) return

        val elementType: String
        val elementClicked: String
        val givenAnswer: String

        when {
            name.startsWith("Slider [") -> {
                elementType = "slider"
                val labelEnd = name.indexOf(']')
                elementClicked = if (labelEnd > 8) name.substring(8, labelEnd) else name
                val setToIndex = name.indexOf("set to: ")
                givenAnswer = if (setToIndex >= 0) name.substring(setToIndex + 8) else ""
            }
            name.startsWith("Entered [") -> {
                elementType = "input"
                val labelEnd = name.indexOf(']')
                elementClicked = if (labelEnd > 9) name.substring(9, labelEnd) else name
                val colonIndex = name.indexOf("]: ")
                givenAnswer = if (colonIndex >= 0) name.substring(colonIndex + 3) else ""
            }
            else -> {
                elementType = "tap"
                elementClicked = name
                givenAnswer = name
            }
        }

        val timestamp = if (isFirstEventOfSession) {
            isFirstEventOfSession = false
            sessionTimestamp
        } else {
            ""
        }

        val event = InteractionEvent(
            simulationTitle = sessionTitle,
            subjectName = sessionSubject,
            chapterName = sessionChapter,
            elementClicked = elementClicked,
            elementType = elementType,
            givenAnswer = givenAnswer,
            isCorrect = "-",
            timeTaken = "",
            timestamp = timestamp
        )

        _totalInteractions.value += 1
        if (sessionCountingEnabled) {
            _sessionInteractionCount.value += 1
        }
        _events.value = _events.value + event
        repository?.let { repo ->
            scope.launch {
                repo.saveInteraction(event)
            }
        }
    }

    fun logVerdict(isCorrect: Boolean) {
        val current = _events.value
        val lastPendingIndex = current.indexOfLast { it.isCorrect == "-" }
        if (lastPendingIndex < 0) return

        val verdict = if (isCorrect) "correct" else "wrong"
        val updated = current.toMutableList()
        updated[lastPendingIndex] = updated[lastPendingIndex].copy(isCorrect = verdict)
        _events.value = updated
        _verdicts.tryEmit(isCorrect)
        repository?.let { repo ->
            scope.launch {
                repo.updateLatestPendingVerdict(verdict)
            }
        }
    }
}
