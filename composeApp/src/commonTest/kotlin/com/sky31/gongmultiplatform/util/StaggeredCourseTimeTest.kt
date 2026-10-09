package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.model.CourseElem
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaggeredCourseTimeTest {
    private fun course(room: String = "逸夫楼101", start: Int = 1, duration: Int = 2) =
        CourseElem("测试课程", "教师", room, "1-20", start, duration, "Monday")

    @Test
    fun recognizesBothBuildingsAndLeavesOtherLocationsAlone() {
        listOf("逸夫楼101", "信息科技大楼A302", "信息科技楼302", " 信息科技 大楼 302 ").forEach {
            assertTrue(course(it).isStaggeredCourse())
        }
        listOf("", "第三教学楼101", "信息楼101", "逸夫图书馆").forEach {
            assertFalse(course(it).isStaggeredCourse())
        }
    }

    @Test
    fun followsAllElevenPeriodsInTheSuppliedTimetable() {
        val times = listOf(
            "08:10" to "08:55", "09:05" to "09:50", "10:30" to "11:15",
            "11:25" to "12:10", "14:10" to "14:55", "15:05" to "15:50",
            "16:30" to "17:15", "17:25" to "18:10", "19:15" to "20:00",
            "20:10" to "20:55", "21:05" to "21:50"
        )
        listOf("逸夫楼101", "信息科技大楼302").forEach { room ->
            listOf(1, 7, 10).forEach { month ->
                times.forEachIndexed { index, (start, end) ->
                    assertEquals(listOf(start, end), getCourseTime(
                        course(room, index + 1, 1), LocalDateTime(2026, month, 7, 8, 0)
                    ).map(::customTimeToString))
                }
            }
        }
    }

    @Test
    fun includesLongBreaksWhenDisplayingConsecutivePeriods() {
        val now = LocalDateTime(2026, 10, 7, 8, 0)
        assertEquals(listOf("08:10", "12:10"), getCourseTime(course(duration = 4), now).map(::customTimeToString))
        assertEquals(listOf("15:05", "17:15"), getCourseTime(course(start = 6), now).map(::customTimeToString))
    }

    @Test
    fun progressUsesStaggeredStartAndEndBoundaries() {
        val lesson = course()
        assertEquals(CourseState.After, getCourseState(LocalDateTime(2026, 10, 7, 8, 9), lesson))
        assertEquals(CourseState.During, getCourseState(LocalDateTime(2026, 10, 7, 8, 10), lesson))
        assertEquals(CourseState.During, getCourseState(LocalDateTime(2026, 10, 7, 9, 50), lesson))
        assertEquals(CourseState.Before, getCourseState(LocalDateTime(2026, 10, 7, 9, 51), lesson))
    }

    @Test
    fun ordinaryCoursesAndWeeklyAxisFollowUpdatedTrialTimes() {
        val times = listOf(
            "08:00" to "08:45", "08:55" to "09:40", "10:20" to "11:05",
            "11:15" to "12:00", "14:00" to "14:45", "14:55" to "15:40",
            "16:20" to "17:05", "17:15" to "18:00", "19:15" to "20:00",
            "20:10" to "20:55", "21:05" to "21:50"
        )
        listOf(1, 7, 10).forEach { month ->
            val now = LocalDateTime(2026, month, 7, 8, 0)
            times.forEachIndexed { index, (start, end) ->
                assertEquals(listOf(start, end), getCourseTime(
                    course("第三教学楼101", index + 1, 1), now
                ).map(::customTimeToString))
            }
            assertEquals(times.map { it.first }, getStartTime(now).map(::customTimeToString))
            assertEquals(times.map { it.first }, getStartTime(now.date).map(::customTimeToString))
            assertEquals(listOf("14:00", "15:40"), getCourseTime(
                course("第三教学楼101", 5), now
            ).map(::customTimeToString))
        }
    }

    @Test
    fun ordinaryConsecutiveCoursesIncludeTheLongBreaks() {
        val now = LocalDateTime(2026, 10, 7, 8, 0)
        assertEquals(listOf("08:00", "12:00"), getCourseTime(
            course("第三教学楼101", duration = 4), now
        ).map(::customTimeToString))
        assertEquals(listOf("14:55", "17:05"), getCourseTime(
            course("第三教学楼101", start = 6, duration = 2), now
        ).map(::customTimeToString))
    }

    @Test
    fun ordinaryProgressUsesTheSameEndTimeAsHomeAndWidget() {
        val lesson = course("第三教学楼101", 3, 2)
        listOf(
            Triple(10, 19, CourseState.After), Triple(10, 20, CourseState.During),
            Triple(12, 0, CourseState.During), Triple(12, 1, CourseState.Before)
        ).forEach { (hour, minute, state) ->
            val now = LocalDateTime(2026, 10, 7, hour, minute)
            assertEquals(state, getCourseState(now, lesson))
            assertEquals(state, getCourseState(now, lesson.startTime, lesson.duration))
        }
    }
}
