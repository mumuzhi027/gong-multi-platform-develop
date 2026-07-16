package com.sky31.gongmultiplatform.util

import kotlinx.serialization.Serializable

@Serializable
data class ClauseRoute(
    val clauseType: String,
    val title: String
)
