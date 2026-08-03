package com.ncert7.aitutorandlab.domain.migration.migrations

import com.ncert7.aitutorandlab.domain.migration.AppDataMigration
import com.ncert7.aitutorandlab.domain.migration.AppMigrationContext

/**
 * Rebuilds all plan-day trial queues from syllabus data, preserving progress by
 * stable item key (kind + conceptId + sourceId).
 *
 * Used for trial ordering / materializer changes at v2, v3, and v4.
 */
internal class RematerializeTrialScheduleMigration(
    override val toVersion: Int,
) : AppDataMigration {
    override suspend fun migrate(context: AppMigrationContext) {
        context.trialScheduleMigrator.materializeAllPlanDays(
            studentId = context.studentId,
            languageCode = context.languageCode,
        )
    }
}
