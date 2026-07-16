package com.sky31.gongmultiplatform.ui.screen.academicScreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.ui.component.CustomScrollBox
import com.sky31.gongmultiplatform.ui.viewModel.AcademicViewModel

@Composable
fun AcademicMainInfoBox(
    viewModel: AcademicViewModel
) {
    val colorScheme = MaterialTheme.colorScheme

    val majorScore by viewModel.majorScore.collectAsState()
    val minorScore by viewModel.minorScore.collectAsState()

    var isMajor by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 15.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp),
            text = "总览",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight(800),
            letterSpacing = 5.sp
        )

        Spacer(
            modifier = Modifier
                .padding(top = 5.dp, bottom = 5.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.surface)
        )

        CustomScrollBox(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if(isMajor) {
                AcademicSingleInfoBox(
                    name = "绩点",
                    value = majorScore?.gpa
                )

                AcademicSingleInfoBox(
                    name = "平均成绩",
                    value = majorScore?.averageScore
                )

                AcademicSingleInfoBoxWithProgress(
                    name = "总学分",
                    values = majorScore?.totalCredit
                )

                AcademicSingleInfoBoxWithProgress(
                    name = "必修学分",
                    values = majorScore?.compulsoryCredit
                )

                AcademicSingleInfoBoxWithProgress(
                    name = "选修学分",
                    values = majorScore?.electiveCredit
                )

                AcademicSingleInfoBoxWithProgress(
                    name = "跨学科选修学分",
                    values = majorScore?.crossCourseCredit
                )
            } else {
                AcademicSingleInfoBox(
                    name = "绩点",
                    value = minorScore?.gpa
                )

                AcademicSingleInfoBoxWithProgress(
                    name = "总学分",
                    values = minorScore?.totalCredit
                )

                AcademicSingleInfoBox(
                    name = "平均成绩",
                    value = minorScore?.averageScore
                )
            }

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val y = 0f + strokeWidth / 2
                    drawLine(
                        color = colorScheme.background,   // 边框颜色
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if(isMajor) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable {
                        isMajor = true
                    }
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "主修",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if(!isMajor) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable {
                        isMajor = false
                    }
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "辅修",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AcademicSingleInfoBox(
    name: String,
    value: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 5.dp, bottom = 5.dp, start = 5.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .width(5.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Text(
            text = name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value?.takeUnless { it.isBlank() } ?: "-",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight(700),
            fontSize = 16.sp
        )
    }
}

@Composable
fun AcademicSingleInfoBoxWithProgress(
    name: String,
    values: List<String>?
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(values) {
        if (values != null) {
            val current = values[1].toFloat()
            val total = values[0].toFloat()

            progress.animateTo(
                targetValue = if (current >= total) 1f else current / total,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 5.dp, bottom = 10.dp, start = 5.dp, end = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(1.dp)
                .clip(RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .width(5.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = values?.let { "${values[1]}/${values[0]}" } ?: "-/-",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight(700),
                fontSize = 16.sp
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(5.dp)
                .padding(start = 15.dp, end = 15.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth(progress.value)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
