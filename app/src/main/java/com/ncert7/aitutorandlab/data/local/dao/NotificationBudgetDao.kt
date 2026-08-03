package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.NotificationBudgetEntity

@Dao
interface NotificationBudgetDao {
    @Query(
        """
        SELECT * FROM notification_budget
        WHERE studentId = :studentId AND budgetEpochDay = :budgetEpochDay
        LIMIT 1
        """,
    )
    suspend fun getBudget(studentId: String, budgetEpochDay: Long): NotificationBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationBudgetEntity)
}
