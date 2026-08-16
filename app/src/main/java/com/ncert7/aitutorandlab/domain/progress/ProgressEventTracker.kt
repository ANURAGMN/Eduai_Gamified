package com.ncert7.aitutorandlab.domain.progress

import android.content.Context
import com.ncert7.aitutorandlab.domain.gamification.FriendFeedService
import com.ncert7.aitutorandlab.domain.gamification.GamificationRewardService
import com.ncert7.aitutorandlab.domain.gamification.InviteRewardService
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.repository.ProgressRepository
import com.ncert7.aitutorandlab.repository.QuestRepository
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.service.analytics.GamificationAnalyticsTracker
import com.ncert7.aitutorandlab.utils.resolveProgressLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Progress Event Tracker — Central entry point for all progress state updates.
 *
 * Every call:
 *   1. Writes the progress row via ProgressRepository
 *   2. Triggers chapter-level recalculation + persistence via ChapterProgressService
 *   3. Records a streak activity via StreakRepository
 *
 * itemType constants (keep in sync with ChapterProgressCalculator):
 *   CONCEPT          — study session progress (non-math)
 *   MATH_AGENT       — math problem session progress
 *   SIMULATION_AGENT — simulation agent session progress
 *   SIMULATION       — simulation URL load progress
 *   REVISION_AGENT   — revision session progress
 *   SCIENCE_AGENT    — science agent progress
 */
@Singleton
class ProgressEventTracker @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val conceptRepository: ConceptRepository,
    private val chapterProgressService: ChapterProgressService,
    private val streakRepository: StreakRepository,
    private val gamificationRewardService: GamificationRewardService,
    private val questRepository: QuestRepository,
    private val friendFeedService: FriendFeedService,
    private val inviteRewardService: InviteRewardService,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ProgressEventTracker"
    }

    /**
     * Look up which chapter a concept belongs to, then trigger chapter progress recalculation
     * and persistence to the chapter_agent_progress table.
     */
    private suspend fun triggerChapterProgressUpdate(
        studentId: String,
        conceptId: String,
        language: String
    ) {
        try {
            val concept = conceptRepository.getConcept(conceptId)
            if (concept == null) {
                DebugLogger.errorLog(TAG, "Concept not found id=$conceptId — chapter progress not updated")
                return
            }
            val progress = chapterProgressService.updateChapterProgress(studentId, concept.chapterId, language)
            DebugLogger.debugLog(TAG, "Chapter ${concept.chapterId} recalculated: $progress% [$language]")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Error triggering chapter progress update: ${e.message}")
        }
    }

    private suspend fun awardGamificationXp(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String,
    ) {
        try {
            gamificationRewardService.awardXpIfEligible(studentId, itemType, itemId, language)
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "XP award failed: ${e.message}")
        }
    }

    private suspend fun refreshDailyQuests(studentId: String, language: String?) {
        try {
            questRepository.refreshTodayQuest(studentId, resolveProgressLanguage(language))
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Quest refresh failed: ${e.message}")
        }
    }

    // ===== STUDY (non-math) =====

    /** Mark a concept study session as COMPLETED (END node reached) */
    suspend fun markStudyCompleted(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.markStudyCompleted(studentId, conceptId, resolvedLang)
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            streakRepository.recordActivity(studentId)
            awardGamificationXp(studentId, "CONCEPT", conceptId, resolvedLang)
            val topicTitle = conceptRepository.getConcept(conceptId)?.conceptName
            friendFeedService.onConceptCompleted(studentId, resolvedLang, topicTitle)
            inviteRewardService.tryGrantOnFirstConceptCompleted(studentId, resolvedLang)
            val chapterId = conceptRepository.getConcept(conceptId)?.chapterId.orEmpty()
            GamificationAnalyticsTracker.studyComplete(conceptId, chapterId)
            refreshDailyQuests(studentId, language)
            DebugLogger.debugLog(TAG, "Study COMPLETED: $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Study mark error: ${e.message}")
        }
    }

    /** Mark a concept study session as IN_PROGRESS (session started) */
    suspend fun markStudyInProgress(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.markStudyInProgress(studentId, conceptId, resolvedLang)
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            streakRepository.recordActivity(studentId)
            DebugLogger.debugLog(TAG, "Study IN_PROGRESS: $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Study progress error: ${e.message}")
        }
    }

    // ===== SIMULATION =====

    /** Mark simulation agent session COMPLETED (contributes to simulation component) */
    suspend fun markSimulationAgentCompleted(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.markSimulationAgentCompleted(studentId, conceptId, resolvedLang)
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            streakRepository.recordActivity(studentId)
            awardGamificationXp(studentId, "SIMULATION_AGENT", conceptId, resolvedLang)
            refreshDailyQuests(studentId, language)
            DebugLogger.debugLog(TAG, "Simulation Agent COMPLETED: $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Simulation Agent error: ${e.message}")
        }
    }

    /** Mark simulation URL as loaded/COMPLETED (contributes to simulation component) */
    suspend fun markSimulationUrlCompleted(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.markSimulationUrlCompleted(studentId, conceptId, resolvedLang)
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            streakRepository.recordActivity(studentId)
            awardGamificationXp(studentId, "SIMULATION", conceptId, resolvedLang)
            refreshDailyQuests(studentId, language)
            DebugLogger.debugLog(TAG, "Simulation URL COMPLETED: $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Simulation URL error: ${e.message}")
        }
    }

    // ===== REVISION =====

    /** Mark revision session as IN_PROGRESS (session started) */
    suspend fun markRevisionInProgress(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.updateProgressStatus(
                studentId          = studentId,
                itemType           = "REVISION_AGENT",
                itemId             = conceptId,
                language           = resolvedLang,
                newStatus          = ProgressStatus.IN_PROGRESS.value,
                progressPercentage = 5
            )
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            DebugLogger.debugLog(TAG, "Revision IN_PROGRESS: $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Revision in-progress error: ${e.message}")
        }
    }

    /** Mark revision session COMPLETED (END state reached) */
    suspend fun markRevisionCompleted(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.markRevisionCompleted(studentId, conceptId, resolvedLang)
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            streakRepository.recordActivity(studentId)
            awardGamificationXp(studentId, "REVISION_AGENT", conceptId, resolvedLang)
            val chapterId = conceptRepository.getConcept(conceptId)?.chapterId.orEmpty()
            GamificationAnalyticsTracker.revisionComplete(chapterId)
            refreshDailyQuests(studentId, language)
            DebugLogger.debugLog(TAG, "Revision COMPLETED: $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Revision error: ${e.message}")
        }
    }

    // ===== MATH AGENT =====

    /**
     * Mark math agent COMPLETED. Called when session starts (math marks complete at session start).
     *
     * Writes two rows:
     *   - MATH_AGENT / conceptId = COMPLETED  (used by ChapterProgressCalculator for math study %)
     *   - CONCEPT    / conceptId = COMPLETED  (used by ConceptViewModel for lock/unlock logic)
     */
    suspend fun markMathAgentCompleted(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.markMathAgentCompleted(studentId, conceptId, resolvedLang)
            progressRepository.markStudyCompleted(studentId, conceptId, resolvedLang)
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            streakRepository.recordActivity(studentId)
            awardGamificationXp(studentId, "MATH_AGENT", conceptId, resolvedLang)
            refreshDailyQuests(studentId, language)
            DebugLogger.debugLog(TAG, "Math Agent COMPLETED (MATH_AGENT + CONCEPT written): $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Math Agent error: ${e.message}")
        }
    }

    /** Mark science agent COMPLETED (100% nodes). */
    suspend fun markScienceAgentCompleted(studentId: String, conceptId: String, language: String? = null) {
        try {
            val resolvedLang = resolveProgressLanguage(language)
            progressRepository.updateProgressStatus(
                studentId = studentId,
                itemType = "SCIENCE_AGENT",
                itemId = conceptId,
                language = resolvedLang,
                newStatus = ProgressStatus.COMPLETED.value,
                progressPercentage = 100,
            )
            triggerChapterProgressUpdate(studentId, conceptId, resolvedLang)
            streakRepository.recordActivity(studentId)
            awardGamificationXp(studentId, "SCIENCE_AGENT", conceptId, resolvedLang)
            refreshDailyQuests(studentId, language)
            DebugLogger.debugLog(TAG, "Science Agent COMPLETED: $conceptId ($resolvedLang)")
        } catch (e: Exception) {
            DebugLogger.errorLog(TAG, "Science Agent error: ${e.message}")
        }
    }

}