package com.sky31.gongmultiplatform.ui.component.drawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.network.dto.UpdateDto
import com.sky31.gongmultiplatform.network.service.InstallService
import com.sky31.gongmultiplatform.ui.component.CustomScrollBox
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

    val progressStateFlow = remember { MutableStateFlow(-1) }
    val stateFlow = progressStateFlow.asStateFlow()

    var progressBarVisible by remember { mutableStateOf(false) }

    fun startInstall() {
        progressBarVisible = true
        progressStateFlow.value = 0

        scope.launch {
            runCatching {
                InstallService.downloadApk(data.updateUrl).collect { progress ->
                    progressStateFlow.value = progress
                }

                InstallService.installApk()
                state.hide()
            }.onFailure {
                progressBarVisible = false
                progressStateFlow.value = -1
                Toast.show(it.message ?: "更新失败")
            }
        }
    }

    fun requestInstallPermissionAndStart() {
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

    NotificationDialog(
        state = state,
        modifier = Modifier.fillMaxWidth(0.86f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
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

            CustomScrollBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = progressBarVisible,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "update_progress"
                ) { downloading ->
                    if (downloading) {
                        ProgressBar(
                            progressStateFlow = stateFlow,
                            modifier = Modifier
                                .padding(start = 15.dp, end = 15.dp, top = 5.dp, bottom = 5.dp)
                                .fillMaxWidth()
                                .height(10.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    val y = strokeWidth / 2
                                    drawLine(
                                        color = colorScheme.background,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                }
                        ) {
                            if (appUpdateState == AppUpdateState.OPTIONAL_UPDATE) {
                                Text(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSkipForWeek() }
                                        .padding(vertical = 10.dp),
                                    text = "一周不提示",
                                    color = colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )

                                DividerSpacer(colorScheme.background)

                                Text(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onDismissOnce() }
                                        .padding(vertical = 10.dp),
                                    text = "本次取消",
                                    color = colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )

                                DividerSpacer(colorScheme.background)
                            }

                            Text(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { requestInstallPermissionAndStart() }
                                    .padding(vertical = 10.dp),
                                text = "更新",
                                color = colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DividerSpacer(color: androidx.compose.ui.graphics.Color) {
    Spacer(
        modifier = Modifier
            .fillMaxHeight()
            .width(2.dp)
            .background(color)
    )
}
