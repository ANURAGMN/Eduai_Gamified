package com.ncert7.aitutorandlab.service.sync

/**
 * Pure rules for when a remote garden may safely overwrite local Room state.
 *
 * Production policy (see docs/P1_PRODUCT_GAPS_DETAILED.md §1 + R.1):
 * - Any planted item → never restore (local progress wins).
 * - Any local steps > 0 → never restore (mid-task progress).
 * - Otherwise treat as pristine placeholder — including starter route `"1"`,
 *   onboarding theme (OUTPOST), or preferredSlot already set — so remote theme/plants win.
 */
object GardenRestorePolicy {

    fun canRestoreFromRemote(localItemCount: Int, localSteps: Int): Boolean =
        localItemCount == 0 && localSteps == 0

    /** Outcome codes for restore telemetry (`restore_applied` / `restore_skipped`). */
    enum class Outcome(val reason: String) {
        APPLIED("applied"),
        SKIPPED_LOCAL_PROGRESS("local_progressed"),
        REMOTE_EMPTY("remote_empty"),
        ERROR("error"),
    }
}
