package com.sky31.gongmultiplatform.ui.component.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.network.dto.UpdateDto
import com.sky31.gongmultiplatform.network.service.InstallService
import com.sky31.gongmultiplatform.network.service.UpdateDownloadPhase
import com.sky31.gongmultiplatform.network.service.UpdateDownloadState
import com.sky31.gongmultiplatform.ui.component.DialogState
import com.sky31.gongmultiplatform.ui.component.NotificationDialog
import com.sky31.gongmultiplatform.ui.component.ProgressBar
import com.sky31.gongmultiplatform.ui.component.rememberDialogState
import com.sky31.gongmultiplatform.util.AppUpdateState
import com.sky31.gongmultiplatform.util.Toast
import com.sky31.gongmultiplatform.util.askInstallPermission
import com.sky31.gongmultiplatform.util.hasInstallPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun UpdateNotificationDialog(
    state: DialogState = rememberDialogState(),
    appUpdateState: AppUpdateState,
    data: UpdateDto = UpdateDto(),
    onSkipForWeek: () -> Unit = {},
    onDismissOnce: () -> Unit = { state.hide() }
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp

    val progressStateFlow = remember(data.updateUrl, data.lastVersion) { MutableStateFlow(0) }
    val stateFlow = progressStateFlow.asStateFlow()

    var download by remember(data.updateUrl, data.lastVersion) { mutableStateOf(UpdateDownloadState()) }
    var loaded by remember(data.updateUrl, data.lastVersion) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var autoInstall by remember(data.updateUrl, data.lastVersion) { mutableStateOf(true) }

    LaunchedEffect(state.isVisible, data.updateUrl, data.lastVersion) {
        loaded = false
        if (!state.isVisible) return@LaunchedEffect
        try {
            InstallService.observeDownload(data.updateUrl, data.lastVersion).collect {
                download = it
                progressStateFlow.value = it.progress
                loaded = true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            download = UpdateDownloadState(UpdateDownloadPhase.FAILED, message = failure.message ?: "无法读取下载进度")
            loaded = true
        }
    }

    fun startInstall() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                InstallService.installApk(data.updateUrl, data.lastVersion)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Toast.show(failure.message ?: "安装失败，更新包已保留")
            } finally {
                busy = false
            }
        }
    }

    fun requestInstallPermission() {
        if (hasInstallPermission()) {
            startInstall()
            return
        }

        askInstallPermission { granted ->
            if (granted || hasInstallPermission()) {
                startInstall()
            } else {
                Toast.show("请先授予安装权限")
            }
        }
    }

    fun enqueueDownload(automatic: Boolean) {
        scope.launch {
            try {
                InstallService.startDownload(data.updateUrl, data.lastVersion, automatic)
                state.hide()
                Toast.show(if (autoInstall && !automatic) "已开始后台下载，安装时需授予权限"
                    else "已开始后台下载，可继续使用")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Toast.show(failure.message ?: "无法开始下载")
            } finally {
                busy = false
            }
        }
    }

    fun startBackgroundDownload() {
        if (busy) return
        busy = true
        try {
            if (autoInstall && !hasInstallPermission()) {
                askInstallPermission { granted ->
                    enqueueDownload(granted || hasInstallPermission())
                }
            } else enqueueDownload(autoInstall)
        } catch (failure: Exception) {
            busy = false
            Toast.show(failure.message ?: "无法开始更新")
        }
    }

    NotificationDialog(
        state = state,
        modifier = Modifier.fillMaxWidth(0.86f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(start = 15.dp, top = 15.dp),
                text = data.updateTitle,
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.padding(start = 15.dp, top = 10.dp, end = 15.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.lastVersion,
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = data.updateDate,
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
                    .background(colorScheme.background, shape = RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = data.updateNotice,
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val downloading = download.phase == UpdateDownloadPhase.DOWNLOADING ||
                download.phase == UpdateDownloadPhase.PAUSED
            if (downloading) {
                ProgressBar(
                    progressStateFlow = stateFlow,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
                        .fillMaxWidth().height(10.dp)
                )
            }
            if (download.message.isNotBlank()) {
                Text(
                    text = download.message,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (loaded && download.phase in setOf(UpdateDownloadPhase.IDLE, UpdateDownloadPhase.FAILED)) {
                Row(modifier = Modifier.fillMaxWidth().padding(end = 15.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoInstall, onCheckedChange = { autoInstall = it }, enabled = !busy)
                    Text("离开应用后自动安装（系统可能要求确认）",
                        color = colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    drawLine(colorScheme.background, Offset(0f, strokeWidth / 2),
                        Offset(size.width, strokeWidth / 2), strokeWidth)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasTask = download.phase != UpdateDownloadPhase.IDLE &&
                    download.phase != UpdateDownloadPhase.FAILED
                if (appUpdateState == AppUpdateState.OPTIONAL_UPDATE && !hasTask) {
                    Text(
                        modifier = Modifier.weight(1f).clickable(enabled = !busy) { onSkipForWeek() }
                            .padding(vertical = 14.dp),
                        text = "一周不提示", color = colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                    )
                    DividerSpacer(colorScheme.background)
                }
                if (hasTask || appUpdateState == AppUpdateState.OPTIONAL_UPDATE) {
                    Text(
                        modifier = Modifier.weight(1f).clickable { onDismissOnce() }.padding(vertical = 14.dp),
                        text = if (hasTask) "继续使用" else "本次取消",
                        color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    DividerSpacer(colorScheme.background)
                }
                Text(
                    modifier = Modifier.weight(1f)
                        .clickable(enabled = loaded && !busy && !downloading &&
                            download.phase != UpdateDownloadPhase.INSTALLING) {
                            if (download.phase == UpdateDownloadPhase.READY) requestInstallPermission()
                            else startBackgroundDownload()
                        }.padding(vertical = 14.dp),
                    text = when {
                        busy -> "处理中…"
                        !loaded -> "读取进度…"
                        downloading -> "后台下载中"
                        download.phase == UpdateDownloadPhase.INSTALLING -> "安装中…"
                        download.phase == UpdateDownloadPhase.READY -> "安装更新"
                        download.phase == UpdateDownloadPhase.FAILED -> "重新下载"
                        else -> "后台下载"
                    },
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DividerSpacer(color: androidx.compose.ui.graphics.Color) {
    Spacer(
        modifier = Modifier
            .height(50.dp)
            .width(2.dp)
            .background(color)
    )
}
