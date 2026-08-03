package com.ncert7.aitutorandlab.di

import com.ncert7.aitutorandlab.domain.migration.AppMigrationVersionStore
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppMigrationVersionStoreModule {
    @Binds
    abstract fun bindAppMigrationVersionStore(
        utils: SharedPreferenceUtils,
    ): AppMigrationVersionStore
}
