package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.domain.CourseEntity

interface CourseEntitySource {

    suspend fun insertCourseEntity(entity: CourseEntity)

    suspend fun updateCourseEntity(entity: CourseEntity)

    suspend fun getCourseEntity(): CourseEntity?

    suspend fun deleteAllCourseEntities()
}