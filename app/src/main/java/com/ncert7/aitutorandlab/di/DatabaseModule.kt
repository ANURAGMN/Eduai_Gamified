package com.ncert7.aitutorandlab.di

import android.content.Context
import com.ncert7.aitutorandlab.data.local.database.EduAiDatabase
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.ChapterAgentProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.data.local.dao.SimulationInteractionDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.dao.SubjectDao
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.utils.StreakManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EduAiDatabase {
        return EduAiDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideConceptDao(database: EduAiDatabase): ConceptDao {
        return database.conceptDao()
    }

    @Provides
    @Singleton
    fun provideChapterDao(database: EduAiDatabase): ChapterDao {
        return database.chapterDao()
    }

    @Provides
    @Singleton
    fun provideSubjectDao(database: EduAiDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    @Singleton
    fun provideStudentDao(database: EduAiDatabase): StudentDao {
        return database.studentDao()
    }

    @Provides
    @Singleton
    fun provideProgressDao(database: EduAiDatabase): ProgressDao {
        return database.progressDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferenceUtils(@ApplicationContext context: Context): SharedPreferenceUtils {
        return SharedPreferenceUtils(context)
    }

    @Provides
    @Singleton
    fun provideStreakDao(database: EduAiDatabase): StreakDao {
        return database.streakDao()
    }

    @Provides
    @Singleton
    fun provideChapterAgentProgressDao(database: EduAiDatabase): ChapterAgentProgressDao {
        return database.chapterAgentProgressDao()
    }

    @Provides
    @Singleton
    fun provideSimulationInteractionDao(database: EduAiDatabase): SimulationInteractionDao {
        return database.simulationInteractionDao()
    }

    @Provides
    @Singleton
    fun provideStreakManager(
        @ApplicationContext context: Context,
        streakRepository: StreakRepository,
        userId: String
    ): StreakManager {
        return StreakManager(
            context,
            streakRepository,
            userId
        )
    }

    @Provides
    @Singleton
    fun provideUserId(sharedPreferenceUtils: SharedPreferenceUtils): String {
        return sharedPreferenceUtils.getUserId().toString()
    }
}