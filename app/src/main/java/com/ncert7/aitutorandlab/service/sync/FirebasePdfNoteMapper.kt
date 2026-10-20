package com.ncert7.aitutorandlab.service.sync

import com.google.firebase.firestore.DocumentSnapshot

data class RemotePdfNote(
    val chapterId: String,
    val subjectId: String,
    val chapterNumber: Int,
    val conceptName: String,
    val pdfUrl: String,
    val pdfUrlKn: String
)

object FirebasePdfNoteMapper {
    fun map(document: DocumentSnapshot): RemotePdfNote {
        val chapterId = document.getString("chapter_id") ?: document.id
        val subjectId = document.getString("subject_id").orEmpty()
        val chapterNumber = when (val num = document.get("chapter_number")) {
            is Number -> num.toInt()
            is String -> num.toIntOrNull() ?: 0
            else -> 0
        }
        val conceptName = document.getString("concept_name").orEmpty()
        val pdfUrl = document.getString("pdf_url").orEmpty()
        val pdfUrlKn = document.getString("pdf_url_kn").orEmpty()

        return RemotePdfNote(
            chapterId = chapterId,
            subjectId = subjectId,
            chapterNumber = chapterNumber,
            conceptName = conceptName,
            pdfUrl = pdfUrl,
            pdfUrlKn = pdfUrlKn
        )
    }
}