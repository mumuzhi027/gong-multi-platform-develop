package com.sky31.gongmultiplatform.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.di.LocalIsDarkTheme

/** 亮色光段沿圆角边框匀速循环，保持轮廓和课程底色不动。 */
fun Modifier.staggeredCourseBorder(
    enabled: Boolean = true,
    cornerRadius: Dp = 4.dp,
    animated: Boolean = true
): Modifier {
    if (!enabled) return this
    return composed {
        val isDark = LocalIsDarkTheme.current
        val trackColor = if (isDark) Color(0xFF243548) else Color(0xFFD5DEED)
        val leadColor = if (isDark) Color(0xFF5AE8FF) else Color(0xFF004FFF)
        val accentColor = if (isDark) Color(0xFFFFC36B) else Color(0xFFD50079)
        val separatorColor = if (isDark) Color(0xFF101820) else Color.White
        val progress = if (animated) {
            rememberInfiniteTransition(label = "courseBorder").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
                label = "courseBorderProgress"
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }
        drawWithCache {
            val width = 3.dp.toPx().coerceAtMost(size.minDimension / 2f)
            val separatorWidth = 0.5.dp.toPx().coerceAtMost(width)
            val colorWidth = width - separatorWidth
            val radius = cornerRadius.toPx().coerceAtMost(size.minDimension / 2f)
            fun ringPath(inset: Float, thickness: Float) = Path().apply {
                fillType = PathFillType.EvenOdd
                addRoundRect(RoundRect(
                    Rect(inset, inset, size.width - inset, size.height - inset),
                    CornerRadius((radius - inset).coerceAtLeast(0f))
                ))
                val innerInset = inset + thickness
                if (size.minDimension > innerInset * 2f) {
                    addRoundRect(RoundRect(
                        Rect(innerInset, innerInset, size.width - innerInset, size.height - innerInset),
                        CornerRadius((radius - innerInset).coerceAtLeast(0f))
                    ))
                }
            }
            val ring = ringPath(0f, colorWidth)
            val separator = ringPath(colorWidth, separatorWidth)
            val centerInset = colorWidth / 2f
            val perimeter = Path().apply {
                addRoundRect(RoundRect(
                    Rect(centerInset, centerInset, size.width - centerInset, size.height - centerInset),
                    CornerRadius((radius - centerInset).coerceAtLeast(0f))
                ))
            }
            val measure = PathMeasure().apply { setPath(perimeter, forceClosed = true) }
            val segment = Path()
            val stroke = Stroke(width = colorWidth, cap = StrokeCap.Round)
            onDrawWithContent {
                drawContent()
                // 按周长推进光段；长条课程卡的四边也能明显看到它在移动。
                clipPath(ring) {
                    drawPath(ring, trackColor)
                    fun drawSegment(start: Float, extent: Float, color: Color) {
                        val normalizedStart = (start % 1f + 1f) % 1f
                        val end = normalizedStart + extent
                        segment.reset()
                        measure.getSegment(normalizedStart * measure.length, minOf(end, 1f) * measure.length, segment)
                        if (end > 1f) {
                            measure.getSegment(0f, (end - 1f) * measure.length, segment, startWithMoveTo = true)
                        }
                        drawPath(segment, color, style = stroke)
                    }
                    val head = progress.value
                    // 短尾逐段变亮，双色光段之间留下明显的暗/浅色间隔。
                    for (step in 0..5) {
                        val tail = (5 - step) * 0.016f
                        val opacity = 0.18f + step * 0.14f
                        drawSegment(head - 0.18f - tail, 0.18f + tail, leadColor.copy(alpha = opacity))
                        drawSegment(head + 0.5f - 0.10f - tail, 0.10f + tail, accentColor.copy(alpha = opacity))
                    }
                }
                drawPath(separator, separatorColor)
            }
        }
    }
}
