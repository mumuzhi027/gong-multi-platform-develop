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
                .padding(end = if(scrollBarVisibility) 16.dp else 0.dp),
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

            drawRoundRect(
                color = colorScheme.background,
                size = size,
                cornerRadius = CornerRadius(100f, 100f)
            )

            // thumb的高度
            val proportion = size.height * (size.height / (size.height + scrollState.maxValue.toFloat()))

            val offset = (scrollState.value.toFloat() / scrollState.maxValue) * (size.height - proportion)

            drawRoundRect(
                color = colorScheme.primary,
                topLeft = Offset(x = 0f, y = offset),
                size = Size(width = size.width, height = proportion),
                cornerRadius = CornerRadius(100f, 100f)
            )
        }
    }
}