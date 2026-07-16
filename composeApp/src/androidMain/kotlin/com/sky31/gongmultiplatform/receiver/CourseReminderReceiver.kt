package com.sky31.gongmultiplatform.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sky31.gongmultiplatform.R
import com.sky31.gongmultiplatform.model.CourseElem
import com.sky31.gongmultiplatform.util.ContextProvider
import kotlinx.serialization.json.Json

class CourseReminderReceiver: BroadcastReceiver() {
    private val context by lazy { ContextProvider.context }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context?, intent: Intent?) {
        val course = intent?.getStringExtra("course")?.let { Json.decodeFromString<CourseElem>(it) }

        course?.let  {
            showNotification(it)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(course: CourseElem) {
        val notification = NotificationCompat.Builder(context, "course_channel")
            .setSmallIcon(R.drawable.course_reminder)
            .setContentTitle("上课通知")
            .setContentText("${course.name}马上就要上课哩！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(course.hashCode(), notification)
    }
}