package com.ncert7.aitutorandlab.domain.migration

/**
 * Monotonic version for derived / computed app data (trial queues, progress fixes, etc.).
 *
 * Bump [CURRENT] and add a new [AppDataMigration] when shipping changes that require
 * rematerialization or backfill. See docs/exam-plan/DATA_MIGRATIONS.md.
 */
object AppDataMigrationVersions {
    /** One-time progress language normalization. */
    const val LEGACY_PROGRESS_LANGUAGE = 1

    /** Trial queue: per-concept SIM_URL → STUDY → SIM_AGENT ordering. */
    const val TRIAL_SCHEDULE_PER_CONCEPT = 2

    /** Trial queue: batched sim URLs before study; sim agents on revise days only. */
    const val TRIAL_SCHEDULE_BATCHED = 3

    /** Trial queue: interleaved sim/study partitions + stacked UI order. */
    const val TRIAL_SCHEDULE_INTERLEAVED = 4

    /** Trial queue: three sims per study slot across plan days; leftover studies stacked. */
    const val TRIAL_SCHEDULE_THREE_SIMS_PER_STUDY = 5

    /** Must equal the highest migration [AppDataMigration.toVersion]. */
    const val CURRENT = TRIAL_SCHEDULE_THREE_SIMS_PER_STUDY
}
