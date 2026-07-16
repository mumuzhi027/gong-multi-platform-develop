package com.sky31.gongmultiplatform.ui.screen.mainScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.di.LocalAppNavController
import com.sky31.gongmultiplatform.ui.component.CircleProgressBar
import com.sky31.gongmultiplatform.ui.component.DataLoadingRing
import com.sky31.gongmultiplatform.ui.viewModel.MainViewModel
import com.sky31.gongmultiplatform.util.AnimationState
import com.sky31.gongmultiplatform.util.CourseState
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.TokenState
import com.sky31.gongmultiplatform.util.getCourseState
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.course
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun CourseBoxTopBar(
    viewModel: MainViewModel
) {
    val navController = LocalAppNavController.current

    val scope = rememberCoroutineScope()

    val completedCourseNum by viewModel.completedCourseNum.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val courseList by viewModel.courseList.collectAsState()

    val courseBoxState by viewModel.courseBoxState.collectAsState()
    val refreshState by remember {
        derivedStateOf {
            when (courseBoxState) {
                is DataState.Loading,
                is DataState.Expired -> AnimationState.Loading
                is DataState.Uninitialized -> AnimationState.Unstarted
                else -> AnimationState.Finished
            }
        }
    }

    // 更新进度条
    LaunchedEffect(courseList, currentTime) {
        if (courseList.isEmpty()) {
            viewModel.setProgression(1f)
            return@LaunchedEffect
        }

        var accomplishment = 0f
        courseList.forEach {
            if (getCourseState(
                    currentTime,
                    it.startTime,
                    it.duration
                ) is CourseState.Before
            )
                accomplishment += 1f
        }
        viewModel.setProgression(accomplishment / courseList.size)
        viewModel.setCompletedNum(accomplishment.toInt())
    }

    Row(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 10.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            color = MaterialTheme.colorScheme.onBackground,
            text = "今日课程",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = 5.dp, bottom = 5.dp)
        )

        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .size(42.dp)
        ) {
            CircleProgressBar(
                targetFlow = viewModel.progression,
                courseCount = courseList.size
            )
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "还剩",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = "${courseList.size - completedCourseNum}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                )

                Text(
                    text = "节课",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(
            modifier = Modifier
                .weight(1f)
        )

        IconButton(
            onClick = {
                scope.launch {
                    if (TokenState.requestVerificationIfOffline()) return@launch

                    viewModel.updateCourseBox()
                }
            }
        ) {
            DataLoadingRing(
                state = refreshState
            )
        }

        IconButton(
            onClick = {
                navController.navigate("courseScreen")
            }
        ) {
            Icon(
                painter = painterResource(Res.drawable.course),
                contentDescription = "course",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(20.dp)
            )
        }
    }
}
