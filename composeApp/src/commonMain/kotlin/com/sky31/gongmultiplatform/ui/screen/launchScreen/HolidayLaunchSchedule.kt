package com.sky31.gongmultiplatform.ui.screen.launchScreen

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** 节日启动页至少完整展示的时间。 */
const val HOLIDAY_LAUNCH_MIN_DISPLAY_MILLIS = 1_500L

/** 同一个节日启动页每天最多展示的次数。 */
const val HOLIDAY_LAUNCH_MAX_SHOW_COUNT_PER_DAY = 2

enum class HolidayLaunch {
    TEACHERS_DAY,
}

/**
 * 每年重复使用的月、日、时、分配置。
 * [HolidayLaunchSchedule.activeYears] 为空时每年生效；指定年份后只在对应年份生效。
 */
data class AnnualDateTime(
    val month: Int,
    val day: Int,
    val hour: Int = 0,
    val minute: Int = 0,
) {
    init {
        require(month in 1..12) { "month must be between 1 and 12" }
        require(day in 1..31) { "day must be between 1 and 31" }
        require(hour in 0..23) { "hour must be between 0 and 23" }
        require(minute in 0..59) { "minute must be between 0 and 59" }
    }

    internal val sortKey: Int
        get() = (((month * 100) + day) * 100 + hour) * 100 + minute
}

data class HolidayLaunchSchedule(
    val holiday: HolidayLaunch,
    val startsAt: AnnualDateTime,
    val endsAtExclusive: AnnualDateTime,
    val activeYears: Set<Int>? = null,
)

data class HolidayLaunchOccurrence(
    val holiday: HolidayLaunch,
    val date: LocalDate,
) {
    internal val storageKey: String
        get() = "${holiday.name}:$date"
}

/*
 * 节日启用时间统一在这里维护，区间采用 [开始时间, 结束时间) 规则。
 *
 * 固定公历日期（教师节、国庆节）可以不填 activeYears，会每年重复生效。
 * 日期每年不同的节日（中秋节）请指定 activeYears，并按年份增加一条配置。
 * 新增节日时，还需要在 HolidayLaunch 中增加类型，并在 HolidayLaunchScreen 中实现页面。
 *
 * 示例：
 * HolidayLaunchSchedule(
 *     holiday = HolidayLaunch.MID_AUTUMN_FESTIVAL,
 *     startsAt = AnnualDateTime(month = 9, day = 25),
 *     endsAtExclusive = AnnualDateTime(month = 9, day = 26),
 *     activeYears = setOf(2026),
 * )
 * HolidayLaunchSchedule(
 *     holiday = HolidayLaunch.NATIONAL_DAY,
 *     startsAt = AnnualDateTime(month = 10, day = 1),
 *     endsAtExclusive = AnnualDateTime(month = 10, day = 8),
 * )
 */
val holidayLaunchSchedules = listOf(
    HolidayLaunchSchedule(
        holiday = HolidayLaunch.TEACHERS_DAY,
        startsAt = AnnualDateTime(month = 9, day = 10),
        endsAtExclusive = AnnualDateTime(month = 9, day = 11),
    ),
)

@OptIn(ExperimentalTime::class)
fun currentHolidayLaunch(
    now: LocalDateTime = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()),
): HolidayLaunchOccurrence? = holidayLaunchSchedules
    .firstOrNull { schedule -> schedule.isActiveAt(now) }
    ?.let { schedule ->
        HolidayLaunchOccurrence(
            holiday = schedule.holiday,
            date = now.date,
        )
    }

private fun HolidayLaunchSchedule.isActiveAt(now: LocalDateTime): Boolean {
    if (activeYears != null && now.year !in activeYears) return false

    val currentKey = AnnualDateTime(
        month = now.month.ordinal + 1,
        day = now.day,
        hour = now.hour,
        minute = now.minute,
    ).sortKey
    val startKey = startsAt.sortKey
    val endKey = endsAtExclusive.sortKey

    return if (startKey <= endKey) {
        currentKey in startKey until endKey
    } else {
        // 支持跨年配置，例如 12 月 31 日至次年 1 月 2 日。
        currentKey >= startKey || currentKey < endKey
    }
}
