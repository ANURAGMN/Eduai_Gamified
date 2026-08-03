package com.ncert7.aitutorandlab.ui.screens.plan

import com.anurag.eduai.uikit.components.PlanDayNode
import com.anurag.eduai.uikit.components.PlanDayType
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.service.analytics.ContentClickNavigation
import com.ncert7.aitutorandlab.ui.navigation.GatedNavigationAction
import com.ncert7.aitutorandlab.ui.screens.home.GamifiedHomeFocus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object PlanDayActions {

    fun navigateForPlanDay(
        day: PlanDayNode,
        gated: GatedNavigationAction,
        selectedSubjectId: String,
        scope: CoroutineScope,
        resolveChapterId: suspend (String) -> String?,
        onNavigateToPlan: () -> Unit,
        onNavigateToChapters: (String) -> Unit,
        onNavigateToRevision: (String) -> Unit,
        onLessonClick: (String) -> Unit,
        onNavigateToTrial: (Int) -> Unit = {},
    ) {
        when (day.type) {
            PlanDayType.Exam -> onNavigateToPlan()
            PlanDayType.Mock -> onNavigateToChapters(selectedSubjectId)
            PlanDayType.Revise -> onNavigateToTrial(day.day)
            else -> onNavigateToTrial(day.day)
        }
    }

    fun navigateForTodayPlanDay(
        todayPlanDay: ExamPlanDayEntity?,
        planDays: List<PlanDayNode>,
        gated: GatedNavigationAction,
        selectedSubjectId: String,
        scope: CoroutineScope,
        resolveChapterId: suspend (String) -> String?,
        onNavigateToPlan: () -> Unit,
        onNavigateToChapters: (String) -> Unit,
        onNavigateToRevision: (String) -> Unit,
        onLessonClick: (String) -> Unit,
        fallbackConceptId: String?,
        onNavigateToTrial: (Int) -> Unit = {},
        focus: GamifiedHomeFocus? = null,
        onNavigateToRoute: (String) -> Unit = {},
    ) {
        if (todayPlanDay == null) {
            if (fallbackConceptId != null) {
                gated.run(
                    trackClick = { ContentClickNavigation.trackHomeLessonClick(fallbackConceptId) },
                    navigate = { onLessonClick(fallbackConceptId) },
                )
            } else {
                onNavigateToChapters(selectedSubjectId)
            }
            return
        }

        val nextRoute = focus?.pendingNextTrialRoute
        val nextItemId = focus?.pendingNextTrialItemId
        if (nextRoute != null && nextItemId != null) {
            TrialSessionStore.activeTrialItemId = nextItemId
            onNavigateToRoute(nextRoute)
            return
        }

        onNavigateToTrial(todayPlanDay.dayIndex)
    }
}
