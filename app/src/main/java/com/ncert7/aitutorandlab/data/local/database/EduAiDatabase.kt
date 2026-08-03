package com.ncert7.aitutorandlab.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ncert7.aitutorandlab.data.local.dao.ExamPlanDao
import com.ncert7.aitutorandlab.data.local.dao.NotificationBudgetDao
import com.ncert7.aitutorandlab.data.local.dao.NotificationLogDao
import com.ncert7.aitutorandlab.data.local.dao.PlanTrialItemDao
import com.ncert7.aitutorandlab.data.local.dao.GardenDao
import com.ncert7.aitutorandlab.data.local.dao.GamificationDao
import com.ncert7.aitutorandlab.data.local.dao.FriendDao
import com.ncert7.aitutorandlab.data.local.dao.LeagueDao
import com.ncert7.aitutorandlab.data.local.dao.QuestDailyDao
import com.ncert7.aitutorandlab.data.local.dao.AppAnalyticsDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterAgentProgressDao
import com.ncert7.aitutorandlab.data.local.dao.ChapterDao
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.ProgressDao
import com.ncert7.aitutorandlab.data.local.dao.SessionDao
import com.ncert7.aitutorandlab.data.local.dao.SimulationInteractionDao
import com.ncert7.aitutorandlab.data.local.dao.StreakDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.dao.SubjectDao
import com.ncert7.aitutorandlab.data.local.dao.TutorConfigDao
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanDayEntity
import com.ncert7.aitutorandlab.data.local.entities.NotificationBudgetEntity
import com.ncert7.aitutorandlab.data.local.entities.NotificationLogEntity
import com.ncert7.aitutorandlab.data.local.entities.ExamPlanEntity
import com.ncert7.aitutorandlab.data.local.entities.PlanTrialItemEntity
import com.ncert7.aitutorandlab.data.local.entities.QuestDailyEntity
import com.ncert7.aitutorandlab.data.local.entities.GardenStateEntity
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import com.ncert7.aitutorandlab.data.local.entities.GemEventEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendConnectionEntity
import com.ncert7.aitutorandlab.data.local.entities.FriendFeedItemEntity
import com.ncert7.aitutorandlab.data.local.entities.LeagueCacheEntity
import com.ncert7.aitutorandlab.data.local.entities.LeagueMemberEntity
import com.ncert7.aitutorandlab.data.local.entities.GamificationProfileEntity
import com.ncert7.aitutorandlab.data.local.entities.XpEventEntity
import com.ncert7.aitutorandlab.data.local.entities.AppAnalyticsEntity
import com.ncert7.aitutorandlab.data.local.entities.ChapterAgentProgressEntity
import com.ncert7.aitutorandlab.data.local.entities.ChapterEntity
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.data.local.entities.SessionEntity
import com.ncert7.aitutorandlab.data.local.entities.SimulationInteractionEntity
import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import com.ncert7.aitutorandlab.data.local.entities.StudentEntity
import com.ncert7.aitutorandlab.data.local.entities.SubjectEntity
import com.ncert7.aitutorandlab.data.local.entities.TutorConfigEntity

/**
 * Main Room Database for EduAi App
 */
@Database(
    entities = [
        StudentEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        ConceptEntity::class,
        SessionEntity::class,
        AppAnalyticsEntity::class,
        ProgressEntity::class,
        StreakEntity::class,
        ChapterAgentProgressEntity::class,
        SimulationInteractionEntity::class,
        GamificationProfileEntity::class,
        XpEventEntity::class,
        ExamPlanEntity::class,
        ExamPlanDayEntity::class,
        PlanTrialItemEntity::class,
        QuestDailyEntity::class,
        GemEventEntity::class,
        LeagueMemberEntity::class,
        LeagueCacheEntity::class,
        FriendConnectionEntity::class,
        FriendFeedItemEntity::class,
        TutorConfigEntity::class,
        NotificationLogEntity::class,
        NotificationBudgetEntity::class,
        GrownItemEntity::class,
        GardenStateEntity::class,
    ],
    version = 15,
    exportSchema = false
)
abstract class EduAiDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun conceptDao(): ConceptDao
    abstract fun progressDao(): ProgressDao
    abstract fun sessionDao(): SessionDao
    abstract fun appAnalyticsDao(): AppAnalyticsDao
    abstract fun streakDao(): StreakDao
    abstract fun chapterAgentProgressDao(): ChapterAgentProgressDao
    abstract fun simulationInteractionDao(): SimulationInteractionDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun examPlanDao(): ExamPlanDao
    abstract fun planTrialItemDao(): PlanTrialItemDao
    abstract fun questDailyDao(): QuestDailyDao
    abstract fun leagueDao(): LeagueDao
    abstract fun friendDao(): FriendDao
    abstract fun tutorConfigDao(): TutorConfigDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun notificationBudgetDao(): NotificationBudgetDao
    abstract fun gardenDao(): GardenDao

    companion object {
        @Volatile
        private var INSTANCE: EduAiDatabase? = null

        private const val DATABASE_NAME = "eduai_database"

        fun getInstance(context: Context): EduAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduAiDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                    )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}