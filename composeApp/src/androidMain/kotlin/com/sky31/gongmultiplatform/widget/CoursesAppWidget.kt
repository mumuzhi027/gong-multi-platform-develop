package com.sky31.gongmultiplatform.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sky31.gongmultiplatform.data.local.AppDatabase
import com.sky31.gongmultiplatform.data.local.getAppDatabase
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.db.getDatabaseBuilder
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.getCourseList
import com.sky31.gongmultiplatform.util.getCourseTime
import com.sky31.gongmultiplatform.util.toWeekdayNameCN
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CoursesAppWidget: GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = getAppDatabase(getDatabaseBuilder(context))
        val courses = getTodayCourses(database)

        provideContent {
            GlanceTheme(
                colors = AppWidgetGlanceColorScheme.colors
            ) {
                CoursesAppWidgetContent(courses)
            }
        }
    }

    @Composable
    private fun CoursesAppWidgetContent(
        courses: List<CourseElem>
    ) {
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.background)
                .padding(12.dp),
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row {
                        Text(
                            text = "${currentTime.month.ordinal + 1}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "/",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier
                                .padding(horizontal = 5.dp)
                        )

                        Text(
                            text = "${currentTime.day}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = "更新于${currentTime.hour.toString().padStart(2, '0')}:${currentTime.minute.toString().padStart(2, '0')}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 8.sp,
                        )
                    )
                }

                Spacer(
                    modifier = GlanceModifier
                        .defaultWeight()
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = toWeekdayNameCN(currentTime.dayOfWeek.ordinal + 1),
                        style = TextStyle(
                            color = GlanceTheme.colors.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "${courses.size}节课",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 8.sp,
                        )
                    )
                }
            }

            if(courses.isEmpty()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日无课",
                        style = TextStyle(
                            color = GlanceTheme.colors.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    items(courses) {course ->
                        CourseBox(course, currentTime)
                    }
                }
            }
        }
    }

    @Composable
    fun CourseBox(
        course: CourseElem,
        currentTime: LocalDateTime
    ) {
        val courseTime = getCourseTime(course, currentTime)

        Row(
            modifier = GlanceModifier
                .height(72.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(GlanceTheme.colors.surface)

                ) {  }

                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .cornerRadius(4.dp)
                        .background(GlanceTheme.colors.onSurfaceVariant)
                ) {  }
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .defaultWeight()
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(8.dp)
                        .background(GlanceTheme.colors.inverseOnSurface)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第${course.startTime}-${course.startTime + course.duration - 1}节",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 8.sp,
                            )
                        )

                        Spacer(
                            modifier = GlanceModifier
                                .width(10.dp)
                        )

                        Text(
                            text = "${courseTime[0].hour.toString().padStart(2, '0')}:${courseTime[0].minute.toString().padStart(2, '0')}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 8.sp,
                            )
                        )
                        Text(
                            text = "-",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 8.sp,
                            )
                        )
                        Text(
                            text = "${courseTime[1].hour.toString().padStart(2, '0')}:${courseTime[1].minute.toString().padStart(2, '0')}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontSize = 8.sp,
                            )
                        )
                    }

                    Text(
                        text = course.classroom,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 8.sp,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }

    private suspend fun getTodayCourses(database: AppDatabase): List<CourseElem> {
        val courseDataRepository = CourseDataRepositoryImpl(
            dao = database.getCourseDao()
        )
        val publicDataRepository = PublicDataRepositoryImpl(
            dao = database.getPublicDao()
        )

        val courseMap = courseDataRepository.getCourseMap()
        val calendar = publicDataRepository.getCalendar()

        if(courseMap != null && calendar != null) {
            return getCourseList(courseMap, calendar, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
        }

        return emptyList()
    }
}