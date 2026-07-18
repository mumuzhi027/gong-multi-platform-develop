package com.sky31.gongmultiplatform.ui.screen.academicScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.ScoreData
import kotlin.math.abs
import kotlin.math.round

@Composable
fun SingleScorePage(
    term: Int,
    scoreList: List<ScoreData.ScoreElem>
) {
    val compulsoryScoreList = scoreList.filter { scoreElem -> scoreElem.type == "必修" }
    val optionalScoreList = scoreList.filter { scoreElem -> scoreElem.type == "选修" }
    val crossCourseScoreList = scoreList.filter { scoreElem -> scoreElem.type == "跨学科选修" }

    Box(
        modifier = Modifier
            .padding(bottom = 15.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(15.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = academicTermLabel(term),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 25.sp,
                fontWeight = FontWeight(800),
                letterSpacing = 3.sp
            )

            if (compulsoryScoreList.isNotEmpty()) {
                ScorePageFragment(
                    title = "必修",
                    scoreList = compulsoryScoreList,
                    weightedAverage = calculateCompulsoryWeightedAverage(compulsoryScoreList)
                )
            }

            if (optionalScoreList.isNotEmpty()) {
                ScorePageFragment(
                    title = "选修",
                    scoreList = optionalScoreList
                )
            }

            if (crossCourseScoreList.isNotEmpty()) {
                ScorePageFragment(
                    title = "跨学科选修",
                    scoreList = crossCourseScoreList
                )
            }
        }
    }
}

internal fun academicTermLabel(term: Int): String {
    if (term <= 0) return "未知学期"

    val grade = (term + 1) / 2
    val gradeLabel = when (grade) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        else -> grade.toString()
    }
    return "大$gradeLabel${if (term % 2 == 1) "上" else "下"}"
}

@Composable
fun ScorePageFragment(
    title: String,
    scoreList: List<ScoreData.ScoreElem>,
    weightedAverage: Double? = null
) {

    Column(
        modifier = Modifier
            .padding(top = 15.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = title,
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

        // 名称-分数-学分
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .weight(3f),
                text = "名称",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight(700),
                fontSize = 18.sp
            )

            Text(
                modifier = Modifier
                    .weight(1f),
                text = "分数",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight(700),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Text(
                modifier = Modifier
                    .weight(1f),
                text = "学分",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight(700),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }

        scoreList.forEach { scoreElem ->
            SingleScore(scoreElem)
        }

        weightedAverage?.let {
            WeightedAverageScore(it)
        }
    }
}

internal fun calculateCompulsoryWeightedAverage(
    scoreList: List<ScoreData.ScoreElem>
): Double? {
    val compulsoryScores = scoreList.mapNotNull { scoreElem ->
        if (scoreElem.type != "必修") return@mapNotNull null

        val score = scoreValue(scoreElem.score) ?: return@mapNotNull null
        val credit = scoreElem.credit.toDoubleOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        score to credit
    }
    val totalCredit = compulsoryScores.sumOf { it.second }
    if (totalCredit == 0.0) return null

    return compulsoryScores.sumOf { (score, credit) -> score * credit } / totalCredit
}

internal fun scoreValue(rawScore: String): Double? {
    rawScore.trim().toDoubleOrNull()?.let { return it }

    return when (rawScore.trim()) {
        "优", "优秀" -> 95.0
        "良", "良好" -> 93.0
        "中", "中等" -> 73.0
        "及格", "合格" -> 62.0
        "不及格", "不合格" -> 30.0
        else -> null
    }
}

internal fun formatWeightedAverage(value: Double): String {
    val scaled = round(value * 100).toLong()
    val integerPart = scaled / 100
    val decimalPart = abs(scaled % 100).toString().padStart(2, '0')
    return "$integerPart.$decimalPart"
}

@Composable
private fun WeightedAverageScore(value: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(3f),
                text = "加权平均分",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight(700),
                fontSize = 16.sp
            )
            Text(
                modifier = Modifier.weight(1f),
                text = formatWeightedAverage(value),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight(800),
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun SingleScore(
    scoreElem: ScoreData.ScoreElem
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(3f),
        ) {
            Text(
                modifier = Modifier
                    .weight(3f),
                text = scoreElem.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            modifier = Modifier
                .weight(1f),
            text = scoreElem.score,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight(700),
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )

        Text(
            modifier = Modifier
                .weight(1f),
            text = scoreElem.credit,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight(700),
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
    }
}
