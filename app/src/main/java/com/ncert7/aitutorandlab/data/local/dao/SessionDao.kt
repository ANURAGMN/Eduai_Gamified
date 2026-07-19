package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ncert7.aitutorandlab.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing study sessions in the local database.
 */
@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Update
    suspend fun updateSession(session: SessionEntity)

    // Get a specific session
    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): SessionEntity?

    // Get all sessions (for current user, sorted by most recent)
    @Query("SELECT * FROM sessions ORDER BY sessionStartTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    // Get sessions for a specific date
    @Query("SELECT * FROM sessions WHERE sessionDate = :date ORDER BY sessionStartTime DESC")
    suspend fun getSessionsForDate(date: String): List<SessionEntity>

    // Get the latest session
    @Query("SELECT * FROM sessions ORDER BY sessionStartTime DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?

    // Get session count for a date
    @Query("SELECT COUNT(*) FROM sessions WHERE sessionDate = :date")
    suspend fun getSessionCountForDate(date: String): Int

    // Get unsynced sessions (for cloud backup)
    @Query("SELECT * FROM sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<SessionEntity>

    // Mark session as synced
    @Query("UPDATE sessions SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionAsSynced(sessionId: String)

    @Query("UPDATE sessions SET studentId = :studentId, isSynced = 0 WHERE studentId = ''")
    suspend fun backfillEmptyStudentId(studentId: String)

    // Get open (unclosed) sessions for a student — used to cap orphan inflation
    @Query(
        "SELECT * FROM sessions WHERE studentId = :studentId AND sessionEndTime IS NULL ORDER BY sessionStartTime ASC"
    )
    suspend fun getOpenSessionsForStudent(studentId: String): List<SessionEntity>

    // Delete a session
    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    // Delete all sessions (useful for logout/testing)
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
}