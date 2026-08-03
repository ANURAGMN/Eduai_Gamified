package com.ncert7.aitutorandlab.domain.migration

/** Persists the app-data migration version (backed by SharedPreferences in production). */
interface AppMigrationVersionStore {
    fun getAppDataMigrationVersion(): Int
    fun setAppDataMigrationVersion(version: Int)
    fun getTrialMaterializerVersion(): Int
    fun setTrialMaterializerVersion(version: Int)
    fun isLegacyProgressMigrationDone(): Boolean
}
