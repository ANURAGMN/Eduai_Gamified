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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** Nav arg — prefer stable subject id / key, never scrape localized display names. */
    private val subjectArg: String =
        savedStateHandle.get<String>("subjectId")
            ?: savedStateHandle.get<String>("subjectName")
            ?: ""

    /** Normalized key matching Firestore `PdfNotes.subject_id` (math / science). */
    val notesSubjectKey: String = normalizeSubjectKey(subjectArg)

    val isKannada = getCurrentLanguageCode() == "kn"

    val isMath: Boolean = notesSubjectKey == "math"

    val screenTitle: String =
        context.getString(if (isMath) R.string.math_notes else R.string.science_notes)

    val notes: StateFlow<List<ChapterNoteEntity>> =
        chapterNoteDao
            .getNotesBySubject(notesSubjectKey)
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

        val targetUrl = rawUrl.replace("&amp;", "&").replace("\"", "").trim()

        viewModelScope.launch {
            chapterNoteDao.updateProgress(note.chapterId, 1)

            val localPath =
                downloadManager.downloadPdf(targetUrl, note.chapterId) { progress ->
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
            val defaultFile = File(File(context.filesDir, "notes"), "${note.chapterId}.pdf")
            if (defaultFile.exists()) defaultFile.delete()

            chapterNoteDao.clearDownload(note.chapterId)
        }
    }

    companion object {
        /**
         * Map nav subject ids to Firestore PdfNotes `subject_id` values.
         * Accepts explicit keys, known subject UUIDs, or English/Kannada *id-like* tokens —
         * not free-form localized chapter titles.
         */
        private const val MATH_SUBJECT_UUID = "5c0a6b6d-7c6b-4f35-9d5b-9fd0fd8e8a01"
        private const val SCIENCE_SUBJECT_UUID = "9a7d0d20-7b8d-4b8c-8c12-5a1a8a55f002"

        fun normalizeSubjectKey(raw: String): String {
            val r = raw.trim().lowercase()
            if (r.isEmpty()) return "science"
            if (r == "math" || r == "mathematics" || r == MATH_SUBJECT_UUID) return "math"
            if (r == "science" || r == SCIENCE_SUBJECT_UUID) return "science"
            // Unknown id: keep as-is so it can still match Firestore subject_id.
            return r
        }
    }
}
