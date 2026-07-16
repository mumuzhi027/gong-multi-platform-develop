package com.sky31.gongmultiplatform.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun <T> LoadingButton(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(30.dp)
        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp)),
    text: String,
    call: suspend () -> T,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clickable(
                enabled = !isLoading
            ) {
                keyboardController?.hide()
                scope.launch {
                    isLoading = true
                    call()
                    isLoading = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = 300
                    )
                ) togetherWith
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 300
                            )
                )
            }
        ) { target ->
            if(target) {
                LoadingRing(
                    size = 16.dp
                )
            } else {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = textStyle
                )
            }
        }
    }
}