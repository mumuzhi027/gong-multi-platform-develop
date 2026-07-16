package com.sky31.gongmultiplatform.ui.navHost

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sky31.gongmultiplatform.di.LocalAppNavController
import com.sky31.gongmultiplatform.ui.layout.MainLayout
import com.sky31.gongmultiplatform.ui.screen.courseScreen.CourseScreen

/**
 * 完成认证后的路由
 *
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        CompositionLocalProvider(
            LocalAppNavController provides navController
        ) {
            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable(
                    route = "main",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    }
                ) {
                    MainLayout()
                }
                composable(
                    route = "courseScreen",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    }
                ) {
                    CourseScreen()
                }
            }
        }
    }
}