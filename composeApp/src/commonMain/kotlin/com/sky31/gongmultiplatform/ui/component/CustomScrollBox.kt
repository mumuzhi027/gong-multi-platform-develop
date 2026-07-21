package com.sky31.gongmultiplatform.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

@Composable
fun CustomScrollBox(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    val scrollBarVisibility by remember {
        derivedStateOf {
            scrollState.maxValue > 0
        }
    }

    Box(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                // Always reserve the scrollbar gutter. Making the content width depend on
                // maxValue can create a layout loop near the overflow boundary: showing the
                // scrollbar changes text wrapping, which can hide it again, and vice versa.
                .padding(end = 16.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            content()
        }

        if(scrollBarVisibility) {
            VerticalScrollBar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(6.dp),
                scrollState = scrollState
            )
        }
    }
}

@Composable
fun VerticalScrollBar(
    modifier: Modifier = Modifier,
    scrollState: ScrollState
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val maxValue = scrollState.maxValue
            if (maxValue <= 0) return@Canvas

            drawRoundRect(
                color = colorScheme.background,
                size = size,
                cornerRadius = CornerRadius(100f, 100f)
            )

            // thumb的高度
            val proportion = size.height * (size.height / (size.height + maxValue.toFloat()))

            val offset = (scrollState.value.toFloat() / maxValue) * (size.height - proportion)

            drawRoundRect(
                color = colorScheme.primary,
                topLeft = Offset(x = 0f, y = offset),
                size = Size(width = size.width, height = proportion),
                cornerRadius = CornerRadius(100f, 100f)
            )
        }
    }
}
