package com.sky31.gongmultiplatform.ui.navHost

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sky31.gongmultiplatform.di.LocalAuthNavController
import com.sky31.gongmultiplatform.ui.component.AuthorizationDialog
import com.sky31.gongmultiplatform.ui.component.ObserveAsEvents
import com.sky31.gongmultiplatform.ui.screen.loginScreen.LoginScreen
import com.sky31.gongmultiplatform.ui.viewModel.AuthViewModel
import com.sky31.gongmultiplatform.ui.viewModel.NavigationEvent
import org.koin.mp.KoinPlatform.getKoin

/**
 * 认证路由
 *
 */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()
    val authViewModel = getKoin().get<AuthViewModel>()

    ObserveAsEvents(authViewModel.navigationEventsChannelFlow) { event ->
        println("collected!!!")
        when(event) {
            is NavigationEvent.ToMainScreen -> {
                navController.navigate("main")
            }
            is NavigationEvent.ToLoginScreen -> {
                navController.navigate("login") {
                    popUpTo("login") {
                        inclusive = true
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CompositionLocalProvider(
            LocalAuthNavController provides navController
        ) {
            NavHost(
                navController = navController,
                startDestination = if (authViewModel.authState.value.isAuthenticated) "main" else "login"
            ) {
                composable(
                    route = "login",
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        ) + fadeOut(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                ) {
                    LoginScreen()
                }

                composable(
                    route = "main",
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { -it / 2 },
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        ) + fadeOut(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                ) {
                    OverloadNavHost()

                    AuthorizationDialog()
                }
            }
        }
    }
}