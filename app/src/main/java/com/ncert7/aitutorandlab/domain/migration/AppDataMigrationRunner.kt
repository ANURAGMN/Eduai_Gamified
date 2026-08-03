package com.ncert7.aitutorandlab.domain.migration

import com.ncert7.aitutorandlab.debug.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs pending [AppDataMigration] steps sequentially, like Room schema migrations.
 *
 * Invoked on app start (logged-in user) and on home/plan refresh so upgrades apply
 * even if the user skips opening the trial screen.
 */
@Singleton
class AppDataMigrationRunner @Inject constructor(
    private val versionStore: AppMigrationVersionStore,
    private val trialScheduleMigrator: TrialScheduleMigrator,
    migrations: Set<@JvmSuppressWildcards AppDataMigration>,
) {
    private val migrations = migrations.sortedBy { it.toVersion }

    init {
        validateMigrationChain()
    }

    suspend fun runPendingMigrations(studentId: String, languageCode: String) {
        if (studentId.isBlank()) return

        var currentVersion = resolveStoredVersion()
        if (currentVersion >= AppDataMigrationVersions.CURRENT) return

        val context =
            AppMigrationContext(
                studentId = studentId,
                languageCode = languageCode,
                trialScheduleMigrator = trialScheduleMigrator,
            )

        for (migration in migrations) {
            if (migration.toVersion <= currentVersion) continue
            if (migration.toVersion != currentVersion + 1) {
                DebugLogger.errorLog(
                    TAG,
                    "Migration chain gap: at $currentVersion, next expected ${currentVersion + 1} " +
                        "but found ${migration.toVersion}",
                )
                return
            }

            try {
                migration.migrate(context)
                currentVersion = migration.toVersion
                persistVersion(currentVersion)
            } catch (e: Exception) {
                DebugLogger.errorLog(
                    TAG,
                    "Migration v${migration.toVersion} failed: ${e.message}",
                )
                return
            }
        }
    }

    private fun resolveStoredVersion(): Int {
        val unified = versionStore.getAppDataMigrationVersion()
        if (unified > 0) return unified

        // Backfill from legacy one-off flags written before the unified registry existed.
        var backfill = 0
        if (versionStore.isLegacyProgressMigrationDone()) {
            backfill = maxOf(backfill, AppDataMigrationVersions.LEGACY_PROGRESS_LANGUAGE)
        }
        val legacyTrialVersion = versionStore.getTrialMaterializerVersion()
        if (legacyTrialVersion > 0) {
            backfill = maxOf(backfill, legacyTrialVersion)
        }
        if (backfill > 0) {
            persistVersion(backfill)
        }
        return backfill
    }

    private fun persistVersion(version: Int) {
        versionStore.setAppDataMigrationVersion(version)
        // Keep legacy trial key in sync for older builds during rollout.
        versionStore.setTrialMaterializerVersion(version)
    }

    private fun validateMigrationChain() {
        if (migrations.isEmpty()) return
        var expected = migrations.first().toVersion
        for (migration in migrations) {
            require(migration.toVersion == expected) {
                "App data migrations must be contiguous starting at 1; missing v$expected"
            }
            expected++
        }
        require(migrations.last().toVersion == AppDataMigrationVersions.CURRENT) {
            "AppDataMigrationVersions.CURRENT must match the last registered migration"
        }
    }

    companion object {
        private const val TAG = "AppDataMigration"
    }
}
