package com.sky31.gongmultiplatform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: String,
    @SerialName("token_type") val tokenType: String
)