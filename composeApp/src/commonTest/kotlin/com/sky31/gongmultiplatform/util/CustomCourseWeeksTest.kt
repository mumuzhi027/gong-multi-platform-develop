package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.model.CourseElem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomCourseWeeksTest {

    @Test
    fun singleWeekKeepsStoringOneNumber() {
        assertEquals("5", formatWeeksRange(5, 5))
    }

    @Test
    fun multiWeekStoresARange() {
        assertEquals("5-10", formatWeeksRange(5, 10))
    }

    @Test
    fun parsesStoredWeeksBackToTheEditorRange() {
        assertEquals(5L to 5L, parseWeeksRange("5"))
        assertEquals(5L to 10L, parseWeeksRange("5-10"))
        assertEquals(5L to 10L, parseWeeksRange("5，6，8-10"))
        assertNull(parseWeeksRange(""))
        assertNull(parseWeeksRange(null))
    }

    @Test
    fun multiWeekCustomCourseIsVisibleInEveryWeekOfTheRange() {
        val course = customCourse(weeks = formatWeeksRange(5, 10))

        (5L..10L).forEach { week ->
            assertTrue(isInThisWeek(week, course), "第${week}周应显示该课程")
        }
        assertFalse(isInThisWeek(4, course))
        assertFalse(isInThisWeek(11, course))

        assertEquals(listOf(course), visibleCoursesForWeek(7, listOf(course)))
        assertTrue(visibleCoursesForWeek(4, listOf(course)).isEmpty())
    }

    private fun customCourse(weeks: String) = CourseElem(
        name = "自习",
        teacher = "测试教师",
        classroom = "测试教室",
        weeks = weeks,
        startTime = 1,
        duration = 2,
        day = "Monday",
        isCustom = true
    )
}
