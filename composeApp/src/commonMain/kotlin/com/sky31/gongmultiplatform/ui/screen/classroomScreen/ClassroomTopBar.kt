package com.sky31.gongmultiplatform.ui.screen.classroomScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ClassroomTopBar(pagerState: PagerState) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var initialOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(size) {
        initialOffset = size.width / 4 - with(density) { 50.dp.toPx() }.toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged {
                size = it
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }
                    .padding(vertical = 4.dp),
                text = "今天",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    }
                    .padding(vertical = 4.dp),
                text = "明天",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((pagerState.currentPage + pagerState.currentPageOffsetFraction) * size.width / 2).toInt() + initialOffset,
                        y = 0
                    )
                }
                .scale(
                    scaleX = 1 + abs(pagerState.currentPageOffsetFraction) * 1.5f,
                    scaleY = 1 - abs(pagerState.currentPageOffsetFraction) * 0.5f
                )
                .width(100.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
