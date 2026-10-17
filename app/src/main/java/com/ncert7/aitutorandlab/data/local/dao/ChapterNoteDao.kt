package com.ncert7.aitutorandlab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ncert7.aitutorandlab.data.local.entities.ChapterNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterNoteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitial(note: ChapterNoteEntity): Long

    @Query(
        """
        UPDATE chapter_notes 
        SET subjectId = :subjectId,
            chapterNumber = :chapterNumber,
            conceptName = :conceptName,
            remotePdfUrl = :remotePdfUrl,
            remotePdfUrlKn = :remotePdfUrlKn
        WHERE chapterId = :chapterId
        """
    )
    suspend fun updateRemoteMetadata(
        chapterId: String,
        subjectId: String,
        chapterNumber: Int,
        conceptName: String,
        remotePdfUrl: String,
        remotePdfUrlKn: String
    )

    @Transaction
    suspend fun upsertRemoteMetadata(
        chapterId: String,
        subjectId: String,
        chapterNumber: Int,
        conceptName: String,
        remotePdfUrl: String,
        remotePdfUrlKn: String
    ) {
        val insertResult = insertInitial(
            ChapterNoteEntity(
                chapterId = chapterId,
                subjectId = subjectId,
                chapterNumber = chapterNumber,
                conceptName = conceptName,
                remotePdfUrl = remotePdfUrl,
                remotePdfUrlKn = remotePdfUrlKn,
                localFilePath = null,
                downloadProgress = 0,
                isDownloaded = false
            )
        )
        if (insertResult == -1L) {
            updateRemoteMetadata(
                chapterId = chapterId,
                subjectId = subjectId,
                chapterNumber = chapterNumber,
                conceptName = conceptName,
                remotePdfUrl = remotePdfUrl,
                remotePdfUrlKn = remotePdfUrlKn
            )
        }
    }

    @Query("SELECT * FROM chapter_notes WHERE LOWER(subjectId) = LOWER(:subjectId) ORDER BY chapterNumber ASC")
    fun getNotesBySubject(subjectId: String): Flow<List<ChapterNoteEntity>>

    @Query("SELECT * FROM chapter_notes WHERE chapterId = :chapterId")
    fun getNoteState(chapterId: String): Flow<ChapterNoteEntity?>

    @Query("UPDATE chapter_notes SET downloadProgress = :progress WHERE chapterId = :chapterId")
    suspend fun updateProgress(chapterId: String, progress: Int)

    @Query("UPDATE chapter_notes SET localFilePath = :filePath, downloadProgress = 100, isDownloaded = 1 WHERE chapterId = :chapterId")
    suspend fun markDownloaded(chapterId: String, filePath: String)

    @Query("UPDATE chapter_notes SET localFilePath = NULL, downloadProgress = 0, isDownloaded = 0 WHERE chapterId = :chapterId")
    suspend fun clearDownload(chapterId: String)
}