package com.sky31.gongmultiplatform.model.config

import kotlinx.serialization.Serializable

@Serializable
data class AuthConfig(
    val reauthentication: Boolean = true,
)
