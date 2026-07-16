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
import com.sky31.gongmultiplatform.util.chineseNumberMap
import kotlin.math.floor

@Composable
fun SingleScorePage(
    term: Int,
    scoreList: List<ScoreData.ScoreElem>
) {
    val compulsoryScoreList = scoreList.filter { scoreElem -> scoreElem.type == "必修" }
    val optionalScoreList = scoreList.filter { scoreElem -> scoreElem.type == "选修" }
    val termStr =
        "大${chineseNumberMap[floor((term - 1) / 2f + 1).toInt()]}${if (term % 2 == 1) "上" else "下"}"

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
                text = termStr,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 25.sp,
                fontWeight = FontWeight(800),
                letterSpacing = 3.sp
            )

            if (compulsoryScoreList.isNotEmpty()) {
                ScorePageFragment(
                    title = "必修",
                    scoreList = compulsoryScoreList
                )
            }

            if (optionalScoreList.isNotEmpty()) {
                ScorePageFragment(
                    title = "选修",
                    scoreList = optionalScoreList
                )
            }
        }
    }
}

@Composable
fun ScorePageFragment(
    title: String,
    scoreList: List<ScoreData.ScoreElem>
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