package com.sky31.gongmultiplatform.util

import androidx.compose.runtime.Composable

expect object PlatformOperation {

    @Composable
    fun BackHandler(enabled: Boolean, onBack: () -> Unit)

    fun moveToBack()
}