package com.sky31.gongmultiplatform.ui.screen.settingScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sky31.gongmultiplatform.model.config.NotificationConfig
import com.sky31.gongmultiplatform.ui.component.SingleConfigRow
import com.sky31.gongmultiplatform.ui.viewModel.ConfigViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun NotificationSettingScreen() {
    val scope = rememberCoroutineScope()
    val vm: ConfigViewModel = getKoin().get<ConfigViewModel>()

    val notificationConfig by vm.notificationConfig.collectAsState()
    val courseNotification = remember(notificationConfig.courseNotification) {
        MutableStateFlow(notificationConfig.courseNotification)
    }
    val examNotification = remember(notificationConfig.examNotification) {
        MutableStateFlow(notificationConfig.examNotification)
    }
    var courseAdvance by remember(notificationConfig.courseReminderMinutesBefore) {
        mutableStateOf(notificationConfig.courseReminderMinutesBefore.toString())
    }
    var examAdvance by remember(notificationConfig.examReminderDaysBefore) {
        mutableStateOf(notificationConfig.examReminderDaysBefore.toString())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, courseNotification, examNotification, courseAdvance, examAdvance) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.notificationConfig.value = NotificationConfig(
                    courseNotification = courseNotification.value,
                    examNotification = examNotification.value,
                    courseReminderMinutesBefore = courseAdvance.toIntOrNull()?.coerceIn(0, 360) ?: 10,
                    examReminderDaysBefore = examAdvance.toIntOrNull()?.coerceIn(0, 365) ?: 7
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
                stateFlow = courseNotification,
                text = "上课通知",
                description = "可自定义提前 0—360 分钟发送提醒"
            )

            OutlinedTextField(
                value = courseAdvance,
                onValueChange = {
                    val value = it.filter(Char::isDigit).take(3)
                    if (value.isEmpty() || value.toInt() <= 360) courseAdvance = value
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = courseNotification.collectAsState().value,
                label = { Text("提前时间") },
                suffix = { Text("分钟") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            SingleConfigRow(
                stateFlow = examNotification,
                text = "考试通知",
                description = "可自定义提前 0—365 天发送提醒"
            )

            OutlinedTextField(
                value = examAdvance,
                onValueChange = {
                    val value = it.filter(Char::isDigit).take(3)
                    if (value.isEmpty() || value.toInt() <= 365) examAdvance = value
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = examNotification.collectAsState().value,
                label = { Text("提前时间") },
                suffix = { Text("天") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
