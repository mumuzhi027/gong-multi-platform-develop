package com.sky31.gongmultiplatform.worker

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.ConfigRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.CustomTime
import com.sky31.gongmultiplatform.util.getCourseList
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
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DailyScheduleWorker(
    context: Context,
    params: WorkerParameters
): Worker(context, params), KoinComponent {
    private val publicDataRepository: PublicDataRepositoryImpl by inject()
    private val courseDataRepository: CourseDataRepositoryImpl by inject()
    private val configRepository: ConfigRepositoryImpl by inject()

    private val uuidList = mutableListOf<UUID>()

    @OptIn(ExperimentalTime::class)
    override fun doWork(): Result {
        val startTimes = getStartTimes()
        val now = System.currentTimeMillis()
        val wm = WorkManager.getInstance(applicationContext)

        for(uuid in uuidList) {
            wm.cancelWorkById(uuid)
        }
        uuidList.clear()

        CoroutineScope(Dispatchers.IO).launch {
            val config = configRepository.getNotificationConfig() ?: return@launch
            if (!config.courseNotification) return@launch
            getCourseList()?.let {
                for(course in it) {
                    val startTime = startTimes[course.startTime - 1]
                    val triggerMillis = getTriggerMillis(
                        startTime.hour,
                        startTime.minute,
                        config.courseReminderMinutesBefore
                    )

                    if(triggerMillis > now) {
                        val delay = triggerMillis - now

                        val reminderWorker = OneTimeWorkRequestBuilder<CourseReminderWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(
                                workDataOf(
                                    "course" to Json.encodeToString<CourseElem>(course)
                                )
                            )
                            .build()

                        wm.enqueue(reminderWorker)
                        uuidList.add(reminderWorker.id)
                    }
                }
            }
        }

        return Result.success()
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun getCourseList(): List<CourseElem>? {
        val courseMap = courseDataRepository.getCourseMap()
        val calendar = publicDataRepository.getCalendar()

        return courseMap?.let {map ->
            calendar?.let { calendar ->
                getCourseList(
                    map,
                    calendar,
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
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
