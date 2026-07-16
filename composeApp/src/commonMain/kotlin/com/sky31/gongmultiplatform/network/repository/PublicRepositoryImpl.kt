package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.CalendarData
import com.sky31.gongmultiplatform.model.ClassroomData
import com.sky31.gongmultiplatform.network.api.PublicApiImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.safeApiCall
import io.ktor.client.HttpClient

class PublicRepositoryImpl(
    client: HttpClient
): PublicRepository {
    private val api = PublicApiImpl(client)

    override suspend fun getTodayClassroom(): NetworkResult<ClassroomData> {
        val result = safeApiCall<ClassroomData> { api.getTodayClassroom() }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data
                )

            is NetworkResult.Error -> result
        }
    }

    override suspend fun getTomorrowClassroom(): NetworkResult<ClassroomData> {
        val result = safeApiCall<ClassroomData> { api.getTomorrowClassroom() }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data
                )

            is NetworkResult.Error -> result
        }
    }

    override suspend fun getCalendar(): NetworkResult<CalendarData> {
        val result = safeApiCall<CalendarData> { api.getCalendar() }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data
                )

            is NetworkResult.Error -> result
        }
    }
}