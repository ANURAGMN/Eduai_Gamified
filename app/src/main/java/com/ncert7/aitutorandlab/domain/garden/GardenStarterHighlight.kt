package com.ncert7.aitutorandlab.domain.garden

import com.anurag.eduai.uikit.garden.quest.PREFERRED_SLOT_SURPRISE
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.placeBased

object GardenStarterHighlight {
    fun shouldShow(
        progress: GardenProgress,
        theme: Theme,
        highlightSeen: Boolean,
    ): Boolean {
        if (highlightSeen || progress.totalPlanted > 0 || !theme.placeBased) return false
        return progress.preferredSlot == PREFERRED_SLOT_SURPRISE
    }
}
