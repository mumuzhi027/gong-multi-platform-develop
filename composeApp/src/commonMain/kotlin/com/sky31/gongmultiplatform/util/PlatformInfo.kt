package com.sky31.gongmultiplatform.util

import org.koin.core.component.KoinComponent

expect class PlatformInfo {
    fun getVersionName(): String
    fun getVersionCode(): Long
}