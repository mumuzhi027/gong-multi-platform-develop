package com.sky31.gongmultiplatform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform