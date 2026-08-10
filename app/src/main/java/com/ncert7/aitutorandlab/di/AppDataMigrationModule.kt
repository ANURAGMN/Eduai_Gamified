package com.ncert7.aitutorandlab.di

import com.ncert7.aitutorandlab.domain.migration.AppDataMigration
import com.ncert7.aitutorandlab.domain.migration.AppDataMigrationVersions
import com.ncert7.aitutorandlab.domain.migration.migrations.LegacyProgressLanguageMigration
import com.ncert7.aitutorandlab.domain.migration.migrations.RematerializeTrialScheduleMigration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object AppDataMigrationModule {

    @Provides
    @IntoSet
    fun provideLegacyProgressLanguageMigration(
        migration: LegacyProgressLanguageMigration,
    ): AppDataMigration = migration

    @Provides
    @IntoSet
    fun provideTrialSchedulePerConceptMigration(): AppDataMigration =
        RematerializeTrialScheduleMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_PER_CONCEPT)

    @Provides
    @IntoSet
    fun provideTrialScheduleBatchedMigration(): AppDataMigration =
        RematerializeTrialScheduleMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_BATCHED)

    @Provides
    @IntoSet
    fun provideTrialScheduleInterleavedMigration(): AppDataMigration =
        RematerializeTrialScheduleMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_INTERLEAVED)

    @Provides
    @IntoSet
    fun provideTrialScheduleThreeSimsPerStudyMigration(): AppDataMigration =
        RematerializeTrialScheduleMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_THREE_SIMS_PER_STUDY)

    @Provides
    @IntoSet
    fun provideTrialScheduleTwoSimsPerStudyMigration(): AppDataMigration =
        RematerializeTrialScheduleMigration(AppDataMigrationVersions.TRIAL_SCHEDULE_TWO_SIMS_PER_STUDY)
}
