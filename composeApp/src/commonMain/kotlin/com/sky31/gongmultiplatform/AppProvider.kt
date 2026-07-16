package com.sky31.gongmultiplatform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    val isDarkTheme = remember {
        derivedStateOf {
            when (globalConfig.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                else -> isSystemInDarkTheme
            }
        }
    }
    val colorScheme = remember {
        derivedStateOf {
            if (isDarkTheme.value) darkColorScheme else lightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme.value,
        typography = typography
    ) {
        CompositionLocalProvider(
            LocalGlobalConfig provides globalConfig,
            LocalIsDarkTheme provides isDarkTheme.value,
        ) {
            content()
        }
    }
}