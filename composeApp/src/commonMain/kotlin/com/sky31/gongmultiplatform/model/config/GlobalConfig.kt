package com.sky31.gongmultiplatform.model.config

import com.sky31.gongmultiplatform.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class GlobalConfig(
    val themeMode: ThemeMode? = ThemeMode.SYSTEM
)