package com.sky31.gongmultiplatform.util

enum class CourseTimeMode(val label: String) {
    MIXED("混合"), STAGGERED("错峰"), REGULAR("常规");

    fun next(): CourseTimeMode = when (this) {
        MIXED -> STAGGERED
        STAGGERED -> REGULAR
        REGULAR -> MIXED
    }

    fun courseAlpha(staggered: Boolean): Float = when {
        this == STAGGERED && !staggered -> 0.25f
        this == REGULAR && staggered -> 0.25f
        else -> 1f
    }
}

/** 时间轴分组独立可选；点击任意分组都恢复混合课程显示。 */
data class CourseTimeDisplay(
    val staggeredGroups: Int = 0,
    val mode: CourseTimeMode = CourseTimeMode.MIXED
) {
    fun toggleGroup(group: Int): CourseTimeDisplay =
        if (group in 0..5) copy(staggeredGroups = staggeredGroups xor (1 shl group), mode = CourseTimeMode.MIXED)
        else this

    fun cycle(): CourseTimeDisplay {
        val next = mode.next()
        return CourseTimeDisplay(if (next == CourseTimeMode.STAGGERED) COURSE_TIME_GROUP_MASK else 0, next)
    }
}
