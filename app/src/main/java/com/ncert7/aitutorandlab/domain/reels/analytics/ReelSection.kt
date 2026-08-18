package com.ncert7.aitutorandlab.domain.reels.analytics

/**
 * Where a reel was surfaced when the user opened it. Stored as [value] in the analytics
 * `source` column so we can tell discovery paths apart (newest rail vs most-watched vs search).
 */
enum class ReelSection(val value: String) {
    NEWEST("newest"),
    MOST_WATCHED("most_watched"),
    SEARCH("search"),
}
