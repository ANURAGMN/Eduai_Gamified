package com.ncert7.aitutorandlab.domain.gamification

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

object GamificationWeekKey {
    private val zone = ZoneId.of("Asia/Kolkata")

    fun current(): String {
        val now = LocalDate.now(zone)
        val weekFields = WeekFields.ISO
        val week = now.get(weekFields.weekOfWeekBasedYear())
        val year = now.get(weekFields.weekBasedYear())
        return String.format("%d-W%02d", year, week)
    }

    /** Days until the ISO week resets (Monday 00:00 IST). Sunday returns 1. */
    fun daysRemainingInWeek(): Int {
        val dayOfWeek = LocalDate.now(zone).dayOfWeek.value
        return 8 - dayOfWeek
    }
}
