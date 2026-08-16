package com.ncert7.aitutorandlab.service.sync

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.GardenDao
import com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity
import com.ncert7.aitutorandlab.data.local.entities.GardenTheme
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.ncert7.aitutorandlab.service.sync.GardenRestorePolicy.Outcome

/**
 * Mirrors the garden/space reward state (theme, route, steps, preferred slot, and every planted item)
 * to Firestore so it survives reinstall / a new device. Push runs with the normal full sync; restore
 * runs once on login and only when local has no plants and no in-progress steps (never clobbers
 * real local progress). Starter placeholder rows (route `"1"`, onboarding theme) are overwrite-safe
 * so remote gardens are not silently skipped (Bug A).
 */
class GardenSyncManager(
    private val gardenDao: GardenDao,
    private val firebaseRepository: FirebaseRepository,
) {

    suspend fun pushGarden(studentId: String) {
        if (studentId.isBlank()) return
        try {
            gardenDao.getState(studentId)?.let { state ->
                firebaseRepository.saveGardenState(
                    userId = studentId,
                    theme = state.theme,
                    route = state.route,
                    steps = state.steps,
                    preferredSlot = state.preferredSlot,
                )
            }
            val items = gardenDao.getAllItems(studentId)
            if (items.isNotEmpty()) {
                firebaseRepository.saveGardenItems(studentId, items.map { it.toPayload() })
            }
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "pushGarden failed: ${e.message}")
        }
    }

    /**
     * Pull remote garden into Room when local is empty of plants and steps.
     * Returns [Outcome] for login gates + telemetry.
     */
    suspend fun restoreGarden(studentId: String): Outcome {
        if (studentId.isBlank()) return Outcome.REMOTE_EMPTY
        return try {
            val remoteState = firebaseRepository.getGardenState(studentId)
            val remoteItems = firebaseRepository.getGardenItems(studentId)
            if (remoteState == null && remoteItems.isEmpty()) {
                logSkipped(Outcome.REMOTE_EMPTY)
                return Outcome.REMOTE_EMPTY
            }

            val localItems = gardenDao.countItems(studentId)
            val localSteps = gardenDao.getState(studentId)?.steps ?: 0
            if (!GardenRestorePolicy.canRestoreFromRemote(localItems, localSteps)) {
                logSkipped(Outcome.SKIPPED_LOCAL_PROGRESS, localItems)
                return Outcome.SKIPPED_LOCAL_PROGRESS
            }

            // Remote theme / route / slot win over any local starter or onboarding placeholder (R.1).
            remoteState?.let { remote ->
                gardenDao.upsertState(
                    GardenStateEntity(
                        studentId = studentId,
                        theme = remote["theme"] as? String ?: GardenTheme.GARDEN,
                        route = remote["route"] as? String ?: "0",
                        steps = (remote["steps"] as? Number)?.toInt() ?: 0,
                        preferredSlot = (remote["preferredSlot"] as? Number)?.toInt() ?: -1,
                    ),
                )
            }
            remoteItems.forEach { map ->
                map.toGrownItem(studentId)?.let { item ->
                    try {
                        gardenDao.insertItem(item)
                    } catch (_: Exception) {
                        // ABORT on conflict — a duplicate id already exists; safe to skip.
                    }
                }
            }
            val appliedCount = gardenDao.countItems(studentId)
            EngagementAnalyticsTracker.restoreApplied(
                domain = DOMAIN,
                itemCount = appliedCount,
            )
            DebugLogger.debugLog(TAG, "Garden restored for $studentId items=$appliedCount")
            Outcome.APPLIED
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "restoreGarden failed: ${e.message}")
            EngagementAnalyticsTracker.restoreSkipped(DOMAIN, Outcome.ERROR.reason)
            Outcome.ERROR
        }
    }

    private fun logSkipped(outcome: Outcome, itemCount: Int = 0) {
        EngagementAnalyticsTracker.restoreSkipped(DOMAIN, outcome.reason)
        DebugLogger.debugLog(TAG, "Garden restore skipped reason=${outcome.reason} items=$itemCount")
    }

    private fun GrownItemEntity.toPayload(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "studentId" to studentId,
            "zone" to zone,
            "plot" to plot,
            "slot" to slot,
            "conceptId" to conceptId,
            "chapterId" to chapterId,
            "kind" to kind,
            "completedAt" to completedAt,
            // Required by the Firestore rule (studentIdMatchesParent → isEduAiWrite).
            "appName" to AppConfig.APP_NAME,
        )

    private fun Map<String, Any?>.toGrownItem(studentId: String): GrownItemEntity? {
        val id = this["id"] as? String ?: return null
        return GrownItemEntity(
            id = id,
            studentId = (this["studentId"] as? String)?.takeIf { it.isNotBlank() } ?: studentId,
            zone = (this["zone"] as? Number)?.toInt() ?: 0,
            plot = (this["plot"] as? Number)?.toInt() ?: 0,
            slot = (this["slot"] as? Number)?.toInt() ?: 0,
            conceptId = this["conceptId"] as? String ?: "",
            chapterId = this["chapterId"] as? String ?: "",
            kind = this["kind"] as? String ?: "",
            completedAt = (this["completedAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    companion object {
        private const val TAG = "GardenSyncManager"
        private const val DOMAIN = "garden"
    }
}
