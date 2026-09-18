package com.sky31.gongmultiplatform.network

import com.sky31.gongmultiplatform.SystemGlobalConfig
import com.sky31.gongmultiplatform.model.bearerTokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * client provider
 */
object HttpClientProvider {
    val client: HttpClient =
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }

            install(Resources)

            install(Auth) {
                bearer {
                    loadTokens {
                        bearerTokenStorage
                            .lastOrNull()
                            ?.takeIf { it.accessToken.isNotBlank() }
                    }

                    sendWithoutRequest { request ->
                        request.url.encodedPath != "/login"
                    }
                }
            }

            defaultRequest {
                val apiEndpoint = SystemGlobalConfig.apiEndpoint
                url {
                    protocol = apiEndpoint.protocol
                    host = apiEndpoint.host
                    port = apiEndpoint.port
                }
                contentType(ContentType.Application.Json)
            }
        }
}
