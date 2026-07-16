package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.model.CalendarData
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

val chineseNumberMap = mapOf(
    1 to "一",
    2 to "二",
    3 to "三",
    4 to "四",
    5 to "五",
    6 to "六",
    7 to "七",
)

@OptIn(ExperimentalTime::class)
fun getWeekNum(calendar: CalendarData, currentTime: LocalDateTime): Long {
    val startLocalDateTime = LocalDate.parse(calendar.start).atTime(0, 0)
    val startInstant = startLocalDateTime.toInstant(TimeZone.currentSystemDefault())
    val currentInstant = currentTime.toInstant(TimeZone.currentSystemDefault())

    val duration = currentInstant - startInstant

    return duration.inWholeDays / 7 + 1
}

@OptIn(ExperimentalTime::class)
fun getDiffDays(firstTime: LocalDateTime, secondeTime: LocalDateTime): Long {
    val firstInstant = firstTime.toInstant(TimeZone.currentSystemDefault())
    val secondeInstant = secondeTime.toInstant(TimeZone.currentSystemDefault())

    return (secondeInstant - firstInstant).inWholeDays
}

fun parseLocalDateTimeOrNull(
    raw: String?,
    label: String = "date time"
): LocalDateTime? {
    if (raw.isNullOrBlank()) {
        return null
    }

    return runCatching {
        LocalDateTime.parse(raw)
    }.getOrElse {
        println("Failed to parse $label: ${it.message}")
        null
    }
}
