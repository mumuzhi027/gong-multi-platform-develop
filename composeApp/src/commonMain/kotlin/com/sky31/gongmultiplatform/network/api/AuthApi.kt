package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface AuthApi {
    suspend fun login(username: String, password: String): HttpResponse
}