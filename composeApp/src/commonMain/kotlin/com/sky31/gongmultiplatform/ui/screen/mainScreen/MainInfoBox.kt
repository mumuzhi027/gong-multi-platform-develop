package com.sky31.gongmultiplatform.ui.screen.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.ui.viewModel.MainViewModel
import com.sky31.gongmultiplatform.util.getWeekNum
import com.sky31.gongmultiplatform.util.weekdayNameMapCN

/**
 * mainScreen的主要信息容器
 */
@Composable
fun MainInfoBox(viewModel: MainViewModel) {
    val currentTime by viewModel.currentTime.collectAsState()
    val calendar by viewModel.calendar.collectAsState()

    var weekNum by remember { mutableLongStateOf(0) }

    LaunchedEffect(calendar) {
        calendar?.let { weekNum = getWeekNum(it, currentTime) }
    }


    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(50.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .height(30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${weekdayNameMapCN[currentTime.dayOfWeek.ordinal + 1]}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight(800),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(30.dp)
                        )

                        Text(
                            text = "第",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            modifier = Modifier
                                .padding(start = 5.dp, end = 5.dp),
                            text = "$weekNum",
                            fontWeight = FontWeight(800),
                            fontSize = 25.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "周",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${currentTime.year}.${
                            (currentTime.month.ordinal + 1).toString().padStart(2, '0')
                        }.${currentTime.day.toString().padStart(2, '0')}",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}