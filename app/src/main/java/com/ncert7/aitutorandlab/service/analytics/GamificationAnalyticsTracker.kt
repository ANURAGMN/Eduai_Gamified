package com.ncert7.aitutorandlab.service.analytics

import android.content.Context
import com.ncert7.aitutorandlab.domain.moment.MomentType
import com.ncert7.aitutorandlab.domain.gamification.LeagueTier

/**
 * GA4-only gamification / trial / social events (see ANALYTICS_EVENT_SPEC.md).
 */
object GamificationAnalyticsTracker {

    fun initialize(context: Context) {
        FirebaseAnalyticsHelper.initialize(context)
    }

    fun refreshUserProperties(context: Context) {
        FirebaseAnalyticsHelper.refreshUserProperties(context)
    }

    // --- Trial & plan ---

    fun planCreated(
        chapterCount: Int,
        dailyMinutes: Int,
        daysToExam: Int,
        totalItems: Int,
    ) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "plan_created",
            screen = ScreenName.PLAN,
            params =
                mapOf(
                    "chapter_count" to chapterCount,
                    "daily_minutes" to dailyMinutes,
                    "days_to_exam" to daysToExam,
                    "total_items" to totalItems,
                ),
        )
    }

    fun planFeasibilityWarning(
        issue: PlanFeasibilityIssueType,
        requiredDays: Int = 0,
        availableDays: Int = 0,
    ) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "plan_feasibility_warning",
            screen = ScreenName.PLAN,
            params =
                mapOf(
                    "issue" to issue.value,
                    "required_days" to requiredDays,
                    "available_days" to availableDays,
                ),
        )
    }

    fun trialDayStart(
        dayIndex: Int,
        dayType: String,
        itemCount: Int,
        trialView: TrialViewMode,
    ) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "trial_day_start",
            screen = ScreenName.PLAN_TRIAL,
            params =
                mapOf(
                    "day_index" to dayIndex,
                    "day_type" to dayType,
                    "item_count" to itemCount,
                    "trial_view" to trialView.value,
                ),
        )
    }

    fun trialItemComplete(
        kind: String,
        chapterId: String,
        dayIndex: Int,
        attempts: Int,
    ) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "trial_item_complete",
            screen = ScreenName.PLAN_TRIAL,
            params =
                mapOf(
                    "kind" to kind,
                    "chapter_id" to chapterId,
                    "day_index" to dayIndex,
                    "attempts" to attempts,
                ),
        )
    }

    fun trialDayComplete(dayIndex: Int, dayType: String, items: Int, durationMs: Long) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "trial_day_complete",
            screen = ScreenName.PLAN_TRIAL,
            params =
                mapOf(
                    "day_index" to dayIndex,
                    "day_type" to dayType,
                    "items" to items,
                    "duration_ms" to durationMs,
                ),
        )
    }

    fun trialViewSelected(trialView: TrialViewMode) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "trial_view_selected",
            screen = ScreenName.PLAN_TRIAL,
            params = mapOf("trial_view" to trialView.value),
        )
    }

    fun trialRollover(movedCount: Int, fromDay: Int, toDay: Int) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "trial_rollover",
            screen = ScreenName.PLAN_TRIAL,
            params =
                mapOf(
                    "moved_count" to movedCount,
                    "from_day" to fromDay,
                    "to_day" to toDay,
                ),
        )
    }

    fun planDeletedDeadline(incompleteCount: Int) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "plan_deleted_deadline",
            screen = ScreenName.PLAN,
            params = mapOf("incomplete_count" to incompleteCount),
        )
    }

    // --- Study ---

    fun studyTurn(conceptId: String, turnIndex: Int) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "study_turn",
            screen = ScreenName.CHATBOT,
            params = mapOf("concept_id" to conceptId, "turn_index" to turnIndex),
        )
    }

    fun studyComplete(conceptId: String, chapterId: String = "") {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "study_complete",
            screen = ScreenName.CHATBOT,
            params =
                mapOf(
                    "concept_id" to conceptId,
                    "chapter_id" to chapterId,
                ),
        )
    }

    fun revisionComplete(chapterId: String, dayIndex: Int = 0) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "revision_complete",
            screen = ScreenName.REVISION,
            params =
                mapOf(
                    "chapter_id" to chapterId,
                    "day_index" to dayIndex,
                ),
        )
    }

    // --- Economy ---

    fun xpEarned(amount: Int, source: EconomySource, kind: String = "") {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "xp_earned",
            params =
                buildMap {
                    put("amount", amount)
                    put("source", source.value)
                    if (kind.isNotBlank()) put("kind", kind)
                },
        )
    }

    fun gemsEarned(amount: Int, source: EconomySource) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "gems_earned",
            params = mapOf("amount" to amount, "source" to source.value),
        )
    }

    fun gemsSpent(amount: Int, sink: GemSink) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "gems_spent",
            params = mapOf("amount" to amount, "sink" to sink.value),
        )
    }

    fun streakExtended(streakLen: Int) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "streak_extended",
            screen = ScreenName.HOME,
            params = mapOf("streak_len" to streakLen),
        )
    }

    fun streakBreak(previousLen: Int) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "streak_break",
            screen = ScreenName.HOME,
            params = mapOf("previous_len" to previousLen),
        )
    }

    fun streakMilestone(milestone: StreakMilestone) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "streak_milestone",
            screen = ScreenName.HOME,
            params = mapOf("milestone" to milestone.value),
        )
    }

    fun questComplete(quest: QuestKind) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "quest_complete",
            screen = ScreenName.QUESTS,
            params = mapOf("quest" to quest.value),
        )
    }

    fun bonusUnlocked() {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "bonus_unlocked",
            screen = ScreenName.QUESTS,
        )
    }

    // --- Moments ---

    fun momentShown(moment: MomentType, variantId: String, celebratory: Boolean) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "moment_shown",
            screen = ScreenName.PLAN_TRIAL,
            params =
                mapOf(
                    "moment" to moment.name,
                    "variant_id" to variantId,
                    "celebratory" to celebratory,
                ),
        )
    }

    fun momentPrimary(moment: MomentType, variantId: String) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "moment_primary",
            screen = ScreenName.PLAN_TRIAL,
            params = mapOf("moment" to moment.name, "variant_id" to variantId),
        )
    }

    fun momentSecondary(moment: MomentType) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "moment_secondary",
            screen = ScreenName.PLAN_TRIAL,
            params = mapOf("moment" to moment.name),
        )
    }

    // --- Social / leagues ---

    fun leaguePromoted(fromTier: LeagueTier, toTier: LeagueTier) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "league_promoted",
            screen = ScreenName.LEAGUES,
            params =
                mapOf(
                    "from_tier" to fromTier.storageKey,
                    "to_tier" to toTier.storageKey,
                ),
        )
    }

    fun leagueDemoted(fromTier: LeagueTier, toTier: LeagueTier) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "league_demoted",
            screen = ScreenName.LEAGUES,
            params =
                mapOf(
                    "from_tier" to fromTier.storageKey,
                    "to_tier" to toTier.storageKey,
                ),
        )
    }

    fun friendAdded(method: String = "code") {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "friend_added",
            screen = ScreenName.FRIENDS,
            params = mapOf("method" to method),
        )
    }

    fun inviteSent(channel: InviteChannel) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "invite_sent",
            screen = ScreenName.HOME,
            params = mapOf("channel" to channel.value),
        )
    }

    fun cheerSent() {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "cheer_sent",
            screen = ScreenName.HOME,
        )
    }

    // --- Avatar ---

    fun avatarSaved(character: String, changedFields: String) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "avatar_saved",
            screen = ScreenName.AVATAR,
            params =
                mapOf(
                    "character" to character,
                    "changed_fields" to changedFields,
                ),
        )
    }

    fun avatarUnlocked(presetId: String, via: AvatarUnlockVia) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "avatar_unlocked",
            screen = ScreenName.AVATAR,
            params = mapOf("preset_id" to presetId, "via" to via.value),
        )
    }

    fun notificationShown(typeId: String) {
        FirebaseAnalyticsHelper.logEvent(
            eventName = "notification_shown",
            params = mapOf("type" to typeId),
        )
    }
}
