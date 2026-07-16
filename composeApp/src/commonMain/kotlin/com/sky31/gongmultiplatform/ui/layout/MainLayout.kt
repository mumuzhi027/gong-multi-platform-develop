package com.sky31.gongmultiplatform.ui.layout

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.ui.component.drawer.UpdateNotificationDialog
import com.sky31.gongmultiplatform.ui.component.rememberDialogState
import com.sky31.gongmultiplatform.ui.component.drawer.MainScreenDrawer
import com.sky31.gongmultiplatform.ui.screen.academicScreen.AcademicScreen
import com.sky31.gongmultiplatform.ui.screen.classroomScreen.ClassroomScreen
import com.sky31.gongmultiplatform.ui.screen.mainScreen.MainScreen
import com.sky31.gongmultiplatform.ui.viewModel.SettingViewModel
import com.sky31.gongmultiplatform.util.AppUpdateState
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.getAppUpdateState
import kotlinx.coroutines.delay

@Composable
fun MainLayout() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        gesturesEnabled = true,
        drawerState = drawerState,
        drawerContent = {
            MainScreenDrawer(drawerState)
        }
    ) {
        AutoUpdateHost()

        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    navController = navController
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home",
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
                ) {
                    composable("home") {
                        MainScreen(
                            drawerState = drawerState,
                            toAcademicScreen = {
                                navController.navigate("academic")
                            }
                        )
                    }

                    composable("emptyClassroom") {
                        ClassroomScreen()
                    }

                    composable("academic") {
                        AcademicScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoUpdateHost() {
    val dialogState = rememberDialogState()
    val settingViewModel: SettingViewModel = viewModel { SettingViewModel() }
    val updateData by settingViewModel.updateData.collectAsState()
    val platformInfo = settingViewModel.platformInfo

    val appUpdateState = updateData?.let {
        getAppUpdateState(
            current = platformInfo.getVersionName(),
            newest = it.lastVersion,
            least = it.leastVersion
        )
    }

    LaunchedEffect(Unit) {
        delay(1200)

        val result = settingViewModel.checkUpdate()
        if (result !is DataState.Newest) return@LaunchedEffect

        val latestUpdateData = settingViewModel.updateData.value ?: return@LaunchedEffect
        val latestUpdateState = getAppUpdateState(
            current = platformInfo.getVersionName(),
            newest = latestUpdateData.lastVersion,
            least = latestUpdateData.leastVersion
        )

        if (
            latestUpdateState != AppUpdateState.UP_TO_DATE &&
            settingViewModel.shouldShowAutoUpdateDialog(latestUpdateData, latestUpdateState)
        ) {
            dialogState.show()
        }
    }

    if (updateData != null && appUpdateState != null && appUpdateState != AppUpdateState.UP_TO_DATE) {
        UpdateNotificationDialog(
            state = dialogState,
            appUpdateState = appUpdateState,
            data = updateData!!,
            onSkipForWeek = {
                settingViewModel.suppressUpdateForWeek(updateData!!)
                dialogState.hide()
            },
            onDismissOnce = {
                dialogState.hide()
            }
        )
    }
}
