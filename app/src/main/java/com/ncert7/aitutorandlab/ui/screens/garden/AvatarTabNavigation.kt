package com.ncert7.aitutorandlab.ui.screens.garden

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
}
