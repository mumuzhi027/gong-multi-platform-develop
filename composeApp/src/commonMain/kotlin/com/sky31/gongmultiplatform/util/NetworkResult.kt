package com.sky31.gongmultiplatform.util

import io.ktor.http.HttpStatusCode

sealed class NetworkResult<out T> {
    data class Success<out T>(
        val data: T,
        val code: HttpStatusCode = HttpStatusCode.OK
    ): NetworkResult<T>()

    data class Error(
        val code: HttpStatusCode? = null,
        val message: String,
        val exception: Throwable? = null
    ): NetworkResult<Nothing>()
}