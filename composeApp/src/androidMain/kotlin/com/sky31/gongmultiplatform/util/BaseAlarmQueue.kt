package com.sky31.gongmultiplatform.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context

abstract class BaseAlarmQueue {
    private val alarmList = mutableListOf<PendingIntent>()

    fun addAlarm(alarm: PendingIntent) {
        alarmList.add(alarm)
    }

    fun cancelAll() {
        val context = runCatching { ContextProvider.context }.getOrElse {
            println("Failed to resolve alarm queue context: ${it.message}")
            null
        } ?: run {
            alarmList.clear()
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: run {
                alarmList.clear()
                return
            }

        for(pendingIntent in alarmList) {
            runCatching {
                alarmManager.cancel(pendingIntent)
            }.onFailure {
                println("Failed to cancel pending alarm: ${it.message}")
            }
        }

        alarmList.clear()
    }
}
