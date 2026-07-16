package com.sky31.gongmultiplatform.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
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

    override fun onReceive(context: Context?, intent: Intent?) {
        val startTimes = getStartTimes()
        val now = System.currentTimeMillis()
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        CoroutineScope(Dispatchers.IO).launch {
            getCourseList()?.let {
                for(course in it) {
                    val startTime = startTimes[course.startTime - 1]
                    val triggerMillis = getTriggerMillis(startTime.hour, startTime.minute)

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

    private fun getTriggerMillis(hour: Int, minute: Int): Long {
        val triggerTime = Pair(
            first = if(minute < 10) hour - 1 else hour,
            second = if(minute < 10) minute + 50 else minute - 10
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, triggerTime.first)
            set(Calendar.MINUTE, triggerTime.second)
            set(Calendar.SECOND, 0)
        }

        return cal.timeInMillis
    }
}