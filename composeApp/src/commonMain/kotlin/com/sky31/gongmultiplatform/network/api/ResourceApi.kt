package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.flow.Flow

interface ResourceApi {
    suspend fun getApkZip(url: String, handler: suspend (response: HttpResponse) -> Unit): Flow<Int>
}