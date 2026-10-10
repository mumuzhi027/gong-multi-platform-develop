package com.sky31.gongmultiplatform.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CourseTimeDisplayTest {
    @Test
    fun cyclesMixedStaggeredRegularAndBack() {
        var state = CourseTimeDisplay()
        repeat(2) {
            assertEquals(CourseTimeMode.MIXED, state.mode)
            assertEquals(1f, state.mode.courseAlpha(false))
            assertEquals(1f, state.mode.courseAlpha(true))
            state = state.cycle()
            assertEquals(COURSE_TIME_GROUP_MASK, state.staggeredGroups)
            assertEquals(0.25f, state.mode.courseAlpha(false))
            assertEquals(1f, state.mode.courseAlpha(true))
            state = state.cycle()
            assertEquals(0, state.staggeredGroups)
            assertEquals(1f, state.mode.courseAlpha(false))
            assertEquals(0.25f, state.mode.courseAlpha(true))
            state = state.cycle()
        }
    }

    @Test
    fun everyTimeGroupRestoresMixedAndRestartsTheCycle() {
        for (mode in CourseTimeMode.entries) {
            for (mask in listOf(0, 1, 31, COURSE_TIME_GROUP_MASK)) {
                for (group in 0..5) {
                    val mixed = CourseTimeDisplay(mask, mode).toggleGroup(group)
                    assertEquals(mask xor (1 shl group), mixed.staggeredGroups)
                    assertEquals(CourseTimeMode.MIXED, mixed.mode)
                    assertEquals(1f, mixed.mode.courseAlpha(false))
                    assertEquals(1f, mixed.mode.courseAlpha(true))
                    assertEquals(CourseTimeDisplay(COURSE_TIME_GROUP_MASK, CourseTimeMode.STAGGERED), mixed.cycle())
                }
            }
        }
    }
}
