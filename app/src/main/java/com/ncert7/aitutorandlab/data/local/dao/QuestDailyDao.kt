package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.QuestDailyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDailyDao {

    @Query(
        """
        SELECT * FROM quest_daily
        WHERE studentId = :studentId AND questDate = :questDate
        LIMIT 1
        """,
    )
    fun observeQuest(studentId: String, questDate: String): Flow<QuestDailyEntity?>

    @Query(
        """
        SELECT * FROM quest_daily
        WHERE studentId = :studentId AND questDate = :questDate
        LIMIT 1
        """,
    )
    suspend fun getQuest(studentId: String, questDate: String): QuestDailyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuest(entity: QuestDailyEntity)

    @Query(
        """
        UPDATE quest_daily SET simsClaimed = 1, updatedAt = :updatedAt, isSynced = 0
        WHERE studentId = :studentId AND questDate = :questDate
        """,
    )
    suspend fun markSimsClaimed(studentId: String, questDate: String, updatedAt: Long)

    @Query(
        """
        UPDATE quest_daily SET studyClaimed = 1, updatedAt = :updatedAt, isSynced = 0
        WHERE studentId = :studentId AND questDate = :questDate
        """,
    )
    suspend fun markStudyClaimed(studentId: String, questDate: String, updatedAt: Long)

    @Query(
        """
        UPDATE quest_daily SET bonusClaimed = 1, updatedAt = :updatedAt, isSynced = 0
        WHERE studentId = :studentId AND questDate = :questDate
        """,
    )
    suspend fun markBonusClaimed(studentId: String, questDate: String, updatedAt: Long)
}
