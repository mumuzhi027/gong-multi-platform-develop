package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.network.api.ExamApiImpl
import com.sky31.gongmultiplatform.network.dto.ExamDto
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.safeApiCall
import io.ktor.client.HttpClient

class ExamRepositoryImpl(
    client: HttpClient
): ExamRepository {
    private val api = ExamApiImpl(client)

    override suspend fun getExamList(): NetworkResult<List<ExamElem>> {
        val result = safeApiCall<ExamDto> { api.getExamList() }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data.exams
                )

            is NetworkResult.Error -> result
        }
    }
}