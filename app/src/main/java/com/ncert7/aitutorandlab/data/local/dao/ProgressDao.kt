package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus
import com.ncert7.aitutorandlab.utils.legacyProgressLanguageAlias
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import kotlinx.coroutines.flow.Flow

/** Data Access Object for managing student progress in learning items. */
@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<ProgressEntity>)

    @Update
    suspend fun updateProgress(progress: ProgressEntity)

    @Query(
        "SELECT * FROM progress WHERE studentId = :studentId AND itemType = :itemType AND itemId = :itemId AND language = :language AND appName = :appName"
    )
    suspend fun getProgress(studentId: String, itemType: String, itemId: String, language: String, appName: String): ProgressEntity?

    @Query(
        "SELECT * FROM progress WHERE studentId = :studentId AND itemType = :itemType AND itemId = :itemId AND language = :language AND appName = :appName"
    )
    fun getProgressFlow(studentId: String, itemType: String, itemId: String, language: String, appName: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE studentId = :studentId AND appName = :appName")
    fun getAllProgress(studentId: String, appName: String): Flow<List<ProgressEntity>>

    @Query("SELECT COUNT(*) FROM progress WHERE studentId = :studentId AND appName = :appName")
    suspend fun getProgressCount(studentId: String, appName: String): Int

    @Query("SELECT * FROM progress WHERE studentId = :studentId AND itemType = :itemType AND appName = :appName")
    suspend fun getAllProgressSync(studentId: String, itemType: String, appName: String): List<ProgressEntity>

    @Query(
        "SELECT COUNT(*) FROM progress WHERE studentId = :studentId AND itemType = :itemType AND status = :completedStatus AND completedAt >= :weekStartTimestamp AND appName = :appName"
    )
    suspend fun getWeeklyCompletedCount(
        studentId: String,
        weekStartTimestamp: Long,
        itemType: String,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Int

    @Query("SELECT * FROM progress WHERE isSynced = 0")
    suspend fun getUnsyncedProgress(): List<ProgressEntity>

    @Query("UPDATE progress SET isSynced = 1 WHERE progressId IN (:ids)")
    suspend fun markProgressAsSynced(ids: List<Long>)

    /** Mark pre-fix English rows that duplicate an explicit Kannada row for the same item. */
    @Query(
        """
        UPDATE progress SET language = 'legacy'
        WHERE language IN ('en', 'English')
        AND EXISTS (
            SELECT 1 FROM progress AS p2
            WHERE p2.itemId = progress.itemId
            AND p2.studentId = progress.studentId
            AND p2.itemType = progress.itemType
            AND p2.appName = progress.appName
            AND p2.language IN ('kn', 'Kannada')
        )
        """
    )
    suspend fun markDuplicateLegacyEnglishProgress()

    @Query(
        """
        UPDATE progress SET language = 'legacy'
        WHERE language IN ('English', 'Kannada')
        """
    )
    suspend fun markFullWordLegacyLanguages()

    @Query(
        "DELETE FROM progress WHERE studentId = :studentId AND itemType = :itemType AND itemId = :itemId AND appName = :appName"
    )
    suspend fun deleteProgress(studentId: String, itemType: String, itemId: String, appName: String)

    @Query("DELETE FROM progress")
    suspend fun clearAllProgress()

    @Transaction
    suspend fun updateProgressStatus(
        studentId: String,
        itemType: String,
        itemId: String,
        appName: String,
        language: String,
        newStatus: String,
        progressPercentage: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val lang = normalizeLanguageCode(language)
        var existing = getProgress(studentId, itemType, itemId, lang, appName)
        if (existing == null) {
            legacyProgressLanguageAlias(lang)?.let { legacy ->
                existing = getProgress(studentId, itemType, itemId, legacy, appName)
            }
        }
        if (existing != null) {
            val updated =
                existing.copy(
                    language = lang,
                    status = newStatus,
                    completedAt =
                        if (newStatus == ProgressStatus.COMPLETED.value) timestamp
                        else existing.completedAt,
                    startedAt = existing.startedAt ?:
                    if (newStatus == ProgressStatus.IN_PROGRESS.value) timestamp else null,
                    lastAccessedAt = timestamp,
                    updatedAt = timestamp,
                    progressPercentage = progressPercentage.coerceIn(0, 100),
                    isSynced = false
                )
            updateProgress(updated)
        } else {
            insertProgress(
                ProgressEntity(
                    studentId = studentId,
                    itemType = itemType,
                    itemId = itemId,
                    appName = appName,
                    status = newStatus,
                    language = lang,
                    progressPercentage = progressPercentage.coerceIn(0, 100),
                    startedAt = if (newStatus == ProgressStatus.IN_PROGRESS.value) timestamp else null,
                    completedAt = if (newStatus == ProgressStatus.COMPLETED.value) timestamp else null,
                    lastAccessedAt = timestamp,
                    updatedAt = timestamp
                )
            )
        }
    }

    /**
     * Get home screen concepts with real-time updates: 1st item - most recently updated IN_PROGRESS
     * concept Next 3 items - NOT_STARTED concepts ordered by ConceptEntity.orderIndex Limit to 4
     * total items
     *
     * Automatically emits new list whenever progress changes
     */
    @Query(
        """
        SELECT p.* FROM progress p
        INNER JOIN concepts c ON p.itemId = c.conceptId
        WHERE p.studentId = :studentId 
        AND p.itemType = :itemType 
        AND p.appName = :appName
        AND p.status != :completedStatus
        ORDER BY 
            CASE WHEN p.status = :inProgressStatus THEN 0 ELSE 1 END ASC,
            CASE WHEN p.status = :inProgressStatus THEN p.lastAccessedAt ELSE 0 END DESC,
            c.orderIndex ASC
        LIMIT 4
    """
    )
    fun getHomeScreenConcepts(
        studentId: String,
        itemType: String,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value,
        inProgressStatus: String = ProgressStatus.IN_PROGRESS.value
    ): Flow<List<ProgressEntity>>

    /**
     * Progress for home screen today progress section
     */
    @Query(
        """
    SELECT * FROM progress
    WHERE studentId = :studentId
      AND itemType = 'CONCEPT'
      AND status = :completedStatus
      AND appName = :appName
    ORDER BY completedAt DESC
    LIMIT 1
"""
    )
    suspend fun getLastCompletedConcept(
        studentId: String,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): ProgressEntity?

    // ===== FLOW-BASED QUERIES FOR REAL-TIME UPDATES =====

    /**
     * Get concepts cleared last 7 days as Flow for real-time updates
     * Emits updated list whenever progress changes
     */
    @Query(
        """
        SELECT 
            DATE(completedAt / 1000, 'unixepoch', 'localtime') as date,
            COUNT(*) as count
        FROM progress
        WHERE studentId = :studentId
        AND itemType = 'CONCEPT'
        AND status = :completedStatus
        AND completedAt >= :sevenDaysAgoTimestamp
        AND appName = :appName
        GROUP BY DATE(completedAt / 1000, 'unixepoch', 'localtime')
        ORDER BY date DESC
    """
    )
    fun getConceptsClearedLast7DaysFlow(
        studentId: String,
        sevenDaysAgoTimestamp: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Flow<List<DailyConceptCount>>

    /**
     * Get the total number of simulations completed today
     * Only counts simulation concepts with valid URLs
     * Uses local date to determine "today"
     */
    @Query(
        """
        SELECT COUNT(DISTINCT p.itemId) 
        FROM progress p
        INNER JOIN concepts c ON p.itemId = c.conceptId
        WHERE p.studentId = :studentId 
        AND p.itemType IN ('SIMULATION_AGENT', 'SIMULATION') 
        AND p.status = 'COMPLETED'
        AND c.type = 'SIMULATION'
        AND DATE(p.completedAt / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')
        AND (
            (c.simulationUrl IS NOT NULL AND c.simulationUrl != '' AND c.simulationUrl != 'Not found')
            OR
            (c.simulationUrlKannada IS NOT NULL AND c.simulationUrlKannada != '' AND c.simulationUrlKannada != 'Not found')
        )
    """
    )
    suspend fun getTodayCompletedSimulations(studentId: String): Int

    /**
     * ✅ FIXED: Real-time chapter-wise progress calculation
     *
     * Counts ONLY visible concepts for the language:
     * - totalConcepts: Count of STUDY + SIMULATION + MATH PROBLEM concepts that have content for this language
     * - completedConcepts: Count of those concepts with ALL required components COMPLETED in progress table
     * - completionPercentage: (completedConcepts / totalConcepts) * 100
     *
     * CRITICAL: Only concepts with valid content for the language are included in totalConcepts
     * This ensures the same progress % across ProgressScreen, ChapterScreen, and ConceptScreen header
     */
    @Query(
        """
        WITH chapter_counts AS (
            SELECT 
                ch.chapterId,
                ch.chapterName,
                ch.chapterNameKannada,
                
                -- 1. STUDY components total and completed
                (SELECT COUNT(*) FROM concepts c 
                 WHERE c.chapterId = ch.chapterId 
                 AND (
                     (ch.subjectId = '5c0a6b6d-7c6b-4f35-9d5b-9fd0fd8e8a01' AND c.type = 'MATH PROBLEM' AND c.problemId IS NOT NULL AND c.problemId != '')
                     OR
                     (ch.subjectId != '5c0a6b6d-7c6b-4f35-9d5b-9fd0fd8e8a01' AND c.type = 'STUDY')
                 )
                ) AS totalStudy,
                
                (SELECT COUNT(*) FROM concepts c 
                 WHERE c.chapterId = ch.chapterId 
                 AND (
                     (ch.subjectId = '5c0a6b6d-7c6b-4f35-9d5b-9fd0fd8e8a01' AND c.type = 'MATH PROBLEM' AND c.problemId IS NOT NULL AND c.problemId != '' AND (
                         EXISTS (SELECT 1 FROM progress p WHERE p.itemId = c.conceptId AND p.studentId = :studentId AND p.itemType = 'MATH_AGENT' AND p.status = :completedStatus AND p.language = :language AND p.appName = :appName)
                         OR
                         EXISTS (SELECT 1 FROM progress p WHERE p.itemId = c.conceptId AND p.studentId = :studentId AND p.itemType = 'CONCEPT' AND p.status = :completedStatus AND p.language = :language AND p.appName = :appName)
                     ))
                     OR
                     (ch.subjectId != '5c0a6b6d-7c6b-4f35-9d5b-9fd0fd8e8a01' AND c.type = 'STUDY' AND EXISTS (
                         SELECT 1 FROM progress p WHERE p.itemId = c.conceptId AND p.studentId = :studentId AND p.itemType = 'CONCEPT' AND p.status = :completedStatus AND p.language = :language AND p.appName = :appName
                     ))
                 )
                ) AS completedStudy,

                -- 2. SIMULATION components total and completed
                (SELECT COUNT(*) FROM concepts c 
                 WHERE c.chapterId = ch.chapterId 
                 AND c.type = 'SIMULATION'
                 AND (
                     CASE 
                         WHEN :language = 'en' THEN
                             (c.simulationId IS NOT NULL AND c.simulationId != '' AND c.simulationId != 'null' AND c.simulationId != 'Not found' AND c.simulationId != 'Not Found') 
                             OR (c.simulationUrl IS NOT NULL AND c.simulationUrl != '' AND c.simulationUrl != 'null' AND c.simulationUrl != 'Not found' AND c.simulationUrl != 'Not Found')
                         WHEN :language = 'kn' THEN
                             (c.simulationIdKannada IS NOT NULL AND c.simulationIdKannada != '' AND c.simulationIdKannada != 'null' AND c.simulationIdKannada != 'Not found' AND c.simulationIdKannada != 'Not Found') 
                             OR (c.simulationUrlKannada IS NOT NULL AND c.simulationUrlKannada != '' AND c.simulationUrlKannada != 'null' AND c.simulationUrlKannada != 'Not found' AND c.simulationUrlKannada != 'Not Found')
                         ELSE 1
                     END
                 )
                ) AS totalSim,

                (SELECT COUNT(*) FROM concepts c 
                 WHERE c.chapterId = ch.chapterId 
                 AND c.type = 'SIMULATION'
                 AND (
                     CASE 
                         WHEN :language = 'en' THEN
                             (c.simulationId IS NOT NULL AND c.simulationId != '' AND c.simulationId != 'null' AND c.simulationId != 'Not found' AND c.simulationId != 'Not Found') 
                             OR (c.simulationUrl IS NOT NULL AND c.simulationUrl != '' AND c.simulationUrl != 'null' AND c.simulationUrl != 'Not found' AND c.simulationUrl != 'Not Found')
                         WHEN :language = 'kn' THEN
                             (c.simulationIdKannada IS NOT NULL AND c.simulationIdKannada != '' AND c.simulationIdKannada != 'null' AND c.simulationIdKannada != 'Not found' AND c.simulationIdKannada != 'Not Found') 
                             OR (c.simulationUrlKannada IS NOT NULL AND c.simulationUrlKannada != '' AND c.simulationUrlKannada != 'null' AND c.simulationUrlKannada != 'Not found' AND c.simulationUrlKannada != 'Not Found')
                         ELSE 1
                     END
                 )
                 AND (
                     EXISTS (SELECT 1 FROM progress p WHERE p.itemId = c.conceptId AND p.studentId = :studentId AND p.itemType = 'SIMULATION_AGENT' AND p.status = :completedStatus AND p.language = :language AND p.appName = :appName)
                     OR
                     EXISTS (SELECT 1 FROM progress p WHERE p.itemId = c.conceptId AND p.studentId = :studentId AND p.itemType = 'SIMULATION' AND p.status = :completedStatus AND p.language = :language AND p.appName = :appName)
                 )
                ) AS completedSim,

                -- 3. REVISION component availability and completion
                (CASE WHEN ch.revisionId IS NOT NULL AND ch.revisionId != '' THEN 1 ELSE 0 END) AS hasRevision,
                
                (CASE WHEN (ch.revisionId IS NOT NULL AND ch.revisionId != '') AND EXISTS (
                    SELECT 1 FROM progress p 
                    INNER JOIN concepts c ON p.itemId = c.conceptId 
                    WHERE c.chapterId = ch.chapterId 
                    AND p.studentId = :studentId 
                    AND p.itemType = 'REVISION_AGENT' 
                    AND p.status = :completedStatus 
                    AND p.language = :language 
                    AND p.appName = :appName
                 ) THEN 1 ELSE 0 END) AS completedRevision,

                 ch.orderIndex
            FROM chapters ch
            WHERE ch.subjectId = :subjectId
        )
        SELECT 
            cc.chapterId,
            cc.chapterName,
            cc.chapterNameKannada,
            (cc.totalStudy + cc.totalSim) AS totalConcepts,
            (cc.completedStudy + cc.completedSim) AS completedConcepts,
            CAST(
                (
                    (CASE WHEN cc.totalStudy > 0 THEN (cc.completedStudy * 100 / cc.totalStudy) ELSE 0 END) +
                    (CASE WHEN cc.totalSim > 0 THEN (cc.completedSim * 100 / cc.totalSim) ELSE 0 END) +
                    (CASE WHEN cc.hasRevision > 0 THEN (cc.completedRevision * 100) ELSE 0 END)
                ) / 
                CASE 
                    WHEN (
                        (CASE WHEN cc.totalStudy > 0 THEN 1 ELSE 0 END) +
                        (CASE WHEN cc.totalSim > 0 THEN 1 ELSE 0 END) +
                        (CASE WHEN cc.hasRevision > 0 THEN 1 ELSE 0 END)
                    ) = 0 THEN 1
                    ELSE (
                        (CASE WHEN cc.totalStudy > 0 THEN 1 ELSE 0 END) +
                        (CASE WHEN cc.totalSim > 0 THEN 1 ELSE 0 END) +
                        (CASE WHEN cc.hasRevision > 0 THEN 1 ELSE 0 END)
                    )
                END
            AS INTEGER) AS completionPercentage
        FROM chapter_counts cc
        LEFT JOIN progress p_dummy ON p_dummy.studentId = :studentId AND 1=0
        ORDER BY cc.orderIndex ASC
        """
    )
    fun getChapterWiseProgressFlow(
        studentId: String,
        subjectId: String,
        language: String,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Flow<List<ChapterProgressSummary>>

    @Query(
        """
        SELECT 
            (SELECT COUNT(DISTINCT itemId) 
             FROM progress 
             WHERE studentId = :studentId 
             AND itemType = 'CONCEPT'
             AND status = :completedStatus
             AND (language = :language OR (:language = 'en' AND language = 'legacy'))
             AND appName = :appName)
            +
            (SELECT COUNT(DISTINCT c.chapterId)
             FROM progress p
             INNER JOIN concepts c ON p.itemId = c.conceptId
             WHERE p.studentId = :studentId
             AND p.itemType = 'REVISION_AGENT'
             AND p.status = :completedStatus
             AND (p.language = :language OR (:language = 'en' AND p.language = 'legacy'))
             AND p.appName = :appName)
    """
    )
    fun getTotalCompletedConceptsFlow(
        studentId: String,
        language: String,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Flow<Int>

    /**
     * Get the total number of completed simulation-type concepts for a specific language.
     * A simulation concept is COMPLETED if ANY of its components (Agent OR URL) is marked COMPLETED.
     * This matches the concept card UI logic in ConceptViewModel.determineSimulationStatus().
     */
    @Query(
        """
        SELECT COUNT(DISTINCT p.itemId)
        FROM progress p
        INNER JOIN concepts c ON p.itemId = c.conceptId
        WHERE c.type = 'SIMULATION'
        AND p.studentId = :studentId
        AND p.itemType IN ('SIMULATION_AGENT', 'SIMULATION')
        AND p.status = :completedStatus
        AND (p.language = :language OR (:language = 'en' AND p.language = 'legacy'))
        AND p.appName = :appName
    """
    )
    fun getTotalCompletedSimulationsFlow(
        studentId: String,
        language: String,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Flow<Int>

    /**
     * Get the number of concepts cleared in the last 7 days, day-wise Returns a list of
     * DailyConceptCount with date and count Ordered from most recent (today) to 7 days ago
     */
    @Query(
        """
        SELECT 
            DATE(completedAt / 1000, 'unixepoch', 'localtime') as date,
            COUNT(DISTINCT itemId) as count
        FROM progress
        WHERE studentId = :studentId
        AND itemType IN ('CONCEPT', 'REVISION_AGENT')
        AND status = :completedStatus
        AND completedAt >= :sevenDaysAgoTimestamp
        AND appName = :appName
        GROUP BY DATE(completedAt / 1000, 'unixepoch', 'localtime')
        ORDER BY date DESC
    """
    )
    suspend fun getConceptsClearedLast7Days(
        studentId: String,
        sevenDaysAgoTimestamp: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): List<DailyConceptCount>

    /**
     * Get count of CONCEPT items completed today (Reactive Flow).
     * Only counts itemType='CONCEPT' — math completions also write CONCEPT rows via markMathAgentCompleted.
     */
    @Query(
        """
        SELECT 
            (SELECT COUNT(DISTINCT itemId) 
             FROM progress 
             WHERE studentId = :studentId 
             AND itemType = 'CONCEPT'
             AND status = :completedStatus
             AND language IN ('en', 'kn')
             AND language = :language
             AND completedAt BETWEEN :startOfDay AND :endOfDay
             AND appName = :appName)
            +
            (SELECT COUNT(DISTINCT c.chapterId)
             FROM progress p
             INNER JOIN concepts c ON p.itemId = c.conceptId
             WHERE p.studentId = :studentId
             AND p.itemType = 'REVISION_AGENT'
             AND p.status = :completedStatus
             AND p.language IN ('en', 'kn')
             AND p.language = :language
             AND p.completedAt BETWEEN :startOfDay AND :endOfDay
             AND p.appName = :appName)
    """
    )
    fun getTodayCompletedConceptCountFlow(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Flow<Int>

    /**
     * Get count of CONCEPT items completed today (Synchronous).
     * Only counts itemType='CONCEPT' — math completions also write CONCEPT rows via markMathAgentCompleted.
     */
    @Query(
        """
        SELECT 
            (SELECT COUNT(DISTINCT itemId) 
             FROM progress 
             WHERE studentId = :studentId 
             AND itemType = 'CONCEPT'
             AND status = :completedStatus
             AND language IN ('en', 'kn')
             AND language = :language
             AND completedAt BETWEEN :startOfDay AND :endOfDay
             AND appName = :appName)
            +
            (SELECT COUNT(DISTINCT c.chapterId)
             FROM progress p
             INNER JOIN concepts c ON p.itemId = c.conceptId
             WHERE p.studentId = :studentId
             AND p.itemType = 'REVISION_AGENT'
             AND p.status = :completedStatus
             AND p.language IN ('en', 'kn')
             AND p.language = :language
             AND p.completedAt BETWEEN :startOfDay AND :endOfDay
             AND p.appName = :appName)
    """
    )
    suspend fun getTodayCompletedConceptCount(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Int

    /**
     * Get count of simulation concepts completed today (Reactive Flow).
     * A simulation concept counts as completed today if ANY component (Agent OR URL)
     * was marked COMPLETED today. This matches the concept card OR logic.
     */
    @Query(
        """
    SELECT COUNT(DISTINCT p.itemId)
    FROM progress p
    INNER JOIN concepts c ON p.itemId = c.conceptId
    WHERE c.type = 'SIMULATION'
    AND p.studentId = :studentId
    AND p.itemType IN ('SIMULATION_AGENT', 'SIMULATION')
    AND p.status = :completedStatus
    AND p.language IN ('en', 'kn')
    AND p.language = :language
    AND p.appName = :appName
    AND p.completedAt BETWEEN :startOfDay AND :endOfDay
    """
    )
    fun getTodayCompletedSimulationCountFlow(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Flow<Int>

    /**
     * Get count of SIMULATION completed today (Synchronous) */
    @Query(
        """
    SELECT COUNT(DISTINCT itemId) 
    FROM progress
    WHERE studentId = :studentId
      AND itemType IN ('SIMULATION', 'SIMULATION_AGENT')
      AND status = :completedStatus
      AND language IN ('en', 'kn')
      AND language = :language
      AND completedAt BETWEEN :startOfDay AND :endOfDay
      AND appName = :appName
      AND itemId IS NOT NULL AND itemId != ''
    """
    )
    suspend fun getTodayCompletedSimulationCount(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Int

    /**
     * Get daily activity: count any completed activity (concept/simulation/revision) on a specific date
     * This is used for streak calculation - ANY activity counts toward the streak
     * Returns count of activities per day
     */
    @Query(
        """
        SELECT 
            DATE(completedAt / 1000, 'unixepoch', 'localtime') as date,
            COUNT(*) as count
        FROM progress
        WHERE studentId = :studentId
          AND status = :completedStatus
          AND completedAt >= :sevenDaysAgoTimestamp
          AND appName = :appName
          AND itemType IN ('CONCEPT', 'SIMULATION', 'SIMULATION_AGENT', 'REVISION_AGENT', 'MATH_AGENT', 'SCIENCE_AGENT')
        GROUP BY DATE(completedAt / 1000, 'unixepoch', 'localtime')
        ORDER BY date DESC
        """
    )
    suspend fun getDailyCompletedActivityLast7Days(
        studentId: String,
        sevenDaysAgoTimestamp: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): List<DailyConceptCount>

    /**
     * Get count of completed activities today (any activity: concept/simulation/revision)
     * Used for streak tracking - ANY completed activity counts
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM progress
        WHERE studentId = :studentId
          AND status = :completedStatus
          AND completedAt BETWEEN :startOfDay AND :endOfDay
          AND appName = :appName
          AND itemType IN ('CONCEPT', 'SIMULATION', 'SIMULATION_AGENT', 'REVISION_AGENT', 'MATH_AGENT', 'SCIENCE_AGENT')
        """
    )
    suspend fun getTodayFullyCompletedActivityCount(
        studentId: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT itemId)
        FROM progress
        WHERE studentId = :studentId
          AND itemType = 'CONCEPT'
          AND itemId IN (:conceptIds)
          AND status = :completedStatus
          AND language IN ('en', 'kn')
          AND language = :language
          AND completedAt BETWEEN :startOfDay AND :endOfDay
          AND appName = :appName
        """,
    )
    suspend fun countTodayCompletedConceptsForIds(
        studentId: String,
        conceptIds: List<String>,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value,
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT itemId)
        FROM progress
        WHERE studentId = :studentId
          AND itemType = 'REVISION_AGENT'
          AND status = :completedStatus
          AND language IN ('en', 'kn')
          AND language = :language
          AND completedAt BETWEEN :startOfDay AND :endOfDay
          AND appName = :appName
        """,
    )
    suspend fun countTodayCompletedRevisions(
        studentId: String,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value,
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT itemId)
        FROM progress
        WHERE studentId = :studentId
          AND itemType = 'REVISION_AGENT'
          AND itemId IN (:conceptIds)
          AND status = :completedStatus
          AND language IN ('en', 'kn')
          AND language = :language
          AND completedAt BETWEEN :startOfDay AND :endOfDay
          AND appName = :appName
        """,
    )
    suspend fun countTodayCompletedRevisionsForConcepts(
        studentId: String,
        conceptIds: List<String>,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value,
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT p.itemId)
        FROM progress p
        INNER JOIN concepts c ON p.itemId = c.conceptId
        WHERE p.studentId = :studentId
          AND c.chapterId IN (:chapterIds)
          AND p.status = :completedStatus
          AND p.language IN ('en', 'kn')
          AND p.language = :language
          AND p.completedAt BETWEEN :startOfDay AND :endOfDay
          AND p.appName = :appName
        """,
    )
    suspend fun countTodayCompletedInChapters(
        studentId: String,
        chapterIds: List<String>,
        language: String,
        startOfDay: Long,
        endOfDay: Long,
        appName: String,
        completedStatus: String = ProgressStatus.COMPLETED.value,
    ): Int
}

/** Data class to hold daily concept completion count */
data class DailyConceptCount(
    val date: String, // Format: YYYY-MM-DD
    val count: Int
)

/**
 *  Data class to hold chapter-wise progress
 * This is used by ProgressScreenViewModel to display chapter progress
 * Updated in real-time via Flow from getChapterWiseProgressFlow()
 */
data class ChapterProgressSummary(
    val chapterId: String,
    val chapterName: String,
    val chapterNameKannada: String = "",
    val totalConcepts: Int,
    val completedConcepts: Int,
    val completionPercentage: Int  // Changed from Float to Int for consistency
)