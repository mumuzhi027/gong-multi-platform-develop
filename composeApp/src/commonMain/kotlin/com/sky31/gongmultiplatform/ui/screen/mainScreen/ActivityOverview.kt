package com.sky31.gongmultiplatform.ui.screen.mainScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.ui.screen.activityScreen.ActivityPosterCarousel
import com.sky31.gongmultiplatform.ui.viewModel.ActivityViewModel
import com.sky31.gongmultiplatform.model.currentActivityDate
import com.sky31.gongmultiplatform.model.isPastActivity
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun ActivityOverview(
    toActivityScreen: () -> Unit,
    toActivityDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = getKoin().get<ActivityViewModel>()
    val activities by viewModel.activities.collectAsState()
    val today = currentActivityDate()
    val currentActivities = activities.filterNot { it.isPastActivity(today) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "校园活动",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = toActivityScreen) {
                Text(
                    text = "查看更多  ›",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        ActivityPosterCarousel(
            activities = currentActivities.take(3),
            onActivityClick = { activity -> toActivityDetail(activity.id) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 10.dp)
        )
    }
}
