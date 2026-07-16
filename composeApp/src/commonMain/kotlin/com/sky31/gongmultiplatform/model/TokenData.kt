package com.sky31.gongmultiplatform.model

import io.ktor.client.plugins.auth.providers.BearerTokens

object TokenData {
    var token: String? = null
}

val bearerTokenStorage = mutableListOf<BearerTokens>()