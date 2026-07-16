package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.CourseDao
import com.sky31.gongmultiplatform.data.local.domain.CourseEntity
import com.sky31.gongmultiplatform.data.local.source.CourseEntitySourceImpl
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.decodeOrNull
import kotlinx.serialization.json.Json

class CourseDataRepositoryImpl(
    dao: CourseDao
): CourseDataRepository {

    private val source = CourseEntitySourceImpl(dao)

    override suspend fun insertCourseMap(courses: Map<String, List<CourseElem>>) {
        source.insertCourseEntity(CourseEntity(
            courses = Json.encodeToString(courses)
        ))
    }

    override suspend fun updateCourseMap(courses: Map<String, List<CourseElem>>) {
        source.updateCourseEntity(CourseEntity(
            courses = Json.encodeToString(courses)
        ))
    }

    override suspend fun getCourseMap(): Map<String, List<CourseElem>>? {
        val entity = source.getCourseEntity()

        return decodeOrNull(
            raw = entity?.courses,
            label = "course map"
        )
    }

    override suspend fun deleteAllCourses() {
        source.deleteAllCourseEntities()
    }
}
