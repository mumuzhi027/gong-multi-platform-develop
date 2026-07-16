package com.sky31.gongmultiplatform.ui.screen.classroomScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.ClassroomData

@Composable
fun ClassroomSubScreen(
    list: List<ClassroomData.ClassroomInfo>,
    checkStatus: List<Boolean>,
    layoutMetrics: ClassroomLayoutMetrics
) {
    val listState = rememberLazyListState()

    LaunchedEffect(checkStatus) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 5.dp, bottom = 5.dp, start = 15.dp, end = 15.dp)
    ) {
        items(list) { classroomInfo ->
            for ((index, selected) in checkStatus.withIndex()) {
                if (selected && classroomInfo.status.getOrNull(index) != "空") {
                    return@items
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layoutMetrics.rowHeight)
                    .padding(top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val roomNameScrollState = rememberScrollState()

                Box(
                    modifier = Modifier
                        .width(layoutMetrics.roomColumnWidth)
                        .padding(end = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .horizontalScroll(roomNameScrollState),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text = classroomInfo.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = layoutMetrics.roomFontSizeSp.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Start
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        classroomInfo.status.forEach { status ->
                            val color = if (status == "满") {
                                Color(0xFFE82E00)
                            } else {
                                Color(0xFF34E800)
                            }

                            Spacer(
                                modifier = Modifier
                                    .padding(start = 2.dp, end = 2.dp)
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}
