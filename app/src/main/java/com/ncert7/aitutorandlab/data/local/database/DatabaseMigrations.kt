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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gamification_profile` (
                `studentId` TEXT NOT NULL,
                `lifetimeXp` INTEGER NOT NULL DEFAULT 0,
                `weeklyXp` INTEGER NOT NULL DEFAULT 0,
                `gems` INTEGER NOT NULL DEFAULT 0,
                `leagueTier` TEXT NOT NULL DEFAULT 'BRONZE',
                `currentWeekKey` TEXT NOT NULL DEFAULT '',
                `cohortId` TEXT,
                `friendCode` TEXT NOT NULL DEFAULT '',
                `invitedByCode` TEXT,
                `inviteRewardGranted` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `xp_event` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `studentId` TEXT NOT NULL,
                `itemType` TEXT NOT NULL,
                `itemId` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `xpAmount` INTEGER NOT NULL,
                `weekKey` TEXT NOT NULL,
                `countsForLeague` INTEGER NOT NULL DEFAULT 1,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_xp_event_studentId_itemType_itemId_language`
            ON `xp_event` (`studentId`, `itemType`, `itemId`, `language`)
            """.trimIndent(),
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exam_plan` (
                `studentId` TEXT NOT NULL,
                `subjectId` TEXT NOT NULL,
                `examType` TEXT NOT NULL DEFAULT 'Unit Test',
                `dailyMinutes` INTEGER NOT NULL DEFAULT 30,
                `startEpochDay` INTEGER NOT NULL,
                `chapterIds` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exam_plan_day` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `studentId` TEXT NOT NULL,
                `dayIndex` INTEGER NOT NULL,
                `calendarEpochDay` INTEGER NOT NULL,
                `dayType` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `conceptIds` TEXT NOT NULL DEFAULT '',
                `estimatedMinutes` INTEGER NOT NULL DEFAULT 18,
                FOREIGN KEY(`studentId`) REFERENCES `exam_plan`(`studentId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_exam_plan_day_studentId_dayIndex`
            ON `exam_plan_day` (`studentId`, `dayIndex`)
            """.trimIndent(),
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quest_daily` (
                `studentId` TEXT NOT NULL,
                `questDate` TEXT NOT NULL,
                `simsDone` INTEGER NOT NULL DEFAULT 0,
                `simsTotal` INTEGER NOT NULL DEFAULT 3,
                `studyDone` INTEGER NOT NULL DEFAULT 0,
                `studyTotal` INTEGER NOT NULL DEFAULT 1,
                `simsClaimed` INTEGER NOT NULL DEFAULT 0,
                `studyClaimed` INTEGER NOT NULL DEFAULT 0,
                `bonusClaimed` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`, `questDate`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `gem_event` (
                `studentId` TEXT NOT NULL,
                `grantKey` TEXT NOT NULL,
                `gemsAmount` INTEGER NOT NULL,
                `source` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`, `grantKey`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `league_member` (
                `weekKey` TEXT NOT NULL,
                `cohortId` TEXT NOT NULL,
                `tier` TEXT NOT NULL,
                `memberId` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `weeklyXp` INTEGER NOT NULL DEFAULT 0,
                `streak` INTEGER NOT NULL DEFAULT 0,
                `isBot` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`weekKey`, `cohortId`, `memberId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `league_cache` (
                `studentId` TEXT NOT NULL,
                `weekKey` TEXT NOT NULL,
                `cohortId` TEXT NOT NULL,
                `rank` INTEGER NOT NULL DEFAULT 0,
                `totalParticipants` INTEGER NOT NULL DEFAULT 0,
                `fetchedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `friend_connection` (
                `studentId` TEXT NOT NULL,
                `friendStudentId` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'ACCEPTED',
                `displayName` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`, `friendStudentId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `friend_feed_item` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ownerStudentId` TEXT NOT NULL,
                `fromStudentId` TEXT NOT NULL,
                `fromDisplayName` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `message` TEXT NOT NULL,
                `cheers` INTEGER NOT NULL DEFAULT 0,
                `cheeredByMe` INTEGER NOT NULL DEFAULT 0,
                `seen` INTEGER NOT NULL DEFAULT 0,
                `eventKey` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_friend_feed_item_ownerStudentId_eventKey`
            ON `friend_feed_item` (`ownerStudentId`, `eventKey`)
            """.trimIndent(),
        )
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tutor_config` (
                `studentId` TEXT NOT NULL,
                `character` TEXT NOT NULL DEFAULT 'Free',
                `outfit` INTEGER NOT NULL DEFAULT 0,
                `neck` INTEGER NOT NULL DEFAULT 0,
                `hair` INTEGER NOT NULL DEFAULT 0,
                `hairColor` INTEGER NOT NULL DEFAULT 0,
                `glasses` INTEGER NOT NULL DEFAULT 0,
                `frameColor` INTEGER NOT NULL DEFAULT 0,
                `eyeLine` INTEGER NOT NULL DEFAULT 0,
                `cheeks` INTEGER NOT NULL DEFAULT 1,
                `presetId` TEXT,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `plan_trial_item` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `studentId` TEXT NOT NULL,
                `planDayId` INTEGER NOT NULL,
                `dayIndex` INTEGER NOT NULL,
                `chapterId` TEXT NOT NULL,
                `conceptId` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `sourceId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `sequenceIndex` INTEGER NOT NULL,
                `requiredCount` INTEGER NOT NULL DEFAULT 1,
                `completedCount` INTEGER NOT NULL DEFAULT 0,
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `celebrated` INTEGER NOT NULL DEFAULT 0,
                `carriedFromDayIndex` INTEGER,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`planDayId`) REFERENCES `exam_plan_day`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_plan_trial_item_studentId_planDayId_sequenceIndex`
            ON `plan_trial_item` (`studentId`, `planDayId`, `sequenceIndex`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_plan_trial_item_studentId_dayIndex`
            ON `plan_trial_item` (`studentId`, `dayIndex`)
            """.trimIndent(),
        )
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `friend_feed_item`
            ADD COLUMN `visibility` TEXT NOT NULL DEFAULT 'FRIENDS'
            """.trimIndent(),
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `exam_plan`
            ADD COLUMN `examEpochDay` INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notification_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `studentId` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `shownEpochDay` INTEGER NOT NULL,
                `dedupKey` TEXT NOT NULL DEFAULT '',
                `shownAtMs` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_notification_log_studentId_shownEpochDay`
            ON `notification_log` (`studentId`, `shownEpochDay`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_notification_log_studentId_type_shownEpochDay_dedupKey`
            ON `notification_log` (`studentId`, `type`, `shownEpochDay`, `dedupKey`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notification_budget` (
                `studentId` TEXT NOT NULL,
                `budgetEpochDay` INTEGER NOT NULL,
                `sentCount` INTEGER NOT NULL DEFAULT 0,
                `lastSentAtMs` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`studentId`, `budgetEpochDay`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_notification_budget_studentId`
            ON `notification_budget` (`studentId`)
            """.trimIndent(),
        )
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `garden_item` (
                `id` TEXT NOT NULL,
                `studentId` TEXT NOT NULL,
                `zone` INTEGER NOT NULL,
                `plot` INTEGER NOT NULL,
                `slot` INTEGER NOT NULL,
                `conceptId` TEXT NOT NULL,
                `chapterId` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_garden_item_studentId_zone_plot`
            ON `garden_item` (`studentId`, `zone`, `plot`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_garden_item_studentId`
            ON `garden_item` (`studentId`)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `garden_state` (
                `studentId` TEXT NOT NULL,
                `theme` TEXT NOT NULL,
                `route` TEXT NOT NULL,
                `steps` INTEGER NOT NULL,
                `preferredSlot` INTEGER NOT NULL,
                PRIMARY KEY(`studentId`)
            )
            """.trimIndent(),
        )
    }
}
