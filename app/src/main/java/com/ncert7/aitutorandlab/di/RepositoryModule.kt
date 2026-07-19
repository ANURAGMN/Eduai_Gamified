package com.ncert7.aitutorandlab.di

import com.ncert7.aitutorandlab.data.local.dao.ChapterAgentProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.data.local.dao.SimulationInteractionDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.dao.SubjectDao
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.repository.ChapterRepository
import com.ncert7.aitutorandlab.repository.ConceptRepository
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.repository.ProgressRepository
import com.ncert7.aitutorandlab.repository.SimulationInteractionRepository
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.repository.StudentLocalRepository
import com.ncert7.aitutorandlab.repository.SubjectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideConceptRepository(
        conceptDao: ConceptDao,
        progressDao: ProgressDao
    ): ConceptRepository {
        return ConceptRepository(conceptDao, progressDao)
    }

    @Provides
    @Singleton
    fun provideChapterRepository(
        chapterDao: ChapterDao,
        progressDao: ProgressDao
    ): ChapterRepository {
        return ChapterRepository(chapterDao, progressDao)
    }

    @Provides
    @Singleton
    fun provideSubjectRepository(subjectDao: SubjectDao): SubjectRepository {
        return SubjectRepository(subjectDao)
    }

    @Provides
    @Singleton
    fun provideStudentRepository(studentDao: StudentDao): StudentLocalRepository {
        return StudentLocalRepository(studentDao)
    }

    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository {
        return FirebaseRepository()
    }

    @Provides
    @Singleton
    fun provideStreakRepository(
        streakDao: StreakDao,
        firebaseRepository: FirebaseRepository
    ): StreakRepository {
        return StreakRepository(streakDao, firebaseRepository)
    }

    @Provides
    @Singleton
    fun provideProgressRepository(
        progressDao: ProgressDao,
        chapterAgentProgressDao: ChapterAgentProgressDao
    ): ProgressRepository {
        return ProgressRepository(progressDao, chapterAgentProgressDao)
    }

    @Provides
    @Singleton
    fun provideSimulationInteractionRepository(
        simulationInteractionDao: SimulationInteractionDao,
        sharedPreferenceUtils: SharedPreferenceUtils
    ): SimulationInteractionRepository {
        return SimulationInteractionRepository(simulationInteractionDao, sharedPreferenceUtils)
    }
}
