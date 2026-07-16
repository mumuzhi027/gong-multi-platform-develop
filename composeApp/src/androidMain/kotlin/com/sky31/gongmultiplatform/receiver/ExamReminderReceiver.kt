package com.sky31.gongmultiplatform.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sky31.gongmultiplatform.R
import com.sky31.gongmultiplatform.model.ExamElem
import com.sky31.gongmultiplatform.util.ContextProvider
import kotlinx.serialization.json.Json

class ExamReminderReceiver: BroadcastReceiver() {
    private val context by lazy { ContextProvider.context }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context?, intent: Intent?) {
        val exam = intent?.getStringExtra("exam")?.let { Json.decodeFromString<ExamElem>(it) }

        exam?.let {
            showNotification(it)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(exam: ExamElem) {
        val notification = NotificationCompat.Builder(context, "course_channel")
            .setSmallIcon(R.drawable.course_reminder)
            .setContentTitle("考试通知")
            .setContentText("${exam.name}的考试马上就要开始了！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(exam.hashCode(), notification)
    }
}