package com.sky31.gongmultiplatform.ui.screen.courseScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.ui.viewModel.CourseViewModel
import com.sky31.gongmultiplatform.util.MAX_COURSE_WEEK
import com.sky31.gongmultiplatform.util.parseWeeksRange

@Composable
fun CourseEditorDialog() {
    val viewModel: CourseViewModel = viewModel { CourseViewModel() }
    val editor by viewModel.editorState.collectAsState()
    editor?.let { state ->
        key(state) {
            var name by remember { mutableStateOf(state.course?.name.orEmpty()) }
            var teacher by remember { mutableStateOf(state.course?.teacher.orEmpty()) }
            var classroom by remember { mutableStateOf(state.course?.classroom.orEmpty()) }
            val weekRange = state.course?.let { parseWeeksRange(it.weeks) }
            var startWeek by remember { mutableStateOf((weekRange?.first ?: state.week).toString()) }
            var endWeek by remember { mutableStateOf((weekRange?.second ?: state.week).toString()) }
            var start by remember { mutableStateOf(state.course?.startTime?.toString() ?: state.startTime.toString()) }
            var end by remember {
                mutableStateOf(state.course?.let { (it.startTime + it.duration - 1).toString() } ?: state.startTime.toString())
            }
            val startNumber = start.toIntOrNull()
            val endNumber = end.toIntOrNull()
            val startWeekNumber = startWeek.toLongOrNull()
            val endWeekNumber = endWeek.toLongOrNull()
            val valid = name.isNotBlank() &&
                    startWeekNumber != null && endWeekNumber != null &&
                    startWeekNumber in 1..MAX_COURSE_WEEK && endWeekNumber in startWeekNumber..MAX_COURSE_WEEK &&
                    startNumber != null && endNumber != null && startNumber in 1..11 && endNumber in startNumber..11

            AlertDialog(
                onDismissRequest = viewModel::closeEditor,
                title = { Text(if (state.course == null) "自主添加课程" else "编辑课程") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name = it }, label = { Text("课程名称") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(teacher, { teacher = it }, label = { Text("老师") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(classroom, { classroom = it }, label = { Text("上课地点") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(startWeek, { startWeek = it.filter(Char::isDigit) }, label = { Text("开始周次") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(endWeek, { endWeek = it.filter(Char::isDigit) }, label = { Text("结束周次") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(start, { start = it.filter(Char::isDigit) }, label = { Text("开始节次") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(end, { end = it.filter(Char::isDigit) }, label = { Text("结束节次") }, modifier = Modifier.weight(1f))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = valid,
                        onClick = {
                            viewModel.saveCustomCourse(
                                name,
                                teacher,
                                classroom,
                                startWeek.toLong(),
                                endWeek.toLong(),
                                start.toInt(),
                                end.toInt()
                            )
                        }
                    ) { Text("保存") }
                },
                dismissButton = {
                    Row {
                        if (state.course != null) {
                            OutlinedButton(onClick = viewModel::deleteCourse) { Text("删除课程") }
                        }
                        TextButton(onClick = viewModel::closeEditor) { Text("取消") }
                    }
                }
            )
        }
    }
}
