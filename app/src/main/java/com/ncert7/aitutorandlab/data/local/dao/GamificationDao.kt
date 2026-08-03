package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.GamificationProfileEntity
import com.ncert7.aitutorandlab.data.local.entities.GemEventEntity
import com.ncert7.aitutorandlab.data.local.entities.XpEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {

    @Query("SELECT * FROM gamification_profile WHERE studentId = :studentId LIMIT 1")
    fun observeProfile(studentId: String): Flow<GamificationProfileEntity?>

    @Query("SELECT * FROM gamification_profile WHERE studentId = :studentId LIMIT 1")
    suspend fun getProfile(studentId: String): GamificationProfileEntity?

    @Query("SELECT * FROM gamification_profile WHERE friendCode = :friendCode LIMIT 1")
    suspend fun getProfileByFriendCode(friendCode: String): GamificationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: GamificationProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertXpEvent(event: XpEventEntity): Long

    @Query(
        """
        SELECT COUNT(*) FROM xp_event
        WHERE studentId = :studentId
          AND itemType = :itemType
          AND itemId = :itemId
          AND language = :language
        """,
    )
    suspend fun countXpEvent(
        studentId: String,
        itemType: String,
        itemId: String,
        language: String,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGemEvent(event: GemEventEntity): Long

    @Query(
        """
        SELECT COUNT(*) FROM gem_event
        WHERE studentId = :studentId AND grantKey = :grantKey
        """,
    )
    suspend fun countGemEvent(studentId: String, grantKey: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM gem_event
        WHERE studentId = :studentId
          AND grantKey LIKE 'quest_%'
          AND createdAt BETWEEN :startOfDay AND :endOfDay
        """,
    )
    suspend fun countQuestGemGrantsToday(
        studentId: String,
        startOfDay: Long,
        endOfDay: Long,
    ): Int
}
