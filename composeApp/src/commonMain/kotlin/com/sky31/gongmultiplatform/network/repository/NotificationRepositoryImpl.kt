package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.network.api.NotificationApiImpl
import com.sky31.gongmultiplatform.network.dto.UpdateDto
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.getMsgMap
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

class NotificationRepositoryImpl: NotificationRepository{
    private val api = NotificationApiImpl()

    override suspend fun getUpdateNotification(): NetworkResult<UpdateDto> {
        try {
            val response = api.getUpdateNotification()
            val code = response.status

            return when(code) {
                HttpStatusCode.OK -> {
                    val body = response.body<UpdateDto>()
                    return NetworkResult.Success(
                        code = code,
                        data = body
                    )
                }

                HttpStatusCode.Conflict,
                HttpStatusCode.ServiceUnavailable,
                HttpStatusCode.GatewayTimeout,
                HttpStatusCode.Unauthorized ->
                    NetworkResult.Error(
                        code = code,
                        message = getMsgMap[code] ?: "Unknown error",
                        exception = Exception("")
                    )

                else -> NetworkResult.Error(
                    code = code,
                    message = "未知错误",
                    exception = Exception("")
                )
            }

        } catch (e: Exception) {
            return NetworkResult.Error(
                message = e.message ?: "Unknown error",
                exception = e
            )
        }
    }
}