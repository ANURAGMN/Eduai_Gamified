package com.ncert7.aitutorandlab.domain.examplan

import com.ncert7.aitutorandlab.data.local.dao.PlanTrialItemDao
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
import com.ncert7.aitutorandlab.domain.garden.GardenMomentCoordinator
import com.ncert7.aitutorandlab.repository.GardenRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanTrialProgressTracker @Inject constructor(
    private val planTrialItemDao: PlanTrialItemDao,
    private val gardenRepository: GardenRepository,
    private val gardenMomentCoordinator: GardenMomentCoordinator,
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
     * Adjusts the trial item's required bite count from the HTML click budget and returns
     * the effective prompt/completion thresholds for this session.
     */
    suspend fun applyHtmlInteractionBudget(
        trialItemId: Long,
        htmlBudget: Int,
    ): SimulationTrialThresholds {
        val item = planTrialItemDao.getItemById(trialItemId)
            ?: return SimulationTrialThresholds.compute(htmlBudget)
        val thresholds = SimulationTrialThresholds.compute(htmlBudget, item.requiredCount)
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
            celebrated = false,
        )
        // A Plan-trial task can reach DONE below the free-browsing engagement gate (e.g. complete@2),
        // so the chapter-completion path (ProgressEventTracker) may never fire and no plant would
        // grow. Grow it here too. recordCompletion is idempotent per concept+activity bucket, so this
        // never double-plants when the chapter-completion path also runs later.
        if (status == PlanTrialItemStatus.DONE) {
            val planted =
                gardenRepository.recordCompletion(
                    studentId = item.studentId,
                    conceptId = item.conceptId,
                    chapterId = item.chapterId,
                    kind = item.kind,
                )
            planted?.let { row ->
                val progress = gardenRepository.getProgress(item.studentId) ?: return@let
                val placeCompleted = progress.filledInZone >= progress.zoneCapacity
                gardenMomentCoordinator.notifyPlanted(
                    planted = row,
                    progress = progress,
                    placeCompleted = placeCompleted,
                )
            }
        }
    }
}
