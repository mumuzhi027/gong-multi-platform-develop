package com.sky31.gongmultiplatform.ui.navHost

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sky31.gongmultiplatform.di.LocalOverloadNavController
import com.sky31.gongmultiplatform.ui.screen.overload.NotificationRequestScreen
import com.sky31.gongmultiplatform.util.hasPostNotificationPermission
import kotlinx.coroutines.delay

@Composable
fun OverloadNavHost() {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        if(!hasPostNotificationPermission()) {
            delay(500)
            navController.navigate("notificationRequest")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompositionLocalProvider(
            LocalOverloadNavController provides navController
        ) {
            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable(
                    route = "main",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    AppNavHost()
                }

                composable(
                    route = "settings",
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(
                                delayMillis = 300,
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                delayMillis = 300,
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { it },
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
                    SettingNavHost()
                }

                composable(
                    route = "about",
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(
                                delayMillis = 300,
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                delayMillis = 300,
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    exitTransition = {
                        slideOutVertically(
                            targetOffsetY = { it },
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
                    AboutNavHost()
                }

                composable(
                    route = "notificationRequest",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                ) {
                    NotificationRequestScreen()
                }
            }
        }
    }
}