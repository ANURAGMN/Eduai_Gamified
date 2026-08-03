package com.ncert7.aitutorandlab.ui.screens.plan

import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.QuestDailyEntity
import com.ncert7.aitutorandlab.domain.examplan.TrialSessionStore
import com.ncert7.aitutorandlab.ui.screens.home.GamifiedHomeFocus

/** Quest trail taps — deep-link into today's trial queue when materialized. */
object TrialQuestClickActions {
    fun handleSimsClick(
        quest: QuestDailyEntity?,
        focus: GamifiedHomeFocus,
        todayPlanDay: ExamPlanDayEntity?,
        onClaim: () -> Unit,
        onNavigateToRoute: (String) -> Unit,
        onNavigateToTrial: (Int) -> Unit,
        onLegacyClick: () -> Unit,
    ) {
        if (
            quest != null &&
                quest.simsTotal > 0 &&
                quest.simsDone >= quest.simsTotal &&
                !quest.simsClaimed
        ) {
            onClaim()
            return
        }
        val route = focus.pendingSimTrialRoute
        val itemId = focus.pendingSimTrialItemId
        if (route != null && itemId != null) {
            TrialSessionStore.activeTrialItemId = itemId
            onNavigateToRoute(route)
            return
        }
        if (todayPlanDay != null) {
            onNavigateToTrial(todayPlanDay.dayIndex)
            return
        }
        onLegacyClick()
    }

    fun handleStudyClick(
        quest: QuestDailyEntity?,
        focus: GamifiedHomeFocus,
        todayPlanDay: ExamPlanDayEntity?,
        onClaim: () -> Unit,
        onNavigateToRoute: (String) -> Unit,
        onNavigateToTrial: (Int) -> Unit,
        onLegacyClick: () -> Unit,
    ) {
        if (
            quest != null &&
                quest.studyTotal > 0 &&
                quest.studyDone >= quest.studyTotal &&
                !quest.studyClaimed
        ) {
            onClaim()
            return
        }
        val route = focus.pendingStudyTrialRoute
        val itemId = focus.pendingStudyTrialItemId
        if (route != null && itemId != null) {
            TrialSessionStore.activeTrialItemId = itemId
            onNavigateToRoute(route)
            return
        }
        if (todayPlanDay != null) {
            onNavigateToTrial(todayPlanDay.dayIndex)
            return
        }
        onLegacyClick()
    }
}
