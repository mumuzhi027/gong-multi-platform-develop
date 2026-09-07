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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

@Composable
fun DesktopWidgetScreen() {
    val navController = LocalOverloadNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val supported = desktopWidgetSupported()
    var installed by remember { mutableStateOf(isDesktopWidgetEnabled()) }
    var waitingForConfirmation by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installed = isDesktopWidgetEnabled()
                if (installed) waitingForConfirmation = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val changeWidgetState: (Boolean) -> Unit = { requested ->
        if (requested) {
            val accepted = setDesktopWidgetEnabled(true)
            installed = isDesktopWidgetEnabled()
            waitingForConfirmation = accepted && !installed
            when {
                installed -> Toast.show("桌面组件已添加")
                accepted -> Toast.show("已发起添加，请在系统弹窗中确认")
                else -> Toast.show("没有弹窗，请打开“允许创建桌面快捷方式”权限")
            }
        } else {
            Toast.show("请在桌面长按该组件并选择移除")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

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
