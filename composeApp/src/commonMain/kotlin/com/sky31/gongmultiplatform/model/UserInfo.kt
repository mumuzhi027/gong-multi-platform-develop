package com.sky31.gongmultiplatform.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    @SerialName("student_id") val studentID: String = "",
    val name: String = "",
    val gender: String = "",
    val birthday: String = "",
    val major: String = "",
    @SerialName("class_") val clazz: String = "",
    @SerialName("entrance_day") val entranceDay: String = ""
)
