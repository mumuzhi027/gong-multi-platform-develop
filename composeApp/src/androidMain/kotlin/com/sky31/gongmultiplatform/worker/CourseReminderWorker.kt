package com.sky31.gongmultiplatform.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sky31.gongmultiplatform.R
import com.sky31.gongmultiplatform.model.CourseElem
import kotlinx.serialization.json.Json

class CourseReminderWorker(
    context: Context,
    params: WorkerParameters
): Worker(context, params) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "course_channel"
            val channelName = "课程提醒"
            val channelDesc = "每天上课前的提醒通知"

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDesc
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        inputData.getString("course")?.let {
            showNotification(Json.decodeFromString<CourseElem>(it) )
        }

        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(course: CourseElem) {
        val notification = NotificationCompat.Builder(applicationContext, "course_channel")
            .setSmallIcon(R.drawable.course_reminder)
            .setContentTitle("上课通知")
            .setContentText("${course.name}马上就要上课哩！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(course.hashCode(), notification)
    }
}