package com.sky31.gongmultiplatform.util

import androidx.compose.runtime.Composable
import com.sky31.gongmultiplatform.MainActivity

actual object PlatformOperation {
    @Composable
    actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
        androidx.activity.compose.BackHandler(enabled, onBack)
    }

    actual fun moveToBack() {
        val activity = MainActivity.getInstance()

        activity?.moveTaskToBack(true)
    }
}