package com.ncert7.aitutorandlab.repository

import com.anurag.eduai.uikit.garden.quest.GardenPlantedRow
import com.anurag.eduai.uikit.garden.quest.GardenSceneSnapshot
import com.anurag.eduai.uikit.garden.quest.SLOTS_PER_ZONE
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.STEPS_PER_TASK
import com.anurag.eduai.uikit.garden.quest.PREFERRED_SLOT_SURPRISE
import com.anurag.eduai.uikit.garden.quest.STARTER_GARDEN_ZONE
import com.anurag.eduai.uikit.garden.quest.starterSlot
import com.anurag.eduai.uikit.garden.quest.starterZone
import com.anurag.eduai.uikit.garden.quest.ZONE_CAPACITY
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.world.hash
import com.ncert7.aitutorandlab.data.local.dao.GardenDao
import com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity
import com.ncert7.aitutorandlab.data.local.entities.GardenTheme
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.garden.GardenMomentCoordinator
import com.ncert7.aitutorandlab.domain.garden.GardenProgress
import com.ncert7.aitutorandlab.domain.garden.GardenRouteUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@Singleton
class GardenRepository @Inject constructor(
    private val gardenDao: GardenDao,
) {
    /**
     * Records one learning step from a trial increment. On the 7th step, plants a row
     * (idempotent on [trialItemId]). Does not touch XP, gems, or streak.
     */
    suspend fun recordStep(
        studentId: String,
        trialItemId: Long,
        conceptId: String,
        chapterId: String,
        kind: String,
    ): GrownItemEntity? {
        if (studentId.isBlank()) return null

        val state = ensureState(studentId)
        val nextSteps = state.steps + 1
        if (nextSteps < STEPS_PER_TASK) {
            gardenDao.updateSteps(studentId, nextSteps)
            return null
        }

        val itemKey = trialItemId.toString()
        if (gardenDao.getItem(itemKey) != null) {
            // Trial item already planted once — credit one step toward the next plant instead.
            val creditedSteps =
                if (state.steps >= STEPS_PER_TASK - 1) {
                    0
                } else {
                    state.steps + 1
                }
            gardenDao.updateSteps(studentId, creditedSteps)
            return null
        }

        val zone = resolvePlantZone(studentId, state)
        if (zone < 0) {
            DebugLogger.debugLog(TAG, "Garden all places full — holding step at 6")
            gardenDao.updateSteps(studentId, STEPS_PER_TASK - 1)
            return null
        }

        val plot = nextFreePlot(studentId, zone)
        if (plot < 0) {
            DebugLogger.debugLog(TAG, "Garden place full at zone $zone — holding step at 6")
            gardenDao.updateSteps(studentId, STEPS_PER_TASK - 1)
            return null
        }

        val freshState = gardenDao.getState(studentId) ?: state
        val plantIndex = gardenDao.countItems(studentId)
        val slot = resolveSlot(freshState, plantIndex)
        val planted =
            GrownItemEntity(
                id = itemKey,
                studentId = studentId,
                zone = zone,
                plot = plot,
                slot = slot,
                conceptId = conceptId,
                chapterId = chapterId,
                kind = kind,
                completedAt = System.currentTimeMillis(),
            )
        return try {
            gardenDao.insertItem(planted)
            gardenDao.updateSteps(studentId, 0)
            planted
        } catch (e: Exception) {
            DebugLogger.debugLog(TAG, "Garden plant skipped (duplicate?): ${e.message}")
            null
        }
    }

    suspend fun getProgress(studentId: String): GardenProgress? {
        if (studentId.isBlank()) return null
        val state = normalizeStarterSlot(studentId, ensureState(studentId))
        val zone = GardenRouteUtils.currentZone(state.route)
        val unlocked = GardenRouteUtils.parseUnlockedZones(state.route)
        val filled = gardenDao.getItemsInZone(studentId, zone).size
        val filledByZone =
            unlocked.associateWith { z -> gardenDao.getItemsInZone(studentId, z).size }
        return GardenProgress(
            steps = state.steps,
            stepsPerPlant = STEPS_PER_TASK,
            totalPlanted = gardenDao.countItems(studentId),
            currentZone = zone,
            filledInZone = filled,
            zoneCapacity = ZONE_CAPACITY,
            theme = state.theme,
            preferredSlot = state.preferredSlot,
            unlockedZones = unlocked,
            filledCountByZone = filledByZone,
        )
    }

    fun observeProgress(studentId: String): Flow<GardenProgress?> =
        gardenDao.observeState(studentId).flatMapLatest { state ->
            flow {
                emit(
                    when {
                        studentId.isBlank() || state == null -> null
                        else -> getProgress(studentId)
                    },
                )
            }
        }.distinctUntilChanged()

    /** Locks in what grows next on the home rail (-1 = surprise). */
    suspend fun setPreferredSlot(studentId: String, slot: Int) {
        if (studentId.isBlank()) return
        ensureState(studentId)
        val normalized =
            if (slot in 0 until SLOTS_PER_ZONE) {
                slot
            } else {
                -1
            }
        gardenDao.updatePreferredSlot(studentId, normalized)
    }

    /** Re-queue a plant celebration if a plant landed but the trial screen never showed it. */
    suspend fun queueCelebrationForUnshownPlant(
        studentId: String,
        lastCelebratedPlantTotal: Int,
        coordinator: GardenMomentCoordinator,
    ): Boolean {
        if (studentId.isBlank()) return false
        if (coordinator.pending.value != null) return false
        val progress = getProgress(studentId) ?: return false
        if (progress.totalPlanted <= lastCelebratedPlantTotal) return false
        val planted = gardenDao.getLatestItem(studentId) ?: return false
        val placeCompleted = progress.filledInZone >= progress.zoneCapacity
        coordinator.notifyPlanted(
            planted = planted,
            progress = progress,
            placeCompleted = placeCompleted,
        )
        return true
    }

    suspend fun getPlantedItems(studentId: String): List<GrownItemEntity> {
        if (studentId.isBlank()) return emptyList()
        return gardenDao.getAllItems(studentId)
    }

    suspend fun getSceneSnapshot(studentId: String): GardenSceneSnapshot? {
        if (studentId.isBlank()) return null
        val progress = getProgress(studentId) ?: return null
        val rows =
            gardenDao.getAllItems(studentId).map { row ->
                GardenPlantedRow(zone = row.zone, plot = row.plot, slot = row.slot)
            }
        return GardenSceneSnapshot(
            currentZone = progress.currentZone,
            steps = progress.steps,
            preferredSlot = progress.preferredSlot,
            planted = rows,
            previewSeed = progress.totalPlanted,
        )
    }

    suspend fun setTheme(studentId: String, theme: String) {
        if (studentId.isBlank()) return
        ensureState(studentId)
        gardenDao.updateTheme(studentId, theme)
    }

    /**
     * First-run starting scene: Woodland for garden, Mars for space. Growth mode defaults to
     * surprise; cherry blossom / solar array are highlighted as the free pick in the scene picker.
     */
    suspend fun applyOnboardingStartingScene(studentId: String, theme: String) {
        if (studentId.isBlank() || gardenDao.countItems(studentId) > 0) return
        ensureState(studentId)
        val composeTheme = toComposeTheme(theme)
        val zone = composeTheme.starterZone()
        gardenDao.updateRoute(studentId, zone.toString())
        gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
    }

    /** Unlocks a zone on the route when a place is completed (expressive picker). */
    suspend fun unlockZoneIfNeeded(studentId: String, zone: Int) {
        if (studentId.isBlank() || zone !in ZONES.indices) return
        val state = ensureState(studentId)
        appendZoneToRoute(studentId, state.route, zone)
    }

    fun toComposeTheme(theme: String): Theme =
        when (theme.uppercase()) {
            GardenTheme.OUTPOST -> Theme.OUTPOST
            GardenTheme.ISLAND -> Theme.ISLAND
            GardenTheme.COLONY -> Theme.COLONY
            else -> Theme.GARDEN
        }

    private suspend fun ensureState(studentId: String): GardenStateEntity {
        val existing = gardenDao.getState(studentId)
        if (existing != null) return existing
        val fresh =
            GardenStateEntity(
                studentId = studentId,
                theme = GardenTheme.GARDEN,
                route = STARTER_GARDEN_ZONE.toString(),
                steps = 0,
                preferredSlot = PREFERRED_SLOT_SURPRISE,
            )
        gardenDao.upsertState(fresh)
        return fresh
    }

    private suspend fun normalizeStarterSlot(
        studentId: String,
        state: GardenStateEntity,
    ): GardenStateEntity {
        if (gardenDao.countItems(studentId) > 0) return state
        val composeTheme = toComposeTheme(state.theme)
        if (composeTheme != Theme.GARDEN && composeTheme != Theme.OUTPOST) return state

        val expectedZone = composeTheme.starterZone()
        var updated = state

        if (GardenRouteUtils.currentZone(state.route) != expectedZone) {
            gardenDao.updateRoute(studentId, expectedZone.toString())
            updated = updated.copy(route = expectedZone.toString())
        }
        return updated
    }

    private suspend fun resolvePlantZone(studentId: String, state: GardenStateEntity): Int {
        var zone = GardenRouteUtils.currentZone(state.route)
        while (nextFreePlot(studentId, zone) < 0) {
            val next = zone + 1
            if (next >= ZONES.size) return -1
            val route = gardenDao.getState(studentId)?.route ?: state.route
            appendZoneToRoute(studentId, route, next)
            zone = next
        }
        return zone
    }

    private suspend fun appendZoneToRoute(studentId: String, currentRoute: String, zone: Int) {
        if (GardenRouteUtils.currentZone(currentRoute) >= zone) return
        val newRoute =
            if (currentRoute.isBlank()) {
                zone.toString()
            } else {
                "$currentRoute,$zone"
            }
        gardenDao.updateRoute(studentId, newRoute)
    }

    private suspend fun nextFreePlot(studentId: String, zone: Int): Int {
        val used = gardenDao.getItemsInZone(studentId, zone).map { it.plot }.toSet()
        return (0 until ZONE_CAPACITY).firstOrNull { it !in used } ?: -1
    }

    private fun resolveSlot(state: GardenStateEntity, plantIndex: Int): Int {
        if (state.preferredSlot in 0 until SLOTS_PER_ZONE) return state.preferredSlot
        return (hash(plantIndex, 11) * SLOTS_PER_ZONE).toInt().coerceIn(0, SLOTS_PER_ZONE - 1)
    }

    companion object {
        private const val TAG = "GardenRepository"
    }
}
