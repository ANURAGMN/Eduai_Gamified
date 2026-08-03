package com.ncert7.aitutorandlab.notification

import com.ncert7.aitutorandlab.data.local.dao.NotificationBudgetDao
import com.ncert7.aitutorandlab.data.local.dao.NotificationLogDao
import com.ncert7.aitutorandlab.data.local.entities.NotificationBudgetEntity
import com.ncert7.aitutorandlab.data.local.entities.NotificationLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationLedgerTest {
    private lateinit var logDao: FakeNotificationLogDao
    private lateinit var budgetDao: FakeNotificationBudgetDao
    private lateinit var ledger: NotificationLedger

    private val studentId = "student-1"
    private val today = 20_000L

    @Before
    fun setUp() {
        logDao = FakeNotificationLogDao()
        budgetDao = FakeNotificationBudgetDao()
        ledger = NotificationLedger(logDao, budgetDao)
    }

    @Test
    fun wasShownToday_returnsTrueAfterRecordSend() =
        runBlocking {
            ledger.recordSend(
                studentId = studentId,
                type = NotificationType.DAILY_REMINDER,
                dedupKey = "daily",
                nowMs = 1_000L,
                todayEpochDay = today,
            )

            assertTrue(
                ledger.wasShownToday(
                    studentId = studentId,
                    type = NotificationType.DAILY_REMINDER,
                    dedupKey = "daily",
                    todayEpochDay = today,
                ),
            )
        }

    @Test
    fun blockReason_fourthSendBlockedWhenCapIsThree() =
        runBlocking {
            repeat(3) { index ->
                ledger.recordSend(
                    studentId = studentId,
                    type = NotificationType.DAILY_REMINDER,
                    dedupKey = "daily_$index",
                    nowMs = 1_000L + index * NotificationLedger.MIN_GAP_MILLIS,
                    todayEpochDay = today,
                )
            }

            val fourthAttemptMs = 1_000L + 3 * NotificationLedger.MIN_GAP_MILLIS
            assertEquals(
                NotificationLedger.SendBlockReason.DailyCap,
                ledger.blockReason(studentId, dailyCap = 3, nowMs = fourthAttemptMs, todayEpochDay = today),
            )
        }

    @Test
    fun blockReason_dailyCapReached() =
        runBlocking {
            repeat(3) { index ->
                ledger.recordSend(
                    studentId = studentId,
                    type = NotificationType.DAILY_REMINDER,
                    dedupKey = "daily_$index",
                    nowMs = 1_000L + index * NotificationLedger.MIN_GAP_MILLIS,
                    todayEpochDay = today,
                )
            }

            val reason = ledger.blockReason(studentId, dailyCap = 3, nowMs = 10_000L, todayEpochDay = today)
            assertEquals(NotificationLedger.SendBlockReason.DailyCap, reason)
        }

    @Test
    fun blockReason_minGapNotElapsed() =
        runBlocking {
            ledger.recordSend(
                studentId = studentId,
                type = NotificationType.DAILY_REMINDER,
                dedupKey = "daily",
                nowMs = 5_000L,
                todayEpochDay = today,
            )

            val reason =
                ledger.blockReason(
                    studentId = studentId,
                    dailyCap = 3,
                    nowMs = 5_000L + NotificationLedger.MIN_GAP_MILLIS - 1,
                    todayEpochDay = today,
                )

            assertTrue(reason is NotificationLedger.SendBlockReason.MinGap)
        }

    @Test
    fun blockReason_allowsAfterGapAndUnderCap() =
        runBlocking {
            ledger.recordSend(
                studentId = studentId,
                type = NotificationType.DAILY_REMINDER,
                dedupKey = "daily",
                nowMs = 5_000L,
                todayEpochDay = today,
            )

            val reason =
                ledger.blockReason(
                    studentId = studentId,
                    dailyCap = 3,
                    nowMs = 5_000L + NotificationLedger.MIN_GAP_MILLIS,
                    todayEpochDay = today,
                )

            assertNull(reason)
        }

    @Test
    fun budgetStatus_tracksRemainingSends() =
        runBlocking {
            ledger.recordSend(
                studentId = studentId,
                type = NotificationType.STREAK_AT_RISK,
                dedupKey = "streak",
                nowMs = 1_000L,
                todayEpochDay = today,
            )

            val status = ledger.budgetStatus(studentId, dailyCap = 3, todayEpochDay = today)
            assertEquals(1, status.sentToday)
            assertEquals(2, status.remainingToday)
            assertNotNull(status.nextAllowedAtMs)
        }

    private class FakeNotificationLogDao : NotificationLogDao {
        private val entries = mutableListOf<NotificationLogEntity>()

        override suspend fun insert(entity: NotificationLogEntity): Long {
            val duplicate =
                entries.any {
                    it.studentId == entity.studentId &&
                        it.type == entity.type &&
                        it.shownEpochDay == entity.shownEpochDay &&
                        it.dedupKey == entity.dedupKey
                }
            if (duplicate) return -1L
            entries.add(entity)
            return entries.size.toLong()
        }

        override suspend fun wasShown(
            studentId: String,
            type: String,
            shownEpochDay: Long,
            dedupKey: String,
        ): Boolean =
            entries.any {
                it.studentId == studentId &&
                    it.type == type &&
                    it.shownEpochDay == shownEpochDay &&
                    it.dedupKey == dedupKey
            }

        override suspend fun wasShownEver(
            studentId: String,
            type: String,
            dedupKey: String,
        ): Boolean =
            entries.any {
                it.studentId == studentId && it.type == type && it.dedupKey == dedupKey
            }
    }

    private class FakeNotificationBudgetDao : NotificationBudgetDao {
        private val budgets = mutableMapOf<Pair<String, Long>, NotificationBudgetEntity>()

        override suspend fun getBudget(
            studentId: String,
            budgetEpochDay: Long,
        ): NotificationBudgetEntity? = budgets[studentId to budgetEpochDay]

        override suspend fun upsert(entity: NotificationBudgetEntity) {
            budgets[entity.studentId to entity.budgetEpochDay] = entity
        }
    }
}
