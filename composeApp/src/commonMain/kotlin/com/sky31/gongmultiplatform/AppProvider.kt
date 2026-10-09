package com.sky31.gongmultiplatform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.di.LocalGlobalConfig
import com.sky31.gongmultiplatform.di.LocalIsDarkTheme
import com.sky31.gongmultiplatform.ui.theme.ThemeMode
import com.sky31.gongmultiplatform.ui.theme.darkColorScheme
import com.sky31.gongmultiplatform.ui.theme.lightColorScheme
import com.sky31.gongmultiplatform.ui.theme.typography
import com.sky31.gongmultiplatform.ui.viewModel.GlobalViewModel

@Composable
fun AppProvider(
    content: @Composable () -> Unit
) {
    val globalViewModel: GlobalViewModel = viewModel { GlobalViewModel() }

    val globalConfig by globalViewModel.globalConfig.collectAsState()

    val isSystemInDarkTheme = isSystemInDarkTheme()

    val isDarkTheme = when (globalConfig.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> isSystemInDarkTheme
    }
    val colorScheme = if (isDarkTheme) darkColorScheme else lightColorScheme
    val systemDensity = LocalDensity.current
    val appDensity = remember(systemDensity) {
        Density(
            density = systemDensity.density,
            fontScale = systemDensity.fontScale.coerceAtMost(MAX_APP_FONT_SCALE),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        CompositionLocalProvider(
            LocalDensity provides appDensity,
            LocalGlobalConfig provides globalConfig,
            LocalIsDarkTheme provides isDarkTheme,
        ) {
            content()
        }
    }
}

private const val MAX_APP_FONT_SCALE = 1.15f
