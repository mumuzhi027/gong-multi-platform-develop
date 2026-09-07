package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface ActivityApi {
    suspend fun getActivities(limit: Int? = null): HttpResponse
    suspend fun getActivityFilters(): HttpResponse
}
