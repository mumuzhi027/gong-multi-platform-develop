package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface UserInfoApi {
    suspend fun getUserInfo(): HttpResponse
}