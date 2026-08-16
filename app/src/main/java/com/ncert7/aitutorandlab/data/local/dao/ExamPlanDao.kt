package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamPlanDao {

    @Query("SELECT * FROM exam_plan WHERE studentId = :studentId AND isActive = 1 LIMIT 1")
    fun observeActivePlan(studentId: String): Flow<ExamPlanEntity?>

    @Query("SELECT * FROM exam_plan WHERE studentId = :studentId AND isActive = 1 LIMIT 1")
    suspend fun getActivePlan(studentId: String): ExamPlanEntity?

    @Query("SELECT * FROM exam_plan_day WHERE studentId = :studentId ORDER BY dayIndex ASC")
    fun observePlanDays(studentId: String): Flow<List<ExamPlanDayEntity>>

    @Query("SELECT * FROM exam_plan_day WHERE studentId = :studentId ORDER BY dayIndex ASC")
    suspend fun getPlanDays(studentId: String): List<ExamPlanDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: ExamPlanEntity)

    @Query("SELECT * FROM exam_plan WHERE isSynced = 0 AND isActive = 1")
    suspend fun getUnsyncedActivePlans(): List<ExamPlanEntity>

    @Query(
        """
        UPDATE exam_plan
        SET isSynced = 1
        WHERE studentId = :studentId
        """,
    )
    suspend fun markPlanSynced(studentId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDays(days: List<ExamPlanDayEntity>)

    @Query("DELETE FROM exam_plan_day WHERE studentId = :studentId")
    suspend fun deletePlanDays(studentId: String)

    @Query("DELETE FROM exam_plan WHERE studentId = :studentId")
    suspend fun deletePlan(studentId: String)

    @Query(
        """
        UPDATE exam_plan_day
        SET status = :status
        WHERE studentId = :studentId AND dayIndex = :dayIndex
        """,
    )
    suspend fun updateDayStatus(studentId: String, dayIndex: Int, status: String)
}
