package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.LeagueCacheEntity
import com.ncert7.aitutorandlab.data.local.entities.LeagueMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeagueDao {

    @Query(
        """
        SELECT * FROM league_member
        WHERE weekKey = :weekKey AND cohortId = :cohortId
        ORDER BY weeklyXp DESC, displayName ASC
        """,
    )
    fun observeMembers(weekKey: String, cohortId: String): Flow<List<LeagueMemberEntity>>

    @Query(
        """
        SELECT * FROM league_member
        WHERE weekKey = :weekKey AND cohortId = :cohortId
        ORDER BY weeklyXp DESC, displayName ASC
        """,
    )
    suspend fun getMembers(weekKey: String, cohortId: String): List<LeagueMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<LeagueMemberEntity>)

    @Query("DELETE FROM league_member WHERE weekKey = :weekKey AND cohortId = :cohortId")
    suspend fun deleteCohort(weekKey: String, cohortId: String)

    @Query(
        """
        UPDATE league_member
        SET weeklyXp = :weeklyXp,
            streak = :streak,
            displayName = :displayName,
            updatedAt = :updatedAt
        WHERE weekKey = :weekKey AND cohortId = :cohortId AND memberId = :memberId
        """,
    )
    suspend fun updateMemberXp(
        weekKey: String,
        cohortId: String,
        memberId: String,
        weeklyXp: Int,
        streak: Int,
        displayName: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM league_cache WHERE studentId = :studentId LIMIT 1")
    fun observeCache(studentId: String): Flow<LeagueCacheEntity?>

    @Query("SELECT * FROM league_cache WHERE studentId = :studentId LIMIT 1")
    suspend fun getCache(studentId: String): LeagueCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCache(cache: LeagueCacheEntity)
}
