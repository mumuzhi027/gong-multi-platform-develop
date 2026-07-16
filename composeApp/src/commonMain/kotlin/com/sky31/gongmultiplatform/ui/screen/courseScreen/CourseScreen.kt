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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sky31.gongmultiplatform.util.CustomTime
import com.sky31.gongmultiplatform.util.TokenState
import com.sky31.gongmultiplatform.util.customTimeToString
import com.sky31.gongmultiplatform.util.getStartTime
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
    val today = currentTime.date

    var refreshing by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { calendar?.weeks ?: 0 }
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
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = pagerState,
                ) { page ->
                    val courseMap by viewModel.getWeekCourseMap(page.toLong() + 1).collectAsState(initial = null)

                    if (calendar != null && courseMap != null) {
                        val calendarStart = LocalDate.parse(calendar!!.start)
                        val weekStart = calendarStart.plus(DatePeriod(days = page * 7))

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(42.dp)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val startTime = getStartTime(weekStart)

                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${weekStart.month.ordinal + 1}",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                startTime.forEachIndexed { index, start ->
                                    val end = CustomTime(
                                        hour = start.hour + (start.minute + 45) / 60,
                                        minute = (start.minute + 45) % 60
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp)
                                            .background(MaterialTheme.colorScheme.surface),
                                        verticalArrangement = Arrangement.SpaceEvenly,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = customTimeToString(start),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = customTimeToString(end),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    if (index == 3 || index == 7) {
                                        Spacer(
                                            modifier = Modifier
                                                .height(5.dp)
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }

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
                                    highlightColor = todayTint
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
