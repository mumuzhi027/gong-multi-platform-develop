package com.sky31.gongmultiplatform.util

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ContextProvider: KoinComponent {
    val context by inject<Context>()
}