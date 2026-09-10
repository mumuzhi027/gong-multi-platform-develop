package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.model.config.NotificationConfig

expect fun hasPostNotificationPermission(): Boolean

expect fun askPostNotificationPermission(
    onResult: (Boolean) -> Unit
)

expect fun scheduleCourseAlarm()

expect fun scheduleExamAlarm(exams: List<ExamElem>, advanceDays: Int = 7)

expect fun applyNotificationConfig(config: NotificationConfig, exams: List<ExamElem>)
