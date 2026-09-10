package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.model.CourseElem

interface CourseDataRepository {

    suspend fun insertCourseMap(courses: Map<String, List<CourseElem>>)

    suspend fun updateCourseMap(courses: Map<String, List<CourseElem>>)

    suspend fun getCourseMap(): Map<String, List<CourseElem>>?

    suspend fun replaceRemoteCoursesPreservingCustom(courses: Map<String, List<CourseElem>>)

    suspend fun deleteCourse(course: CourseElem)

    suspend fun restoreRemoteCoursesForWeek(week: Long): Int

    suspend fun deleteAllCourses()

}
