package com.ncert7.aitutorandlab.domain.gamification

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object QuestDayKey {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun current(): String = LocalDate.now(zone).format(formatter)

    fun startOfDayMillis(questDate: String): Long {
        val date = LocalDate.parse(questDate, formatter)
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfDayMillis(questDate: String): Long {
        val date = LocalDate.parse(questDate, formatter)
        return date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    }
}
