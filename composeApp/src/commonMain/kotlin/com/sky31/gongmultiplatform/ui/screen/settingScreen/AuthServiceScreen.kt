package com.sky31.gongmultiplatform.ui.screen.settingScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sky31.gongmultiplatform.model.config.AuthConfig
import com.sky31.gongmultiplatform.ui.component.SingleConfigRow
import com.sky31.gongmultiplatform.ui.viewModel.ConfigViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun AuthServiceScreen() {
    val scope = rememberCoroutineScope()
    val vm: ConfigViewModel = getKoin().get<ConfigViewModel>()

    val authConfig by vm.authConfig.collectAsState()
    val reauthentication = MutableStateFlow(authConfig.reauthentication)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.authConfig.value = AuthConfig(
                    reauthentication.value
                )
                scope.launch {
                    vm.updateFunctionalConfig()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 16.dp, start = 15.dp, end = 15.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleConfigRow(
                stateFlow = reauthentication,
                text = "重新认证",
                description = "当请求返回未授权状态时，弹出认证模块。"
            )
        }
    }
}