package com.sky31.gongmultiplatform.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 细窄低亮度光片沿左上到右下移动，文字保持可读。 */
fun Modifier.courseConflictSheen(enabled: Boolean): Modifier = if (!enabled) this else composed {
    val progress = rememberInfiniteTransition(label = "courseConflictSheen").animateFloat(
        initialValue = -1f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "diagonalSheen"
    )
    drawWithCache {
        val width = 16.dp.toPx()
        onDrawWithContent {
            drawContent()
            val center = Offset(size.width * progress.value, size.height * progress.value)
            drawRect(Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = .16f), Color.Transparent),
                start = center - Offset(width, width), end = center + Offset(width, width)
            ))
        }
    }
}
