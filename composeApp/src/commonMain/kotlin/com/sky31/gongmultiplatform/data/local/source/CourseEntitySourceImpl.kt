package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.dao.CourseDao
import com.sky31.gongmultiplatform.data.local.domain.CourseEntity

class CourseEntitySourceImpl(
    private val dao: CourseDao
): CourseEntitySource {

    override suspend fun insertCourseEntity(entity: CourseEntity) {
        dao.insertCourseEntity(entity)
    }

    override suspend fun updateCourseEntity(entity: CourseEntity) {
        dao.updateCourseEntity(entity)
    }

    override suspend fun getCourseEntity(): CourseEntity? {
        return dao.getCourseEntity()
    }

    override suspend fun deleteAllCourseEntities() {
        dao.clearAll()
    }
}