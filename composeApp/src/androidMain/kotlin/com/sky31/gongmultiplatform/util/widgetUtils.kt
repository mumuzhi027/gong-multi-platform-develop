package com.sky31.gongmultiplatform.util

import androidx.glance.appwidget.GlanceAppWidgetManager
import com.sky31.gongmultiplatform.MainActivity
import com.sky31.gongmultiplatform.widget.CoursesAppWidget

actual suspend fun updateAppWidget() {
    val context = MainActivity.getInstance()

    if (context == null) {
        println("Skip widget update because MainActivity instance is unavailable.")
        return
    }

    runCatching {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(CoursesAppWidget::class.java)
        glanceIds.forEach { glanceId ->
            runCatching {
                CoursesAppWidget().update(context, glanceId)
            }.onFailure {
                println("Failed to update widget $glanceId: ${it.message}")
            }
        }
    }.onFailure {
        println("Failed to refresh app widgets: ${it.message}")
    }
}
