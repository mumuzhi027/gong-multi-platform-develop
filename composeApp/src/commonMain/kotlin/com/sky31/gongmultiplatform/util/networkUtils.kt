package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.SystemGlobalConfig
import com.sky31.gongmultiplatform.network.response.ApiResponse
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

val authMsgMap = mapOf(
    HttpStatusCode.Unauthorized to "账号或密码错误",
    HttpStatusCode.Conflict to "账号未初始化",
    HttpStatusCode.ServiceUnavailable to "教务系统暂时不可用",
    HttpStatusCode.GatewayTimeout to "请求超时，请稍后重试",
    HttpStatusCode.BadGateway to "网关错误，请稍后重试",
)

val getMsgMap = mapOf(
    HttpStatusCode.Unauthorized to "登录状态已失效，请重新登录",
    HttpStatusCode.NonAuthoritativeInformation to "资源已过期，请刷新后重试",
    HttpStatusCode.NotFound to "请求的资源不存在",
    HttpStatusCode.Locked to "账号已被锁定",
    HttpStatusCode.ServiceUnavailable to "教务系统暂时不可用",
    HttpStatusCode.GatewayTimeout to "请求超时，请稍后重试",
)

suspend inline fun <reified T> safeApiCall(
    apiCall: suspend () -> HttpResponse
): NetworkResult<T> {
    try {
        val response = apiCall()
        val code = response.status

        println(code.toString())
        return when(code) {
            HttpStatusCode.OK -> {
                val body = response.body<ApiResponse<T>>()
                NetworkResult.Success(
                    code = code,
                    data = body.data
                )
            }

            HttpStatusCode.Conflict,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout,
            HttpStatusCode.Unauthorized,
            HttpStatusCode.NonAuthoritativeInformation,
            HttpStatusCode.NotFound,
            HttpStatusCode.Locked -> {
                NetworkResult.Error(
                    code = code,
                    message = getMsgMap[code] ?: "请求失败（HTTP ${code.value}）",
                    exception = Exception("")
                )
            }

            else -> NetworkResult.Error(
                code = code,
                message = "请求失败（HTTP ${code.value}）",
                exception = Exception("")
            )
        }

    } catch (e: Exception) {
        return NetworkResult.Error(
            message = e.message ?: "网络请求失败，请稍后重试",
            exception = e
        )
    }
}

fun codeToDataState(code: HttpStatusCode?): DataState {
    return when(code) {
        HttpStatusCode.Unauthorized -> DataState.Unauthorized
        HttpStatusCode.NonAuthoritativeInformation -> DataState.Expired
        HttpStatusCode.Locked -> DataState.Error("账号已被锁定")
        HttpStatusCode.ServiceUnavailable -> DataState.Error("教务系统暂时不可用")
        HttpStatusCode.GatewayTimeout -> DataState.Error("请求超时，请稍后重试")
        else -> DataState.Error("未知错误")
    }
}

suspend fun safeApiCallsSequential(
    calls: List<suspend () -> DataState>
): List<DataState> = coroutineScope {
    val deferredResults = calls.map { call ->
        async {
            var count = 0
            while (count < SystemGlobalConfig.MAX_RETRY_TIMES) {
                val dataState = call()
                when (dataState) {
                    is DataState.Expired -> {
                        count++
                        delay(SystemGlobalConfig.RETRY_INTERVAL)
                    }

                    is DataState.Unauthorized -> {
                        TokenState.expired()
                        return@async dataState
                    }

                    else -> {
                        return@async dataState
                    }
                }
            }

            DataState.Error("请求超时，请稍后重试")
        }
    }

    deferredResults.awaitAll()
}

suspend fun retryExpiredDataState(
    maxRetryTimes: Int,
    retryIntervalMillis: Long,
    finalErrorMessage: String,
    call: suspend () -> DataState
): DataState {
    repeat(maxRetryTimes) {
        when (val dataState = call()) {
            is DataState.Expired -> {
                delay(retryIntervalMillis)
            }

            is DataState.Unauthorized -> {
                TokenState.expired()
                return dataState
            }

            else -> {
                return dataState
            }
        }
    }

    return DataState.Error(finalErrorMessage)
}

fun checkResults(results: List<DataState>): Boolean {
    for(result in results) {
        if(result != DataState.Newest) return false
    }

    return true
}

object TokenState {
    private val _isExpired = MutableStateFlow(false)
    val isExpired = _isExpired.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode = _isOfflineMode.asStateFlow()

    fun expired() {
        if (!_isOfflineMode.value) {
            _isExpired.value = true
        }
    }

    fun enterOfflineMode() {
        _isOfflineMode.value = true
        _isExpired.value = false
    }

    fun requestVerificationIfOffline(): Boolean {
        if (!_isOfflineMode.value) return false

        _isExpired.value = true
        return true
    }

    fun refreshed() {
        _isOfflineMode.value = false
        _isExpired.value = false
    }
}
