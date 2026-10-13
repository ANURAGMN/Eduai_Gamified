package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ncert7.aitutorandlab.data.local.entities.ChapterNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(note: ChapterNoteEntity)

    @Query("SELECT * FROM chapter_notes WHERE chapterId = :chapterId")
    fun getNoteState(chapterId: String): Flow<ChapterNoteEntity?>

    @Query("UPDATE chapter_notes SET downloadProgress = :progress WHERE chapterId = :chapterId")
    suspend fun updateProgress(chapterId: String, progress: Int)
}