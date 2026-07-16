package com.sky31.gongmultiplatform.util

import com.sky31.gongmultiplatform.MainActivity

actual fun hasInstallPermission(): Boolean {
    val helper = MainActivity.getPermissionHelper()
        ?: throw IllegalStateException("PermissionHelper 不存在")

    return helper.hasInstallPermission()
}

actual fun askInstallPermission(
    onResult: (Boolean) -> Unit
) {
    MainActivity.getPermissionHelper()?.apply {
        askInstallPermission(onResult)
    }
}