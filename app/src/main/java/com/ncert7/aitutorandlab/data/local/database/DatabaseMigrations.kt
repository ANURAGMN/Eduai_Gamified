package com.ncert7.aitutorandlab.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_analytics ADD COLUMN conceptId TEXT")
        db.execSQL("ALTER TABLE app_analytics ADD COLUMN source TEXT")
        db.execSQL("ALTER TABLE app_analytics ADD COLUMN interactionType TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `simulation_interactions` (
                `interactionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `studentId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `simulationTitle` TEXT NOT NULL,
                `subjectName` TEXT NOT NULL,
                `chapterName` TEXT NOT NULL,
                `elementClicked` TEXT NOT NULL,
                `elementType` TEXT NOT NULL,
                `givenAnswer` TEXT NOT NULL,
                `isCorrect` TEXT NOT NULL,
                `timeTaken` TEXT NOT NULL,
                `timestamp` TEXT NOT NULL,
                `occurredAt` INTEGER NOT NULL,
                `interactionDate` TEXT NOT NULL,
                `appName` TEXT NOT NULL DEFAULT '',
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`sessionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_studentId` ON `simulation_interactions`(`studentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_sessionId` ON `simulation_interactions`(`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_interactionDate` ON `simulation_interactions`(`interactionDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_simulation_interactions_studentId_interactionDate_isSynced` ON `simulation_interactions`(`studentId`, `interactionDate`, `isSynced`)")
    }
}
