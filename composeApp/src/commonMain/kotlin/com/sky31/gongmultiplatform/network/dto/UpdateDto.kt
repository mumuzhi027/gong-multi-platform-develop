package com.sky31.gongmultiplatform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateDto(
    @SerialName("last_version") val lastVersion: String = "",
    @SerialName("least_version") val leastVersion: String = "",
    @SerialName("update_url") val updateUrl: String = "",
    @SerialName("update_title") val updateTitle: String = "",
    @SerialName("update_notice") val updateNotice: String = "",
    @SerialName("update_date") val updateDate: String = ""
)
