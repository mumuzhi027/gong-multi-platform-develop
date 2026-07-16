package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.model.CourseElem

interface CourseDataRepository {

    suspend fun insertCourseMap(courses: Map<String, List<CourseElem>>)

    suspend fun updateCourseMap(courses: Map<String, List<CourseElem>>)

    suspend fun getCourseMap(): Map<String, List<CourseElem>>?

    suspend fun deleteAllCourses()

}