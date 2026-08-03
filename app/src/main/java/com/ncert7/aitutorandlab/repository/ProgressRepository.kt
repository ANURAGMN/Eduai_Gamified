package com.ncert7.aitutorandlab.repository

import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.local.dao.ChapterAgentProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterProgressSummaryDto
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterProgressSummary
import com.ncert7.aitutorandlab.data.local.entities.ChapterAgentProgressEntity
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus
import com.ncert7.aitutorandlab.service.sync.DataSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for all progress-related data access.
 *
 * Centralises access to both:
 * - `progress` table (per-concept/item progress rows)
 * - `chapter_agent_progress` table (aggregated chapter-level progress)
 *
 * All ViewModels and domain classes must use this instead of DAOs directly.
 */
class ProgressRepository(
    private val progressDao: ProgressDao,
    private val chapterAgentProgressDao: ChapterAgentProgressDao
) {

    // ===== CONCEPT / ITEM PROGRESS =====

    /** Get progress for a single item (concept, simulation, etc.) */
    suspend fun getProgress(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String
    ): ProgressEntity? =
        progressDao.getProgress(studentId, itemType, itemId, language, AppConfig.APP_NAME)

    /** Update (upsert) progress status for an item */
    suspend fun updateProgressStatus(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String,
        newStatus: String,
        progressPercentage: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        progressDao.updateProgressStatus(
            studentId, itemType, itemId, AppConfig.APP_NAME, language,
            newStatus, progressPercentage, timestamp
        )
        // Coalesced deferred upload instead of an immediate full sync of every collection.
        DataSyncService.scheduleDeferredUpload()
    }

    /** Get a reactive Flow of progress for a single item */
    fun getProgressFlow(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String
    ): Flow<ProgressEntity?> =
        progressDao.getProgressFlow(studentId, itemType, itemId, language, AppConfig.APP_NAME)

    /** Count completed concepts for a student (Reactive Flow) */
    fun getTotalCompletedConceptsFlow(studentId: String, language: String): Flow<Int> =
        progressDao.getTotalCompletedConceptsFlow(studentId, language, AppConfig.APP_NAME)

    /** Count completed simulations for a student (Reactive Flow) */
    fun getTotalCompletedSimulationsFlow(studentId: String, language: String): Flow<Int> =
        progressDao.getTotalCompletedSimulationsFlow(studentId, language, AppConfig.APP_NAME)

    /** 7-day daily concept completion summary */
    suspend fun getConceptsClearedLast7Days(
        studentId: String,
        sevenDaysAgoTimestamp: Long
    ) = progressDao.getConceptsClearedLast7Days(studentId, sevenDaysAgoTimestamp, AppConfig.APP_NAME)

    /** Daily completed activity last 7 days (for streak) */
    suspend fun getDailyCompletedActivityLast7Days(
        studentId: String,
        sevenDaysAgoTimestamp: Long
    ) = progressDao.getDailyCompletedActivityLast7Days(studentId, sevenDaysAgoTimestamp, AppConfig.APP_NAME)

    /** Count completed activities today (for streak) (Reactive Flow) */
    fun getTodayCompletedConceptCountFlow(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<Int> = progressDao.getTodayCompletedConceptCountFlow(studentId, language, startOfDay, endOfDay, AppConfig.APP_NAME)

    fun getTodayCompletedSimulationCountFlow(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<Int> = progressDao.getTodayCompletedSimulationCountFlow(studentId, language, startOfDay, endOfDay, AppConfig.APP_NAME)

    suspend fun getTodayFullyCompletedActivityCount(
        studentId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Int = progressDao.getTodayFullyCompletedActivityCount(studentId, startOfDay, endOfDay, AppConfig.APP_NAME)

    suspend fun getTodayCompletedConceptCount(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long
    ): Int = progressDao.getTodayCompletedConceptCount(studentId, language, startOfDay, endOfDay, AppConfig.APP_NAME)

    suspend fun getTodayCompletedSimulationCount(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long
    ): Int = progressDao.getTodayCompletedSimulationCount(studentId, language, startOfDay, endOfDay, AppConfig.APP_NAME)


    // ===== CHAPTER-WISE PROGRESS (REAL-TIME) =====

    /**
     * ✅ NEW: Get chapter-wise progress with REAL-TIME updates
     *
     * This is the KEY FIX for real-time progress tracking.
     *
     * Returns a Flow<List<ChapterProgressSummary>> that:
     * - Counts actual concept completions from progress table
     * - Emits new data whenever progress table changes
     * - Provides real-time updates to ProgressScreenViewModel
     * - Calculates progress directly from data (no aggregation needed)
     *
     * @param studentId Student ID
     * @param subjectId Subject ID to filter chapters
     * @return Flow emitting list of chapters with their progress
     */
    fun getChapterWiseProgress(
        studentId: String,
        subjectId: String,
        language: String
    ): Flow<List<ChapterProgressSummary>> =
        progressDao.getChapterWiseProgressFlow(
            studentId = studentId,
            subjectId = subjectId,
            language = language,
            appName = AppConfig.APP_NAME
        )

    // ===== CHAPTER AGENT PROGRESS (LEGACY - kept for backward compatibility) =====

    /** Get the aggregated chapter-level progress row */
    suspend fun getChapterAgentProgress(
        studentId: String,
        chapterId: String,
        language: String = "en"
    ): ChapterAgentProgressEntity? =
        chapterAgentProgressDao.getChapterProgress(studentId, chapterId, language, AppConfig.APP_NAME)

    /**
     * Upsert aggregated chapter progress.
     * The overallPercentage must be pre-computed by ChapterProgressCalculator.
     */
    suspend fun updateChapterAgentProgress(
        studentId: String,
        chapterId: String,
        language: String,
        studyPercentage: Int,
        simulationPercentage: Int,
        revisionPercentage: Int,
        overallPercentage: Int
    ) {
        chapterAgentProgressDao.updateChapterAgentProgress(
            studentId, chapterId, language, AppConfig.APP_NAME,
            studyPercentage, simulationPercentage, revisionPercentage, overallPercentage
        )
        // Coalesced deferred upload instead of an immediate full sync of every collection.
        com.ncert7.aitutorandlab.service.sync.DataSyncService.scheduleDeferredUpload()
    }

    /**
     * Get all chapters for a subject with their agent progress (LEFT JOIN — includes chapters
     * with no progress rows yet, showing 0%).
     */
    suspend fun getChapterWiseProgressSummary(
        studentId: String,
        language: String,
        subjectId: String
    ): List<ChapterProgressSummaryDto> =
        chapterAgentProgressDao.getChapterWiseProgressSummary(
            studentId, language, subjectId, AppConfig.APP_NAME
        )

    /** Get reactive flow of chapter progress entity */
    fun getChapterProgressFlow(
        studentId: String,
        chapterId: String,
        language: String = "en"
    ): Flow<ChapterAgentProgressEntity?> =
        chapterAgentProgressDao.getChapterProgressFlow(studentId, chapterId, language, AppConfig.APP_NAME)

    suspend fun getCompletedChaptersCount(
        studentId: String,
        language: String = "en"
    ): Int = chapterAgentProgressDao.getCompletedChaptersCount(studentId, language, AppConfig.APP_NAME)

    /** Mark study as IN_PROGRESS (quick helper) */
    suspend fun markStudyInProgress(studentId: String, conceptId: String, language: String) {
        updateProgressStatus(
            studentId, "CONCEPT", conceptId, language,
            ProgressStatus.IN_PROGRESS.value, 5
        )
    }

    /** Mark study as COMPLETED (quick helper) */
    suspend fun markStudyCompleted(studentId: String, conceptId: String, language: String) {
        updateProgressStatus(
            studentId, "CONCEPT", conceptId, language,
            ProgressStatus.COMPLETED.value, 100
        )
    }

    /** Mark simulation agent as COMPLETED */
    suspend fun markSimulationAgentCompleted(studentId: String, conceptId: String, language: String) {
        updateProgressStatus(
            studentId, "SIMULATION_AGENT", conceptId, language,
            ProgressStatus.COMPLETED.value, 100
        )
    }

    /** Mark simulation URL loaded (COMPLETED) */
    suspend fun markSimulationUrlCompleted(studentId: String, conceptId: String, language: String) {
        updateProgressStatus(
            studentId, "SIMULATION", conceptId, language,
            ProgressStatus.COMPLETED.value, 100
        )
    }

    /** Mark revision agent as COMPLETED */
    suspend fun markRevisionCompleted(studentId: String, conceptId: String, language: String) {
        updateProgressStatus(
            studentId, "REVISION_AGENT", conceptId, language,
            ProgressStatus.COMPLETED.value, 100
        )
    }

    /** Mark math agent as COMPLETED */
    suspend fun markMathAgentCompleted(studentId: String, conceptId: String, language: String) {
        updateProgressStatus(
            studentId, "MATH_AGENT", conceptId, language,
            ProgressStatus.COMPLETED.value, 100
        )
    }
}