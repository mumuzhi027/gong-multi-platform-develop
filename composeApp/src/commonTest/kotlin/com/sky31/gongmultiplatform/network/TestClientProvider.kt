package com.sky31.gongmultiplatform.network

import com.sky31.gongmultiplatform.SystemGlobalConfig
import com.sky31.gongmultiplatform.model.TokenData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object TestClientProvider {

    fun createClient(
        engine: MockEngine
    ): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        install(Resources)

        install(Auth) {
            bearer {
                loadTokens {
                    TokenData.token
                        ?.takeIf { it.isNotBlank() }
                        ?.let { BearerTokens(it, "") }
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
        }
    }

    fun createIntegrationTest(): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        install(Resources)

        install(Auth) {
            bearer {
                loadTokens {
                    TokenData.token
                        ?.takeIf { it.isNotBlank() }
                        ?.let { BearerTokens(it, "") }
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
        }
    }
}
