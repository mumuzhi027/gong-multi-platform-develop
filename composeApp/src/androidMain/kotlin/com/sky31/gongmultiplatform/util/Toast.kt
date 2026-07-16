package com.sky31.gongmultiplatform.util

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object Toast: KoinComponent {
    val context: Context by inject()

    actual fun show(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}