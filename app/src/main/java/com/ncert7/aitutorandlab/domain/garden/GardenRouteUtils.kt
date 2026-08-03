package com.ncert7.aitutorandlab.domain.garden

/** Parses the comma-separated zone route stored on [com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity]. */
object GardenRouteUtils {
    fun parseUnlockedZones(route: String): List<Int> =
        route
            .split(',')
            .mapNotNull { it.trim().toIntOrNull()?.coerceAtLeast(0) }
            .distinct()
            .sorted()

    fun currentZone(route: String): Int = parseUnlockedZones(route).lastOrNull() ?: 0
}
