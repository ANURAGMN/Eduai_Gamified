package com.ncert7.aitutorandlab.domain.garden

/** Read model for home / avatar segments (Phase 2+ UI). */
data class GardenProgress(
    val steps: Int,
    val stepsPerPlant: Int,
    val totalPlanted: Int,
    val currentZone: Int,
    val filledInZone: Int,
    val zoneCapacity: Int,
    val theme: String,
    val preferredSlot: Int,
    /** Zones the student has unlocked along their route (e.g. Meadow, then Woodland). */
    val unlockedZones: List<Int> = listOf(currentZone),
    /** Plants placed per zone index. */
    val filledCountByZone: Map<Int, Int> = emptyMap(),
)
