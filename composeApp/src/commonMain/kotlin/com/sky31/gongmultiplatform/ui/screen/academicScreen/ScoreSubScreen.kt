package com.sky31.gongmultiplatform.ui.screen.academicScreen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.model.ScoreData
import com.sky31.gongmultiplatform.ui.viewModel.AcademicViewModel

@Composable
fun ScoreSubScreen(viewModel: AcademicViewModel) {
    val majorScore by viewModel.majorScore.collectAsState()
    val minorScore by viewModel.minorScore.collectAsState()

    var isMajor by remember { mutableStateOf(true) }

    val scoreMap by remember {
        derivedStateOf {
            val result = mutableMapOf<Int, MutableList<ScoreData.ScoreElem>>()

            val academicInfo = if(isMajor) majorScore else minorScore

            academicInfo?.scores?.forEach { scoreElem ->
                if (!result.containsKey(scoreElem.term)) {
                    result[scoreElem.term] = mutableListOf(scoreElem)
                } else {
                    result[scoreElem.term]!!.add(scoreElem)
                }
            }

            result
        }
    }
    val keys by remember {
        derivedStateOf {
            scoreMap.keys.toList().sortedByDescending { it }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { scoreMap.size }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, start = 10.dp, end = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        isMajor = true
                    }
                    .background(if(isMajor) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .padding(top = 2.dp, bottom = 2.dp, start = 25.dp, end = 25.dp)
            ) {
                Text(
                    text = "主修",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        isMajor = false
                    }
                    .background(if(!isMajor) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .padding(top = 2.dp, bottom = 2.dp, start = 25.dp, end = 25.dp)
            ) {
                Text(
                    text = "辅修",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        VerticalPager(
            state = pagerState,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(
                    stiffness = Spring.StiffnessMedium
                ),
                snapPositionalThreshold = 0.2f
            )
        ) { page ->
            scoreMap[keys[page]]?.let {
                SingleScorePage(keys[page], it)
            }
        }
    }

}