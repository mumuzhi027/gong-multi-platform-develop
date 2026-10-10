package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.model.CourseElem
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CourseSlotsTest {
    private fun lesson(start: Int = 7, duration: Int = 2, lab: Boolean = false, weeks: String = "10") =
        CourseElem("数据结构与算法", "老师", "工科楼南楼607", weeks, start, duration, "Tuesday",
            isExperiment = lab, experimentId = if (lab) "lab-1" else "", experimentProject = if (lab) "二叉树寻找" else "")

    @Test
    fun retainsOrdinaryFirstAndLabOnlyInItsWeek() {
        val ordinary = lesson(weeks = "1-20")
        val lab = lesson(lab = true)
        val courses = timetableCoursesForWeek(10, listOf(lab, ordinary))
        val slots = courseSlots(courses, 5, 8)
        assertEquals(1, slots.size)
        assertEquals(listOf(ordinary, lab), slots.single().courses)
        assertEquals(listOf(ordinary), timetableCoursesForWeek(9, listOf(lab, ordinary)))
        assertEquals(listOf(lab), visibleCoursesForWeek(10, listOf(ordinary, lab)))
    }

    @Test
    fun partialOverlapKeepsTheRealLabPosition() {
        val ordinary = lesson(start = 5, duration = 4)
        val lab = lesson(lab = true)
        val slots = courseSlots(listOf(ordinary, lab), 5, 8)
        assertEquals(listOf(5, 7), slots.map { it.startTime })
        assertEquals(listOf(2, 2), slots.map { it.duration })
        assertEquals(listOf(ordinary), slots.first().courses)
        assertEquals(listOf(ordinary, lab), slots.last().courses)
    }

    @Test
    fun customCourseRemainsHigherPriority() {
        val custom = lesson().copy(isCustom = true)
        assertEquals(listOf(custom), timetableCoursesForWeek(10, listOf(lesson(), lesson(lab = true), custom)))
    }

    @Test
    fun twelfthPeriodKeepsPeriodLabelsAndMetadataRoundTrips() {
        val lab = lesson(start = 9, duration = 4, lab = true)
        assertTrue(lab.isValidForSchedule())
        assertFalse(lab.copy(isExperiment = false).isValidForSchedule())
        assertEquals(4, courseSlots(listOf(lab), 9, 12).single().duration)
        assertEquals(listOf("19:15", "第12节"), getCourseTime(lab, LocalDateTime(2026, 10, 10, 12, 0)).map(::customTimeToString))
        assertEquals(lab, Json.decodeFromString<CourseElem>(Json.encodeToString(CourseElem.serializer(), lab)))
        val old = """{"name":"课程","teacher":"老师","classroom":"教室","weeks":"10","start_time":1,"duration":2,"day":"Monday"}"""
        assertFalse(Json.decodeFromString<CourseElem>(old).isExperiment)
    }
}
