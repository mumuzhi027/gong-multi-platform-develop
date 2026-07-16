package com.sky31.gongmultiplatform.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sky31.gongmultiplatform.util.AppUpdateState

class DialogState(
    initial: Boolean = false
) {
    var isVisible by mutableStateOf(initial)

    fun show() { isVisible = true }
    fun hide() { isVisible = false }
}

@Composable
fun rememberDialogState(initial: Boolean = false): DialogState {
    return remember { DialogState(initial) }
}

@Composable
fun NotificationDialog(
    state: DialogState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (state.isVisible)
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            ) {
                content()
            }
        }
}