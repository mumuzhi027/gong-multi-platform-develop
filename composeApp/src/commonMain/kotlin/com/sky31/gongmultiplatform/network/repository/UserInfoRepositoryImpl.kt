package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.UserInfo
import com.sky31.gongmultiplatform.network.api.UserInfoApiImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.safeApiCall
import io.ktor.client.HttpClient

class UserInfoRepositoryImpl(
    client: HttpClient
): UserInfoRepository {
    private val api = UserInfoApiImpl(client)

    override suspend fun getUserInfo(): NetworkResult<UserInfo> {
        val result = safeApiCall<UserInfo> { api.getUserInfo() }

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