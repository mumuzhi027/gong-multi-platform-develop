package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.GongAiChatRequest
import com.sky31.gongmultiplatform.model.GongAiChatResponse
import com.sky31.gongmultiplatform.model.GongAiConfirmResponse
import com.sky31.gongmultiplatform.model.GongAiEnvelope
import com.sky31.gongmultiplatform.network.api.GongAiApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.http.isSuccess

class GongAiRepository(client: HttpClient) {
    private val api = GongAiApi(client)

    suspend fun chat(request: GongAiChatRequest): Result<GongAiChatResponse> = runCatching {
        val response = api.chat(request)
        val envelope = response.body<GongAiEnvelope<GongAiChatResponse>>()
        envelope.data ?: error(envelope.message.ifBlank { "拱拱AI暂时无法响应" })
    }

    suspend fun confirm(actionId: String): Result<GongAiConfirmResponse> = runCatching {
        val response = api.confirm(actionId)
        val envelope = response.body<GongAiEnvelope<GongAiConfirmResponse>>()
        envelope.data ?: error(envelope.message.ifBlank { "确认操作失败" })
    }

    suspend fun cancel(actionId: String): Result<Unit> = runCatching {
        val response = api.cancel(actionId)
        if (!response.status.isSuccess()) error("取消操作失败")
    }

    suspend fun clearConversation(conversationId: String): Result<Unit> = runCatching {
        val response = api.clearConversation(conversationId)
        if (!response.status.isSuccess()) error("清空对话失败")
    }
}

