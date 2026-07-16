package com.sky31.gongmultiplatform.ui.screen.settingScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sky31.gongmultiplatform.ui.component.LoadingRing
import com.sky31.gongmultiplatform.ui.component.drawer.UpdateNotificationDialog
import com.sky31.gongmultiplatform.ui.component.rememberDialogState
import com.sky31.gongmultiplatform.ui.viewModel.ConfigViewModel
import com.sky31.gongmultiplatform.ui.viewModel.SettingViewModel
import com.sky31.gongmultiplatform.util.AppUpdateState
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.Toast
import com.sky31.gongmultiplatform.util.getAppUpdateState
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.authentication
import gongmultiplatform.composeapp.generated.resources.notification
import gongmultiplatform.composeapp.generated.resources.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun SettingScreen(
    navController: NavController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val configViewModel: ConfigViewModel = getKoin().get()
    val settingViewModel: SettingViewModel = viewModel { SettingViewModel() }

    val dialogState = rememberDialogState()
    var loadingRingVisible by remember { mutableStateOf(false) }

    val platformInfo = settingViewModel.platformInfo
    val updateData by settingViewModel.updateData.collectAsState()
    val appUpdateState = updateData?.let {
        getAppUpdateState(
            current = platformInfo.getVersionName(),
            newest = it.lastVersion,
            least = it.leastVersion
        )
    }

    val checkUpdate: suspend () -> Unit = {
        loadingRingVisible = true
        val result = settingViewModel.checkUpdate()
        loadingRingVisible = false

        when (result) {
            is DataState.Newest -> {
                when (appUpdateState) {
                    AppUpdateState.UP_TO_DATE -> Toast.show("当前为最新版本")
                    AppUpdateState.OPTIONAL_UPDATE,
                    AppUpdateState.REQUIRED_UPDATE -> dialogState.show()
                    null -> Unit
                }
            }

            is DataState.Error -> Toast.show(result.message)
            else -> Unit
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                scope.launch {
                    configViewModel.updateFunctionalConfig()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (updateData != null && appUpdateState != null) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 16.dp, start = 15.dp, end = 15.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            IconRow(
                icon = Res.drawable.notification,
                text = "通知管理",
                click = {
                    navController.navigate("notification")
                }
            )

            IconRow(
                icon = Res.drawable.authentication,
                text = "认证服务",
                click = {
                    navController.navigate("authService")
                }
            )

            IconRow(
                icon = Res.drawable.update,
                text = "检查更新",
                click = {
                    scope.launch {
                        checkUpdate()
                    }
                }
            ) {
                AnimatedContent(
                    targetState = loadingRingVisible,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "check_update_loading"
                ) { visible ->
                    if (visible) {
                        LoadingRing(size = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun IconRow(
    icon: DrawableResource,
    text: String,
    click: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                click()
            }
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "icon",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelMedium
        )

        content()
    }
}
