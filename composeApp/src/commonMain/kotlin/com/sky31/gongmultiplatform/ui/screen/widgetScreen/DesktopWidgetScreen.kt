package com.sky31.gongmultiplatform.ui.screen.widgetScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sky31.gongmultiplatform.di.LocalOverloadNavController
import com.sky31.gongmultiplatform.ui.component.ScaffoldTopBar
import com.sky31.gongmultiplatform.util.Toast
import com.sky31.gongmultiplatform.util.desktopWidgetSupported
import com.sky31.gongmultiplatform.util.isDesktopWidgetEnabled
import com.sky31.gongmultiplatform.util.setDesktopWidgetEnabled
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.course
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay

@Composable
fun DesktopWidgetScreen() {
    val navController = LocalOverloadNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val supported = desktopWidgetSupported()
    var installed by remember { mutableStateOf(isDesktopWidgetEnabled()) }
    var waitingForConfirmation by remember { mutableStateOf(false) }
    var showAddExplanation by remember { mutableStateOf(false) }
    var showManualGuide by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installed = isDesktopWidgetEnabled()
                if (installed) {
                    waitingForConfirmation = false
                    showManualGuide = false
                } else if (waitingForConfirmation) {
                    showManualGuide = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(waitingForConfirmation) {
        if (waitingForConfirmation) {
            delay(2500)
            installed = isDesktopWidgetEnabled()
            if (!installed && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                showManualGuide = true
            }
        }
    }

    val requestWidget = {
        val accepted = setDesktopWidgetEnabled(true)
        installed = isDesktopWidgetEnabled()
        waitingForConfirmation = accepted && !installed
        when {
            installed -> Toast.show("桌面组件已添加")
            accepted -> Toast.show("已请求系统添加，请在系统窗口中确认")
            else -> showManualGuide = true
        }
    }

    val changeWidgetState: (Boolean) -> Unit = { requested ->
        if (requested) {
            showAddExplanation = true
        } else {
            Toast.show("请在桌面长按该组件并选择移除")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

    if (showAddExplanation) {
        AlertDialog(
            onDismissRequest = { showAddExplanation = false },
            title = { Text("添加今日课程组件") },
            text = {
                Text(
                    "桌面组件会在桌面显示当天课程，点击组件可进入拱拱首页。继续后，系统可能弹出添加确认窗口。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddExplanation = false
                        requestWidget()
                    }
                ) { Text("继续添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddExplanation = false }) { Text("取消") }
            }
        )
    }

    if (showManualGuide) {
        AlertDialog(
            onDismissRequest = {
                showManualGuide = false
                waitingForConfirmation = false
            },
            title = { Text("系统没有显示添加窗口") },
            text = {
                Text(
                    "请在系统设置中为拱拱开启“允许创建桌面快捷方式”权限，然后回来重试。若系统没有这项权限，请长按桌面空白处，进入“窗口小工具”，找到拱拱并手动添加。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualGuide = false
                        waitingForConfirmation = false
                        showAddExplanation = true
                    }
                ) { Text("重新尝试") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManualGuide = false
                        waitingForConfirmation = false
                    }
                ) { Text("我知道了") }
            }
        )
    }

    if (showManualGuide) {
        AlertDialog(
            onDismissRequest = { showManualGuide = false },
            title = { Text("系统没有弹出添加窗口") },
            text = {
                Text(
                    "请先在系统设置中为拱拱开启“允许创建桌面快捷方式”权限，然后回来重试。若手机没有这项权限，请长按桌面空白处，进入“窗口小工具/桌面组件”，找到“拱拱”并手动添加。"
                )
            },
            confirmButton = {
                Button(onClick = { showManualGuide = false }) { Text("我知道了") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        topBar = { ScaffoldTopBar(navController = navController, title = "桌面组件") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 15.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = supported) {
                        changeWidgetState(!installed)
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.course),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("今日课程", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelMedium)
                    Text(
                        when {
                            installed -> "已添加到桌面"
                            waitingForConfirmation -> "等待系统确认添加"
                            else -> "尚未添加到桌面"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = installed,
                    enabled = supported,
                    onCheckedChange = {
                        changeWidgetState(it)
                    }
                )
            }
            Text(
                if (supported) {
                    "开启后系统会请求把组件添加到桌面。如果没有弹窗出现，请自行打开权限“允许创建桌面快捷方式”，再回来重新开启；也可以长按桌面空白处，在“窗口小工具”中选择拱拱。"
                } else {
                    "当前系统暂不支持自动添加桌面组件。"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 36.dp, end = 8.dp)
            )
        }
    }
}
