package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.model.CourseElem

data class CourseSlot(val startTime: Int, val duration: Int, val courses: List<CourseElem>)

/** 课表保留普通课与实验课的重叠关系，自定义课程仍优先。 */
fun timetableCoursesForWeek(week: Long, courses: List<CourseElem>): List<CourseElem> {
    val ordinary = visibleCoursesForWeek(week, courses.filterNot { it.isExperiment })
    val custom = ordinary.filter { it.isCustom }
    val labs = courses.filter { lab -> lab.isExperiment && lab.isValidForSchedule() && isInThisWeek(week, lab) &&
        custom.none { it.startTime < lab.startTime + lab.duration && lab.startTime < it.startTime + it.duration } }
    return (ordinary + labs).distinct().sortedBy { it.startTime }
}

/** 按实际重叠节次分段，部分重叠时不移动实验课的真实位置。 */
fun courseSlots(courses: List<CourseElem>, start: Int, end: Int): List<CourseSlot> {
    val slots = mutableListOf<CourseSlot>()
    for (period in start..end) {
        val active = courses.filter { period >= it.startTime && period < it.startTime + it.duration }
            .sortedWith(compareBy<CourseElem> { it.isExperiment }.thenBy { it.startTime }.thenBy { it.name })
        if (active.isEmpty()) continue
        val last = slots.lastOrNull()
        if (last != null && last.startTime + last.duration == period && last.courses == active) {
            slots[slots.lastIndex] = last.copy(duration = last.duration + 1)
        } else slots += CourseSlot(period, 1, active)
    }
    return slots
}
