package com.sky31.gongmultiplatform.ui.screen.mainScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sky31.gongmultiplatform.di.LocalAppNavController
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.ui.component.CustomScrollBox
import com.sky31.gongmultiplatform.ui.viewModel.MainViewModel
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.TokenState
import com.sky31.gongmultiplatform.util.customTimeToString
import com.sky31.gongmultiplatform.util.getCourseColor
import com.sky31.gongmultiplatform.util.getCourseState
import com.sky31.gongmultiplatform.util.getCourseTime
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.baseline_refresh_24
import gongmultiplatform.composeapp.generated.resources.course
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.painterResource

@Composable
fun VerticalCourseBox(
    viewModel: MainViewModel
) {
    val scope = rememberCoroutineScope()
    val navController = LocalAppNavController.current
    val colorScheme = MaterialTheme.colorScheme

    val currentTime by viewModel.currentTime.collectAsState()
    val courseList by viewModel.courseList.collectAsState()

    val courseBoxState by viewModel.courseBoxState.collectAsState()

    // 加载时动态模糊效果
    val blurValue = remember { Animatable(0f) }

    LaunchedEffect(courseBoxState) {
        when(courseBoxState) {
            is DataState.Loading -> {
                blurValue.animateTo(
                    targetValue = 10f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            is DataState.Newest,
            is DataState.Uninitialized -> {
                blurValue.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            else -> {
                blurValue.snapTo(10f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            CourseBoxTopBar(viewModel)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .blur(blurValue.value.dp)
                        .drawWithContent {
                            drawContent() // 先绘制 Box 原内容

                            // 绘制遮罩层
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to colorScheme.background,
                                        0.1f to Color.Transparent,
                                        0.9f to Color.Transparent,
                                        1.0f to colorScheme.background
                                    ),
                                    startY = 0f,
                                    endY = size.height
                                ),
                                size = size,
                                blendMode = BlendMode.SrcOver // 默认即可，用于覆盖在内容上
                            )
                        }
                        .padding(start = 5.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (courseList.isEmpty()) {
                        NoCourseBox(navController)
                    } else {
                        CustomScrollBox(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            Alignment.CenterHorizontally
                        ) {
                            courseList.forEach { it ->
                                CourseBoxElem(it, currentTime)
                            }
                        }
                    }
                }

                /* 重新加载overlay */
                AnimatedContent(
                    targetState = courseBoxState !is DataState.Newest &&
                            courseBoxState !is DataState.Loading &&
                            courseBoxState !is DataState.Uninitialized
                ) {targetState ->
                    if(targetState) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                // 消耗点击事件，防止下层响应
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes.forEach { it.consume() } // 消费掉事件，防止下传
                                        }
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_refresh_24),
                                contentDescription = "loadingRing",
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        scope.launch {
                                            if (TokenState.requestVerificationIfOffline()) return@launch

                                            viewModel.updateCourseBox()
                                        }
                                    }
                            )

                            Text(
                                text = "重新加载",
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoCourseBox(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "今日无课",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = {
                navController.navigate("courseScreen")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,  // 背景透明
                contentColor = Color.Unspecified
            ),
            contentPadding = PaddingValues(start = 25.dp, end = 25.dp),
            modifier = Modifier.defaultMinSize(0.dp, 0.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.course),
                    contentDescription = "course",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp)
                )

                Text(
                    text = "课程表",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun CourseBoxElem(
    course: CourseElem,
    currentTime: LocalDateTime
) {
    // 课程容器bar的颜色
    val courseColor = remember(currentTime) {
        val courseState = getCourseState(currentTime, course.startTime, course.duration)
        getCourseColor(courseState)
    }

    // 课程时间
    val courseTime = remember { mutableStateOf(getCourseTime(course, currentTime)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .width(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = customTimeToString(courseTime.value[0]),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "-",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = customTimeToString(courseTime.value[1]),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(courseColor)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.inverseOnSurface)
                .padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(.5f)
                    .padding(start = 4.dp, end = 10.dp)
                    .width(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(courseColor)
                    .align(Alignment.CenterVertically)
            )

            // 课程主要信息
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = course.name,
                    fontWeight = FontWeight(600),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = course.classroom,
                    fontWeight = FontWeight(500),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
