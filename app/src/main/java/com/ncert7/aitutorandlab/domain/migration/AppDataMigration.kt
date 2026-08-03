package com.ncert7.aitutorandlab.domain.migration

import com.ncert7.aitutorandlab.repository.PlanTrialRepository

/** Context passed to each [AppDataMigration]. */
data class AppMigrationContext(
    val studentId: String,
    val languageCode: String,
    val trialScheduleMigrator: TrialScheduleMigrator,
)

/**
 * One upgrade step for derived app data (separate from Room schema migrations in
 * [com.ncert7.aitutorandlab.data.local.database.DatabaseMigrations]).
 *
 * Migrations must be registered in ascending [toVersion] order with no gaps.
 */
interface AppDataMigration {
    val toVersion: Int
    suspend fun migrate(context: AppMigrationContext)
}
