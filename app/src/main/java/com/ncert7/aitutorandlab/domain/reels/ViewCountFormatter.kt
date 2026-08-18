package com.ncert7.aitutorandlab.domain.reels

/**
 * Compact view-count labels for reel tiles: `540`, `1.2K`, `12.3K`, `3.4M`, `1.5B`.
 * One decimal for K/M/B, trailing `.0` trimmed, and boundary round-ups promoted to the next unit
 * (e.g. 999,999 → `1M`, not `1000K`). Negatives clamp to 0. Pure + deterministic.
 */
object ViewCountFormatter {

    fun format(count: Long): String {
        val n = if (count < 0) 0L else count
        return when {
            n < 1_000L -> n.toString()
            n < 1_000_000L -> promote(n, 1_000.0, "K", "M")
            n < 1_000_000_000L -> promote(n, 1_000_000.0, "M", "B")
            else -> trim(round1(n / 1_000_000_000.0)) + "B"
        }
    }

    private fun promote(n: Long, div: Double, suffix: String, nextSuffix: String): String {
        val r = round1(n / div)
        return if (r >= 1_000.0) "1$nextSuffix" else trim(r) + suffix
    }

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0

    private fun trim(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
}
