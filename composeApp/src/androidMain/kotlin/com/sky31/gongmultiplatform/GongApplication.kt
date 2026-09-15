package com.sky31.gongmultiplatform

import android.app.Application
import com.sky31.gongmultiplatform.util.initKoin
import com.sky31.gongmultiplatform.util.ensureDesktopWidgetProviderAvailable

class GongApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        ensureDesktopWidgetProviderAvailable(this)
        initKoin(this)
    }
}
