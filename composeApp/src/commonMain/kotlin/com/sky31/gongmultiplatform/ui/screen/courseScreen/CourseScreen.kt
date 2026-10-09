package com.sky31.gongmultiplatform.ui.screen.courseScreen

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.ui.viewModel.CourseViewModel
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.TokenState
import com.sky31.gongmultiplatform.util.reverseWeekdayNameMap
import com.sky31.gongmultiplatform.util.weekdayNameMapCN
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen() {
    val scope = rememberCoroutineScope()
    val viewModel: CourseViewModel = viewModel { CourseViewModel() }

    val currentTime by viewModel.currentTime.collectAsState()
    val currentWeekNum by viewModel.currentWeekNum.collectAsState()
    val calendar by viewModel.calendar.collectAsState()
    val courseMapState by viewModel.courseMapState.collectAsState()
    val staggeredTimeGroups by viewModel.staggeredTimeGroups.collectAsState()
    val today = currentTime.date

    var refreshing by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { calendar?.weeks?.coerceAtLeast(0) ?: 0 }
    )

    LaunchedEffect(currentWeekNum) {
        if (currentWeekNum > 0) {
            pagerState.scrollToPage(currentWeekNum.toInt() - 1)
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            CourseTopBar(
                state = pagerState
            )
        }
    ) { innerPadding ->
        CourseBottomSheet()
        CourseEditorDialog()

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    if (TokenState.requestVerificationIfOffline()) {
                        refreshing = false
                        return@launch
                    }

                    viewModel.update()
                    refreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) {
            Column(
                // 分页课表使用 weight 分配剩余高度，父容器必须保留有限的高度约束。
                // verticalScroll 会让高度变成无限，分页区域会被测量为零高。
                modifier = Modifier.fillMaxSize()
            ) courseContent@{
                if (calendar == null || calendar!!.weeks <= 0 ||
                    runCatching { LocalDate.parse(calendar!!.start) }.isFailure
                ) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (refreshing || courseMapState is DataState.Loading) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("正在加载课表", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("暂无校历数据，无法显示课表", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = {
                                    scope.launch {
                                        if (TokenState.requestVerificationIfOffline()) return@launch
                                        refreshing = true
                                        try {
                                            viewModel.update()
                                        } finally {
                                            refreshing = false
                                        }
                                    }
                                }) { Text("重新加载") }
                            }
                        }
                    }
                    return@courseContent
                }
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = pagerState,
                ) { page ->
                    val courseMap by viewModel.getWeekCourseMap(page.toLong() + 1).collectAsState(initial = null)

                    val calendarStart = calendar?.start?.let { value ->
                        runCatching { LocalDate.parse(value) }.getOrNull()
                    }
                    if (calendar != null && courseMap != null && calendarStart != null) {
                        val weekStart = calendarStart.plus(DatePeriod(days = page * 7))

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                        ) {
                            CourseTimeAxis(
                                date = weekStart,
                                staggeredGroups = staggeredTimeGroups,
                                onToggleGroup = viewModel::toggleTimeGroup,
                                onToggleAll = viewModel::toggleAllTimes,
                                modifier = Modifier
                                    .width(42.dp)
                                    .fillMaxHeight()
                            )

                            courseMap!!.keys.forEachIndexed { index, item ->
                                val date = calendarStart.plus(DatePeriod(days = page * 7 + index))
                                val isTodayColumn = currentWeekNum > 0 &&
                                        page == currentWeekNum.toInt() - 1 &&
                                        date == today
                                val backgroundLuminance = MaterialTheme.colorScheme.background.luminance()
                                val todayTint = if (backgroundLuminance > 0.5f) {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                                } else {
                                    Color.White.copy(alpha = 0.12f)
                                }
                                val todayHeaderTint = if (backgroundLuminance > 0.5f) {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
                                } else {
                                    Color.White.copy(alpha = 0.16f)
                                }
                                val todayTextColor = if (backgroundLuminance > 0.5f) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    Color.White
                                }

                                CourseColumn(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    courseList = courseMap!![item]
                                        ?.sortedBy { course -> course.startTime }
                                        ?: emptyList(),
                                    highlighted = isTodayColumn,
                                    highlightColor = todayTint,
                                    day = item,
                                    week = page.toLong() + 1
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 1.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isTodayColumn) todayHeaderTint else MaterialTheme.colorScheme.surface
                                            )
                                            .padding(vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = weekdayNameMapCN.getValue(
                                                reverseWeekdayNameMap.getValue(item)
                                            ),
                                            color = if (isTodayColumn) {
                                                todayTextColor
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )

                                        Text(
                                            text = (date.month.ordinal + 1).toString().padStart(2, '0') +
                                                    "-" +
                                                    date.day.toString().padStart(2, '0'),
                                            color = if (isTodayColumn) {
                                                todayTextColor
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
