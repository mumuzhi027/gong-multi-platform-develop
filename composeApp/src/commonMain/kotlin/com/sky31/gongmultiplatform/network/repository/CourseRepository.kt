package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.NetworkResult

interface CourseRepository {
    suspend fun getCourses(): NetworkResult<List<CourseElem>>
}