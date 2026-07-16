package com.sky31.gongmultiplatform.util

import android.content.Context
import android.os.Build
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class PlatformInfo: KoinComponent {
    private val context: Context by inject()

    actual fun getVersionName(): String {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(
            context.packageName,
            0
        )

        return packageInfo.versionName!!
    }

    actual fun getVersionCode(): Long {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val packageInfo = packageManager.getPackageInfo(packageName, 0)


        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }
}