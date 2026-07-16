package com.sky31.gongmultiplatform.ui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ToggleButton(
    stateFlow: MutableStateFlow<Boolean>,
    modifier: Modifier = Modifier,
    dotSize: Dp = 16.dp,
    containerWidth: Dp = 50.dp
) {
    val colorScheme = MaterialTheme.colorScheme

    // 最大水平位移
    val maxOffsetX = with(LocalDensity.current) { (containerWidth - dotSize).toPx() }

    val state by stateFlow.collectAsState()

    val backgroundColor = remember { Animatable(Color.Gray) }
    val animatableOffset = remember { Animatable(0f) }

    // 记住最新的状态（用于动画目标值）
    val currentState by rememberUpdatedState(state)

    // 是否是首次组合
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(currentState) {
        if(isFirstComposition) {
            isFirstComposition = false
            coroutineScope {
                launch {
                    animatableOffset.snapTo(if (currentState) maxOffsetX else 0f)
                }
                launch {
                    backgroundColor.snapTo(if (currentState) colorScheme.primary else Color.Gray)
                }
            }
        } else {
            coroutineScope {
                launch {
                    animatableOffset.animateTo(if (currentState) maxOffsetX else 0f)
                }
                launch {
                    backgroundColor.animateTo(if (currentState) colorScheme.primary else Color.Gray)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor.value)
            .clickable {
                stateFlow.value = !state
            }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(containerWidth)
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatableOffset.value.roundToInt(), 0) }
                    .size(dotSize)
                    .background(Color.White, shape = RoundedCornerShape(50))
            )
        }
    }
}