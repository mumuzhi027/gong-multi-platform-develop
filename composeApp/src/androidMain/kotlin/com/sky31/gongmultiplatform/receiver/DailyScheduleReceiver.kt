package com.sky31.gongmultiplatform.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.ConfigRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.CustomTime
import com.sky31.gongmultiplatform.util.getStartTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DailyScheduleReceiver: BroadcastReceiver(), KoinComponent {
    private val publicDataRepository: PublicDataRepositoryImpl by inject()
    private val courseDataRepository: CourseDataRepositoryImpl by inject()
    private val configRepository: ConfigRepositoryImpl by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        val startTimes = getStartTimes()
        val now = System.currentTimeMillis()
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        CoroutineScope(Dispatchers.IO).launch {
            val config = configRepository.getNotificationConfig() ?: return@launch
            if (!config.courseNotification) return@launch
            getCourseList()?.let {
                for(course in it) {
                    val startTime = startTimes.getOrNull(course.startTime - 1) ?: continue
                    val triggerMillis = getTriggerMillis(
                        startTime.hour,
                        startTime.minute,
                        config.courseReminderMinutesBefore
                    )

                    if(triggerMillis < now) {
                        return@launch
                    }

                    val intent = Intent(context, CourseReminderReceiver::class.java).apply {
                        putExtra("course", Json.encodeToString<CourseElem>(course))
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        course.hashCode(),
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    CourseAlarmQueue.addAlarm(pendingIntent)
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if(alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerMillis,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                }
            }
        }

    }

    private suspend fun getCourseList(): List<CourseElem>? {
        val courseMap = courseDataRepository.getCourseMap()
        val calendar = publicDataRepository.getCalendar()

        return courseMap?.let {map ->
            calendar?.let { calendar ->
                com.sky31.gongmultiplatform.util.getCourseList(
                    map,
                    calendar,
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            }
        }
    }

    private fun getStartTimes(): List<CustomTime> {
        return getStartTime(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
    }

    private fun getTriggerMillis(hour: Int, minute: Int, advanceMinutes: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -advanceMinutes.coerceIn(0, 360))
        }

        return cal.timeInMillis
    }
}
