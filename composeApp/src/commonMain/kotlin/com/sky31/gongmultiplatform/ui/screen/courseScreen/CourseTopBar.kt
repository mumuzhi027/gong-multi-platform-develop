package com.sky31.gongmultiplatform.ui.screen.courseScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky31.gongmultiplatform.di.LocalAppNavController
import com.sky31.gongmultiplatform.ui.viewModel.CourseViewModel
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun CourseTopBar(
    state: PagerState
) {
    val scope = rememberCoroutineScope()
    val viewModel: CourseViewModel = viewModel { CourseViewModel() }
    val navController = LocalAppNavController.current

    val calendar by viewModel.calendar.collectAsState()
    val currentWeekNum by viewModel.currentWeekNum.collectAsState()
    var weekListState by remember { mutableStateOf(false) }

    val displayedWeek = state.currentPage + 1
    val isCurrentWeek = currentWeekNum > 0 && displayedWeek == currentWeekNum.toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = weekListState,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith
                            fadeOut(tween(300)) using SizeTransform(clip = false)
                },
                label = "animatedWeekList",
            ) { targetState ->
                if (targetState) {
                    if (calendar != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Spacer(modifier = Modifier.width(25.dp))

                                for (index in 1..calendar!!.weeks) {
                                    val selected = index == displayedWeek
                                    Text(
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp)
                                            .clickable {
                                                scope.launch { state.scrollToPage(index - 1) }
                                                weekListState = false
                                            },
                                        text = index.toString(),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }

                                Spacer(modifier = Modifier.width(25.dp))
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.clickable { weekListState = true },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "第${displayedWeek}周",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (isCurrentWeek) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "本周",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "非本周",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        navController.navigate("main")
                    }
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.baseline_arrow_back_ios_new_24),
                    contentDescription = "back_arrow",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
