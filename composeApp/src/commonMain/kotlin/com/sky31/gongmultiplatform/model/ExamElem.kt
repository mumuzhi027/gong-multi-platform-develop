package com.sky31.gongmultiplatform.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamElem(
    val name: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val location: String,
    val type: String,
)
