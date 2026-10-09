package com.sky31.gongmultiplatform.util

import androidx.compose.ui.graphics.Color
import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.CourseElem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

val weekdayNameMap = mapOf(
    1 to "Monday",
    2 to "Tuesday",
    3 to "Wednesday",
    4 to "Thursday",
    5 to "Friday",
    6 to "Saturday",
    7 to "Sunday"
)

val weekdayNameMapCN = mapOf(
    1 to "周一",
    2 to "周二",
    3 to "周三",
    4 to "周四",
    5 to "周五",
    6 to "周六",
    7 to "周日",
)

data class CustomTime(
    val hour: Int,
    val minute: Int
)

// 1–2、3–4、5–6、7–8、9–10 及第 11 节的六个时间轴查看分组。
const val COURSE_TIME_GROUP_MASK = 0b111111

// 具体课程按教室使用错峰时间；周课表默认常规作息，可手动切换查看。
private val staggeredStartTime = listOf(
    CustomTime(8, 10), CustomTime(9, 5), CustomTime(10, 30), CustomTime(11, 25),
    CustomTime(14, 10), CustomTime(15, 5), CustomTime(16, 30), CustomTime(17, 25),
    CustomTime(19, 15), CustomTime(20, 10), CustomTime(21, 5)
)

fun CourseElem.isStaggeredCourse(): Boolean {
    val location = classroom.filterNot { it.isWhitespace() }
    return location.contains("逸夫楼") || location.contains("信息科技大楼") ||
        location.contains("信息科技楼")
}

// 试行表中的“其他教学区域”，不再按旧夏/冬季作息切换。
private val regularStartTime = listOf(
    CustomTime(8, 0), CustomTime(8, 55), CustomTime(10, 20), CustomTime(11, 15),
    CustomTime(14, 0), CustomTime(14, 55), CustomTime(16, 20), CustomTime(17, 15),
    CustomTime(19, 15), CustomTime(20, 10), CustomTime(21, 5)
)

val reverseWeekdayNameMap=  mapOf(
    "Monday" to 1,
    "Tuesday" to 2,
    "Wednesday" to 3,
    "Thursday" to 4,
    "Friday" to 5,
    "Saturday" to 6,
    "Sunday" to 7
)

val courseColor = mapOf(
    CourseState.Before to Color(0xFF2FB8BE),
    CourseState.During to Color(0xFFF8931D),
    CourseState.After to Color(0xFFEF5C66)
)

sealed class CourseState {
    data object Before: CourseState()
    data object During: CourseState()
    data object After: CourseState()
}

fun toCourseMap(courses: List<CourseElem>): Map<String, List<CourseElem>> {
    val map = mutableMapOf<String, List<CourseElem>>()

    weekdayNameMap.forEach { (_, s) ->
        map[s] = listOf()
    }

    for(course in courses) {
        if (!course.isValidForSchedule()) {
            println("Skipped invalid course: $course")
            continue
        }
        val key = course.day
        map[key] = map[key]?.plus(course) ?: listOf(course)
    }

    return map
}

fun getCourseList(courseMap: Map<String, List<CourseElem>>, calendar: CalendarData, currentTime: LocalDateTime): List<CourseElem> {
    val weekNum = getWeekNum(calendar, currentTime)
    val courseList = courseMap[weekdayNameMap[currentTime.dayOfWeek.ordinal + 1]].orEmpty()
    return visibleCoursesForWeek(weekNum, courseList)
}

/** 同一节次只展示一门课程，保留原始数据以便后续恢复或修改。 */
fun visibleCoursesForWeek(week: Long, courses: List<CourseElem>): List<CourseElem> {
    val occupied = BooleanArray(12)
    val visible = mutableListOf<CourseElem>()
    val candidates = courses
        .filter { it.isValidForSchedule() && isInThisWeek(week, it) }
        .sortedWith(compareByDescending<CourseElem> { it.isCustom }.thenBy { it.startTime })

    for (course in candidates) {
        val slots = course.startTime until (course.startTime + course.duration)
        if (slots.any { occupied[it] }) {
            println("Skipped overlapping course in week $week: $course")
            continue
        }
        slots.forEach { occupied[it] = true }
        visible += course
    }
    return visible.sortedBy { it.startTime }
}

/** 课程支持的最大周次，与本地存储中周次展开的范围保持一致。 */
const val MAX_COURSE_WEEK = 60L

/**
 * 将周次表达式解析为起止周次，供自定义课程编辑弹窗回填。
 * 非连续的表达式（例如 "5,6,8"）取最小值和最大值。
 *
 * @param weeks 周次表达式，例如 "5"、"5-10"、"5,6,8"
 * @return 起止周次，无法解析时返回 null
 */
fun parseWeeksRange(weeks: String?): Pair<Long, Long>? {
    val numbers = weeks.orEmpty()
        .replace('，', ',')
        .replace('、', ',')
        .replace('；', ',')
        .replace(';', ',')
        .replace('～', '-')
        .replace('~', '-')
        .split(',')
        .flatMap { part ->
            Regex("\\d+").findAll(part).mapNotNull { it.value.toLongOrNull() }.toList()
        }

    if (numbers.isEmpty()) return null

    return numbers.min() to numbers.max()
}

/**
 * 将起止周次拼接为课程存储使用的周次表达式。
 *
 * @param startWeek 开始周次
 * @param endWeek 结束周次
 */
fun formatWeeksRange(startWeek: Long, endWeek: Long): String =
    if (startWeek >= endWeek) startWeek.toString() else "$startWeek-$endWeek"

fun isInThisWeek(week: Long, course: CourseElem): Boolean {
    if (week < 1 || course.weeks.isBlank()) return false

    val normalized = course.weeks
        .replace('，', ',')
        .replace('、', ',')
        .replace('；', ',')
        .replace(';', ',')
        .replace('～', '-')
        .replace('~', '-')

    return normalized.split(',').any { rawPart ->
        val part = rawPart.trim()
        if (part.isEmpty()) return@any false

        val numbers = Regex("\\d+").findAll(part).mapNotNull { it.value.toLongOrNull() }.toList()
        if (numbers.isEmpty()) return@any false

        val matchesNumber = if (part.contains('-') && numbers.size >= 2) {
            week in minOf(numbers[0], numbers[1])..maxOf(numbers[0], numbers[1])
        } else {
            numbers.any { it == week }
        }
        if (!matchesNumber) return@any false

        when {
            part.contains("单") -> week % 2L == 1L
            part.contains("双") -> week % 2L == 0L
            else -> true
        }
    }
}

fun CourseElem.isValidForSchedule(): Boolean =
    day in weekdayNameMap.values &&
        startTime in 1..11 &&
        duration > 0 &&
        startTime + duration - 1 <= 11 &&
        name.isNotBlank()

/**
 * 获取课程状态
 *
 * @param currentTime 当前时间
 * @param start 起始时间
 * @param duration 课程时长
 */
fun getCourseState(currentTime: LocalDateTime, start: Int, duration: Int): CourseState {
    return courseStateAt(currentTime, coursePeriodTimes(start, duration, getStartTime(currentTime)))
}

fun getCourseColor(state: CourseState): Color {
    return courseColor[state] ?: Color(0xFF7A7A7A)
}

/** 使用课程所在教学楼的实际时间判断今日课程进度。 */
fun getCourseState(currentTime: LocalDateTime, course: CourseElem): CourseState {
    return courseStateAt(currentTime, getCourseTime(course, currentTime))
}

private fun courseStateAt(currentTime: LocalDateTime, times: List<CustomTime>): CourseState {
    val (start, end) = times
    val currentMinutes = currentTime.hour * 60 + currentTime.minute
    return when {
        currentMinutes < start.hour * 60 + start.minute -> CourseState.After
        currentMinutes > end.hour * 60 + end.minute -> CourseState.Before
        else -> CourseState.During
    }
}

/**
 * 获取课程时间 {HH::mm, HH::mm}
 *
 * @param course 课程数据
 * @param currentTime 当前时间
 */
fun getCourseTime(course: CourseElem, currentTime: LocalDateTime): List<CustomTime> {
    return coursePeriodTimes(
        course.startTime, course.duration,
        getStartTime(currentTime.date, staggered = course.isStaggeredCourse())
    )
}

private fun coursePeriodTimes(start: Int, duration: Int, starts: List<CustomTime>): List<CustomTime> {
    // 两种作息都从最后一节的起点取结束时间，完整包含连堂课间的大休息。
    val lastStart = starts[start + duration - 2]
    val endMinutes = lastStart.hour * 60 + lastStart.minute + 45
    return listOf(starts[start - 1], CustomTime(endMinutes / 60, endMinutes % 60))
}

/**
 * 获取课程时间字符串
 *
 * @param customTime 自定义时间
 */
fun customTimeToString(customTime: CustomTime): String {
    return "${customTime.hour.let { if(it >= 10) it else "0${it}" } }:${customTime.minute.let { if(it >= 10) it else "0${it}" }}"
}

/** 试行期间使用通知中的统一常规作息。 */
fun getStartTime(dateTime: LocalDateTime): List<CustomTime> = getStartTime(dateTime.date)

/** 保留日期参数供调用方使用；当前试行表不区分季节。 */
@Suppress("UNUSED_PARAMETER")
fun getStartTime(date: LocalDate, staggered: Boolean = false): List<CustomTime> =
    if (staggered) staggeredStartTime else regularStartTime

fun toWeekdayNameCN(index: Int): String {
    if(index < 1 || index > 7) {
        throw Exception("weekday out of index")
    }

    return weekdayNameMapCN[index]!!
}
