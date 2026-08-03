package com.ncert7.aitutorandlab.notification

import com.ncert7.aitutorandlab.data.local.dao.NotificationBudgetDao
import com.ncert7.aitutorandlab.data.local.dao.NotificationLogDao
import com.ncert7.aitutorandlab.data.local.entities.NotificationBudgetEntity
import com.ncert7.aitutorandlab.data.local.entities.NotificationLogEntity
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed dedup log + daily send budget (3/day default, 2h min gap).
 */
@Singleton
class NotificationLedger @Inject constructor(
    private val logDao: NotificationLogDao,
    private val budgetDao: NotificationBudgetDao,
) {
    private val zone = ZoneId.of("Asia/Kolkata")

    data class BudgetStatus(
        val sentToday: Int,
        val remainingToday: Int,
        val lastSentAtMs: Long,
        val nextAllowedAtMs: Long,
    )

    sealed class SendBlockReason {
        data object DailyCap : SendBlockReason()

        data class MinGap(val nextAllowedAtMs: Long) : SendBlockReason()
    }

    suspend fun wasShownToday(
        studentId: String,
        type: NotificationType,
        dedupKey: String = "",
        todayEpochDay: Long = todayEpochDay(),
    ): Boolean =
        logDao.wasShown(
            studentId = studentId,
            type = type.id,
            shownEpochDay = todayEpochDay,
            dedupKey = dedupKey,
        )

    suspend fun budgetStatus(
        studentId: String,
        dailyCap: Int = DEFAULT_DAILY_CAP,
        todayEpochDay: Long = todayEpochDay(),
    ): BudgetStatus {
        val row = budgetDao.getBudget(studentId, todayEpochDay)
        val sent = row?.sentCount ?: 0
        val lastSent = row?.lastSentAtMs ?: 0L
        val nextAllowed = if (lastSent == 0L) 0L else lastSent + MIN_GAP_MILLIS
        return BudgetStatus(
            sentToday = sent,
            remainingToday = (dailyCap - sent).coerceAtLeast(0),
            lastSentAtMs = lastSent,
            nextAllowedAtMs = nextAllowed,
        )
    }

    suspend fun blockReason(
        studentId: String,
        dailyCap: Int = DEFAULT_DAILY_CAP,
        nowMs: Long = System.currentTimeMillis(),
        todayEpochDay: Long = todayEpochDay(),
    ): SendBlockReason? {
        val status = budgetStatus(studentId, dailyCap, todayEpochDay)
        if (status.sentToday >= dailyCap) return SendBlockReason.DailyCap
        if (status.lastSentAtMs != 0L && nowMs < status.nextAllowedAtMs) {
            return SendBlockReason.MinGap(status.nextAllowedAtMs)
        }
        return null
    }

    suspend fun recordSend(
        studentId: String,
        type: NotificationType,
        dedupKey: String = "",
        nowMs: Long = System.currentTimeMillis(),
        todayEpochDay: Long = todayEpochDay(),
    ) {
        logDao.insert(
            NotificationLogEntity(
                studentId = studentId,
                type = type.id,
                shownEpochDay = todayEpochDay,
                dedupKey = dedupKey,
                shownAtMs = nowMs,
            ),
        )

        val existing = budgetDao.getBudget(studentId, todayEpochDay)
        val sent = (existing?.sentCount ?: 0) + 1
        budgetDao.upsert(
            NotificationBudgetEntity(
                studentId = studentId,
                budgetEpochDay = todayEpochDay,
                sentCount = sent,
                lastSentAtMs = nowMs,
            ),
        )
    }

    fun todayEpochDay(): Long = LocalDate.now(zone).toEpochDay()

    companion object {
        const val DEFAULT_DAILY_CAP = 3
        const val MIN_GAP_MILLIS = 2 * 60 * 60 * 1000L
    }
}
