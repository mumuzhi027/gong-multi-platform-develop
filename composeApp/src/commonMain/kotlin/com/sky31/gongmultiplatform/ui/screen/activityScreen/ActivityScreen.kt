package com.sky31.gongmultiplatform.ui.screen.activityScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.CampusActivity
import com.sky31.gongmultiplatform.model.currentActivityDate
import com.sky31.gongmultiplatform.model.durationLabel
import com.sky31.gongmultiplatform.model.isPastActivity
import com.sky31.gongmultiplatform.ui.viewModel.ActivityViewModel
import org.koin.mp.KoinPlatform.getKoin
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.filter_list
import org.jetbrains.compose.resources.painterResource

private enum class ActivityPeriod(val label: String) {
    Current("近期活动"),
    Past("往期活动")
}

@Composable
fun ActivityScreen(onActivityClick: (String) -> Unit) {
    val viewModel = getKoin().get<ActivityViewModel>()
    LaunchedEffect(viewModel) {
        // The singleton may have made its first request while login/session setup
        // was still settling. Entering the page should always get one fresh chance.
        viewModel.refresh()
    }
    val activities by viewModel.activities.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val usingFallback by viewModel.usingFallback.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val today = currentActivityDate()
    val currentActivities = activities.filterNot { it.isPastActivity(today) }
    val pastActivities = activities
        .filter { it.isPastActivity(today) }
        .sortedByDescending { it.endDate.replace('.', '-') }
    var selectedPeriod by remember { mutableStateOf(ActivityPeriod.Current) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val periodActivities = when (selectedPeriod) {
        ActivityPeriod.Current -> currentActivities
        ActivityPeriod.Past -> pastActivities
    }
    val displayedActivities = periodActivities.filter { activity ->
        selectedFilter == null || activity.filterTags.isEmpty() || selectedFilter in activity.filterTags
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "校园活动",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "发现湘大正在发生的新鲜事",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )
                if (currentActivities.isNotEmpty()) {
                    ActivityPosterCarousel(
                        activities = currentActivities.take(3),
                        onActivityClick = { activity -> onActivityClick(activity.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "活动",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                    if (usingFallback) {
                        Text(
                            text = "当前展示示例内容",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                TextButton(onClick = viewModel::refresh, enabled = !loading) {
                    Text(if (loading) "更新中…" else "刷新")
                }
                IconButton(onClick = { selectedFilter = null }) {
                    Icon(
                        painter = painterResource(Res.drawable.filter_list),
                        contentDescription = "筛选活动",
                        tint = if (selectedFilter == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            ActivityPeriodSwitcher(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it }
            )
        }

        if (filters.isNotEmpty()) {
            item {
                ActivityFilterRow(
                    filters = filters,
                    selected = selectedFilter,
                    onSelected = { selectedFilter = it }
                )
            }
        }

        items(displayedActivities, key = { "${selectedPeriod.name}-${it.id}" }) { activity ->
            ActivityListItem(
                activity = activity,
                isPast = selectedPeriod == ActivityPeriod.Past,
                onClick = { onActivityClick(activity.id) }
            )
        }

        if (displayedActivities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedPeriod == ActivityPeriod.Current) {
                            "暂无近期活动"
                        } else {
                            "暂无往期活动"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        item {
            Box(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActivityFilterRow(filters: List<String>, selected: String?, onSelected: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf<String?>(null) .plus(filters).forEach { filter ->
            val active = selected == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                    .clickable { onSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(filter ?: "全部", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ActivityPeriodSwitcher(
    selectedPeriod: ActivityPeriod,
    onPeriodSelected: (ActivityPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ActivityPeriod.entries.forEach { period ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selectedPeriod == period) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(700)
                )
            }
        }
    }
}

@Composable
private fun ActivityListItem(
    activity: CampusActivity,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val thumbnailUrl = activity.posterUrl
    val thumbnailBitmap = rememberActivityImageBitmap(thumbnailUrl)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            thumbnailBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = activity.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Text(
                    text = activity.posterTitle.take(2),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight(800)
                )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = activity.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight(700),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isPast) {
                Text(
                    text = "已结束",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(700)
                )
            }
            Text(
                text = activity.participation,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = activity.location,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (activity.startDate == activity.endDate) {
                    activity.startDate
                } else {
                    "${activity.startDate} - ${activity.endDate}"
                },
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight(600),
                modifier = Modifier.padding(top = 3.dp)
            )
            Text(
                text = activity.durationLabel(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
