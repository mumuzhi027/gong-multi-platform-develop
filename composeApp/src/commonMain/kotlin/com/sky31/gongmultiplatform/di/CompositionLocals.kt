package com.sky31.gongmultiplatform.di

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import com.sky31.gongmultiplatform.model.config.GlobalConfig

val LocalGlobalConfig = staticCompositionLocalOf<GlobalConfig> {
    error("No GlobalConfig provided! Please wrap your app with GlobalConfigProvider.")
}

val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> {
    error("No IsDarkTheme provided! Please wrap your app with IsDarkThemeProvider.")
}

val LocalAuthNavController = staticCompositionLocalOf<NavController> {
    error("No AuthNavController provided")
}

val LocalAppNavController = staticCompositionLocalOf<NavController> {
    error("No AppNavController provided")
}

val LocalOverloadNavController = staticCompositionLocalOf<NavController> {
    error("No OverloadNavController provided")
}