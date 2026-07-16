package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.network.api.CourseApiImpl
import com.sky31.gongmultiplatform.network.dto.CourseDto
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.safeApiCall
import io.ktor.client.HttpClient

class CourseRepositoryImpl(
    client: HttpClient
): CourseRepository {
    private val api = CourseApiImpl(client)

    override suspend fun getCourses(): NetworkResult<List<CourseElem>> {
        val result = safeApiCall<CourseDto> { api.getCourses() }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data.courses
                )

            is NetworkResult.Error -> result
        }
    }
}