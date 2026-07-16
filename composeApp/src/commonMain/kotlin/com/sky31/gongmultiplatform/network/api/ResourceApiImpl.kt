package com.sky31.gongmultiplatform.network.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ResourceApiImpl(
    private val client: HttpClient
): ResourceApi {

    override suspend fun getApkZip(url: String, handler: suspend (response: HttpResponse) -> Unit): Flow<Int> =
        callbackFlow {
            client.prepareGet(url) {
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        val percent = (bytesSentTotal * 100 / contentLength).toInt()
                        println(percent)

                        trySend(percent).isSuccess
                    }
                }
            }.execute { response ->
                handler(response)
                close()
            }

            awaitClose {  }
        }
}