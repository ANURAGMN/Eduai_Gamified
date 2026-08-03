package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.FriendConnectionEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendFeedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query(
        """
        SELECT * FROM friend_connection
        WHERE studentId = :studentId AND status = 'ACCEPTED'
        ORDER BY displayName ASC
        """,
    )
    fun observeConnections(studentId: String): Flow<List<FriendConnectionEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM friend_connection
        WHERE studentId = :studentId AND status = 'ACCEPTED'
        """,
    )
    fun observeFriendCount(studentId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM friend_connection
        WHERE studentId = :studentId AND friendStudentId = :friendStudentId LIMIT 1
        """,
    )
    suspend fun getConnection(studentId: String, friendStudentId: String): FriendConnectionEntity?

    @Query(
        """
        SELECT * FROM friend_connection
        WHERE studentId = :studentId AND status = 'ACCEPTED'
        ORDER BY displayName ASC
        """,
    )
    suspend fun getConnections(studentId: String): List<FriendConnectionEntity>

    @Query(
        """
        SELECT COUNT(*) FROM friend_feed_item
        WHERE ownerStudentId = :studentId AND eventKey = :eventKey
        """,
    )
    suspend fun hasFeedItem(studentId: String, eventKey: String): Int

    @Query(
        """
        UPDATE friend_feed_item
        SET fromDisplayName = :fromDisplayName, message = :message
        WHERE ownerStudentId = :ownerStudentId AND eventKey = :eventKey
        """,
    )
    suspend fun updateFeedItemDisplay(
        ownerStudentId: String,
        eventKey: String,
        fromDisplayName: String,
        message: String,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnection(connection: FriendConnectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnections(connections: List<FriendConnectionEntity>)

    @Query(
        """
        SELECT * FROM friend_feed_item
        WHERE ownerStudentId = :studentId
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    fun observeFeed(studentId: String, limit: Int = 20): Flow<List<FriendFeedItemEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM friend_feed_item
        WHERE ownerStudentId = :studentId AND seen = 0
        """,
    )
    fun observeUnseenFeedCount(studentId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeedItem(item: FriendFeedItemEntity): Long

    @Query(
        """
        UPDATE friend_feed_item
        SET cheers = cheers + 1, cheeredByMe = 1
        WHERE id = :feedItemId AND ownerStudentId = :studentId AND cheeredByMe = 0
        """,
    )
    suspend fun cheerFeedItem(studentId: String, feedItemId: Long): Int

    @Query("UPDATE friend_feed_item SET seen = 1 WHERE ownerStudentId = :studentId AND seen = 0")
    suspend fun markAllFeedSeen(studentId: String)

    @Query(
        """
        SELECT friendStudentId FROM friend_connection
        WHERE studentId = :studentId AND status = 'ACCEPTED'
        """,
    )
    suspend fun getFriendIds(studentId: String): List<String>

    @Query("UPDATE friend_feed_item SET seen = 1 WHERE id = :feedItemId AND ownerStudentId = :studentId")
    suspend fun markFeedItemSeen(studentId: String, feedItemId: Long)
}
