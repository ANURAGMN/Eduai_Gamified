package com.ncert7.aitutorandlab.domain.youtube

/** Curated YouTube lesson linked from Firestore `youtube_videos`. */
data class YoutubeVideo(
    val videoId: String,
    val title: String,
    val titleKannada: String = "",
    val publishedAtMillis: Long = 0L,
)
