package com.ncert7.aitutorandlab.domain.garden

import com.anurag.eduai.uikit.garden.quest.SLOTS_PER_ZONE
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.anurag.eduai.uikit.garden.world.hash
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity

object GardenSlotResolver {
    /** Slot shown on the home rail and in celebrations — student's pick, or seeded preview. */
    fun displaySlot(progress: GardenProgress): Int {
        if (progress.preferredSlot in 0 until SLOTS_PER_ZONE) return progress.preferredSlot
        return (hash(progress.totalPlanted, 11) * SLOTS_PER_ZONE).toInt().coerceIn(0, SLOTS_PER_ZONE - 1)
    }

    /** Illustration slot after a plant — matches what the student picked when set. */
    fun celebrationSlot(
        progress: GardenProgress,
        planted: GrownItemEntity,
    ): Int {
        if (progress.preferredSlot in 0 until SLOTS_PER_ZONE) return progress.preferredSlot
        return planted.slot.coerceIn(0, SLOTS_PER_ZONE - 1)
    }

    fun remainingScenes(currentZone: Int): Int =
        (ZONES.size - currentZone - 1).coerceAtLeast(0)
}
