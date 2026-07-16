package com.sky31.gongmultiplatform.ui.screen.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.util.CustomTime
import com.sky31.gongmultiplatform.util.customTimeToString
import com.sky31.gongmultiplatform.util.parseLocalDateTimeOrNull
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.baseline_access_time_filled_24
import gongmultiplatform.composeapp.generated.resources.baseline_location_on_24
import kotlinx.datetime.daysUntil
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.painterResource

private const val TODAY_LABEL = "\u4eca\u5929"
private const val DAY_LABEL = "\u5929"
private const val FINISHED_LABEL = "\u5df2\u7ed3\u675f"
private const val NO_LOCATION_LABEL = "\u65e0\u5730\u70b9\u5b89\u6392"
private const val NO_TIME_LABEL = "\u65e0\u65f6\u95f4\u5b89\u6392"

private sealed interface ExamCountdownState {
    data object Today : ExamCountdownState
    data object Finished : ExamCountdownState
    data class Days(val days: Int) : ExamCountdownState
}

@Composable
fun ExamBox(exam: ExamElem, currentTime: LocalDateTime) {
    val parsedStartTime = remember(exam.startTime) {
        parseLocalDateTimeOrNull(exam.startTime, "exam startTime")
    }
    val parsedEndTime = remember(exam.endTime) {
        parseLocalDateTimeOrNull(exam.endTime, "exam endTime")
    }
    val nameScrollState = rememberScrollState()
    val locationScrollState = rememberScrollState()
    val examDateText = parsedStartTime?.let { "${it.monthNumber}\u6708${it.dayOfMonth}\u65e5" }

    val countdownState by remember(currentTime, exam.startTime, exam.endTime) {
        mutableStateOf(
            when {
                parsedStartTime == null -> null
                parsedEndTime != null && currentTime >= parsedEndTime -> ExamCountdownState.Finished
                currentTime.date > parsedStartTime.date -> ExamCountdownState.Finished
                currentTime.date == parsedStartTime.date -> ExamCountdownState.Today
                else -> ExamCountdownState.Days(currentTime.date.daysUntil(parsedStartTime.date))
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(.42f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(nameScrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exam.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight(600),
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!examDateText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .weight(.18f)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = examDateText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight(600),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.weight(.26f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                countdownState?.let { state ->
                    when (state) {
                        is ExamCountdownState.Days -> {
                            Text(
                                text = "${state.days}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight(600),
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = DAY_LABEL,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        ExamCountdownState.Today -> {
                            Text(
                                text = TODAY_LABEL,
                                fontSize = 20.sp,
                                fontWeight = FontWeight(600),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        ExamCountdownState.Finished -> {
                            Text(
                                text = FINISHED_LABEL,
                                fontSize = 20.sp,
                                fontWeight = FontWeight(600),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.baseline_location_on_24),
                    contentDescription = "location",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp)
                )

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(locationScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exam.location.ifBlank { NO_LOCATION_LABEL },
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.baseline_access_time_filled_24),
                    contentDescription = "time",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp)
                )

                when {
                    parsedStartTime != null && parsedEndTime != null -> {
                        val startTime = CustomTime(parsedStartTime.hour, parsedStartTime.minute)
                        val endTime = CustomTime(parsedEndTime.hour, parsedEndTime.minute)

                        Text(
                            text = "${customTimeToString(startTime)}-${customTimeToString(endTime)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    exam.startTime.isNotBlank() -> {
                        Text(
                            text = exam.startTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    else -> {
                        Text(
                            text = NO_TIME_LABEL,
                            fontSize = 12.sp,
                            fontWeight = FontWeight(600),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
