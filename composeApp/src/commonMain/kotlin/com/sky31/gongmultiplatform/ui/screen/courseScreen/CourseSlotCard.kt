package com.sky31.gongmultiplatform.ui.screen.courseScreen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.ui.component.courseConflictSheen
import com.sky31.gongmultiplatform.ui.component.staggeredCourseBorder
import com.sky31.gongmultiplatform.util.CourseSlot
import com.sky31.gongmultiplatform.util.CourseTimeMode
import com.sky31.gongmultiplatform.util.isStaggeredCourse

@Composable
internal fun CourseSlotCard(
    slot: CourseSlot,
    week: Long,
    timeMode: CourseTimeMode,
    onOpen: (CourseElem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember(slot.courses, week) { mutableIntStateOf(0) }
    val course = slot.courses[selected]
    val conflict = slot.courses.size > 1 && slot.courses.any { it.isExperiment }
    val opacity by animateFloatAsState(timeMode.courseAlpha(course.isStaggeredCourse()), tween(320), label = "courseModeAlpha")
    Box(modifier.padding(horizontal = 2.dp, vertical = 3.dp).alpha(opacity).clip(RoundedCornerShape(4.dp))) {
        Crossfade(targetState = course, modifier = Modifier.fillMaxSize(), animationSpec = tween(320), label = "experimentCourseSwap") { displayed ->
            val textColor = MaterialTheme.colorScheme.onSurface
            val longest = maxOf(displayed.name.length, displayed.classroom.length)
            val font = when {
                slot.duration >= 3 -> 12.sp
                slot.duration == 2 -> 11.sp
                longest >= 11 -> 9.5.sp
                longest >= 8 -> 10.sp
                else -> 11.sp
            }
            Column(
                Modifier.fillMaxSize()
                    .clickable(role = Role.Button, onClickLabel = if (conflict && !displayed.isExperiment) "切换实验课" else "查看课程详情") {
                        if (conflict && !displayed.isExperiment) selected = slot.courses.indexOfFirst { it.isExperiment }
                        else onOpen(displayed)
                    }
                    .background(generateCourseColor(displayed.name))
                    .staggeredCourseBorder(enabled = displayed.isStaggeredCourse())
                    .courseConflictSheen(enabled = conflict && !displayed.isExperiment)
                    .padding(horizontal = 3.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(displayed.name, color = textColor, fontSize = font, lineHeight = (font.value + 1).sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        maxLines = if (slot.duration >= 2) 4 else 3, overflow = TextOverflow.Ellipsis)
                    if (displayed.classroom.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(displayed.classroom, color = textColor, fontSize = (font.value - 1).sp, lineHeight = font,
                            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (displayed.isExperiment) {
                    Text("实验课", color = Color(0xFF171717).copy(alpha = .85f), fontSize = 8.5.sp, lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = .2.sp,
                        modifier = Modifier.padding(top = 3.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = .42f))
                            .border(BorderStroke(.5.dp, Color(0xFF171717).copy(alpha = .14f)), RoundedCornerShape(3.dp))
                            .clickable(role = Role.Button, onClickLabel = if (conflict) "切换同一时段课程" else "查看实验课详情") {
                                if (conflict) selected = (selected + 1) % slot.courses.size else onOpen(displayed)
                            }.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }
        }
    }
}
