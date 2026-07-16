package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.SystemGlobalConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class NotificationApiImpl: NotificationApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        defaultRequest {
            val updateEndpoint = SystemGlobalConfig.updateEndpoint
            url {
                protocol = updateEndpoint.protocol
                host = updateEndpoint.host
                port = updateEndpoint.port
            }
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun getUpdateNotification(): HttpResponse {
        return client.get("/version.json")
    }
}
