package com.sky31.gongmultiplatform.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoadingRing(
    size: Dp = 20.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onBackground,
    duration: Int = 2000
) {
    val rotation = remember { Animatable(0f) }
    val sweep = remember { Animatable(20f) }
    val startAngle = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while(true) {
            coroutineScope {
                launch {
                    sweep.animateTo(
                        targetValue = 290f,
                        animationSpec = tween(durationMillis = duration / 2, easing = LinearEasing)
                    )
                }
                // 2️⃣ rotation 持续旋转
                launch {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = tween(durationMillis = duration, easing = LinearEasing)
                    )
                }
                // 等扩张结束
                delay(duration / 2L)
                // 3️⃣ sweep 从 290 → 0（收缩），startAngle 同步前移
                launch {
                    startAngle.animateTo(
                        targetValue = startAngle.value + 290f,
                        animationSpec = tween(durationMillis = duration / 2, easing = LinearEasing)
                    )
                }
                sweep.animateTo(
                    targetValue = 20f,
                    animationSpec = tween(durationMillis = duration / 2, easing = LinearEasing)
                )
            }
        }
    }

    Canvas(
        modifier = Modifier
            .padding(4.dp)
            .size(size)
    ) {
        val diameter = size.toPx()
        val stroke = strokeWidth.toPx()

        val start = (startAngle.value + rotation.value) % 360

        drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep.value,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            size = Size(diameter, diameter)
        )
    }
}