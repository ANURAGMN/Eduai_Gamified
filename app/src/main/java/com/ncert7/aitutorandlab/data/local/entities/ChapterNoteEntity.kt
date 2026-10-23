package com.ncert7.aitutorandlab.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapter_notes")
data class ChapterNoteEntity(
    @PrimaryKey
    val chapterId: String,
    val subjectId: String? = null,
    val chapterNumber: Int = 0,
    val conceptName: String? = null,
    val remotePdfUrl: String? = null,
    val remotePdfUrlKn: String? = null,
    val localFilePath: String? = null,
    val downloadProgress: Int = 0,
    val isDownloaded: Boolean = false
)