package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.model.GongAiChatRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class GongAiApi(private val client: HttpClient) {
    suspend fun chat(request: GongAiChatRequest): HttpResponse =
        client.post("/api/gong-ai/chat") { setBody(request) }

    suspend fun confirm(actionId: String): HttpResponse =
        client.post("/api/gong-ai/actions/$actionId/confirm")

    suspend fun cancel(actionId: String): HttpResponse =
        client.post("/api/gong-ai/actions/$actionId/cancel")

    suspend fun clearConversation(conversationId: String): HttpResponse =
        client.delete("/api/gong-ai/conversations/$conversationId")
}

