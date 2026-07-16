package com.sky31.gongmultiplatform.model.config

import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val courseNotification: Boolean = true,
    val examNotification: Boolean = true,
)