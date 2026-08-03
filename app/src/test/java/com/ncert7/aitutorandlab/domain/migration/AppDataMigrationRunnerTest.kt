package com.ncert7.aitutorandlab.domain.migration

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDataMigrationRunnerTest {

    @Test
    fun runPendingMigrations_executesStepsInOrderFromZero() = runBlocking {
        val store = InMemoryVersionStore()
        val steps = mutableListOf<Int>()
        val runner =
            AppDataMigrationRunner(
                store,
                RecordingTrialScheduleMigrator(),
                fullMigrationChain(steps),
            )

        runner.runPendingMigrations("student-1", "en")

        assertEquals(listOf(1, 2, 3, 4, 5), steps)
        assertEquals(5, store.version)
    }

    @Test
    fun runPendingMigrations_skipsWhenAlreadyCurrent() = runBlocking {
        val store = InMemoryVersionStore(storedVersion = AppDataMigrationVersions.CURRENT)
        val migrator = RecordingTrialScheduleMigrator()
        val runner =
            AppDataMigrationRunner(
                store,
                migrator,
                fullMigrationChain(mutableListOf()),
            )

        runner.runPendingMigrations("student-1", "en")

        assertEquals(0, migrator.calls)
    }

    @Test
    fun runPendingMigrations_backfillsLegacyTrialVersion() = runBlocking {
        val store =
            InMemoryVersionStore(
                trialMaterializerVersion = 3,
                legacyProgressMigrationDone = true,
            )
        val steps = mutableListOf<Int>()
        val runner =
            AppDataMigrationRunner(
                store,
                RecordingTrialScheduleMigrator(),
                fullMigrationChain(steps),
            )

        runner.runPendingMigrations("student-1", "en")

        assertEquals(listOf(4, 5), steps)
    }

    private fun fullMigrationChain(steps: MutableList<Int>): Set<AppDataMigration> =
        setOf(
            recordingMigration(AppDataMigrationVersions.LEGACY_PROGRESS_LANGUAGE, steps),
            recordingMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_PER_CONCEPT, steps),
            recordingMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_BATCHED, steps),
            recordingMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_INTERLEAVED, steps),
            recordingMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_THREE_SIMS_PER_STUDY, steps),
        )

    private fun recordingMigration(
        version: Int,
        steps: MutableList<Int>,
    ): AppDataMigration =
        object : AppDataMigration {
            override val toVersion: Int = version

            override suspend fun migrate(context: AppMigrationContext) {
                steps += version
            }
        }

    private class InMemoryVersionStore(
        private var storedVersion: Int = 0,
        private val trialMaterializerVersion: Int = 0,
        private val legacyProgressMigrationDone: Boolean = false,
    ) : AppMigrationVersionStore {
        val version: Int get() = storedVersion

        override fun getAppDataMigrationVersion(): Int = storedVersion

        override fun setAppDataMigrationVersion(version: Int) {
            storedVersion = version
        }

        override fun getTrialMaterializerVersion(): Int = trialMaterializerVersion

        override fun setTrialMaterializerVersion(version: Int) {
            storedVersion = maxOf(storedVersion, version)
        }

        override fun isLegacyProgressMigrationDone(): Boolean = legacyProgressMigrationDone
    }

    private class RecordingTrialScheduleMigrator : TrialScheduleMigrator {
        var calls = 0

        override suspend fun materializeAllPlanDays(studentId: String, languageCode: String) {
            calls++
        }
    }
}
