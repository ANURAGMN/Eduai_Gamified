package com.ncert7.aitutorandlab.domain.reels

import com.ncert7.aitutorandlab.domain.youtube.YoutubeVideo

/**
 * Type-2 search over the reels list: case-insensitive **substring** match plus **typo tolerance**
 * (Levenshtein) — so `fotosynthesis` still finds "Photosynthesis". Searches title, Kannada title,
 * and caption. Multi-word queries are AND-ed (every term must match); results are ranked by score
 * (exact substring beats fuzzy), then newest first. Pure + deterministic for unit testing.
 */
object ReelsSearch {

    private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE = Regex("\\s+")

    fun filter(videos: List<YoutubeVideo>, query: String): List<YoutubeVideo> {
        val terms = query.trim().lowercase().split(WHITESPACE).filter { it.isNotBlank() }
        if (terms.isEmpty()) return videos

        return videos
            .mapNotNull { video ->
                val haystack = buildString {
                    append(video.title.lowercase()).append(' ')
                    append(video.titleKannada.lowercase()).append(' ')
                    append(video.caption.lowercase())
                }
                val words = haystack.split(SEPARATORS).filter { it.isNotBlank() }
                var total = 0
                for (term in terms) {
                    val score = termScore(term, haystack, words)
                    if (score <= 0) return@mapNotNull null // AND: any unmatched term drops the video
                    total += score
                }
                video to total
            }
            .sortedWith(
                compareByDescending<Pair<YoutubeVideo, Int>> { it.second }
                    .thenByDescending { it.first.publishedAtMillis }
                    .thenBy { it.first.videoId },
            )
            .map { it.first }
    }

    private fun termScore(term: String, haystack: String, words: List<String>): Int {
        if (haystack.contains(term)) return 100 // exact substring / prefix
        val threshold = when {
            term.length <= 4 -> 1
            term.length <= 7 -> 2
            else -> 3
        }
        var best = Int.MAX_VALUE
        for (word in words) {
            val d = levenshtein(term, word)
            if (d < best) best = d
            if (best == 0) break
        }
        return if (best <= threshold) (100 - best * 15).coerceAtLeast(1) else 0
    }

    /** Classic two-row Levenshtein edit distance. */
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[b.length]
    }
}
