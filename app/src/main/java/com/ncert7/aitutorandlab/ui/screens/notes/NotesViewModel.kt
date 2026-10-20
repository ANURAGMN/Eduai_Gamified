package com.ncert7.aitutorandlab.ui.screens.notes

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.dao.ChapterNoteDao
import com.ncert7.aitutorandlab.data.local.entities.ChapterNoteEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.pdf.PdfDownloadManager
import com.ncert7.aitutorandlab.service.sync.FirebaseSyncManager
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chapterNoteDao: ChapterNoteDao,
    private val downloadManager: PdfDownloadManager,
    private val syncManager: FirebaseSyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 1. Fetch exactly what was passed in the Navigation Graph
    private val subjectPassed: String = savedStateHandle.get<String>("subjectId")
        ?: savedStateHandle.get<String>("subjectName")
        ?: ""

    val isKannada = getCurrentLanguageCode() == "kn"

    // 2. Static, rock-solid evaluation (Your original correct logic)
    val isMath = subjectPassed.contains("math", ignoreCase = true) ||
            subjectPassed.contains("ಗಣಿತ", ignoreCase = true)

    private val normalizedSubject = if (isMath) "math" else "science"

    // 3. Title locks in immediately and never changes
    val screenTitle: String = context.getString(
        if (isMath) R.string.math_notes else R.string.science_notes
    )

    // 4. Query exactly that subject and apply a strict memory filter just in case
    val notes: StateFlow<List<ChapterNoteEntity>> = chapterNoteDao
        .getNotesBySubject(normalizedSubject)
        .map { list -> list.filter { it.subjectId.equals(normalizedSubject, ignoreCase = true) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val result = syncManager.syncPdfNotes()
            DebugLogger.debugLog("NotesViewModel", "PDF Sync: ${result.success} - ${result.message}")
        }
    }

    fun startDownload(note: ChapterNoteEntity) {
        val rawUrl = if (isKannada) note.remotePdfUrlKn else note.remotePdfUrl
        if (rawUrl.isNullOrBlank()) return

        // Clean encoded links so download manager doesn't crash
        val targetUrl = rawUrl.replace("&amp;", "&").replace("\"", "").trim()

        viewModelScope.launch {
            chapterNoteDao.updateProgress(note.chapterId, 1)

            val localPath = downloadManager.downloadPdf(targetUrl, note.chapterId) { progress ->
                viewModelScope.launch {
                    chapterNoteDao.updateProgress(note.chapterId, progress)
                }
            }

            if (localPath != null) {
                chapterNoteDao.markDownloaded(note.chapterId, localPath)
            } else {
                chapterNoteDao.updateProgress(note.chapterId, 0)
            }
        }
    }

    fun deleteDownload(note: ChapterNoteEntity) {
        viewModelScope.launch {
            note.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            val defaultFile = File(context.filesDir, "${note.chapterId}.pdf")
            if (defaultFile.exists()) defaultFile.delete()

            chapterNoteDao.clearDownload(note.chapterId)
        }
    }
}