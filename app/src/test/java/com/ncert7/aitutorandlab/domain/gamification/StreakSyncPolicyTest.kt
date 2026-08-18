package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.data.firebase.model.Streak
import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class StreakSyncPolicyTest {

    private fun dayOffset(daysFromToday: Int, hour: Int = 10): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, daysFromToday)
        }
        return cal.timeInMillis
    }

    private fun local(
        count: Int,
        lastDaysFromToday: Int,
        synced: Boolean = false,
        updatedAt: Long? = null,
    ): StreakEntity {
        val last = StreakDayLogic.startOfDay(dayOffset(lastDaysFromToday))
        val now = dayOffset(0, 12)
        return StreakEntity(
            userId = "mail2anuragmn@gmail.com",
            streakCount = count,
            lastStreakDate = last,
            createdAt = last,
            updatedAt = updatedAt ?: now,
            appName = "eduai_app",
            isSynced = synced,
        )
    }

    private fun remote(count: Int, lastDaysFromToday: Int, updatedAt: Long = dayOffset(-1, 18)): Streak {
        val last = StreakDayLogic.startOfDay(dayOffset(lastDaysFromToday))
        return Streak(
            userId = "mail2anuragmn@gmail.com",
            streakCount = count,
            lastStreakDate = last,
            createdAt = last,
            updatedAt = updatedAt,
            appName = "eduai_app",
        )
    }

    @Test
    fun reinstallSameDay_seedDoesNotBeatLiveRemote() {
        val now = dayOffset(0, 21)
        val merged = StreakSyncPolicy.mergeForLogin(
            local = local(count = 1, lastDaysFromToday = 0, updatedAt = now),
            remote = remote(count = 9, lastDaysFromToday = 0, updatedAt = dayOffset(0, 9)),
            nowMs = now,
        )
        assertEquals(9, merged.streakCount)
        assertEquals(true, merged.isSynced)
    }

    @Test
    fun reinstallNextMorning_restoresLiveRemote() {
        val now = dayOffset(0, 8)
        val merged = StreakSyncPolicy.mergeForLogin(
            local = local(count = 1, lastDaysFromToday = 0, updatedAt = now),
            remote = remote(count = 9, lastDaysFromToday = -1),
            nowMs = now,
        )
        assertEquals(9, merged.streakCount)
    }

    @Test
    fun offlineContinuation_keepsHigherLocal() {
        val now = dayOffset(0, 10)
        val merged = StreakSyncPolicy.mergeForLogin(
            local = local(count = 10, lastDaysFromToday = 0),
            remote = remote(count = 9, lastDaysFromToday = -1),
            nowMs = now,
        )
        assertEquals(10, merged.streakCount)
        assertEquals(false, merged.isSynced)
    }

    @Test
    fun sameDay_takesMaxCount() {
        val now = dayOffset(0)
        val merged = StreakSyncPolicy.mergeForLogin(
            local = local(count = 4, lastDaysFromToday = 0),
            remote = remote(count = 6, lastDaysFromToday = 0),
            nowMs = now,
        )
        assertEquals(6, merged.streakCount)
    }

    @Test
    fun cloudWrite_sameDayNeverDecreases() {
        val today = StreakDayLogic.startOfDay(dayOffset(0))
        val out = StreakSyncPolicy.mergeCloudWrite(
            incomingCount = 1,
            incomingLastDate = today,
            remoteCount = 9,
            remoteLastDate = today,
            nowMs = dayOffset(0, 21),
        )
        assertEquals(9, out.streakCount)
        assertEquals(today, out.lastStreakDate)
    }

    @Test
    fun cloudWrite_nextDaySeedContinuesRemote() {
        val today = StreakDayLogic.startOfDay(dayOffset(0))
        val yesterday = StreakDayLogic.startOfDay(dayOffset(-1))
        val out = StreakSyncPolicy.mergeCloudWrite(
            incomingCount = 1,
            incomingLastDate = today,
            remoteCount = 9,
            remoteLastDate = yesterday,
            nowMs = dayOffset(0, 8),
        )
        assertEquals(10, out.streakCount)
        assertEquals(today, out.lastStreakDate)
    }

    @Test
    fun cloudWrite_realGapAllowsResetToOne() {
        val today = StreakDayLogic.startOfDay(dayOffset(0))
        val threeAgo = StreakDayLogic.startOfDay(dayOffset(-3))
        val out = StreakSyncPolicy.mergeCloudWrite(
            incomingCount = 1,
            incomingLastDate = today,
            remoteCount = 9,
            remoteLastDate = threeAgo,
            nowMs = dayOffset(0),
        )
        assertEquals(1, out.streakCount)
        assertEquals(today, out.lastStreakDate)
    }

    @Test
    fun cloudWrite_normalIncrementPreserved() {
        val today = StreakDayLogic.startOfDay(dayOffset(0))
        val yesterday = StreakDayLogic.startOfDay(dayOffset(-1))
        val out = StreakSyncPolicy.mergeCloudWrite(
            incomingCount = 10,
            incomingLastDate = today,
            remoteCount = 9,
            remoteLastDate = yesterday,
            nowMs = dayOffset(0),
        )
        assertEquals(10, out.streakCount)
    }
}
