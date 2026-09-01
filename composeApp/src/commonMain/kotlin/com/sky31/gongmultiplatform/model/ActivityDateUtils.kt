package com.sky31.gongmultiplatform.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun parseActivityDate(value: String): LocalDate? = runCatching {
    LocalDate.parse(value.trim().replace('.', '-'))
}.getOrNull()

@OptIn(ExperimentalTime::class)
fun currentActivityDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun CampusActivity.isPastActivity(today: LocalDate = currentActivityDate()): Boolean =
    parseActivityDate(endDate)?.let { it < today } ?: false

fun CampusActivity.durationDays(): Int? {
    val start = parseActivityDate(startDate) ?: return null
    val end = parseActivityDate(endDate) ?: return null
    return (start.daysUntil(end) + 1).takeIf { it > 0 }
}

fun CampusActivity.durationLabel(): String =
    durationDays()?.let { "共 $it 天" } ?: "持续时间待定"
