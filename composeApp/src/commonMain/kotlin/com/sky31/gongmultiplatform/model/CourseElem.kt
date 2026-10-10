package com.sky31.gongmultiplatform.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CourseElem(
    val name: String,
    val teacher: String,
    val classroom: String,
    val weeks: String,
    @SerialName("start_time")
    val startTime: Int,
    val duration: Int,
    val day: String,
    @SerialName("is_custom")
    val isCustom: Boolean = false,
    @SerialName("is_experiment") val isExperiment: Boolean = false,
    @SerialName("experiment_id") val experimentId: String = "",
    @SerialName("experiment_project") val experimentProject: String = "",
    @SerialName("experiment_group") val experimentGroup: String = ""
)
