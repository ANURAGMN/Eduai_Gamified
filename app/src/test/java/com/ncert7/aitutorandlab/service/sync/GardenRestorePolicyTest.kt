package com.ncert7.aitutorandlab.service.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenRestorePolicyTest {

    @Test
    fun canRestore_whenEmptyAndNoSteps() {
        assertTrue(GardenRestorePolicy.canRestoreFromRemote(localItemCount = 0, localSteps = 0))
    }

    @Test
    fun canRestore_blocksWhenAnyPlantExists() {
        assertFalse(GardenRestorePolicy.canRestoreFromRemote(localItemCount = 1, localSteps = 0))
        assertFalse(GardenRestorePolicy.canRestoreFromRemote(localItemCount = 3, localSteps = 0))
    }

    @Test
    fun canRestore_blocksWhenLocalStepsInProgress() {
        assertFalse(GardenRestorePolicy.canRestoreFromRemote(localItemCount = 0, localSteps = 1))
        assertFalse(GardenRestorePolicy.canRestoreFromRemote(localItemCount = 0, localSteps = 6))
    }

    @Test
    fun canRestore_allowsStarterPlaceholderRegardlessOfThemeOrRoute() {
        // Theme OUTPOST + route "1" + preferredSlot set are still pristine if steps/items empty.
        // Those fields must not block restore (Bug A / R.1).
        assertTrue(GardenRestorePolicy.canRestoreFromRemote(localItemCount = 0, localSteps = 0))
    }
}
