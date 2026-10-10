package com.sky31.gongmultiplatform.ui.screen.courseScreen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.di.LocalIsDarkTheme
import com.sky31.gongmultiplatform.util.CustomTime
import com.sky31.gongmultiplatform.util.CourseTimeMode
import com.sky31.gongmultiplatform.util.customTimeToString
import com.sky31.gongmultiplatform.util.getStartTime
import kotlinx.datetime.LocalDate

/** 六个独立的查看分组；状态只改变时间轴，不修改课程或提醒。 */
@Composable
internal fun CourseTimeAxis(
    date: LocalDate,
    staggeredGroups: Int,
    mode: CourseTimeMode,
    periodCount: Int = 11,
    onToggleGroup: (Int) -> Unit,
    onToggleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val regularStarts = remember(date) { getStartTime(date) }
    val staggeredStarts = remember(date) { getStartTime(date, staggered = true) }
    val dark = LocalIsDarkTheme.current
    val selectedBackground = if (dark) Color(0xFF163E58) else Color(0xFFCEE9FC)
    val selectedText = if (dark) Color(0xFFB6E4FF) else Color(0xFF135379)
    val normalBackground = MaterialTheme.colorScheme.surface
    val normalText = MaterialTheme.colorScheme.onSurface
    val nextMode = mode.next().label
    val headerBackground by animateColorAsState(
        if (staggeredGroups == 0) normalBackground else selectedBackground,
        animationSpec = tween(260), label = "timeAxisHeaderBackground"
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(headerBackground)
                .semantics { stateDescription = "${mode.label}时段" }
                .clickable(role = Role.Button, onClickLabel = "整列切换为${nextMode}时段", onClick = onToggleAll),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Crossfade(targetState = mode.label, animationSpec = tween(240), label = "timeAxisMode") { label ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = if (staggeredGroups == 0) normalText else selectedText)
                }
            }
            Text("${date.month.ordinal + 1}月", style = MaterialTheme.typography.labelSmall,
                color = if (staggeredGroups == 0) MaterialTheme.colorScheme.onSurfaceVariant else selectedText)
        }

        repeat(6) { group ->
            val firstPeriod = group * 2
            val count = minOf(2, periodCount - firstPeriod)
            val enabled = (staggeredGroups and (1 shl group)) != 0
            val periodLabel = if (count == 2) "第${firstPeriod + 1}–${firstPeriod + 2}节" else "第${firstPeriod + 1}节"
            val groupBackground by animateColorAsState(
                if (enabled) selectedBackground else normalBackground,
                animationSpec = tween(260), label = "timeAxisGroupBackground"
            )
            val displayedStarts = if (enabled) staggeredStarts else regularStarts
            val lastStart = displayedStarts.getOrNull(firstPeriod + count - 1)
            val endMinutes = lastStart?.let { it.hour * 60 + it.minute + 45 }
            val range = customTimeToString(displayedStarts[firstPeriod]) + "至" +
                (endMinutes?.let { customTimeToString(CustomTime(it / 60, it % 60)) } ?: "第12节")

            Box(
                modifier = Modifier
                    .weight(count.toFloat())
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(groupBackground)
                    .semantics(mergeDescendants = true) {
                        selected = enabled
                        stateDescription = "$periodLabel，${if (enabled) "错峰" else "常规"}时段，$range"
                    }
                    .clickable(role = Role.Button,
                        onClickLabel = "$periodLabel 切换为${if (enabled) "常规" else "错峰"}时段",
                        onClick = { onToggleGroup(group) })
            ) {
                Crossfade(targetState = enabled, modifier = Modifier.fillMaxSize(),
                    animationSpec = tween(240), label = "timeAxisGroupTimes") { staggered ->
                    val starts = if (staggered) staggeredStarts else regularStarts
                    Column(Modifier.fillMaxSize()) {
                        repeat(count) { offset ->
                            val start = starts.getOrNull(firstPeriod + offset)
                            val end = start?.let { it.hour * 60 + it.minute + 45 }
                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.SpaceEvenly,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (start != null && end != null) {
                                    Text(customTimeToString(start), style = MaterialTheme.typography.labelSmall,
                                        color = if (staggered) selectedText else normalText)
                                    Text(customTimeToString(CustomTime(end / 60, end % 60)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (staggered) selectedText else normalText)
                                } else Text("第12节", style = MaterialTheme.typography.labelSmall, color = normalText)
                            }
                        }
                    }
                }
            }
            if (group == 1 || group == 3) {
                // 与课程列的午间、晚间间隔保持对齐。
                Box(Modifier.height(10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Spacer(Modifier.height(5.dp).fillMaxWidth().background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}
