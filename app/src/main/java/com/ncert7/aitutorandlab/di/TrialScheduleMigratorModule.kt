package com.ncert7.aitutorandlab.di

import com.ncert7.aitutorandlab.domain.migration.TrialScheduleMigrator
import com.ncert7.aitutorandlab.repository.PlanTrialRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TrialScheduleMigratorModule {
    @Binds
    abstract fun bindTrialScheduleMigrator(
        repository: PlanTrialRepository,
    ): TrialScheduleMigrator
}
