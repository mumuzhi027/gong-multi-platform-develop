package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.network.resources.UserInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse

class UserInfoApiImpl(
    private val client: HttpClient
): UserInfoApi {
    override suspend fun getUserInfo(): HttpResponse =
        client.get(UserInfo())
}