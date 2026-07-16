package com.sky31.gongmultiplatform.ui.navHost

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sky31.gongmultiplatform.di.LocalOverloadNavController
import com.sky31.gongmultiplatform.ui.component.ScaffoldTopBar
import com.sky31.gongmultiplatform.ui.screen.settingScreen.AuthServiceScreen
import com.sky31.gongmultiplatform.ui.screen.settingScreen.NotificationSettingScreen
import com.sky31.gongmultiplatform.ui.screen.settingScreen.SettingScreen
import com.sky31.gongmultiplatform.ui.viewModel.SettingViewModel

@Composable
fun SettingNavHost() {
    val overloadNavController = LocalOverloadNavController.current
    val navController = rememberNavController()

    val vm: SettingViewModel = viewModel { SettingViewModel() }

    val title by vm.topBarTitle.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topBarNavController by remember(currentRoute) {
        derivedStateOf {
            if(currentRoute == "main") overloadNavController else navController
        }
    }

    LaunchedEffect(currentRoute) {
        when(currentRoute) {
            "main" -> vm.setTopBarTitle("设置")
            "notification" -> vm.setTopBarTitle("通知管理")
            "authService" -> vm.setTopBarTitle("认证服务")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            ScaffoldTopBar(
                navController = topBarNavController,
                title = title
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "main",
                enterTransition = {
                    fadeIn()
                },
                exitTransition = {
                    fadeOut(
                        targetAlpha = 0f,
                        animationSpec = tween(
                            durationMillis = 5
                        )
                    )
                },
                popEnterTransition = {
                    fadeIn()
                },
                popExitTransition = {
                    fadeOut(
                        targetAlpha = 0f,
                        animationSpec = tween(
                            durationMillis = 5
                        )
                    )
                }
            ) {
                composable(
                    route = "main"
                ) {
                    SettingScreen(navController)
                }

                composable(
                    route = "notification"
                ) {
                    NotificationSettingScreen()
                }

                composable(
                    route = "authService"
                ) {
                    AuthServiceScreen()
                }
            }
        }
    }
}