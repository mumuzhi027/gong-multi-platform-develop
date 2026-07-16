package com.sky31.gongmultiplatform.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarData(
    val start: String,
    val weeks: Int,
    @SerialName("term_id") val termId: String
)
