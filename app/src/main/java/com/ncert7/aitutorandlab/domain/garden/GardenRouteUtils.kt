package com.ncert7.aitutorandlab.domain.garden

/** Parses the comma-separated zone route stored on [com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity]. */
object GardenRouteUtils {
    /** Every place that has ever been visited (sorted, unique) — used for shelf / unlock UI. */
    fun parseUnlockedZones(route: String): List<Int> =
        route
            .split(',')
            .mapNotNull { it.trim().toIntOrNull()?.coerceAtLeast(0) }
            .distinct()
            .sorted()

    /**
     * The place the learner is looking at now = the **last** entry in the journey string
     * (order-preserving). Not the max index — so after finishing zones 1…7 we can still
     * focus zone 0 (or any earlier free place) by appending it: `"1,2,3,4,5,6,7,0"`.
     */
    fun currentZone(route: String): Int =
        route
            .split(',')
            .mapNotNull { it.trim().toIntOrNull()?.coerceAtLeast(0) }
            .lastOrNull()
            ?: 0
}
