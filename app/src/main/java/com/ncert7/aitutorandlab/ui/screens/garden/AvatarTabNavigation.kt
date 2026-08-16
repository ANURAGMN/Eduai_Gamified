package com.ncert7.aitutorandlab.ui.screens.garden

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Avatar tab segments — Scene first (garden), then Journey, then Look (tutor). */
enum class AvatarGardenSegment(val index: Int) {
    Scene(0),
    Journey(1),
    Look(2),
    ;

    companion object {
        fun fromIndex(index: Int): AvatarGardenSegment =
            entries.firstOrNull { it.index == index } ?: Scene
    }
}

/** One-shot handoff when home rail "Open" should land on Scene, not Look. */
object AvatarTabNavigation {
    @Volatile
    var pendingSegment: AvatarGardenSegment? = null

    fun consumePendingSegment(): AvatarGardenSegment? {
        val segment = pendingSegment
        pendingSegment = null
        return segment
    }

    fun openScene() {
        pendingSegment = AvatarGardenSegment.Scene
    }

    // Live segment override so the first-run walkthrough can step the Avatar tab through
    // Scene → Journey → Look while its spotlight cards advance. Non-null forces the segment;
    // null releases control back to the user. Cleared when the tour ends.
    private val _forcedSegment = MutableStateFlow<AvatarGardenSegment?>(null)
    val forcedSegment: StateFlow<AvatarGardenSegment?> = _forcedSegment.asStateFlow()

    fun setForcedSegment(segment: AvatarGardenSegment?) {
        _forcedSegment.value = segment
    }
}
