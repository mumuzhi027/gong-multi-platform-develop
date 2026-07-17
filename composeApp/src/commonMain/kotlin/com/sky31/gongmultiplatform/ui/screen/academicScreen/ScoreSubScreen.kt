package com.sky31.gongmultiplatform.ui.screen.academicScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    var selectedTerm by remember { mutableStateOf<Int?>(null) }

    val scoreMap = remember(isMajor, majorScore, minorScore) {
        val academicInfo = if (isMajor) majorScore else minorScore
        academicInfo?.scores.orEmpty().groupBy(ScoreData.ScoreElem::term)
    }
    val terms = remember(scoreMap) {
        scoreMap.keys.sortedDescending()
    }

    LaunchedEffect(isMajor, terms) {
        selectedTerm = terms.firstOrNull()
    }

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
            ScoreTypeButton(
                text = "主修",
                selected = isMajor,
                onClick = { isMajor = true }
            )
            ScoreTypeButton(
                text = "辅修",
                selected = !isMajor,
                onClick = { isMajor = false }
            )
        }

        if (terms.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = terms,
                    key = { it }
                ) { term ->
                    TermButton(
                        term = term,
                        selected = term == selectedTerm,
                        onClick = { selectedTerm = term }
                    )
                }
            }
        }

        selectedTerm?.let { term ->
            scoreMap[term]?.let { scores ->
                SingleScorePage(term, scores)
            }
        }
    }
}

@Composable
private fun ScoreTypeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .padding(vertical = 2.dp, horizontal = 25.dp)
    ) {
        Text(
            text = text,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun TermButton(
    term: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .padding(vertical = 7.dp, horizontal = 14.dp)
    ) {
        Text(
            text = academicTermLabel(term),
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
