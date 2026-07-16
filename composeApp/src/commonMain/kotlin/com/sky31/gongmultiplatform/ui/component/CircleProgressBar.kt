package com.sky31.gongmultiplatform.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

/**
 * 圆形进度条
 *
 * @param targetFlow 目标进度，取值范围为0-1
 * @param courseCount 课程数量
 */
@Composable
fun CircleProgressBar(targetFlow: StateFlow<Float>, courseCount: Int) {
    val target by targetFlow.collectAsState()
    val progression = remember { Animatable(0f) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(target) {
        println("target is $target")
        if (target != -1f) {
            progression.animateTo(
                targetValue = target * 360f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = FastOutLinearInEasing
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = courseCount.toString(),
            fontWeight = FontWeight(700),
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            drawArc(
                color = colorScheme.surface,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )

            drawArc(
                color = colorScheme.primary,
                startAngle = -90f,
                sweepAngle = progression.value,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}