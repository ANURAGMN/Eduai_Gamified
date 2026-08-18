package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing chapters in the local database.
 */
@Dao
interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    fun getChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    suspend fun getChaptersForSubjectSync(subjectId: String): List<ChapterEntity>

    /** Chapter counts per subject, for the Home subject rows ("N chapters"). */
    @Query("SELECT subjectId AS subjectId, COUNT(*) AS chapterCount FROM chapters GROUP BY subjectId")
    suspend fun getChapterCountsBySubject(): List<SubjectChapterCount>

    /** Reactive variant — emits again when chapters are synced/inserted. */
    @Query("SELECT subjectId AS subjectId, COUNT(*) AS chapterCount FROM chapters GROUP BY subjectId")
    fun getChapterCountsBySubjectFlow(): Flow<List<SubjectChapterCount>>

    @Query("SELECT * FROM chapters WHERE chapterId = :chapterId")
    suspend fun getChapter(chapterId: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE chapterId = :chapterId")
    fun getChapterFlow(chapterId: String): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE chapterId = :chapterId")
    suspend fun getChapterById(chapterId: String): ChapterEntity?

    /**
     * Get the chapter that contains a specific concept.
     * Unified query to jump from conceptId -> ChapterEntity.
     */
    @Query("""
        SELECT chapters.* FROM chapters 
        INNER JOIN concepts ON chapters.chapterId = concepts.chapterId 
        WHERE concepts.conceptId = :conceptId
    """)
    suspend fun getChapterForConcept(conceptId: String): ChapterEntity?

    @Query("DELETE FROM chapters WHERE subjectId = :subjectId")
    suspend fun deleteChaptersForSubject(subjectId: String)

    @Query("DELETE FROM chapters WHERE chapterId = :chapterId")
    suspend fun deleteChapter(chapterId: String)

    /**
     * Get all chapters synchronously (for debugging)
     */
    @Query("SELECT * FROM chapters ORDER BY orderIndex ASC")
    suspend fun getAllChaptersSync(): List<ChapterEntity>

    /**
     * Get all concepts for a chapter
     * Used for progress calculation
     */
    @Query("SELECT * FROM concepts WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    suspend fun getConceptsByChapterId(chapterId: String): List<ConceptEntity>

    /**
     * Get a single concept by its ID
     * Used to find chapter for a concept
     */
    @Query("SELECT * FROM concepts WHERE conceptId = :conceptId")
    suspend fun getConceptById(conceptId: String): ConceptEntity?

    /**
     * Find a chapter by its name (case-insensitive)
     * Used by RevisionViewModel to resolve chapter name → chapterId
     */
    @Query("SELECT * FROM chapters WHERE chapterName = :chapterName LIMIT 1")
    suspend fun getChapterByName(chapterName: String): ChapterEntity?
}

/** Room projection: number of chapters in a subject. */
data class SubjectChapterCount(
    val subjectId: String,
    val chapterCount: Int,
)