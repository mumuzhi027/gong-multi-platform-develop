package com.sky31.gongmultiplatform.ui.screen.courseScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.ui.viewModel.CourseViewModel
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.baseline_access_time_filled_24
import gongmultiplatform.composeapp.generated.resources.baseline_location_on_24
import gongmultiplatform.composeapp.generated.resources.course
import gongmultiplatform.composeapp.generated.resources.teacher
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseBottomSheet() {
    val viewModel: CourseViewModel = viewModel { CourseViewModel() }

    val sheetCourse by viewModel.sheetCourse.collectAsState()
    val sheetVisible by viewModel.sheetVisible.collectAsState()

    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(sheetVisible) {
        println(sheetVisible)
        if(sheetVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if(sheetVisible) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { viewModel.hideSheet() }
        ) {
            sheetCourse?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, bottom = 10.dp, end = 15.dp, top = 10.dp)
                ) {
                    Text(
                        text = it.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.teacher),
                                contentDescription = "location",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                            )

                            Text(
                                text = it.teacher,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.course),
                                contentDescription = "location",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                            )

                            Text(
                                text = "${it.weeks}周",
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_location_on_24),
                                contentDescription = "location",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                            )

                            Text(
                                text = it.classroom,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_access_time_filled_24),
                                contentDescription = "location",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                            )

                            Text(
                                text = "${it.startTime}-${it.startTime + it.duration - 1}节",
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}