package com.ncert7.aitutorandlab.ui.screens.plan

import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemStatus
object TrialQuestNavigation {
    data class PendingLaunch(
        val itemId: Long,
        val route: String,
    )

    fun firstPendingSim(items: List<PlanTrialItemEntity>): PendingLaunch? =
        items
            .firstOrNull {
                (it.kind == PlanTrialItemKind.SIM_AGENT || it.kind == PlanTrialItemKind.SIM_URL) &&
                    it.status != PlanTrialItemStatus.DONE
            }?.let { entity ->
                val ui = PlanTrialItemUiMapper.toUi(entity)
                val simTitle = ui.title.substringAfterLast(" · ").ifBlank { "Simulation" }
                PlanTrialNavigation.buildDestination(ui, titleForUrl = simTitle)?.let { route ->
                    PendingLaunch(itemId = entity.id, route = route)
                }
            }

    fun firstPendingStudy(items: List<PlanTrialItemEntity>): PendingLaunch? =
        items
            .firstOrNull {
                (it.kind == PlanTrialItemKind.STUDY || it.kind == PlanTrialItemKind.REVISION) &&
                    it.status != PlanTrialItemStatus.DONE
            }?.let { entity ->
                PlanTrialNavigation.buildDestination(PlanTrialItemUiMapper.toUi(entity))?.let { route ->
                    PendingLaunch(itemId = entity.id, route = route)
                }
            }

    /** First incomplete item in queue order; prefers in-progress over fresh pending duplicates. */
    fun firstPendingInQueue(items: List<PlanTrialItemEntity>): PendingLaunch? =
        items
            .asSequence()
            .filter { it.status != PlanTrialItemStatus.DONE }
            .sortedWith(
                compareBy(
                    { if (it.status == PlanTrialItemStatus.IN_PROGRESS) 0 else 1 },
                    { it.sequenceIndex },
                    { it.id },
                ),
            )
            .firstOrNull()
            ?.let { entity -> buildPendingLaunch(entity) }

    private fun buildPendingLaunch(entity: PlanTrialItemEntity): PendingLaunch? {
        val ui = PlanTrialItemUiMapper.toUi(entity)
        val route =
            when (entity.kind) {
                PlanTrialItemKind.SIM_AGENT, PlanTrialItemKind.SIM_URL -> {
                    val simTitle = ui.title.substringAfterLast(" · ").ifBlank { "Simulation" }
                    PlanTrialNavigation.buildDestination(ui, titleForUrl = simTitle)
                }
                else -> PlanTrialNavigation.buildDestination(ui)
            }
        return route?.let { PendingLaunch(itemId = entity.id, route = it) }
    }
}
