package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.SimulationInteractionDao
import com.ncert7.aitutorandlab.data.local.entities.SimulationInteractionEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.InteractionEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimulationInteractionRepository(
    private val interactionDao: SimulationInteractionDao,
    private val sharedPreferenceUtils: SharedPreferenceUtils
) {
    suspend fun saveInteraction(event: InteractionEvent, occurredAt: Long = System.currentTimeMillis()) {
        val studentId = sharedPreferenceUtils.getUserId().orEmpty()
        val sessionId = sharedPreferenceUtils.getCurrentSession().orEmpty()

        if (studentId.isBlank() || sessionId.isBlank()) {
            DebugLogger.debugLog(TAG, "Skipping interaction save: missing studentId or sessionId")
            return
        }

        interactionDao.insertInteraction(
            SimulationInteractionEntity(
                studentId = studentId,
                sessionId = sessionId,
                simulationTitle = event.simulationTitle,
                subjectName = event.subjectName,
                chapterName = event.chapterName,
                elementClicked = event.elementClicked,
                elementType = event.elementType,
                givenAnswer = event.givenAnswer,
                isCorrect = event.isCorrect,
                timeTaken = event.timeTaken,
                timestamp = event.timestamp,
                occurredAt = occurredAt,
                interactionDate = dayFormat.format(Date(occurredAt)),
                appName = AppConfig.APP_NAME,
                isSynced = false
            )
        )
    }

    suspend fun updateLatestPendingVerdict(verdict: String) {
        val sessionId = sharedPreferenceUtils.getCurrentSession().orEmpty()
        if (sessionId.isBlank()) return
        interactionDao.updateLatestPendingVerdict(sessionId, verdict)
    }

    suspend fun updateLatestSessionTime(
        simulationTitle: String,
        subjectName: String,
        chapterName: String,
        timeTaken: String
    ) {
        val sessionId = sharedPreferenceUtils.getCurrentSession().orEmpty()
        if (sessionId.isBlank()) return
        interactionDao.updateLatestSessionTime(
            sessionId = sessionId,
            simulationTitle = simulationTitle,
            subjectName = subjectName,
            chapterName = chapterName,
            timeTaken = timeTaken
        )
    }

    companion object {
        private const val TAG = "SimulationInteractionRepo"
        private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
}
