package com.sky31.gongmultiplatform.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import com.sky31.gongmultiplatform.MainActivity
import com.sky31.gongmultiplatform.widget.CoursesWidgetReceiver

actual fun desktopWidgetSupported(): Boolean {
    val context = MainActivity.getInstance() ?: return false
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
}

actual fun isDesktopWidgetEnabled(): Boolean {
    val context = MainActivity.getInstance() ?: return false
    val component = ComponentName(context, CoursesWidgetReceiver::class.java)
    // 迁移旧版本曾写入的 DISABLED 状态，确保桌面（尤其是澎湃 OS）始终能发现组件。
    if (context.packageManager.getComponentEnabledSetting(component) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
        context.packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            PackageManager.DONT_KILL_APP
        )
    }
    return AppWidgetManager.getInstance(context).getAppWidgetIds(component).isNotEmpty()
}

actual fun setDesktopWidgetEnabled(enabled: Boolean): Boolean {
    val context = MainActivity.getInstance() ?: return false
    val component = ComponentName(context, CoursesWidgetReceiver::class.java)
    // 组件提供者保持注册；禁用 Receiver 会导致部分小米桌面无法重新发现它。
    context.packageManager.setComponentEnabledSetting(
        component,
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
        PackageManager.DONT_KILL_APP
    )
    // Android 不允许应用主动移除已固定的桌面组件，关闭需由用户在桌面长按移除。
    if (!enabled) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = AppWidgetManager.getInstance(context)
    return manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(component, null, null)
}
