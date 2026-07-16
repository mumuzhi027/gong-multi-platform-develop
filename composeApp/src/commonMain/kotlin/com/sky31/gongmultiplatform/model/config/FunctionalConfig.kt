package com.sky31.gongmultiplatform.model.config

import kotlinx.serialization.Serializable

@Serializable
data class FunctionalConfig(
    val notificationConfig: NotificationConfig = NotificationConfig(),
    val authConfig: AuthConfig = AuthConfig(),
)
