package com.sky31.gongmultiplatform.ui.screen.activityScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.CampusActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ActivityPosterCarousel(
    activities: List<CampusActivity>,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    onActivityClick: ((CampusActivity) -> Unit)? = null
) {
    if (activities.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { activities.size })

    LaunchedEffect(autoPlay, activities.size) {
        if (!autoPlay || activities.size < 2) return@LaunchedEffect

        while (isActive) {
            delay(3_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % activities.size)
        }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ActivityPoster(
                activity = activities[page],
                onClick = onActivityClick
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(activities.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = .45f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun ActivityPoster(
    activity: CampusActivity,
    onClick: ((CampusActivity) -> Unit)?
) {
    val posterBitmap = rememberActivityImageBitmap(activity.posterUrl)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(activity) }
                } else {
                    Modifier
                }
            )
    ) {
        posterBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = activity.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (posterBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = .04f), Color.Black.copy(alpha = .52f))
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Text(
                text = activity.title,
                color = if (posterBitmap == null) Color(0xB3000000) else Color.White.copy(alpha = .86f),
                fontSize = 12.sp,
                fontWeight = FontWeight(600),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = activity.posterTitle,
                color = if (posterBitmap == null) Color(0xFF132E3D) else Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight(800),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = activity.posterSubtitle,
                color = if (posterBitmap == null) Color(0xCC132E3D) else Color.White.copy(alpha = .9f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = activity.startDate,
            color = if (posterBitmap == null) Color(0xB3132E3D) else Color.White.copy(alpha = .9f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 24.dp, vertical = 18.dp)
        )
    }
}
