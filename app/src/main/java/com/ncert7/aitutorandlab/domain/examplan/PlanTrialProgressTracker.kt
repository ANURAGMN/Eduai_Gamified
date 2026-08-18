package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.dao.PlanTrialItemDao
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.repository.GardenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanTrialProgressTracker @Inject constructor(
    private val planTrialItemDao: PlanTrialItemDao,
    private val gardenRepository: GardenRepository,
) {
    /** Whether the trial item is currently DONE (used to decide if a soft-proceed should skip celebration). */
    suspend fun isDone(trialItemId: Long): Boolean =
        planTrialItemDao.getItemById(trialItemId)?.status == PlanTrialItemStatus.DONE

    suspend fun recordIncrement(trialItemId: Long) {
        val item = planTrialItemDao.getItemById(trialItemId) ?: return
        if (item.status == PlanTrialItemStatus.DONE) return
        applyCount(item.id, item.completedCount + 1, item.requiredCount)
    }

    /** Simulation agent reached the GE node — persist immediately, celebrate later. */
    suspend fun recordGeReached(trialItemId: Long) {
        val item = planTrialItemDao.getItemById(trialItemId) ?: return
        if (item.status == PlanTrialItemStatus.DONE) return
        applyCount(item.id, item.requiredCount.coerceAtLeast(1), item.requiredCount)
    }

    suspend fun markCelebrated(trialItemId: Long) {
        val item = planTrialItemDao.getItemById(trialItemId) ?: return
        planTrialItemDao.updateProgress(
            itemId = item.id,
            status = item.status,
            completedCount = item.completedCount,
            celebrated = true,
        )
    }

    suspend fun syncToCount(trialItemId: Long, totalCount: Int) {
        val item = planTrialItemDao.getItemById(trialItemId) ?: return
        if (totalCount <= item.completedCount) {
            if (item.status != PlanTrialItemStatus.DONE &&
                item.completedCount >= item.requiredCount
            ) {
                applyCount(item.id, item.completedCount, item.requiredCount)
            }
            return
        }
        applyCount(
            itemId = item.id,
            newCount = totalCount.coerceAtMost(item.requiredCount),
            requiredCount = item.requiredCount,
        )
    }

    /** Undo a premature DONE when the learner has not met the bite threshold. */
    suspend fun reconcileCompletion(trialItemId: Long) {
        val item = planTrialItemDao.getItemById(trialItemId) ?: return
        if (item.status == PlanTrialItemStatus.DONE && item.completedCount < item.requiredCount) {
            planTrialItemDao.updateProgress(
                itemId = item.id,
                status = PlanTrialItemStatus.IN_PROGRESS,
                completedCount = item.completedCount,
                celebrated = false,
            )
        }
    }

    /**
     * Syncs the trial item's required bite count to the fixed Plan goal ([SimulationTrialThresholds.DEFAULT_GOAL])
     * and returns session thresholds. HTML click budget is logged/returned for coaching only — it does
     * **not** lower [completionAt]. Existing items that still have requiredCount 2/7 are upgraded to 15
     * the next time the sim opens.
     */
    suspend fun applyHtmlInteractionBudget(
        trialItemId: Long,
        htmlBudget: Int,
    ): SimulationTrialThresholds {
        val item = planTrialItemDao.getItemById(trialItemId)
            ?: return SimulationTrialThresholds.compute(htmlBudget)
        val thresholds =
            SimulationTrialThresholds.compute(htmlBudget, SimulationTrialThresholds.DEFAULT_GOAL)
        if (thresholds.completionAt != item.requiredCount) {
            planTrialItemDao.updateRequiredCount(trialItemId, thresholds.completionAt)
            val refreshed = planTrialItemDao.getItemById(trialItemId)
            if (refreshed != null &&
                refreshed.completedCount >= thresholds.completionAt &&
                refreshed.status != PlanTrialItemStatus.DONE
            ) {
                applyCount(refreshed.id, refreshed.completedCount, thresholds.completionAt)
            }
        }
        return thresholds
    }

    private suspend fun applyCount(itemId: Long, newCount: Int, requiredCount: Int) {
        val item = planTrialItemDao.getItemById(itemId) ?: return
        val wasDone = item.status == PlanTrialItemStatus.DONE
        val status =
            if (newCount >= requiredCount) {
                PlanTrialItemStatus.DONE
            } else {
                PlanTrialItemStatus.IN_PROGRESS
            }
        planTrialItemDao.updateProgress(
            itemId = itemId,
            status = status,
            completedCount = newCount.coerceAtMost(requiredCount),
            // Keep celebrated sticky once set — re-syncing an already-DONE item must not re-arm
            // the XP/garden celebration chain.
            celebrated = if (wasDone) item.celebrated else false,
        )
        // Plant only on the transition into DONE. Re-applying the same DONE (flush on back,
        // HTML budget refresh, soft-proceed) must not grow another plant or re-queue a moment.
        if (status != PlanTrialItemStatus.DONE || wasDone) return

        // Grow the row here (Plan trials can finish below the free-browse tap gate). Do NOT
        // notifyPlanted — the global host would pop over the sim, then Plan would show it again
        // via queueCelebrationForUnshownPlant. Plan owns the one celebration for trial plants.
        gardenRepository.recordCompletion(
            studentId = item.studentId,
            conceptId = item.conceptId,
            chapterId = item.chapterId,
            kind = item.kind,
        )
    }
}
