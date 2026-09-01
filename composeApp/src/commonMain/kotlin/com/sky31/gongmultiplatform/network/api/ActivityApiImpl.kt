package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.network.resources.Activities
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse

class ActivityApiImpl(
    private val client: HttpClient
) : ActivityApi {
    override suspend fun getActivities(limit: Int?): HttpResponse =
        client.get(Activities(limit = limit))
}
