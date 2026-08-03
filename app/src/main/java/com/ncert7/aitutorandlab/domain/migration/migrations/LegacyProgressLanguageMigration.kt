package com.ncert7.aitutorandlab.domain.migration.migrations

import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.domain.migration.AppDataMigration
import com.ncert7.aitutorandlab.domain.migration.AppDataMigrationVersions
import com.ncert7.aitutorandlab.domain.migration.AppMigrationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Normalizes legacy progress language codes (formerly run from [EduAiApplication]). */
@Singleton
class LegacyProgressLanguageMigration @Inject constructor(
    private val progressDao: ProgressDao,
    private val sharedPrefs: SharedPreferenceUtils,
) : AppDataMigration {
    override val toVersion: Int = AppDataMigrationVersions.LEGACY_PROGRESS_LANGUAGE

    override suspend fun migrate(context: AppMigrationContext) {
        progressDao.markFullWordLegacyLanguages()
        progressDao.markDuplicateLegacyEnglishProgress()
        sharedPrefs.setLegacyProgressMigrationDone()
    }
}
