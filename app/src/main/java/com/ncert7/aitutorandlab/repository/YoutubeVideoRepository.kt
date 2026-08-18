package com.ncert7.aitutorandlab.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeVideoRepository @Inject constructor() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun fetchVideos(): List<YoutubeVideo> {
        return try {
            val snapshot =
                firestore
                    .collection(COLLECTION)
                    .whereEqualTo(FIELD_ACTIVE, true)
                    .orderBy(FIELD_PUBLISHED_AT, Query.Direction.DESCENDING)
                    .get()
                    .await()
            val remote =
                snapshot.documents.mapNotNull { doc ->
                    val videoId = doc.getString(FIELD_VIDEO_ID)?.trim().orEmpty()
                    if (videoId.isBlank()) return@mapNotNull null
                    YoutubeVideo(
                        videoId = videoId,
                        title = doc.getString(FIELD_TITLE)?.trim().orEmpty().ifBlank { "EduAI video" },
                        titleKannada = doc.getString(FIELD_TITLE_KN)?.trim().orEmpty(),
                        publishedAtMillis = doc.getLong(FIELD_PUBLISHED_AT) ?: 0L,
                        viewCount = doc.getLong(FIELD_VIEW_COUNT) ?: 0L,
                        caption = doc.getString(FIELD_CAPTION)?.trim().orEmpty(),
                        // Missing field = not verified kids-safe → false (defensive; reels filters on this).
                        madeForKids = doc.getBoolean(FIELD_MADE_FOR_KIDS) ?: false,
                    )
                }
            if (remote.isNotEmpty()) remote else defaultVideos()
        } catch (e: Exception) {
            DebugLogger.debugLog(TAG, "YouTube catalog fetch failed, using defaults: ${e.message}")
            defaultVideos()
        }
    }

    fun localizedTitle(video: YoutubeVideo, languageCode: String): String {
        val lang = normalizeLanguageCode(languageCode)
        return if (lang == "kn") {
            video.titleKannada.ifBlank { video.title }
        } else {
            video.title
        }
    }

    fun thumbnailUrl(videoId: String): String =
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    private fun defaultVideos(): List<YoutubeVideo> =
        listOf(
            YoutubeVideo(
                videoId = "QmEw0LO417E",
                title = "EduAI learning video",
                titleKannada = "EduAI ಕಲಿಕೆ ವೀಡಿಯೋ",
                publishedAtMillis = 2L,
                madeForKids = true,
            ),
            YoutubeVideo(
                videoId = "qTs6e_XmYNo",
                title = "EduAI learning video",
                titleKannada = "EduAI ಕಲಿಕೆ ವೀಡಿಯೋ",
                publishedAtMillis = 1L,
                madeForKids = true,
            ),
        )

    companion object {
        private const val TAG = "YoutubeVideoRepository"
        const val COLLECTION = "youtube_videos"
        const val FIELD_VIDEO_ID = "videoId"
        const val FIELD_TITLE = "title"
        const val FIELD_TITLE_KN = "titleKannada"
        const val FIELD_PUBLISHED_AT = "publishedAtMillis"
        const val FIELD_ACTIVE = "active"
        const val FIELD_VIEW_COUNT = "viewCount"
        const val FIELD_CAPTION = "caption"
        const val FIELD_MADE_FOR_KIDS = "madeForKids"
    }
}
