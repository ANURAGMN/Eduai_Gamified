package com.ncert7.aitutorandlab.domain.youtube

/** Curated YouTube lesson linked from Firestore `youtube_videos`. */
data class YoutubeVideo(
    val videoId: String,
    val title: String,
    val titleKannada: String = "",
    val publishedAtMillis: Long = 0L,
    /** YouTube total views (Data API statistics.viewCount), for the "most watched" section + tiles. */
    val viewCount: Long = 0L,
    /** Caption/description, indexed by search. */
    val caption: String = "",
    /** Data API status.madeForKids — only kids-safe videos may surface (defensive gate). */
    val madeForKids: Boolean = false,
)
