package com.sky31.gongmultiplatform.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.sky31.gongmultiplatform.MainActivity
import androidx.core.net.toUri

class PermissionHelper(
    private val activity: ComponentActivity
) {
    private val notificationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationCallback?.invoke(granted)
    }

    private val installLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        installCallback?.invoke(hasInstallPermission())
    }

    private var notificationCallback: ((Boolean) -> Unit)? = null
    private var installCallback: ((Boolean) -> Unit)? = null

    fun askPostNotificationPermission(onResult: (Boolean) -> Unit) {
        val hasPermission = hasPostNotificationPermission()

        if(!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationCallback = {granted ->
                onResult(granted)

                if(granted) {
                    val channelId = "course_channel"
                    val channelName = "课程提醒"
                    val channelDesc = "每天上课前的提醒通知"

                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(channelId, channelName, importance).apply {
                        description = channelDesc
                    }

                    val manager = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.createNotificationChannel(channel)
                }
            }

            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun hasPostNotificationPermission(): Boolean {
        MainActivity.getInstance()?.let { context ->
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            return true
        }

        throw Exception("No MainActivity instance!")
    }

    fun askInstallPermission(onResult: (Boolean) -> Unit) {
        val hasPermission = hasInstallPermission()

        if(!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            installCallback = onResult

            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${activity.packageName}".toUri()
            }
            installLauncher.launch(intent)
        }
    }

    fun hasInstallPermission(): Boolean {
        MainActivity.getInstance()?.let { context ->
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return context.packageManager.canRequestPackageInstalls()
            }

            return true
        }

        throw Exception("No MainActivity instance!")
    }
}
