package com.ncert7.aitutorandlab.domain.gamification

import com.ncert7.aitutorandlab.data.local.entities.StreakEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Thorough coverage for app-open streak semantics:
 * same-day no-write, consecutive increment, gap reset, freeze continuation, UI display.
 */
class StreakActivityRulesTest {

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

    private fun entity(
        count: Int,
        lastDaysFromToday: Int,
        lastHour: Int = 9,
    ): StreakEntity {
        val last = dayOffset(lastDaysFromToday, lastHour)
        return StreakEntity(
            userId = "u1",
            streakCount = count,
            lastStreakDate = StreakDayLogic.startOfDay(last),
            createdAt = last,
            updatedAt = last,
            appName = "eduai_app",
            isSynced = true,
        )
    }

    // --- Day logic ---

    @Test
    fun sameDay_trueAcrossHours() {
        val morning = dayOffset(0, 8)
        val evening = dayOffset(0, 22)
        assertTrue(StreakDayLogic.isSameDay(morning, evening))
    }

    @Test
    fun consecutiveDay_trueForYesterdayToToday() {
        val yesterday = dayOffset(-1, 20)
        val today = dayOffset(0, 7)
        assertTrue(StreakDayLogic.isConsecutiveDay(yesterday, today))
        assertFalse(StreakDayLogic.isSameDay(yesterday, today))
    }

    @Test
    fun consecutiveDay_falseForTwoDayGap() {
        val twoDaysAgo = dayOffset(-2, 12)
        val today = dayOffset(0, 12)
        assertFalse(StreakDayLogic.isConsecutiveDay(twoDaysAgo, today))
    }

    // --- App-open / recordActivity rules ---

    @Test
    fun firstOpen_persistsOne() {
        val now = dayOffset(0)
        val r = StreakActivityRules.next(null, now)
        assertEquals(StreakActivityRules.Result.Persist(1), r)
    }

    @Test
    fun sameDayReopen_noWrite_keepsCount() {
        // Simulates: open app morning, reopen afternoon — Firestore must not be scheduled.
        val now = dayOffset(0, 18)
        val current = entity(count = 5, lastDaysFromToday = 0, lastHour = 9)
        val r = StreakActivityRules.next(current, now)
        assertEquals(StreakActivityRules.Result.NoWrite(5), r)
    }

    @Test
    fun nextDayOpen_increments() {
        // Yesterday streak=1 → today app open → 2 (user's Aug 9→10 case)
        val now = dayOffset(0, 10)
        val current = entity(count = 1, lastDaysFromToday = -1)
        val r = StreakActivityRules.next(current, now)
        assertEquals(StreakActivityRules.Result.Persist(2), r)
    }

    @Test
    fun nextDayOpen_fromFour_becomesFive() {
        val now = dayOffset(0)
        val current = entity(count = 4, lastDaysFromToday = -1)
        val r = StreakActivityRules.next(current, now)
        assertEquals(StreakActivityRules.Result.Persist(5), r)
    }

    @Test
    fun gapWithoutFreeze_resetsToOne() {
        val now = dayOffset(0)
        val current = entity(count = 7, lastDaysFromToday = -3)
        val r = StreakActivityRules.next(current, now, freezeContinuation = null)
        assertEquals(StreakActivityRules.Result.Persist(1), r)
    }

    @Test
    fun gapWithFreeze_usesContinuation() {
        val now = dayOffset(0)
        val current = entity(count = 7, lastDaysFromToday = -2)
        val r = StreakActivityRules.next(current, now, freezeContinuation = 8)
        assertEquals(StreakActivityRules.Result.Persist(8), r)
    }

    @Test
    fun repeatedSameDayOpens_idempotent() {
        val morning = dayOffset(0, 8)
        val noon = dayOffset(0, 12)
        val night = dayOffset(0, 23)
        var current: StreakEntity? = null

        // Day 1 first open
        when (val r = StreakActivityRules.next(current, morning)) {
            is StreakActivityRules.Result.Persist ->
                current = entity(r.count, 0).copy(lastStreakDate = StreakDayLogic.startOfDay(morning))
            is StreakActivityRules.Result.NoWrite -> error("expected persist")
        }
        assertEquals(1, current!!.streakCount)

        // Same day reopens (app foreground spam)
        assertEquals(
            StreakActivityRules.Result.NoWrite(1),
            StreakActivityRules.next(current, noon),
        )
        assertEquals(
            StreakActivityRules.Result.NoWrite(1),
            StreakActivityRules.next(current, night),
        )
    }

    @Test
    fun multiDayAppOpenChain() {
        // Open once per day for 4 days → 1,2,3,4
        var current: StreakEntity? = null
        for (day in 0 until 4) {
            val now = dayOffset(day - 3) // -3,-2,-1,0 relative to "today"
            when (val r = StreakActivityRules.next(current, now)) {
                is StreakActivityRules.Result.Persist -> {
                    current = StreakEntity(
                        userId = "u1",
                        streakCount = r.count,
                        lastStreakDate = StreakDayLogic.startOfDay(now),
                        createdAt = now,
                        updatedAt = now,
                        appName = "eduai_app",
                        isSynced = false,
                    )
                }
                is StreakActivityRules.Result.NoWrite -> error("day $day should persist")
            }
        }
        assertEquals(4, current!!.streakCount)
        // Extra opens today still NoWrite
        assertEquals(
            StreakActivityRules.Result.NoWrite(4),
            StreakActivityRules.next(current, dayOffset(0, 21)),
        )
    }

    // --- Display ---

    @Test
    fun display_nullIsOne() {
        assertEquals(1, StreakDayLogic.effectiveDisplayCount(null, dayOffset(0)))
    }

    @Test
    fun display_todayShowsCount() {
        val s = entity(3, 0)
        assertEquals(3, StreakDayLogic.effectiveDisplayCount(s, dayOffset(0, 15)))
    }

    @Test
    fun display_yesterdayStillShowsCount() {
        val s = entity(2, -1)
        assertEquals(2, StreakDayLogic.effectiveDisplayCount(s, dayOffset(0)))
    }

    @Test
    fun display_expiredShowsZero() {
        val s = entity(9, -3)
        assertEquals(0, StreakDayLogic.effectiveDisplayCount(s, dayOffset(0)))
    }

    @Test
    fun noWriteMeansNoFirestoreSchedule_contract() {
        // Documented contract: NoWrite ⇒ caller must not call updateStreak / scheduleDeferredUpload.
        val r = StreakActivityRules.next(entity(1, 0), dayOffset(0, 20))
        assertTrue(r is StreakActivityRules.Result.NoWrite)
    }
}
