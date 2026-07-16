package com.sky31.gongmultiplatform

import io.ktor.http.URLProtocol

expect object PlatformNetworkDefaults {
    val apiHost: String
    val updateHost: String
    val webHost: String
}

data class NetworkEndpoint(
    val host: String,
    val port: Int,
    val protocol: URLProtocol = URLProtocol.HTTP
) {
    val baseUrl: String
        get() = "${protocol.name}://$host:$port"
}

object SystemGlobalConfig {
    val apiEndpoint = NetworkEndpoint(
        host = PlatformNetworkDefaults.apiHost,
        port = 8000
    )

    val updateEndpoint = NetworkEndpoint(
        host = PlatformNetworkDefaults.updateHost,
        port = 80
    )

    val webEndpoint = NetworkEndpoint(
        host = PlatformNetworkDefaults.webHost,
        port = 8000
    )

    const val MAX_RETRY_TIMES = 3
    const val RETRY_INTERVAL = 1000L
}
