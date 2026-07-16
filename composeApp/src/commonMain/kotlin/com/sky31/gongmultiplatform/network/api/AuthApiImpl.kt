package com.sky31.gongmultiplatform.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters

class AuthApiImpl(
    private val client: HttpClient
): AuthApi {

    override suspend fun login(username: String, password: String): HttpResponse =
        client.submitForm(
            url = "/login",
            formParameters = Parameters.build {
                append("username", username)
                append("password", password)
            }
        )
}