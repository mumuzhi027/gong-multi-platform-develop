package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.model.ExamElem

expect fun hasPostNotificationPermission(): Boolean

expect fun askPostNotificationPermission(
    onResult: (Boolean) -> Unit
)

expect fun scheduleCourseAlarm()

expect fun scheduleExamAlarm(exams: List<ExamElem>)

