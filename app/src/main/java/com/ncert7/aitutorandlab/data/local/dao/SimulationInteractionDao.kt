package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.SimulationInteractionEntity

@Dao
interface SimulationInteractionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: SimulationInteractionEntity): Long

    @Query("""
        SELECT * FROM simulation_interactions
        WHERE studentId = :studentId
        AND interactionDate = :interactionDate
        AND isSynced = 0
        ORDER BY occurredAt ASC, interactionId ASC
    """)
    suspend fun getUnsyncedInteractionsForDay(
        studentId: String,
        interactionDate: String
    ): List<SimulationInteractionEntity>

    @Query("""
        SELECT * FROM simulation_interactions
        WHERE studentId = :studentId
        AND interactionDate = :interactionDate
        ORDER BY occurredAt ASC, interactionId ASC
    """)
    suspend fun getInteractionsForDay(
        studentId: String,
        interactionDate: String
    ): List<SimulationInteractionEntity>

    @Query("""
        UPDATE simulation_interactions
        SET isSynced = 1
        WHERE interactionId IN (:interactionIds)
    """)
    suspend fun markInteractionsAsSynced(interactionIds: List<Long>)

    @Query("""
        UPDATE simulation_interactions
        SET isCorrect = :verdict
        WHERE interactionId = (
            SELECT interactionId FROM simulation_interactions
            WHERE sessionId = :sessionId
            AND isCorrect = '-'
            ORDER BY occurredAt DESC, interactionId DESC
            LIMIT 1
        )
    """)
    suspend fun updateLatestPendingVerdict(sessionId: String, verdict: String)

    @Query("""
        UPDATE simulation_interactions
        SET timeTaken = :timeTaken
        WHERE interactionId = (
            SELECT interactionId FROM simulation_interactions
            WHERE sessionId = :sessionId
            AND simulationTitle = :simulationTitle
            AND subjectName = :subjectName
            AND chapterName = :chapterName
            ORDER BY occurredAt DESC, interactionId DESC
            LIMIT 1
        )
    """)
    suspend fun updateLatestSessionTime(
        sessionId: String,
        simulationTitle: String,
        subjectName: String,
        chapterName: String,
        timeTaken: String
    )
}
