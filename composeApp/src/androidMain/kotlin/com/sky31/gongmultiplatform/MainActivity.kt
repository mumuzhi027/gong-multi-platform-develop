package com.sky31.gongmultiplatform

import android.app.AlarmManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.sky31.gongmultiplatform.network.service.InstallService
import com.sky31.gongmultiplatform.util.PermissionHelper
import com.sky31.gongmultiplatform.util.setCourseAlarm
import java.lang.ref.WeakReference

class MainActivity: ComponentActivity() {
    companion object {
        private var activityRef: WeakReference<ComponentActivity>? = null
        private var permissionHelper: WeakReference<PermissionHelper>? = null

        fun getInstance(): ComponentActivity? = activityRef?.get()
        fun getPermissionHelper(): PermissionHelper? = permissionHelper?.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        activityRef = WeakReference(this)
        permissionHelper = WeakReference(PermissionHelper(this))

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()

        runCatching {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    setCourseAlarm()
                }
            }
        }.onFailure {
            println("Failed to refresh course alarms on resume: ${it.message}")
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
