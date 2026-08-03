package com.ncert7.aitutorandlab.service.sync

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.GardenDao
import com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity
import com.ncert7.aitutorandlab.data.local.entities.GardenTheme
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.FirebaseRepository

/**
 * Mirrors the garden/space reward state (theme, route, steps, preferred slot, and every planted item)
 * to Firestore so it survives reinstall / a new device. Push runs with the normal full sync; restore
 * runs once on login and only when the local garden is empty (never clobbers a device with progress).
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

    /** Pull remote garden into Room when local is empty or still at the default placeholder row. */
    suspend fun restoreGarden(studentId: String) {
        if (studentId.isBlank()) return
        try {
            val remoteState = firebaseRepository.getGardenState(studentId)
            val remoteItems = firebaseRepository.getGardenItems(studentId)
            if (remoteState == null && remoteItems.isEmpty()) return
            if (!canRestoreFromRemote(studentId)) return

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
            DebugLogger.debugLog(TAG, "Garden restored for $studentId")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "restoreGarden failed: ${e.message}")
        }
    }

    /** True when local has no real garden progress (safe to hydrate from Firestore). */
    private suspend fun canRestoreFromRemote(studentId: String): Boolean {
        if (gardenDao.countItems(studentId) > 0) return false
        val state = gardenDao.getState(studentId) ?: return true
        return isPristinePlaceholder(state)
    }

    /** Default row created by [com.ncert7.aitutorandlab.repository.GardenRepository.ensureState]. */
    private fun isPristinePlaceholder(state: GardenStateEntity): Boolean =
        state.steps == 0 &&
            state.theme == GardenTheme.GARDEN &&
            state.route == "0" &&
            state.preferredSlot == -1

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
    }
}
