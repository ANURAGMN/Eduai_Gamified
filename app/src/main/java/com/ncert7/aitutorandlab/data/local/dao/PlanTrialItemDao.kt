package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanTrialItemDao {

    @Query(
        """
        SELECT * FROM plan_trial_item
        WHERE studentId = :studentId AND dayIndex = :dayIndex
        ORDER BY sequenceIndex ASC
        """,
    )
    fun observeItemsForDay(studentId: String, dayIndex: Int): Flow<List<PlanTrialItemEntity>>

    @Query(
        """
        SELECT * FROM plan_trial_item
        WHERE studentId = :studentId AND dayIndex = :dayIndex
        ORDER BY sequenceIndex ASC
        """,
    )
    suspend fun getItemsForDay(studentId: String, dayIndex: Int): List<PlanTrialItemEntity>

    @Query(
        """
        SELECT * FROM plan_trial_item
        WHERE studentId = :studentId AND planDayId = :planDayId
        ORDER BY sequenceIndex ASC
        """,
    )
    suspend fun getItemsForPlanDay(studentId: String, planDayId: Long): List<PlanTrialItemEntity>

    @Query("SELECT COUNT(*) FROM plan_trial_item WHERE studentId = :studentId AND planDayId = :planDayId")
    suspend fun countForPlanDay(studentId: String, planDayId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<PlanTrialItemEntity>)

    @Query("DELETE FROM plan_trial_item WHERE studentId = :studentId")
    suspend fun deleteAllForStudent(studentId: String)

    @Query("DELETE FROM plan_trial_item WHERE studentId = :studentId AND planDayId = :planDayId")
    suspend fun deleteForPlanDay(studentId: String, planDayId: Long)

    @Query(
        """
        UPDATE plan_trial_item
        SET status = :status,
            completedCount = :completedCount,
            celebrated = :celebrated,
            updatedAt = :updatedAt
        WHERE id = :itemId
        """,
    )
    suspend fun updateProgress(
        itemId: Long,
        status: String,
        completedCount: Int,
        celebrated: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE plan_trial_item
        SET requiredCount = :requiredCount,
            updatedAt = :updatedAt
        WHERE id = :itemId
        """,
    )
    suspend fun updateRequiredCount(
        itemId: Long,
        requiredCount: Int,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("SELECT * FROM plan_trial_item WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: Long): PlanTrialItemEntity?

    @Query(
        """
        SELECT * FROM plan_trial_item
        WHERE studentId = :studentId AND dayIndex = :dayIndex
          AND status = 'DONE' AND celebrated = 0
        ORDER BY sequenceIndex DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestUncelebratedDone(
        studentId: String,
        dayIndex: Int,
    ): PlanTrialItemEntity?

    @Query(
        """
        SELECT * FROM plan_trial_item
        WHERE studentId = :studentId AND dayIndex = :dayIndex
          AND status != 'DONE'
        ORDER BY sequenceIndex ASC
        LIMIT 1
        """,
    )
    suspend fun getNextIncomplete(
        studentId: String,
        dayIndex: Int,
    ): PlanTrialItemEntity?

    /** Next incomplete item strictly after [afterSequenceIndex] in today's queue. */
    @Query(
        """
        SELECT * FROM plan_trial_item
        WHERE studentId = :studentId AND dayIndex = :dayIndex
          AND status != 'DONE'
          AND sequenceIndex > :afterSequenceIndex
        ORDER BY sequenceIndex ASC
        LIMIT 1
        """,
    )
    suspend fun getNextIncompleteAfterSequence(
        studentId: String,
        dayIndex: Int,
        afterSequenceIndex: Int,
    ): PlanTrialItemEntity?

    @Query(
        """
        SELECT COUNT(*) FROM plan_trial_item
        WHERE studentId = :studentId AND status != 'DONE'
        """,
    )
    suspend fun countIncompleteForStudent(studentId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM plan_trial_item
        WHERE studentId = :studentId AND status != 'DONE' AND kind = :kind
        """,
    )
    suspend fun countIncompleteByKind(studentId: String, kind: String): Int

    @Query("DELETE FROM plan_trial_item WHERE id IN (:itemIds)")
    suspend fun deleteItemsByIds(itemIds: List<Long>)
}
