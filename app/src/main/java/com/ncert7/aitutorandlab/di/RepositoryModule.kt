package com.ncert7.aitutorandlab.di

import com.ncert7.aitutorandlab.data.local.dao.ChapterAgentProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
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
import com.ncert7.aitutorandlab.data.local.dao.QuestDailyDao
import com.ncert7.aitutorandlab.domain.gamification.FriendFeedService
import com.ncert7.aitutorandlab.domain.gamification.StreakFreezeService
import com.ncert7.aitutorandlab.domain.examplan.PlanFeasibilityAnalyzer
import com.ncert7.aitutorandlab.domain.examplan.ExamPlanMutationLock
import com.ncert7.aitutorandlab.domain.examplan.PlanTrialRolloverService
import com.ncert7.aitutorandlab.repository.ExamPlanRepository
import com.ncert7.aitutorandlab.repository.PlanTrialRepository
import com.ncert7.aitutorandlab.repository.QuestRepository
import com.ncert7.aitutorandlab.repository.GamificationRepository
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import com.ncert7.aitutorandlab.repository.ProgressRepository
import com.ncert7.aitutorandlab.repository.SimulationInteractionRepository
import com.ncert7.aitutorandlab.repository.StreakRepository
import com.ncert7.aitutorandlab.repository.StudentLocalRepository
import com.ncert7.aitutorandlab.repository.SubjectRepository
import com.ncert7.aitutorandlab.repository.TutorConfigRepository
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
        firebaseRepository: FirebaseRepository,
        friendFeedService: FriendFeedService,
        streakFreezeService: StreakFreezeService,
    ): StreakRepository {
        return StreakRepository(streakDao, firebaseRepository, friendFeedService, streakFreezeService)
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
    fun provideExamPlanRepository(
        examPlanDao: ExamPlanDao,
        chapterDao: ChapterDao,
        conceptDao: ConceptDao,
        progressDao: ProgressDao,
        planTrialRepository: PlanTrialRepository,
        planTrialRolloverService: PlanTrialRolloverService,
        planFeasibilityAnalyzer: PlanFeasibilityAnalyzer,
        sharedPreferenceUtils: SharedPreferenceUtils,
        planMutationLock: ExamPlanMutationLock,
    ): ExamPlanRepository {
        return ExamPlanRepository(
            examPlanDao,
            chapterDao,
            conceptDao,
            progressDao,
            planTrialRepository,
            planTrialRolloverService,
            planFeasibilityAnalyzer,
            sharedPreferenceUtils,
            planMutationLock,
        )
    }

    @Provides
    @Singleton
    fun provideQuestRepository(
        questDailyDao: QuestDailyDao,
        progressDao: ProgressDao,
        examPlanDao: ExamPlanDao,
        planTrialRepository: PlanTrialRepository,
        gamificationRepository: GamificationRepository,
        sharedPreferenceUtils: SharedPreferenceUtils,
    ): QuestRepository {
        return QuestRepository(
            questDailyDao,
            progressDao,
            examPlanDao,
            planTrialRepository,
            gamificationRepository,
            sharedPreferenceUtils,
        )
    }

    @Provides
    @Singleton
    fun provideSimulationInteractionRepository(
        simulationInteractionDao: SimulationInteractionDao,
        sharedPreferenceUtils: SharedPreferenceUtils
    ): SimulationInteractionRepository {
        return SimulationInteractionRepository(simulationInteractionDao, sharedPreferenceUtils)
    }

    @Provides
    @Singleton
    fun provideTutorConfigRepository(
        tutorConfigDao: com.ncert7.aitutorandlab.data.local.dao.TutorConfigDao,
        firebaseRepository: FirebaseRepository,
    ): TutorConfigRepository {
        return TutorConfigRepository(tutorConfigDao, firebaseRepository)
    }
}
