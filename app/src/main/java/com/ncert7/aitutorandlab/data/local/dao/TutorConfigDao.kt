package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.TutorConfigEntity

@Dao
interface TutorConfigDao {
    @Query("SELECT * FROM tutor_config WHERE studentId = :studentId LIMIT 1")
    suspend fun get(studentId: String): TutorConfigEntity?

    @Query("SELECT * FROM tutor_config WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TutorConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TutorConfigEntity)
}
