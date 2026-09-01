package com.sky31.gongmultiplatform.ui.screen.activityScreen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sky31.gongmultiplatform.ui.viewModel.ActivityViewModel
import com.sky31.gongmultiplatform.model.durationLabel
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.baseline_arrow_back_24
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun ActivityDetailScreen(
    activityId: String,
    onBack: () -> Unit
) {
    val viewModel = getKoin().get<ActivityViewModel>()
    val activities by viewModel.activities.collectAsState()
    val activity = activities.firstOrNull { it.id == activityId }
    var previewBitmap by remember(activityId) { mutableStateOf<ImageBitmap?>(null) }
    var previewDescription by remember(activityId) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.baseline_arrow_back_24),
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBack)
                    .padding(8.dp)
            )
            Text(
                text = "活动详情",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (activity == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "活动不存在或已下架",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            return@Column
        }

        val heroBitmap = rememberActivityImageBitmap(activity.posterUrl)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            heroBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = activity.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            previewBitmap = bitmap
                            previewDescription = activity.title
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = .42f))
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    text = activity.posterTitle.ifBlank { activity.title },
                    color = if (heroBitmap == null) MaterialTheme.colorScheme.onSurface else Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight(800)
                )
                val heroSubtitle = activity.posterSubtitle.ifBlank { activity.organizer }
                if (heroSubtitle.isNotBlank()) {
                    Text(
                        text = heroSubtitle,
                        color = if (heroBitmap == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = .88f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activity.organizer.isNotBlank()) {
                ActivityDetailLine("主办方", activity.organizer)
            }
            ActivityDetailLine("活动时间", formatActivityDate(activity.startDate, activity.endDate))
            ActivityDetailLine("持续时间", activity.durationLabel())
            ActivityDetailLine("活动地点", activity.location.ifBlank { "待定" })
        }

        Text(
            text = "参与方式",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
        )
        Text(
            text = activity.participation.ifBlank { "请关注活动后续通知" },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(18.dp)
        )

        Text(
            text = "活动介绍",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
        )
        Text(
            text = activity.description.ifBlank { "暂无更多活动介绍" },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(18.dp)
        )

        if (activity.images.isNotEmpty()) {
            Text(
                text = "活动图片",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activity.images.forEachIndexed { index, imageUrl ->
                    val imageBitmap = rememberActivityImageBitmap(imageUrl)
                    imageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = "${activity.title} 活动图片 ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    previewBitmap = bitmap
                                    previewDescription = "${activity.title} 活动图片 ${index + 1}"
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }

    previewBitmap?.let { bitmap ->
        ActivityImagePreviewDialog(
            bitmap = bitmap,
            contentDescription = previewDescription,
            onDismiss = { previewBitmap = null }
        )
    }
}

@Composable
private fun ActivityImagePreviewDialog(
    bitmap: ImageBitmap,
    contentDescription: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .94f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .clickable { }
            )
            Text(
                text = "关闭",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight(700),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 18.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = .45f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ActivityDetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(.28f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight(600),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(.72f)
        )
    }
}

private fun formatActivityDate(startDate: String, endDate: String): String =
    if (startDate == endDate) startDate else "$startDate - $endDate"
