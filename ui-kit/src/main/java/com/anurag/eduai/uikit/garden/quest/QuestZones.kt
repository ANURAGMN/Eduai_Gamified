package com.anurag.eduai.uikit.garden.quest

import com.anurag.eduai.uikit.garden.world.Habitat
import com.anurag.eduai.uikit.garden.world.Habitats
import com.anurag.eduai.uikit.garden.world.ModuleKind
import com.anurag.eduai.uikit.garden.world.SpeciesTable
import com.anurag.eduai.uikit.garden.world.WORLDS
import com.anurag.eduai.uikit.garden.world.World

/**
 * Four ways to watch the same work pile up. They divide on *shape*, not just skin:
 *
 * - [GARDEN] and [OUTPOST] are **place** themes — eight places of twelve plots, and before each
 *   task you pick one of six things to grow. Identical underneath, so switching between them is
 *   lossless: the slot index is what gets stored, never the species.
 * - [ISLAND] and [COLONY] are **single-scene** themes — one scene that keeps growing, a tile or a
 *   tier at a time. There is nothing to pick, which makes them calmer and shorter.
 *
 * Every one of the four is driven by the same single number: tasks finished. Switching to a
 * single-scene theme does not read the stored slot — but it does not erase it either, so coming
 * back to the garden finds all forty plants exactly where they were.
 */
enum class Theme { GARDEN, OUTPOST, ISLAND, COLONY }

/** True when the theme has places to travel between and six things to choose from. */
val Theme.placeBased: Boolean
    get() = this == Theme.GARDEN || this == Theme.OUTPOST

const val SLOTS_PER_ZONE = 6
const val ZONE_CAPACITY = 12
const val STEPS_PER_TASK = 7
/** Locked-in pick: surprise — random slot from the six options. */
const val PREFERRED_SLOT_SURPRISE = -1
/** Woodland slot index for cherry blossom — free first-time pick for garden students. */
const val STARTER_PLANT_SLOT = 2
/** Outpost slot index for solar array — free first-time pick for space students. */
const val STARTER_MODULE_SLOT = 1
/** Woodland — default starting place for the garden theme. */
const val STARTER_GARDEN_ZONE = 1
/** Mars — default starting site for the space / outpost theme. */
const val STARTER_OUTPOST_ZONE = 1

/** Highlighted free starter in the picker — not the locked preferred slot (surprise is default). */
fun Theme.starterSlot(): Int = if (this == Theme.OUTPOST) STARTER_MODULE_SLOT else STARTER_PLANT_SLOT

/** First zone/site unlocked for new students in this theme. */
fun Theme.starterZone(): Int = if (this == Theme.OUTPOST) STARTER_OUTPOST_ZONE else STARTER_GARDEN_ZONE

/**
 * A zone is one place. The garden habitat and the outpost world at the same index are
 * the *same* zone wearing different clothes — six slots either side, so the theme can
 * be swapped without losing anything. What we store is the slot index, never the species.
 */
class Zone(val index: Int) {
    val habitat: Habitat get() = Habitats.all[index]
    val world: World get() = WORLDS[index]

    fun name(theme: Theme): String =
        if (theme == Theme.GARDEN) habitat.name else world.name

    fun slotName(theme: Theme, slot: Int): String =
        if (theme == Theme.GARDEN) SpeciesTable[habitat.species[slot]].name
        else world.moduleNames[slot]

    fun speciesKey(slot: Int): String = habitat.species[slot]

    fun moduleKind(slot: Int): ModuleKind = ModuleKind.entries[slot]

    /** Three names to hint what grows here, used on the "where next" cards. */
    fun teaser(theme: Theme): String =
        (0 until 3).joinToString(", ") { slotName(theme, it).lowercase() }
}

val ZONES: List<Zone> = List(Habitats.all.size) { Zone(it) }
