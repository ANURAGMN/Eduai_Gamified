package com.anurag.eduai.uikit.garden

import com.anurag.eduai.uikit.garden.quest.Theme

/** One collected plant/module/tile shown on the home or scene shelf. */
data class CollectionShelfItem(
    val zone: Int,
    val slot: Int,
    val label: String = "",
)

/** Scrollable strip of finished work — garden, space, island, or colony. */
data class CollectionShelfState(
    val theme: Theme,
    val items: List<CollectionShelfItem>,
    val totalCount: Int,
    val emptyMessage: String,
    val sectionTitle: String = "",
    val seeAllLabel: String = "Open",
    /** Hint for how many items fit without scrolling on a typical phone. */
    val visibleCapacity: Int = 5,
    /** Empty placeholder slots shown after collected items. */
    val lockedSlotCount: Int = 3,
    /** Shown on the first empty slot — e.g. complete tasks to unlock more. */
    val lockedSlotHint: String = "",
    /**
     * Labels for empty “up next” slots (colony modules, island landmarks).
     * Index 0 is the next build after the last collected item.
     */
    val upcomingLabels: List<String> = emptyList(),
)
