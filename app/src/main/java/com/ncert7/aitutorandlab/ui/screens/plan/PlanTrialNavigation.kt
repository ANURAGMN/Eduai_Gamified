package com.ncert7.aitutorandlab.ui.screens.plan

import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemKind
import com.ncert7.aitutorandlab.ui.screens.plan.viewmodel.PlanTrialItemUi
import java.net.URLEncoder

object PlanTrialNavigation {

    fun routeForDay(dayIndex: Int): String = "plan_trial/$dayIndex"

    fun buildDestination(item: PlanTrialItemUi, titleForUrl: String = "Simulation"): String? =
        when (item.kind) {
            PlanTrialItemKind.STUDY ->
                "chatbot?conceptId=${item.conceptId}"
            PlanTrialItemKind.SIM_AGENT ->
                "simulation_agent/${item.sourceId}?conceptId=${item.conceptId}"
            PlanTrialItemKind.SIM_URL -> {
                val encodedUrl = URLEncoder.encode(item.sourceId, Charsets.UTF_8.name())
                val encodedConceptId =
                    URLEncoder.encode(item.conceptId, Charsets.UTF_8.name())
                val safeTitle = titleForUrl.replace("/", "-")
                "concept_sim_view/$encodedUrl/$safeTitle/$encodedConceptId//"
            }
            PlanTrialItemKind.REVISION ->
                "revision/${item.chapterId}"
            PlanTrialItemKind.MATH ->
                "math_agent?chapterId=${item.chapterId}&problemId=${item.sourceId}"
            else -> null
        }
}
