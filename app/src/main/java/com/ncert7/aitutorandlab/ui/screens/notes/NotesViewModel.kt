package com.ncert7.aitutorandlab.ui.screens.notes

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.dao.ChapterNoteDao
import com.ncert7.aitutorandlab.data.local.entities.ChapterNoteEntity
import com.ncert7.aitutorandlab.domain.pdf.PdfDownloadManager
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class StaticNoteItem(
    val id: String,
    val chapterNum: Int,
    val title: String,
    val urlResId: Int
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chapterNoteDao: ChapterNoteDao,
    private val downloadManager: PdfDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val subjectPassed: String = savedStateHandle.get<String>("subjectId") ?: ""
    val isKannada = getCurrentLanguageCode() == "kn"

    // Check for English "math" OR Kannada "ಗಣಿತ" safely
    private val isMath = subjectPassed.contains("math", ignoreCase = true) ||
            subjectPassed.contains("ಗಣಿತ", ignoreCase = true)

    // Pulls the exact translated string you added to strings.xml
    val screenTitle: String = context.getString(
        if (isMath) R.string.math_notes else R.string.science_notes
    )

    private val _notes = MutableStateFlow<List<StaticNoteItem>>(emptyList())
    val notes: StateFlow<List<StaticNoteItem>> = _notes.asStateFlow()

    init {
        if (isMath) {
            _notes.value = listOf(
                StaticNoteItem("math_1", 1, "Large Numbers Around Us", R.string.math_ch1_large_numbers_around_us_url),
                StaticNoteItem("math_2", 2, "Arithmetic Expressions", R.string.math_ch2_arithmetic_expressions_url),
                StaticNoteItem("math_3", 3, "A Peek Beyond the Point", R.string.math_ch3_a_peek_beyond_the_point_url)
            )
        } else {
            _notes.value = listOf(
                StaticNoteItem("sci_2", 2, "Acidic, Basic and Neutral", R.string.science_ch2_acid_basic_neutral_url),
                StaticNoteItem("sci_9", 9, "Respiratory System", R.string.science_ch9_respiratory_system_url),
                StaticNoteItem("sci_10", 10, "Photosynthesis", R.string.science_ch10_photosynthesis_url)
            )
        }
    }

    fun getNoteState(id: String) = chapterNoteDao.getNoteState(id)

    fun startDownload(item: StaticNoteItem) {
        if (isKannada) return

        val url = context.getString(item.urlResId)
        val noteId = item.id

        viewModelScope.launch {
            chapterNoteDao.insertOrUpdate(ChapterNoteEntity(chapterId = noteId, downloadProgress = 1))

            val localPath = downloadManager.downloadPdf(url, noteId) { progress ->
                chapterNoteDao.updateProgress(noteId, progress)
            }

            if (localPath != null) {
                chapterNoteDao.insertOrUpdate(
                    ChapterNoteEntity(chapterId = noteId, localFilePath = localPath, downloadProgress = 100, isDownloaded = true)
                )
            } else {
                chapterNoteDao.insertOrUpdate(ChapterNoteEntity(chapterId = noteId, downloadProgress = 0, isDownloaded = false))
            }
        }
    }

    fun deleteDownload(item: StaticNoteItem) {
        viewModelScope.launch {
            val file = File(context.filesDir, "${item.id}.pdf")
            if (file.exists()) {
                file.delete()
            }
            chapterNoteDao.insertOrUpdate(ChapterNoteEntity(chapterId = item.id, downloadProgress = 0, isDownloaded = false, localFilePath = null))
        }
    }
}