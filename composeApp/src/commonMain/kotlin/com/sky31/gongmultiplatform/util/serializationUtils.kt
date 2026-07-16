package com.sky31.gongmultiplatform.util

import kotlinx.serialization.json.Json

inline fun <reified T> decodeOrNull(
    raw: String?,
    label: String = T::class.simpleName ?: "unknown"
): T? {
    if (raw.isNullOrBlank()) {
        return null
    }

    return runCatching {
        Json.decodeFromString<T>(raw)
    }.getOrElse { error ->
        println("Failed to decode $label: ${error.message}")
        null
    }
}
