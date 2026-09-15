package com.sky31.gongmultiplatform.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.sky31.gongmultiplatform.MainActivity
import com.sky31.gongmultiplatform.widget.CoursesWidgetReceiver

// 不支持 requestPinAppWidget 只代表桌面不能自动弹出固定窗口，
// 不代表用户不能从系统的“小组件/窗口小工具”列表手动添加。
actual fun desktopWidgetSupported(): Boolean = true

fun ensureDesktopWidgetProviderAvailable(context: Context) {
    ensureWidgetProviderEnabled(
        context.packageManager,
        ComponentName(context, CoursesWidgetReceiver::class.java)
    )
}

actual fun isDesktopWidgetEnabled(): Boolean {
    val context = MainActivity.getInstance() ?: return false
    val component = ComponentName(context, CoursesWidgetReceiver::class.java)
    ensureWidgetProviderEnabled(context.packageManager, component)
    return AppWidgetManager.getInstance(context).getAppWidgetIds(component).isNotEmpty()
}

actual fun setDesktopWidgetEnabled(enabled: Boolean): Boolean {
    val context = MainActivity.getInstance() ?: return false
    val component = ComponentName(context, CoursesWidgetReceiver::class.java)
    // 组件提供者保持注册；禁用 Receiver 会导致部分小米桌面无法重新发现它。
    ensureWidgetProviderEnabled(context.packageManager, component)
    // Android 不允许应用主动移除已固定的桌面组件，关闭需由用户在桌面长按移除。
    if (!enabled) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = AppWidgetManager.getInstance(context)
    return manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(component, null, null)
}

private fun ensureWidgetProviderEnabled(packageManager: PackageManager, component: ComponentName) {
    if (packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
