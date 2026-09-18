package com.sky31.gongmultiplatform.model

import kotlinx.serialization.Serializable

@Serializable
data class GongAiEnvelope<T>(
    val code: String,
    val message: String = "",
    val data: T? = null
)

@Serializable
data class GongAiChatRequest(
    val conversationId: String? = null,
    val message: String,
    val attachments: List<GongAiAttachmentRef> = emptyList()
)

@Serializable
data class GongAiAttachmentRef(
    val id: String,
    val purpose: String,
    val mediaType: String
)

@Serializable
data class GongAiChatResponse(
    val conversationId: String,
    val blocks: List<GongAiBlock>,
    val workflowState: String
)

@Serializable
data class GongAiConfirmResponse(val block: GongAiBlock)

@Serializable
data class GongAiBlock(
    val type: String,
    val text: String = "",
    val title: String = "",
    val items: List<GongAiCardItem> = emptyList(),
    val fields: List<GongAiPreviewField> = emptyList(),
    val warnings: List<String> = emptyList(),
    val actionId: String = "",
    val confirmText: String = "",
    val cancelText: String = "",
    val expiresAt: String = "",
    val status: String = ""
)

@Serializable
data class GongAiCardItem(
    val title: String,
    val subtitle: String = "",
    val meta: Map<String, String> = emptyMap()
)

@Serializable
data class GongAiPreviewField(
    val label: String,
    val value: String,
    val sensitive: Boolean = false
)

data class GongAiUiMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String = "",
    val blocks: List<GongAiBlock> = emptyList()
)

