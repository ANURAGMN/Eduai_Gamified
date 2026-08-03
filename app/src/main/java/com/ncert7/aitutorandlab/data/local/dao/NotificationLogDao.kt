package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.NotificationLogEntity

@Dao
interface NotificationLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationLogEntity): Long

    @Query(
        """
        SELECT COUNT(*) > 0 FROM notification_log
        WHERE studentId = :studentId
          AND type = :type
          AND shownEpochDay = :shownEpochDay
          AND dedupKey = :dedupKey
        """,
    )
    suspend fun wasShown(
        studentId: String,
        type: String,
        shownEpochDay: Long,
        dedupKey: String,
    ): Boolean

    @Query(
        """
        SELECT COUNT(*) > 0 FROM notification_log
        WHERE studentId = :studentId
          AND type = :type
          AND dedupKey = :dedupKey
        """,
    )
    suspend fun wasShownEver(
        studentId: String,
        type: String,
        dedupKey: String,
    ): Boolean
}
