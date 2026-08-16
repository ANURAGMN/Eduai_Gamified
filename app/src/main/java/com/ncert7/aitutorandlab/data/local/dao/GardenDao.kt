package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity

@Dao
interface GardenDao {

    @Query("SELECT * FROM garden_state WHERE studentId = :studentId LIMIT 1")
    suspend fun getState(studentId: String): GardenStateEntity?

    @Query("SELECT * FROM garden_state WHERE studentId = :studentId LIMIT 1")
    fun observeState(studentId: String): kotlinx.coroutines.flow.Flow<GardenStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: GardenStateEntity)

    @Query("UPDATE garden_state SET steps = :steps WHERE studentId = :studentId")
    suspend fun updateSteps(studentId: String, steps: Int)

    @Query("UPDATE garden_state SET preferredSlot = :slot WHERE studentId = :studentId")
    suspend fun updatePreferredSlot(studentId: String, slot: Int)

    @Query("UPDATE garden_state SET route = :route WHERE studentId = :studentId")
    suspend fun updateRoute(studentId: String, route: String)

    @Query("UPDATE garden_state SET theme = :theme WHERE studentId = :studentId")
    suspend fun updateTheme(studentId: String, theme: String)

    @Query("SELECT * FROM garden_item WHERE studentId = :studentId ORDER BY completedAt ASC")
    suspend fun getAllItems(studentId: String): List<GrownItemEntity>

    @Query("SELECT * FROM garden_item WHERE id = :id LIMIT 1")
    suspend fun getItem(id: String): GrownItemEntity?

    @Query(
        "SELECT * FROM garden_item WHERE studentId = :studentId AND zone = :zone ORDER BY plot ASC",
    )
    suspend fun getItemsInZone(studentId: String, zone: Int): List<GrownItemEntity>

    @Query("SELECT COUNT(*) FROM garden_item WHERE studentId = :studentId")
    suspend fun countItems(studentId: String): Int

    @Query(
        "SELECT * FROM garden_item WHERE studentId = :studentId ORDER BY completedAt DESC LIMIT 1",
    )
    suspend fun getLatestItem(studentId: String): GrownItemEntity?

    /** Most recent plant for a specific task (concept + activity bucket) — used to collapse the
     * duplicate completion calls that belong to one completion, while still allowing later re-dos. */
    @Query(
        "SELECT * FROM garden_item WHERE studentId = :studentId AND conceptId = :conceptId AND kind = :kind ORDER BY completedAt DESC LIMIT 1",
    )
    suspend fun getLatestItemForTask(studentId: String, conceptId: String, kind: String): GrownItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: GrownItemEntity)
}
