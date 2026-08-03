package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.*
import com.ncert7.aitutorandlab.data.local.entities.ChapterAgentProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing chapter-wise agent progress tracking
 * Stores progress for Study Agent, Simulation Agent, and Revision Agent per chapter and per language
 */
@Dao
interface ChapterAgentProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapterProgress(progress: ChapterAgentProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ChapterAgentProgressEntity>)

    @Update
    suspend fun updateChapterProgress(progress: ChapterAgentProgressEntity)

    @Query(
        """
        SELECT * FROM chapter_agent_progress 
        WHERE studentId = :studentId 
        AND chapterId = :chapterId 
        AND language = :language
        AND appName = :appName
        LIMIT 1
        """
    )
    suspend fun getChapterProgress(
        studentId: String,
        chapterId: String,
        language: String,
        appName: String
    ): ChapterAgentProgressEntity?

    @Query("SELECT COUNT(*) FROM chapter_agent_progress WHERE studentId = :studentId AND appName = :appName")
    suspend fun getChapterProgressCount(studentId: String, appName: String): Int

    @Query(
        """
        SELECT * FROM chapter_agent_progress 
        WHERE studentId = :studentId 
        AND chapterId = :chapterId 
        AND language = :language
        AND appName = :appName
        LIMIT 1
        """
    )
    fun getChapterProgressFlow(
        studentId: String,
        chapterId: String,
        language: String,
        appName: String
    ): Flow<ChapterAgentProgressEntity?>

    @Query(
        """
        SELECT * FROM chapter_agent_progress 
        WHERE studentId = :studentId 
        AND language = :language
        AND appName = :appName
        ORDER BY chapterId ASC
        """
    )
    suspend fun getAllChapterProgress(studentId: String, language: String, appName: String): List<ChapterAgentProgressEntity>

    @Query(
        """
        SELECT * FROM chapter_agent_progress 
        WHERE studentId = :studentId 
        AND language = :language
        AND appName = :appName
        ORDER BY chapterId ASC
        """
    )
    fun getAllChapterProgressFlow(studentId: String, language: String, appName: String): Flow<List<ChapterAgentProgressEntity>>

    @Query(
        """
        SELECT * FROM chapter_agent_progress 
        WHERE studentId = :studentId 
        AND language = :language
        AND status = :completedStatus
        AND appName = :appName
        """
    )
    suspend fun getCompletedChapters(
        studentId: String,
        language: String,
        appName: String,
        completedStatus: String = "COMPLETED"
    ): List<ChapterAgentProgressEntity>

    @Query(
        """
        SELECT COUNT(*) 
        FROM chapter_agent_progress 
        WHERE studentId = :studentId 
        AND language = :language
        AND status = :completedStatus
        AND appName = :appName
        """
    )
    suspend fun getCompletedChaptersCount(
        studentId: String,
        language: String,
        appName: String,
        completedStatus: String = "COMPLETED"
    ): Int

    @Query(
        """
        DELETE FROM chapter_agent_progress 
        WHERE studentId = :studentId 
        AND chapterId = :chapterId 
        AND language = :language
        AND appName = :appName
        """
    )
    suspend fun deleteChapterProgress(
        studentId: String,
        chapterId: String,
        language: String,
        appName: String
    )

    @Transaction
    suspend fun updateChapterAgentProgress(
        studentId: String,
        chapterId: String,
        language: String,
        appName: String,
        studyPercentage: Int,
        simulationPercentage: Int,
        revisionPercentage: Int,
        overallPercentage: Int   // Precomputed by ChapterProgressCalculator (accounts for /2 vs /3)
    ) {
        val existing = getChapterProgress(studentId, chapterId, language, appName)

        // Use the precomputed overall — do NOT recompute here, because the calculator
        // already chose the correct divisor (2 without revision agent, 3 with).
        val clampedOverall = overallPercentage.coerceIn(0, 100)

        // Determine status based on overall percentage
        val status = when {
            clampedOverall >= 100 -> "COMPLETED"
            clampedOverall > 0   -> "IN_PROGRESS"
            else                 -> "NOT_STARTED"
        }

        val timestamp = System.currentTimeMillis()

        if (existing != null) {
            val updated = existing.copy(
                studyPercentage = studyPercentage.coerceIn(0, 100),
                simulationPercentage = simulationPercentage.coerceIn(0, 100),
                revisionPercentage = revisionPercentage.coerceIn(0, 100),
                overallPercentage = clampedOverall,
                status = status,
                completedAt = if (status == "COMPLETED") timestamp else existing.completedAt,
                updatedAt = timestamp,
                isSynced = false
            )
            updateChapterProgress(updated)
        } else {
            insertChapterProgress(
                ChapterAgentProgressEntity(
                    studentId = studentId,
                    chapterId = chapterId,
                    language = language,
                    appName = appName,
                    studyPercentage = studyPercentage.coerceIn(0, 100),
                    simulationPercentage = simulationPercentage.coerceIn(0, 100),
                    revisionPercentage = revisionPercentage.coerceIn(0, 100),
                    overallPercentage = clampedOverall,
                    status = status,
                    updatedAt = timestamp
                )
            )
        }
    }

    /**
     * Get chapter progress summary for display on chapter list
     * Returns chapters with their overall progress percentage
     */
    @Query(
        """
        SELECT 
    ch.chapterId,
    ch.chapterName,
    COALESCE(cap.overallPercentage, 0) AS completionPercentage,
    COALESCE(cap.status, 'NOT_STARTED') AS status
FROM chapters ch
LEFT JOIN chapter_agent_progress cap 
    ON cap.chapterId = ch.chapterId
    AND cap.studentId = :studentId
    AND cap.language = :language
    AND cap.appName = :appName
WHERE ch.subjectId = :subjectId
ORDER BY ch.orderIndex ASC
        """
    )
    suspend fun getChapterWiseProgressSummary(
        studentId: String,
        language: String,
        subjectId :String,
        appName: String
    ): List<ChapterProgressSummaryDto>

    /**
     * Get all chapters for a subject with progress
     */
    @Query(
        """
        SELECT 
            cap.chapterId,
            ch.chapterName,
            cap.overallPercentage as completionPercentage,
            cap.status
        FROM chapter_agent_progress cap
        INNER JOIN chapters ch ON cap.chapterId = ch.chapterId
        WHERE cap.studentId = :studentId 
        AND cap.language = :language
        AND cap.appName = :appName
        AND ch.subjectId = :subjectId
        ORDER BY ch.orderIndex ASC
        """
    )
    suspend fun getChapterProgressBySubject(
        studentId: String,
        subjectId: String,
        language: String,
        appName: String
    ): List<ChapterProgressSummaryDto>
    @Query("SELECT * FROM chapter_agent_progress WHERE isSynced = 0")
    suspend fun getUnsyncedProgress(): List<ChapterAgentProgressEntity>

    @Query("UPDATE chapter_agent_progress SET isSynced = 1 WHERE progressId IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}

/**
 * DTO for chapter progress summary display
 */
data class ChapterProgressSummaryDto(
    val chapterId: String,
    val chapterName: String,
    val completionPercentage: Int,
    val status: String
)
