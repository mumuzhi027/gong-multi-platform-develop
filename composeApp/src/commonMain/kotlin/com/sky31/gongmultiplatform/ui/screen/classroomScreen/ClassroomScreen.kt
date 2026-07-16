package com.sky31.gongmultiplatform.ui.screen.classroomScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.ClassroomData
import com.sky31.gongmultiplatform.ui.component.LoadingRing
import com.sky31.gongmultiplatform.ui.viewModel.ClassroomViewModel
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.TokenState
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.koin.mp.KoinPlatform.getKoin

val periodStrList = listOf("1-2", "3-4", "5-6", "7-8", "9-11")

data class ClassroomLayoutMetrics(
    val roomColumnWidth: Dp,
    val roomFontSizeSp: Int,
    val headerFontSizeSp: Int,
    val rowHeight: Dp,
    val locationChipMinWidth: Dp,
    val locationChipFontSizeSp: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen() {
    val scope = rememberCoroutineScope()
    val viewModel = getKoin().get<ClassroomViewModel>()
    val shadowColor = MaterialTheme.colorScheme.background

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val fontScale = density.fontScale
    val screenWidthDp = configuration.screenWidthDp

    val todayClassroomState by viewModel.todayClassroomState.collectAsState()
    val tomorrowClassroomState by viewModel.tomorrowClassroomState.collectAsState()

    val todayClassroomMap by viewModel.todayClassroomMap.collectAsState()
    val todayDate by viewModel.todayDate.collectAsState()
    val tomorrowClassroomMap by viewModel.tomorrowClassroomMap.collectAsState()
    val tomorrowDate by remember {
        derivedStateOf { todayDate.plus(1, DateTimeUnit.DAY) }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    val currentLocation = remember { mutableStateOf<String?>(null) }
    val currentDate by remember {
        derivedStateOf {
            if (pagerState.currentPage == 0) todayDate else tomorrowDate
        }
    }
    val currentClassroomMap by remember {
        derivedStateOf {
            if (pagerState.currentPage == 0) todayClassroomMap else tomorrowClassroomMap
        }
    }
    val currentLocationList by remember {
        derivedStateOf { currentClassroomMap?.keys?.toList().orEmpty() }
    }

    val periodStatus = remember { mutableStateListOf(false, false, false, false, false) }
    var refreshing by remember { mutableStateOf(false) }

    val layoutMetrics = remember(screenWidthDp, fontScale) {
        when {
            fontScale >= 1.35f || screenWidthDp <= 360 -> ClassroomLayoutMetrics(
                roomColumnWidth = 76.dp,
                roomFontSizeSp = 13,
                headerFontSizeSp = 13,
                rowHeight = 38.dp,
                locationChipMinWidth = 72.dp,
                locationChipFontSizeSp = 14
            )

            fontScale >= 1.15f || screenWidthDp <= 393 -> ClassroomLayoutMetrics(
                roomColumnWidth = 72.dp,
                roomFontSizeSp = 14,
                headerFontSizeSp = 14,
                rowHeight = 39.dp,
                locationChipMinWidth = 74.dp,
                locationChipFontSizeSp = 15
            )

            else -> ClassroomLayoutMetrics(
                roomColumnWidth = 68.dp,
                roomFontSizeSp = 15,
                headerFontSizeSp = 15,
                rowHeight = 40.dp,
                locationChipMinWidth = 76.dp,
                locationChipFontSizeSp = 16
            )
        }
    }

    LaunchedEffect(currentLocationList, pagerState.currentPage) {
        if (currentLocation.value !in currentLocationList) {
            currentLocation.value = currentLocationList.firstOrNull()
        }
    }

    DisposableEffect(Unit) {
        if (
            !TokenState.isOfflineMode.value &&
            (todayClassroomState is DataState.Uninitialized || tomorrowClassroomState is DataState.Uninitialized)
        ) {
            scope.launch {
                refreshing = true
                viewModel.update()
                refreshing = false
            }
        }

        onDispose {
            viewModel.resetLoadingState()
        }
    }

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
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 5.dp)
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${currentDate.month.ordinal + 1}月${currentDate.day}日",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight(800)
                )

                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = if (pagerState.currentPage == 0) "今天" else "明天",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight(800)
                )
            }

            ClassroomTopBar(pagerState = pagerState)

            if (currentLocationList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(top = 5.dp, bottom = 5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.width(20.dp))

                        currentLocationList.forEach { location ->
                            val backgroundColor =
                                if (location == currentLocation.value) MaterialTheme.colorScheme.primary else Color.Transparent
                            Text(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = layoutMetrics.locationChipMinWidth)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        currentLocation.value = location
                                    }
                                    .background(backgroundColor)
                                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                                text = location,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = layoutMetrics.locationChipFontSizeSp.sp,
                                fontWeight = FontWeight(800),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to shadowColor,
                                    0.03f to Color.Transparent,
                                    0.97f to Color.Transparent,
                                    1.0f to shadowColor
                                )
                            ),
                            size = size
                        )
                    }
                }
            }

            if (currentLocationList.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 5.dp, start = 15.dp, end = 15.dp)
                        .height(30.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.width(layoutMetrics.roomColumnWidth),
                        text = "教室",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = layoutMetrics.headerFontSizeSp.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        periodStatus.forEachIndexed { index, selected ->
                            val fontColor =
                                if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            val backgroundColor =
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .padding(start = 5.dp, end = 5.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        periodStatus[index] = !periodStatus[index]
                                    }
                                    .background(backgroundColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = periodStrList[index],
                                    color = fontColor,
                                )
                            }
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageState = if (page == 0) todayClassroomState else tomorrowClassroomState
                val pageMap = if (page == 0) todayClassroomMap else tomorrowClassroomMap
                val classroomList = currentLocation.value?.let { pageMap?.get(it) }.orEmpty()

                ClassroomPageState(
                    state = pageState,
                    hasLocations = pageMap?.isNotEmpty() == true,
                    classroomList = classroomList,
                    checkStatus = periodStatus,
                    layoutMetrics = layoutMetrics
                )
            }
        }
    }
}

@Composable
private fun ClassroomPageState(
    state: DataState,
    hasLocations: Boolean,
    classroomList: List<ClassroomData.ClassroomInfo>,
    checkStatus: List<Boolean>,
    layoutMetrics: ClassroomLayoutMetrics
) {
    when {
        state is DataState.Loading && !hasLocations -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingRing()
            }
        }

        state is DataState.Error -> {
            ClassroomMessage(state.message)
        }

        state is DataState.Unauthorized -> {
            ClassroomMessage("登录状态已失效，请重新登录")
        }

        !hasLocations -> {
            ClassroomMessage("当前暂无空教室数据")
        }

        else -> {
            ClassroomSubScreen(
                list = classroomList,
                checkStatus = checkStatus,
                layoutMetrics = layoutMetrics
            )
        }
    }
}

@Composable
private fun ClassroomMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
