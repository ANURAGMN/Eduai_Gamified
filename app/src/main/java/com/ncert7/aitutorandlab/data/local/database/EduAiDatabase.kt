package com.ncert7.aitutorandlab.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        SimulationInteractionEntity::class
    ],
    version = 3,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}