package com.sky31.gongmultiplatform.util

expect fun hasInstallPermission(): Boolean

expect fun askInstallPermission(
    onResult: (Boolean) -> Unit
)