package com.sky31.gongmultiplatform.network

import com.sky31.gongmultiplatform.network.dto.AuthDto
import com.sky31.gongmultiplatform.network.repository.AuthRepositoryImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.authMsgMap
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginTest {

    @Test
    fun loginSuccess200() = runTest {
        val client = TestClientProvider.createClient(MockEngine { request ->
            if(request.url.encodedPath == "/login") {
                respond(
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    content = """
                        {
                            "access_token": "123456789",
                            "expires_in": "264000000",
                            "token_type": "Bearer"
                        }
                    """.trimIndent()
                )
            } else {
                respondBadRequest()
            }
        })

        val repository = AuthRepositoryImpl(client)
        val result = repository.login("2021", "666666")

        assertEquals(
            NetworkResult.Success(
                code = HttpStatusCode.OK,
                data = AuthDto("123456789", "264000000", "Bearer")
            ),
            result
        )
    }

    @Test
    fun loginUnauthorized401() = runTest {
        val client = TestClientProvider.createClient(MockEngine { request ->
            if(request.url.encodedPath == "/login") {
                respond(
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    content = ""
                )
            } else {
                respondBadRequest()
            }
        })

        val repository = AuthRepositoryImpl(client)
        val result = repository.login("2021", "666666")

        assertEquals(
            NetworkResult.Error(
                code = HttpStatusCode.Unauthorized,
                message = authMsgMap[HttpStatusCode.Unauthorized]!!
            ),
            result
        )
    }

    @Test
    fun loginConflict409() = runTest {
        val client = TestClientProvider.createClient(MockEngine { request ->
            if(request.url.encodedPath == "/login") {
                respond(
                    status = HttpStatusCode.Conflict,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    content = ""
                )
            } else {
                respondBadRequest()
            }
        })

        val repository = AuthRepositoryImpl(client)
        val result = repository.login("2021", "666666")

        assertEquals(
            NetworkResult.Error(
                code = HttpStatusCode.Conflict,
                message = authMsgMap[HttpStatusCode.Conflict]!!
            ),
            result
        )
    }

    @Test
    fun loginServiceUnavailable503() = runTest {
        val client = TestClientProvider.createClient(MockEngine { request ->
            if(request.url.encodedPath == "/login") {
                respond(
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    content = ""
                )
            } else {
                respondBadRequest()
            }
        })

        val repository = AuthRepositoryImpl(client)
        val result = repository.login("2021", "666666")

        assertEquals(
            NetworkResult.Error(
                code = HttpStatusCode.ServiceUnavailable,
                message = authMsgMap[HttpStatusCode.ServiceUnavailable]!!
            ),
            result
        )
    }

    @Test
    fun loginGatewayTimeout504() = runTest {
        val client = TestClientProvider.createClient(MockEngine { request ->
            if(request.url.encodedPath == "/login") {
                respond(
                    status = HttpStatusCode.GatewayTimeout,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    content = ""
                )
            } else {
                respondBadRequest()
            }
        })

        val repository = AuthRepositoryImpl(client)
        val result = repository.login("2021", "666666")

        assertEquals(
            NetworkResult.Error(
                code = HttpStatusCode.GatewayTimeout,
                message = authMsgMap[HttpStatusCode.GatewayTimeout]!!
            ),
            result
        )
    }

}