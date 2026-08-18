package com.ncert7.aitutorandlab.repository

import com.anurag.eduai.uikit.garden.quest.GardenPlantedRow
import com.anurag.eduai.uikit.garden.quest.GardenSceneSnapshot
import com.anurag.eduai.uikit.garden.quest.SLOTS_PER_ZONE
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.STEPS_PER_TASK
import com.anurag.eduai.uikit.garden.quest.PREFERRED_SLOT_SURPRISE
import com.anurag.eduai.uikit.garden.quest.STARTER_GARDEN_ZONE
import com.anurag.eduai.uikit.garden.quest.placeBased
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
            scheduleGardenUpload()
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
            scheduleGardenUpload()
            return null
        }

        val placement = resolvePlantPlacement(studentId, state)
        if (placement == null) {
            DebugLogger.debugLog(TAG, "Garden all places full — holding step at 6")
            gardenDao.updateSteps(studentId, STEPS_PER_TASK - 1)
            scheduleGardenUpload()
            return null
        }
        val zone = placement.zone
        val plot = placement.plot

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
            scheduleGardenUpload()
            planted
        } catch (e: Exception) {
            DebugLogger.debugLog(TAG, "Garden plant skipped (duplicate?): ${e.message}")
            null
        }
    }

    /**
     * Grows the garden for any completed learning task (study / sim / revision / math / science),
     * not just plan-trial items. Plants one row **per completion** (a genuine re-do later grows
     * another plant), with a unique id `task:<concept>:<bucket>:<ts>`. `kind` is normalised to a
     * coarse bucket (STUDY / REVISION / SIM) so the same completion reported by two paths — the
     * Plan-trial DONE path (kinds like "SIM_URL") and the free-browse chapter-completion path
     * ("SIMULATION") — converges. To avoid double-planting the *same* completion (those two paths,
     * plus burst DONE calls), a plant for the same concept+bucket within
     * [COMPLETION_DEDUP_WINDOW_MS] is skipped. Uses the same zone/plot/surprise-slot resolution as
     * [recordStep]; does not touch XP, gems, or streak.
     */
    suspend fun recordCompletion(
        studentId: String,
        conceptId: String,
        chapterId: String,
        kind: String,
    ): GrownItemEntity? {
        if (studentId.isBlank() || conceptId.isBlank()) return null

        // Normalise to a coarse activity bucket (STUDY / REVISION / SIM) so the same completion
        // reported by two paths converges: Plan-trial DONE passes kinds like "SIM_URL"; the
        // free-browsing chapter-completion path passes "SIMULATION" — both map to one bucket.
        val bucket = taskKindBucket(kind)

        // Per-completion dedup (not per-concept-forever): a single completion can fire several times
        // — Plan-DONE + chapter-completion, and repeated DONE calls in a burst — so collapse anything
        // for the same concept+bucket within a short window. A genuine re-do later still grows a new
        // plant, so every completion increments the garden.
        val now = System.currentTimeMillis()
        val recent = gardenDao.getLatestItemForTask(studentId, conceptId, bucket)
        if (recent != null && now - recent.completedAt < COMPLETION_DEDUP_WINDOW_MS) {
            return null
        }
        val itemKey = "task:$conceptId:$bucket:$now" // unique per plant

        val state = ensureState(studentId)
        val placement = resolvePlantPlacement(studentId, state)
        if (placement == null) {
            DebugLogger.debugLog(TAG, "Garden all places full — completion plant skipped")
            return null
        }

        val freshState = gardenDao.getState(studentId) ?: state
        val plantIndex = gardenDao.countItems(studentId)
        val slot = resolveSlot(freshState, plantIndex)
        val planted =
            GrownItemEntity(
                id = itemKey,
                studentId = studentId,
                zone = placement.zone,
                plot = placement.plot,
                slot = slot,
                conceptId = conceptId,
                chapterId = chapterId,
                kind = bucket,
                completedAt = now,
            )
        return try {
            gardenDao.insertItem(planted)
            // Each task now yields a whole plant, so the partial sprout counter resets.
            gardenDao.updateSteps(studentId, 0)
            scheduleGardenUpload()
            planted
        } catch (e: Exception) {
            DebugLogger.debugLog(TAG, "Garden completion plant skipped (duplicate?): ${e.message}")
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
        scheduleGardenUpload()
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
        val state = ensureState(studentId)
        val normalized = theme.uppercase()
        if (state.theme.equals(normalized, ignoreCase = true)) return
        gardenDao.updateTheme(studentId, normalized)
        // Each journey starts on Surprise — don't carry a locked plant/module pick across themes.
        gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
        scheduleGardenUpload()
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
        scheduleGardenUpload()
    }

    /** Unlocks a zone on the route when a place is completed (expressive picker). */
    suspend fun unlockZoneIfNeeded(studentId: String, zone: Int) {
        if (studentId.isBlank() || zone !in ZONES.indices) return
        val state = ensureState(studentId)
        val alreadyCurrent = GardenRouteUtils.currentZone(state.route) == zone
        appendZoneToRoute(studentId, state.route, zone)
        if (!alreadyCurrent) {
            // New place → Surprise again (same default as a fresh journey).
            gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
        }
        scheduleGardenUpload()
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
        val composeTheme = toComposeTheme(state.theme)
        var updated = state

        // Island / colony have nothing to pick — keep growth mode on Surprise.
        if (!composeTheme.placeBased && state.preferredSlot != PREFERRED_SLOT_SURPRISE) {
            gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
            updated = updated.copy(preferredSlot = PREFERRED_SLOT_SURPRISE)
        }

        if (gardenDao.countItems(studentId) > 0) return updated
        if (composeTheme != Theme.GARDEN && composeTheme != Theme.OUTPOST) return updated

        val expectedZone = composeTheme.starterZone()

        if (GardenRouteUtils.currentZone(updated.route) != expectedZone) {
            gardenDao.updateRoute(studentId, expectedZone.toString())
            updated = updated.copy(route = expectedZone.toString())
        }
        if (updated.preferredSlot != PREFERRED_SLOT_SURPRISE) {
            gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
            updated = updated.copy(preferredSlot = PREFERRED_SLOT_SURPRISE)
        }
        return updated
    }

    private data class PlantPlacement(val zone: Int, val plot: Int)

    /**
     * Picks where the next plant/module goes:
     * 1. Fill the current place, then walk forward unlocking the next.
     * 2. If the forward path is exhausted, use any place that still has a free plot
     *    (e.g. zone 0 / Luna when the journey started at Woodland/Mars and never visited it).
     *    Switching GARDEN → OUTPOST so the learner "proceeds to space".
     * 3. If every place plot is full, switch to COLONY (spaceship) and overflow-plant so
     *    completions never stall.
     */
    private suspend fun resolvePlantPlacement(
        studentId: String,
        state: GardenStateEntity,
    ): PlantPlacement? {
        val zone = resolvePlantZone(studentId, state)
        if (zone >= 0) {
            val plot = nextFreePlot(studentId, zone)
            if (plot >= 0) return PlantPlacement(zone, plot)
        }
        // All 8×12 place plots taken → spaceship overflow.
        advanceThemeToColony(studentId)
        val overflowZone = (ZONES.size - 1).coerceAtLeast(0)
        val overflowPlot = ZONE_CAPACITY + gardenDao.countItems(studentId)
        val route = gardenDao.getState(studentId)?.route ?: state.route
        appendZoneToRoute(studentId, route, overflowZone)
        return PlantPlacement(overflowZone, overflowPlot)
    }

    private suspend fun resolvePlantZone(studentId: String, state: GardenStateEntity): Int {
        var zone = GardenRouteUtils.currentZone(state.route)
        while (zone in ZONES.indices && nextFreePlot(studentId, zone) < 0) {
            val next = zone + 1
            if (next >= ZONES.size) break
            val route = gardenDao.getState(studentId)?.route ?: state.route
            val alreadyCurrent = GardenRouteUtils.currentZone(route) == next
            appendZoneToRoute(studentId, route, next)
            if (!alreadyCurrent) {
                // New place → Surprise again (don't carry a locked plant across places).
                gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
            }
            zone = next
        }
        if (zone in ZONES.indices && nextFreePlot(studentId, zone) >= 0) {
            return zone
        }

        // Forward path full — any remaining free place (often the skipped starter index 0).
        val freeZone = ZONES.indices.firstOrNull { nextFreePlot(studentId, it) >= 0 } ?: return -1
        advanceThemePastGardenIfNeeded(studentId)
        val route = gardenDao.getState(studentId)?.route ?: state.route
        val alreadyCurrent = GardenRouteUtils.currentZone(route) == freeZone
        appendZoneToRoute(studentId, route, freeZone)
        if (!alreadyCurrent) {
            gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
        }
        return freeZone
    }

    /** Garden journey exhausted → continue growth under the Outpost (space) skin. */
    private suspend fun advanceThemePastGardenIfNeeded(studentId: String) {
        val theme = gardenDao.getState(studentId)?.theme ?: return
        if (!theme.equals(GardenTheme.GARDEN, ignoreCase = true)) return
        gardenDao.updateTheme(studentId, GardenTheme.OUTPOST)
        gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
        scheduleGardenUpload()
    }

    /** Every place plot filled → Colony / spaceship single-scene growth. */
    private suspend fun advanceThemeToColony(studentId: String) {
        val theme = gardenDao.getState(studentId)?.theme ?: return
        if (theme.equals(GardenTheme.COLONY, ignoreCase = true)) return
        gardenDao.updateTheme(studentId, GardenTheme.COLONY)
        gardenDao.updatePreferredSlot(studentId, PREFERRED_SLOT_SURPRISE)
        scheduleGardenUpload()
    }

    /** Focus [zone] by appending it as the journey's current place (may revisit earlier indices). */
    private suspend fun appendZoneToRoute(studentId: String, currentRoute: String, zone: Int) {
        if (GardenRouteUtils.currentZone(currentRoute) == zone) return
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

    /** Coarse activity bucket for the plant dedup key + label (STUDY / REVISION / SIM). */
    private fun taskKindBucket(kind: String): String =
        when (kind.uppercase()) {
            "STUDY", "CONCEPT", "MATH", "MATH_AGENT" -> "STUDY"
            "REVISION", "REVISION_AGENT" -> "REVISION"
            else -> "SIM" // SIMULATION, SIM_URL, SIMULATION_AGENT, SCIENCE, SCIENCE_AGENT
        }

    /**
     * Coalesced Firestore push (unique WorkManager KEEP + delay). Safe on the per-step hot path —
     * rapid [recordStep] calls collapse into one deferred upload.
     */
    private fun scheduleGardenUpload() {
        try {
            com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
        } catch (e: Exception) {
            DebugLogger.debugLog(TAG, "Garden deferred upload schedule skipped: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "GardenRepository"

        /** Window for collapsing the multiple completion callbacks of ONE task completion into a
         * single plant. Long enough to cover Plan-DONE + chapter-completion + burst DONE calls;
         * short enough that a deliberate re-do afterwards grows a fresh plant. */
        private const val COMPLETION_DEDUP_WINDOW_MS = 60_000L
    }
}
