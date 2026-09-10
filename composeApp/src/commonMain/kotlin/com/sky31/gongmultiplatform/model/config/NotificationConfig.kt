package com.sky31.gongmultiplatform.model.config

import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val courseNotification: Boolean = true,
    val examNotification: Boolean = true,
    val courseReminderMinutesBefore: Int = 10,
    val examReminderDaysBefore: Int = 7,
)
