package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.RankData
import com.sky31.gongmultiplatform.model.ScoreData
import com.sky31.gongmultiplatform.network.api.AcademicApiImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.safeApiCall
import io.ktor.client.HttpClient

class AcademicRepositoryImpl(
    client: HttpClient
): AcademicRepository {
    private val api = AcademicApiImpl(client)

    override suspend fun getMajorAcademicInfo(forceRefresh: Boolean): NetworkResult<ScoreData> {
        val result = safeApiCall<ScoreData> { api.getMajorAcademicInfo(forceRefresh) }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data
                )

            is NetworkResult.Error -> result
        }
    }

    override suspend fun getMinorAcademicInfo(forceRefresh: Boolean): NetworkResult<ScoreData> {
        val result = safeApiCall<ScoreData> { api.getMinorAcademicInfo(forceRefresh) }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data
                )

            is NetworkResult.Error -> result
        }
    }

    override suspend fun getTotalRank(forceRefresh: Boolean): NetworkResult<RankData> {
        val result = safeApiCall<RankData> { api.getTotalRank(forceRefresh) }

        return when(result) {
            is NetworkResult.Success ->
                NetworkResult.Success(
                    code = result.code,
                    data = result.data
                )

            is NetworkResult.Error -> result
        }
    }

    override suspend fun getCompulsoryRank(forceRefresh: Boolean): NetworkResult<RankData> {
        val result = safeApiCall<RankData> { api.getCompulsoryRank(forceRefresh) }

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
