package com.ncert7.aitutorandlab.service.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.SimulationInteractionDao
import com.ncert7.aitutorandlab.data.local.entities.SimulationInteractionEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimulationSyncManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val interactionDao: SimulationInteractionDao,
    private val sharedPreferenceUtils: SharedPreferenceUtils
) {
    suspend fun syncTodayIfNeeded(): SyncResult {
        val studentId = sharedPreferenceUtils.getUserId()
        if (studentId.isNullOrBlank()) {
            return SyncResult(success = true, message = "No student ID found")
        }

        val today = currentDate()
        val unsyncedInteractions = interactionDao.getUnsyncedInteractionsForDay(studentId, today)
        if (unsyncedInteractions.isEmpty()) {
            return SyncResult(success = true, message = "No unsynced simulation interactions")
        }

        val interactions = interactionDao.getInteractionsForDay(studentId, today)

        return try {
            val docRef = firestore
                .collection(COLLECTION)
                .document(studentId)
                .collection("daily")
                .document(today)

            val data = mapOf(
                "studentId" to studentId,
                "interactionDate" to today,
                "appName" to AppConfig.APP_NAME,
                "updatedAt" to System.currentTimeMillis(),
                "syncedCount" to interactions.size,
                "interactions" to interactions.map { it.toFirestoreMap() }
            )

            docRef.set(data, SetOptions.merge()).await()
            interactionDao.markInteractionsAsSynced(unsyncedInteractions.map { it.interactionId })
            sharedPreferenceUtils.setSimulationLastSyncedDate(today)

            val message = "Synced ${interactions.size} simulation interactions for $today"
            DebugLogger.debugLog(TAG, message)
            SyncResult(success = true, message = message)
        } catch (e: Exception) {
            val message = "Simulation interaction sync failed: ${e.message}"
            DebugLogger.errorLog(TAG, message)
            SyncResult(success = false, message = message)
        }
    }

    private fun SimulationInteractionEntity.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "interactionId" to interactionId,
            "sessionId" to sessionId,
            "simulationTitle" to simulationTitle,
            "subjectName" to subjectName,
            "chapterName" to chapterName,
            "elementClicked" to elementClicked,
            "elementType" to elementType,
            "givenAnswer" to givenAnswer,
            "isCorrect" to isCorrect,
            "timeTaken" to timeTaken,
            "timestamp" to timestamp,
            "occurredAt" to occurredAt,
            "interactionDate" to interactionDate,
            "appName" to appName
        )
    }

    private fun currentDate(): String = dateFormat.format(Date())

    companion object {
        private const val TAG = "SimulationSyncManager"
        private const val COLLECTION = "simulation_interactions"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
}
