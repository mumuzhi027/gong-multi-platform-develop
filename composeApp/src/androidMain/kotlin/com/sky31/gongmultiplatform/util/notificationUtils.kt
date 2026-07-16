package com.sky31.gongmultiplatform.util

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.sky31.gongmultiplatform.MainActivity
import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.receiver.CourseAlarmQueue
import com.sky31.gongmultiplatform.receiver.DailyScheduleReceiver
import com.sky31.gongmultiplatform.receiver.ExamAlarmQueue
import com.sky31.gongmultiplatform.receiver.ExamReminderReceiver
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.Calendar
import kotlin.time.ExperimentalTime

const val ONE_WEEK_MILLIS = 604800000
var scheduleIntent: PendingIntent? = null

actual fun scheduleCourseAlarm() {
    val context = MainActivity.getInstance() ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
            } else {
                setCourseAlarm()
            }
        } else {
            setCourseAlarm()
        }
    }.onFailure {
        println("Failed to request course alarm scheduling: ${it.message}")
    }
}

fun setCourseAlarm() {
    val context = runCatching { ContextProvider.context }.getOrElse {
        println("Failed to resolve notification context: ${it.message}")
        null
    } ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    runCatching {
        scheduleIntent?.let { alarmManager.cancel(it) }
        CourseAlarmQueue.cancelAll()

        val intent = Intent(context, DailyScheduleReceiver::class.java)

        // Trigger today's reminder once, then schedule the repeating midnight alarm.
        context.sendBroadcast(intent)

        scheduleIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            scheduleIntent ?: return@runCatching
        )
    }.onFailure {
        println("Failed to set repeating course alarm: ${it.message}")
    }
}

@OptIn(ExperimentalTime::class)
actual fun scheduleExamAlarm(exams: List<ExamElem>) {
    val context = runCatching { ContextProvider.context }.getOrElse {
        println("Failed to resolve exam notification context: ${it.message}")
        null
    } ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    runCatching {
        ExamAlarmQueue.cancelAll()

        exams.forEach { exam ->
            if (exam.startTime.isBlank()) return@forEach

            val examDateTime = LocalDateTime.parse(exam.startTime)
            val triggerMillis =
                examDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() - ONE_WEEK_MILLIS

            val intent = Intent(context, ExamReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                exam.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            ExamAlarmQueue.addAlarm(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
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
    }.onFailure {
        println("Failed to schedule exam alarms: ${it.message}")
    }
}

actual fun hasPostNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = MainActivity.getInstance()
        context != null && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

actual fun askPostNotificationPermission(onResult: (Boolean) -> Unit) {
    MainActivity.getPermissionHelper()?.apply {
        askPostNotificationPermission(onResult)
    } ?: onResult(false)
}
