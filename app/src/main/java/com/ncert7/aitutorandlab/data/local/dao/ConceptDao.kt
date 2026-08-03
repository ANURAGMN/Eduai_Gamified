package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import kotlinx.coroutines.flow.Flow

//Concept DAO
@Dao
interface ConceptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcepts(concepts: List<ConceptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcept(concept: ConceptEntity)

    @Update
    suspend fun updateConcept(concept: ConceptEntity)

    @Query("SELECT * FROM concepts ORDER BY chapterId ASC, orderIndex ASC")
    suspend fun getAllConceptsSync(): List<ConceptEntity>

    @Query("SELECT COUNT(*) FROM concepts")
    suspend fun getConceptCount(): Int

    @Query("SELECT * FROM concepts WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    fun getConceptsForChapter(chapterId: String): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts WHERE chapterId = :chapterId AND type = :type COLLATE NOCASE ORDER BY orderIndex ASC")
    suspend fun getConceptsForChapterSync(chapterId: String, type: String): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE conceptId = :conceptId")
    suspend fun getConcept(conceptId: String): ConceptEntity?

    @Query("SELECT * FROM concepts WHERE conceptId = :conceptId")
    fun getConceptFlow(conceptId: String): Flow<ConceptEntity?>

    @Query("SELECT * FROM concepts WHERE problemId = :problemId LIMIT 1")
    suspend fun getConceptByProblemId(problemId: String): ConceptEntity?

    // Get next 2 concepts to show as "locked" in UI
    @Query("SELECT * FROM concepts WHERE chapterId = :chapterId AND orderIndex > :currentIndex ORDER BY orderIndex ASC LIMIT :limit")
    suspend fun getNextConcepts(
        chapterId: String,
        currentIndex: Int,
        limit: Int = 2
    ): List<ConceptEntity>

    @Query("DELETE FROM concepts WHERE chapterId = :chapterId")
    suspend fun deleteConceptsForChapter(chapterId: String)

    @Query("SELECT * FROM concepts WHERE conceptId IN (:conceptIds)")
    fun getConceptsByIds(conceptIds: List<String>): Flow<List<ConceptEntity>>

    @Query("DELETE FROM concepts WHERE conceptId = :conceptId")
    suspend fun deleteConcept(conceptId: String)

    @Query("DELETE FROM concepts")
    suspend fun deleteAllConcepts()


    /**
     * Progress for home screen today progress section
     */
    @Query(
        """
    SELECT * FROM concepts
    WHERE orderIndex = :orderIndex AND type = :type COLLATE NOCASE
    ORDER BY orderIndex ASC
    LIMIT :limit
    """
    )
    suspend fun getFirstConceptsOfChapter(
        orderIndex: String,
        type: String,
        limit: Int
    ): List<ConceptEntity>

    // ============================
    // STUDY TYPE CONCEPTS
    // ============================
    /**
     * Get all STUDY type concepts for a chapter
     * Study concepts have no language filtering - they load the same regardless of language
     * These are used for non-math subjects
     */
    @Query(
        """
        SELECT * FROM concepts
        WHERE chapterId = :chapterId
        AND type = 'STUDY' COLLATE NOCASE
        ORDER BY orderIndex ASC
        """
    )
    suspend fun getStudyConceptsForChapter(chapterId: String): List<ConceptEntity>

    // ============================
    // MATH PROBLEM TYPE CONCEPTS
    // ============================
    /**
     * Get all MATH PROBLEM type concepts for a chapter
     * Only loads concepts that have a valid problemId (not null, not empty)
     * Language filtering happens at backend API level
     * Used exclusively for Math subject
     */
    @Query(
        """
        SELECT * FROM concepts 
        WHERE chapterId = :chapterId 
        AND type = 'MATH PROBLEM'
        AND (problemId IS NOT NULL AND problemId != '')
        ORDER BY orderIndex ASC
        """
    )
    suspend fun getMathProblemConceptsForChapter(chapterId: String): List<ConceptEntity>

    // ============================
    // SIMULATION TYPE CONCEPTS
    // ============================
    /**
     * Get all SIMULATION type concepts for a chapter filtered by language
     * For English (language = 'en'): only load concepts that have simulationId OR simulationUrl (English fields)
     * For Kannada (language = 'kn'): only load concepts that have simulationIdKannada OR simulationUrlKannada
     * This ensures we don't display incomplete simulations
     */
    @Query(
        """
        SELECT * FROM concepts
        WHERE chapterId = :chapterId
        AND type = 'SIMULATION' COLLATE NOCASE
        AND (
            CASE 
                WHEN :language = 'en' THEN
                    (simulationId IS NOT NULL AND simulationId != '' AND simulationId != 'null' AND simulationId != 'Not found' AND simulationId != 'Not Found') 
                    OR (simulationUrl IS NOT NULL AND simulationUrl != '' AND simulationUrl != 'null' AND simulationUrl != 'Not found' AND simulationUrl != 'Not Found')
                WHEN :language = 'kn' THEN
                    (simulationIdKannada IS NOT NULL AND simulationIdKannada != '' AND simulationIdKannada != 'null' AND simulationIdKannada != 'Not found' AND simulationIdKannada != 'Not Found') 
                    OR (simulationUrlKannada IS NOT NULL AND simulationUrlKannada != '' AND simulationUrlKannada != 'null' AND simulationUrlKannada != 'Not found' AND simulationUrlKannada != 'Not Found')
                ELSE 1
            END
        )
        ORDER BY orderIndex ASC
        """
    )
    suspend fun getSimulationConceptsForChapter(
        chapterId: String,
        language: String
    ): List<ConceptEntity>

    // ============================
    // COUNT QUERIES FOR CHAPTER FILTERING
    // ============================
    /**
     * Check if a chapter has any STUDY type concepts
     * Used to determine if "Study" button should be shown on ChapterCard
     * Returns count of available STUDY concepts
     */
    @Query(
        """
        SELECT COUNT(*) FROM concepts
        WHERE chapterId = :chapterId
        AND type = 'STUDY' COLLATE NOCASE
        """
    )
    suspend fun getStudyConceptCount(chapterId: String): Int

    /**
     * Check if a chapter has any MATH PROBLEM type concepts with valid problemId
     * Used to determine if "Study" button should be shown on ChapterCard for Math subject
     * Returns count of available MATH PROBLEM concepts
     */
    @Query(
        """
        SELECT COUNT(*) FROM concepts 
        WHERE chapterId = :chapterId 
        AND type = 'MATH PROBLEM'
        AND (problemId IS NOT NULL AND problemId != '')
        """
    )
    suspend fun getMathProblemConceptCount(chapterId: String): Int

    /**
     * Check if a chapter has any SIMULATION type concepts for a specific language
     * Used to determine if "Simulation" button should be shown on ChapterCard
     * Returns count of available SIMULATION concepts for the language
     */
    @Query(
        """
        SELECT COUNT(*) FROM concepts
        WHERE chapterId = :chapterId
        AND type = 'SIMULATION' COLLATE NOCASE
        AND (
            CASE 
                WHEN :language = 'en' THEN
                    (simulationId IS NOT NULL AND simulationId != '' AND simulationId != 'null' AND simulationId != 'Not found' AND simulationId != 'Not Found') 
                    OR (simulationUrl IS NOT NULL AND simulationUrl != '' AND simulationUrl != 'null' AND simulationUrl != 'Not found' AND simulationUrl != 'Not Found')
                WHEN :language = 'kn' THEN
                    (simulationIdKannada IS NOT NULL AND simulationIdKannada != '' AND simulationIdKannada != 'null' AND simulationIdKannada != 'Not found' AND simulationIdKannada != 'Not Found') 
                    OR (simulationUrlKannada IS NOT NULL AND simulationUrlKannada != '' AND simulationUrlKannada != 'null' AND simulationUrlKannada != 'Not found' AND simulationUrlKannada != 'Not Found')
                ELSE 1
            END
        )
        """
    )
    suspend fun getSimulationConceptCount(chapterId: String, language: String): Int

    /**
     * Get a chapter by its ID
     */
    @Query("SELECT * FROM chapters WHERE chapterId = :chapterId")
    suspend fun getChapter(chapterId: String): ChapterEntity?

    /**
     * Get the chapter that contains a specific concept.
     */
    @Query("""
        SELECT chapters.* FROM chapters 
        INNER JOIN concepts ON chapters.chapterId = concepts.chapterId 
        WHERE concepts.conceptId = :conceptId
    """)
    suspend fun getChapterForConcept(conceptId: String): ChapterEntity?
}