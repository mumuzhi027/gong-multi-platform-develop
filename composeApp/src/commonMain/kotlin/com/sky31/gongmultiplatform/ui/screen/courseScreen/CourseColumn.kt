package com.sky31.gongmultiplatform.ui.screen.courseScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.ui.theme.CourseColor
import com.sky31.gongmultiplatform.ui.viewModel.CourseViewModel
import com.sky31.gongmultiplatform.util.CourseTimeMode
import com.sky31.gongmultiplatform.util.courseSlots

fun generateCourseColor(courseName: String): Color {
    val colors = CourseColor.entries.toTypedArray()
    val index = (courseName.hashCode() and Int.MAX_VALUE) % colors.size

    return Color(colors[index].rgb)
}

@Composable
fun CourseColumn(
    modifier: Modifier = Modifier,
    courseList: List<CourseElem> = emptyList(),
    highlighted: Boolean = false,
    highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    day: String,
    week: Long,
    timeMode: CourseTimeMode = CourseTimeMode.MIXED,
    periodCount: Int = 11,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        if (highlighted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 1.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(highlightColor)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()

            CourseColumnFragment(
                courseList = courseList,
                start = 1,
                end = 4,
                day = day,
                week = week,
                timeMode = timeMode,
                modifier = Modifier.weight(4f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            CourseColumnFragment(
                courseList = courseList,
                start = 5,
                end = 8,
                day = day,
                week = week,
                timeMode = timeMode,
                modifier = Modifier.weight(4f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            CourseColumnFragment(
                courseList = courseList,
                start = 9,
                end = periodCount,
                day = day,
                week = week,
                timeMode = timeMode,
                modifier = Modifier.weight((periodCount - 8).toFloat())
            )
        }
    }
}

@Composable
fun CourseColumnFragment(
    modifier: Modifier = Modifier,
    courseList: List<CourseElem>,
    start: Int,
    end: Int,
    day: String,
    week: Long,
    timeMode: CourseTimeMode = CourseTimeMode.MIXED
) {
    val viewModel: CourseViewModel = viewModel { CourseViewModel() }

    Column(modifier = modifier) {
        var index = start
        courseSlots(courseList, start, end).forEach { slot ->
            if (slot.startTime > index) {
                repeat(slot.startTime - index) { offset ->
                    val period = index + offset
                    EmptyCourseSlot(Modifier.weight(1f)) {
                        if (period <= 11) viewModel.openEditor(day, week, period)
                    }
                }
            }
            CourseSlotCard(slot, week, timeMode, onOpen = { course ->
                if (course.isCustom) viewModel.openEditor(day, week, course.startTime, course)
                else {
                    viewModel.setSheetCourse(course, week)
                    viewModel.showSheet()
                }
            }, modifier = Modifier.weight(slot.duration.toFloat()))
            index = slot.startTime + slot.duration
        }

        if (index <= end) {
            repeat(end - index + 1) { offset ->
                val slot = index + offset
                EmptyCourseSlot(
                    modifier = Modifier.weight(1f),
                    onClick = { if (slot <= 11) viewModel.openEditor(day, week, slot) }
                )
            }
        }
    }
}

@Composable
private fun EmptyCourseSlot(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("＋", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .28f), fontSize = 12.sp)
    }
}
