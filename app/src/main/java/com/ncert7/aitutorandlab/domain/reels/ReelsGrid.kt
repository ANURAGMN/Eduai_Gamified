package com.ncert7.aitutorandlab.domain.reels

import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo

/** The two labelled sections of the reels explore grid (3 columns × 4 rows = up to 12 tiles). */
data class ReelsGrid(
    val newest: List<YoutubeVideo>,
    val mostWatched: List<YoutubeVideo>,
) {
    val all: List<YoutubeVideo> get() = newest + mostWatched
    val isEmpty: Boolean get() = newest.isEmpty() && mostWatched.isEmpty()
}

/**
 * Pure selection for the reels grid, deduped:
 *  - rows 1–2 = top [sectionSize] newest (by publishedAtMillis desc),
 *  - rows 3–4 = top [sectionSize] most-watched (by viewCount desc) **excluding** the newest set.
 *
 * When [requireMadeForKids] is true, only [YoutubeVideo.madeForKids] items are considered (release /
 * Families path). Default is **false** while we validate UI looks against the live catalog.
 * Each video appears at most once across both sections. Ordering is deterministic (id tie-breaker).
 */
object ReelsGridSelector {
    const val SECTION_SIZE = 6

    /** TEMP default false for UI testing — flip to true before Play Families release. */
    const val REQUIRE_MADE_FOR_KIDS_DEFAULT = false

    fun select(
        videos: List<YoutubeVideo>,
        sectionSize: Int = SECTION_SIZE,
        requireMadeForKids: Boolean = REQUIRE_MADE_FOR_KIDS_DEFAULT,
    ): ReelsGrid {
        if (sectionSize <= 0) return ReelsGrid(emptyList(), emptyList())

        val safe = videos
            .filter { it.videoId.isNotBlank() && (!requireMadeForKids || it.madeForKids) }
            .distinctBy { it.videoId }

        val newest = safe
            .sortedWith(
                compareByDescending<YoutubeVideo> { it.publishedAtMillis }
                    .thenBy { it.videoId },
            )
            .take(sectionSize)

        val newestIds = newest.mapTo(HashSet()) { it.videoId }

        val mostWatched = safe
            .filter { it.videoId !in newestIds }
            .sortedWith(
                compareByDescending<YoutubeVideo> { it.viewCount }
                    .thenByDescending { it.publishedAtMillis }
                    .thenBy { it.videoId },
            )
            .take(sectionSize)

        return ReelsGrid(newest = newest, mostWatched = mostWatched)
    }
}
