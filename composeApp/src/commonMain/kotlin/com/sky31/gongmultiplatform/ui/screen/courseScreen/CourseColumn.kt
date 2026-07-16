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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.ui.theme.CourseColor
import com.sky31.gongmultiplatform.ui.viewModel.CourseViewModel
import kotlin.math.absoluteValue

fun generateCourseColor(courseName: String): Color {
    val hash = courseName.hashCode().absoluteValue
    val colors = CourseColor.entries.toTypedArray()

    return Color(colors[hash % 10].rgb)
}

@Composable
fun CourseColumn(
    modifier: Modifier = Modifier,
    courseList: List<CourseElem> = emptyList(),
    highlighted: Boolean = false,
    highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
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
                modifier = Modifier.weight(4f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            CourseColumnFragment(
                courseList = courseList,
                start = 5,
                end = 8,
                modifier = Modifier.weight(4f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            CourseColumnFragment(
                courseList = courseList,
                start = 9,
                end = 11,
                modifier = Modifier.weight(3f)
            )
        }
    }
}

@Composable
fun CourseColumnFragment(
    modifier: Modifier = Modifier,
    courseList: List<CourseElem>,
    start: Int,
    end: Int
) {
    val viewModel: CourseViewModel = viewModel { CourseViewModel() }

    Column(modifier = modifier) {
        var index = start
        courseList
            .filter { course -> course.startTime in start..end }
            .forEach { course ->
                val courseColor = generateCourseColor(course.name)
                val longestTextLength = maxOf(course.name.length, course.classroom.length)
                val nameFontSize = when {
                    course.duration >= 3 -> 12.sp
                    course.duration == 2 -> 11.sp
                    longestTextLength >= 11 -> 9.5.sp
                    longestTextLength >= 8 -> 10.sp
                    else -> 11.sp
                }
                val roomFontSize = when {
                    course.duration >= 3 -> 11.sp
                    course.duration == 2 -> 10.sp
                    longestTextLength >= 11 -> 9.sp
                    longestTextLength >= 8 -> 9.5.sp
                    else -> 10.sp
                }
                val nameStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = nameFontSize,
                    lineHeight = (nameFontSize.value + 1f).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                val classroomStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = roomFontSize,
                    lineHeight = (roomFontSize.value + 1f).sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                val nameMaxLines = if (course.duration >= 2) 4 else 3
                val classroomMaxLines = if (course.duration >= 2) 4 else 3

                if (course.startTime > index) {
                    Spacer(modifier = Modifier.weight((course.startTime - index).toFloat()))
                }

                Column(
                    modifier = Modifier
                        .weight(course.duration.toFloat())
                        .padding(horizontal = 2.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            viewModel.setSheetCourse(course)
                            viewModel.showSheet()
                        }
                        .background(courseColor)
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Text(
                            text = course.name,
                            maxLines = nameMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            style = nameStyle
                        )

                        if (course.classroom.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = course.classroom,
                                style = classroomStyle,
                                maxLines = classroomMaxLines,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                index = course.startTime + course.duration
            }

        if (index <= end) {
            Spacer(modifier = Modifier.weight((end - index + 1).toFloat()))
        }
    }
}
