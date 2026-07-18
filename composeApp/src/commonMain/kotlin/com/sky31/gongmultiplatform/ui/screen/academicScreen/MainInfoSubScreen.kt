package com.sky31.gongmultiplatform.ui.screen.academicScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.RankData
import com.sky31.gongmultiplatform.ui.component.CustomScrollBox
import com.sky31.gongmultiplatform.ui.viewModel.AcademicViewModel

private fun formatAcademicValue(value: String?): String {
    return value?.takeUnless { it.isBlank() || it == "0" } ?: "-"
}

private fun formatRankValue(value: Int?): String {
    return value?.takeIf { it > 0 }?.toString() ?: "-"
}

private enum class StatisticsType(val label: String) {
    Compulsory("必修"),
    All("必+选")
}

@Composable
fun MainInfoSubScreen(viewModel: AcademicViewModel) {
    val majorScore by viewModel.majorScore.collectAsState()
    val compulsoryRank by viewModel.compulsoryRank.collectAsState()
    val totalRank by viewModel.totalRank.collectAsState()

    var statisticsType by remember { mutableStateOf(StatisticsType.All) }

    val currentRank: RankData? = when (statisticsType) {
        StatisticsType.Compulsory -> compulsoryRank
        StatisticsType.All -> totalRank
    }
    val cumulativeCompulsoryWeightedAverage = remember(majorScore) {
        majorScore?.scores
            ?.let(::calculateCompulsoryWeightedAverage)
            ?.let(::formatWeightedAverage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 10.dp, end = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .fillMaxWidth()
                    .weight(3f)
            ) {
                AcademicMainInfoBox(viewModel)
            }

            Box(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(15.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "英语",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
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

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            AcademicSingleInfoBox(
                                name = "CET4",
                                value = formatAcademicValue(majorScore?.cet4)
                            )
                        }

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            AcademicSingleInfoBox(
                                name = "CET6",
                                value = formatAcademicValue(majorScore?.cet6)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .fillMaxWidth()
                    .weight(2f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "统计",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
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

                    StatisticsTypeSwitcher(
                        selectedType = statisticsType,
                        onTypeSelected = { statisticsType = it }
                    )

                    CustomScrollBox(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AcademicSingleInfoBox(
                            name = "平均学分绩点",
                            value = formatAcademicValue(currentRank?.gpa)
                        )

                        AcademicSingleInfoBox(
                            name = "平均成绩",
                            value = formatAcademicValue(currentRank?.averageScore)
                        )

                        AcademicSingleInfoBox(
                            name = "班级排名",
                            value = formatRankValue(currentRank?.classRank)
                        )

                        AcademicSingleInfoBox(
                            name = "年级专业排名",
                            value = formatRankValue(currentRank?.majorRank)
                        )

                        if (statisticsType == StatisticsType.Compulsory) {
                            AcademicSingleInfoBox(
                                name = "加权平均分",
                                value = cumulativeCompulsoryWeightedAverage
                            )

                            AcademicSingleInfoBox(
                                name = "加权平均成绩排名",
                                value = formatRankValue(compulsoryRank?.weightedAverageRank)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsTypeSwitcher(
    selectedType: StatisticsType,
    onTypeSelected: (StatisticsType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatisticsType.entries.forEach { type ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selectedType == type) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(700)
                )
            }
        }
    }
}
