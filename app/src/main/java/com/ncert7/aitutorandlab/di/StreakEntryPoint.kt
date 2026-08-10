package com.ncert7.aitutorandlab.di

import com.ncert7.aitutorandlab.repository.StreakRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StreakEntryPoint {
    fun streakRepository(): StreakRepository
}
